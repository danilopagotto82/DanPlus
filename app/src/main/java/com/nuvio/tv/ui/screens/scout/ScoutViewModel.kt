package com.nuvio.tv.ui.screens.scout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.nuvio.tv.core.build.AppFeaturePolicy
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.trailer.TrailerPlaybackSource
import com.nuvio.tv.data.trailer.TrailerService
import com.nuvio.tv.data.trailer.lookupTrailerPreview
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nuvio.tv.core.image.sizedHomeArtworkUrl
import com.nuvio.tv.core.party.PartyAvatars
import com.nuvio.tv.core.profile.LocalProfileAvatars
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.ratinglimit.RatingLimit
import com.nuvio.tv.core.ratinglimit.RatingLimitGate
import com.nuvio.tv.core.scout.ScoutSelection
import com.nuvio.tv.core.scout.scoutReadPolicy
import com.nuvio.tv.core.scout.scoutSelection
import com.nuvio.tv.core.scout.scoutSelections
import com.nuvio.tv.data.remote.supabase.AvatarRepository
import com.nuvio.tv.data.repository.MemberAccessRepository
import com.nuvio.tv.domain.model.CosmeticEntitlement
import com.nuvio.tv.core.scout.SCOUT_QUICK_EPISODE_LIMIT
import com.nuvio.tv.core.scout.ScoutAnswers
import com.nuvio.tv.core.scout.ScoutFolderSupport
import com.nuvio.tv.core.scout.ScoutAiStore
import com.nuvio.tv.core.scout.ScoutGroupMember
import com.nuvio.tv.core.scout.ScoutMood
import com.nuvio.tv.core.scout.ScoutPickDetail
import com.nuvio.tv.core.scout.ScoutPicksFinder
import com.nuvio.tv.core.scout.ScoutTime
import com.nuvio.tv.core.scout.ScoutTune
import com.nuvio.tv.core.scout.ScoutWatch
import com.nuvio.tv.core.scout.scoutOrderCinemaPicks
import com.nuvio.tv.core.scout.scoutOrderPicks
import com.nuvio.tv.core.scout.ask.AskFailure
import com.nuvio.tv.core.scout.ask.AskItem
import com.nuvio.tv.core.scout.ask.AskOutcome
import com.nuvio.tv.core.scout.ask.AskKeyVault
import com.nuvio.tv.core.scout.ask.AskRunner
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.core.sync.ProfileSyncService
import com.nuvio.tv.data.local.ProfileLockStateDataStore
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.data.local.WatchedSeriesStateHolder
import com.nuvio.tv.domain.model.ScoutPick
import com.nuvio.tv.domain.model.ScoutAdvancedSearch
import com.nuvio.tv.domain.model.ScoutDiscoveryHistory
import com.nuvio.tv.domain.model.scoutSurpriseSuggestions
import com.nuvio.tv.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class ScoutShownPick(val pick: ScoutPick, val detail: ScoutPickDetail? = null, val suitsEveryone: Boolean = false, val reason: String? = null)

internal data class ScoutWatchDisplay(
    val profileId: Int,
    val showWatched: Boolean = true,
    val loaded: Boolean = false,
    val movies: com.nuvio.tv.core.scout.ScoutWatchedMovies = com.nuvio.tv.core.scout.ScoutWatchedMovies(),
    val seriesIds: Set<Int> = emptySet()
) {
    fun isWatched(pick: ScoutPick): Boolean = if (pick.tv) pick.tmdbId in seriesIds
        else movies.contains("tmdb:${pick.tmdbId}", pick.name, pick.released)

    fun visible(picks: List<ScoutPick>): List<ScoutPick> =
        if (showWatched) picks else picks.filterNot(::isWatched)
}

internal fun scoutRecommendationLimit(active: RatingLimit?, group: List<RatingLimit>, family: Boolean): RatingLimit? =
    (listOfNotNull(active, RatingLimit.PG.takeIf { family }) + group).minByOrNull { it.highest }

internal data class ScoutAudience(
    val owner: ScoutSelection, val limit: RatingLimit?, val others: Set<Int>, val family: Boolean
)

internal fun ScoutScreenState.visibleTo(current: ScoutAudience?): ScoutScreenState =
    if (current != null && audience == current) this
    else copy(row = emptyList(), hero = null, heroLogo = null, everyone = emptySet(), audience = null)

internal class ScoutPinRequests {
    data class Request(val owner: ScoutSelection, val generation: Long)

    private var generation = 0L

    fun begin(owner: ScoutSelection): Request = Request(owner, ++generation)
    fun invalidate() { generation++ }
    fun accepts(request: Request, owner: ScoutSelection): Boolean =
        request.owner == owner && request.generation == generation
}

private fun scoutWatchedTmdbId(id: String): Int? =
    id.removePrefix("tmdb:").removePrefix("movie:").removePrefix("series:").removePrefix("tv:").toIntOrNull()

sealed interface ScoutTab {
    data object ForYou : ScoutTab
    data object Taste : ScoutTab
    data class Feeling(val mood: ScoutMood) : ScoutTab
}

