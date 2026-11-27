package com.nuvio.tv.ui.screens.scout

import com.nuvio.tv.domain.model.ScoutPick
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal fun cinemaCandidateFits(pick: ScoutPick, today: LocalDate, rejected: Set<String>): Boolean {
    if (pick.tv || pick.tmdbId <= 0 || pick.name.isBlank() || pick.overview.isNullOrBlank() ||
        pick.key in rejected) return false
    val released = pick.released ?: return false
    return try {
        LocalDate.parse(released) <= today
    } catch (_: DateTimeParseException) {
        false
    }
}

internal fun cinemaRuntimeFits(minutes: Int?, minimum: Int?, maximum: Int?, sessionMaximum: Int?): Boolean =
    minutes != null && minutes > 0 &&
        (minimum == null || minutes >= minimum) &&
        (maximum == null || minutes <= maximum) &&
        (sessionMaximum == null || minutes <= sessionMaximum)


internal data class ScoutCinemaGuest(val familyLimit: Boolean = false, val genre: Int? = null)

internal fun cinemaRefineCached(
    local: List<ScoutPick>,
    cachedKeys: List<String>
): List<ScoutPick> {
    val candidateKeys = local.take(3).mapTo(hashSetOf(), ScoutPick::key)
    val first = cachedKeys.firstOrNull { it in candidateKeys } ?: return local
    return local.sortedBy { it.key != first }
}
