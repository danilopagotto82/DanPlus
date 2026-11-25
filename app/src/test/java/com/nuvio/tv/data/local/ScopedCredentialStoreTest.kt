package com.nuvio.tv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nuvio.tv.core.profile.ProfileManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopedCredentialStoreTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        var beforeEdit: () -> Unit = {}

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            beforeEdit()
            return transform(data.value).also { data.value = it }
        }
    }

    @Test
    fun `a delayed edit for A cannot run after selection returns to A with a new revision`() = runTest {
        val active = MutableStateFlow(1)
        var revision = 1L
        val a = MemoryStore()
        val b = MemoryStore()
        val manager = mockk<ProfileManager> { every { activeProfileId } returns active }
        val factory = mockk<ProfileDataStoreFactory> {
            every { get(1, "mdblist_settings") } returns a
            every { get(2, "mdblist_settings") } returns b
        }
        val settings = MDBListSettingsDataStore(factory, manager)
        settings.setApiKeyForProfile(1, "fixture-a") {}
        settings.setApiKeyForProfile(2, "fixture-b") {}
        a.beforeEdit = {
            active.value = 2
            revision++
            active.value = 1
            revision++
        }

        val result = runCatching { settings.setApiKeyForProfile(1, "late") { check(revision == 1L) } }
        assertTrue(result.isFailure)
        active.value = 2
        assertEquals("fixture-a", settings.settingsForProfile(1).first().apiKey)
        assertEquals("fixture-b", settings.settingsForProfile(2).first().apiKey)
    }

    @Test
    fun `a late highlight response is checked inside the original profile transaction`() = runTest {
        val active = MutableStateFlow(1)
        var current = true
        val a = MemoryStore()
        val b = MemoryStore()
        val manager = mockk<ProfileManager> { every { activeProfileId } returns active }
        val factory = mockk<ProfileDataStoreFactory> {
            every { get(1, "collections") } returns a
            every { get(2, "collections") } returns b
        }
        val collections = CollectionsDataStore(mockk(), factory, manager)
        val first = SpecialHighlightSelection("first", "SOLID", -1, null)
        val late = SpecialHighlightSelection("late", "SOLID", -2, null)
        val empty = SpecialHighlightSnapshot(0, null, null)
        collections.applySpecialHighlight(1, "server-a", empty, 1, first) { true }
        val request = collections.specialHighlightSnapshot(1, "server-a")
        a.beforeEdit = { current = false }
        collections.applySpecialHighlight(1, "server-a", request, 2, late) { current }
        assertEquals(request, collections.specialHighlightSnapshot(1, "server-a"))
        assertEquals(empty, collections.specialHighlightSnapshot(2, "server-b"))
    }
}
