package com.nuvio.tv.data.trailer

import android.os.SystemClock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Trailer links that failed in the player. Caches skip them and the screen that owns one looks it up again. */
object TrailerPlaybackFailures {
    private const val MAX_REMEMBERED = 200

    private const val RETRY_AFTER_MS = 2 * 60_000L

    private val failed = LinkedHashMap<String, Long>()
    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun report(videoUrl: String) = report(videoUrl, SystemClock.elapsedRealtime())

    internal fun report(videoUrl: String, nowMs: Long) {
        if (videoUrl.isBlank()) return
        synchronized(failed) {
            failed.remove(videoUrl)
            failed[videoUrl] = nowMs + RETRY_AFTER_MS
            while (failed.size > MAX_REMEMBERED) failed.remove(failed.keys.first())
        }
        _events.tryEmit(videoUrl)
    }

    fun hasFailed(videoUrl: String): Boolean = hasFailed(videoUrl, SystemClock.elapsedRealtime())

    internal fun hasFailed(videoUrl: String, nowMs: Long): Boolean = synchronized(failed) {
        val retryAfter = failed[videoUrl] ?: return@synchronized false
        if (nowMs < retryAfter) return@synchronized true
        failed.remove(videoUrl)
        false
    }
}
