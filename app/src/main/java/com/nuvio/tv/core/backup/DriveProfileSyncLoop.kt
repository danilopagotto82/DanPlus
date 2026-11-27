package com.nuvio.tv.core.backup

import com.nuvio.tv.data.local.ScoutStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/** Created only after the current account, profile and Drive grant have been verified. */
internal class DriveProfileSession(
    val scope: DriveCinemaScope,
    val localProfileId: Int,
    val pin: String,
    val coordinator: DriveJournalCoordinator,
    val isCurrent: () -> Boolean
)

internal class DriveProfileSyncLoop(
    private val scout: ScoutStore,
    private val remotePollMs: Long = 5 * 60_000L
) {
    init { require(remotePollMs > 0) }
    private val manual = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _outcome = MutableStateFlow<DriveJournalOutcome?>(null)
    val outcome = _outcome.asStateFlow()

    fun syncNow() { manual.tryEmit(Unit) }

    /** Supply null on sign-out, disablement, or when the app leaves its permitted lifecycle state. */
    @OptIn(FlowPreview::class)
    suspend fun run(sessions: Flow<DriveProfileSession?>) {
        sessions.collectLatest { session ->
            _outcome.value = null
            if (session == null || !session.isCurrent()) return@collectLatest
            val settings = scout.observeDriveSettings(session.localProfileId)
            var initial = true
            val changes = settings.debounce {
                if (initial) {
                    initial = false
                    0L
                } else 750L
            }.map { Unit }
            // A second TV can change the journal without changing this local DataStore.
            // Poll only during the authorized lifecycle; cancellation stops the timer.
            val remoteRefresh = flow {
                while (true) {
                    delay(remotePollMs)
                    emit(Unit)
                }
            }
            merge(changes, manual, remoteRefresh).collect {
                if (!session.isCurrent()) return@collect
                try {
                    for (attempt in 0..2) {
                        if (!session.isCurrent()) break
                        val snapshot = settings.first()
                        val result = session.coordinator.synchronize(
                            session.scope, session.pin, snapshot, session.isCurrent
                        ) { expected, replacement ->
                            scout.applyDriveSettings(session.localProfileId, expected, replacement, session.isCurrent)
                        }
                        if (!session.isCurrent()) break
                        _outcome.value = result
                        if (result != DriveJournalOutcome.AWAITING_REMOTE || attempt == 2) break
                        delay(1_000L shl attempt)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (session.isCurrent()) _outcome.value = DriveJournalOutcome.UNAVAILABLE
                }
            }
        }
    }
}
