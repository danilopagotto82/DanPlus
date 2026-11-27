package com.nuvio.tv.ui.screens.scout

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoutSynopsisTest {
    private val brazil = Locale.forLanguageTag("pt-BR")

    @Test
    fun shortSynopsisKeepsOriginalPunctuation() {
        val synopsis = "Uma família muda de cidade. Tudo muda!"
        assertEquals(synopsis, selectScoutSynopsis(synopsis, brazil) { it.length < 60 })
    }

    @Test
    fun choosesOnlyWholeSentencesAtDifferentWidths() {
        val synopsis = "Uma família chega. A casa parece vazia. O segredo vai mudar tudo."
        assertEquals("Uma família chega.", selectScoutSynopsis(synopsis, brazil) { it.length < 27 })
        assertEquals("Uma família chega. A casa parece vazia.",
            selectScoutSynopsis(synopsis, brazil) { it.length < 50 })
    }

    @Test
    fun longFirstSentenceUsesAnOriginalClauseOrCompleteWords() {
        val synopsis = "Depois da tempestade, os moradores decidem procurar ajuda em outra cidade."
        assertEquals("Depois da tempestade,", selectScoutSynopsis(synopsis, brazil) { it.length < 40 })
        assertEquals("Depois da", selectScoutSynopsis(synopsis, brazil) { it.length < 12 })
    }
}
