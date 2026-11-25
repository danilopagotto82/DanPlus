package com.nuvio.tv.ui.screens.player

/** Scope and monotonic deadline of a single automatic source recovery. */
internal class SourceFailoverWindow(
    val requestKey: String,
    val profileId: Int,
    val addonName: String,
    startedAtMs: Long
) {
    companion object {
        const val TIMEOUT_MS = 120_000L
    }

    val attemptedKeys = mutableSetOf<String>()
    private val deadlineMs = startedAtMs + TIMEOUT_MS
    var positionMs: Long? = null

    fun matches(requestKey: String?, profileId: Int, addonName: String?): Boolean =
        this.requestKey == requestKey && this.profileId == profileId && this.addonName == addonName

    fun remainingMs(nowMs: Long): Long = (deadlineMs - nowMs).coerceAtLeast(0L)
}
