package com.nuvio.tv.core.scout

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.ratinglimit.RatingLimit
import com.nuvio.tv.core.ratinglimit.RatingLimitGate
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.core.util.ArcDispatchers
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.local.WatchedItemsPreferences
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbDiscoverResult
import com.nuvio.tv.data.remote.api.TmdbRecommendationResult
import com.nuvio.tv.domain.model.SCOUT_MIN_SEEDS
import com.nuvio.tv.domain.model.SCOUT_POOL_VERSION
import com.nuvio.tv.domain.model.ScoutBecause
import com.nuvio.tv.domain.model.ScoutExclusions
import com.nuvio.tv.domain.model.ScoutPick
import com.nuvio.tv.domain.model.ScoutPool
import com.nuvio.tv.domain.model.ScoutProfile
import com.nuvio.tv.domain.model.ScoutSeed
import com.nuvio.tv.domain.model.scoutExclusionName
import com.nuvio.tv.domain.model.scoutHistorySignature
import com.nuvio.tv.domain.model.scoutNormalizedName
import com.nuvio.tv.domain.model.scoutRank
import com.nuvio.tv.domain.model.scoutSeeds
import com.nuvio.tv.domain.model.scoutShouldRefresh
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScoutEngine"
private const val MAX_CONCURRENT_REQUESTS = 2
private const val BECAUSE_ROWS = 2
private const val BECAUSE_ITEMS = 30
private const val GEM_ITEMS = 60
private const val TOP_UP_BELOW = 40
private const val TOP_UP_WEIGHT = 0.02
private const val GEM_MIN_VOTES = 150
private const val GEM_MIN_AVERAGE = 7.2
private const val POPULAR_MIN_VOTES = 500
private const val KIDS_TV_MIN_VOTES = 50
private const val KIDS_TOP_UP_PAGES = 4
private const val TASTE_ROW_ITEMS = 30
private const val CERTIFICATION_COUNTRY = "US"
private const val TV_LEFT_OUT = "10763,10764,10766,10767"
private const val KIDS_MOVIE_LEFT_OUT = "27,53,80,10752"
private const val KIDS_TV_LEFT_OUT = "$TV_LEFT_OUT,80,10768"
private const val MEMBER_POOL_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
private const val CHECK_EVERY_MS = 60L * 60 * 1000
private const val RATING_SETTLE_MS = 3_000L
private const val LIMIT_MARK = "|limit="

/** A profile's rating limit is part of what its picks were built for; profiles without one keep their old signature. */
internal fun scoutLimitSignature(history: String, limitCode: String): String =
    if (limitCode.isEmpty()) history else history + LIMIT_MARK + limitCode

internal fun scoutLimitChanged(pool: ScoutPool?, limitCode: String): Boolean =
    pool != null && pool.historySignature.substringAfterLast(LIMIT_MARK, "") != limitCode

internal data class ScoutSelection(val profileId: Int, val revision: Long)

internal fun ProfileManager.scoutSelection(): ScoutSelection =
    ScoutSelection(activeProfileId.value, profileSelectionRevision.value)

internal fun ProfileManager.scoutSelections() =
    combine(activeProfileId, profileSelectionRevision, ::ScoutSelection).distinctUntilChanged()

internal data class ScoutPoolScope(val selection: ScoutSelection, val limit: RatingLimit?)
internal data class OwnedScoutPool(val scope: ScoutPoolScope, val pool: ScoutPool?)

internal inline fun <T> scoutReadPolicy(read: () -> T): T? = try {
    read()
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    null
}

