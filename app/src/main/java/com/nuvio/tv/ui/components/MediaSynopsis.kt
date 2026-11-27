package com.nuvio.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.contentTextDirection
import java.util.Locale

/** The preview stays still. Only explicit activation composes the full, scrollable text. */
@Composable
internal fun MediaSynopsis(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    adaptiveHeight: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = NuvioTheme.colors.TextPrimary
) {
    val source = remember(text) { text.trim() }
    val textStyle = style.copy(textDirection = source.contentTextDirection())
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val measure = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = stringResource(R.string.hero_synopsis_read_more)
    val labelStyle = MaterialTheme.typography.labelMedium
    val actionHeight = 24.dp
    val gap = 8.dp
    var open by remember(title, source) { mutableStateOf(false) }
    var focused by remember(title, source) { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val labelWidth = remember(label, labelStyle, measure, width) {
            measure.measure(label, style = labelStyle, maxLines = 1).size.width.coerceAtMost(width / 3)
        }
        val gapPx = with(density) { gap.roundToPx() }
        val actionHeightPx = with(density) { actionHeight.roundToPx() }
        val (excerpt, more) = remember(source, locale, textStyle, measure, maxLines, width, height,
            adaptiveHeight, labelWidth, gapPx, actionHeightPx) {
            fun fits(value: String, availableWidth: Int, availableHeight: Int): Boolean =
                !measure.measure(value, style = textStyle, maxLines = maxLines, overflow = TextOverflow.Clip,
                    constraints = Constraints(maxWidth = availableWidth, maxHeight = availableHeight)).hasVisualOverflow

            if (fits(source, width, height)) source to false
            else {
                val excerptWidth = if (adaptiveHeight) width else (width - labelWidth - gapPx).coerceAtLeast(0)
                val excerptHeight = if (adaptiveHeight) (height - actionHeightPx).coerceAtLeast(0) else height
                selectSynopsisExcerpt(source, locale) { fits(it, excerptWidth, excerptHeight) } to true
            }
        }
        val action = if (more) Modifier
            .onFocusChanged { focused = it.isFocused }
            .background(Color.White.copy(alpha = if (focused) .14f else 0f), RoundedCornerShape(5.dp))
            .clickable(role = Role.Button) { open = true }
        else Modifier

        if (more && adaptiveHeight) {
            Column(action.fillMaxWidth()) {
                Text(excerpt, color = color, style = textStyle, maxLines = maxLines, overflow = TextOverflow.Clip)
                Text(label, Modifier.height(actionHeight).padding(top = 4.dp), color = color, style = labelStyle)
            }
        } else {
            Row(action.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom) {
                Text(excerpt, Modifier.weight(1f), color = color, style = textStyle,
                    maxLines = maxLines, overflow = TextOverflow.Clip)
                if (more) {
                    Text(label, Modifier.width(with(density) { labelWidth.toDp() }), color = color,
                        style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (open) SynopsisOverlay(title, source, onDismiss = { open = false }, cinematic = true)
}
