package com.nuvio.tv.ui.screens.player

import java.util.concurrent.atomic.AtomicReference

internal class RangeSeekHint {
    private class Seek(val url: String, val atMs: Long)
    private val pending = AtomicReference<Seek?>()

    fun record(url: String, nowMs: Long) {
        pending.set(Seek(url, nowMs))
    }

    fun hasPending(nowMs: Long): Boolean = activeSeek(nowMs) != null

    fun take(url: String, resolvedUrl: String?, nowMs: Long): Boolean {
        val seek = activeSeek(nowMs) ?: return false
        return (seek.url == url || seek.url == resolvedUrl) && pending.compareAndSet(seek, null)
    }

    private fun activeSeek(nowMs: Long): Seek? {
        val seek = pending.get() ?: return null
        if (nowMs - seek.atMs !in 0L..3_000L) {
            pending.compareAndSet(seek, null)
            return null
        }
        return seek
    }
}
