package com.nuvio.tv.ui.screens.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.data.local.WatchedSeriesStateHolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal fun searchWatchedSeriesFixture(): WatchedSeriesStateHolder {
    val profiles = mockk<ProfileManager> {
        every { activeProfileId } returns MutableStateFlow(1)
    }
    val store = mockk<DataStore<Preferences>> {
        every { data } returns flowOf(preferencesOf(intPreferencesKey("validation_reset_version") to 1))
    }
    val factory = mockk<ProfileDataStoreFactory> {
        every { get(1, "watched_series_cache") } returns store
    }
    return WatchedSeriesStateHolder(factory, profiles)
}
