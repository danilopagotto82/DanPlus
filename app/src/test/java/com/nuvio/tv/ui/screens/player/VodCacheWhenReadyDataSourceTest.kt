package com.nuvio.tv.ui.screens.player

import android.net.Uri
import androidx.media3.common.ByteBufferDataReader
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.nuvio.tv.core.player.VodCacheSizing
import com.nuvio.tv.data.local.VodCacheSizeMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class VodCacheWhenReadyDataSourceTest {
    private interface DirectSource : DataSource, ByteBufferDataReader

    @Test
    fun coldPlaybackKeepsItsDirectRangeAndWarmReopenCanUseCachePastTwoGiB() {
        val uri = mockk<Uri>(relaxed = true)
        val spec = DataSpec.Builder().setUri(uri).setPosition(5L * GiB).setLength(4096L).build()
        val direct = mockk<DirectSource>(relaxed = true)
        val disk = mockk<DataSource>(relaxed = true)
        val listener = mockk<TransferListener>()
        val buffer = ByteBuffer.allocate(8)
        every { direct.open(spec) } returns 4096L
        every { direct.supportsByteBufferRead() } returns true
        every { direct.read(buffer, 8) } returns 8
        every { disk.open(spec) } returns 4096L
        every { disk.read(any(), any(), any()) } returns 4
        var ready = false
        val source = VodCacheWhenReadyDataSource(DataSource.Factory { direct }) {
            disk.takeIf { ready }
        }
        source.addTransferListener(listener)
        assertEquals(4096L, source.open(spec))
        ready = true
        assertTrue(source.supportsByteBufferRead())
        assertEquals(8, source.read(buffer, 8))
        verify(exactly = 0) { disk.open(any()) }
        source.close()

        assertEquals(4096L, source.open(spec))
        assertFalse(source.supportsByteBufferRead())
        assertEquals(4, source.read(ByteArray(4), 0, 4))
        source.close()
        verify(exactly = 1) { direct.addTransferListener(listener); direct.close() }
        verify(exactly = 1) { disk.addTransferListener(listener); disk.open(spec); disk.close() }
    }

    @Test
    fun formatChecksAndAdaptiveSourcesBypassDisk() {
        val url = "https://cdn.invalid/sample.mkv"
        assertTrue(PlayerMediaSourceFactory.shouldUsePlaybackVodCache(true, false, false, url))
        assertFalse(PlayerMediaSourceFactory.shouldUsePlaybackVodCache(true, true, false, url))
        assertFalse(PlayerMediaSourceFactory.shouldUsePlaybackVodCache(true, false, true, url))
        assertFalse(PlayerMediaSourceFactory.shouldUsePlaybackVodCache(false, false, false, url))
    }

    @Test
    fun cacheCapUsesLongSizesAndSmallStorageBypassesDisk() {
        val cap = VodCacheSizing.resolveMaxBytes(10 * GiB, VodCacheSizeMode.MANUAL, 16_384)
        assertEquals(9 * GiB, cap)
        assertEquals(0L, VodCacheSizing.resolveMaxBytes(8 * 1024L * 1024L, VodCacheSizeMode.AUTO, 0))
    }

    private companion object {
        const val GiB = 1024L * 1024L * 1024L
    }
}
