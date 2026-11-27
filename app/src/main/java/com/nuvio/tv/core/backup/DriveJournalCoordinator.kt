package com.nuvio.tv.core.backup

import org.json.JSONArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface DriveJournalStore {
    suspend fun reserveUploadId(): String
    suspend fun readJournal(): DriveJournalState
    suspend fun append(fileId: String, entry: DriveJournalEntry)
}

internal data class DriveJournalPending(
    val fileId: String,
    val entry: DriveJournalEntry,
    val resolutionExpectedLocal: DrivePortableSettings? = null
)

internal data class DriveConflictPreview(
    val local: DrivePortableSettings,
    val remote: Map<String, DrivePortableSettings>
)
internal data class DriveJournalProgress(
    val baseline: DriveJournalBaseline? = null,
    val pending: DriveJournalPending? = null
)

internal interface DriveJournalProgressStore {
    suspend fun load(scope: DriveCinemaScope): DriveJournalProgress
    suspend fun save(scope: DriveCinemaScope, progress: DriveJournalProgress)
}

internal enum class DriveJournalOutcome { IN_SYNC, AWAITING_REMOTE, CONFLICT, CAPACITY, INVALID, UNAVAILABLE }

/** One coordinator owns the active profile. Callers pin profile generation in both callbacks. */
internal class DriveJournalCoordinator(
    private val remote: DriveJournalStore,
    private val localProgress: DriveJournalProgressStore
) {
    private val mutex = Mutex()

    /** Returns authenticated alternatives only. A rewind or missing pending revision is not resolvable by guessing. */
    suspend fun previewConflict(
        scope: DriveCinemaScope,
        pin: String,
        snapshot: DrivePortableSettings,
        isCurrent: () -> Boolean
    ): DriveConflictPreview? = mutex.withLock {
        if (!isCurrent()) return@withLock null
        val progress = localProgress.load(scope)
        val state = remote.readJournal()
        if (!isCurrent()) return@withLock null
        conflictPreview(scope, pin, snapshot, progress, state)
    }

    /** Null selectedRevision means keep the local settings. All current heads become authenticated parents. */
    suspend fun resolveConflict(
        scope: DriveCinemaScope,
        pin: String,
        preview: DriveConflictPreview,
        selectedRevision: String?,
        isCurrent: () -> Boolean,
        localSnapshot: suspend () -> DrivePortableSettings
    ): DriveJournalOutcome = mutex.withLock {
        if (!isCurrent() || localSnapshot() != preview.local) return@withLock DriveJournalOutcome.CONFLICT
        val progress = localProgress.load(scope)
        val state = remote.readJournal()
        if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
        if (state == DriveJournalState.CapacityExceeded || revisionCount(state) >= MAX_DRIVE_REVISIONS) {
            return@withLock DriveJournalOutcome.CAPACITY
        }
        val fresh = conflictPreview(scope, pin, preview.local, progress, state)
        if (fresh == null || fresh != preview) return@withLock DriveJournalOutcome.CONFLICT
        val chosen = if (selectedRevision == null) preview.local
            else preview.remote[selectedRevision] ?: return@withLock DriveJournalOutcome.CONFLICT
        val parents = preview.remote.keys.toSet()
        val payload = encodeDriveSettings(scope, chosen).put("parents", JSONArray(parents.sorted()))
        val entry = DriveJournalEntry(parents, BackupCrypto.seal(payload.toString(), pin))
        val upload = DriveJournalPending(remote.reserveUploadId(), entry, preview.local)
        if (!isCurrent() || localSnapshot() != preview.local) return@withLock DriveJournalOutcome.CONFLICT
        localProgress.save(scope, DriveJournalProgress(progress.baseline, upload))
        if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
        remote.append(upload.fileId, upload.entry)
        DriveJournalOutcome.AWAITING_REMOTE
    }

    private fun conflictPreview(
        scope: DriveCinemaScope,
        pin: String,
        local: DrivePortableSettings,
        progress: DriveJournalProgress,
        state: DriveJournalState
    ): DriveConflictPreview? {
        if (!BackupCrypto.isValidPin(pin)) return null
        val (heads, history) = when (state) {
            is DriveJournalState.Current -> mapOf(state.entry.id to state.entry) to state.history
            is DriveJournalState.Conflict -> {
                if (state.heads != state.entries.keys) return null
                state.entries to state.history
            }
            else -> return null
        }
        if (progress.baseline?.revision?.let { it !in history } == true ||
            progress.pending?.entry?.id?.let { it !in history } == true) return null
        val alternatives = try {
            heads.mapValues { (_, entry) -> decodeDriveJournalEntry(entry, scope, pin) }
        } catch (_: Exception) {
            return null
        }
        return DriveConflictPreview(local, alternatives)
    }

    private fun revisionCount(state: DriveJournalState): Int = when (state) {
        is DriveJournalState.Current -> state.history.size
        is DriveJournalState.Conflict -> state.history.size
        else -> 0
    }

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
        if (state == DriveJournalState.CapacityExceeded) return@withLock DriveJournalOutcome.CAPACITY
        val head = state as? DriveJournalState.Current
        val pending = progress.pending
        var baseline = progress.baseline
        if (pending != null) {
            if (state == DriveJournalState.Invalid) return@withLock DriveJournalOutcome.INVALID
            val history = when (state) {
                is DriveJournalState.Current -> state.history
                is DriveJournalState.Conflict -> state.history
                else -> emptySet()
            }
            if (pending.entry.id !in history) {
                if (history.size >= MAX_DRIVE_REVISIONS) return@withLock DriveJournalOutcome.CAPACITY
                if (progress.baseline?.revision?.let { it !in history } == true ||
                    !history.containsAll(pending.entry.parents)) return@withLock DriveJournalOutcome.CONFLICT
                if (!isCurrent()) return@withLock DriveJournalOutcome.UNAVAILABLE
                remote.append(pending.fileId, pending.entry)
                return@withLock DriveJournalOutcome.AWAITING_REMOTE
            }
            if (state is DriveJournalState.Conflict) return@withLock DriveJournalOutcome.CONFLICT
            val acknowledged = runCatching { decodeDriveJournalEntry(pending.entry, scope, pin) }.getOrNull()
                ?: return@withLock DriveJournalOutcome.INVALID
            baseline = DriveJournalBaseline(pending.entry.id, acknowledged)
            val expected = pending.resolutionExpectedLocal
            if (expected != null) {
                // A confirmed choice cannot silently erase edits made after the conflict dialog opened.
                if (head?.entry?.id != pending.entry.id || (snapshot != expected && snapshot != acknowledged)) {
                    return@withLock DriveJournalOutcome.CONFLICT
                }
                if (!isCurrent() || !applyIfUnchanged(snapshot, acknowledged) || !isCurrent()) {
                    return@withLock DriveJournalOutcome.UNAVAILABLE
                }
                localProgress.save(scope, DriveJournalProgress(baseline))
                return@withLock DriveJournalOutcome.IN_SYNC
            }
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
                if (revisionCount(state) >= MAX_DRIVE_REVISIONS) return@withLock DriveJournalOutcome.CAPACITY
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
