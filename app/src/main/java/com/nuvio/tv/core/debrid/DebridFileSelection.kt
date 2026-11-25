package com.nuvio.tv.core.debrid

import com.nuvio.tv.domain.model.StreamClientResolve

internal fun <T> selectDebridFile(
    files: List<T>,
    resolve: StreamClientResolve,
    season: Int?,
    episode: Int?,
    path: (T) -> String,
    isPlayable: (T) -> Boolean,
    size: (T) -> Long,
): T? {
    val playable = files.filter(isPlayable)
    if (playable.isEmpty()) return null

    val names = listOfNotNull(resolve.filename, resolve.stream?.raw?.filename)
        .map { it.normalizedPath() }
        .filter { it.isNotBlank() }
        .distinct()
    for (name in names) {
        val matches = playable.matchingFiles(name, path)
        if (matches.isNotEmpty()) return matches.singleOrNull()
    }

    val episodePattern = buildEpisodePattern(season ?: resolve.season, episode ?: resolve.episode)
    for (name in names) {
        val matches = playable.matchingAliasFiles(name, path)
        if (matches.isEmpty()) continue
        // Alias matching is weaker than an exact hint. A different explicit episode cannot win.
        val eligible = if (episodePattern == null) matches else matches.filter {
            val basename = path(it).normalizedPath().substringAfterLast('/')
            !episodeMarker.containsMatchIn(basename) || episodePattern.containsMatchIn(basename)
        }
        return eligible.singleOrNull()
    }

    if (episodePattern != null) {
        val matches = playable.filter {
            episodePattern.containsMatchIn(path(it).normalizedPath().substringAfterLast('/'))
        }
        if (matches.isNotEmpty()) return matches.singleOrNull()
    }

    if (names.isNotEmpty() || episodePattern != null) return null

    resolve.fileIdx?.let { index ->
        return files.getOrNull(index)?.takeIf(isPlayable)
    }

    return playable.maxByOrNull(size)
}

private fun String.normalizedPath(): String = trim().replace('\\', '/').removePrefix("/")

private fun <T> List<T>.matchingFiles(name: String, path: (T) -> String): List<T> {
    for (ignoreCase in listOf(false, true)) {
        val matches = filter {
            val filePath = path(it).normalizedPath()
            filePath.equals(name, ignoreCase = ignoreCase) ||
                (name.contains('/') && filePath.endsWith("/$name", ignoreCase = ignoreCase))
        }
        if (matches.isNotEmpty()) return matches
    }
    val basename = name.substringAfterLast('/')
    for (ignoreCase in listOf(false, true)) {
        val matches = filter {
            path(it).normalizedPath().substringAfterLast('/').equals(basename, ignoreCase = ignoreCase)
        }
        if (matches.isNotEmpty()) return matches
    }
    return emptyList()
}

private fun <T> List<T>.matchingAliasFiles(name: String, path: (T) -> String): List<T> {
    val normalized = name.ampersandAlias()
    val paths = map { it to path(it).normalizedPath().ampersandAlias() }
    if ('/' in normalized) {
        val qualified = paths.filter { (_, candidate) ->
            candidate == normalized || candidate.endsWith("/$normalized")
        }
        if (qualified.isNotEmpty()) return qualified.map { it.first }
    }

    val basename = normalized.substringAfterLast('/')
    return paths.filter { (_, candidate) -> candidate.substringAfterLast('/') == basename }.map { it.first }
}

private fun String.ampersandAlias(): String =
    lowercase().replace(ampersandConjunction, " and ")

private val ampersandConjunction = Regex("""\s+&\s+""")
private val episodeMarker = Regex("""(?i)(?<![a-z0-9])(?:s\d+e\d+|\d+x\d+)(?![0-9])""")

private fun buildEpisodePattern(season: Int?, episode: Int?): Regex? {
    if (season == null || episode == null) return null
    return Regex(
        "(?<![a-z0-9])(?:s0*${season}e0*${episode}|0*${season}x0*${episode})(?![0-9])",
        RegexOption.IGNORE_CASE,
    )
}

internal fun String.hasDebridVideoExtension(): Boolean = videoExtensions.any { endsWith(it, ignoreCase = true) }

private val videoExtensions = setOf(
    ".mp4",
    ".mkv",
    ".webm",
    ".avi",
    ".mov",
    ".m4v",
    ".ts",
    ".m2ts",
    ".wmv",
    ".flv",
)
