package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class DetailCollectionFocusTest {
    @get:Rule val rule = createComposeRule()

    @Test fun splitCollectionIsReachedBothWaysWhenLazyRowsEnterTheViewport() {
        rule.setContent { Sections(active = "cast", splitCollection = true) }
        focus("cast")
        press(Key.DirectionDown, "collection")
        press(Key.DirectionDown, "comments")
        press(Key.DirectionUp, "collection")
        press(Key.DirectionUp, "cast")
    }

    @Test fun missingCastReturnsToTheOtherActiveSection() {
        rule.setContent { Sections(active = "recommendations", splitCollection = true) }
        focus("recommendations")
        press(Key.DirectionDown, "collection")
        press(Key.DirectionUp, "recommendations")
    }

    @Test fun absentCollectionUsesDefaultOrder() {
        rule.setContent { Sections(active = "recommendations", splitCollection = false) }
        focus("recommendations")
        press(Key.DirectionDown, "comments")
        press(Key.DirectionUp, "recommendations")
    }

    @Composable
    private fun Sections(active: String, splitCollection: Boolean) {
        val state = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val people = remember { FocusRequester() }
        val collection = remember { FocusRequester() }
        val comments = remember { FocusRequester() }
        LazyColumn(Modifier.height(115.dp), state = state) {
            detailSection("people", "row", true,
                if (splitCollection) Modifier.focusAdjacentDetailSection(state, scope, "people", Key.DirectionDown, collection)
                else Modifier
            ) { SectionTarget(active, Modifier.focusRequester(people)) }
            if (splitCollection) {
                detailSection("collection", "row", true,
                    Modifier.focusAdjacentDetailSection(state, scope, "collection", Key.DirectionUp, people)
                        .focusAdjacentDetailSection(state, scope, "collection", Key.DirectionDown, comments)
                ) { SectionTarget("collection", Modifier.focusRequester(collection)) }
            }
            detailSection("comments", "row", true,
                if (splitCollection) Modifier.focusAdjacentDetailSection(state, scope, "comments", Key.DirectionUp, collection)
                else Modifier
            ) { SectionTarget("comments", Modifier.focusRequester(comments)) }
        }
    }

    @Composable
    private fun SectionTarget(tag: String, modifier: Modifier = Modifier) {
        Box(modifier.fillMaxWidth().height(110.dp).testTag(tag).focusable())
    }

    private fun focus(tag: String) {
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    private fun press(key: Key, expected: String) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag(expected).assertIsFocused()
    }
}
