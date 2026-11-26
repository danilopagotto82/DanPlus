package com.nuvio.tv.core.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.trailer.TrailerPlaybackSource
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

class TrailerPlayerPoolOwnershipTest {
    private val revision = MutableStateFlow(1L)
    private val source = TrailerPlaybackSource("https://example.test/e09-trailer.mp4")
    private val mediaItem = mockk<MediaItem>()

    @Before fun stubMediaItem() {
        mockkStatic(MediaItem::class)
        every { MediaItem.fromUri(source.videoUrl) } returns mediaItem
    }

    @After fun restoreMediaItem() = unmockkStatic(MediaItem::class)

    private fun pool(player: ExoPlayer): TrailerPlayerPool {
        val settings = mockk<PlayerSettingsDataStore>(relaxed = true) {
            every { nuvioPerformanceModeEnabled } returns flowOf(false)
            every { playerSettings } returns flowOf(com.nuvio.tv.data.local.PlayerSettings())
        }
        val profiles = mockk<ProfileManager> {
            every { profileSelectionRevision } returns revision
        }
        return TrailerPlayerPool(mockk(relaxed = true), settings, mockk(relaxed = true), profiles).also {
            TrailerPlayerPool::class.java.getDeclaredField("_player").apply { isAccessible = true }.set(it, player)
        }
    }

    @Test fun `outgoing screen disposal cannot stop the incoming preview`() {
        val player = mockk<ExoPlayer>(relaxed = true)
        val pool = pool(player)
        val outgoing = Any(); val incoming = Any()
        assertSame(player, pool.acquire(outgoing))
        assertSame(player, pool.acquire(incoming))
        pool.stop(outgoing)
        verify(exactly = 0) { player.stop(); player.clearMediaItems() }
        assertTrue(pool.isOwner(incoming))
        pool.stop(incoming)
        verify(exactly = 1) { player.stop(); player.clearMediaItems() }
        assertFalse(pool.isOwner(incoming))
    }

    @Test fun overlappingSurfacesAndResumeReusePreparedMedia() {
        val player = mockk<ExoPlayer>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.playerError } returns null
        val pool = pool(player)
        val home = Any()
        val detail = Any()

        pool.acquire(home)
        assertTrue(pool.prepare(home, source))
        pool.pause(home)
        pool.acquire(home)
        assertFalse(pool.prepare(home, source))
        pool.acquire(detail)
        assertFalse(pool.prepare(detail, source))
        pool.stop(home)

        assertTrue(pool.isOwner(detail))
        verify(exactly = 1) { player.prepare() }
        verify(exactly = 0) { player.stop() }
    }

    @Test fun nonOverlappingSurfacesRecoverPositionUntilProfileChanges() {
        val player = mockk<ExoPlayer>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 12_500L
        every { player.playerError } returns null
        val pool = pool(player)
        val home = Any()
        val detail = Any()

        pool.acquire(home)
        pool.prepare(home, source)
        pool.stop(home)
        pool.acquire(detail)
        pool.prepare(detail, source)
        verify(exactly = 1) { player.setMediaItem(mediaItem, 12_500L) }

        pool.stop(detail)
        revision.value = 2L
        pool.stopStaleProfile()
        revision.value = 3L
        assertNull(pool.acquire(home, revision = 1L))
        val newSelection = Any()
        pool.acquire(newSelection)
        pool.prepare(newSelection, source)
        verify(exactly = 2) { player.setMediaItem(mediaItem, 0L) }
        verify(exactly = 1) { player.setMediaItem(mediaItem, 12_500L) }
    }

    @Test fun `screensaver suppression refuses a new owner`() {
        val player = mockk<ExoPlayer>(relaxed = true)
        val pool = pool(player)
        pool.setScreensaverSuppressed(true)
        val owner = Any()
        assertNull(pool.acquire(owner))
        assertFalse(pool.isOwner(owner))
        pool.setScreensaverSuppressed(false)
        assertSame(player, pool.acquire(owner))
    }

    @Test fun `yield invalidates old screen cleanup`() {
        val player = mockk<ExoPlayer>(relaxed = true)
        val pool = pool(player)
        val owner = Any()
        pool.acquire(owner)
        pool.yield()
        assertFalse(pool.isOwner(owner))
        assertNull(pool.acquire(Any()))
        pool.stop(owner)
        verify(exactly = 1) { player.release(); player.stop() }
    }
}
