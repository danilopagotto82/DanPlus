package com.nuvio.tv.data.repository

import com.nuvio.tv.data.remote.api.ImdbTapframeApi
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
    fun `a primary host that does not resolve is skipped on the next series`() = runBlocking {
        val primary = mockk<ImdbTapframeApi>()
        coEvery { primary.getSeasonRatings(any()) } throws UnknownHostException("imdb.tapframe.space")
        val fallback = mockk<SeriesGraphApi>()
        coEvery { fallback.getSeasonRatings(any()) } returns Response.success(backup)
        val repository = repository(primary, fallback).apply { primaryConfigured = true }

        assertEquals(mapOf((1 to 1) to 8.1), repository.getEpisodeRatings("tt0000001", 11))
        assertEquals(mapOf((1 to 1) to 8.1), repository.getEpisodeRatings("tt0000002", 12))

        coVerify(exactly = 1) { primary.getSeasonRatings(any()) }
        coVerify(exactly = 2) { fallback.getSeasonRatings(any()) }
    }

    @Test
    fun `a primary that answers is still asked first`() = runBlocking {
        val primary = mockk<ImdbTapframeApi>()
        coEvery { primary.getSeasonRatings(any()) } returns Response.success(backup)
        val fallback = mockk<SeriesGraphApi>()
        val repository = repository(primary, fallback).apply { primaryConfigured = true }

        repository.getEpisodeRatings("tt0000001", 11)
        repository.getEpisodeRatings("tt0000002", 12)

        coVerify(exactly = 2) { primary.getSeasonRatings(any()) }
        coVerify(exactly = 0) { fallback.getSeasonRatings(any()) }
    }

    @Test
    fun `no primary address goes straight to the backup`() = runBlocking {
        val primary = mockk<ImdbTapframeApi>()
        val fallback = mockk<SeriesGraphApi>()
        coEvery { fallback.getSeasonRatings(any()) } returns Response.success(backup)
        val repository = repository(primary, fallback)

        assertEquals(mapOf((1 to 1) to 8.1), repository.getEpisodeRatings("tt0000001", 11))
        coVerify(exactly = 0) { primary.getSeasonRatings(any()) }
    }

    private fun repository(
        primary: ImdbTapframeApi = mockk(),
        graph: SeriesGraphApi = mockk(),
        tmdb: TmdbApi = mockk()
    ) = ImdbEpisodeRatingsRepository(primary, graph, tmdb).apply {
        primaryConfigured = false
        seriesGraphConfigured = true
        tmdbKey = { "test-app-key" }
    }

    @Test
    fun `Rings of Power uses public TMDB per season when both partner URLs are absent`() = runBlocking {
        val primary = mockk<ImdbTapframeApi>()
        val graph = mockk<SeriesGraphApi>()
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
        val repository = repository(primary, graph, tmdb).apply { seriesGraphConfigured = false }
        val season1 = repository.getSeasonRatings("tt7631058", 84773, 1, setOf(1, 2, 3, 4))
        assertEquals(mapOf((1 to 1) to EpisodeRating(7.0, EpisodeRatingSource.TMDB)), season1.ratings)
        assertFalse(season1.failed)
        assertEquals(season1, repository.getSeasonRatings("tt7631058", 84773, 1, setOf(1, 2, 3, 4)))
        assertEquals(EpisodeRating(8.0, EpisodeRatingSource.TMDB),
            repository.getSeasonRatings("tt7631058", 84773, 2, setOf(1)).ratings[2 to 1])
        coVerify(exactly = 1) { tmdb.getTvSeasonDetails(84773, 1, any(), null) }
        coVerify(exactly = 0) { primary.getSeasonRatings(any()) }
        coVerify(exactly = 0) { graph.getSeasonRatings(any()) }
    }

    @Test
    fun `partial responses keep the origin of each score in priority order`() = runBlocking {
        val primary = mockk<ImdbTapframeApi>()
        val graph = mockk<SeriesGraphApi>()
        val tmdb = mockk<TmdbApi>()
        coEvery { primary.getSeasonRatings("tt7631058") } returns Response.success(backup)
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
        val result = repository(primary, graph, tmdb).apply { primaryConfigured = true }
            .getSeasonRatings("imdb:TT7631058:1:1", 84773, 1, setOf(1, 2, 3))
        assertEquals(mapOf(
            (1 to 1) to EpisodeRating(8.1, EpisodeRatingSource.IMDB),
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
        val ratings = repository(tmdb = tmdb).apply { seriesGraphConfigured = false }
        assertTrue(ratings.getSeasonRatings(null, 84773, 1, setOf(1)).failed)
        assertFalse(ratings.getSeasonRatings(null, 84773, 1, setOf(1)).failed)
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
            val ratings = repository(tmdb = tmdb).apply { seriesGraphConfigured = false }
            val first = async { ratings.getSeasonRatings(null, 84773, 1, setOf(1)) }
            started.await()
            val other = async(start = CoroutineStart.UNDISPATCHED) { ratings.getSeasonRatings(null, 84773, 1, setOf(1)) }
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
