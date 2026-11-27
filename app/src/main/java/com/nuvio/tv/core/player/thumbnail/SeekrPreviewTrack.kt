package com.nuvio.tv.core.player.thumbnail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tv.seekr.previews.android.SeekrThumbnail
import tv.seekr.previews.android.SeekrTrack
import kotlin.math.abs

/** A single track and small shared tile cache for the timeline and chapters of one playback session. */
internal class SeekrPreviewTrack(private val track: SeekrTrack, first: SeekrThumbnail) {
    private val lock = Mutex()
    private val tiles = LinkedHashMap<Long, SeekrThumbnail>(MAX_TILES, .75f, true).apply {
        put(first.cueStartMs, first)
    }

    @Volatile
    var diagnostic = first.observed(fromCache = false)
        private set

    fun fitsChapters(durationMs: Long): Boolean = chapterDurationMatches(track.sourceDurationMs, durationMs)

    suspend fun thumbnailFor(positionMs: Long): SeekrThumbnail? = lock.withLock {
        val cachedKey = tiles.values.firstOrNull { it.covers(positionMs) }?.cueStartMs
        val cached = cachedKey?.let(tiles::get)
        if (cached != null && cached.usable()) {
            diagnostic = cached.observed(fromCache = true)
            return cached
        }
        cachedKey?.let(tiles::remove)
        val picture = try {
            track.thumbnailFor(positionMs)?.takeIf { it.covers(positionMs) && it.usable() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            diagnostic = ThumbnailDiagnostic(ThumbnailState.FAILED, ThumbnailOrigin.SEEKR)
            return null
        }
        currentCoroutineContext().ensureActive()
        if (picture == null) {
            diagnostic = ThumbnailDiagnostic(ThumbnailState.UNAVAILABLE)
        } else {
            tiles[picture.cueStartMs] = picture
            if (tiles.size > MAX_TILES) tiles.remove(tiles.keys.first())
            diagnostic = picture.observed(fromCache = false)
        }
        picture
    }

    private fun SeekrThumbnail.observed(fromCache: Boolean) = ThumbnailDiagnostic(
        ThumbnailState.AVAILABLE, ThumbnailOrigin.SEEKR, fromCache,
        (cueEndMs - cueStartMs).takeIf { it > 0 }
    )

    private companion object {
        const val MAX_TILES = 4
    }
}

internal fun SeekrThumbnail.usable(): Boolean = !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0

internal fun SeekrThumbnail.covers(positionMs: Long): Boolean = positionMs in cueStartMs until cueEndMs

/** The seek bar can tolerate a nearby rendition. Chapter art uses a tighter duration check. */
internal fun chapterDurationMatches(sourceMs: Long, playingMs: Long): Boolean =
    sourceMs > 0 && playingMs > 0 && abs(sourceMs - playingMs) <= 5_000L
