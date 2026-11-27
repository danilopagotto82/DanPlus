package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.data.local.partialProgressKeys
import com.nuvio.tv.domain.model.WatchProgress
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class WatchDateAndPartialProgressTest {
    @Test fun strictLocalDateRejectsFutureAndImpossibleDates() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-11-27T13:00:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-26T15:30:00Z").toEpochMilli(),
            watchDateMillis("2026-11-26 12:30", zone, now))
        assertNull(watchDateMillis("2026-11-28 12:30", zone, now))
        assertNull(watchDateMillis("2026-11-31 12:30", zone, now))
    }

    @Test fun removingPartialRewatchDoesNotRemoveCompletedAndNewerEntries() {
        val original = WatchProgress(
            contentId = "show", contentType = "series", name = "Synthetic", poster = null,
            backdrop = null, logo = null, videoId = "e2", season = 1, episode = 2,
            episodeTitle = null, position = 2000, duration = 10000, lastWatched = 200
        )
        val entries = linkedMapOf(
            "show" to original.copy(episode = 1, position = 10000),
            "show_s1e2" to original,
            "otherAlias" to original.copy(lastWatched = 300),
            "history" to original.copy(position = 10000, lastWatched = 100)
        )
        assertEquals(listOf("show_s1e2"), partialProgressKeys(entries, original))
        assertTrue(partialProgressKeys(entries, original.copy(position = 10000)).isEmpty())
    }
}
