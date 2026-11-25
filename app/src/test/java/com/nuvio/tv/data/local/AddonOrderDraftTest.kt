package com.nuvio.tv.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.domain.model.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonOrderDraftTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        var beforeEdit: (() -> Unit)? = null

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            beforeEdit?.invoke()
            return transform(data.value).also { data.value = it }
        }
    }

    private val a = "https://a.example"
    private val b = "https://b.example"
    private val c = "https://c.example"
    private val baseline = listOf(a, b)
    private val rearranged = listOf(b, a)
    private val active = MutableStateFlow(1)
    private val revision = MutableStateFlow(5L)
    private val roster = MutableStateFlow(listOf(
        UserProfile(1, "Primary", "#112233"),
        UserProfile(2, "Inherited", "#223344", usesPrimaryAddons = true)
    ))
    private val stores = mapOf(1 to MemoryStore(), 2 to MemoryStore())
    private val preferences = AddonPreferences(
        mockk<ProfileDataStoreFactory> {
            every { get(any(), "addon_preferences") } answers { stores.getValue(firstArg()) }
        },
        mockk<ProfileManager> {
            every { activeProfileId } returns active
            every { profileSelectionRevision } returns revision
            every { profiles } returns roster
            every { activeProfile } answers { roster.value.first { it.id == active.value } }
        }
    )

    @Test
    fun `a saved order preserves a concurrent change to enabled state`() = runTest {
        preferences.setAddonOrder(baseline)
        preferences.setAddonEnabled(a, false)
        assertTrue(preferences.saveAddonOrderDraft(1 to 5L, baseline, rearranged))
        val saved = preferences.readAddonSnapshot(1)
        assertEquals(rearranged, saved.urls)
        assertEquals(false, saved.enabled[a])
        assertFalse(preferences.saveAddonOrderDraft(1 to 5L, baseline, rearranged))
    }

    @Test
    fun `concurrent additions removals and reorder keep the new stored list`() = runTest {
        for (current in listOf(listOf(a, b, c), listOf(b), listOf(b, a))) {
            preferences.setAddonOrder(baseline)
            preferences.setAddonOrder(current)
            val fromSync = preferences.readAddonSnapshot(1)
            assertFalse(preferences.saveAddonOrderDraft(1 to 5L, baseline, rearranged))
            assertEquals(fromSync, preferences.readAddonSnapshot(1))
        }
    }

    @Test
    fun `a profile reselected inside the transaction invalidates the draft and inherited addons are read only`() = runTest {
        preferences.setAddonOrder(baseline)
        val owner = preferences.readAddonSnapshot(1)
        val other = preferences.readAddonSnapshot(2)
        stores.getValue(1).beforeEdit = {
            active.value = 2
            revision.value++
            active.value = 1
            revision.value++
        }
        assertFalse(preferences.saveAddonOrderDraft(1 to 5L, baseline, rearranged))
        stores.getValue(1).beforeEdit = null
        active.value = 2
        revision.value++
        assertFalse(preferences.saveAddonOrderDraft(2 to revision.value, baseline, rearranged))
        assertEquals(owner, preferences.readAddonSnapshot(1))
        assertEquals(other, preferences.readAddonSnapshot(2))
    }
}
