package com.nuvio.tv.ui.screens.scout

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.scout.ScoutMood
import com.nuvio.tv.core.scout.scoutReadPolicy
import com.nuvio.tv.data.trailer.TrailerPlaybackSource
import com.nuvio.tv.domain.model.ScoutPick
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.TrailerPlayer
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ScoutCinemaChoice(val shown: ScoutShownPick, val logo: String?)

internal data class ScoutCinemaTrailer(val source: TrailerPlaybackSource, val external: Boolean)

private enum class CinemaStep { MENU, SESSION, PREFERENCES, LOADING, RESULT, EMPTY, TRAILER }

private val cinemaGold = Color(0xFFD8B675)
private val cinemaMoods = listOf(
    null, ScoutMood.EDGE, ScoutMood.LAUGH, ScoutMood.ACTION, ScoutMood.TEARS,
    ScoutMood.MIND, ScoutMood.EPIC, ScoutMood.SCARY, ScoutMood.FEEL_GOOD, ScoutMood.DARK
)
private val cinemaDurations = listOf(30, 60, 90, 120, 150, null)

@Composable
internal fun ScoutCinemaDialog(
    initialMood: ScoutMood?,
    visible: Boolean,
    onPickCompany: () -> Unit,
    choose: suspend (ScoutMood?, Int?, Set<String>, Boolean, Boolean, ScoutCinemaGuest?) -> ScoutCinemaChoice?,
    loadGenre: suspend () -> Int?,
    saveGenre: suspend (Int?) -> Unit,
    loadTrailer: suspend (ScoutPick) -> ScoutCinemaTrailer?,
    onDismiss: () -> Unit,
    attendanceAvailable: Boolean,
    onPlay: (ScoutCinemaChoice, Boolean, Boolean) -> Unit
) {
    var step by remember { mutableStateOf(CinemaStep.MENU) }
    val scope = rememberCoroutineScope()
    var favoriteGenre by remember { mutableStateOf<Int?>(null) }
    var loadingGenre by remember { mutableStateOf(false) }
    var askAttendance by remember { mutableStateOf(false) }
    var guest by remember { mutableStateOf<ScoutCinemaGuest?>(null) }
    var allowRewatch by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf(initialMood?.takeIf { it in cinemaMoods }) }
    var duration by remember { mutableStateOf<Int?>(null) }
    var choice by remember { mutableStateOf<ScoutCinemaChoice?>(null) }
    var rejected by remember { mutableStateOf(emptySet<String>()) }
    var trailer by remember { mutableStateOf<TrailerPlaybackSource?>(null) }
    var returnToTrailerButton by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var trailerLoading by remember { mutableStateOf(false) }
    var trailerFailed by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val trailerButton = remember { FocusRequester() }

    if (!visible) return

    LaunchedEffect(step) {
        if (step == CinemaStep.LOADING) {
            choice = withTimeoutOrNull(25_000) {
                scoutReadPolicy { choose(mood, duration, rejected, configured, configured && allowRewatch, guest.takeIf { configured }) }
            }
            step = if (choice == null) CinemaStep.EMPTY else CinemaStep.RESULT
        } else if (step == CinemaStep.PREFERENCES) {
            favoriteGenre = scoutReadPolicy { loadGenre() }
            loadingGenre = false
            first.requestFocusAfterFrames()
        } else if (step == CinemaStep.TRAILER) {
            first.requestFocusAfterFrames()
            val resolved = withTimeoutOrNull(15_000) {
                scoutReadPolicy { choice?.shown?.pick?.let { loadTrailer(it) } }
            }
            trailerLoading = false
            if (resolved?.external == true) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolved.source.videoUrl)))
                    step = CinemaStep.RESULT
                } catch (_: ActivityNotFoundException) {
                    trailerFailed = true
                } catch (_: SecurityException) {
                    trailerFailed = true
                }
            } else {
                trailer = resolved?.source
                trailerFailed = trailer == null
            }
        } else if (step == CinemaStep.RESULT && returnToTrailerButton) {
            trailer = null
            returnToTrailerButton = false
            trailerButton.requestFocusAfterFrames()
        } else {
            first.requestFocusAfterFrames()
        }
    }

    val title = when (step) {
        CinemaStep.MENU -> R.string.scout_title
        CinemaStep.SESSION -> R.string.cinema_session
        CinemaStep.PREFERENCES -> R.string.cinema_profile_preferences
        CinemaStep.TRAILER -> R.string.cinema_trailer
        CinemaStep.LOADING, CinemaStep.RESULT, CinemaStep.EMPTY -> R.string.cinema_choice
    }
    NuvioDialog(
        onDismiss = {
            when (step) {
                CinemaStep.MENU -> onDismiss()
                CinemaStep.SESSION -> step = CinemaStep.MENU
                CinemaStep.PREFERENCES -> step = CinemaStep.SESSION
                CinemaStep.TRAILER -> step = CinemaStep.RESULT
                else -> step = if (configured) CinemaStep.SESSION else CinemaStep.MENU
            }
        },
        title = stringResource(title),
        width = 780.dp,
        usePlatformDefaultWidth = false,
        cinematicPageSurface = true,
        containerBorderColor = cinemaGold.copy(alpha = .42f),
        contentSpacing = 10.dp,
        backgroundContent = {
            if (step == CinemaStep.RESULT) ScoutBackdrop(choice?.shown?.pick?.backdrop, wide = true)
        }
    ) {
        when (step) {
            CinemaStep.MENU -> {
                ScoutChip(
                    stringResource(R.string.cinema_quick),
                    onClick = {
                        configured = false
                        rejected = emptySet()
                        step = CinemaStep.LOADING
                    },
                    modifier = Modifier.focusRequester(first),
                    primary = true
                )
                Text(stringResource(R.string.cinema_quick_description))
                ScoutChip(stringResource(R.string.cinema_session), onClick = { step = CinemaStep.SESSION })
                Text(stringResource(R.string.cinema_session_description))
            }
            CinemaStep.SESSION -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScoutChip(stringResource(R.string.cinema_company), onClick = onPickCompany, modifier = Modifier.focusRequester(first))
                    ScoutChip(
                        stringResource(R.string.cinema_guest),
                        onClick = { guest = if (guest == null) ScoutCinemaGuest() else null },
                        on = guest != null
                    )
                    ScoutChip(
                        stringResource(R.string.cinema_profile_preferences),
                        onClick = { loadingGenre = true; step = CinemaStep.PREFERENCES }
                    )
                    guest?.let { companion ->
                        ScoutChip(
                            stringResource(R.string.cinema_guest_family),
                            onClick = { guest = companion.copy(familyLimit = !companion.familyLimit) },
                            on = companion.familyLimit
                        )
                    }
                }
                guest?.let { companion ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(28 to R.string.cinema_guest_action, 35 to R.string.cinema_guest_comedy, 18 to R.string.cinema_guest_drama).forEach { (genre, label) ->
                            ScoutChip(
                                stringResource(label),
                                onClick = { guest = companion.copy(genre = genre.takeUnless { it == companion.genre }) },
                                on = genre == companion.genre
                            )
                        }
                    }
                }
                Text(stringResource(R.string.cinema_mood))
                cinemaMoods.chunked(5).forEach { moods ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        moods.forEach { candidate ->
                            ScoutChip(
                                stringResource(candidate?.labelRes ?: R.string.cinema_any_mood),
                                onClick = { mood = candidate },
                                on = mood == candidate
                            )
                        }
                    }
                }
                Text(stringResource(R.string.scout_q_how_long))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cinemaDurations.forEach { candidate ->
                        ScoutChip(
                            if (candidate == null) stringResource(R.string.cinema_no_limit)
                            else stringResource(R.string.cinema_minutes, candidate),
                            onClick = { duration = candidate },
                            on = candidate == duration
                        )
                    }
                }
                ScoutChip(
                    stringResource(R.string.cinema_allow_rewatch),
                    onClick = { allowRewatch = !allowRewatch },
                    on = allowRewatch
                )
                ScoutChip(
                    stringResource(R.string.cinema_find),
                    onClick = {
                        configured = true
                        rejected = emptySet()
                        step = CinemaStep.LOADING
                    },
                    primary = true
                )
            }
            CinemaStep.PREFERENCES -> {
                Text(stringResource(R.string.cinema_profile_hint))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to R.string.cinema_no_genre, 28 to R.string.cinema_genre_action,
                        35 to R.string.cinema_genre_comedy, 18 to R.string.cinema_genre_drama).forEach { (genre, label) ->
                        ScoutChip(
                            stringResource(label),
                            onClick = { favoriteGenre = genre },
                            enabled = !loadingGenre,
                            on = favoriteGenre == genre,
                            modifier = if (genre == null) Modifier.focusRequester(first) else Modifier
                        )
                    }
                }
                ScoutChip(
                    stringResource(R.string.cinema_save_genre),
                    onClick = {
                        scope.launch {
                            if (scoutReadPolicy { saveGenre(favoriteGenre) } != null) {
                                step = CinemaStep.SESSION
                            }
                        }
                    },
                    enabled = !loadingGenre,
                    primary = true
                )
            }
            CinemaStep.LOADING -> Text(stringResource(R.string.cinema_searching))
            CinemaStep.EMPTY -> {
                Text(stringResource(R.string.cinema_empty))
                ScoutChip(
                    stringResource(R.string.cinema_adjust),
                    onClick = { step = CinemaStep.SESSION },
                    modifier = Modifier.focusRequester(first)
                )
            }
            CinemaStep.TRAILER -> {
                when {
                    trailerLoading -> Text(stringResource(R.string.cinema_trailer_loading))
                    trailerFailed -> Text(stringResource(R.string.cinema_trailer_unavailable))
                    else -> trailer?.let { source ->
                        TrailerPlayer(
                            trailerUrl = source.videoUrl,
                            trailerAudioUrl = source.audioUrl,
                            isPlaying = true,
                            onEnded = { step = CinemaStep.RESULT },
                            onError = { trailerFailed = true },
                            modifier = Modifier.fillMaxWidth(.78f).aspectRatio(16f / 9f)
                        )
                    }
                }
                ScoutChip(
                    stringResource(R.string.cinema_back_to_pick),
                    onClick = { step = CinemaStep.RESULT },
                    modifier = Modifier.focusRequester(first)
                )
            }
            CinemaStep.RESULT -> choice?.let { result ->
                val pick = result.shown.pick
                val detail = result.shown.detail
                ScoutHeroLogo(result.logo, pick.name, 54.dp)
                Text(
                    listOfNotNull(
                        pick.released?.take(4),
                        detail?.minutes?.let { stringResource(R.string.cinema_minutes, it) },
                        pick.rating?.takeIf { it > 0 }?.let { stringResource(R.string.cinema_rating, it) },
                        detail?.genres?.take(3)?.joinToString(", ")?.takeIf { it.isNotBlank() }
                    ).joinToString("  ·  "),
                    color = Color(0xFFE2DDDA)
                )
                HorizontalDivider(color = cinemaGold.copy(alpha = .45f))
                Text(pick.overview.orEmpty(), maxLines = 5, overflow = TextOverflow.Ellipsis)
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color.Black.copy(alpha = .74f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.cinema_reason), color = cinemaGold)
                    Text(
                        if (configured && mood != null) stringResource(
                            R.string.cinema_mood_reason, stringResource(requireNotNull(mood).labelRes)
                        ) else stringResource(R.string.cinema_filter_reason)
                    )
                }
                if (attendanceAvailable || (configured && guest != null)) {
                    ScoutChip(
                        stringResource(R.string.close_attendance_opt_in),
                        onClick = { askAttendance = !askAttendance },
                        on = askAttendance
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoutChip(
                        stringResource(R.string.hero_play),
                        onClick = { onPlay(result, askAttendance, configured && guest != null) },
                        modifier = Modifier.focusRequester(first),
                        primary = true
                    )
                    ScoutChip(
                        stringResource(R.string.cinema_trailer),
                        onClick = {
                            trailer = null
                            trailerFailed = false
                            trailerLoading = true
                            returnToTrailerButton = true
                            step = CinemaStep.TRAILER
                        },
                        modifier = Modifier.focusRequester(trailerButton)
                    )
                    ScoutChip(
                        stringResource(R.string.cinema_another),
                        onClick = {
                            rejected = rejected + pick.key
                            step = CinemaStep.LOADING
                        }
                    )
                }
            }
        }
    }
}
