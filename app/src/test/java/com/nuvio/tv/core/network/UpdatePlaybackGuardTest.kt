package com.nuvio.tv.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePlaybackGuardTest {
    @Test
    fun installationHandoffWaitsForAllPlaybackSessionsToClose() {
        val playback = DiagnosticPlaybackGuard()
        val currentSession = playback.enterPlayback()
        val replacementSession = playback.enterPlayback()
        var commits = 0
        assertTrue(playback.isPlaybackActive)
        assertFalse(playback.runWhileIdle { commits++ })
        currentSession.close()
        assertFalse(playback.runWhileIdle { commits++ })
        replacementSession.close()

        assertFalse(playback.isPlaybackActive)
        assertTrue(playback.runWhileIdle { commits++ })
        assertEquals(1, commits)
    }
}
