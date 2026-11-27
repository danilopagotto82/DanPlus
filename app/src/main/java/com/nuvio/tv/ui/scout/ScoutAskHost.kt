package com.nuvio.tv.ui.scout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.nuvio.tv.ui.screens.scout.ScoutChip
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.domain.model.ScoutAskTarget
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.PanelActionRow
import kotlinx.coroutines.delay

private const val SETTLE_AFTER_PLAYER_MS = 500L

/** Asks "How was it?" over whichever page the player returned to. */
@Composable
fun ScoutAskHost(isPlayerShowing: Boolean, viewModel: ScoutRatingViewModel = hiltViewModel()) {
    val pending by viewModel.pendingAsk.collectAsStateWithLifecycle()
    val attendance by viewModel.cinemaAttendance.collectAsStateWithLifecycle()
    var showing by remember { mutableStateOf<ScoutAskTarget?>(null) }

    LaunchedEffect(pending, isPlayerShowing) {
        val target = pending
        if (target == null) {
            showing = null
            return@LaunchedEffect
        }
        if (isPlayerShowing) return@LaunchedEffect
        delay(SETTLE_AFTER_PLAYER_MS)
        if (viewModel.shouldAsk(target)) showing = target else viewModel.dropAsk()
    }

    val target = showing
    if (target == null) {
        if (!isPlayerShowing && pending == null) attendance?.let { ticket ->
            ScoutAttendanceDialog(ticket) { profiles, guest ->
                viewModel.answerCinemaAttendance(ticket, profiles, guest)
            }
        }
        return
    }
    val focus = remember(target) { FocusRequester() }
    NuvioDialog(
        onDismiss = { viewModel.answerAsk(target, null) },
        title = stringResource(R.string.scout_ask_title, target.title.name),
        subtitle = stringResource(R.string.scout_ask_subtitle)
    ) {
        ScoutRateRow(
            current = null,
            onRate = { viewModel.answerAsk(target, it) },
            firstFocus = focus
        )
        PanelActionRow(
            label = stringResource(R.string.scout_ask_skip),
            onClick = { viewModel.answerAsk(target, null) }
        )
    }
    LaunchedEffect(target) {
        focus.requestFocusAfterFrames()
    }
}

@Composable
private fun ScoutAttendanceDialog(
    ticket: com.nuvio.tv.core.scout.CinemaPresenceTicket,
    onAnswer: (Set<Int>, Boolean) -> Unit
) {
    var selected by remember(ticket) { mutableStateOf(emptySet<Int>()) }
    var guest by remember(ticket) { mutableStateOf(false) }
    val focus = remember(ticket) { FocusRequester() }
    NuvioDialog(
        onDismiss = { onAnswer(emptySet(), false) },
        title = stringResource(R.string.close_attendance_title),
        subtitle = stringResource(R.string.close_attendance_private)
    ) {
        for ((id, name) in ticket.companions) {
            ScoutChip(name, onClick = { selected = if (id in selected) selected - id else selected + id }, on = id in selected)
        }
        if (ticket.guest) {
            ScoutChip(stringResource(R.string.cinema_guest), onClick = { guest = !guest }, on = guest)
        }
        ScoutChip(stringResource(R.string.close_attendance_confirm), onClick = { onAnswer(selected, guest) },
            enabled = selected.isNotEmpty() || guest, primary = true)
        ScoutChip(stringResource(R.string.close_skip), onClick = { onAnswer(emptySet(), false) },
            modifier = Modifier.focusRequester(focus))
    }
    LaunchedEffect(ticket) { focus.requestFocusAfterFrames() }
}
