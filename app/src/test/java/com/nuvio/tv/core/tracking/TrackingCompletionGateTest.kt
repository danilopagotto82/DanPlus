package com.nuvio.tv.core.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingCompletionGateTest {
    @Test
    fun `a temporarily disconnected failed provider remains pending`() {
        val gate = TrackingCompletionGate()
        val first = requireNotNull(gate.begin())
        gate.finish(first, TrackingCompletionResult(
            setOf(TrackingProviderId.TRAKT), complete = false, failed = setOf(TrackingProviderId.SIMKL)
        ))
        val disconnected = requireNotNull(gate.begin())
        gate.finish(disconnected, TrackingCompletionResult(emptySet(), complete = true))
        val reconnected = requireNotNull(gate.begin())
        assertEquals(setOf(TrackingProviderId.TRAKT), reconnected.accepted)
        gate.finish(reconnected, TrackingCompletionResult(setOf(TrackingProviderId.SIMKL), complete = true))
        assertNull(gate.begin())
    }

    @Test
    fun `a retry keeps acknowledged providers and excludes a concurrent stop`() {
        val gate = TrackingCompletionGate()
        val first = requireNotNull(gate.begin())
        assertNull(gate.begin())
        gate.finish(first, TrackingCompletionResult(setOf(TrackingProviderId.TRAKT), complete = false))
        val retry = requireNotNull(gate.begin())
        assertEquals(setOf(TrackingProviderId.TRAKT), retry.accepted)
        gate.finish(retry, TrackingCompletionResult(setOf(TrackingProviderId.SIMKL), complete = true))
        assertNull(gate.begin())
    }

    @Test
    fun `a late result cannot complete a replacement playback session`() {
        val gate = TrackingCompletionGate()
        val stale = requireNotNull(gate.begin())
        gate.reset()
        val current = requireNotNull(gate.begin())
        gate.finish(stale, TrackingCompletionResult(setOf(TrackingProviderId.TRAKT), complete = true))
        assertFalse(gate.isCurrent(stale))
        assertTrue(gate.isCurrent(current))
        assertEquals(emptySet<TrackingProviderId>(), current.accepted)
    }
}
