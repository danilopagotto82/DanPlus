package com.nuvio.tv.ui.screens.scout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.scout.scoutReadPolicy
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import kotlinx.coroutines.launch

@Composable
internal fun ScoutCompanionForm(profile: UserProfile, viewModel: ScoutViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val first = remember(profile.id) { FocusRequester() }
    var relation by remember(profile.id) { mutableStateOf<String?>(null) }
    var ageBand by remember(profile.id) { mutableStateOf<String?>(null) }
    var ready by remember(profile.id) { mutableStateOf(false) }
    var saving by remember(profile.id) { mutableStateOf(false) }
    LaunchedEffect(profile.id) {
        val saved = scoutReadPolicy { viewModel.cinemaCompanion(profile.id) }
        if (saved != null) {
            relation = saved.relation
            ageBand = saved.ageBand
            ready = true
        }
        first.requestFocusAfterFrames()
    }
    NuvioDialog(onDismiss = onDismiss, title = profile.name, cinematicPageSurface = true) {
        Text(stringResource(R.string.close_companion_relation))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("family" to R.string.close_relation_family, "partner" to R.string.close_relation_partner,
                "friend" to R.string.close_relation_friend).forEach { (code, label) ->
                ScoutChip(stringResource(label), onClick = { relation = code.takeUnless { it == relation } },
                    enabled = ready && !saving, on = relation == code)
            }
        }
        Text(stringResource(R.string.close_companion_age))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("child" to R.string.close_age_child, "teen" to R.string.close_age_teen,
                "adult" to R.string.close_age_adult).forEach { (code, label) ->
                ScoutChip(stringResource(label), onClick = { ageBand = code.takeUnless { it == ageBand } },
                    enabled = ready && !saving, on = ageBand == code)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScoutChip(stringResource(R.string.close_skip), onClick = {
                saving = true
                scope.launch {
                    if (ready) scoutReadPolicy { viewModel.skipCinemaOnboarding(profile.id) }
                    onDismiss()
                }
            }, enabled = !saving, modifier = Modifier.focusRequester(first))
            ScoutChip(stringResource(R.string.close_save), onClick = {
                saving = true
                scope.launch {
                    if (scoutReadPolicy { viewModel.saveCinemaCompanion(profile.id, relation, ageBand) } != null) onDismiss()
                    saving = false
                }
            }, enabled = ready && !saving, primary = true)
        }
    }
}