internal data class ScoutScreenState(
    val answers: ScoutAnswers = ScoutAnswers(),
    val others: Set<Int> = emptySet(),
    val tab: ScoutTab = ScoutTab.ForYou,
    val surprise: Boolean = false,
    val advancedSearch: ScoutAdvancedSearch? = null,
    val moreLike: String? = null,
    val row: List<ScoutPick> = emptyList(),
    val audience: ScoutAudience? = null,
    /** Goes up each time the row shows different titles, so the page can start the new row at its first one. */
    val rowTurn: Int = 0,
    val everyone: Set<String> = emptySet(),
    val rowLoading: Boolean = true,
    val nothing: Boolean = false,
    val hero: ScoutShownPick? = null,
    val heroLogo: String? = null,
    val fromCountryOffered: Boolean = false,
    val pinFor: UserProfile? = null,
    val pinWrong: Boolean = false,
    val pinChecking: Boolean = false,
    val asking: Boolean = false,
    val askBusy: Boolean = false,
    val askFailure: AskFailure? = null
) {
    /** Unset answers, a surprise or "more like" draw from elsewhere, which is not a query a row could repeat. */
    val canSave: Boolean get() = !surprise && moreLike == null && !asking && !answers.isUnset
}

internal fun nextRowTurn(shown: List<ScoutPick>, next: List<ScoutPick>, turn: Int): Int =
    if (shown.map { it.key } == next.map { it.key }) turn else turn + 1

const val SCOUT_FOCUS_FOR_YOU = "tab_you"
const val SCOUT_FOCUS_THREE_MORE = "tab_more"
private const val MAX_EPISODE_CHECKS = 15
private const val ROW_SETTLE_MS = 150L
private const val KEPT_ROW_SETTLE_MS = 50L
private val NEAR_MOODS = listOf(1, -1, 2, -2)
private const val SPEED_TAG = "ScoutSpeed"
private const val HERO_SETTLE_MS = 250L
private const val ROW_SIZE = 12
private const val WARM_AFTER_MS = 800L
private const val WARM_AT_ONCE = 2
private const val WARM_TILES = 3
private const val WARM_IMAGE_WIDTH = 96
private const val WARM_IMAGE_HEIGHT = 54

/** TMDB's series length filter lets long shows through, so a short-episode answer is checked against the last episode. */
fun scoutEpisodeFits(detail: ScoutPickDetail?): Boolean = (detail?.minutes ?: 0) <= SCOUT_QUICK_EPISODE_LIMIT

