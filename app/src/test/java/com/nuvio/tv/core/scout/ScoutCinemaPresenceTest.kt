package com.nuvio.tv.core.scout

import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.TestPreferencesStore
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.data.local.ScoutStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ScoutCinemaPresenceTest {
    private val owner = ScoutSelection(1, 2)
    private val ticket = CinemaPresenceTicket(owner, 3, "tmdb:6", listOf(2 to "Guest profile"), true, 100)

    @Test fun reviewIsOneShotAndConsentMustMatchTheAccountGeneration() {
        val presence = ScoutCinemaPresence()
        presence.arm(ticket)
        assertSame(ticket, presence.enter(owner, 3, "tmdb:6", 99))
        presence.finish(ticket, owner, 3, "tmdb:6", true)
        presence.finish(ticket, owner, 3, "tmdb:6", true)
        assertSame(ticket, presence.pending.value)
        assertTrue(presence.consume(ticket, owner, 3))
        assertFalse(presence.consume(ticket, owner, 3))

        presence.arm(ticket)
        presence.enter(owner, 3, "tmdb:6", 99)
        presence.finish(ticket, owner, 4, "tmdb:6", true)
        assertNull(presence.pending.value)
    }

    @Test fun cancellationExpiryAndMismatchedPlaybackDoNotOfferReview() {
        val presence = ScoutCinemaPresence()
        presence.arm(ticket)
        assertNull(presence.enter(owner, 3, "tmdb:8", 99))
        assertNull(presence.enter(owner, 3, "tmdb:6", 99))
        presence.arm(ticket)
        assertNull(presence.enter(owner, 3, "tmdb:6", 101))
        presence.arm(ticket)
        presence.cancelUnstarted()
        assertNull(presence.enter(owner, 3, "tmdb:6", 99))
        presence.arm(ticket)
        presence.enter(owner, 3, "tmdb:6", 99)
        presence.finish(ticket, owner, 3, "tmdb:6", false)
        assertNull(presence.pending.value)
    }
    @Test fun companionAndAttendanceRecordsArePrivateAndBoundToTheirOwners() = runTest {
        val hostStore = TestPreferencesStore()
        val companionStore = TestPreferencesStore()
        val factory = mockk<ProfileDataStoreFactory>()
        every { factory.get(1, "scout_cinema_private") } returns hostStore
        every { factory.get(2, "scout_cinema_private") } returns companionStore
        val scout = ScoutStore(factory, mockk(relaxed = true))
        scout.saveCinemaCompanion(2, "family", "teen") { false }
        assertNull(scout.readCinemaCompanion(2).relation)
        scout.skipCinemaOnboarding(2) { true }
        assertTrue(scout.readCinemaCompanion(2).wasOffered)
        scout.saveCinemaCompanion(2, "family", "teen") { true }
        assertEquals("teen", scout.readCinemaCompanion(2).ageBand)
        assertNull(scout.readCinemaCompanion(1).ageBand)

        val attendance = stringPreferencesKey("cinema_last_attendance_v1")
        scout.recordCinemaAttendance(ticket, setOf(2, 7), true) { false }
        assertNull(hostStore.data.first()[attendance])
        scout.recordCinemaAttendance(ticket, setOf(2, 7), true) { true }
        assertEquals("tmdb:6|2|true", hostStore.data.first()[attendance])
        assertNull(companionStore.data.first()[attendance])
    }
}
