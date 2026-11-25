package com.nuvio.tv.ui.screens.player

import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

/**
 * Presents Blu-ray's 192-byte packets to the TS extractor as 188-byte packets.
 * Each progressive media source keeps its detected layout when it reopens for a seek.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class BdavM2tsDataSourceFactory(
    private val requireBdav: Boolean = false,
    private val upstream: DataSource.Factory
) : DataSource.Factory {
    @Volatile private var bdavDetected: Boolean? = null
    val hasDetectedBdav: Boolean get() = bdavDetected == true

    override fun createDataSource(): DataSource = BdavM2tsDataSource(upstream.createDataSource())

    private inner class BdavM2tsDataSource(private val source: DataSource) : DataSource by source {
        private val buffer = ByteArray(16 * 1024)
        private var next = 0
        private var limit = 0
        private var rawPosition = 0L
        private var stripPrefixes = false

        override fun open(dataSpec: DataSpec): Long {
            next = 0
            limit = 0
            val layout = bdavDetected
            val start = dataSpec.position
            stripPrefixes = layout == true && dataSpec.uriPositionOffset == 0L
            rawPosition = if (stripPrefixes) toRawPosition(start) else start
            val requested = if (stripPrefixes) {
                dataSpec.buildUpon()
                    .setPosition(rawPosition)
                    .setLength(
                        if (dataSpec.length == C.LENGTH_UNSET.toLong()) dataSpec.length
                        else toRawPosition(Math.addExact(start, dataSpec.length)) - rawPosition
                    )
                    .build()
            } else dataSpec
            val rawLength = source.open(requested)

            // The initial unbounded read starts at byte zero. Bounded probes and nonzero
            // reads before layout detection are passed through without setting the layout.
            if (layout == null && start == 0L && dataSpec.uriPositionOffset == 0L &&
                dataSpec.length == C.LENGTH_UNSET.toLong()
            ) {
                while (limit < PROBE_SIZE) {
                    val read = source.read(buffer, limit, PROBE_SIZE - limit)
                    if (read == C.RESULT_END_OF_INPUT) break
                    limit += read
                }
                stripPrefixes = limit == PROBE_SIZE &&
                    hasSyncPattern(PREFIX_SIZE, RAW_PACKET_SIZE) &&
                    !hasSyncPattern(0, TS_PACKET_SIZE)
                bdavDetected = stripPrefixes
            }
            if (requireBdav && !stripPrefixes) throw java.io.IOException("Invalid BDAV packet layout")
            if (!stripPrefixes || rawLength == C.LENGTH_UNSET.toLong()) return rawLength
            return toTsPosition(Math.addExact(rawPosition, rawLength)) - start
        }

        private fun hasSyncPattern(start: Int, stride: Int): Boolean =
            (0..2).all { buffer[start + stride * it] == TS_SYNC_BYTE }

        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (!stripPrefixes && next == limit) return source.read(target, offset, length)
            var written = 0
            while (written < length) {
                if (next == limit) {
                    if (written > 0) return written
                    limit = source.read(buffer, 0, buffer.size)
                    if (limit == C.RESULT_END_OF_INPUT) {
                        limit = 0
                        next = 0
                        return C.RESULT_END_OF_INPUT
                    }
                    next = 0
                }
                val packetOffset = (rawPosition % RAW_PACKET_SIZE).toInt()
                if (stripPrefixes && packetOffset < PREFIX_SIZE) {
                    val skip = minOf(limit - next, PREFIX_SIZE - packetOffset)
                    next += skip
                    rawPosition += skip
                    continue
                }
                val available = if (stripPrefixes) minOf(limit - next, RAW_PACKET_SIZE - packetOffset)
                    else limit - next
                val count = minOf(length - written, available)
                buffer.copyInto(target, offset + written, next, next + count)
                next += count
                rawPosition += count
                written += count
            }
            return written
        }
    }

    private companion object {
        const val RAW_PACKET_SIZE = 192
        const val TS_PACKET_SIZE = 188
        const val PREFIX_SIZE = RAW_PACKET_SIZE - TS_PACKET_SIZE
        const val TS_SYNC_BYTE: Byte = 0x47
        const val PROBE_SIZE = PREFIX_SIZE + 2 * RAW_PACKET_SIZE + 1

        fun toRawPosition(position: Long): Long {
            val offset = position % TS_PACKET_SIZE
            val packetStart = Math.multiplyExact(position / TS_PACKET_SIZE, RAW_PACKET_SIZE.toLong())
            return if (offset == 0L) packetStart else Math.addExact(packetStart, PREFIX_SIZE + offset)
        }

        fun toTsPosition(position: Long): Long =
            position / RAW_PACKET_SIZE * TS_PACKET_SIZE +
                (position % RAW_PACKET_SIZE - PREFIX_SIZE).coerceAtLeast(0)
    }
}
