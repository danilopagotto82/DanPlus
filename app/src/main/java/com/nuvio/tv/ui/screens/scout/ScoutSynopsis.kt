package com.nuvio.tv.ui.screens.scout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.tv.ui.components.MediaSynopsis

@Composable
internal fun ScoutSynopsis(title: String, text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    MediaSynopsis(title, text, modifier, maxLines)
}
