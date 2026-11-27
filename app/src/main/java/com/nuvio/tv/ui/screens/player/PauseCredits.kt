package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.MetaCastMember
import java.text.Normalizer
import java.util.Locale

private fun creditName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

private val pauseDirectionRoles = setOf("director", "directors", "direction", "diretor", "diretores", "direcao", "diretora", "diretoras")
private val pauseWritingRoles = setOf(
    "writer", "writers", "writing", "screenwriter", "screenwriters", "screenplay", "teleplay",
    "escritor", "escritores", "escritora", "escritoras", "roteiro", "roteirista", "roteiristas",
    "guionista", "guionistas", "scriptwriter", "scriptwriters", "story", "novel", "author", "authors"
)

private fun pauseCreditRoles(character: String?): List<String> =
    Normalizer.normalize(creditName(character.orEmpty()), Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .split(Regex("""\s*(?:[/,|&;]|\band\b|\be\b)\s*"""))
        .filter(String::isNotBlank)

private fun isPauseCrew(member: MetaCastMember): Boolean {
    val roles = pauseCreditRoles(member.character)
    return roles.isNotEmpty() && roles.all { it in pauseDirectionRoles || it in pauseWritingRoles }
}

internal fun pauseAddonDirectors(credits: List<MetaCastMember>, names: List<String>): List<MetaCastMember> {
    val directors = credits.filter { member ->
        isPauseCrew(member) && pauseCreditRoles(member.character).any { it in pauseDirectionRoles }
    }
    return pauseDirectors(names, directors, credits.filterNot(::isPauseCrew))
}

internal fun pauseAddonCast(
    credits: List<MetaCastMember>,
    castNames: List<String>,
    writerNames: List<String>
): List<MetaCastMember> {
    val crewNames = writerNames.map(::creditName).toSet() +
        credits.filter(::isPauseCrew).map { creditName(it.name) }
    val actors = credits.filter { member ->
        !isPauseCrew(member) &&
            (!member.character.isNullOrBlank() || creditName(member.name) !in crewNames)
    }
    val actorNames = actors.map { creditName(it.name) }.toSet()
    return actors + castNames.filter {
        val name = creditName(it)
        name.isNotBlank() && name !in crewNames && name !in actorNames
    }.map { MetaCastMember(it.trim()) }
}

internal fun mergePauseCast(primary: List<MetaCastMember>, enrichment: List<MetaCastMember>): List<MetaCastMember> {
    if (primary.isEmpty()) return enrichment
    if (enrichment.isEmpty()) return primary
    val byId = enrichment.mapNotNull { member -> member.tmdbId?.let { it to member } }.toMap()
    val byName = enrichment.associateBy { creditName(it.name) }
    val enrichedPrimary = primary.map { member ->
        val detail = member.tmdbId?.let(byId::get) ?: byName[creditName(member.name)]
        if (detail == null) member else member.copy(
            character = member.character?.takeIf(String::isNotBlank) ?: detail.character,
            photo = member.photo?.takeIf(String::isNotBlank) ?: detail.photo,
            tmdbId = member.tmdbId ?: detail.tmdbId
        )
    }
    val primaryIds = enrichedPrimary.mapNotNull { it.tmdbId }.toSet()
    val primaryNames = enrichedPrimary.map { creditName(it.name) }.toSet()
    return enrichedPrimary + enrichment.filter {
        it.tmdbId !in primaryIds && creditName(it.name) !in primaryNames
    }
}

internal fun pauseDirectors(
    names: List<String>,
    credited: List<MetaCastMember>,
    cast: List<MetaCastMember>
): List<MetaCastMember> {
    val directors = credited.filter { member ->
        val roles = pauseCreditRoles(member.character)
        roles.none { it in pauseWritingRoles } || roles.any { it in pauseDirectionRoles }
    }
    val details = (cast + directors).associateBy { creditName(it.name) }
    return (names + directors.map { it.name })
        .filter(String::isNotBlank)
        .distinctBy(::creditName)
        .map { name ->
            val credit = details[creditName(name)]
            MetaCastMember(name.trim(), photo = credit?.photo, tmdbId = credit?.tmdbId)
        }
        .distinctBy { it.tmdbId ?: creditName(it.name) }
        .take(3)
}

internal fun pauseActors(cast: List<MetaCastMember>, directors: List<MetaCastMember>): List<MetaCastMember> {
    val directorIds = directors.mapNotNull { it.tmdbId }.toSet()
    val directorNames = directors.map { creditName(it.name) }.toSet()
    return cast.filter { member ->
        member.name.isNotBlank() && member.tmdbId !in directorIds && creditName(member.name) !in directorNames
    }.distinctBy { it.tmdbId ?: creditName(it.name) }.take(8)
}