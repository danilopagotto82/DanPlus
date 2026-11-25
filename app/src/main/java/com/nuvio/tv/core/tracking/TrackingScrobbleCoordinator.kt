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

    internal suspend fun complete(event: TrackingScrobbleEvent): TrackingCompletionResult {
        if (!event.isCurrentOwner()) return TrackingCompletionResult(emptySet(), complete = false)
        val connected = providerRegistry.connectedScrobblers()
        val targets = connected.filter { it.providerId !in event.skippedProviders }
        val failures = dispatch(targets, TrackingScrobbleAction.STOP, event)
        val failed = failures.mapTo(mutableSetOf()) { it.providerId }
        return TrackingCompletionResult(
            targets.mapTo(mutableSetOf()) { it.providerId } - failed,
            complete = connected.isNotEmpty() && failures.isEmpty() && event.isCurrentOwner(),
            failed = failed
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
    scrobbler.scrobble(action, event)
    null
} catch (error: Throwable) {
    currentCoroutineContext().ensureActive()
    TrackingScrobbleFailure(scrobbler.providerId, error)
}
