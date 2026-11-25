package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscExtraSessionTest {
    @Test
    fun `end error and manual return share one snapshot and suppress feature activity through teardown`() {
        val extras = DiscExtraSession<Long>()
        val beforeExtra = extras.revision
        val ticket = requireNotNull(extras.begin(252_000L))
        assertTrue(extras.active)
        assertNull(extras.begin(10L))
        assertEquals(252_000L, extras.takeReturn(ticket))
        assertNull(extras.takeReturn(ticket))
        assertTrue(extras.active)
        extras.finish(ticket)
        assertFalse(extras.active)
        assertEquals(beforeExtra + 1, extras.revision)
        assertNull(extras.takeReturn(ticket))
    }

    @Test
    fun `late callbacks cannot finish another extra or overwrite its feature position`() {
        val extras = DiscExtraSession<Long>()
        val old = requireNotNull(extras.begin(300_000L))
        extras.takeReturn(old)
        extras.finish(old)
        val current = requireNotNull(extras.begin(340_000L))
        assertNull(extras.takeReturn(old))
        extras.finish(old)
        assertEquals(current, extras.ticket)
        assertEquals(340_000L, extras.takeReturn(current))
        extras.finish(current)
        assertFalse(extras.active)
    }
}
