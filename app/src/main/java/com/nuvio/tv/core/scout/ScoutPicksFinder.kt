package com.nuvio.tv.core.scout

import android.content.Context
import com.nuvio.tv.core.ratinglimit.RatingLimit
import com.nuvio.tv.core.ratinglimit.ratingLimitGateOrNull
import com.nuvio.tv.core.ratinglimit.ratingRank
import com.nuvio.tv.domain.model.isAnimeRecap
import com.nuvio.tv.domain.model.ScoutAdvancedSearch
import android.util.Log
import com.nuvio.tv.data.local.ScoutStore
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.domain.model.ScoutExclusions
import com.nuvio.tv.domain.model.ScoutPick
import com.nuvio.tv.domain.model.ScoutPool
import com.nuvio.tv.domain.model.ScoutProfile
import com.nuvio.tv.domain.model.TmdbCollectionFilters
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

const val SCOUT_PICKS_SHOWN = 3
const val SCOUT_FROM_COUNTRY_MIN_TITLES = 20
const val SCOUT_QUICK_EPISODE_LIMIT = 35
private const val SCOUT_MOOD_ROW_TARGET = 12
private const val SURPRISE_FROM = 60
private const val ANIMATION = 16
private const val LAUGH_ANIMATION_SHARE = 5
private const val POOL_BONUS = 3.0
private const val SHUFFLE = 1.2
private const val TASTE_GENRES = 50
private const val WELL_RATED = 7.0f
private const val GEM_RATING = 7.5f
private const val POPULAR = "popularity.desc"
private const val ENDED_STATUS = "Ended"

/** Cartoons are light rather than funny for most grown-ups, unless at least one pick in five is animated. */
internal fun ScoutAnswers.scoutLaughLeavesOutAnimation(pool: ScoutPool?): Boolean {
    if (mood != ScoutMood.LAUGH || family || watch == ScoutWatch.ANIME) return false
    val top = pool?.picked.orEmpty().take(TASTE_GENRES)
    return top.count { ANIMATION in it.genres } * LAUGH_ANIMATION_SHARE < top.size.coerceAtLeast(1)
}

/** The second half of a pick's line: only what Arc can stand behind. */
sealed interface ScoutWhy {
    data object SuitsEveryone : ScoutWhy
    data class Close(val name: String) : ScoutWhy
    data object Finished : ScoutWhy
    data object Gem : ScoutWhy
    data object WellRated : ScoutWhy
    data object New : ScoutWhy
}

@JsonClass(generateAdapter = true)
data class ScoutPickDetail(
    val seasons: Int? = null,
    val minutes: Int? = null,
    val ended: Boolean = false,
    val rating: String? = null,
    val episodeCount: Int? = null,
    val lastAirDate: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList()
)

/** One extra profile in the room: its picks, and what it must not be offered. */
data class ScoutGroupMember(val pool: ScoutPool?, val exclusions: ScoutExclusions)

fun scoutWhy(pick: ScoutPick, detail: ScoutPickDetail?, suitsEveryone: Boolean = false): ScoutWhy {
    val rating = pick.rating ?: 0f
    return when {
        suitsEveryone -> ScoutWhy.SuitsEveryone
        !pick.seedName.isNullOrBlank() -> ScoutWhy.Close(pick.seedName)
        detail?.ended == true -> ScoutWhy.Finished
        (pick.votes ?: Int.MAX_VALUE) <= SCOUT_GEM_MAX_VOTES && rating >= GEM_RATING -> ScoutWhy.Gem
        rating >= WELL_RATED -> ScoutWhy.WellRated
        else -> ScoutWhy.New
    }
}

private fun scoutFit(pick: ScoutPick, pool: ScoutPool?): Double {
    val picked = pool?.picked.orEmpty()
    val counts = picked.take(TASTE_GENRES).flatMap { it.genres }.groupingBy { it }.eachCount()
    val top = (counts.values.maxOrNull() ?: 1).toDouble()
    val inPool = if (picked.any { it.key == pick.key }) POOL_BONUS else 0.0
    return inPool + (pick.genres.maxOfOrNull { (counts[it] ?: 0) / top } ?: 0.0)
}

/**
 * Titles from the profile's own pool first, then those sharing its genres, with enough shuffle to differ
 * between visits. With several people each title counts by whoever it suits least, so nobody is outvoted.
 */
