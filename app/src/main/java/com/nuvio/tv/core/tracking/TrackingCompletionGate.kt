package com.nuvio.tv.core.tracking

import java.util.UUID

internal data class TrackingCompletionResult(
    val accepted: Set<TrackingProviderId>,
    val complete: Boolean,
    val pending: Set<TrackingProviderId> = emptySet(),
    val accounts: Map<TrackingProviderId, TrackingScrobbleAccount> = emptyMap(),
    val delegated: Set<TrackingProviderId> = emptySet()
)

/** One item in one player session. A retry skips providers that already handled the stop. */
internal class TrackingCompletionGate {
    data class Attempt(
        val generation: Long,
        val action: TrackingScrobbleAction,
        val accepted: Set<TrackingProviderId>,
        val accounts: Map<TrackingProviderId, TrackingScrobbleAccount>,
        val completionId: String
    )

    private var generation = 0L
    private var activeAction: TrackingScrobbleAction? = null
    private var deferredStopProgress: Float? = null
    private var complete = false
    private var accepted = emptySet<TrackingProviderId>()
    private var pending = emptySet<TrackingProviderId>()
    private var accounts = emptyMap<TrackingProviderId, TrackingScrobbleAccount>()
    private var completionId: String? = null

    val confirmedProviders: Set<TrackingProviderId> get() = accepted
    val isDispatching: Boolean get() = activeAction != null

    fun reset() {
        generation++
        activeAction = null
        deferredStopProgress = null
        complete = false
        accepted = emptySet()
        pending = emptySet()
        accounts = emptyMap()
        completionId = null
    }

    fun begin(action: TrackingScrobbleAction = TrackingScrobbleAction.STOP, progress: Float = 80f): Attempt? {
        if (complete) return null
        if (activeAction != null) {
            if (activeAction == TrackingScrobbleAction.PAUSE && action == TrackingScrobbleAction.STOP) {
                deferredStopProgress = maxOf(deferredStopProgress ?: progress, progress)
            }
            return null
        }
        activeAction = action
        val id = completionId ?: UUID.randomUUID().toString().also { completionId = it }
        return Attempt(++generation, action, accepted, accounts, id)
    }

    fun finish(attempt: Attempt, result: TrackingCompletionResult): Float? {
        if (!isCurrent(attempt)) return null
        activeAction = null
        accepted = accepted + result.accepted
        pending = (pending + result.pending) - result.accepted - result.delegated
        accounts = result.accounts + accounts
        complete = result.complete && pending.isEmpty()
        val nextProgress = deferredStopProgress
        deferredStopProgress = null
        return nextProgress?.takeUnless { complete }
    }

    fun isCurrent(attempt: Attempt): Boolean = activeAction == attempt.action && attempt.generation == generation
}
