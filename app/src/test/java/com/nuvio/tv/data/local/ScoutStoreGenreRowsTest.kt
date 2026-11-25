package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.nuvio.tv.domain.model.ScoutAdvancedSearch
import com.nuvio.tv.domain.model.ScoutPick
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoutStoreGenreRowsTest {
    private val store = ScoutStore(mockk(relaxed = true), mockk(relaxed = true))
    private val picked = booleanPreferencesKey("picked_row")
    private val genre = booleanPreferencesKey("genre_rows")

    @Test
    fun `discovery history and saved searches belong to the explicit profile`() = runTest {
        val factory = mockk<ProfileDataStoreFactory>()
        every { factory.get(1, "scout") } returns MemoryPreferences()
        every { factory.get(2, "scout") } returns MemoryPreferences()
        val scoped = ScoutStore(factory, mockk(relaxed = true))
        val pick = ScoutPick(90, false, "One")
        val query = ScoutAdvancedSearch(exactPhrase = "Test")
        scoped.updateDiscovery(1) { it.record(listOf(pick)).save(query) }

        assertEquals(listOf(pick), scoped.observeDiscovery(1).first().suggestions)
        assertEquals(listOf(query.normalized()), scoped.observeDiscovery(1).first().savedSearches)
        assertTrue(scoped.observeDiscovery(2).first().suggestions.isEmpty())
        assertTrue(scoped.observeDiscovery(2).first().savedSearches.isEmpty())
    }

    @Test
    fun `the sidebar retains the legacy visible default and accepts an explicit preference`() {
        assertTrue(store.readSettings(mutablePreferencesOf()).showInMenu)
        assertFalse(store.readSettings(mutablePreferencesOf(booleanPreferencesKey("show_in_menu") to false)).showInMenu)
    }

    private class MemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(mutablePreferencesOf())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    @Test
    fun `genre rows start on`() {
        assertTrue(store.readSettings(mutablePreferencesOf()).genreRows)
        assertTrue(store.readSettings(mutablePreferencesOf(picked to true)).genreRows)
    }

    @Test
    fun `picked for you switched off earlier keeps the genre rows off`() {
        assertFalse(store.readSettings(mutablePreferencesOf(picked to false)).genreRows)
    }

    @Test
    fun `a saved genre switch wins over picked for you`() {
        assertTrue(store.readSettings(mutablePreferencesOf(picked to false, genre to true)).genreRows)
        assertFalse(store.readSettings(mutablePreferencesOf(picked to true, genre to false)).genreRows)
    }
}
