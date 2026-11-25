package com.nuvio.tv.data.trailer

import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.local.TrailerSettings
import com.nuvio.tv.data.local.TrailerSettingsDataStore
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbDetailsResponse
import com.nuvio.tv.data.remote.api.TmdbVideoResult
import com.nuvio.tv.data.remote.api.TmdbVideosResponse
import com.nuvio.tv.domain.model.TmdbSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TrailerE09SelectionTest {
    private val tmdb = mockk<TmdbApi>()
    private val ids = mockk<TmdbService> { every { apiKey() } returns "fixture" }
    private val extractor = mockk<InAppYouTubeExtractor>(relaxed = true)
    private val service = TrailerService(
        trailerApi = mockk(),
        tmdbApi = tmdb,
        inAppYouTubeExtractor = extractor,
        tmdbSettingsDataStore = mockk<TmdbSettingsDataStore> {
            every { settings } returns MutableStateFlow(TmdbSettings(language = "en", useTrailers = true))
        },
        tmdbService = ids,
        imdbTrailerResolver = mockk(relaxed = true),
        trailerSettingsDataStore = mockk<TrailerSettingsDataStore> {
            every { settings } returns MutableStateFlow(TrailerSettings())
        },
        clock = Clock.systemUTC(),
        backendConfigured = false
    )

    @Test fun theTwoFilmsUseTheirOwnIdentityAndShareAHotLookupAcrossScreens() = runTest {
        val minoritySource = TrailerPlaybackSource("https://example.test/minority.mp4")
        val aiSource = TrailerPlaybackSource("https://example.test/ai.mp4", "https://example.test/ai.m4a")
        val minorityOriginal = video("aaaaaaaaaaa", "Official Trailer", "en", "US")
        val aiOriginal = video("bbbbbbbbbbb", "Official Trailer", "en", "US")
        val aiDub = video("ccccccccccc", "Trailer dublado", "pt", "BR")

        coEvery { ids.ensureTmdbId("tt0181689", "movie", null) } returns "180"
        coEvery { ids.ensureTmdbId("tt0212720", "movie", null) } returns "644"
        stubMovie(180, pt = emptyList(), en = listOf(minorityOriginal))
        stubMovie(644, pt = listOf(aiDub), en = listOf(aiOriginal))
        coEvery { extractor.extractPlaybackSource("https://www.youtube.com/watch?v=aaaaaaaaaaa") } returns minoritySource
        coEvery { extractor.extractPlaybackSource("https://www.youtube.com/watch?v=ccccccccccc") } returns aiSource

        val homeMinority = preview("Minority Report", "2002", "tt0181689")
        val detailsAi = preview("A.I. Inteligência Artificial", "2001", "tt0212720")
        val detailsMinority = preview("Minority Report — A Nova Lei", "2002", "tt0181689")
        val homeAi = preview("A.I. Artificial Intelligence", "2001", "tt0212720")

        assertEquals(minoritySource.videoUrl, homeMinority.source?.videoUrl)
        assertEquals(homeMinority.source, detailsMinority.source)
        assertEquals(aiSource.videoUrl, detailsAi.source?.videoUrl)
        assertEquals(aiSource.audioUrl, detailsAi.source?.audioUrl)
        assertEquals(detailsAi.source, homeAi.source)
        coVerify(exactly = 1) { tmdb.getMovieVideos(180, "fixture", "pt-BR") }
        coVerify(exactly = 1) { tmdb.getMovieVideos(644, "fixture", "pt-BR") }
        coVerify(exactly = 2) { extractor.extractPlaybackSource(any()) }
    }

    @Test fun evidenceOrdersDubThenSubThenOriginalAndRejectsRegionalAssumptions() {
        val original = video("aaaaaaaaaaa", "予告編", "ja", "JP")
        val english = video("bbbbbbbbbbb", "Official Trailer", "en", "US")
        val brazilMetadataOnly = video("ccccccccccc", "Trailer oficial", "pt", "BR")
        val portuguese = video("ddddddddddd", "Trailer dublado", "pt", "PT")
        val notDubbed = video("eeeeeeeeeee", "Trailer não dublado", "pt", "BR")
        val dub = video("fffffffffff", "Trailer Dublado", "pt", "BR")
        val sub = video("ggggggggggg", "Trailer Legendado Português do Brasil", "en", "US")
        val teaser = video("hhhhhhhhhhh", "Teaser dublado", "pt", "BR").copy(type = "Teaser")
        val extras = video("iiiiiiiiiii", "Entrevista dublada", "pt", "BR").copy(type = "Featurette")
        val malformed = video("bad?v=video", "Trailer dublado", "pt", "BR")

        val ranked = rankTmdbVideoCandidates(
            listOf(portuguese, notDubbed, teaser, original, english, brazilMetadataOnly, dub, sub, extras, malformed),
            originalLanguage = "ja",
            preferBrazilian = true
        )

        assertEquals(listOf(dub, sub, original), ranked.take(3))
        assertEquals(teaser, ranked.last())
        assertFalse(extras in ranked)
        assertFalse(malformed in ranked)
        val limited = rankTmdbVideoCandidates(
            listOf(dub, dub.copy(key = "jjjjjjjjjjj"), dub.copy(key = "kkkkkkkkkkk"), sub, original),
            originalLanguage = "ja",
            preferBrazilian = true,
            attemptLimit = 3
        )
        assertEquals(listOf(dub, sub, original), limited)
    }

    @Test fun anUntypedTmdbIdDoesNotGuessBetweenMovieAndSeries() = runTest {
        val result = service.lookupTrailer("Same name", "2002", "180", type = null)
        assertNull(result.source)
        coVerify(exactly = 0) { tmdb.getMovieVideos(any(), any(), any()) }
        coVerify(exactly = 0) { tmdb.getTvVideos(any(), any(), any()) }
    }

    @Test fun aRejectedDubTriesAnotherOfficialVariantWithoutUsingOtherFilms() = runTest {
        val dub = video("ddddddddddd", "Trailer dublado", "pt", "BR")
        val sub = video("sssssssssss", "Trailer legendado", "pt", "BR")
        val original = video("ooooooooooo", "Official Trailer", "en", "US")
        coEvery { ids.ensureTmdbId("tt0181689", "movie", null) } returns "180"
        stubMovie(180, pt = listOf(sub, dub), en = listOf(original))
        val dubUrl = "https://www.youtube.com/watch?v=ddddddddddd"
        val subUrl = "https://www.youtube.com/watch?v=sssssssssss"
        coEvery { extractor.extractPlaybackSource(dubUrl) } returns null
        every { extractor.unplayableReason(dubUrl) } returns YouTubeUnplayableReason.UNAVAILABLE
        coEvery { extractor.extractPlaybackSource(subUrl) } returns TrailerPlaybackSource("https://example.test/sub.mp4")

        val result = preview("Minority Report", "2002", "tt0181689")

        assertEquals("https://example.test/sub.mp4", result.source?.videoUrl)
        coVerify(exactly = 1) { extractor.extractPlaybackSource(dubUrl) }
        coVerify(exactly = 1) { extractor.extractPlaybackSource(subUrl) }
        coVerify(exactly = 0) { tmdb.getMovieVideos(644, any(), any()) }
    }

    @Test fun transientPlaybackFailuresExpire() {
        val url = "https://example.test/e09-retry.mp4"
        TrailerPlaybackFailures.report(url, nowMs = 1_000)
        assertTrue(TrailerPlaybackFailures.hasFailed(url, nowMs = 120_999))
        assertFalse(TrailerPlaybackFailures.hasFailed(url, nowMs = 121_000))
    }

    private fun stubMovie(id: Int, pt: List<TmdbVideoResult>, en: List<TmdbVideoResult>) {
        coEvery { tmdb.getMovieDetails(id, "fixture", null) } returns
            Response.success(TmdbDetailsResponse(id = id, originalLanguage = "en"))
        coEvery { tmdb.getMovieVideos(id, "fixture", "pt-BR") } returns
            Response.success(TmdbVideosResponse(id, pt))
        coEvery { tmdb.getMovieVideos(id, "fixture", "en-US") } returns
            Response.success(TmdbVideosResponse(id, en))
    }

    private suspend fun preview(title: String, year: String, imdbId: String): TrailerPreviewLookup =
        lookupTrailerPreview(service, ids, imdbId, "movie", title, year, imdbId, emptyList())

    private fun video(id: String, name: String, language: String, country: String) = TmdbVideoResult(
        iso6391 = language,
        iso31661 = country,
        name = name,
        key = id,
        site = "YouTube",
        type = "Trailer",
        official = true
    )
}
