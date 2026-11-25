package com.nuvio.tv.core.tracking

import android.util.Log
import com.nuvio.tv.core.profile.ProfileManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope

data class TrackingScrobbleFailure(
    val providerId: TrackingProviderId,
    val cause: Throwable
)

@Singleton
class TrackingScrobbleCoordinator @Inject constructor(
    private val providerRegistry: TrackingProviderRegistry,
    private val profileManager: ProfileManager
) {
    private fun TrackingScrobbleEvent.isCurrentOwner(): Boolean =
        profileId == null || profileId == profileManager.activeProfileId.value

    suspend fun scrobble(
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent
    ): List<TrackingScrobbleFailure> {
        if (!event.isCurrentOwner()) return emptyList()
        val scrobblers = providerRegistry.connectedScrobblers()
        Log.d(
            TRACKING_SCROBBLE_DIAGNOSTIC_TAG,
            "coordinator dispatch action=${action.wireValue} providers=" +
                scrobblers.joinToString(prefix = "[", postfix = "]") { it.providerId.storageId } +
                " ${event.scrobbleDiagnosticSummary()}"
        )
        return dispatch(
            scrobblers = scrobblers,
            action = action,
            event = event
        ).also { failures ->
            Log.d(
                TRACKING_SCROBBLE_DIAGNOSTIC_TAG,
                "coordinator complete action=${action.wireValue} failures=" +
                    failures.joinToString(prefix = "[", postfix = "]") { it.providerId.storageId } +
                    " ${event.scrobbleDiagnosticSummary()}"
            )
        }
    }

    /** [restart] runs per tracker after its own stop, so it is read again each time and may return null to skip the start. */
    suspend fun scrobbleSeekRestart(
        stop: TrackingScrobbleEvent,
        restart: () -> TrackingScrobbleEvent?
    ): List<TrackingScrobbleFailure> = if (!stop.isCurrentOwner()) emptyList()
    else dispatchTrackingSeekRestart(providerRegistry.connectedScrobblers(), stop) { restart()?.takeIf { it.isCurrentOwner() } }
        .also { failures -> failures.log(TrackingScrobbleAction.STOP, true) }

    suspend fun scrobbleStopWhenLeftPaused(event: TrackingScrobbleEvent): List<TrackingScrobbleFailure> =
        if (!event.isCurrentOwner()) emptyList() else dispatchTrackingStopWhenLeftPaused(providerRegistry.connectedScrobblers(), event)
            .also { failures -> failures.log(TrackingScrobbleAction.STOP, false) }

    internal suspend fun complete(
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent,
        accounts: Map<TrackingProviderId, TrackingScrobbleAccount>,
        resolveDelegatedProviders: suspend () -> Set<TrackingProviderId>,
        isCurrent: () -> Boolean
    ): TrackingCompletionResult = supervisorScope {
        fun current() = isCurrent() && event.isCurrentOwner()
        if (!current()) return@supervisorScope TrackingCompletionResult(emptySet(), complete = false)
        val scrobblers = providerRegistry.connectedScrobblers()
        val snapshots = scrobblers.filter { it.providerId !in event.skippedProviders && it.providerId !in accounts }
            .map { tracker -> async { tracker.providerId to tracker.accountOrNull() } }.awaitAll().toMap()
        val pinned = snapshots.mapNotNull { (id, account) -> account?.let { id to it } }.toMap() + accounts
        if (!current()) return@supervisorScope TrackingCompletionResult(emptySet(), complete = false)
        val delegated = try {
            resolveDelegatedProviders()
        } catch (error: java.io.IOException) {
            Log.w("TrackingScrobble", "Could not resolve server scrobble delegation", error)
            return@supervisorScope TrackingCompletionResult(
                emptySet(), complete = false, pending = snapshots.keys, accounts = pinned
            )
        }
        val excluded = event.skippedProviders + delegated
        val unbound = snapshots.filterValues { it == null }.keys - excluded
        val receipt = dispatchTrackingCompletion(
            scrobblers.filterNot { it.providerId in unbound }, action,
            event.copy(skippedProviders = excluded), pinned, ::current
        )
        if (!current()) return@supervisorScope receipt
        receipt.copy(
            pending = receipt.pending + unbound,
            accounts = receipt.accounts + pinned.filterKeys { it !in excluded },
            delegated = delegated
        )
    }

    private suspend fun dispatch(
        scrobblers: Collection<TrackingScrobbler>,
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent
    ): List<TrackingScrobbleFailure> = dispatchTrackingScrobble(scrobblers, action, event)
        .also { failures -> failures.log(action, false) }

    private fun List<TrackingScrobbleFailure>.log(
        action: TrackingScrobbleAction,
        seek: Boolean
    ) {
        forEach { failure ->
            Log.w(
                "TrackingScrobble",
                "${failure.providerId.storageId} ${if (seek) "seek " else ""}scrobble ${action.wireValue} failed",
                failure.cause
            )
        }
    }
}

private suspend fun TrackingScrobbler.accountOrNull(): TrackingScrobbleAccount? =
    try {
        currentScrobbleAccount()
    } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }

