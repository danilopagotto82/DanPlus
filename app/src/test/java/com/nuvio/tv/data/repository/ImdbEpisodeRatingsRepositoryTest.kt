package com.nuvio.tv.data.repository

import com.nuvio.tv.data.remote.api.SeriesGraphApi
import com.nuvio.tv.data.remote.api.SeriesGraphEpisodeRatingDto
import com.nuvio.tv.data.remote.api.SeriesGraphSeasonRatingsDto
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbEpisode
import com.nuvio.tv.data.remote.api.TmdbSeasonResponse
import com.nuvio.tv.domain.model.EpisodeRating
import com.nuvio.tv.domain.model.EpisodeRatingSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.net.UnknownHostException

class ImdbEpisodeRatingsRepositoryTest {

    private val backup = listOf(
        SeriesGraphSeasonRatingsDto(
            episodes = listOf(SeriesGraphEpisodeRatingDto(seasonNumber = 1, episodeNumber = 1, voteAverage = 8.1))
        )
    )

    @Test
    fun `Home scores keep the SeriesGraph episode number`() = runBlocking {
        val graph = mockk<SeriesGraphApi>()
        coEvery { graph.getSeasonRatings(84773) } returns Response.success(backup)
        assertEquals(mapOf((1 to 1) to 8.1), repository(graph).getEpisodeRatings(84773))
        coVerify(exactly = 1) { graph.getSeasonRatings(84773) }
    }

    private fun repository(
        graph: SeriesGraphApi = mockk {
            coEvery { getSeasonRatings(any()) } returns Response.success(emptyList())
        },
        tmdb: TmdbApi = mockk()
    ) = ImdbEpisodeRatingsRepository(graph, tmdb).apply {
        tmdbKey = { "test-app-key" }
    }

    @Test
    fun `Rings of Power uses public TMDB per season when SeriesGraph has no scores`() = runBlocking {
        val tmdb = mockk<TmdbApi>()
        coEvery { tmdb.getTvSeasonDetails(84773, any(), "test-app-key", null) } coAnswers {
            val season = secondArg<Int>()
            Response.success(TmdbSeasonResponse(season, listOf(
                TmdbEpisode(seasonNumber = season, episodeNumber = 1, showId = 84773,
                    voteCount = 20, voteAverage = if (season == 1) 7.0 else 8.0),
                TmdbEpisode(seasonNumber = season, episodeNumber = 2, voteCount = 0, voteAverage = 9.9),
                TmdbEpisode(seasonNumber = season, episodeNumber = 3, showId = 99, voteCount = 20, voteAverage = 9.9),
                TmdbEpisode(seasonNumber = 4, episodeNumber = 4, voteCount = 20, voteAverage = 9.9)
            )))
        }
        val repository = repository(tmdb = tmdb)
        val season1 = repository.getSeasonRatings(84773, 1, setOf(1, 2, 3, 4))
        assertEquals(mapOf((1 to 1) to EpisodeRating(7.0, EpisodeRatingSource.TMDB)), season1.ratings)
        assertFalse(season1.failed)
        assertEquals(season1, repository.getSeasonRatings(84773, 1, setOf(1, 2, 3, 4)))
        assertEquals(EpisodeRating(8.0, EpisodeRatingSource.TMDB),
            repository.getSeasonRatings(84773, 2, setOf(1)).ratings[2 to 1])
        coVerify(exactly = 1) { tmdb.getTvSeasonDetails(84773, 1, any(), null) }
    }

    @Test
    fun `partial responses keep the origin of each score in priority order`() = runBlocking {
        val graph = mockk<SeriesGraphApi>()
        val tmdb = mockk<TmdbApi>()
        coEvery { graph.getSeasonRatings(84773) } returns Response.success(listOf(
            SeriesGraphSeasonRatingsDto(seasonNumber = 1, episodes = listOf(
                SeriesGraphEpisodeRatingDto(episodeNumber = 1, voteAverage = 9.0),
                SeriesGraphEpisodeRatingDto(episodeNumber = 2, voteAverage = 8.2),
                SeriesGraphEpisodeRatingDto(seasonNumber = 2, episodeNumber = 3, voteAverage = 9.0),
                SeriesGraphEpisodeRatingDto(episodeNumber = 3, voteAverage = -1.0)
            ))
        ))
        coEvery { tmdb.getTvSeasonDetails(84773, 1, any(), null) } returns Response.success(
            TmdbSeasonResponse(1, listOf(TmdbEpisode(episodeNumber = 3, voteCount = 10, voteAverage = 7.3)))
        )
        val result = repository(graph, tmdb)
            .getSeasonRatings(84773, 1, setOf(1, 2, 3))
        assertEquals(mapOf(
            (1 to 1) to EpisodeRating(9.0, EpisodeRatingSource.SERIES_GRAPH),
            (1 to 2) to EpisodeRating(8.2, EpisodeRatingSource.SERIES_GRAPH),
            (1 to 3) to EpisodeRating(7.3, EpisodeRatingSource.TMDB)
        ), result.ratings)
    }

    @Test
    fun `HTTP failure differs from no data and permits a retry`() = runBlocking {
        val tmdb = mockk<TmdbApi>()
        coEvery { tmdb.getTvSeasonDetails(84773, 1, any(), null) } returnsMany listOf(
            Response.error(503, "".toResponseBody()), Response.error(404, "".toResponseBody())
        )
        val ratings = repository(tmdb = tmdb)
        assertTrue(ratings.getSeasonRatings(84773, 1, setOf(1)).failed)
        assertFalse(ratings.getSeasonRatings(84773, 1, setOf(1)).failed)
        coVerify(exactly = 2) { tmdb.getTvSeasonDetails(84773, 1, any(), null) }
    }

    @Test
    fun `deduplicates callers and cancels the call only after the last waiter leaves`() = runBlocking {
        withTimeout(4_000) {
            val started = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<Unit>()
            val tmdb = mockk<TmdbApi>()
            coEvery { tmdb.getTvSeasonDetails(84773, 1, any(), null) } coAnswers {
                started.complete(Unit)
                try { awaitCancellation() } finally { stopped.complete(Unit) }
            }
            val ratings = repository(tmdb = tmdb)
            val first = async { ratings.getSeasonRatings(84773, 1, setOf(1)) }
            started.await()
            val other = async(start = CoroutineStart.UNDISPATCHED) { ratings.getSeasonRatings(84773, 1, setOf(1)) }
            first.cancelAndJoin()
            assertFalse(stopped.isCompleted)
            other.cancelAndJoin()
            stopped.await()
            coVerify(exactly = 1) { tmdb.getTvSeasonDetails(84773, 1, any(), null) }
        }
    }

    @Test
    fun `an outage lasts a day when the network works and ten minutes when it may be offline`() {
        var now = 1_000L
        val outage = HostOutage(now = { now })

        outage.markDown(networkUp = false)
        now += HostOutage.OFFLINE_MS - 1
        assertTrue(outage.isDown())
        now += 1
        assertFalse(outage.isDown())

        outage.markDown(networkUp = true)
        now += HostOutage.GONE_MS - 1
        assertTrue(outage.isDown())
        now += 1
        assertFalse(outage.isDown())
    }

    @Test
    fun `only connection failures count as an unreachable host`() {
        assertTrue(HostOutage.isUnreachable(UnknownHostException("x")))
        assertTrue(HostOutage.isUnreachable(java.io.IOException("wrapped", java.net.ConnectException("refused"))))
        assertFalse(HostOutage.isUnreachable(java.net.SocketTimeoutException("slow")))
    }
}
