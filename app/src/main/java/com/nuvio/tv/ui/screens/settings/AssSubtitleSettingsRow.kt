package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.player.ImportedSubtitleFonts
import com.nuvio.tv.data.local.AssSubtitleStyleSettings
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AssSubtitleSettingsRow(viewModel: PlaybackSettingsViewModel, enabled: Boolean) {
    var owner by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    SettingsActionRow(
        title = stringResource(R.string.e05_ass_title),
        subtitle = stringResource(R.string.e05_ass_subtitle),
        enabled = enabled,
        onClick = { owner = viewModel.assStyleOwner() }
    )
    val selection = owner ?: return
    if (!enabled || selection != viewModel.assStyleOwner()) {
        LaunchedEffect(selection) { owner = null }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember(selection) { mutableStateOf<AssSubtitleStyleSettings?>(null) }
    var pickingFont by remember(selection) { mutableStateOf(false) }
    var uploadingFont by remember(selection) { mutableStateOf(false) }
    var error by remember(selection) { mutableStateOf(false) }
    var saving by remember(selection) { mutableStateOf(false) }
    LaunchedEffect(selection) {
        try {
            val loaded = viewModel.readAssStyle(selection.first)
            if (viewModel.assStyleOwner() == selection) draft = loaded
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        }
    }
    val preview = draft
    when {
        uploadingFont -> SubtitleFontUploadDialog(
            onUploaded = { font ->
                if (viewModel.assStyleOwner() == selection) draft = draft?.copy(importedFontId = font.id)
            },
            onDismiss = { uploadingFont = false }
        )
        pickingFont -> ImportedSubtitleFontsDialog(
            selectedId = preview?.importedFontId,
            onSelect = { id -> if (viewModel.assStyleOwner() == selection) draft = draft?.copy(importedFontId = id) },
            onUpload = { uploadingFont = true },
            onDismiss = { pickingFont = false }
        )
        else -> NuvioDialog(
            title = stringResource(R.string.e05_ass_title),
            onDismiss = { if (!saving) owner = null }
        ) {
            if (preview != null) {
                val font = remember(preview.importedFontId) { ImportedSubtitleFonts.get(context, preview.importedFontId) }
                Column(Modifier.heightIn(max = 294.dp).verticalScroll(rememberScrollState())) {
                    SliderSettingsItem(
                        title = stringResource(R.string.sub_size),
                        value = preview.size, valueText = "${preview.size}%",
                        minValue = 50, maxValue = 200, step = 5,
                        onValueChange = { draft = draft?.copy(size = it) }, enabled = !saving
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.e05_ass_mpv),
                        subtitle = stringResource(R.string.e05_ass_subtitle),
                        value = stringResource(if (preview.customizeInMpv) R.string.e05_ass_custom else R.string.e05_ass_author),
                        enabled = !saving,
                        onClick = { draft = draft?.copy(customizeInMpv = !preview.customizeInMpv) }
                    )
                    SliderSettingsItem(
                        title = stringResource(R.string.e05_ass_offset),
                        value = preview.verticalOffset, valueText = "${preview.verticalOffset}%",
                        minValue = -20, maxValue = 50, step = 1,
                        onValueChange = { draft = draft?.copy(verticalOffset = it) },
                        enabled = !saving && preview.customizeInMpv
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.subtitle_font_family),
                        subtitle = null,
                        value = font?.family ?: stringResource(R.string.e05_ass_author),
                        enabled = !saving && preview.customizeInMpv,
                        onClick = { pickingFont = true }
                    )
                }
            }
            if (error) Text(stringResource(R.string.e05_ass_error))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsDialogActionButton(
                    text = stringResource(R.string.action_save), primary = true,
                    enabled = preview != null && !saving,
                    onClick = {
                        val style = draft
                        if (!saving && style != null && viewModel.assStyleOwner() == selection) {
                            saving = true
                            error = false
                            scope.launch {
                                try {
                                    viewModel.setAssSubtitleStyle(selection.first, style, selection.second)
                                    owner = null
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    error = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    }
                )
                SettingsDialogActionButton(stringResource(R.string.action_cancel), { owner = null }, enabled = !saving)
            }
        }
    }
}