/** Builds the picks on this box, about once a day, from what the profile watched and rated. */
@Singleton
class ScoutEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tmdbApi: TmdbApi,
    private val tmdbService: TmdbService,
    private val tmdbSettings: TmdbSettingsDataStore,
    private val watchedItems: WatchedItemsPreferences,
    private val progressOf: dagger.Lazy<com.nuvio.tv.data.local.WatchProgressPreferences>,
    private val store: ScoutStore,
    private val profileManager: ProfileManager,
    private val folderSupport: ScoutFolderSupport,
    private val picksBuilder: ScoutPicksBuilder,
    private val moodCheck: dagger.Lazy<ScoutMoodCheck>,
    private val ratingLimits: dagger.Lazy<com.nuvio.tv.core.ratinglimit.RatingLimitGate>
) {
    private val adapter = Moshi.Builder().build().adapter(ScoutPool::class.java)
    private val refreshLock = Mutex()
    private val _pool = MutableStateFlow<OwnedScoutPool?>(null)
    private val filesLock = Any()
    @Volatile private var fileRevision = 0L

    internal fun selection(): ScoutSelection = profileManager.scoutSelection()
    fun activeProfile(): Int = selection().profileId

    internal fun scopeFor(owner: ScoutSelection = selection()): ScoutPoolScope? {
        if (owner != selection()) return null
        val scope = scoutReadPolicy { ScoutPoolScope(owner, ratingLimits.get().limitFor(owner.profileId)) }
        return scope.takeIf { owner == selection() }
    }

    internal fun activePool(owner: ScoutSelection = selection()): ScoutPool? =
        _pool.value?.takeIf { it.scope == scopeFor(owner) }?.pool

    internal val pool: Flow<OwnedScoutPool?> = flow {
        val gate = scoutReadPolicy { ratingLimits.get() }
        if (gate == null) emit(null)
        else emitAll(combine(_pool, profileManager.scoutSelections(), gate.limits) { stored, owner, _ ->
            stored?.takeIf { it.pool != null && it.scope == scopeFor(owner) }
        }.distinctUntilChanged())
    }

    private fun file(profileId: Int) = File(context.filesDir, "scout/pool_$profileId.json")

    private fun readPool(profileId: Int, limit: RatingLimit?): ScoutPool? =
        runCatching { adapter.fromJson(file(profileId).readText()) }.getOrNull()
            ?.takeIf { it.version == SCOUT_POOL_VERSION && !scoutLimitChanged(it, limit?.code.orEmpty()) }

    /** Shows the active profile's picks on every switch and keeps them no older than a day while the app stays open. */
    suspend fun followProfiles(checkEveryMs: Long = CHECK_EVERY_MS) {
        profileManager.scoutSelections().collectLatest { owner ->
            coroutineScope {
                launch {
                    store.profile.filter { it.profileId == owner.profileId }
                        .map { profile -> profile.ratings.values.sumOf { it.ratedAt } }.distinctUntilChanged().drop(1).collectLatest {
                            delay(RATING_SETTLE_MS)
                            refresh(owner, System.currentTimeMillis())
                        }
                }
                scoutReadPolicy { ratingLimits.get() }?.let { gate ->
                    scoutReadPolicy { gate.limitFor(owner.profileId) }
                    launch {
                        gate.limits.map { it[owner.profileId] }.distinctUntilChanged().drop(1).collectLatest {
                            refresh(owner, System.currentTimeMillis())
                        }
                    }
                }
                while (true) {
                    refresh(owner, System.currentTimeMillis())
                    if (owner == selection()) moodCheck.get().runIfDue()
                    delay(checkEveryMs)
                }
            }
        }
    }

    /** Drops what is held for a removed profile, or for every profile when [profileId] is null. */
    fun forget(profileId: Int?) = synchronized(filesLock) {
        fileRevision++
        if (profileId == null || _pool.value?.scope?.selection?.profileId == profileId) _pool.value = null
    }

    suspend fun refreshIfDue(now: Long = System.currentTimeMillis()) = refresh(selection(), now)

    private suspend fun refresh(owner: ScoutSelection, now: Long) = refreshLock.withLock {
        withContext(ArcDispatchers.background) {
            val scope = scopeFor(owner) ?: return@withContext
            val revision = fileRevision
            val operation = currentCoroutineContext()
            val isCurrent = { operation.isActive && fileRevision == revision && scopeFor(owner) == scope }
            val profileId = owner.profileId
            try {
                if (_pool.value?.scope != scope) {
                    val saved = readPool(profileId, scope.limit)
                    synchronized(filesLock) {
                        if (isCurrent()) _pool.value = OwnedScoutPool(scope, saved)
                    }
                }
                if (!isCurrent()) return@withContext
                val profile = store.profileOf(profileId)
                if (profile.profileId != profileId || !isCurrent()) return@withContext
                val watched = watchedItems.getAllItems(profileId)
                if (!isCurrent()) return@withContext
                val started = progressOf.get().observeAllProgress(profileId).first()
                if (!isCurrent()) return@withContext
                val signals = scoutSignals(watched, started, profile.ratings.values, now)
                val watching = scoutWatchingSeeds(started, signals)
                val seeds = scoutSignalSeeds(signals, watching)
                val signature = scoutLimitSignature(
                    scoutHistorySignature(seeds, profile.settings.leaveOutGenres, profile.settings.leaveOutLanguages),
                    scope.limit?.code.orEmpty()
                )
                if (!scoutShouldRefresh(activePool(owner), signature, now)) return@withContext
                val listed = folderSupport.listed(owner, started)
                if (!isCurrent()) return@withContext
                val built = build(
                    seeds, profile, signature, now, exclusionsOf(profile, watched, started, listed),
                    signals = signals + watching.filter { it in seeds }, ageTitles = signals + watching,
                    limit = scope.limit, isCurrent = isCurrent
                )
                operation.ensureActive()
                if (built == null || built.picked.isEmpty() || !isCurrent()) return@withContext
                savePool(profileId, built, isCurrent) {
                    _pool.value = OwnedScoutPool(scope, built)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Picks not rebuilt: ${error.javaClass.simpleName}")
            }
        }
    }

    private fun savePool(profileId: Int, pool: ScoutPool, isCurrent: () -> Boolean, publish: () -> Unit = {}) {
        if (!isCurrent()) return
        val target = file(profileId)
        val directory = requireNotNull(target.parentFile).apply { mkdirs() }
        val staged = File.createTempFile("pool_$profileId-", ".tmp", directory)
        try {
            staged.writeText(adapter.toJson(pool))
            synchronized(filesLock) {
                if (!isCurrent()) return
                FileSystem.SYSTEM.atomicMove(staged.toOkioPath(), target.toOkioPath())
                if (isCurrent()) publish()
            }
        } finally {
            staged.delete()
        }
    }

    /** Another profile on this box: its saved picks, built once if it has none, and what it has seen. */
    suspend fun memberFor(profileId: Int, now: Long = System.currentTimeMillis()): ScoutGroupMember =
        memberForCinema(profileId, false, now)

    internal suspend fun memberForCinema(profileId: Int, allowRewatch: Boolean, now: Long = System.currentTimeMillis()): ScoutGroupMember = withContext(ArcDispatchers.background) {
        val owner = selection()
        val revision = fileRevision
        val policy = scoutReadPolicy { ScoutPoolScope(owner, ratingLimits.get().limitFor(profileId)) }
            ?: return@withContext ScoutGroupMember(null, ScoutExclusions())
        val operation = currentCoroutineContext()
        fun isCurrent() = operation.isActive && owner == selection() && revision == fileRevision &&
            scoutReadPolicy { ScoutPoolScope(owner, ratingLimits.get().limitFor(profileId)) } == policy
        val profile = store.profileOf(profileId)
        if (profile.profileId != profileId || !isCurrent()) return@withContext ScoutGroupMember(null, ScoutExclusions())
        val watched = watchedItems.getAllItems(profileId)
        if (!isCurrent()) return@withContext ScoutGroupMember(null, ScoutExclusions())
        val started = progressOf.get().observeAllProgress(profileId).first()
        if (!isCurrent()) return@withContext ScoutGroupMember(null, ScoutExclusions())
        val exclusions = exclusionsOf(profile, watched, started, ScoutListed(), allowRewatch)
        val saved = readPool(profileId, policy.limit)
        if (!isCurrent()) return@withContext ScoutGroupMember(null, ScoutExclusions())
        if (saved != null && now - saved.builtAt < MEMBER_POOL_MAX_AGE_MS) {
            return@withContext ScoutGroupMember(saved, exclusions)
        }
        val finished = scoutSeeds(watched, profile.ratings.values)
        val watching = scoutWatchingSeeds(started, finished)
        val seeds = scoutSignalSeeds(finished, watching)
        if (seeds.isEmpty()) return@withContext ScoutGroupMember(saved, exclusions)
        val built = try {
            build(
                seeds, profile, scoutLimitSignature(
                    scoutHistorySignature(seeds, profile.settings.leaveOutGenres, profile.settings.leaveOutLanguages),
                    policy.limit?.code.orEmpty()
                ), now, exclusions, ageTitles = finished + watching, limit = policy.limit, isCurrent = ::isCurrent
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Member picks not built: ${error.javaClass.simpleName}")
            null
        }
        if (!isCurrent()) return@withContext ScoutGroupMember(null, ScoutExclusions())
        val ready = built?.takeIf { it.picked.isNotEmpty() } ?: return@withContext ScoutGroupMember(saved, exclusions)
        if (profileId != owner.profileId) runCatching { savePool(profileId, ready, ::isCurrent) }
        if (isCurrent()) ScoutGroupMember(ready, exclusions) else ScoutGroupMember(null, ScoutExclusions())
    }

    private suspend fun build(
        seeds: List<ScoutSeed>,
        profile: ScoutProfile,
        signature: String,
        now: Long,
        exclusions: ScoutExclusions,
        signals: List<ScoutSeed> = seeds,
        ageTitles: List<ScoutSeed> = signals,
        limit: RatingLimit?,
        isCurrent: () -> Boolean
    ): ScoutPool? {
        val profileId = profile.profileId
        val language = tmdbSettings.settings.first().language
        if (!isCurrent()) return null
        val gate = Semaphore(MAX_CONCURRENT_REQUESTS)
        val read = try {
            picksBuilder.ageCeiling(ageTitles, now, profileId, isCurrent)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "No age read: ${error.javaClass.simpleName}")
            null
        }
        if (!isCurrent()) return null
        val ratingGate = ratingLimits.get()
        val ceiling = listOfNotNull(read, limit?.scoutCeiling).minOrNull()
        val resolved = if (limit != null) emptyList() else coroutineScope {
            seeds.map { seed ->
                async { gate.withPermit { seed to recommendations(seed, language) } }
            }.awaitAll()
        }
        val blockedSeeds = resolved.filter { (seed, _) -> seed.finishedOrder != null || seed.weight < 0 }.mapNotNull { (seed, result) ->
            result?.first?.let { (if (seed.tv) "tv:" else "movie:") + it }
        }
        if (!isCurrent()) return null
        val full = exclusions.copy(tmdbKeys = exclusions.tmdbKeys + blockedSeeds)
        val results = resolved.map { (seed, result) -> seed to result?.second.orEmpty() }.toMutableList()

        val rated = HashSet<String>()
        var levels = if (ceiling == null) emptyMap() else picksBuilder.knownAgeLevels(now)
        fun fits(pick: ScoutPick) = pick.key in rated || scoutWithinCeiling(pick, levels[pick.key], ceiling)

        val positive = seeds.count { it.weight > 0 }
        var picked = scoutRank(results, full)
        if (positive < SCOUT_MIN_SEEDS || picked.count(::fits) < TOP_UP_BELOW) {
            val popular = if (ceiling == null) {
                discover(false, language, sortBy = "popularity.desc", votes = POPULAR_MIN_VOTES) +
                    discover(true, language, sortBy = "popularity.desc", votes = POPULAR_MIN_VOTES)
            } else {
                val found = ArrayList<ScoutPick>()
                val suitable = picked.filter(::fits).mapTo(HashSet()) { it.key }
                var page = 1
                while (page <= KIDS_TOP_UP_PAGES && suitable.size < TOP_UP_BELOW) {
                    val films = discover(false, language, sortBy = "popularity.desc", votes = POPULAR_MIN_VOTES,
                        certification = scoutCertificationAtMost(ceiling), without = KIDS_MOVIE_LEFT_OUT, page = page)
                    rated += films.map { it.key }
                    val shows = discover(true, language, sortBy = "popularity.desc", votes = KIDS_TV_MIN_VOTES,
                        genres = scoutKidsTvGenres(ceiling), without = KIDS_TV_LEFT_OUT, page = page,
                        certification = limit?.let { scoutLimitCertification(it, tv = true) })
                    if (films.isEmpty() && shows.isEmpty()) break
                    found += films + shows
                    (films + shows).filterNot(full::blocks).filter(::fits).forEach { suitable += it.key }
                    page++
                }
                found
            }
            results += ScoutSeed("", false, "", TOP_UP_WEIGHT, null) to popular
            picked = scoutRank(results, full).map { if (it.seedName.isNullOrBlank()) it.copy(seedName = null) else it }
        }

        val tasteGenres = picked.filter(::fits).take(50).flatMap { it.genres }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(3).joinToString("|") { it.key.toString() }
        val gemFilms = discover(false, language, "vote_average.desc", GEM_MIN_VOTES, SCOUT_GEM_MAX_VOTES, GEM_MIN_AVERAGE, tasteGenres,
            certification = ceiling?.let(::scoutCertificationAtMost), without = if (ceiling == null) null else KIDS_MOVIE_LEFT_OUT)
        if (ceiling != null) rated += gemFilms.map { it.key }
        val gemSeries = if (ceiling == null) {
            discover(true, language, "vote_average.desc", GEM_MIN_VOTES / 2, SCOUT_GEM_MAX_VOTES_TV, GEM_MIN_AVERAGE, tasteGenres)
        } else {
            discover(true, language, "vote_average.desc", GEM_MIN_VOTES / 2, SCOUT_GEM_MAX_VOTES_TV, GEM_MIN_AVERAGE,
                scoutKidsTvGenres(ceiling), without = KIDS_TV_LEFT_OUT,
                certification = limit?.let { scoutLimitCertification(it, tv = true) })
        }

        if (!isCurrent()) return null
        val improved = if (limit != null || profileId != activeProfile()) null else try {
            picksBuilder.improve(signals, profile.ratings.values, results, full, now, language, ceiling, profileId, isCurrent)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Picks kept simple: ${error.javaClass.simpleName}")
            null
        }
        if (!isCurrent()) return null
        if (ceiling != null) levels = picksBuilder.knownAgeLevels(now)

        suspend fun ratedForProfile(list: List<ScoutPick>): List<ScoutPick> =
            if (limit == null) list else ratingGate.keepFitting(limit, list) { "tmdb:${it.tmdbId}" to if (it.tv) "series" else "movie" }
        val because = seeds.filter { it.weight > 0 && it.finishedOrder != null }
            .sortedBy { it.finishedOrder }
            .take(BECAUSE_ROWS)
            .mapNotNull { seed ->
                val own = results.firstOrNull { it.first.contentId == seed.contentId }?.second.orEmpty()
                val candidates = own.filterNot(full::blocks).filter(::fits).take(BECAUSE_ITEMS * 2)
                val picks = ratedForProfile(candidates).take(BECAUSE_ITEMS)
                if (picks.isEmpty()) null else ScoutBecause(seed.name, picks)
            }
        val gems = ratedForProfile((gemFilms + gemSeries).filterNot(full::blocks).filter(::fits)).take(GEM_ITEMS)
        val suitable = ratedForProfile((improved?.picked?.takeIf { it.isNotEmpty() } ?: picked).filter(::fits))
        val rows = improved?.rows.orEmpty().map { row ->
            val kept = row.picks.filter(::fits)
            row.copy(picks = if (ceiling == null) kept else scoutTopUpRow(kept, suitable, row.genre, TASTE_ROW_ITEMS))
        }.filter { it.picks.size >= SCOUT_TASTE_ROW_MIN_PICKS }

        return ScoutPool(
            builtAt = now,
            historySignature = signature,
            picked = suitable,
            because = because,
            gems = gems,
            tasteRows = rows,
            newForYou = improved?.newForYou.orEmpty().filter(::fits),
            trendingForYou = improved?.trendingForYou.orEmpty().filter(::fits),
            version = SCOUT_POOL_VERSION
        ).also { pool ->
            Log.i(TAG, "Built profile=$profileId ceiling=$ceiling picked=${pool.picked.size} because=${pool.because.size} " +
                "gems=${pool.gems.size} rows=${pool.tasteRows.size}/${improved?.rows?.size ?: 0} new=${pool.newForYou.size} trending=${pool.trendingForYou.size}")
        }
    }

    /** Forgets the picks; the next refresh builds them again from what is left. */
    suspend fun reset() {
        val owner = selection()
        refreshLock.withLock {
            withContext(Dispatchers.IO) {
                synchronized(filesLock) {
                    if (owner == selection()) {
                        fileRevision++
                        file(owner.profileId).delete()
                        _pool.value = null
                    }
                }
            }
        }
    }

    internal suspend fun exclusions(profile: ScoutProfile, owner: ScoutSelection): ScoutExclusions? =
        exclusionsForCinema(profile, owner, false)

    internal suspend fun exclusionsForCinema(profile: ScoutProfile, owner: ScoutSelection, allowRewatch: Boolean): ScoutExclusions? = withContext(Dispatchers.Default) {
        if (profile.profileId != owner.profileId || owner != selection()) return@withContext null
        val watched = watchedItems.getAllItems(owner.profileId)
        if (owner != selection()) return@withContext null
        val started = progressOf.get().observeAllProgress(owner.profileId).first()
        if (owner != selection()) return@withContext null
        val listed = folderSupport.listed(owner, started)
        if (owner == selection()) exclusionsOf(profile, watched, started, listed, allowRewatch) else null
    }

    private fun exclusionsOf(
        profile: ScoutProfile,
        watched: List<com.nuvio.tv.domain.model.WatchedItem>,
        started: List<com.nuvio.tv.domain.model.WatchProgress>,
        listed: ScoutListed,
        allowRewatch: Boolean = false
    ): ScoutExclusions {
        val excludedWatched = if (allowRewatch) watched.filter { it.contentType != "movie" } else watched
        val names = excludedWatched.map { scoutExclusionName(it.title, it.releaseInfo) } +
            started.map { scoutNormalizedName(it.name) } +
            profile.ratings.values.filter { it.rating == com.nuvio.tv.domain.model.ScoutRating.NOT_FOR_ME }.map { scoutNormalizedName(it.title.name) } +
            listed.names
        val listedKeys = listed.ids.filter { it.startsWith("tmdb:") }.flatMap { id ->
            val tmdb = id.removePrefix("tmdb:")
            listOf("movie:$tmdb", "tv:$tmdb")
        }
        val ids = (excludedWatched.map { it.contentId to it.contentType } + started.map { it.contentId to it.contentType } +
            profile.ratings.values.filter { it.rating == com.nuvio.tv.domain.model.ScoutRating.NOT_FOR_ME }.map { it.title.id to it.title.type })
            .mapNotNull { (id, type) ->
                val tmdb = id.removePrefix("tmdb:").takeIf { id.startsWith("tmdb:") } ?: return@mapNotNull null
                val tv = type.trim().lowercase() != "movie"
                (if (tv) "tv:" else "movie:") + tmdb
            }
        return ScoutExclusions(names = names.toSet(), tmdbKeys = ids.toSet() + listedKeys, genres = profile.settings.leaveOutGenres, languages = scoutLeaveOutLanguageCodes(profile.settings.leaveOutLanguages))
    }

    private suspend fun recommendations(seed: ScoutSeed, language: String): Pair<Int, List<ScoutPick>>? {
        val media = if (seed.tv) "tv" else "movie"
        val id = runCatching { tmdbService.ensureTmdbId(seed.contentId, media)?.toIntOrNull() }.getOrNull() ?: return null
        val response = runCatching {
            if (seed.tv) tmdbApi.getTvRecommendations(id, com.nuvio.tv.core.tmdb.TmdbApiKey.value, language)
            else tmdbApi.getMovieRecommendations(id, com.nuvio.tv.core.tmdb.TmdbApiKey.value, language)
        }.getOrNull()?.body() ?: return id to emptyList()
        return id to response.results.orEmpty().mapNotNull { it.toPick(seed.tv) }
    }

    private suspend fun discover(
        tv: Boolean,
        language: String,
        sortBy: String,
        votes: Int,
        votesMax: Int? = null,
        average: Double? = null,
        genres: String? = null,
        certification: String? = null,
        without: String? = null,
        page: Int = 1
    ): List<ScoutPick> {
        val body = runCatching {
            if (tv) tmdbApi.discoverTv(
                apiKey = com.nuvio.tv.core.tmdb.TmdbApiKey.value, language = language, page = page, sortBy = sortBy, voteCountGte = votes,
                voteCountLte = votesMax, voteAverageGte = average, withGenres = genres?.ifBlank { null },
                withoutGenres = without ?: TV_LEFT_OUT, certificationCountry = certification?.let { CERTIFICATION_COUNTRY },
                certificationLte = certification
            ) else tmdbApi.discoverMovies(
                apiKey = com.nuvio.tv.core.tmdb.TmdbApiKey.value, language = language, page = page, sortBy = sortBy, voteCountGte = votes,
                voteCountLte = votesMax, voteAverageGte = average, withGenres = genres?.ifBlank { null },
                withoutGenres = without, certificationCountry = certification?.let { CERTIFICATION_COUNTRY },
                certificationLte = certification
            )
        }.getOrNull()?.body()
        return body?.results.orEmpty().mapNotNull { it.toPick(tv) }
    }
}

/** The highest US film rating at or under an age ceiling. */
internal fun scoutCertificationAtMost(ceiling: Int): String = when {
    ceiling <= 0 -> "G"
    ceiling == 1 -> "PG"
    else -> "PG-13"
}

/** TV discover has no rating filter, so a ceiling picks kids shows, and family shows from PG up. */
internal fun scoutKidsTvGenres(ceiling: Int): String = if (ceiling <= 0) "10762" else "10762|10751"

private fun image(path: String?, size: String) = path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }

