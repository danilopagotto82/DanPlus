package com.nuvio.tv.data.repository

import com.nuvio.tv.data.remote.dto.trakt.TraktEpisodeDto
import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.remote.dto.trakt.TraktMovieDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingItemDto
import com.nuvio.tv.data.remote.dto.trakt.TraktShowDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktPersonalRatingRepositoryTest {
    @Test
    fun `an episode rating writes only episode identifiers`() {
        val target = TraktPersonalRatingTarget.Episode(TraktIdsDto(trakt = 17, tvdb = 991))
        val request = target.request(8)
        assertNull(request.shows)
        assertNull(request.movies)
        assertEquals(991, request.episodes?.single()?.ids?.tvdb)
        assertEquals(8, request.episodes?.single()?.rating)
    }

    @Test
    fun `a show score or conflicting episode ID cannot be used as the episode rating`() {
        val ids = TraktIdsDto(trakt = 17, tvdb = 991)
        val target = TraktPersonalRatingTarget.Episode(ids)
        assertNull(target.ratingIn(TraktRatingItemDto(rating = 9, show = TraktShowDto(ids = ids))))
        assertNull(target.ratingIn(TraktRatingItemDto(rating = 9, episode = TraktEpisodeDto(ids = ids.copy(tvdb = 992)))))
        assertEquals(9, target.ratingIn(TraktRatingItemDto(rating = 9, episode = TraktEpisodeDto(ids = ids))))
    }

    @Test
    fun `movies use their own bucket and a missing identity or invalid score is rejected`() {
        val ids = TraktIdsDto(imdb = "tt10101")
        val target = TraktPersonalRatingTarget.Movie(ids)
        assertEquals(ids, target.request(null).movies?.single()?.ids)
        assertEquals(6, target.ratingIn(TraktRatingItemDto(rating = 6, movie = TraktMovieDto(ids = ids))))
        assertTrue(runCatching { target.request(11) }.isFailure)
        assertTrue(runCatching { TraktPersonalRatingTarget.Episode(TraktIdsDto()).request(7) }.isFailure)
    }
}
