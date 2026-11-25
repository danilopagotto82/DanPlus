package com.nuvio.tv.ui.screens.detail

import com.nuvio.tv.data.remote.api.TmdbEpisode
import com.nuvio.tv.data.remote.api.TmdbSeasonResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktRatingEpisodeIdentityTest {
    @Test fun specialSeasonKeepsTheEpisodeIdentityInsteadOfReusingTheSeriesId() {
        val special = TmdbEpisode(episodeNumber = 1, seasonNumber = 0, id = 734)
        val response = TmdbSeasonResponse(seasonNumber = 0, episodes = listOf(special))
        assertEquals(734, exactRatingEpisodeId(response, 0, 1))
        assertNull(exactRatingEpisodeId(response, 1, 1))
        assertNull(exactRatingEpisodeId(response, 0, 2))
    }

    @Test fun duplicatesAndNonRepresentableIdsAreRejected() {
        val episode = TmdbEpisode(episodeNumber = 1, seasonNumber = 2, id = 734)
        val duplicate = TmdbSeasonResponse(seasonNumber = 2, episodes = listOf(episode, episode.copy(id = 735)))
        assertNull(exactRatingEpisodeId(duplicate, 2, 1))
        val outOfRange = duplicate.copy(episodes = listOf(episode.copy(id = Int.MAX_VALUE.toLong() + 1)))
        assertNull(exactRatingEpisodeId(outOfRange, 2, 1))
    }
}
