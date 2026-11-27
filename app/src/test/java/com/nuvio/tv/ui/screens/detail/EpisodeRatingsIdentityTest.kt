package com.nuvio.tv.ui.screens.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nuvio.tv.domain.model.EpisodeRatingSource
import com.nuvio.tv.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeRatingsIdentityTest {
    @Test
    fun ringsOfPowerAcceptsQualifiedAliasesAndRejectsOpaqueOrConflictingIds() {
        assertEquals(EpisodeRatingsIdentity("tt7631058", 84773),
            episodeRatingsIdentity("tmdb:84773", "IMDb:TT7631058", "tt7631058"))
        assertEquals(EpisodeRatingsIdentity(), episodeRatingsIdentity("84773", null, "trakt:84773"))
        assertEquals(EpisodeRatingsIdentity(), episodeRatingsIdentity("tt7631058", "tt0000000", "tmdb:84773"))
        assertEquals(EpisodeRatingsIdentity(), episodeRatingsIdentity("tt7631058:garbage", null, ""))
    }

    @Test
    fun contradictoryEpisodeIdsAndInvalidAddonScoresDoNotAcquireRatings() {
        fun video(id: String, number: Int, score: Double) =
            Video(id, "", null, null, season = 1, episode = number, overview = null, rating = score)
        val candidates = episodeRatingsCandidates(listOf(
            video("tt7631058:1:1", 1, 7.0),
            video("tt7631058:2:2", 2, 8.0),
            video("tt0000000:1:3", 3, 8.0),
            video("tmdb:84773:1:4", 4, Double.NaN),
            video("tmdb:99:1:5", 5, 8.0),
            video("tt7631058:1:6", 6, 11.0)
        ), 1, EpisodeRatingsIdentity("tt7631058", 84773))
        assertEquals(setOf(1, 4, 6), candidates.keys)
        val ratings = addonEpisodeRatings(candidates, 1)
        assertEquals(7.0, ratings.getValue(1 to 1).value, .001)
        assertEquals(EpisodeRatingSource.ADDON, ratings.getValue(1 to 1).source)
        assertEquals(1, ratings.size)
    }

    @Test
    fun whiteScoreTextHasContrastAcrossAllRatingBands() {
        val colors = listOf(9.4, 8.4, 7.4, 6.4, 5.4).map(::episodeRatingColor)
        assertEquals(5, colors.distinct().size)
        assertTrue(colors.all { (Color.White.luminance() + .05f) / (it.luminance() + .05f) >= 4.5f })
    }
}