private data class CompletionReceipt(
    val providerId: TrackingProviderId,
    val account: TrackingScrobbleAccount?,
    val confirmed: Boolean
)

internal suspend fun dispatchTrackingCompletion(
    scrobblers: List<TrackingScrobbler>,
    action: TrackingScrobbleAction,
    event: TrackingScrobbleEvent,
    accounts: Map<TrackingProviderId, TrackingScrobbleAccount>,
    isCurrent: () -> Boolean
): TrackingCompletionResult = supervisorScope {
    if (!isCurrent()) return@supervisorScope TrackingCompletionResult(emptySet(), complete = false)
    val targets = scrobblers.filter { it.providerId !in event.skippedProviders }
    val receipts = targets.map { scrobbler ->
        async {
            val prior = accounts[scrobbler.providerId]
            var owner = prior
            val outcome = try {
                val current = scrobbler.currentScrobbleAccount()
                owner = prior ?: current
                when {
                    !isCurrent() -> TrackingScrobbleOutcome.OWNER_CHANGED
                    current == null -> TrackingScrobbleOutcome.UNAVAILABLE
                    prior != null && prior != current || !event.belongsTo(current) ->
                        TrackingScrobbleOutcome.OWNER_CHANGED
                    else -> scrobbler.scrobble(action, event.copy(expectedAccount = current))
                        .let { outcome ->
                            if (outcome.isConfirmed && scrobbler.currentScrobbleAccount() != current) {
                                TrackingScrobbleOutcome.OWNER_CHANGED
                            } else outcome
                        }
                }
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                TrackingScrobbleOutcome.RETRYABLE_FAILURE
            }
            val canComplete = event.progressPercent >= scrobbler.completionPercent &&
                (action == TrackingScrobbleAction.STOP || scrobbler.completesOnPause)
            CompletionReceipt(scrobbler.providerId, owner, canComplete && outcome.isConfirmed)
        }
    }.awaitAll()
    if (!isCurrent()) return@supervisorScope TrackingCompletionResult(emptySet(), complete = false)
    val accepted = receipts.filter { it.confirmed }.mapTo(mutableSetOf(), CompletionReceipt::providerId)
    val pending = (targets.map(TrackingScrobbler::providerId) + accounts.keys).toSet() -
        event.skippedProviders - accepted
    TrackingCompletionResult(
        accepted = accepted,
        complete = (scrobblers.isNotEmpty() || accounts.isNotEmpty()) && pending.isEmpty(),
        pending = pending,
        accounts = receipts.mapNotNull { (id, account) -> account?.let { id to it } }.toMap()
    )
}

suspend fun dispatchTrackingScrobble(
    scrobblers: Collection<TrackingScrobbler>,
    action: TrackingScrobbleAction,
    event: TrackingScrobbleEvent
): List<TrackingScrobbleFailure> = supervisorScope {
    scrobblers.filter { it.providerId !in event.skippedProviders }.map { scrobbler ->
        async { scrobbleCatching(scrobbler, action, event) }
    }.awaitAll().filterNotNull()
}

suspend fun dispatchTrackingStopWhenLeftPaused(
    scrobblers: Collection<TrackingScrobbler>,
    event: TrackingScrobbleEvent
): List<TrackingScrobbleFailure> =
    dispatchTrackingScrobble(scrobblers.filter { it.stopsWhenLeftPaused }, TrackingScrobbleAction.STOP, event)

suspend fun dispatchTrackingSeekRestart(
    scrobblers: Collection<TrackingScrobbler>,
    stop: TrackingScrobbleEvent,
    restart: () -> TrackingScrobbleEvent?
): List<TrackingScrobbleFailure> = supervisorScope {
    scrobblers
        .filter { it.seekScrobblePolicy == TrackingSeekScrobblePolicy.STOP_AND_RESTART && it.providerId !in stop.skippedProviders }
        .map { scrobbler ->
            async {
                val stopFailure = scrobbleCatching(scrobbler, TrackingScrobbleAction.STOP, stop)
                val startFailure = restart()?.let { scrobbleCatching(scrobbler, TrackingScrobbleAction.START, it) }
                listOfNotNull(stopFailure, startFailure)
            }
        }
        .awaitAll()
        .flatten()
}

/** A tracker's own cancellation (an account switched mid-call) is its failure; only a cancelled caller stops the rest. */
private suspend fun scrobbleCatching(
    scrobbler: TrackingScrobbler,
    action: TrackingScrobbleAction,
    event: TrackingScrobbleEvent
): TrackingScrobbleFailure? = try {
    val outcome = scrobbler.scrobble(action, event)
    if (outcome == TrackingScrobbleOutcome.RETRYABLE_FAILURE) {
        TrackingScrobbleFailure(scrobbler.providerId, IllegalStateException("Scrobble was not acknowledged"))
    } else null
} catch (error: Throwable) {
    currentCoroutineContext().ensureActive()
    TrackingScrobbleFailure(scrobbler.providerId, error)
}
