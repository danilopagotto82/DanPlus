package com.nuvio.tv.ui.screens.profile

import com.nuvio.tv.core.danexus.ProfileRecommendationRules
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterRecommendationMetaTest {
    private val poster = MetaPreview(
        id = "tmdb:84773", type = ContentType.SERIES, name = "Os Anéis de Poder",
        poster = "poster", posterShape = PosterShape.POSTER, background = "backdrop",
        logo = "logo", description = "sinopse", releaseInfo = null, imdbRating = null,
        genres = emptyList(), imdbId = "tt7631058"
    )

    @Test
    fun recommendationUsesThePosterIdentityAndMatchedEpisodeKeys() {
        val episode = Video("tt7631058:1:1", "E1", null, null, season = 1, episode = 1, overview = null)
        val fullMeta = posterRecommendationMeta(poster, null).copy(id = "tt7631058", videos = listOf(episode))
        val recommendation = posterRecommendationMeta(poster, fullMeta)
        assertEquals(poster.id, recommendation.id)
        assertEquals(poster.name, recommendation.name)
        assertEquals("logo", recommendation.logo)
        assertEquals(setOf("1:1"), ProfileRecommendationRules.episodeKeys(recommendation))
    }

    @Test
    fun keepsTheOriginalPosterWithoutBorrowingAnotherTitlesMetadata() {
        val full = posterRecommendationMeta(poster, null)
        val wrong = full.copy(id = "tt0000000", imdbId = "tt0000000", logo = "wrong")
        val wrongType = full.copy(type = ContentType.MOVIE, rawType = "movie")
        assertTrue(matchesRecommendationPoster(poster, full))
        assertFalse(matchesRecommendationPoster(poster, wrong))
        assertFalse(matchesRecommendationPoster(poster, wrongType))
        assertEquals(full, posterRecommendationMeta(poster, wrong))
    }

    @Test
    fun continueWatchingMovieRecommendsTheTitleWithoutItsPlaybackState() {
        val progress = WatchProgress(
            contentId = "tt1234567", contentType = "movie", name = "Filme",
            poster = "poster", backdrop = null, logo = null, videoId = "resume-id",
            season = null, episode = null, episodeTitle = null,
            position = 700L, duration = 2_000L, lastWatched = 12L
        )
        val preview = checkNotNull(continueWatchingRecommendationItem(ContinueWatchingItem.InProgress(progress), null))
        assertEquals("tt1234567", preview.id)
        assertEquals("movie", preview.apiType)
        assertTrue(posterRecommendationMeta(preview, null).videos.isEmpty())
    }

}
