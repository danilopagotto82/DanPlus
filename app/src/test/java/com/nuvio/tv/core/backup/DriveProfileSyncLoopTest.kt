package com.nuvio.tv.core.backup

import com.nuvio.tv.data.local.ScoutStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveProfileSyncLoopTest {
    @Test
    fun manualOnlySessionWaitsForRequestAndKeepsLastSuccessfulSync() = runTest {
        val original = DrivePortableSettings(null, scoutInMenu = true, scoutListEmojis = false)
        val settings = MutableStateFlow(original)
        val scout = mockk<ScoutStore>()
        every { scout.observeDriveSettings(3) } returns settings
        val coordinator = mockk<DriveJournalCoordinator>()
        coEvery { coordinator.synchronize(any(), any(), any(), any(), any()) } returns DriveJournalOutcome.IN_SYNC
        val lastSuccess = 1_000L
        val recorded = mutableListOf<Long>()
        val profile = DriveProfileSession(DriveCinemaScope("drive", "app", "profile"), 3, "714895126038",
            coordinator, automatic = false, lastSuccessfulSync = lastSuccess, recordSuccess = { recorded.add(it) }) { true }
        val loop = DriveProfileSyncLoop(scout, remotePollMs = 60_000L)
        backgroundScope.launch { loop.run(MutableStateFlow(profile)) }
        runCurrent()
        settings.value = original.copy(scoutShowWatched = false)
        advanceTimeBy(120_000L)
        runCurrent()
        coVerify(exactly = 0) { coordinator.synchronize(any(), any(), any(), any(), any()) }
        assertEquals(lastSuccess, loop.lastSuccess.value)
        loop.syncNow()
        runCurrent()
        coVerify(exactly = 1) { coordinator.synchronize(any(), any(), settings.value, any(), any()) }
        assertEquals(listOf(checkNotNull(loop.lastSuccess.value)), recorded)
    }

    @Test
    fun checksRemoteWithoutLocalEditsAndStopsPollingWhenSessionEnds() = runTest {
        val original = DrivePortableSettings(null, scoutInMenu = true, scoutListEmojis = false)
        val scout = mockk<ScoutStore>()
        every { scout.observeDriveSettings(3) } returns MutableStateFlow(original)
        val coordinator = mockk<DriveJournalCoordinator>()
        coEvery { coordinator.synchronize(any(), any(), any(), any(), any()) } returns DriveJournalOutcome.IN_SYNC
        val profile = DriveProfileSession(DriveCinemaScope("drive", "app", "profile"), 3, "714895126038", coordinator) { true }
        val sessions = MutableStateFlow<DriveProfileSession?>(profile)
        val loop = DriveProfileSyncLoop(scout, remotePollMs = 60_000L)
        backgroundScope.launch { loop.run(sessions) }
        runCurrent()
        coVerify(exactly = 1) { coordinator.synchronize(any(), any(), original, any(), any()) }
        advanceTimeBy(59_999)
        runCurrent()
        coVerify(exactly = 1) { coordinator.synchronize(any(), any(), original, any(), any()) }
        advanceTimeBy(1)
        runCurrent()
        coVerify(exactly = 2) { coordinator.synchronize(any(), any(), original, any(), any()) }
        sessions.value = null
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()
        coVerify(exactly = 2) { coordinator.synchronize(any(), any(), original, any(), any()) }
    }

    @Test
    fun opensDebouncesManualRunsAndStopsAfterSignOut() = runTest {
        val original = DrivePortableSettings(null, scoutInMenu = true, scoutListEmojis = false)
        val settings = MutableStateFlow(original)
        val scout = mockk<ScoutStore>()
        every { scout.observeDriveSettings(3) } returns settings
        val coordinator = mockk<DriveJournalCoordinator>()
        coEvery { coordinator.synchronize(any(), any(), any(), any(), any()) } returns DriveJournalOutcome.IN_SYNC
        val profile = DriveProfileSession(DriveCinemaScope("drive", "app", "profile"), 3, "714895126038", coordinator) { true }
        val sessions = MutableStateFlow<DriveProfileSession?>(profile)
        val loop = DriveProfileSyncLoop(scout)
        backgroundScope.launch { loop.run(sessions) }
        runCurrent()
        assertEquals(DriveJournalOutcome.IN_SYNC, loop.outcome.value)
        coVerify(exactly = 1) { coordinator.synchronize(any(), any(), original, any(), any()) }

        advanceTimeBy(751)
        runCurrent()
        val changed = original.copy(scoutShowWatched = false)
        settings.value = changed
        runCurrent()
        advanceTimeBy(749)
        coVerify(exactly = 0) { coordinator.synchronize(any(), any(), changed, any(), any()) }
        advanceTimeBy(1)
        runCurrent()
        coVerify(exactly = 1) { coordinator.synchronize(any(), any(), changed, any(), any()) }
        loop.syncNow()
        runCurrent()
        coVerify(exactly = 2) { coordinator.synchronize(any(), any(), changed, any(), any()) }

        sessions.value = null
        runCurrent()
        assertNull(loop.outcome.value)
        loop.syncNow()
        runCurrent()
        coVerify(exactly = 2) { coordinator.synchronize(any(), any(), changed, any(), any()) }
    }
}
