package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun FocusRequester.requestFocusAfterFrames(frames: Int = 2): Boolean {
    repeat(frames.coerceAtLeast(0)) {
        withFrameNanos { }
    }
    repeat(4) { attempt ->
        val requested = runCatching { requestFocus() }.getOrDefault(false)
        if (requested) return true
        if (attempt < 3) {
            withFrameNanos { }
        }
    }
    return false
}

internal fun Modifier.focusAdjacentDetailSection(
    state: LazyListState,
    scope: CoroutineScope,
    sectionKey: String,
    key: Key,
    destination: FocusRequester
): Modifier = onPreviewKeyEvent { event ->
    if (event.key != key || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (runCatching { destination.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) {
        return@onPreviewKeyEvent true
    }
    val layout = state.layoutInfo
    val index = layout.visibleItemsInfo.firstOrNull { it.key == sectionKey }?.index
        ?: return@onPreviewKeyEvent false
    val target = index + if (key == Key.DirectionDown) 1 else -1
    if (target !in 0 until layout.totalItemsCount) return@onPreviewKeyEvent false
    scope.launch {
        state.scrollToItem(target)
        destination.requestFocusAfterFrames(frames = 1)
    }
    true
}
