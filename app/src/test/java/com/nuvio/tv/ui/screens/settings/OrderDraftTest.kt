package com.nuvio.tv.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class OrderDraftTest {
    @Test fun previewsWithoutMutatingTheBaselineAndDetectsConcurrentChanges() {
        val saved = listOf("a", "b", "c")
        val draft = OrderDraft(saved).move("a", 2)
        assertEquals(listOf("b", "c", "a"), draft.display(saved) { it })
        assertEquals(listOf("a", "b", "c"), saved)
        val changed = listOf("a", "c", "d")
        assertEquals(changed, draft.display(changed) { it })
        assertEquals(draft, draft.move("not installed", 0))
        assertEquals(draft, draft.move("b", -1))
    }
}