@HiltViewModel
class ScoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val finder: ScoutPicksFinder,
    private val asker: AskRunner,
    private val folders: ScoutFolderSupport,
    private val profileManager: ProfileManager,
    private val locks: ProfileLockStateDataStore,
    private val sync: ProfileSyncService,
    private val store: ScoutStore,
    watchedSeries: WatchedSeriesStateHolder,
    localAvatars: LocalProfileAvatars,
    private val avatars: AvatarRepository,
    private val memberAccess: MemberAccessRepository,
    private val ratingLimits: RatingLimitGate,
    private val trailerService: TrailerService,
    private val tmdbService: TmdbService,
    private val tmdbSettings: TmdbSettingsDataStore,
    private val cinemaAi: ScoutAiStore,
    private val cinemaKeys: AskKeyVault
) : ViewModel() {
    private val _state = MutableStateFlow(ScoutScreenState())
    private var sessionOwner = profileOwner()

    internal val state: StateFlow<ScoutScreenState> =
        combine(_state, profileManager.scoutSelections(), ratingLimits.limits) { current, _, _ ->
            current.visibleTo(audienceFor(current))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ScoutScreenState())

    val savedRows: StateFlow<Set<String>> = store.profile.map { it.settings.savedSlices.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val profiles: StateFlow<List<UserProfile>> = profileManager.profiles
    val activeProfileId: StateFlow<Int> = profileManager.activeProfileId
    internal val discoverySelectionRevision = profileManager.profileSelectionRevision
    internal val discoveryRatingLimits = ratingLimits.limits

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    internal val watchedDisplay = profileManager.scoutSelections().flatMapLatest { owner ->
        val id = owner.profileId
        combine(store.observeShowWatched(id), folders.watchedMoviesForProfile(id), watchedSeries.observeCompletedIds(id)) { show, movies, series ->
            ScoutWatchDisplay(
                profileId = id,
                showWatched = show,
                loaded = true,
                movies = movies,
                seriesIds = series.mapNotNull(::scoutWatchedTmdbId).toSet()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScoutWatchDisplay(activeProfileId.value))

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val discoveryHistory = activeProfileId.flatMapLatest { id ->
        store.observeDiscovery(id).map { id to it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), activeProfileId.value to ScoutDiscoveryHistory())

    internal fun toggleShowWatched(display: ScoutWatchDisplay) {
        if (display.profileId != activeProfileId.value) return
        viewModelScope.launch { store.setShowWatched(display.profileId, !display.showWatched) }
    }

    private val avatarCatalog = flow {
        val full = memberAccess.access.value.entitlements.includes(CosmeticEntitlement.PROFILE_AVATARS)
        emit(emptyList())
        emit(runCatching { avatars.getAvatarCatalog(full) }.getOrDefault(emptyList()))
    }

    /** The same picture the side menu and Who's watching draw for each profile. */
    val profilePictures: StateFlow<Map<Int, String>> =
        combine(profileManager.profiles, localAvatars.avatars, avatarCatalog) { all, local, catalog ->
            all.mapNotNull { profile ->
                val picture = local[profile.id]?.let(PartyAvatars::imageUri)
                    ?: profile.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: profile.avatarId?.let { avatars.getAvatarImageUrl(it, catalog) }
                picture?.let { profile.id to it }
            }.toMap()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val country: String = Locale.getDefault().country

    /** Where focus was, so a trip to a title page comes back to the same spot. */
    var focusKey: String = SCOUT_FOCUS_FOR_YOU

    /** Where focus has to land after the tabs change, whatever it brushes past on the way. */
    var focusPending: String? = null

    private var job: Job? = null
    private var heroJob: Job? = null
    private var warmJob: Job? = null
    private var nearJob: Job? = null
    private val building = HashMap<Pair<ScoutAudience, String>, Deferred<List<ScoutPick>>>()
    private var warmedFor: String? = null
    private val shown = LinkedHashSet<String>()
    private val details = HashMap<String, ScoutPickDetail>()
    private val logos = HashMap<String, String?>()
    private val members = HashMap<Int, ScoutGroupMember>()
    private val unlocked = HashSet<Int>()
    private var unlockedFor: ScoutSelection? = null
    private val pinRequests = ScoutPinRequests()
    private var pinJob: Job? = null

    private fun profileOwner() = profileManager.scoutSelection()

    private fun audienceFor(s: ScoutScreenState): ScoutAudience? {
        val owner = profileOwner()
        if (owner != sessionOwner) return null
        val audience = scoutReadPolicy {
            val configured = ratingLimits.limitFor(owner.profileId)
            val group = s.others.mapNotNull(ratingLimits::limitFor)
            ScoutAudience(owner, scoutRecommendationLimit(configured, group, s.answers.family), s.others, s.answers.family)
        }
        return audience.takeIf { owner == profileOwner() }
    }

    private fun isCurrent(audience: ScoutAudience): Boolean = audience == audienceFor(_state.value)

    internal fun canOpenRecommendation(pick: ScoutPick): Boolean {
        val current = _state.value
        val audience = current.audience ?: return false
        return isCurrent(audience) && current.row.any { it.key == pick.key }
    }

    init {
        viewModelScope.launch {
            profileManager.scoutSelections().collectLatest { owner ->
                if (owner != sessionOwner) {
                    sessionOwner = owner
                    listOfNotNull(job, heroJob, warmJob, nearJob).forEach { it.cancel() }
                    building.values.toList().forEach { it.cancel() }
                    building.clear()
                    cancelPinRequest()
                    unlocked.clear()
                    unlockedFor = null
                    members.clear()
                    shown.clear()
                    warmedFor = null
                    _state.value = ScoutScreenState(fromCountryOffered = _state.value.fromCountryOffered)
                }
                val saved = finder.pool() != null
                if (!saved) finder.refreshPool()
                if (owner == profileOwner()) loadRow(0)
                if (saved) finder.refreshPool()
            }
        }
        viewModelScope.launch {
            ratingLimits.limits.collect {
                members.clear()
                if (_state.value.audience != audienceFor(_state.value)) {
                    _state.update { it.visibleTo(audienceFor(it)) }
                    if (_state.value.tab != ScoutTab.Taste) loadRow(0)
                }
            }
        }
        viewModelScope.launch { _state.update { it.copy(fromCountryOffered = finder.fromCountryOffered(country)) } }
    }

    /** Resting on a tab shows it, the way Seasons work on a title's page. */
    fun focusTab(tab: ScoutTab) {
        val now = _state.value
        if (now.tab == tab && now.moreLike == null) return
        _state.update {
            it.copy(tab = tab, moreLike = null, surprise = false, advancedSearch = null, asking = false, askBusy = false, askFailure = null, answers = it.answers.copy(mood = (tab as? ScoutTab.Feeling)?.mood, tunes = emptySet()))
        }
        if (tab != ScoutTab.Taste) {
            loadRow()
            warmNear()
        }
    }

    fun pressWatch(watch: ScoutWatch) = answer { it.pressWatch(watch) }
    fun pressTime(time: ScoutTime) = answer { it.pressTime(time) }
    fun pressFamily() = answer { it.pressFamily(!it.family) }

    private fun answer(change: (ScoutAnswers) -> ScoutAnswers) {
        _state.update { s ->
            val next = change(s.answers)
            s.copy(answers = next, moreLike = null, tab = if (next.mood == null && s.tab is ScoutTab.Feeling) ScoutTab.ForYou else s.tab)
        }
        if (_state.value.tab != ScoutTab.Taste) loadRow()
    }

    /** The tile in focus takes over the page once the remote has settled on it. */
    fun focusPick(pick: ScoutPick) {
        val audience = _state.value.audience ?: return
        if (!canOpenRecommendation(pick) || _state.value.hero?.pick?.key == pick.key) return
        heroJob?.cancel()
        heroJob = viewModelScope.launch {
            delay(HERO_SETTLE_MS)
            showHero(pick, audience)
        }
    }

    private suspend fun showHero(pick: ScoutPick, audience: ScoutAudience) {
        if (_state.value.audience != audience || !canOpenRecommendation(pick)) return
        val key = pick.key
        val withRating = _state.value.answers.family
        _state.update {
            it.copy(hero = ScoutShownPick(pick, details[key], key in it.everyone, pick.aiReason.takeIf { _ -> it.asking }), heroLogo = logos[key])
        }
        coroutineScope {
            if (key !in details) launch {
                finder.detail(pick, withRating)?.let { found ->
                    if (!isCurrent(audience)) return@let
                    details[key] = found
                    _state.update { s -> if (s.hero?.pick?.key == key) s.copy(hero = s.hero?.copy(detail = found)) else s }
                }
            }
            if (key !in logos) launch {
                val found = finder.logo(pick)
                if (!isCurrent(audience)) return@launch
                logos[key] = found
                _state.update { s -> if (s.hero?.pick?.key == key) s.copy(heroLogo = found) else s }
            }
        }
    }

    private fun rowKey(s: ScoutScreenState) = s.answers.encode() + "|" + s.others.sorted().joinToString(",") + "|" + s.surprise

    private fun loadRow(settle: Long = ROW_SETTLE_MS) {
        job?.cancel()
        val audience = audienceFor(_state.value)
        _state.update {
            it.visibleTo(audience).copy(
                rowLoading = audience != null, nothing = audience == null, asking = false, askBusy = false, askFailure = null
            )
        }
        if (audience == null) return
        job = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            val landed = _state.value
            val keptRow = keptFor(landed, audience)
            val kept = keptRow != null
            delay(if (kept) minOf(settle, KEPT_ROW_SETTLE_MS) else settle)
            val asked = _state.value
            if (!isCurrent(audience)) return@launch
            val picks = keptRow?.takeIf { rowKey(asked) == rowKey(landed) } ?: rowFor(asked, audience)
            if (!showRow(picks, audience)) return@launch
            if (asked.surprise && picks.isNotEmpty() && isCurrent(audience)) {
                store.updateDiscovery(audience.owner.profileId) { if (isCurrent(audience)) it.record(picks) else it }
            }
            Log.i(SPEED_TAG, "row ${asked.answers.mood?.code ?: "for_you"} kept=$kept ${SystemClock.elapsedRealtime() - started} ms")
        }
    }

    private fun cacheKey(s: ScoutScreenState, audience: ScoutAudience) = rowKey(s) + "|" + audience.limit?.code

    private suspend fun keptFor(s: ScoutScreenState, audience: ScoutAudience): List<ScoutPick>? =
        if (!isCurrent(audience) || s.surprise || s.advancedSearch != null || shown.isNotEmpty()) null
        else finder.keptRow(cacheKey(s, audience), audience.owner)?.let { restrictRecommendations(it, audience) }

    /** A row already being fetched is waited for, so landing on it never starts the same fetch twice. */
    private suspend fun rowFor(s: ScoutScreenState, audience: ScoutAudience): List<ScoutPick> {
        if (!isCurrent(audience)) return emptyList()
        keptFor(s, audience)?.let { return it }
        if (s.surprise || s.advancedSearch != null || shown.isNotEmpty()) return buildRow(s, audience)
        val key = cacheKey(s, audience)
        val taskKey = audience to key
        val fetch = building.getOrPut(taskKey) {
            viewModelScope.async {
                val operation = currentCoroutineContext()
                buildRow(s, audience).also {
                    operation.ensureActive()
                    if (it.isNotEmpty()) finder.keepRow(key, it, audience.owner) { isCurrent(audience) }
                }
            }.also { started -> started.invokeOnCompletion { building.remove(taskKey, started) } }
        }
        return fetch.await().takeIf { isCurrent(audience) }.orEmpty()
    }

    /** The moods either side of where the remote is, ahead of everything else. */
    private fun warmNear() {
        val s = _state.value
        val quick = s.answers.time == ScoutTime.QUICK_EPISODES || s.answers.time == ScoutTime.SHORT
        if (s.surprise || s.advancedSearch != null || s.moreLike != null || s.asking || quick) return
        val moods = listOf<ScoutMood?>(null) + ScoutMood.choicesFor(s.answers.watch, s.answers.family)
        val at = moods.indexOf((s.tab as? ScoutTab.Feeling)?.mood).coerceAtLeast(0)
        nearJob?.cancel()
        nearJob = viewModelScope.launch {
            NEAR_MOODS.mapNotNull { (at + it).takeIf(moods.indices::contains) }.forEach { index ->
                launch { warm(s.copy(answers = s.answers.copy(mood = moods[index], tunes = emptySet()))) }
            }
        }
    }

    /** Once a row is up, the neighbouring moods are fetched quietly, so resting on one shows it at once. */
    private fun warmMoods() {
        val s = _state.value
        val quick = s.answers.time == ScoutTime.QUICK_EPISODES || s.answers.time == ScoutTime.SHORT
        if (s.surprise || s.advancedSearch != null || s.moreLike != null || s.asking || quick) return
        val asked = s.answers.copy(mood = null, tunes = emptySet()).encode() + s.others.sorted()
        if (asked == warmedFor) return
        warmedFor = asked
        warmJob?.cancel()
        warmJob = viewModelScope.launch {
            delay(WARM_AFTER_MS)
            val gate = Semaphore(WARM_AT_ONCE)
            val moods = listOf<ScoutMood?>(null) + ScoutMood.choicesFor(s.answers.watch, s.answers.family)
            val from = moods.indexOf((s.tab as? ScoutTab.Feeling)?.mood).coerceAtLeast(0)
            coroutineScope {
                moods.indices.sortedBy { abs(it - from) }.forEach { index ->
                    launch {
                        gate.withPermit { warm(s.copy(answers = s.answers.copy(mood = moods[index], tunes = emptySet()))) }
                    }
                }
            }
            val audience = audienceFor(s) ?: return@launch
            finder.saveKept { isCurrent(audience) }
        }
    }

    private suspend fun warm(s: ScoutScreenState) {
        val audience = audienceFor(s) ?: return
        val picks = rowFor(s, audience)
        val top = picks.firstOrNull()?.takeIf { isCurrent(audience) } ?: return
        coroutineScope {
            launch { finder.detail(top, s.answers.family)?.let { if (isCurrent(audience)) details[top.key] = it } }
            launch { finder.logo(top).let { if (isCurrent(audience)) logos[top.key] = it } }
            launch { saveImage(top.backdrop?.let { sizedHomeArtworkUrl(it, SCOUT_BACKDROP_WIDTH) }) }
            picks.take(WARM_TILES).forEach { pick ->
                launch { saveImage((pick.backdrop ?: pick.poster)?.let { sizedHomeArtworkUrl(it, SCOUT_TILE_IMAGE_WIDTH) }) }
            }
        }
    }

    /** To disk only: nothing here takes up picture memory until the page shows it. */
    private suspend fun saveImage(url: String?) {
        if (url == null) return
        context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(url)
                .size(WARM_IMAGE_WIDTH, WARM_IMAGE_HEIGHT)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        )
    }

    private suspend fun showRow(picks: List<ScoutPick>, audience: ScoutAudience): Boolean {
        if (!isCurrent(audience)) return false
        val others = group(audience)
        val ownPool = finder.pool()?.picked.orEmpty().map { it.key }.toSet()
        val everyone = if (others.isEmpty()) emptySet() else picks.filter { pick ->
            pick.key in ownPool && others.all { member -> member.pool?.picked.orEmpty().any { it.key == pick.key } }
        }.map { it.key }.toSet()
        currentCoroutineContext().ensureActive()
        if (!isCurrent(audience)) return false
        _state.update {
            it.copy(
                row = picks,
                audience = audience,
                rowTurn = nextRowTurn(it.row, picks, it.rowTurn),
                everyone = everyone,
                rowLoading = false,
                nothing = picks.isEmpty(),
                hero = it.hero?.takeIf { previous -> picks.any { it.key == previous.pick.key } },
                heroLogo = null
            )
        }
        warmMoods()
        picks.firstOrNull()?.let { showHero(it, audience) }
        return isCurrent(audience)
    }

    private suspend fun buildRow(s: ScoutScreenState, audience: ScoutAudience): List<ScoutPick> {
        val answers = s.answers
        if (s.surprise) {
            val profileId = audience.owner.profileId
            val display = watchedDisplay.first { it.profileId == profileId && it.loaded }
            if (!isCurrent(audience)) return emptyList()
            val recent = store.observeDiscovery(profileId).first().suggestions
            val currentKeys = s.row.map(ScoutPick::key).toSet()
            var candidates = if (answers.isUnset) display.visible(order(finder.surprisePool(), audience)) else emptyList()
            val prior = recent.map(ScoutPick::key).toSet() + currentKeys
            for (page in 1..2) {
                if (candidates.count { it.key !in prior } >= ROW_SIZE) break
                val pagePicks = display.visible(order(finder.fetch(answers, page, relaxed = false), audience))
                candidates = (candidates + pagePicks).distinctBy(ScoutPick::key)
            }
            return scoutSurpriseSuggestions(candidates, recent, currentKeys, ROW_SIZE)
        }
        s.advancedSearch?.let { query ->
            var candidates = emptyList<ScoutPick>()
            for (page in 1..3) {
                candidates = (candidates + order(finder.fetchAdvanced(query, answers, page), audience)).distinctBy(ScoutPick::key)
                if (candidates.size >= ROW_SIZE) break
            }
            return candidates.take(ROW_SIZE)
        }
        var fresh = order(
            when {
                answers.isUnset -> finder.pool()?.picked.orEmpty()
                else -> emptyList()
            },
            audience
        )
        if (fresh.size < ROW_SIZE) fresh = (fresh + order(finder.fetch(answers, 1, relaxed = false), audience)).distinctBy { it.key }
        if (fresh.size < ROW_SIZE / 2) fresh = (fresh + order(finder.fetch(answers, 1, relaxed = true), audience)).distinctBy { it.key }
        if (answers.time != ScoutTime.QUICK_EPISODES && answers.time != ScoutTime.SHORT) return fresh.take(ROW_SIZE)
        val kept = ArrayList<ScoutPick>()
        var checks = 0
        for (pick in fresh) {
            if (kept.size == ROW_SIZE) break
            if (pick.tv && checks < MAX_EPISODE_CHECKS) {
                checks++
                val detail = details[pick.key] ?: finder.detail(pick, answers.family)?.also { details[pick.key] = it }
                if (!scoutEpisodeFits(detail)) continue
            }
            kept += pick
        }
        return kept
    }

    /** A profile with a PIN joins only once the PIN is right, so its taste cannot be read by anyone else on the box. */
    fun pressProfile(profile: UserProfile) {
        val owner = profileOwner()
        if (owner != sessionOwner || owner.profileId == profile.id) return
        if (unlockedFor != owner) {
            unlockedFor = owner
            unlocked.clear()
        }
        cancelPinRequest()
        val request = pinRequests.begin(owner)
        _state.update { it.copy(pinFor = null, pinChecking = false, pinWrong = false) }
        val current = _state.value.others
        when {
            profile.id in current -> {
                _state.update { it.copy(others = current - profile.id) }
                othersChanged()
            }
            profile.id !in unlocked -> {
                pinJob = viewModelScope.launch {
                    val requiresPin = scoutReadPolicy { locks.pinEnabled.first()[profile.id] == true } ?: true
                    if (!pinRequests.accepts(request, profileOwner())) return@launch
                    if (requiresPin) _state.update { it.copy(pinFor = profile, pinWrong = false) }
                    else {
                        _state.update { it.copy(others = it.others + profile.id) }
                        othersChanged()
                    }
                }
            }
            else -> {
                _state.update { it.copy(others = current + profile.id) }
                othersChanged()
            }
        }
    }

    fun submitPin(pin: String) {
        val profile = _state.value.pinFor ?: return
        if (_state.value.pinChecking || unlockedFor != profileOwner()) return
        val request = pinRequests.begin(profileOwner())
        _state.update { it.copy(pinChecking = true, pinWrong = false) }
        pinJob = viewModelScope.launch {
            val ok = scoutReadPolicy { sync.verifyProfilePin(profile.id, pin).getOrNull()?.unlocked == true } ?: false
            if (!pinRequests.accepts(request, profileOwner()) || _state.value.pinFor != profile) return@launch
            if (ok) {
                pinRequests.invalidate()
                unlocked += profile.id
                _state.update { it.copy(pinFor = null, pinChecking = false, others = it.others + profile.id) }
                othersChanged()
            } else {
                _state.update { it.copy(pinChecking = false, pinWrong = true) }
            }
        }
    }

    private fun cancelPinRequest() {
        pinRequests.invalidate()
        pinJob?.cancel()
        pinJob = null
    }

    fun dismissPin() {
        cancelPinRequest()
        _state.update { it.copy(pinFor = null, pinChecking = false, pinWrong = false) }
    }

    fun clearPinError() = _state.update { it.copy(pinWrong = false) }

    private fun othersChanged() {
        if (_state.value.tab != ScoutTab.Taste) loadRow()
    }

    fun surprise() {
        focusKey = SCOUT_FOCUS_THREE_MORE
        focusPending = SCOUT_FOCUS_THREE_MORE
        _state.update { it.copy(answers = ScoutAnswers(family = it.answers.family), surprise = true, advancedSearch = null, moreLike = null, tab = ScoutTab.ForYou) }
        shown.clear()
        loadRow(0)
    }

    fun pressTune(tune: ScoutTune) {
        _state.update { s ->
            val next = s.answers.pressTune(tune)
            s.copy(answers = next, surprise = s.surprise, moreLike = null)
        }
        shown.clear()
        loadRow(0)
    }

    fun threeMore() {
        if (!_state.value.surprise) shown += _state.value.row.map { it.key }
        loadRow(0)
    }

    internal suspend fun cinemaGenre(audience: ScoutAudience): Int? =
        if (isCurrent(audience)) store.observeCinemaGenre(audience.owner.profileId).first().takeIf { isCurrent(audience) }
        else null

    internal suspend fun saveCinemaGenre(audience: ScoutAudience, genre: Int?) {
        if (!isCurrent(audience)) return
        if (genre != null && genre !in setOf(28, 35, 18)) return
        store.saveCinemaGenre(audience.owner.profileId, genre) { isCurrent(audience) }
    }

    internal suspend fun cinemaChoice(
        audience: ScoutAudience,
        mood: ScoutMood?,
        maxMinutes: Int?,
        rejected: Set<String>,
        configuredSession: Boolean,
        allowRewatch: Boolean,
        guest: ScoutCinemaGuest?
    ): ScoutCinemaChoice? {
        val snapshot = _state.value
        if (!isCurrent(audience)) return null
        val answers = snapshot.answers.copy(
            watch = if (snapshot.answers.watch == ScoutWatch.ANYTHING) ScoutWatch.MOVIE else snapshot.answers.watch,
            mood = if (configuredSession) mood else snapshot.answers.mood,
            time = if (configuredSession) ScoutTime.ANY else snapshot.answers.time,
            family = snapshot.answers.family || guest?.familyLimit == true
        )
        val filters = answers.slice().movie ?: return null
        val today = java.time.LocalDate.now()
        val display = watchedDisplay.first { it.profileId == audience.owner.profileId && it.loaded }
        if (!isCurrent(audience)) return null

        val exclusions = (if (allowRewatch) finder.rewatchExclusions(audience.owner)
            else finder.exclusions(audience.owner)) ?: return null
        val others = if (allowRewatch) audience.others.map { id ->
            if (!isCurrent(audience)) return null
            finder.rewatchMember(id)
        } else group(audience)
        if (others.size != audience.others.size || !isCurrent(audience)) return null
        val pool = finder.pool()
        val profileGenre = cinemaGenre(audience)
        val cachedAi = if (guest == null && audience.others.isEmpty()) {
            withContext(Dispatchers.IO) {
                val ownerId = audience.owner.profileId
                if (cinemaKeys.current(ownerId).activeConfig == null) emptyList()
                else cinemaAi.read(ownerId).takeIf { it.sortOn }?.sorted.orEmpty().map { it.key }
            }
        } else emptyList()
        for (page in 1..3) {
            val query = snapshot.advancedSearch
            val fetched = if (query == null) finder.fetch(answers, page, relaxed = false)
                else finder.fetchAdvanced(query, answers, page)
            val candidates = (if (allowRewatch) fetched else display.visible(fetched)).filter { pick ->
                cinemaCandidateFits(pick, today, rejected)
            }
            val allowed = restrictRecommendations(candidates.filterNot { pick ->
                exclusions.blocks(pick) || others.any { it.exclusions.blocks(pick) }
            }, audience)
            val guestSafe = if (guest?.familyLimit == true) {
                ratingLimits.keepFitting(RatingLimit.PG, allowed) { "tmdb:${it.tmdbId}" to "movie" }
            } else allowed
            val preferredGenre = guest?.genre ?: profileGenre
            val ordered = withContext(Dispatchers.Default) {
                val ranked = scoutOrderCinemaPicks(guestSafe, pool, exclusions, others)
                if (preferredGenre == null) ranked else ranked.sortedBy { preferredGenre !in it.genres }
            }
            val reviewed = linkedMapOf<ScoutPick, ScoutPickDetail>()
            val shortlistSize = if (cachedAi.any { key -> ordered.take(3).any { it.key == key } }) 3 else 1
            for (pick in ordered) {
                val detail = finder.detail(pick, withRating = audience.limit != null || guest?.familyLimit == true) ?: continue
                if (!cinemaRuntimeFits(detail.minutes, filters.withRuntimeGte, filters.withRuntimeLte, maxMinutes)) continue
                reviewed[pick] = detail
                if (reviewed.size == shortlistSize) break
            }
            val pick = cinemaRefineCached(reviewed.keys.toList(), cachedAi).firstOrNull() ?: continue
            val logo = finder.logo(pick)
            if (!isCurrent(audience) || _state.value.answers != snapshot.answers ||
                _state.value.advancedSearch != snapshot.advancedSearch) return null
            return ScoutCinemaChoice(ScoutShownPick(pick, reviewed.getValue(pick)), logo)
        }
        return null
    }

    internal fun cinemaAudienceIsCurrent(audience: ScoutAudience): Boolean = isCurrent(audience)

    internal suspend fun cinemaTrailer(audience: ScoutAudience, pick: ScoutPick): ScoutCinemaTrailer? {
        if (!isCurrent(audience) || !tmdbSettings.settings.first().useTrailers) return null
        val source = if (AppFeaturePolicy.inAppTrailerPlaybackEnabled) {
            lookupTrailerPreview(
                trailerService, tmdbService, "tmdb:${pick.tmdbId}", "movie", pick.name,
                pick.released?.take(4), imdbId = null, ytIds = emptyList()
            ).source?.let { ScoutCinemaTrailer(it, external = false) }
        } else if (AppFeaturePolicy.externalTrailerPlaybackEnabled) {
            trailerService.getExternalTrailerUrl(pick.tmdbId.toString(), "movie", preferBrazilian = true)
                ?.let { ScoutCinemaTrailer(TrailerPlaybackSource(it), external = true) }
        } else null
        return source.takeIf { isCurrent(audience) }
    }

    internal fun discoveryAudience(): ScoutAudience? = audienceFor(_state.value)
        ?.takeIf { _state.value.pinFor == null }

    internal suspend fun readDiscovery(audience: ScoutAudience): ScoutDiscoveryHistory {
        if (discoveryAudience() != audience) return ScoutDiscoveryHistory()
        val history = store.observeDiscovery(audience.owner.profileId).first()
        val visible = restrictRecommendations(history.suggestions, audience)
        return if (discoveryAudience() == audience) history.copy(suggestions = visible) else ScoutDiscoveryHistory()
    }

    internal suspend fun discoveryKeywords(audience: ScoutAudience, text: String): List<Pair<Int, String>> {
        if (discoveryAudience() != audience) return emptyList()
        return finder.searchKeywords(text).takeIf { discoveryAudience() == audience }.orEmpty()
    }

    internal fun applyAdvancedSearch(query: ScoutAdvancedSearch, audience: ScoutAudience) {
        if (discoveryAudience() != audience) return
        shown.clear()
        _state.update {
            it.copy(
                advancedSearch = query.normalized(), surprise = false, moreLike = null, tab = ScoutTab.ForYou,
                answers = it.answers.copy(mood = null, tunes = emptySet())
            )
        }
        loadRow(0)
    }

    internal fun saveAdvancedSearch(query: ScoutAdvancedSearch, audience: ScoutAudience) {
        viewModelScope.launch {
            store.updateDiscovery(audience.owner.profileId) {
                if (discoveryAudience() == audience) it.save(query) else it
            }
        }
    }

    internal fun forgetAdvancedSearch(query: ScoutAdvancedSearch, audience: ScoutAudience) {
        viewModelScope.launch {
            store.updateDiscovery(audience.owner.profileId) {
                if (discoveryAudience() == audience) it.forget(query) else it
            }
        }
    }

    fun moreLike(pick: ScoutPick) {
        if (!canOpenRecommendation(pick)) return
        val audience = audienceFor(_state.value) ?: return
        job?.cancel()
        _state.update { it.copy(moreLike = pick.name, rowLoading = true, nothing = false, asking = false, askBusy = false, askFailure = null) }
        job = viewModelScope.launch { showRow(order(finder.moreLike(pick), audience).take(ROW_SIZE), audience) }
    }

    /** "Or just ask": the input takes the mood line's place and the page stays as it is until there is an answer. */
    fun openAsk() {
        job?.cancel()
        focusPending = null
        _state.update { it.copy(asking = true, askBusy = false, askFailure = null, rowLoading = false) }
    }

    fun ask(question: String) {
        val audience = audienceFor(_state.value) ?: return
        job?.cancel()
        heroJob?.cancel()
        _state.update { it.copy(askBusy = true, askFailure = null, row = emptyList(), rowLoading = true, nothing = false) }
        job = viewModelScope.launch {
            val outcome = asker.ask(question)
            if (!isCurrent(audience)) return@launch
            when (outcome) {
                is AskOutcome.Answer -> {
                    _state.update { it.copy(askBusy = false) }
                    showRow(order(outcome.items.ifEmpty { outcome.held }.mapNotNull(::askedPick), audience), audience)
                }
                is AskOutcome.Failed -> _state.update { it.copy(askBusy = false, askFailure = outcome.failure, rowLoading = false) }
            }
        }
    }

    private fun askedPick(item: AskItem): ScoutPick? {
        val id = item.meta.id.removePrefix("tmdb:").toIntOrNull() ?: return null
        return ScoutPick(
            tmdbId = id,
            tv = item.meta.type == ContentType.SERIES,
            name = item.meta.name,
            poster = item.meta.poster,
            backdrop = item.meta.background,
            overview = item.meta.description,
            released = item.meta.released,
            rating = item.meta.imdbRating,
            aiReason = item.reason
        )
    }

    fun leaveTaste() {
        focusKey = SCOUT_FOCUS_FOR_YOU
        focusPending = SCOUT_FOCUS_FOR_YOU
        focusTab(ScoutTab.ForYou)
    }

    /** Back from a surprise or "more like this" to the moods. */
    fun backToMoods() {
        focusKey = SCOUT_FOCUS_FOR_YOU
        focusPending = SCOUT_FOCUS_FOR_YOU
        shown.clear()
        _state.update { it.copy(surprise = false, advancedSearch = null, moreLike = null, tab = ScoutTab.ForYou, answers = ScoutAnswers(family = it.answers.family)) }
        loadRow(0)
    }

    fun toggleSaved() {
        val id = _state.value.answers.encode()
        viewModelScope.launch { folders.toggleSaved(id) }
    }

    private suspend fun group(audience: ScoutAudience): List<ScoutGroupMember> =
        audience.others.mapNotNull { id ->
            if (!isCurrent(audience)) null
            else members[id] ?: finder.member(id).takeIf { isCurrent(audience) }?.also { members[id] = it }
        }

    private suspend fun order(list: List<ScoutPick>, audience: ScoutAudience): List<ScoutPick> {
        if (!isCurrent(audience)) return emptyList()
        val exclusions = finder.exclusions(audience.owner) ?: return emptyList()
        val others = group(audience)
        val ordered = withContext(Dispatchers.Default) { scoutOrderPicks(list, finder.pool(), exclusions, System.nanoTime(), others) }
        return restrictRecommendations(ordered.filterNot { it.key in shown }, audience)
    }

    private suspend fun restrictRecommendations(picks: List<ScoutPick>, audience: ScoutAudience): List<ScoutPick> {
        if (!isCurrent(audience) || picks.isEmpty()) return emptyList()
        val limit = audience.limit ?: return picks
        val allowed = scoutReadPolicy {
            ratingLimits.keepFitting(limit, picks) { "tmdb:${it.tmdbId}" to if (it.tv) "series" else "movie" }
        }.orEmpty()
        return allowed.takeIf { isCurrent(audience) }.orEmpty()
    }
}
