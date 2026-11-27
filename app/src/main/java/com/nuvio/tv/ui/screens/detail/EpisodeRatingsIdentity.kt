package com.nuvio.tv.ui.screens.detail

import com.nuvio.tv.data.repository.canonicalRatingsImdbId
import com.nuvio.tv.domain.model.EpisodeRating
import com.nuvio.tv.domain.model.EpisodeRatingSource
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.isPublicEpisodeScore

internal data class EpisodeRatingsIdentity(val imdb: String? = null, val tmdb: Int? = null)

internal fun episodeRatingsIdentity(metaId: String, metaImdbId: String?, routeId: String): EpisodeRatingsIdentity {
    val imdb = listOf(metaId, metaImdbId, routeId).mapNotNull(::canonicalRatingsImdbId).distinct()
    val tmdb = listOf(metaId, routeId).mapNotNull { id ->
        val value = id.trim()
        if (value.startsWith("tmdb:", ignoreCase = true)) {
            value.substringAfter(':').toIntOrNull()?.takeIf { it > 0 }
        } else null
    }.distinct()
    if (imdb.size > 1 || tmdb.size > 1) return EpisodeRatingsIdentity()
    return EpisodeRatingsIdentity(imdb.singleOrNull(), tmdb.singleOrNull())
}

private val numberedVideoId = Regex("((?:imdb:)?tt[0-9]+|tmdb:[0-9]+):([0-9]+):([0-9]+)", RegexOption.IGNORE_CASE)

internal fun episodeRatingsCandidates(videos: List<Video>, season: Int, identity: EpisodeRatingsIdentity): Map<Int, List<Video>> =
    videos.asSequence().filter { it.season == season && (it.episode ?: 0) > 0 }
        .groupBy { checkNotNull(it.episode) }
        .filter { (number, candidates) ->
            candidates.all { video ->
                val id = numberedVideoId.matchEntire(video.id)
                if (id == null) true else {
                    val (series, encodedSeason, encodedNumber) = id.destructured
                    val imdb = canonicalRatingsImdbId(series)
                    val tmdb = series.substringAfter(':').toIntOrNull()
                    encodedSeason.toIntOrNull() == season && encodedNumber.toIntOrNull() == number &&
                        (imdb == null || identity.imdb == null || imdb == identity.imdb) &&
                        (tmdb == null || identity.tmdb == null || tmdb == identity.tmdb)
                }
            }
        }

internal fun addonEpisodeRatings(candidates: Map<Int, List<Video>>, season: Int): Map<Pair<Int, Int>, EpisodeRating> =
    candidates.mapNotNull { (episode, videos) ->
        videos.mapNotNull { it.rating?.takeIf(Double::isPublicEpisodeScore) }.distinct().singleOrNull()
            ?.let { (season to episode) to EpisodeRating(it, EpisodeRatingSource.ADDON) }
    }.toMap()
