package com.nuvio.tv.core.tracking

internal data class TrackingCompletionResult(
    val accepted: Set<TrackingProviderId>,
    val complete: Boolean,
    val failed: Set<TrackingProviderId> = emptySet()
)

/** One item in one player session. A retry skips providers that already handled the stop. */
internal class TrackingCompletionGate {
    data class Attempt(val generation: Long, val accepted: Set<TrackingProviderId>)

    private var generation = 0L
    private var active = false
    private var complete = false
    private var accepted = emptySet<TrackingProviderId>()
    private var pending = emptySet<TrackingProviderId>()

    fun reset() {
        generation++
        active = false
        complete = false
        accepted = emptySet()
        pending = emptySet()
    }

    fun begin(): Attempt? {
        if (active || complete) return null
        active = true
        return Attempt(++generation, accepted)
    }

    fun finish(attempt: Attempt, result: TrackingCompletionResult) {
        if (!isCurrent(attempt)) return
        active = false
        accepted = accepted + result.accepted
        pending = (pending + result.failed) - result.accepted
        complete = result.complete && pending.isEmpty()
    }

    fun isCurrent(attempt: Attempt): Boolean = active && attempt.generation == generation
}
