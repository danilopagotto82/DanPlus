package com.nuvio.tv.data.repository

import android.util.Log
import com.nuvio.tv.core.tmdb.TmdbApiKey
import com.nuvio.tv.data.remote.api.SeriesGraphApi
import com.nuvio.tv.data.remote.api.SeriesGraphSeasonRatingsDto
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.domain.model.EpisodeRating
import com.nuvio.tv.domain.model.EpisodeRatingSource
import com.nuvio.tv.domain.model.EpisodeRatingsResult
import com.nuvio.tv.domain.model.isPublicEpisodeScore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ImdbEpisodeRatingsRepo"
private val imdbTitlePattern = Regex("tt[0-9]+(?::[0-9]+:[0-9]+)?", RegexOption.IGNORE_CASE)

internal fun canonicalRatingsImdbId(raw: String?): String? = raw?.trim()?.let { id ->
    val unprefixed = if (id.startsWith("imdb:", ignoreCase = true)) id.substringAfter(':') else id
    unprefixed.takeIf(imdbTitlePattern::matches)?.substringBefore(':')?.lowercase()
}

@Singleton
class ImdbEpisodeRatingsRepository @Inject constructor(
    private val seriesGraphApi: SeriesGraphApi,
    private val tmdbApi: TmdbApi
) {
    private data class Key(val source: EpisodeRatingSource, val id: String, val season: Int = 0)
    private data class ProviderResult(
        val ratings: Map<Pair<Int, Int>, EpisodeRating> = emptyMap(),
        val failed: Boolean = false
    )
    private data class CacheEntry(val result: ProviderResult, val expiresAt: Long)
    private class Request(val result: Deferred<ProviderResult>, var consumers: Int = 0)
    private val cache = LinkedHashMap<Key, CacheEntry>(16, .75f, true)
    private val inFlight = mutableMapOf<Key, Request>()
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal var tmdbKey: () -> String = { TmdbApiKey.value }

    // Keep the Home card's existing score lookup limited to SeriesGraph.
    suspend fun getEpisodeRatings(tmdbId: Int?): Map<Pair<Int, Int>, Double> =
        seriesGraphRatings(tmdbId).ratings.mapValues { it.value.value }

    suspend fun getSeasonRatings(tmdbId: Int?, season: Int, episodes: Set<Int>): EpisodeRatingsResult {
        val wanted = episodes.filter { season > 0 && it > 0 }.mapTo(hashSetOf()) { season to it }
        if (wanted.isEmpty()) return EpisodeRatingsResult()
        val graph = seriesGraphRatings(tmdbId)
        val graphRatings = graph.ratings.filterKeys { it in wanted }
        if (graphRatings.size == wanted.size) return EpisodeRatingsResult(graphRatings)
        val tmdb = tmdbRatings(tmdbId, season)
        val scores = tmdb.ratings.filterKeys { it in wanted } + graphRatings
        return EpisodeRatingsResult(scores, scores.size < wanted.size && (graph.failed || tmdb.failed))
    }

    private suspend fun seriesGraphRatings(tmdbId: Int?): ProviderResult {
        val id = tmdbId?.takeIf { it > 0 } ?: return ProviderResult()
        val source = EpisodeRatingSource.SERIES_GRAPH
        return fetch(Key(source, id.toString())) {
            seriesGraphApi.getSeasonRatings(id).toResult(source) { ratingsFromGraph(it, source) }
        }
    }

    private suspend fun tmdbRatings(tmdbId: Int?, season: Int): ProviderResult {
        val id = tmdbId?.takeIf { it > 0 } ?: return ProviderResult()
        val apiKey = tmdbKey().takeIf(String::isNotBlank) ?: return ProviderResult()
        val source = EpisodeRatingSource.TMDB
        return fetch(Key(source, id.toString(), season)) {
            tmdbApi.getTvSeasonDetails(id, season, apiKey).toResult(source) { body ->
                if (body.seasonNumber != null && body.seasonNumber != season) return@toResult emptyMap()
                uniqueRatings(body.episodes.orEmpty().mapNotNull { episode ->
                    val number = episode.episodeNumber?.takeIf { it > 0 } ?: return@mapNotNull null
                    val episodeSeason = episode.seasonNumber ?: body.seasonNumber
                    if (episodeSeason != season || (episode.showId != null && episode.showId != id)) return@mapNotNull null
                    val score = episode.voteAverage?.takeIf { it.isPublicEpisodeScore() && (episode.voteCount ?: 0) > 0 }
                        ?: return@mapNotNull null
                    (season to number) to EpisodeRating(score, source)
                })
            }
        }
    }

    private fun <T> Response<T>.toResult(
        source: EpisodeRatingSource,
        decode: (T) -> Map<Pair<Int, Int>, EpisodeRating>
    ): ProviderResult {
        if (isSuccessful) return ProviderResult(body()?.let(decode).orEmpty())
        errorBody()?.close()
        if (code() == 404) return ProviderResult()
        Log.w(TAG, "$source episode ratings HTTP ${code()}")
        return ProviderResult(failed = true)
    }

    private suspend fun fetch(key: Key, load: suspend () -> ProviderResult): ProviderResult {
        val request = synchronized(lock) {
            cache[key]?.let { entry ->
                if (entry.expiresAt > System.currentTimeMillis()) return entry.result
                cache.remove(key)
            }
            inFlight.getOrPut(key) {
                Request(scope.async(start = CoroutineStart.LAZY) {
                    try {
                        load()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        // Retrofit exceptions can include a credential in a request URL.
                        Log.w(TAG, "${key.source} episode ratings failed: ${error.javaClass.simpleName}")
                        ProviderResult(failed = true)
                    }
                })
            }.also { it.consumers++ }
        }
        try {
            val result = request.result.await()
            synchronized(lock) {
                if (!result.failed && key !in cache) {
                    val ttl = if (result.ratings.isEmpty()) 2L else 30L
                    cache[key] = CacheEntry(result, System.currentTimeMillis() + ttl * 60_000)
                    if (cache.size > 128) cache.remove(cache.keys.first())
                }
            }
            return result
        } finally {
            synchronized(lock) {
                if (--request.consumers == 0) {
                    inFlight.remove(key)
                    request.result.cancel()
                }
            }
        }
    }
}

private fun ratingsFromGraph(
    payload: List<SeriesGraphSeasonRatingsDto>, source: EpisodeRatingSource
): Map<Pair<Int, Int>, EpisodeRating> = uniqueRatings(payload.flatMap { group ->
    group.episodes.orEmpty().mapNotNull { episode ->
        val season = (episode.seasonNumber ?: group.seasonNumber)?.takeIf { it > 0 } ?: return@mapNotNull null
        if (group.seasonNumber != null && group.seasonNumber != season) return@mapNotNull null
        val number = episode.episodeNumber?.takeIf { it > 0 } ?: return@mapNotNull null
        val raw = episode.voteAverage?.takeIf(Double::isPublicEpisodeScore) ?: return@mapNotNull null
        (season to number) to EpisodeRating(raw, source)
    }
})

private fun uniqueRatings(ratings: List<Pair<Pair<Int, Int>, EpisodeRating>>): Map<Pair<Int, Int>, EpisodeRating> =
    ratings.groupBy({ it.first }, { it.second }).mapNotNull { (episode, values) ->
        values.distinct().singleOrNull()?.let { episode to it }
    }.toMap()
