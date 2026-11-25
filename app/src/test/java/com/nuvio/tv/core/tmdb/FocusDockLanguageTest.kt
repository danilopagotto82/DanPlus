package com.nuvio.tv.core.tmdb

import com.nuvio.tv.data.remote.api.TmdbImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusDockLanguageTest {
    @Test fun brazilianTagWinsOverOriginalPortugueseAndUntaggedArt() {
        val logos = listOf(
            TmdbImage("/neutral.png"), TmdbImage("/portuguese.png", "pt"),
            TmdbImage("/original.png", "ja"), TmdbImage("/brazil.png", "pt", "BR")
        )
        assertEquals("/brazil.png", focusDockLogoPath(logos, "ja"))
        assertEquals("/brazil.png", focusDockLogoPath(logos + TmdbImage("/english.png", "en"), "en"))
    }

    @Test fun unqualifiedPortugueseIsOnlyUsableAsTheOriginalLanguage() {
        val logos = listOf(
            TmdbImage("/english.png", "en"), TmdbImage("/portuguese.png", "pt"), TmdbImage("/neutral.png")
        )
        assertEquals("/portuguese.png", focusDockLogoPath(logos, "pt"))
        assertEquals("/english.png", focusDockLogoPath(logos, "en"))
        assertNull(focusDockLogoPath(logos, "ja"))
        assertNull(focusDockLogoPath(logos, null))
    }

    @Test fun explicitRegionalTagAndAValidPathAreRequiredForBrazilianPriority() {
        val logos = listOf(
            TmdbImage("", "pt", "BR"), TmdbImage("/portugal.png", "pt", "PT"),
            TmdbImage("/brazil.png", "pt-BR")
        )
        assertEquals("/brazil.png", focusDockLogoPath(logos, "fr"))
        assertEquals("/portugal.png", focusDockLogoPath(logos.dropLast(1), "pt-PT"))
        assertNull(focusDockLogoPath(logos.dropLast(1), "pt-BR"))
    }

    @Test fun talkShowsOnlyExcludeExplicitGuestCredits() {
        assertTrue(isTalkShowGuestCredit(listOf(10767), "Self - Guest"))
        assertFalse(isTalkShowGuestCredit(listOf(10767), "Host"))
        assertFalse(isTalkShowGuestCredit(listOf(18), "Self"))
        assertFalse(isTalkShowGuestCredit(listOf(10767), "Ada"))
    }
}
