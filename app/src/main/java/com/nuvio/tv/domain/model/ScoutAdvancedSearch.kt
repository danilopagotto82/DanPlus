package com.nuvio.tv.domain.model

import com.squareup.moshi.JsonClass

private const val DISCOVERY_HISTORY_LIMIT = 20
private const val DISCOVERY_SAVED_LIMIT = 20

/** A portable query for the Scout TMDB catalog. IDs come from TMDB's genre and keyword pickers. */
@JsonClass(generateAdapter = true)
data class ScoutAdvancedSearch(
    val exactPhrase: String = "",
    val genreIds: Set<Int> = emptySet(),
    val keywordIds: Set<Int> = emptySet(),
    val matchAnyKeyword: Boolean = false,
    val year: Int? = null,
    val originalLanguage: String? = null,
) {
    fun normalized(): ScoutAdvancedSearch = copy(
        exactPhrase = titleTerms().joinToString(", "),
        genreIds = genreIds.filter { it > 0 }.sorted().take(12).toSet(),
        keywordIds = keywordIds.filter { it > 0 }.sorted().take(12).toSet(),
        year = year?.takeIf { it in 1800..2199 },
        originalLanguage = originalLanguage?.trim()?.lowercase()?.takeIf { it.matches(Regex("[a-z]{2}")) },
    )

    internal fun keywordExpression(): String? = keywordIds.takeIf { it.isNotEmpty() }
        ?.joinToString(if (matchAnyKeyword) "|" else ",")

    internal fun titleTerms(): List<String> = exactPhrase.take(160)
        .split(',')
        .map(::scoutNormalizedName)
        .filter(String::isNotBlank)
        .distinct()
        .take(3)

    internal fun matches(pick: ScoutPick): Boolean {
        if (year != null && pick.released?.take(4)?.toIntOrNull() != year) return false
        if (originalLanguage != null && originalLanguage != pick.language) return false
        if (!pick.genres.containsAll(genreIds)) return false
        val terms = titleTerms()
        if (terms.isEmpty()) return true
        val fields = listOfNotNull(pick.name, pick.overview).map { " " + scoutNormalizedName(it) + " " }
        val matches: (String) -> Boolean = { term -> fields.any { it.contains(" $term ") } }
        return if (matchAnyKeyword) terms.any(matches) else terms.all(matches)
    }
}

@JsonClass(generateAdapter = true)
data class ScoutDiscoveryHistory(
    val suggestions: List<ScoutPick> = emptyList(),
    val savedSearches: List<ScoutAdvancedSearch> = emptyList(),
) {
    internal fun record(picks: List<ScoutPick>): ScoutDiscoveryHistory =
        copy(suggestions = (picks + suggestions).distinctBy(ScoutPick::key).take(DISCOVERY_HISTORY_LIMIT))

    internal fun save(query: ScoutAdvancedSearch): ScoutDiscoveryHistory =
        copy(savedSearches = (listOf(query.normalized()) + savedSearches).distinct().take(DISCOVERY_SAVED_LIMIT))

    internal fun forget(query: ScoutAdvancedSearch): ScoutDiscoveryHistory =
        copy(savedSearches = savedSearches - query.normalized())
}

/** Fresh titles first; when exhausted, favor the least recent results without repeating the current row. */
internal fun scoutSurpriseSuggestions(
    candidates: List<ScoutPick>,
    recent: List<ScoutPick>,
    currentKeys: Set<String>,
    limit: Int
): List<ScoutPick> {
    val eligible = candidates.distinctBy(ScoutPick::key).filterNot { it.key in currentKeys }
    val recentKeys = recent.map(ScoutPick::key).toSet()
    val fresh = eligible.filterNot { it.key in recentKeys }
    if (fresh.size >= limit) return fresh.take(limit)
    val byKey = eligible.associateBy(ScoutPick::key)
    return (fresh + recent.asReversed().mapNotNull { byKey[it.key] }).take(limit)
}
