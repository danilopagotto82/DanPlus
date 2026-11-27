package com.nuvio.tv.ui.screens.profile

import com.nuvio.tv.core.danexus.ProfileRecommendationRules
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem

internal fun matchesRecommendationPoster(item: MetaPreview, meta: Meta): Boolean =
    ProfileRecommendationRules.isSupportedType(meta.apiType) &&
        item.apiType.equals("movie", true) == meta.apiType.equals("movie", true) &&
        (meta.id == item.id || item.imdbId?.takeIf(String::isNotBlank)?.let { it == meta.id || it == meta.imdbId } == true)

internal fun posterRecommendationMeta(item: MetaPreview, meta: Meta?): Meta {
    val matched = meta?.takeIf { matchesRecommendationPoster(item, it) }
    return Meta(
        id = item.id,
        type = item.type,
        rawType = item.rawType,
        name = item.name,
        poster = item.poster ?: matched?.poster,
        posterShape = item.posterShape,
        background = item.background ?: matched?.background,
        logo = item.logo ?: matched?.logo,
        description = item.description ?: matched?.description,
        releaseInfo = item.releaseInfo,
        imdbRating = item.imdbRating,
        genres = item.genres,
        runtime = item.runtime,
        director = item.director,
        cast = emptyList(),
        videos = matched?.videos.orEmpty(),
        country = item.country,
        awards = null,
        language = item.language,
        links = item.links,
        imdbId = item.imdbId ?: matched?.imdbId,
        landscapePoster = item.landscapePoster ?: matched?.landscapePoster
    )
}

internal fun continueWatchingRecommendationItem(item: ContinueWatchingItem, mixItem: MetaPreview?): MetaPreview? {
    mixItem?.takeIf { ProfileRecommendationRules.isSupportedType(it.apiType) }?.let { return it }
    val progress = (item as? ContinueWatchingItem.InProgress)?.progress
        ?.takeIf { it.contentType.equals("movie", true) } ?: return null
    return MetaPreview(
        id = progress.contentId, type = ContentType.MOVIE, name = progress.name,
        poster = progress.poster, posterShape = PosterShape.POSTER, background = progress.backdrop,
        logo = progress.logo, description = null, releaseInfo = null, imdbRating = null,
        genres = emptyList(), sourceAddonBaseUrl = progress.addonBaseUrl
    )
}
