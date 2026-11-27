package com.nuvio.tv.ui.screens.collection

import com.nuvio.tv.core.tmdb.TmdbEnrichment
import com.nuvio.tv.domain.model.MetaCastMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderPreviewCreditsTest {
    private val actor = MetaCastMember("Elenco", photo = "portrait")
    private val enrichment = TmdbEnrichment(
        localizedTitle = null, description = null, genres = emptyList(), backdrop = null,
        logo = null, poster = null, directorMembers = emptyList(), writerMembers = emptyList(),
        castMembers = listOf(actor), releaseInfo = null, rating = null, runtimeMinutes = null,
        director = listOf("Criação mapeada no campo legado"), writer = emptyList(),
        productionCompanies = emptyList(), networks = emptyList(), ageRating = null,
        status = null, countries = null, language = null, collectionId = null, collectionName = null,
        episodeCount = 10
    )

    @Test
    fun keepsPortraitCountAndOriginalCreditsInsteadOfInferringRolesFromLegacyField() {
        val preview = enrichment.copy(creators = listOf("Criação real")).toFolderPreviewCredits("logo-pt")
        assertEquals("Criação real", preview.creator)
        assertNull(preview.director)
        assertEquals(listOf(actor), preview.cast)
        assertEquals(10, preview.episodeCount)
        assertEquals("logo-pt", preview.logo)
    }

    @Test
    fun seriesCanHaveDistinctCreatorAndDirector() {
        val preview = enrichment.copy(creators = listOf("Criação"), directors = listOf("Direção"))
            .toFolderPreviewCredits(null)
        assertEquals("Criação", preview.creator)
        assertEquals("Direção", preview.director)
        assertNull(preview.logo)
    }
}