internal fun TmdbRecommendationResult.toPick(seedTv: Boolean): ScoutPick? {
    val tv = when (mediaType?.trim()?.lowercase()) {
        "tv" -> true
        "movie" -> false
        else -> seedTv
    }
    return ScoutPick(
        tmdbId = id,
        tv = tv,
        name = title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: return null,
        poster = image(posterPath, "w342") ?: return null,
        backdrop = image(backdropPath, "w1280"),
        overview = overview?.takeIf { it.isNotBlank() },
        released = (if (tv) firstAirDate else releaseDate)?.takeIf { it.isNotBlank() },
        rating = voteAverage?.toFloat(),
        votes = voteCount,
        genres = genreIds.orEmpty(),
        language = originalLanguage
    )
}

internal fun TmdbDiscoverResult.toPick(tv: Boolean): ScoutPick? = ScoutPick(
    tmdbId = id,
    tv = tv,
    name = title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: return null,
    poster = image(posterPath, "w342") ?: return null,
    backdrop = image(backdropPath, "w1280"),
    overview = overview?.takeIf { it.isNotBlank() },
    released = (if (tv) firstAirDate else releaseDate)?.takeIf { it.isNotBlank() },
    rating = voteAverage?.toFloat(),
    votes = voteCount,
    genres = genreIds.orEmpty(),
    language = originalLanguage,
    origin = originCountry.orEmpty()
)
