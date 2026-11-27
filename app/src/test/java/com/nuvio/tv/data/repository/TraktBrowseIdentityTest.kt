package com.nuvio.tv.data.repository

import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.remote.dto.trakt.TraktMovieDto
import com.nuvio.tv.data.remote.dto.trakt.TraktSearchResultDto
import com.nuvio.tv.data.remote.dto.trakt.TraktShowDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktBrowseIdentityTest {
    @Test
    fun movieAndShowWithSameTraktIdKeepSeparateCanonicalIdentities() {
        val movie = TraktIdsDto(trakt = 13, tmdb = 2014)
        val show = TraktIdsDto(trakt = 13, tmdb = 2022)
        val results = listOf(
            TraktSearchResultDto(type = "movie", movie = TraktMovieDto(ids = movie)),
            TraktSearchResultDto(type = "show", show = TraktShowDto(ids = show))
        )
        assertEquals(movie, exactTraktBrowseIds(results, 13, TraktRelatedType.MOVIE))
        assertEquals(show, exactTraktBrowseIds(results, 13, TraktRelatedType.SHOW))
        assertNull(exactTraktBrowseIds(results, 99, TraktRelatedType.MOVIE))
    }

    @Test
    fun duplicateRowsAndImdbOnlyMappingsAreAllowedButConflictingMappingsAreRejected() {
        val ids = TraktIdsDto(trakt = 13, imdb = "tt1234567")
        val response = TraktSearchResultDto(type = "movie", movie = TraktMovieDto(ids = ids))
        assertEquals(ids, exactTraktBrowseIds(listOf(response, response), 13, TraktRelatedType.MOVIE))
        val conflict = response.copy(movie = TraktMovieDto(ids = ids.copy(imdb = "tt7654321")))
        assertNull(exactTraktBrowseIds(listOf(response, conflict), 13, TraktRelatedType.MOVIE))
    }
}
