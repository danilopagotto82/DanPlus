package com.nuvio.tv.core.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class DriveJournalCoordinatorTest {
    private val scope = DriveCinemaScope("drive", "app", "profile")
    private val pin = "864275109638"
    private val settings = DrivePortableSettings(null, scoutInMenu = true, scoutListEmojis = false)

    @Test
    fun lostConnectionReusesReservationAfterRestartAndNeverAdoptsUnconfirmedUpload() = runBlocking {
        val remote = FakeRemote()
        val progress = MemoryProgress()
        var applied = 0
        suspend fun sync() = DriveJournalCoordinator(remote, progress).synchronize(scope, pin, settings, { true }) { _, _ ->
            applied++
            true
        }
        remote.fail = true
        val failure = runCatching { sync() }.exceptionOrNull()
        assertEquals("offline", failure?.message)
        assertEquals("file-1", progress.value.pending?.fileId)
        assertEquals(0, applied)

        remote.fail = false
        assertEquals(DriveJournalOutcome.AWAITING_REMOTE, sync())
        assertEquals(listOf("file-1", "file-1"), remote.attempts)
        assertEquals(0, applied)
        assertEquals(DriveJournalOutcome.IN_SYNC, sync())
        assertEquals(1, applied)
        assertNull(progress.value.pending)
    }

    @Test
    fun changedLocalStateIsNotOverwrittenWhenTheSnapshotIsApplied() = runBlocking {
        val remote = FakeRemote()
        val progress = MemoryProgress()
        val coordinator = DriveJournalCoordinator(remote, progress)
        assertEquals(DriveJournalOutcome.AWAITING_REMOTE,
            coordinator.synchronize(scope, pin, settings, { true }) { _, _ -> error("Too early") })
        assertEquals(DriveJournalOutcome.UNAVAILABLE,
            coordinator.synchronize(scope, pin, settings, { true }) { _, _ -> false })
        assertNull(progress.value.baseline)
    }

    @Test
    fun protectedStorageRepresentationRoundTripsAndRejectsOtherProfileBindings() {
        val entry = DriveJournalEntry(emptySet(), SealedBox("kdf", 600_000, "salt", "iv", "opaque"))
        val progress = DriveJournalProgress(DriveJournalBaseline(entry.id, settings), DriveJournalPending("reserved", entry))
        val encoded = encodeProgress(scope, progress)
        assertEquals(progress, decodeProgress(scope, encoded))
        assertEquals(DriveJournalProgress(), decodeProgress(scope.copy(profile = "other"), encoded))
    }

    @Test
    fun confirmedRemoteChoiceIsAppliedOnlyAfterAcknowledgement() = runBlocking {
        val remote = FakeRemote()
        val progress = MemoryProgress()
        val first = DriveJournalCoordinator(remote, progress)
        first.synchronize(scope, pin, settings, { true }) { _, _ -> true }
        first.synchronize(scope, pin, settings, { true }) { _, _ -> true }
        val otherProgress = MemoryProgress()
        val other = DriveJournalCoordinator(remote, otherProgress)
        val changed = settings.copy(scoutShowWatched = false)
        val preview = checkNotNull(other.previewConflict(scope, pin, changed, { true }))
        assertEquals(settings, preview.remote.values.single())
        assertEquals(DriveJournalOutcome.CONFLICT,
            other.resolveConflict(scope, pin, preview, preview.remote.keys.single(), { true }) { settings })
        assertEquals(DriveJournalOutcome.AWAITING_REMOTE,
            other.resolveConflict(scope, pin, preview, preview.remote.keys.single(), { true }) { changed })
        assertEquals(changed, otherProgress.value.pending?.resolutionExpectedLocal)
        assertEquals(otherProgress.value, decodeProgress(scope, encodeProgress(scope, otherProgress.value)))
        val newer = changed.copy(scoutListEmojis = true)
        assertEquals(DriveJournalOutcome.CONFLICT,
            other.synchronize(scope, pin, newer, { true }) { _, _ -> error("No overwrite") })
        assertEquals(DriveJournalOutcome.IN_SYNC,
            other.synchronize(scope, pin, changed, { true }) { expected, replacement ->
                assertEquals(changed, expected)
                assertEquals(settings, replacement)
                true
            })
        assertNull(otherProgress.value.pending)
    }

    @Test
    fun simultaneousHeadsArePreservedAndStalePreviewCannotResolve() = runBlocking {
        val remote = FakeRemote()
        val progress = MemoryProgress()
        val coordinator = DriveJournalCoordinator(remote, progress)
        coordinator.synchronize(scope, pin, settings, { true }) { _, _ -> true }
        coordinator.synchronize(scope, pin, settings, { true }) { _, _ -> true }
        val root = checkNotNull(progress.value.baseline)
        val left = (reconcileDriveJournal(scope, pin, remote.readJournal(), settings.copy(favoriteGenre = 18), root) as DriveJournalDecision.Append).entry
        val right = (reconcileDriveJournal(scope, pin, remote.readJournal(), settings.copy(favoriteGenre = 28), root) as DriveJournalDecision.Append).entry
        remote.append(remote.reserveUploadId(), left)
        remote.append(remote.reserveUploadId(), right)
        val preview = checkNotNull(coordinator.previewConflict(scope, pin, settings, { true }))
        assertEquals(setOf(left.id, right.id), preview.remote.keys)
        val third = (reconcileDriveJournal(scope, pin, DriveJournalState.Current(left, setOf(root.revision, left.id)),
            settings.copy(favoriteGenre = 35), DriveJournalBaseline(left.id, settings.copy(favoriteGenre = 18))) as DriveJournalDecision.Append).entry
        remote.append(remote.reserveUploadId(), third)
        assertEquals(DriveJournalOutcome.CONFLICT,
            coordinator.resolveConflict(scope, pin, preview, right.id, { true }) { settings })
        val fresh = checkNotNull(coordinator.previewConflict(scope, pin, settings, { true }))
        assertEquals(DriveJournalOutcome.AWAITING_REMOTE,
            coordinator.resolveConflict(scope, pin, fresh, right.id, { true }) { settings })
        val current = remote.readJournal() as DriveJournalState.Current
        assertEquals(setOf(third.id, right.id), current.entry.parents)
        assertEquals(DriveJournalOutcome.IN_SYNC,
            coordinator.synchronize(scope, pin, settings, { true }) { _, replacement ->
                assertEquals(28, replacement.favoriteGenre); true
            })
    }

    @Test
    fun fullJournalStopsNewUploadsButCanStillReadTheCurrentSettings() = runBlocking {
        val root = (reconcileDriveJournal(scope, pin, DriveJournalState.Empty, settings, null) as DriveJournalDecision.Append).entry
        val history = (1 until MAX_DRIVE_REVISIONS).map { it.toString() }.toSet() + root.id
        val progress = MemoryProgress().also { it.value = DriveJournalProgress(DriveJournalBaseline(root.id, settings)) }
        val remote = object : DriveJournalStore {
            override suspend fun reserveUploadId(): String = error("No reservation at capacity")
            override suspend fun readJournal() = DriveJournalState.Current(root, history)
            override suspend fun append(fileId: String, entry: DriveJournalEntry) = error("No append at capacity")
        }
        val coordinator = DriveJournalCoordinator(remote, progress)
        assertEquals(DriveJournalOutcome.CAPACITY,
            coordinator.synchronize(scope, pin, settings.copy(scoutShowWatched = false), { true }) { _, _ -> error("No apply") })
        assertEquals(DriveJournalOutcome.IN_SYNC,
            coordinator.synchronize(scope, pin, settings, { true }) { _, _ -> true })
    }

    private class MemoryProgress : DriveJournalProgressStore {
        var value = DriveJournalProgress()
        override suspend fun load(scope: DriveCinemaScope) = value
        override suspend fun save(scope: DriveCinemaScope, progress: DriveJournalProgress) {
            value = progress
        }
    }

    private class FakeRemote : DriveJournalStore {
        private val records = linkedMapOf<String, DriveJournalEntry>()
        val attempts = mutableListOf<String>()
        var fail = false
        private var counter = 0

        override suspend fun reserveUploadId() = "file-${++counter}"
        override suspend fun readJournal() = inspectDriveJournal(records.values.associateBy { it.id })
        override suspend fun append(fileId: String, entry: DriveJournalEntry) {
            attempts += fileId
            if (fail) throw IOException("offline")
            check(records.putIfAbsent(fileId, entry).let { it == null || it == entry })
        }
    }
}
