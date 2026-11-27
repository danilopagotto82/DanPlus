package com.nuvio.tv.ui.screens.scout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.contentTextDirection
import java.text.BreakIterator
import java.util.Locale

internal fun selectScoutSynopsis(text: String, locale: Locale, fits: (String) -> Boolean): String {
    val source = text.trim()
    if (source.isEmpty() || fits(source)) return source

    val sentences = BreakIterator.getSentenceInstance(locale).apply { setText(source) }
    var sentencePrefix = ""
    var boundary = sentences.next()
    while (boundary != BreakIterator.DONE) {
        val candidate = source.substring(0, boundary).trimEnd()
        if (!fits(candidate)) break
        sentencePrefix = candidate
        boundary = sentences.next()
    }
    if (sentencePrefix.isNotEmpty()) return sentencePrefix

    val words = BreakIterator.getWordInstance(locale).apply { setText(source) }
    var wordPrefix = ""
    var clausePrefix = ""
    boundary = words.next()
    while (boundary != BreakIterator.DONE) {
        val candidate = source.substring(0, boundary).trimEnd()
        if (!fits(candidate)) break
        if (candidate.lastOrNull()?.isLetterOrDigit() == true) wordPrefix = candidate
        if (candidate.lastOrNull() in setOf(',', ';', ':', '—')) clausePrefix = candidate
        boundary = words.next()
    }
    return clausePrefix.ifBlank { wordPrefix.ifBlank { source } }
}

@Composable
internal fun ScoutSynopsis(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    val style = MaterialTheme.typography.bodyMedium.copy(textDirection = text.contentTextDirection())
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val selected = remember(text, locale, maxLines, style, width, measurer) {
            selectScoutSynopsis(text, locale) { candidate ->
                !measurer.measure(
                    text = candidate,
                    style = style,
                    maxLines = maxLines,
                    overflow = TextOverflow.Clip,
                    constraints = Constraints(maxWidth = width)
                ).hasVisualOverflow
            }
        }
        Text(selected, style = style, color = NuvioTheme.colors.TextPrimary,
            maxLines = maxLines, overflow = TextOverflow.Clip)
    }
}
