package com.nuvio.tv.data.repository

import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.TraktAuthState
import com.nuvio.tv.data.remote.api.TraktApi
import com.nuvio.tv.data.remote.dto.trakt.TraktEpisodeDto
import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.remote.dto.trakt.TraktMovieDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingItemDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingsMutationResponseDto
import com.nuvio.tv.data.remote.dto.trakt.TraktShowDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

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

    private class RatingRequests {
        val owner = TraktRatingOwner(1, 4L, 6L)
        val ids = TraktIdsDto(trakt = 17, tvdb = 991)
        val episode = TraktPersonalRatingTarget.Episode(ids)
        val profile = MutableStateFlow(1)
        val revision = MutableStateFlow(4L)
        var state = TraktAuthState("linked", "refresh", authorizationGeneration = 6L)
        var readHeader = "Bearer linked"
        var writeHeader = "Bearer linked"
        val api = mockk<TraktApi>()
        val auth = mockk<TraktAuthService> {
            coEvery { getCurrentAuthState(1) } coAnswers { state }
            coEvery { executeAuthorizedRequest<List<TraktRatingItemDto>>(any()) } coAnswers {
                firstArg<suspend (String) -> Response<List<TraktRatingItemDto>>>()(readHeader)
            }
            coEvery { executeAuthorizedWriteRequest<TraktRatingsMutationResponseDto>(any()) } coAnswers {
                firstArg<suspend (String) -> Response<TraktRatingsMutationResponseDto>>()(writeHeader)
            }
        }
        val repository = TraktPersonalRatingRepository(api, auth, mockk<ProfileManager> {
            every { activeProfileId } returns profile
            every { profileSelectionRevision } returns revision
        })
    }

    @Test
    fun `wrong bearer never reaches the API for a rating read save or removal`() = runTest {
        val requests = RatingRequests()
        requests.readHeader = "Bearer other-account"
        assertTrue(runCatching { requests.repository.read(requests.owner, requests.episode) }.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { requests.api.getRatings(any(), any(), any(), any()) }
        requests.readHeader = "Bearer linked"
        requests.writeHeader = "Bearer other-account"
        coEvery { requests.api.getRatings(any(), "episodes", 1, 100) } returns Response.success(
            listOf(TraktRatingItemDto(rating = 5, episode = TraktEpisodeDto(ids = requests.ids)))
        )
        for (rating in listOf(8, null)) {
            assertTrue(runCatching {
                requests.repository.set(requests.owner, requests.episode, rating)
            }.exceptionOrNull() is IllegalStateException)
        }
        coVerify(exactly = 0) { requests.api.putPersonalRating(any(), any()) }
        coVerify(exactly = 0) { requests.api.deletePersonalRating(any(), any()) }
    }

    @Test
    fun `a renewed bearer on retry can write an episode without changing authorization generation`() = runTest {
        val requests = RatingRequests()
        coEvery { requests.api.getRatings("Bearer linked", "episodes", 1, 100) } returns Response.success(emptyList())
        val receipt = TraktRatingsMutationResponseDto(added = mapOf("episodes" to 1))
        val request = requests.episode.request(8)
        coEvery { requests.api.putPersonalRating("Bearer linked", request) } returns Response.error(401, "".toResponseBody())
        coEvery { requests.api.putPersonalRating("Bearer renewed", request) } returns Response.success(receipt)
        coEvery { requests.auth.executeAuthorizedWriteRequest<TraktRatingsMutationResponseDto>(any()) } coAnswers {
            val send = firstArg<suspend (String) -> Response<TraktRatingsMutationResponseDto>>()
            assertEquals(401, send("Bearer linked").code())
            requests.state = requests.state.copy(accessToken = "renewed")
            send("Bearer renewed")
        }
        assertTrue(requests.repository.set(requests.owner, requests.episode, 8))
        coVerify(exactly = 1) { requests.api.putPersonalRating("Bearer renewed", request) }
    }

    @Test
    fun `a response after profile reselection or replacing the account is discarded`() = runTest {
        val requests = RatingRequests()
        coEvery { requests.api.getRatings(any(), "episodes", 1, 100) } coAnswers {
            requests.profile.value = 2
            requests.revision.value++
            requests.profile.value = 1
            requests.revision.value++
            Response.success(emptyList())
        }
        assertTrue(runCatching {
            requests.repository.read(requests.owner, requests.episode)
        }.exceptionOrNull() is IllegalStateException)

        val current = RatingRequests()
        coEvery { current.api.getRatings(any(), "episodes", 1, 100) } returns Response.success(emptyList())
        coEvery { current.api.putPersonalRating(any(), any()) } coAnswers {
            current.state = current.state.copy(accessToken = "replacement", authorizationGeneration = 7L)
            Response.success(TraktRatingsMutationResponseDto(added = mapOf("episodes" to 1)))
        }
        assertTrue(runCatching {
            current.repository.set(current.owner, current.episode, 8)
        }.exceptionOrNull() is IllegalStateException)
    }
}
