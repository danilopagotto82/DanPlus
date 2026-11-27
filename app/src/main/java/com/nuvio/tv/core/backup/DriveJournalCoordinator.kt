package com.nuvio.tv.core.backup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface DriveJournalStore {
    suspend fun reserveUploadId(): String
    suspend fun readJournal(): DriveJournalState
    suspend fun append(fileId: String, entry: DriveJournalEntry)
}

internal data class DriveJournalPending(val fileId: String, val entry: DriveJournalEntry)
internal data class DriveJournalProgress(
    val baseline: DriveJournalBaseline? = null,
    val pending: DriveJournalPending? = null
)

internal interface DriveJournalProgressStore {
    suspend fun load(scope: DriveCinemaScope): DriveJournalProgress
    suspend fun save(scope: DriveCinemaScope, progress: DriveJournalProgress)
}

internal enum class DriveJournalOutcome { IN_SYNC, AWAITING_REMOTE, CONFLICT, INVALID, UNAVAILABLE }

/** One coordinator owns the active profile. Callers pin profile generation in both callbacks. */
internal class DriveJournalCoordinator(
    private val remote: DriveJournalStore,
    private val localProgress: DriveJournalProgressStore
) {
    private val mutex = Mutex()

    suspend fun synchronize(
        scope: DriveCinemaScope,
        pin: String,
        snapshot: DrivePortableSettings,
        isCurrent: () -> Boolean,
        applyIfUnchanged: suspend (expected: DrivePortableSettings, replacement: DrivePortableSettings) -> Boolean
    ): DriveJournalOutcome = mutex.withLock {
        if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
        val progress = localProgress.load(scope)
        val state = remote.readJournal()
        if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
        val head = state as? DriveJournalState.Current
        val pending = progress.pending
        var baseline = progress.baseline
        if (pending != null) {
            if (state is DriveJournalState.Conflict) return@withLock DriveJournalOutcome.CONFLICT
            if (state == DriveJournalState.Invalid) return@withLock DriveJournalOutcome.INVALID
            if (head?.history?.contains(pending.entry.id) != true) {
                if (progress.baseline != null && head?.history?.contains(progress.baseline.revision) != true) {
                    return@withLock DriveJournalOutcome.CONFLICT
                }
                remote.append(pending.fileId, pending.entry)
                return@withLock DriveJournalOutcome.AWAITING_REMOTE
            }
            val acknowledged = runCatching { decodeDriveJournalEntry(pending.entry, scope, pin) }.getOrNull()
                ?: return@withLock DriveJournalOutcome.INVALID
            baseline = DriveJournalBaseline(pending.entry.id, acknowledged)
        }
        when (val decision = reconcileDriveJournal(scope, pin, state, snapshot, baseline)) {
            DriveJournalDecision.Conflict -> DriveJournalOutcome.CONFLICT
            DriveJournalDecision.Invalid -> DriveJournalOutcome.INVALID
            is DriveJournalDecision.Adopt -> {
                if (!isCurrent() || !applyIfUnchanged(snapshot, decision.settings) || !isCurrent()) {
                    return@withLock DriveJournalOutcome.UNAVAILABLE
                }
                val revision = checkNotNull(head).entry.id
                localProgress.save(scope, DriveJournalProgress(DriveJournalBaseline(revision, decision.settings)))
                DriveJournalOutcome.IN_SYNC
            }
            is DriveJournalDecision.Append -> {
                val upload = DriveJournalPending(remote.reserveUploadId(), decision.entry)
                if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
                localProgress.save(scope, DriveJournalProgress(baseline, upload))
                if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
                remote.append(upload.fileId, upload.entry)
                DriveJournalOutcome.AWAITING_REMOTE
            }
        }
    }
}
