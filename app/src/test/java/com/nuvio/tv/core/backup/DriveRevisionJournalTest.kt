package com.nuvio.tv.core.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveRevisionJournalTest {
    private val root = entry(null, "initial")

    @Test
    fun aSingleChainReturnsTheNewestEntryWithoutUsingDeviceClocks() {
        val second = entry(root.id, "second")
        val third = entry(second.id, "third")
        assertEquals(DriveJournalState.Current(third, setOf(root.id, second.id, third.id)), inspectDriveJournal(journal(third, root, second)))
    }

    @Test
    fun concurrentClientsPreserveBothBranches() {
        val firstClient = entry(root.id, "new theme")
        val secondClient = entry(root.id, "other preference")
        val files = journal(root, firstClient, secondClient)
        assertEquals(DriveJournalState.Conflict(setOf(firstClient.id, secondClient.id)), inspectDriveJournal(files))
        assertEquals(root, files[root.id])
        val resolved = entry(null, "manually confirmed").copy(parents = setOf(firstClient.id, secondClient.id))
        assertEquals(DriveJournalState.Current(resolved, files.keys + resolved.id), inspectDriveJournal(files + (resolved.id to resolved)))
    }

    @Test
    fun missingParentOrChangedPayloadStopsAdoption() {
        val child = entry(root.id, "original")
        assertEquals(DriveJournalState.Invalid, inspectDriveJournal(journal(child)))
        assertEquals(DriveJournalState.Invalid,
            inspectDriveJournal(mapOf(root.id to root, child.id to entry(root.id, "changed"))))
    }

    private fun entry(parent: String?, data: String) =
        DriveJournalEntry(setOfNotNull(parent), SealedBox("PBKDF2WithHmacSHA256", 600_000, "salt", "iv", data))

    private fun journal(vararg entries: DriveJournalEntry) = entries.associateBy { it.id }
}
