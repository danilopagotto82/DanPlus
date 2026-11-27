package com.nuvio.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.localizedGenreLabel
import java.text.NumberFormat
import java.util.Locale

private val mediaYear = Regex("(?<!\\d)\\d{4}(?!\\d)")
private val bareMinutes = Regex("(\\d+)\\s*(?:min)?", RegexOption.IGNORE_CASE)

internal enum class MediaSeriesStatus { RUNNING, ENDED, CANCELLED }

internal fun mediaSeriesStatus(status: String?): MediaSeriesStatus? = when (status?.trim()?.lowercase(Locale.ROOT)) {
    "returning series", "in production", "running", "continuing", "em andamento" -> MediaSeriesStatus.RUNNING
    "ended", "completed", "encerrada", "encerrado", "finalizada", "finalizado" -> MediaSeriesStatus.ENDED
    "canceled", "cancelled", "cancelada", "cancelado" -> MediaSeriesStatus.CANCELLED
    else -> null
}

internal fun mediaYears(release: String?, end: String?, series: Boolean, status: MediaSeriesStatus?): String? {
    val first = release?.let { mediaYear.find(it)?.value } ?: return null
    if (!series) return first
    val last = end?.let { mediaYear.find(it)?.value }
        ?: mediaYear.findAll(release).lastOrNull()?.value
    return if (status != MediaSeriesStatus.RUNNING && last != null && last > first) "$first–$last" else first
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediaMetadataChips(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    episodeCount: Int? = null,
    lastAirYear: String? = null,
    maxLines: Int = 2,
    onArtwork: Boolean = false
) {
    val series = item.type == ContentType.SERIES || item.type == ContentType.TV
    val status = item.status?.takeIf { series }?.let(::mediaSeriesStatus)
    val year = mediaYears(item.releaseInfo ?: item.released, lastAirYear, series, status)
    val duration = item.runtime?.trim()?.takeIf(String::isNotEmpty)?.let { raw ->
        val minutes = bareMinutes.matchEntire(raw)?.groupValues?.get(1)?.toIntOrNull()
        when {
            minutes == null -> raw
            minutes <= 0 -> null
            series -> stringResource(R.string.danplus_metadata_episode_runtime, minutes)
            else -> stringResource(R.string.dan_folder_minutes, minutes)
        }
    }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val rating = item.imdbRating?.takeIf { it.isFinite() && it > 0f }?.let {
        val formatted = remember(it, locale) {
            NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(it)
        }
        "★ $formatted"
    }
    val episodes = episodeCount?.takeIf { series && it > 0 }
        ?.let { pluralStringResource(R.plurals.e05_grade_episodes, it, it) }
    val seasons = item.seasonCount?.takeIf { series && it > 0 }
        ?.let { pluralStringResource(R.plurals.scout_meta_seasons, it, it) }
    val state = when (status) {
        MediaSeriesStatus.RUNNING -> stringResource(R.string.e05_grade_running)
        MediaSeriesStatus.ENDED -> stringResource(R.string.e05_grade_ended)
        MediaSeriesStatus.CANCELLED -> stringResource(R.string.e05_grade_cancelled)
        null -> null
    }
    val labels = listOfNotNull(year, duration, rating, item.ageRating?.trim()?.takeIf(String::isNotEmpty), episodes, seasons, state)
    if (labels.isEmpty()) return
    val foreground = if (onArtwork) Color.White else NuvioTheme.colors.TextPrimary
    val background = if (onArtwork) Color.Black.copy(alpha = .72f) else foreground.copy(alpha = .12f)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        maxLines = maxLines
    ) {
        labels.distinct().forEach { label ->
            Text(
                text = label,
                color = foreground,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(background, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun MediaGenres(genres: List<String>, modifier: Modifier = Modifier, onArtwork: Boolean = false) {
    val context = LocalContext.current
    val labels = genres.filter(String::isNotBlank).distinct().take(3)
    if (labels.isEmpty()) return
    Text(
        text = labels.joinToString(" · ") { localizedGenreLabel(context, it) },
        style = MaterialTheme.typography.labelLarge,
        color = if (onArtwork) Color(0xFFDCE7F4) else NuvioTheme.colors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
