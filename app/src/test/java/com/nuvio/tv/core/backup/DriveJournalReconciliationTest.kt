package com.nuvio.tv.core.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveJournalReconciliationTest {
    private val scope = DriveCinemaScope("drive-a", "danplus-a", "profile-a")
    private val pin = "531602947284"
    private val base = DrivePortableSettings(null, scoutInMenu = true, scoutListEmojis = false)

    @Test
    fun mergesSeparatePreferencesAndAuthenticatesParentAndScope() {
        val root = append(DriveJournalState.Empty, base, null)
        val baseline = DriveJournalBaseline(root.id, base)
        val online = base.copy(scoutInMenu = false)
        val remote = append(current(root), online, baseline)
        val local = base.copy(scoutShowWatched = false)
        val merged = append(current(remote, root), local, baseline)
        assertEquals(setOf(remote.id), merged.parents)
        assertEquals(online.copy(scoutShowWatched = false), decodeDriveJournalEntry(merged, scope, pin))
        assertEquals(DriveJournalDecision.Invalid,
            reconcileDriveJournal(scope, pin, current(merged.copy(parents = setOf(root.id)), root), local, baseline))
        assertEquals(DriveJournalDecision.Invalid,
            reconcileDriveJournal(scope.copy(profile = "profile-b"), pin, current(merged, root, remote), local, baseline))
    }

    @Test
    fun absentBaselineOrDivergentHeadsNeverOverwriteAnExistingBackup() {
        val root = append(DriveJournalState.Empty, base, null)
        val changed = base.copy(scoutShowWatched = false)
        assertEquals(DriveJournalDecision.Conflict,
            reconcileDriveJournal(scope, pin, current(root), changed, null))
        assertEquals(DriveJournalDecision.Conflict,
            reconcileDriveJournal(scope, pin, DriveJournalState.Conflict(setOf("head-a", "head-b")), changed, DriveJournalBaseline(root.id, base)))
    }

    @Test
    fun remoteRewindDoesNotRestoreOlderSettings() {
        val root = append(DriveJournalState.Empty, base, null)
        val local = base.copy(scoutShowWatched = false)
        val previous = DriveJournalBaseline(root.id, base)
        val newer = append(current(root), local, previous)
        val localBaseline = DriveJournalBaseline(newer.id, local)
        assertEquals(DriveJournalDecision.Conflict,
            reconcileDriveJournal(scope, pin, current(root), local, localBaseline))
    }

    private fun current(entry: DriveJournalEntry, vararg ancestors: DriveJournalEntry): DriveJournalState.Current =
        DriveJournalState.Current(entry, setOf(entry.id) + ancestors.map { it.id })

    private fun append(
        state: DriveJournalState, local: DrivePortableSettings, baseline: DriveJournalBaseline?
    ): DriveJournalEntry =
        (reconcileDriveJournal(scope, pin, state, local, baseline) as DriveJournalDecision.Append).entry
}
