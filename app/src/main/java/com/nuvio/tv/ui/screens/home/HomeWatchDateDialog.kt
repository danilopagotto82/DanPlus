package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.collection.NuvioTextField
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import com.nuvio.tv.ui.screens.scout.ScoutChip
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal data class WatchDateRequest(val item: ContinueWatchingItem, val profileId: Int, val revision: Long, val generation: Long)

private val watchDateFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)

internal fun watchDateMillis(text: String, zone: ZoneId, now: Long): Long? {
    val parsed = try {
        LocalDateTime.parse(text.trim(), watchDateFormat)
    } catch (_: DateTimeParseException) {
        return null
    }
    val offset = zone.rules.getValidOffsets(parsed).singleOrNull() ?: return null
    return parsed.toInstant(offset).toEpochMilli().takeIf { it in 1..now }
}

@Composable
internal fun HomeWatchDateDialog(viewModel: HomeViewModel) {
    val request by viewModel.watchDate.collectAsStateWithLifecycle()
    val revision by viewModel.profileManager.profileSelectionRevision.collectAsStateWithLifecycle()
    val generation by viewModel.profileManager.profileHistoryGenerationChanges.collectAsStateWithLifecycle()
    val current = request ?: return
    LaunchedEffect(revision, generation, current) {
        if (revision != current.revision || generation != current.generation) viewModel.closeWatchDate()
    }
    if (revision != current.revision || generation != current.generation) return
    val first = remember { FocusRequester() }
    var text by remember(current) { mutableStateOf("") }
    val timestamp = watchDateMillis(text, ZoneId.systemDefault(), System.currentTimeMillis())
    NuvioDialog(
        onDismiss = viewModel::closeWatchDate,
        title = stringResource(R.string.close_watch_date_title),
        cinematicPageSurface = true
    ) {
        NuvioTextField(text, { text = it.take(16) }, Modifier.fillMaxWidth(), "YYYY-MM-DD HH:MM", first)
        ScoutChip(
            stringResource(if (text.isBlank()) R.string.close_watch_date_now else R.string.close_save),
            enabled = text.isBlank() || timestamp != null,
            onClick = { viewModel.confirmWatchDate(current, timestamp ?: System.currentTimeMillis()) },
            primary = true
        )
    }
    LaunchedEffect(first) { first.requestFocusAfterFrames() }
}
