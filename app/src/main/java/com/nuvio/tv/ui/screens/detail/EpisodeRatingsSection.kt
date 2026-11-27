package com.nuvio.tv.ui.screens.detail

import com.nuvio.tv.ui.theme.NuvioTheme

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.nuvio.tv.domain.model.EpisodeRating
import com.nuvio.tv.domain.model.EpisodeRatingSource
import com.nuvio.tv.domain.model.EpisodeRatingsResult
import java.util.Locale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Video

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EpisodeRatingsSection(
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, EpisodeRating>,
    ratingsBySeason: Map<Int, EpisodeRatingsResult>,
    onLoadRatingSeason: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Ratings",
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    firstItemFocusRequester: FocusRequester? = null,
    ratingsGridFocusRequester: FocusRequester? = null
) {
    val seasonNumbers = remember(episodes) {
        episodes
            .mapNotNull { it.season }
            .filter { it > 0 } // Never show specials season (S0)
            .distinct()
            .sorted()
    }
    val seasonSignature = remember(seasonNumbers) { seasonNumbers.joinToString(",") }
    val seasonFocusRequesters = remember(seasonNumbers) {
        seasonNumbers.associateWith { FocusRequester() }
    }
    val internalRatingsGridFocusRequester = remember { FocusRequester() }
    val effectiveRatingsGridFocusRequester = ratingsGridFocusRequester ?: internalRatingsGridFocusRequester
    val firstEpisodeRatingFocusRequester = remember { FocusRequester() }
    val defaultSeason = remember(seasonNumbers) {
        seasonNumbers.firstOrNull { it > 0 } ?: seasonNumbers.firstOrNull() ?: 0
    }
    var selectedSeason by rememberSaveable(seasonSignature) {
        mutableIntStateOf(defaultSeason)
    }

    LaunchedEffect(seasonNumbers, defaultSeason) {
        if (selectedSeason !in seasonNumbers) {
            selectedSeason = defaultSeason
        }
    }

    val episodesForSeason = remember(episodes, selectedSeason) {
        episodes
            .filter { it.season == selectedSeason && it.episode != null }
            .distinctBy { it.season to it.episode }
            .sortedBy { it.episode }
    }
    val defaultChipColor = NuvioTheme.colors.BackgroundCard
    val defaultChipTextColor = NuvioTheme.colors.TextSecondary
    val result = ratingsBySeason[selectedSeason]
    val pending = selectedSeason in seasonNumbers && result == null
    LaunchedEffect(selectedSeason, episodesForSeason.isNotEmpty()) {
        if (selectedSeason > 0 && episodesForSeason.isNotEmpty()) onLoadRatingSeason(selectedSeason)
    }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val noScore = stringResource(if (pending) R.string.ratings_episode_loading else R.string.ratings_episode_no_score)
    val seasonRatings = remember(episodesForSeason, ratings, locale, noScore, defaultChipColor, defaultChipTextColor) {
        episodesForSeason.mapNotNull { episode ->
            val season = episode.season ?: return@mapNotNull null
            val episodeNumber = episode.episode ?: return@mapNotNull null
            val rating = ratings[season to episodeNumber]
            val ratingText = rating?.let { String.format(locale, "%.1f", it.value) } ?: noScore
            val chipColor = rating?.takeUnless { it.source == EpisodeRatingSource.ADDON }
                ?.let { episodeRatingColor(it.value) } ?: defaultChipColor
            val chipTextColor = if (rating != null) Color.White else defaultChipTextColor
            EpisodeRatingChipUi(
                source = rating?.source,
                seasonNumber = season,
                episodeNumber = episodeNumber,
                ratingText = ratingText,
                chipColor = chipColor,
                chipTextColor = chipTextColor
            )
        }
    }
    val hasTitle = title.isNotBlank()
    val currentUpFocusRequester by rememberUpdatedState(upFocusRequester)
    val currentDownFocusRequester by rememberUpdatedState(downFocusRequester)
    val selectedSeasonRequester = firstItemFocusRequester
        ?: seasonFocusRequesters[selectedSeason]
        ?: FocusRequester.Default

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (hasTitle) 14.dp else 6.dp, bottom = NuvioTheme.spacing.sm)
    ) {
        if (hasTitle) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NuvioTheme.colors.TextPrimary,
                modifier = Modifier.padding(start = detailStartInset, end = NuvioTheme.spacing.xxxl)
            )
        }

        when {
            seasonNumbers.isEmpty() -> {
                Text(
                    text = stringResource(R.string.ratings_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary,
                    modifier = Modifier.padding(start = detailStartInset, end = NuvioTheme.spacing.xxxl, top = NuvioTheme.spacing.md, bottom = NuvioTheme.spacing.md)
                )
            }
            else -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRestorer { selectedSeasonRequester }
                        .focusGroup(),
                    contentPadding = PaddingValues(start = detailStartInset, end = NuvioTheme.spacing.xxxl, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(seasonNumbers, key = { it }) { season ->
                        val isSelected = season == selectedSeason
                        val focusRequester = if (isSelected) {
                            selectedSeasonRequester
                        } else {
                            seasonFocusRequesters.getValue(season)
                        }

                        Card(
                            onClick = { selectedSeason = season },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .focusProperties {
                                    val upRequester = currentUpFocusRequester
                                    if (upRequester != null) up = upRequester
                                    down = effectiveRatingsGridFocusRequester
                                }
                                .onFocusChanged { state ->
                                    if (state.isFocused && selectedSeason != season) {
                                        selectedSeason = season
                                    }
                                }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                },
                            shape = CardDefaults.shape(shape = RoundedCornerShape(14.dp)),
                            colors = CardDefaults.colors(
                                containerColor = if (isSelected) {
                                    NuvioTheme.colors.FocusBackground
                                } else {
                                    NuvioTheme.colors.BackgroundCard
                                },
                                focusedContainerColor = NuvioTheme.colors.FocusBackground
                            ),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ),
                            scale = CardDefaults.scale(focusedScale = 1f)
                        ) {
                            Text(
                                text = stringResource(R.string.ratings_season_label, season),
                                style = MaterialTheme.typography.labelMedium,
                                color = NuvioTheme.colors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                val seasonStatus = when {
                    pending -> stringResource(R.string.ratings_loading)
                    seasonRatings.any { it.source != null } ->
                        stringResource(R.string.ratings_season_summary, selectedSeason, episodesForSeason.size)
                    result?.ratings?.isNotEmpty() == true -> stringResource(R.string.ratings_hidden)
                    result?.failed == true -> stringResource(R.string.ratings_load_error)
                    else -> stringResource(R.string.ratings_unavailable)
                }
                Text(
                    text = seasonStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = NuvioTheme.colors.TextSecondary,
                    modifier = Modifier.padding(start = detailStartInset, end = NuvioTheme.spacing.xxxl, top = NuvioTheme.spacing.xxs, bottom = NuvioTheme.spacing.xxs)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(effectiveRatingsGridFocusRequester)
                        .focusRestorer(firstEpisodeRatingFocusRequester)
                        .focusGroup(),
                    contentPadding = PaddingValues(start = detailStartInset, end = NuvioTheme.spacing.xxxl, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(seasonRatings, key = { "${it.seasonNumber}:${it.episodeNumber}" }) { episodeRating ->
                        val isFirstEpisode = episodeRating == seasonRatings.firstOrNull()

                        // These chips only display a rating and have no action, but their focus is
                        // load-bearing: the season tabs point "down" at this row and the row's
                        // focusRestorer resolves to the first chip, so dropping them from the D-pad
                        // order would strand the whole ratings row. Keep the stop and name it.
                        val sourceLabel = episodeRating.source?.let { episodeRatingSourceLabel(it) }.orEmpty()
                        val ratingDescription = listOfNotNull(
                            "S${episodeRating.seasonNumber}E${episodeRating.episodeNumber}",
                            episodeRating.ratingText, sourceLabel.takeIf(String::isNotEmpty)
                        ).joinToString(", ")

                        Card(
                            onClick = { },
                            modifier = Modifier
                                .focusProperties {
                                    up = selectedSeasonRequester
                                    val downRequester = currentDownFocusRequester
                                    if (downRequester != null) down = downRequester
                                }
                                .then(
                                    if (isFirstEpisode) Modifier.focusRequester(firstEpisodeRatingFocusRequester) else Modifier
                                )
                                // (tv-material3's Card only has the clickable overload, so the
                                // empty onClick stays; clearAndSetSemantics replaces the chip's two
                                // Texts with one name that carries both the episode and its rating.)
                                .clearAndSetSemantics { contentDescription = ratingDescription },
                            shape = CardDefaults.shape(shape = RoundedCornerShape(14.dp)),
                            colors = CardDefaults.colors(
                                containerColor = episodeRating.chipColor,
                                focusedContainerColor = episodeRating.chipColor
                            ),
                            border = CardDefaults.border(
                                focusedBorder = Border(
                                    border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ),
                            scale = CardDefaults.scale(focusedScale = 1.03f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .size(width = 88.dp, height = 66.dp)
                                    .padding(horizontal = NuvioTheme.spacing.sm, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.ratings_episode_label, episodeRating.episodeNumber),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = episodeRating.chipTextColor
                                )
                                Text(
                                    text = episodeRating.ratingText,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    color = episodeRating.chipTextColor
                                )
                                if (sourceLabel.isNotEmpty()) {
                                    Text(sourceLabel, style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1, color = episodeRating.chipTextColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun episodeRatingColor(value: Double): Color = when {
    value >= 9.0 -> Color(0xFF1B405A)
    value >= 8.0 -> Color(0xFF204634)
    value >= 7.0 -> Color(0xFF514223)
    value >= 6.0 -> Color(0xFF543528)
    else -> Color(0xFF532E42)
}

@Composable
internal fun episodeRatingSourceLabel(source: EpisodeRatingSource): String = when (source) {
    EpisodeRatingSource.IMDB -> "IMDb"
    EpisodeRatingSource.SERIES_GRAPH -> "SeriesGraph"
    EpisodeRatingSource.TMDB -> "TMDB"
    EpisodeRatingSource.ADDON -> stringResource(R.string.ratings_source_addon)
}

private data class EpisodeRatingChipUi(
    val source: EpisodeRatingSource?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val ratingText: String,
    val chipColor: Color,
    val chipTextColor: Color
)
