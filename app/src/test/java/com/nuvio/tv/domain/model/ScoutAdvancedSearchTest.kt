package com.nuvio.tv.domain.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoutAdvancedSearchTest {
    private fun pick(id: Int) = ScoutPick(id, false, "Title $id")

    @Test
    fun exactPhraseFiltersNormalizedWordsAndCombinesFields() {
        val query = ScoutAdvancedSearch(
            exactPhrase = "  Ação   Final ",
            genreIds = setOf(18, 28),
            year = 2025,
            originalLanguage = "PT"
        ).normalized()
        val title = pick(1).copy(name = "Uma Ação final!", genres = listOf(18, 28), released = "2025-11-25", language = "pt")
        assertTrue(query.matches(title))
        assertFalse(query.matches(title.copy(name = "Uma ação finalista")))
        assertFalse(query.matches(title.copy(language = "en")))
        assertFalse(query.matches(title.copy(released = null)))
        assertFalse(query.matches(title.copy(genres = listOf(28))))
    }

    @Test
    fun keywordsUseSupportedTmdbOperatorsWithStableIds() {
        val query = ScoutAdvancedSearch(keywordIds = setOf(22, -1, 8)).normalized()
        assertEquals("8,22", query.keywordExpression())
        assertEquals("8|22", query.copy(matchAnyKeyword = true).keywordExpression())
    }

    @Test
    fun historyKeepsTwentyDistinctSuggestionsAndSavedSearchSurvivesSerialization() {
        val query = ScoutAdvancedSearch(exactPhrase = "Café", keywordIds = setOf(23))
        val history = ScoutDiscoveryHistory().record((1..20).map(::pick))
            .record(listOf(pick(21), pick(1))).save(query).save(query)
        assertEquals(listOf(21, 1) + (2..19).toList(), history.suggestions.map { it.tmdbId })
        val adapter = Moshi.Builder().build().adapter(ScoutDiscoveryHistory::class.java)
        assertEquals(history, adapter.fromJson(adapter.toJson(history)))
        assertEquals(listOf(query.normalized()), history.savedSearches)
        assertTrue(history.forget(query).savedSearches.isEmpty())
    }

    @Test
    fun surprisePrefersNewTitlesAndNeverRepeatsCurrentRow() {
        val suggestions = scoutSurpriseSuggestions(
            candidates = (1..6).map(::pick),
            recent = listOf(pick(4), pick(3), pick(2)),
            currentKeys = setOf(pick(4).key, pick(6).key),
            limit = 4
        )
        assertEquals(listOf(1, 5, 2, 3), suggestions.map { it.tmdbId })
    }

    @Test
    fun animeRecapDetectionPreservesOriginalFilms() {
        val movie = ScoutPick(1, false, "Meu anime: Recapitulação", genres = listOf(16), language = "ja")
        assertTrue(movie.isAnimeRecap())
        assertFalse(movie.copy(name = "Meu anime: A Origem").isAnimeRecap())
        assertFalse(movie.copy(tv = true).isAnimeRecap())
        assertFalse(movie.copy(language = null).isAnimeRecap())
    }
}
