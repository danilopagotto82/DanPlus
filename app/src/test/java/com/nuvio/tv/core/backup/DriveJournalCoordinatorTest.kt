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
