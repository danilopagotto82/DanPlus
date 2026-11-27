package com.nuvio.tv.ui.screens.player

import android.graphics.Bitmap
import com.nuvio.tv.core.player.thumbnail.ThumbnailDiagnostic
import com.nuvio.tv.core.player.thumbnail.ThumbnailOrigin
import com.nuvio.tv.core.player.thumbnail.ThumbnailState
import com.nuvio.tv.core.player.thumbnail.SeekrPreviews
import com.nuvio.tv.core.player.thumbnail.SeekrPreviewTrack
import com.nuvio.tv.core.player.thumbnail.chapterDurationMatches
import com.nuvio.tv.core.player.thumbnail.chooseThumbnailDiagnostic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import tv.seekr.previews.android.SeekrThumbnail
import tv.seekr.previews.android.SeekrTrack
import tv.seekr.previews.core.SeekrContent

class ChapterPreviewTest {
    @Test fun cacheSharesSeekrTilesAcrossRequestsAndEvictsOldCues() = runTest {
        val bitmap = mockk<Bitmap> {
            every { isRecycled } returns false
            every { width } returns 320
            every { height } returns 180
        }
        val source = mockk<SeekrTrack>()
        coEvery { source.thumbnailFor(any()) } coAnswers {
            val cueStartMs = firstArg<Long>() / 10_000L * 10_000L
            SeekrThumbnail(bitmap, cueStartMs, cueStartMs + 10_000L)
        }
        val track = SeekrPreviewTrack(source, SeekrThumbnail(bitmap, 0L, 10_000L))

        assertEquals(0L, track.thumbnailFor(500L)?.cueStartMs)
        assertTrue(track.diagnostic.fromCache)
        coVerify(exactly = 0) { source.thumbnailFor(500L) }
        for (cue in 1L..4L) track.thumbnailFor(cue * 10_000L)
        assertEquals(0L, track.thumbnailFor(500L)?.cueStartMs)
        assertFalse(track.diagnostic.fromCache)
        assertEquals(10_000L, track.diagnostic.intervalMs)
        coVerify(exactly = 1) { source.thumbnailFor(500L) }
    }

    @Test fun episodeIdentityKeepsTheShowSeasonAndEpisode() {
        val first = SeekrContent.Episode(showImdbId = "tt123", season = 2, episode = 1)
        val second = SeekrContent.Episode(showImdbId = "tt123", season = 2, episode = 2)
        assertEquals(first, SeekrPreviews.content("tt123:2:1", "series", 2, 1))
        assertEquals(second, SeekrPreviews.content("tt123:2:2", "series", 2, 2))
        assertNull(SeekrPreviews.content("tt123", "series", 2, null))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun scrollingCancelsTheObsoleteRequestAndKeepsOnlyTheNewWindow() = runTest {
        val targets = MutableStateFlow(listOf(0L, 10L))
        val previews = linkedMapOf<Long, String?>()
        val calls = mutableListOf<Long>()
        var cancelled = false
        val loader = launch {
            loadVisibleChapterPreviews(targets, previews) { position ->
                calls += position
                if (position == 10L) {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
                "frame $position"
            }
        }
        advanceTimeBy(101)
        assertEquals(listOf(0L, 10L), calls)
        targets.value = listOf(20L, 30L)
        runCurrent()
        advanceTimeBy(101)
        assertTrue(cancelled)
        assertEquals(mapOf(20L to "frame 20", 30L to "frame 30"), previews)
        loader.cancel()
    }
    @Test fun nearbyRenditionsMustHaveAKnownAndCompatibleDuration() {
        assertTrue(chapterDurationMatches(10_000_000, 10_005_000))
        assertTrue(chapterDurationMatches(10_005_000, 10_000_000))
        assertFalse(chapterDurationMatches(10_000_000, 10_005_001))
        assertFalse(chapterDurationMatches(0, 0))
        assertFalse(chapterDurationMatches(0, 10_000_000))
    }

    @Test fun requestsFocusFirstAndNeverHoldMoreThanFourVisibleChapters() {
        val visible = listOf(0L, 10L, 20L, 30L, 40L, 50L)
        assertEquals(listOf(40L, 0L, 10L, 20L), chapterPreviewPositions(visible, 40L, 50L))
        assertEquals(listOf(0L, 10L, 20L, 30L), chapterPreviewPositions(visible, 55L, 60L))
        assertEquals(listOf(50L), chapterPreviewPositions(emptyList(), null, 50L))
        assertTrue(chapterPreviewPositions(emptyList(), null, null).isEmpty())
    }

    @Test fun diagnosticReportsTheEffectiveSourceAndItsCache() {
        val local = ThumbnailDiagnostic(ThumbnailState.AVAILABLE, ThumbnailOrigin.LOCAL, fromCache = true, intervalMs = 30_000)
        val seekr = ThumbnailDiagnostic(ThumbnailState.AVAILABLE, ThumbnailOrigin.SEEKR, intervalMs = 10_000)
        assertEquals(seekr, chooseThumbnailDiagnostic(seekr, local))
        assertEquals(local, chooseThumbnailDiagnostic(ThumbnailDiagnostic(ThumbnailState.GENERATING), local))
        assertEquals(local, chooseThumbnailDiagnostic(ThumbnailDiagnostic(ThumbnailState.FAILED), local))
        assertEquals(ThumbnailDiagnostic(ThumbnailState.UNAVAILABLE), chooseThumbnailDiagnostic(null, null))
    }

    @Test fun thumbnailsAreBetweenNetworkAndSystem() {
        val sections = buildStatsSections(listOf(StatsRow("Memory", "120 MB"), StatsRow("Thumbnail state", "Available"), StatsRow("Buffer", "5 s")))
        assertEquals(listOf(StatsGroup.NETWORK, StatsGroup.THUMBNAILS, StatsGroup.SYSTEM), sections.map { it.group })
    }
}
