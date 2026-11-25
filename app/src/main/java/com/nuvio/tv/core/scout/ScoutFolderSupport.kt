package com.nuvio.tv.core.scout

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.data.local.WatchedItemsPreferences
import android.util.Log
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.ScoutProfile
import com.nuvio.tv.domain.model.listedTitles
import com.nuvio.tv.domain.model.scoutExclusionName
import com.nuvio.tv.domain.model.scoutNormalizedName
import com.nuvio.tv.domain.repository.LibraryRepository
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.Lazy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val LIBRARY_WAIT_MS = 2_000L
private const val LISTED_FRESH_MS = 60_000L
private const val CHANGE_SETTLE_MS = 2_000L

/** Titles Scout must not offer: on Plan to watch or Watching, whatever the Home switches say. */
data class ScoutListed(val ids: Set<String> = emptySet(), val names: Set<String> = emptySet())

/** What Scout's rows and tile pages leave out, read fresh each time a row loads. */
@Singleton
class ScoutFolderSupport @Inject constructor(
    private val store: ScoutStore,
    private val profiles: ProfileManager,
    private val watchProgress: Lazy<WatchProgressRepository>,
    private val library: Lazy<LibraryRepository>,
    private val tmdbIds: com.nuvio.tv.core.tmdb.TmdbIdStore,
    private val localWatched: WatchedItemsPreferences
) {
    fun sliceId(collectionId: String, folderId: String): String? =
        folderId.takeIf { collectionId == ScoutCatalogProvider.TILES_COLLECTION_ID && ScoutSlices.byId(it) != null }

    fun isSaved(sliceId: String): Flow<Boolean> = store.profile.map { sliceId in it.settings.savedSlices }

    internal suspend fun displayTitle(sliceId: String, title: String): String =
        scoutListTitle(title, scoutSliceEmoji(sliceId), store.current().settings.listEmojis)

    suspend fun toggleSaved(sliceId: String) {
        store.update { settings ->
            val saved = settings.savedSlices
            settings.copy(savedSlices = if (sliceId in saved) saved - sliceId else saved + sliceId)
        }
    }

    private class RememberedList(val owner: ScoutSelection, val at: Long, val listed: ScoutListed)

    @Volatile private var remembered: RememberedList? = null
    private val refreshing = AtomicBoolean(false)
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The last answer at once when there is one; the library can take seconds to reply. */
    suspend fun listed(): ScoutListed = listed(profiles.scoutSelection())

    internal suspend fun listed(owner: ScoutSelection, localProgress: List<com.nuvio.tv.domain.model.WatchProgress>? = null): ScoutListed =
        withContext(Dispatchers.Default) {
            if (owner != profiles.scoutSelection()) return@withContext ScoutListed()
            val last = remembered?.takeIf { it.owner == owner }
                ?: return@withContext listedLock.withLock {
                    remembered?.takeIf { it.owner == owner && owner == profiles.scoutSelection() }?.listed
                        ?: loadListed(owner, localProgress).also { rememberListed(owner, it) }
                        ?: ScoutListed()
                }
            if (System.currentTimeMillis() - last.at >= LISTED_FRESH_MS && refreshing.compareAndSet(false, true)) {
                background.launch {
                    try {
                        rememberListed(owner, loadListed(owner, localProgress))
                    } finally {
                        refreshing.set(false)
                    }
                }
            }
            last.listed.takeIf { owner == profiles.scoutSelection() } ?: ScoutListed()
        }

    private fun rememberListed(owner: ScoutSelection, listed: ScoutListed?) {
        if (listed == null || owner != profiles.scoutSelection()) return
        remembered = RememberedList(owner, System.currentTimeMillis(), listed)
    }

    private suspend fun loadListed(
        owner: ScoutSelection,
        localProgress: List<com.nuvio.tv.domain.model.WatchProgress>?
    ): ScoutListed? = withTimeoutOrNull(LIBRARY_WAIT_MS) {
        if (owner != profiles.scoutSelection()) return@withTimeoutOrNull null
        val tabs = library.get().listTabs.first()
        if (owner != profiles.scoutSelection()) return@withTimeoutOrNull null
        val items = library.get().libraryItems.first()
        if (owner != profiles.scoutSelection()) return@withTimeoutOrNull null
        val inProgress = (localProgress ?: watchProgress.get().allProgress.first())
            .filter { it.isInProgress() }.map { it.contentId }
        if (owner != profiles.scoutSelection()) return@withTimeoutOrNull null
        val listed = listedTitles(tabs, items, inProgress)
        val keys = listed.planListKeys + listed.watchingListKeys
        ScoutListed(
            ids = listed.planIds + listed.watchingIds,
            names = items.filter { entry -> entry.listKeys.any(keys::contains) }
                .mapTo(HashSet()) { scoutExclusionName(it.name, it.releaseInfo) }
        )
    }

    private class SeenNames(val owner: ScoutSelection, val profile: ScoutProfile, val listed: ScoutListed, val at: Long, val names: Set<String>)

    @Volatile private var seenNames: SeenNames? = null
    private val namesLock = Mutex()
    private val listedLock = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val watchedMovies: Flow<ScoutWatchedMovies> =
        profiles.activeProfileId.flatMapLatest(::watchedMoviesForProfile)

    internal fun watchedMoviesForProfile(profileId: Int): Flow<ScoutWatchedMovies> = flow {
        emitAll(movieHistory(profileId, useLocalFirst = true))
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    private fun movieHistory(profileId: Int, useLocalFirst: Boolean): Flow<ScoutWatchedMovies> {
        val owner = profiles.scoutSelection()
        if (owner.profileId != profileId) return localWatched.observeAllItems(profileId).map {
            scoutWatchedMovies(it, emptySet(), tmdbIds.load())
        }
        val repository = watchProgress.get()
        val history = repository.tasteHistory
        return combine(
            if (useLocalFirst) history.onStart { emit(emptyList()) } else history,
            repository.observeWatchedMovieIds(),
            localWatched.observeAllItems(profileId)
        ) { remote, marked, local ->
            val known = tmdbIds.load()
            if (owner == profiles.scoutSelection()) scoutWatchedMovies(remote + local, marked, known)
            else scoutWatchedMovies(local, emptySet(), known)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val showWatched: Flow<Boolean> = profiles.activeProfileId.flatMapLatest(store::observeShowWatched)

    internal suspend fun toggleShowWatched() {
        val profileId = profiles.activeProfileId.value
        val show = store.observeShowWatched(profileId).first()
        if (profiles.activeProfileId.value == profileId) store.setShowWatched(profileId, !show)
    }

    private suspend fun namesToHide(owner: ScoutSelection, profile: ScoutProfile, listed: ScoutListed): Set<String> = namesLock.withLock {
        val now = System.currentTimeMillis()
        if (owner != profiles.scoutSelection()) return@withLock emptySet()
        seenNames?.takeIf { it.owner == owner && it.profile == profile && it.listed == listed && now - it.at < LISTED_FRESH_MS }?.let { return@withLock it.names }
        val repository = watchProgress.get()
        val names = repository.allProgress.first().filter { it.isInProgress() }.map { scoutNormalizedName(it.name) }.toSet() +
            profile.ratings.values.filter { it.rating == com.nuvio.tv.domain.model.ScoutRating.NOT_FOR_ME }.map { scoutNormalizedName(it.title.name) } +
            listed.names
        if (owner != profiles.scoutSelection()) return@withLock emptySet()
        seenNames = SeenNames(owner, profile, listed, now, names)
        names
    }

    suspend fun withoutSeen(items: List<MetaPreview>, applyWatchFilter: Boolean = true): List<MetaPreview> = withContext(Dispatchers.Default) {
        val started = System.currentTimeMillis()
        val owner = profiles.scoutSelection()
        val profile = store.profileOf(owner.profileId)
        if (owner != profiles.scoutSelection()) return@withContext emptyList()
        val listed = listed(owner)
        val names = namesToHide(owner, profile, listed)
        val watched = if (applyWatchFilter && !store.observeShowWatched(profile.profileId).first()) {
            movieHistory(profile.profileId, useLocalFirst = false).flowOn(Dispatchers.IO).first()
        } else null
        if (owner != profiles.scoutSelection()) return@withContext emptyList()
        val named = System.currentTimeMillis()
        items.filterNot { item ->
            val name = scoutNormalizedName(item.name)
            val year = item.releaseInfo?.take(4)?.toIntOrNull()
            item.id in listed.ids || name in names ||
                (item.apiType == "movie" && watched?.contains(item.id, item.name, item.releaseInfo, item.imdbId) == true) ||
                (year != null && (year - 1..year + 1).any { "$name|$it" in names })
        }.also {
            Log.i("ScoutSpeed", "seen names=${named - started} filter=${System.currentTimeMillis() - named} items=${items.size}")
        }
    }

    /** A rating, a watch or a list change: rows on Home read themselves again. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val changes: Flow<Unit> = flow {
        emitAll(
            combine(
                store.profile.map { profile -> profile.ratings.mapValues { it.value.rating } },
                watchedMovies,
                showWatched,
                library.get().libraryItems.map { items -> items.map { it.id to it.listKeys } }
            ) { ratings, watched, show, lists -> listOf(ratings, watched, show, lists) }
                .distinctUntilChanged()
                .drop(1)
                .debounce(CHANGE_SETTLE_MS)
                .map { }
        )
    }.flowOn(Dispatchers.Default)
}
