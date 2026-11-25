package com.nuvio.tv.ui.screens.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object BlurayMediaSources {
    private val rangeHeader = Regex("""bytes=([0-9]+)-([0-9]*)""", RegexOption.IGNORE_CASE)

    fun extractors(
        bdav: BdavM2tsDataSourceFactory,
        fallback: ExtractorsFactory,
        decorate: (ExtractorsFactory) -> ExtractorsFactory
    ): ExtractorsFactory {
        val disc = decorate(ExtractorsFactory { arrayOf<Extractor>(newExtractor()) })
        return object : ExtractorsFactory {
            override fun createExtractors(): Array<Extractor> =
                (if (bdav.hasDetectedBdav) disc else fallback).createExtractors()

            override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
                (if (bdav.hasDetectedBdav) disc else fallback).createExtractors(uri, responseHeaders)
        }
    }

    private fun newExtractor(clockStartTicks: Long? = null, onLpcmIgnored: () -> Unit = {}): TsExtractor = TsExtractor(
        TsExtractor.MODE_SINGLE_PMT,
        0,
        DefaultSubtitleParserFactory(),
        TimestampAdjuster(0).apply {
            // CLPI uses 45 kHz, PES uses 90 kHz. Preserve the same zero as the MPLS intervals
            // even when the first PES decoded in this clip starts before or after that point.
            if (clockStartTicks != null) adjustTsTimestamp(clockStartTicks * 2L)
        },
        BdavTsPayloadReaderFactory.withDefaults(
            DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
                DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM,
            onLpcmIgnored
        ),
        1_900 * 188
    )

    fun create(
        sourceUrl: String,
        titleId: String,
        clips: List<BlurayPlaylistResolver.Clip>,
        headers: Map<String, String>,
        metadata: MediaMetadata,
        decorate: (ExtractorsFactory) -> ExtractorsFactory,
        onLpcmIgnored: () -> Unit
    ): MediaSource {
        val clipUrls = clips.mapTo(mutableSetOf()) { it.url }
        val client = PlayerPlaybackNetworking.playbackHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.toString() !in clipUrls) throw IOException("Invalid disc clip URL")
                val range = request.header("Range")?.let(rangeHeader::matchEntire)
                val start = range?.groupValues?.get(1)?.toLongOrNull()
                val response = chain.proceed(request)
                if (start != null && start > 0 && !BlurayPlaylistResolver.supportsRange(
                        response.code, response.header("Content-Range"),
                        requiredStart = start, requiredEnd = range?.groupValues?.get(2)?.toLongOrNull()
                    )
                ) {
                    response.close()
                    throw BlurayPlaylistResolver.DiscException(BlurayPlaylistResolver.Reason.RANGE)
                }
                response
            }
            .build()
        val http = OkHttpDataSource.Factory(client)
            .setDefaultRequestProperties(PlayerMediaSourceFactory.sanitizeHeaders(headers))
        val transfer = PlaybackByteCounter.beginSource(sourceUrl, PlaybackTransferCoverage.PROGRESSIVE)
        val raw: DataSource.Factory = CountingDataSourceFactory(http, transfer)
        val reportedLpcm = AtomicBoolean()
        val playlist = ConcatenatingMediaSource2.Builder().setMediaItem(
            MediaItem.Builder().setMediaId(titleId).setUri(sourceUrl).setMediaMetadata(metadata).build()
        )
        for (clip in clips) {
            val bdav = BdavM2tsDataSourceFactory(requireBdav = true, upstream = raw)
            val extractors = ExtractorsFactory {
                arrayOf<Extractor>(newExtractor(clip.clockStartTicks) {
                    if (reportedLpcm.compareAndSet(false, true)) onLpcmIgnored()
                })
            }
            val factory = ProgressiveMediaSource.Factory(bdav, decorate(extractors))
            val item = MediaItem.Builder().setUri(clip.url).setMimeType(MimeTypes.VIDEO_MP2T).build()
            val clipped = ClippingMediaSource.Builder(factory.createMediaSource(item))
                .setStartPositionUs(clip.startUs)
                .setEndPositionUs(clip.endUs)
                .build()
            val durationMs = ((clip.endUs - clip.startUs) / 1_000).coerceAtLeast(1)
            playlist.add(clipped, durationMs)
        }
        return playlist.build()
    }
}