fun scoutOrderPicks(
    candidates: List<ScoutPick>,
    pool: ScoutPool?,
    exclusions: ScoutExclusions,
    seed: Long,
    others: List<ScoutGroupMember> = emptyList()
): List<ScoutPick> = orderScoutPicks(candidates, pool, exclusions, seed, others, balanceCinema = false)

internal fun scoutOrderCinemaPicks(
    candidates: List<ScoutPick>,
    pool: ScoutPool?,
    exclusions: ScoutExclusions,
    others: List<ScoutGroupMember>
): List<ScoutPick> = orderScoutPicks(candidates, pool, exclusions, 0L, others, balanceCinema = true)

private fun orderScoutPicks(
    candidates: List<ScoutPick>,
    pool: ScoutPool?,
    exclusions: ScoutExclusions,
    seed: Long,
    others: List<ScoutGroupMember>,
    balanceCinema: Boolean
): List<ScoutPick> {
    val seeds = pool?.picked.orEmpty().associate { it.key to it.seedName }
    val random = Random(seed)
    val pools = listOf(pool) + others.map { it.pool }
    val informedPools = pools.filter { !it?.picked.isNullOrEmpty() }
    return candidates.distinctBy { it.key }
        .filterNot { pick -> exclusions.blocks(pick) || others.any { it.exclusions.blocks(pick) } }
        .map { pick ->
            val fit = if (balanceCinema) {
                val affinities = informedPools.map { scoutFit(pick, it) }
                if (affinities.isEmpty()) 0.0 else .65 * affinities.min() + .35 * affinities.average()
            } else pools.minOf { scoutFit(pick, it) }
            val named = seeds[pick.key]?.let { pick.copy(seedName = it) } ?: pick
            val shuffle = if (balanceCinema) 0.0 else random.nextDouble() * SHUFFLE
            named to fit + (pick.rating ?: 0f) / 10.0 + shuffle
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

private const val EXCLUSIONS_FRESH_MS = 60_000L
private const val DAY_MS = 24L * 60 * 60 * 1000
private const val ROWS_KEPT = 48
private const val TITLES_KEPT = 300

private class Recent<V : Any>(private val most: Int) {
    private val entries = LinkedHashMap<String, V>(16, .75f, true)

    @Synchronized
    operator fun get(key: String): V? = entries[key]

    @Synchronized
    operator fun set(key: String, value: V) {
        entries[key] = value
        if (entries.size > most) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun all(): Map<String, V> = LinkedHashMap(entries)

    @Synchronized
    fun clear() = entries.clear()
}

/** Today's Not for me and left-out genres over an older list, so a rating counts at once. */
internal fun ScoutExclusions.withNotForMe(profile: ScoutProfile): ScoutExclusions {
    val out = profile.ratings.values.filter { it.rating == com.nuvio.tv.domain.model.ScoutRating.NOT_FOR_ME }
    return copy(
        names = names + out.map { com.nuvio.tv.domain.model.scoutNormalizedName(it.title.name) },
        tmdbKeys = tmdbKeys + out.filter { it.title.id.startsWith("tmdb:") }.map {
            (if (it.title.type.trim().lowercase() != "movie") "tv:" else "movie:") + it.title.id.removePrefix("tmdb:")
        },
        genres = profile.settings.leaveOutGenres,
        languages = scoutLeaveOutLanguageCodes(profile.settings.leaveOutLanguages)
    )
}

@JsonClass(generateAdapter = true)
data class ScoutKeptRow(val picks: List<ScoutPick>, val at: Long)

@JsonClass(generateAdapter = true)
data class ScoutKeptFile(
    val rows: Map<String, ScoutKeptRow> = emptyMap(),
    val details: Map<String, ScoutPickDetail> = emptyMap(),
    val logos: Map<String, String> = emptyMap(),
    val titlesAt: Long = 0L
)

@Singleton
class ScoutPicksFinder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tmdbApi: TmdbApi,
    private val tmdbSettings: TmdbSettingsDataStore,
    private val engine: ScoutEngine,
    private val store: ScoutStore,
    private val moodAi: ScoutMoodAiStore,
    private val collectionSources: dagger.Lazy<com.nuvio.tv.core.tmdb.TmdbCollectionSourceResolver>
) {
    private val fromCountryCounts = HashMap<String, Int>()

    private class SeenExclusions(val owner: ScoutSelection, val profile: ScoutProfile, val at: Long, val exclusions: ScoutExclusions)

    @Volatile private var seenExclusions: SeenExclusions? = null
    private val refreshingExclusions = AtomicBoolean(false)
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val keptRows = Recent<ScoutKeptRow>(ROWS_KEPT)
    private val keptDetails = Recent<ScoutPickDetail>(TITLES_KEPT)
    private val keptLogos = Recent<String>(TITLES_KEPT)
    private val keptAdapter = Moshi.Builder().build().adapter(ScoutKeptFile::class.java)
    private val keptFile get() = File(context.filesDir, "scout/kept_rows.json")
    private val restoring = Mutex()
    @Volatile private var restored = false
    @Volatile private var titlesAt = 0L

    private suspend fun restoreKept() {
        if (restored) return
        restoring.withLock {
            if (restored) return
            withContext(Dispatchers.IO) {
                val saved = runCatching { keptAdapter.fromJson(keptFile.readText()) }.getOrNull()
                val now = System.currentTimeMillis()
                saved?.rows?.forEach { (key, row) -> if (now - row.at < DAY_MS) keptRows[key] = row }
                if (saved != null && now - saved.titlesAt < DAY_MS) {
                    saved.details.forEach { (key, detail) -> keptDetails[key] = detail }
                    saved.logos.forEach { (key, logo) -> keptLogos[key] = logo }
                    titlesAt = saved.titlesAt
                } else {
                    titlesAt = now
                }
            }
            restored = true
        }
    }

    /** So tomorrow's first visit, or one after the app was closed, doesn't start from nothing. */
    internal suspend fun saveKept(isCurrent: () -> Boolean) = withContext(Dispatchers.IO) {
        if (!isCurrent()) return@withContext
        runCatching {
            val serialized = keptAdapter.toJson(ScoutKeptFile(keptRows.all(), keptDetails.all(), keptLogos.all(), titlesAt))
            if (isCurrent()) {
                keptFile.parentFile?.mkdirs()
                keptFile.writeText(serialized)
            }
        }
        Unit
    }

    private fun expireTitles() {
        val now = System.currentTimeMillis()
        if (now - titlesAt < DAY_MS) return
        keptDetails.clear()
        keptLogos.clear()
        titlesAt = now
    }

    private suspend fun fullRowKey(key: String, scope: ScoutPoolScope): String? {
        val owner = scope.selection
        if (scope != engine.scopeFor(owner)) return null
        val profile = owner.profileId
        val rated = store.profileOf(profile).ratings.values.sumOf { it.ratedAt }
        if (scope != engine.scopeFor(owner)) return null
        val moodsChecked = withContext(Dispatchers.IO) { moodAi.read(profile).moods.values.sumOf { it.checkedAt } }
        val language = tmdbSettings.settings.first().language
        if (scope != engine.scopeFor(owner)) return null
        return "scoped|$profile|${scope.limit?.code}|$key|$language|${engine.activePool(owner)?.builtAt}|$rated|$moodsChecked"
    }

    /** A row built in the last day, without anything the profile has since seen or rated. */
    internal suspend fun keptRow(key: String, owner: ScoutSelection): List<ScoutPick>? {
        val scope = engine.scopeFor(owner) ?: return null
        restoreKept()
        val fullKey = fullRowKey(key, scope) ?: return null
        val now = System.currentTimeMillis()
        val kept = keptRows[fullKey]?.takeIf { now - it.at < DAY_MS } ?: return null
        val exclusions = exclusions(owner) ?: return null
        return eligibleForActiveProfile(kept.picks.filterNot(exclusions::blocks), scope)
            .takeIf { scope == engine.scopeFor(owner) }
    }

    internal suspend fun keepRow(key: String, picks: List<ScoutPick>, owner: ScoutSelection, isCurrent: () -> Boolean) {
        if (!isCurrent()) return
        val scope = engine.scopeFor(owner) ?: return
        val fullKey = fullRowKey(key, scope) ?: return
        if (isCurrent() && scope == engine.scopeFor(owner)) keptRows[fullKey] = ScoutKeptRow(picks, System.currentTimeMillis())
    }

    internal suspend fun exclusions(owner: ScoutSelection = engine.selection()): ScoutExclusions? {
        if (owner != engine.selection()) return null
        val profile = store.profileOf(owner.profileId)
        if (owner != engine.selection()) return null
        val now = System.currentTimeMillis()
        val seen = seenExclusions?.takeIf { it.owner == owner }
            ?: return engine.exclusions(profile, owner)?.takeIf { owner == engine.selection() }?.also {
                seenExclusions = SeenExclusions(owner, profile, now, it)
            }
        if ((seen.profile != profile || now - seen.at >= EXCLUSIONS_FRESH_MS) && refreshingExclusions.compareAndSet(false, true)) {
            background.launch {
                try {
                    val fresh = engine.exclusions(profile, owner)
                    if (fresh != null && owner == engine.selection()) {
                        seenExclusions = SeenExclusions(owner, profile, System.currentTimeMillis(), fresh)
                    }
                } finally {
                    refreshingExclusions.set(false)
                }
            }
        }
        return seen.exclusions.withNotForMe(profile).takeIf { owner == engine.selection() }
    }

    fun pool(): ScoutPool? = engine.activePool()

    suspend fun refreshPool() = engine.refreshIfDue()

    suspend fun member(profileId: Int): ScoutGroupMember = engine.memberFor(profileId)

    /** One page of each side the answers allow. */
    suspend fun fetch(answers: ScoutAnswers, page: Int, relaxed: Boolean): List<ScoutPick> {
        val scope = engine.scopeFor() ?: return emptyList()
        val found = fetchUnchecked(answers, page, relaxed)
        if (engine.scopeFor(scope.selection) != scope) return emptyList()
        val rejected = moodAi.rejected(answers)
        if (engine.scopeFor(scope.selection) != scope) return emptyList()
        val kept = found.scoutWithoutRejected(rejected)
        if (rejected.isEmpty() || kept.size >= SCOUT_MOOD_ROW_TARGET) return kept
        val more = fetchUnchecked(answers, page + 1, relaxed).scoutWithoutRejected(rejected)
        return (kept + more).distinctBy { it.key }.takeIf { engine.scopeFor(scope.selection) == scope }.orEmpty()
    }

    suspend fun fetchUnchecked(answers: ScoutAnswers, page: Int, relaxed: Boolean = false): List<ScoutPick> {
        val scope = engine.scopeFor() ?: return emptyList()
        val slice = answers.slice()
        val language = tmdbSettings.settings.first().language
        if (engine.scopeFor(scope.selection) != scope) return emptyList()
        val noAnimation = answers.scoutLaughLeavesOutAnimation(engine.activePool(scope.selection))
        val shaped = { f: TmdbCollectionFilters ->
            if (noAnimation) f.copy(withoutGenres = listOfNotNull(f.withoutGenres, ANIMATION.toString()).joinToString(",")) else f
        }
        return coroutineScope {
            val movies = slice.movie?.let(shaped)?.let { f -> async { discover(if (relaxed) f.scoutRelaxed() else f, false, slice.sortBy, page, language, scope) } }
            val series = slice.tv?.let(shaped)?.let { f -> async { discover(if (relaxed) f.scoutRelaxed() else f, true, slice.sortBy, page, language, scope) } }
            val a = movies?.await().orEmpty()
            val b = series?.await().orEmpty()
            if (engine.scopeFor(scope.selection) != scope) return@coroutineScope emptyList()
            (0 until maxOf(a.size, b.size)).flatMap { i -> listOfNotNull(a.getOrNull(i), b.getOrNull(i)) }
        }
    }

    fun surprisePool(): List<ScoutPick> = pool()?.picked.orEmpty().take(SURPRISE_FROM)

    internal suspend fun searchKeywords(text: String): List<Pair<Int, String>> {
        val scope = engine.scopeFor() ?: return emptyList()
        val query = text.trim().take(80)
        if (query.length < 2) return emptyList()
        val found = scoutReadPolicy { collectionSources.get().searchKeywords(query) }.orEmpty()
        if (engine.scopeFor(scope.selection) != scope) return emptyList()
        return found.entries.take(8).map { it.key to it.value }
    }

    suspend fun fetchAdvanced(query: ScoutAdvancedSearch, answers: ScoutAnswers, page: Int): List<ScoutPick> {
        val normalized = query.normalized()
        val scope = engine.scopeFor() ?: return emptyList()
        val slice = answers.slice()
        val language = tmdbSettings.settings.first().language
        fun TmdbCollectionFilters.withQuery(): TmdbCollectionFilters {
            val year = normalized.year
            val earliest = listOfNotNull(releaseDateGte, year?.let { "$it-01-01" }).maxOrNull()
            val latest = listOfNotNull(releaseDateLte, year?.let { "$it-12-31" }).minOrNull()
            return copy(
                withGenres = listOfNotNull(withGenres, normalized.genreIds.takeIf { it.isNotEmpty() }?.joinToString(","))
                    .joinToString(",").ifEmpty { null },
                withKeywords = listOfNotNull(withKeywords, normalized.keywordExpression()).joinToString(",").ifEmpty { null },
                withOriginalLanguage = withOriginalLanguage ?: normalized.originalLanguage,
                releaseDateGte = earliest,
                releaseDateLte = latest,
            )
        }
        val matches = coroutineScope {
            suspend fun candidates(filters: TmdbCollectionFilters?, tv: Boolean): List<ScoutPick> {
                if (filters == null) return emptyList()
                val canUseTitleSearch = answers.watch in setOf(ScoutWatch.ANYTHING, ScoutWatch.MOVIE, ScoutWatch.SERIES) &&
                    answers.copy(watch = ScoutWatch.ANYTHING).isUnset
                if (normalized.exactPhrase.isNotEmpty() && canUseTitleSearch && normalized.keywordIds.isEmpty()) {
                    val apiKey = com.nuvio.tv.core.tmdb.TmdbApiKey.value
                    val response = try {
                        if (tv) tmdbApi.searchTv(apiKey, normalized.exactPhrase, language, page = page)
                        else tmdbApi.searchMovies(apiKey, normalized.exactPhrase, language, page = page)
                    } catch (_: java.io.IOException) {
                        return emptyList()
                    }
                    return eligibleForActiveProfile(response.body()?.results.orEmpty().mapNotNull { it.toPick(tv) }, scope)
                }
                return discover(filters.withQuery(), tv, slice.sortBy, page, language, scope)
            }
            val movies = async { candidates(slice.movie, false) }
            val series = async { candidates(slice.tv, true) }
            movies.await() + series.await()
        }
        return if (engine.scopeFor(scope.selection) == scope) matches.filter(normalized::matches) else emptyList()
    }

    suspend fun moreLike(pick: ScoutPick): List<ScoutPick> {
        val scope = engine.scopeFor() ?: return emptyList()
        val language = tmdbSettings.settings.first().language
        if (engine.scopeFor(scope.selection) != scope) return emptyList()
        val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
        if (scope.limit != null) {
            val genres = pick.genres.distinct().takeIf { it.isNotEmpty() } ?: return emptyList()
            return discover(TmdbCollectionFilters(withGenres = genres.joinToString("|")), pick.tv, null, 1, language, scope)
                .filterNot { it.key == pick.key }
        }
        val body = runCatching {
            if (pick.tv) tmdbApi.getTvRecommendations(pick.tmdbId, key, language) else tmdbApi.getMovieRecommendations(pick.tmdbId, key, language)
        }.getOrNull()?.body()
        return eligibleForActiveProfile(body?.results.orEmpty().mapNotNull { it.toPick(pick.tv) }, scope)
    }

    /** Only worth offering where the list would not come up nearly empty. Asked once per country. */
    suspend fun fromCountryOffered(country: String): Boolean {
        if (country.isBlank()) return false
        val total = fromCountryCounts[country] ?: run {
            val language = tmdbSettings.settings.first().language
            val answers = ScoutAnswers(tunes = setOf(ScoutTune.FROM_COUNTRY)).slice(country = country)
            val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
            val counted = coroutineScope {
                val films = async {
                    val f = answers.movie ?: return@async null
                    runCatching {
                        tmdbApi.discoverMovies(apiKey = key, language = language, withOriginCountry = f.withOriginCountry, withOriginalLanguage = f.withOriginalLanguage, voteCountGte = f.voteCountGte, voteAverageGte = f.voteAverageGte)
                    }.getOrNull()?.body()?.totalResults
                }
                val series = async {
                    val f = answers.tv ?: return@async null
                    runCatching {
                        tmdbApi.discoverTv(apiKey = key, language = language, withOriginCountry = f.withOriginCountry, withOriginalLanguage = f.withOriginalLanguage, voteCountGte = f.voteCountGte, voteAverageGte = f.voteAverageGte, withoutGenres = f.withoutGenres)
                    }.getOrNull()?.body()?.totalResults
                }
                val a = films.await()
                val b = series.await()
                if (a == null && b == null) null else (a ?: 0) + (b ?: 0)
            }
            counted?.also { fromCountryCounts[country] = it } ?: return false
        }
        return total >= SCOUT_FROM_COUNTRY_MIN_TITLES
    }

    suspend fun detail(pick: ScoutPick, withRating: Boolean): ScoutPickDetail? {
        restoreKept()
        expireTitles()
        val keptAs = "${pick.key}|$withRating"
        keptDetails[keptAs]?.let { return it }
        return fetchDetail(pick, withRating)?.also { keptDetails[keptAs] = it }
    }

    private suspend fun fetchDetail(pick: ScoutPick, withRating: Boolean): ScoutPickDetail? {
        val language = tmdbSettings.settings.first().language
        val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
        val details = runCatching {
            if (pick.tv) tmdbApi.getTvDetails(pick.tmdbId, key, language) else tmdbApi.getMovieDetails(pick.tmdbId, key, language)
        }.getOrNull()?.body() ?: return null
        val rating = if (withRating) certification(pick) else null
        val genres = details.genres.orEmpty().map { it.name }.filter(String::isNotBlank)
        return if (pick.tv) ScoutPickDetail(
            seasons = (details.seasonCount ?: details.seasons?.count { (it.seasonNumber ?: 0) > 0 })?.takeIf { it > 0 },
            minutes = details.episodeRunTime?.firstOrNull() ?: details.lastEpisodeToAir?.runtime,
            ended = details.status == ENDED_STATUS,
            rating = rating,
            episodeCount = details.episodeCount,
            lastAirDate = details.lastAirDate,
            status = details.status,
            genres = genres
        ) else ScoutPickDetail(minutes = details.runtime, rating = rating, genres = genres)
    }

    /** The title's logo in the app's language, else English, else one with no language. */
    suspend fun logo(pick: ScoutPick): String? = logo(pick.tmdbId, pick.tv)

    suspend fun logo(tmdbId: Int, tv: Boolean): String? {
        val language = tmdbSettings.settings.first().language.substringBefore("-")
        restoreKept()
        expireTitles()
        val keptAs = "$tv:$tmdbId|$language"
        keptLogos[keptAs]?.let { return it.ifEmpty { null } }
        val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
        val body = runCatching {
            val wanted = "$language,en,null"
            if (tv) tmdbApi.getTvImages(tmdbId, key, wanted) else tmdbApi.getMovieImages(tmdbId, key, wanted)
        }.getOrNull()?.body() ?: return null
        val logos = body.logos.orEmpty().filter { it.filePath?.endsWith(".svg") == false }
        val best = logos.firstOrNull { it.iso6391 == language } ?: logos.firstOrNull { it.iso6391 == "en" } ?: logos.firstOrNull()
        val found = best?.filePath?.let { "https://image.tmdb.org/t/p/w500$it" }
        keptLogos[keptAs] = found.orEmpty()
        return found
    }

    private suspend fun certification(pick: ScoutPick): String? = runCatching {
        val country = Locale.getDefault().country.uppercase(Locale.ROOT)
        val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
        if (pick.tv) {
            val results = tmdbApi.getTvContentRatings(pick.tmdbId, key).body()?.results.orEmpty()
            results.firstOrNull { it.iso31661 == country }?.rating?.takeIf { it.isNotBlank() }
                ?: results.firstOrNull { it.iso31661 == "US" }?.rating?.takeIf { it.isNotBlank() }
        } else {
            val results = tmdbApi.getMovieReleaseDates(pick.tmdbId, key).body()?.results.orEmpty()
            fun of(code: String) = results.firstOrNull { it.iso31661 == code }?.releaseDates.orEmpty()
                .firstNotNullOfOrNull { it.certification?.takeIf(String::isNotBlank) }
            of(country) ?: of("US")
        }
    }.getOrNull()

    /** One page of series for a ready-made mix, in the app's language. */
    suspend fun discoverSeries(f: TmdbCollectionFilters, page: Int, sortBy: String): List<ScoutPick> =
        discover(f, true, sortBy, page, tmdbSettings.settings.first().language)

    private suspend fun eligibleForActiveProfile(
        picks: List<ScoutPick>, scope: ScoutPoolScope? = engine.scopeFor()
    ): List<ScoutPick> {
        if (scope == null || scope != engine.scopeFor(scope.selection)) return emptyList()
        val candidates = picks.filterNot(ScoutPick::isAnimeRecap)
        val limit = scope.limit ?: return candidates
        val gate = scoutReadPolicy { ratingLimitGateOrNull(context) } ?: return emptyList()
        val kept = scoutReadPolicy {
            gate.keepFitting(limit, candidates) { "tmdb:${it.tmdbId}" to if (it.tv) "series" else "movie" }
        }.orEmpty()
        return kept.takeIf { scope == engine.scopeFor(scope.selection) }.orEmpty()
    }

    private suspend fun discover(
        filters: TmdbCollectionFilters, tv: Boolean, sortBy: String?, page: Int, language: String,
        scope: ScoutPoolScope? = engine.scopeFor()
    ): List<ScoutPick> {
        if (scope == null || engine.scopeFor(scope.selection) != scope) return emptyList()
        val limit = scope.limit
        val f = if (limit != null) filters.copy(
            certificationCountry = "US",
            certificationLte = filters.certificationLte?.takeIf {
                filters.certificationCountry == "US" && limit.allows(ratingRank(it))
            } ?: scoutLimitCertification(limit, tv)
        ) else filters
        val key = com.nuvio.tv.core.tmdb.TmdbApiKey.value
        val body = runCatching {
            if (tv) tmdbApi.discoverTv(
                apiKey = key, language = language, page = page, sortBy = sortBy ?: POPULAR,
                firstAirDateLte = f.releaseDateLte, firstAirDateGte = f.releaseDateGte,
                voteCountGte = f.voteCountGte, voteCountLte = f.voteCountLte, voteAverageGte = f.voteAverageGte,
                withGenres = f.withGenres, withoutGenres = f.withoutGenres, withKeywords = f.withKeywords, withoutKeywords = f.withoutKeywords,
                withOriginCountry = f.withOriginCountry, withOriginalLanguage = f.withOriginalLanguage,
                withStatus = f.withStatus, withRuntimeLte = f.withRuntimeLte, withRuntimeGte = f.withRuntimeGte,
                certificationCountry = f.certificationCountry, certificationLte = f.certificationLte,
                withType = f.withType
            ) else tmdbApi.discoverMovies(
                apiKey = key, language = language, page = page, sortBy = sortBy ?: POPULAR,
                releaseDateLte = f.releaseDateLte, releaseDateGte = f.releaseDateGte,
                voteCountGte = f.voteCountGte, voteCountLte = f.voteCountLte, voteAverageGte = f.voteAverageGte,
                withGenres = f.withGenres, withoutGenres = f.withoutGenres, withKeywords = f.withKeywords, withoutKeywords = f.withoutKeywords,
                withOriginCountry = f.withOriginCountry, withOriginalLanguage = f.withOriginalLanguage,
                withRuntimeLte = f.withRuntimeLte, withRuntimeGte = f.withRuntimeGte,
                certificationCountry = f.certificationCountry, certificationLte = f.certificationLte
            )
        }.getOrNull()?.body()
        return eligibleForActiveProfile(body?.results.orEmpty().mapNotNull { it.toPick(tv) }, scope)
    }
}

internal fun scoutLimitCertification(limit: RatingLimit, tv: Boolean): String =
    if (tv) when (limit) {
        RatingLimit.PRESCHOOL -> "TV-Y"
        RatingLimit.YOUNG -> "TV-G"
        RatingLimit.PG -> "TV-PG"
        RatingLimit.TEEN -> "TV-14"
    } else scoutCertificationAtMost(limit.scoutCeiling)
