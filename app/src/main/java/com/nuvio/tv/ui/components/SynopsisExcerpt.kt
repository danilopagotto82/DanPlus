package com.nuvio.tv.ui.components

import java.text.BreakIterator
import java.util.Locale

private val clauseEnds = setOf(',', ';', ':', '—')

internal fun selectSynopsisExcerpt(text: String, locale: Locale, fits: (String) -> Boolean): String {
    val source = text.trim()
    if (source.isEmpty() || fits(source)) return source

    fun boundaries(iterator: BreakIterator): List<Int> = buildList {
        iterator.setText(source)
        var boundary = iterator.next()
        while (boundary != BreakIterator.DONE && boundary < source.length) {
            add(boundary)
            boundary = iterator.next()
        }
    }

    fun longestFitting(ends: List<Int>): String {
        var first = 0
        var last = ends.lastIndex
        while (first <= last) {
            val middle = (first + last) / 2
            if (fits(source.substring(0, ends[middle]).trimEnd())) first = middle + 1
            else last = middle - 1
        }
        return if (last < 0) "" else source.substring(0, ends[last]).trimEnd()
    }

    val sentence = longestFitting(boundaries(BreakIterator.getSentenceInstance(locale)))
    if (sentence.isNotEmpty()) return sentence

    val words = boundaries(BreakIterator.getLineInstance(locale))
    val clause = longestFitting(words.filter { end ->
        var index = end - 1
        while (index >= 0 && source[index].isWhitespace()) index--
        source.getOrNull(index) in clauseEnds
    })
    if (clause.isNotEmpty()) return clause

    val prefix = longestFitting(words)
    if (prefix.isNotEmpty()) return prefix

    // A single URL or a long unbroken word may exceed the measure by itself.
    return longestFitting(boundaries(BreakIterator.getCharacterInstance(locale)))
}
