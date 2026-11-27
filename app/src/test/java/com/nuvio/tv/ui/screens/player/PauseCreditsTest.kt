package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.MetaCastMember
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseCreditsTest {
    @Test
    fun repeatedDirectorHasOneCardAndKeepsTheEnrichedPortrait() {
        val names = listOf("David Robert Mitchell", " david  robert mitchell ", "David Robert Mitchell")
        val credited = listOf(MetaCastMember("David Robert Mitchell", photo = "portrait"))

        assertEquals(credited, pauseDirectors(names, credited, emptyList()))
    }

    @Test
    fun actorWithAnExistingPortraitGetsTheCharacterFromTheExistingEnrichment() {
        val actor = MetaCastMember("Anne Hathaway", photo = "original", tmdbId = 1)
        val enriched = actor.copy(character = "Personagem", photo = "new")

        assertEquals(listOf(actor.copy(character = "Personagem")), mergePauseCast(listOf(actor), listOf(enriched)))
    }

    @Test
    fun writerOnlyFromAddonIsNeverAnActor() {
        val writer = MetaCastMember("Pessoa", "Roteirista")
        assertEquals(emptyList<MetaCastMember>(), pauseAddonCast(listOf(writer), listOf(writer.name), emptyList()))
        assertEquals(emptyList<MetaCastMember>(), pauseDirectors(emptyList(), listOf(writer), emptyList()))
    }

    @Test
    fun multipleCreditsKeepRealDirectorsAndActors() {
        val writing = MetaCastMember("Pessoa", "Writer", tmdbId = 1)
        val direction = writing.copy(character = "Direção")
        val acting = writing.copy(character = "Detetive")
        val silentActor = MetaCastMember("Ator")
        val credits = listOf(writing, direction, acting, silentActor)
        assertEquals(listOf(acting, silentActor), pauseAddonCast(credits, emptyList(), listOf(writing.name)))
        assertEquals(listOf(MetaCastMember("Pessoa", tmdbId = 1)),
            pauseDirectors(listOf(writing.name), listOf(direction, writing), listOf(acting)))
    }

    @Test
    fun addonDirectorKeepsPortraitEvenWhenAlsoCreditedAsWriter() {
        val director = MetaCastMember("Pessoa", "Roteirista / Direção", "portrait", 1)
        val writer = MetaCastMember("Só roteiro", "Screenwriter", "another", 2)
        assertEquals(listOf(director.copy(character = null)), pauseAddonDirectors(listOf(director, writer), emptyList()))
        assertEquals(emptyList<MetaCastMember>(), pauseAddonCast(listOf(director, writer), emptyList(), emptyList()))
    }

    @Test
    fun missingStructuredCreditsKeepAddonCastAndTmdbFallback() {
        val actor = MetaCastMember("Ator")
        val another = MetaCastMember("Outro", tmdbId = 5)
        assertEquals(listOf(actor), pauseAddonCast(emptyList(), listOf(actor.name, "Roteirista"), listOf("Roteirista")))
        assertEquals(listOf(actor, another), mergePauseCast(listOf(actor), listOf(another)))
    }

    @Test
    fun directorListedInCastAppearsOnceAndOtherActorsRemain() {
        val director = MetaCastMember("Director Name", tmdbId = 10)
        val alternateCredit = director.copy(name = "Alternate Credit", character = "Cameo")
        val actor = MetaCastMember("Actor Name", tmdbId = 20)
        val cast = listOf(alternateCredit, actor, actor.copy(name = "Actor Alias"))

        val direction = pauseDirectors(listOf(director.name), listOf(director, alternateCredit), cast)
        assertEquals(listOf(director), direction)
        assertEquals(listOf(actor), pauseActors(cast, direction))
    }
}
