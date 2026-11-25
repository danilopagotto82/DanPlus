package com.nuvio.tv.ui.screens.plugin

import com.nuvio.tv.domain.model.PluginRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingRepositoryRemovalTest {
    private val repository = PluginRepository("one", "Example repository", "https://example.invalid/plugins")
    private val request = PendingRepositoryRemoval(profileId = 2, repository = repository)

    @Test
    fun confirmationRequiresTheSameWritableProfileAndRepository() {
        assertTrue(request.matches(2, false, listOf(repository.copy(name = "Renamed"))))
        assertFalse(request.matches(2, true, listOf(repository)))
        assertFalse(request.matches(1, false, listOf(repository)))
        assertFalse(request.matches(2, false, emptyList()))
        assertFalse(request.matches(2, false, listOf(repository.copy(url = "https://other.invalid/plugins"))))
    }
}
