package com.nuvio.tv.domain.model

enum class EpisodeRatingSource { IMDB, SERIES_GRAPH, TMDB, ADDON }

data class EpisodeRating(val value: Double, val source: EpisodeRatingSource)

data class EpisodeRatingsResult(
    val ratings: Map<Pair<Int, Int>, EpisodeRating> = emptyMap(),
    val failed: Boolean = false
)

internal fun Double.isPublicEpisodeScore(): Boolean = isFinite() && this > 0.0 && this <= 10.0
