package com.nuvio.tv.data.repository

import com.nuvio.tv.core.tracking.TrackingExternalIds
import com.nuvio.tv.core.tracking.TrackingMediaKind
import com.nuvio.tv.core.tracking.TrackingMediaReference
import com.nuvio.tv.core.tracking.TrackingScrobbleAction
import com.nuvio.tv.core.tracking.TrackingScrobbleEvent
import com.nuvio.tv.core.tracking.TrackingScrobbleAccount
import com.nuvio.tv.core.tracking.TrackingScrobbleOutcome
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class TraktTrackingScrobblerTest {
    @Test
    fun `pause preserves trakt stop behavior`() = runTest {
        val service = mockk<TraktScrobbleService>()
        val account = TrackingScrobbleAccount(1, 1)
        coEvery { service.currentScrobbleAccount() } returns account
        coEvery { service.sendTrackedScrobble("stop", any(), 45f, account) } returns TrackingScrobbleOutcome.CONFIRMED
        val scrobbler = TraktTrackingScrobbler(
            service = service,
            episodeMappingService = mockk(relaxed = true)
        )

        val result = scrobbler.scrobble(
            action = TrackingScrobbleAction.PAUSE,
            event = TrackingScrobbleEvent(
                media = TrackingMediaReference(
                    kind = TrackingMediaKind.MOVIE,
                    ids = TrackingExternalIds(imdb = "tt0111161")
                ),
                progressPercent = 45.0
            )
        )

        assertEquals(TrackingScrobbleOutcome.CONFIRMED, result)
        coVerify(exactly = 1) { service.sendTrackedScrobble("stop", any(), 45f, account) }
    }

    @Test
    fun `an unresolved identity is ignored without sending`() = runTest {
        val service = mockk<TraktScrobbleService>()
        coEvery { service.currentScrobbleAccount() } returns TrackingScrobbleAccount(1, 1)
        val scrobbler = TraktTrackingScrobbler(service, mockk())

        val result = scrobbler.scrobble(
            TrackingScrobbleAction.STOP,
            TrackingScrobbleEvent(
                media = TrackingMediaReference(kind = TrackingMediaKind.MOVIE, ids = TrackingExternalIds()),
                progressPercent = 96.0,
                profileId = 1
            )
        )

        assertEquals(TrackingScrobbleOutcome.IGNORED, result)
        coVerify(exactly = 0) { service.sendTrackedScrobble(any(), any(), any(), any()) }
    }
}
