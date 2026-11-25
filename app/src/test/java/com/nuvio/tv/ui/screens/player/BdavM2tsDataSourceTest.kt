package com.nuvio.tv.ui.screens.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

@androidx.annotation.OptIn(UnstableApi::class)
class BdavM2tsDataSourceTest {
    private val uri = mockk<Uri>(relaxed = true)
    private val ts = ByteArray(188 * 4) { index ->
        if (index % 188 == 0) 0x47 else (index % 64).toByte()
    }
    private val bdav = ByteArray(192 * 4) { index ->
        val packetOffset = index % 192
        if (packetOffset < 4) 0x75 else ts[index / 192 * 188 + packetOffset - 4]
    }

    @Test
    fun `Blu-ray prefixes are removed and reported size uses TS coordinates`() {
        val factory = BdavM2tsDataSourceFactory { ByteArrayDataSource(bdav) }
        val source = factory.createDataSource()
        try {
            assertEquals(ts.size.toLong(), source.open(spec()))
            assertArrayEquals(ts, source.readRemaining())
        } finally {
            source.close()
        }
    }

    @Test
    fun `seek on a new instance maps a bounded read crossing a packet boundary`() {
        val opens = mutableListOf<DataSpec>()
        val factory = BdavM2tsDataSourceFactory {
            val source = ByteArrayDataSource(bdav)
            object : DataSource by source {
                override fun open(dataSpec: DataSpec): Long {
                    opens += dataSpec
                    return source.open(dataSpec)
                }
            }
        }
        factory.createDataSource().let { source ->
            try {
                source.open(spec())
            } finally {
                source.close()
            }
        }
        val source = factory.createDataSource()
        try {
            assertEquals(8L, source.open(spec(185L, 8L)))
            assertEquals(189L, opens.last().position)
            assertEquals(12L, opens.last().length)
            assertArrayEquals(ts.copyOfRange(185, 193), source.readRemaining())
        } finally {
            source.close()
        }
    }

    @Test
    fun `ordinary TS wins when payload bytes also match the BDAV probe pattern`() {
        val ambiguous = ts.copyOf().apply {
            this[4] = 0x47
            this[196] = 0x47
            this[388] = 0x47
        }
        val source = BdavM2tsDataSourceFactory { ByteArrayDataSource(ambiguous) }.createDataSource()
        try {
            assertEquals(ambiguous.size.toLong(), source.open(spec()))
            assertArrayEquals(ambiguous, source.readRemaining())
        } finally {
            source.close()
        }
    }

    private fun spec(position: Long = 0, length: Long = C.LENGTH_UNSET.toLong()): DataSpec =
        DataSpec.Builder().setUri(uri).setPosition(position).setLength(length).build()

    private fun DataSource.readRemaining(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(213)
        while (true) {
            val count = read(buffer, 3, buffer.size - 3)
            if (count == C.RESULT_END_OF_INPUT) return output.toByteArray()
            output.write(buffer, 3, count)
        }
    }
}
