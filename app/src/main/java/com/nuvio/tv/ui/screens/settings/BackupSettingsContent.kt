package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.nuvio.tv.core.backup.DriveJournalOutcome
import com.nuvio.tv.ui.components.NuvioDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.backup.BackupFlow
import com.nuvio.tv.ui.screens.backup.BackupPhoneOverlay
import com.nuvio.tv.ui.screens.backup.BackupViewModel
import com.nuvio.tv.ui.screens.backup.ReturnFocusWhenClosed
import com.nuvio.tv.ui.screens.backup.dateTime

@Composable
internal fun BackupSettingsContent(
    initialFocusRequester: FocusRequester?,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val drive by viewModel.driveStatus.collectAsStateWithLifecycle()
    val result by viewModel.driveOutcome.collectAsStateWithLifecycle()
    val lastSuccess by viewModel.driveLastSuccess.collectAsStateWithLifecycle()
    val dialog by viewModel.driveDialog.collectAsStateWithLifecycle()
    val driveFocus = remember { FocusRequester() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val nowFocus = remember { FocusRequester() }
    val savePhoneFocus = remember { FocusRequester() }
    val restoreBoxFocus = remember { FocusRequester() }
    val restorePhoneFocus = remember { FocusRequester() }
    var opener by remember { mutableStateOf<FocusRequester?>(null) }
    ReturnFocusWhenClosed(state, opener)
    var driveWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(dialog, state.pin) {
        val open = dialog != null || state.pin is com.nuvio.tv.ui.screens.backup.BackupPinStep.Drive
        if (!open && driveWasOpen) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { driveFocus.requestFocus() }
        }
        driveWasOpen = open
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsDetailHeader(
                title = stringResource(R.string.backup_title),
                subtitle = stringResource(R.string.backup_subtitle)
            )
            SettingsGroupCard(title = stringResource(R.string.backup_group_back_up)) {
                SettingsActionRow(
                    title = stringResource(R.string.backup_now),
                    subtitle = stringResource(R.string.backup_now_sub),
                    value = state.lastBackupMs.takeIf { it > 0L }?.let { stringResource(R.string.backup_last, dateTime(context, it)) },
                    onClick = { opener = nowFocus; viewModel.backUpNow() },
                    modifier = Modifier.focusRequester(nowFocus)
                        .then(if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier)
                )
                SettingsToggleRow(
                    title = stringResource(R.string.backup_include_secrets),
                    subtitle = stringResource(R.string.backup_include_secrets_sub),
                    checked = state.includeSecrets,
                    onToggle = { viewModel.setIncludeSecrets(!state.includeSecrets) }
                )
                SettingsActionRow(
                    title = stringResource(R.string.backup_save_phone),
                    subtitle = stringResource(R.string.backup_save_phone_sub),
                    onClick = { opener = savePhoneFocus; viewModel.openPhone(saving = true) },
                    modifier = Modifier.focusRequester(savePhoneFocus)
                )
            }
            SettingsGroupCard(title = stringResource(R.string.backup_group_restore)) {
                SettingsActionRow(
                    title = stringResource(R.string.backup_restore_box),
                    subtitle = null,
                    value = state.backups.firstOrNull()?.let { stringResource(R.string.backup_newest, dateTime(context, it.modifiedMs)) },
                    onClick = { opener = restoreBoxFocus; viewModel.openChoose() },
                    modifier = Modifier.focusRequester(restoreBoxFocus)
                )
                SettingsActionRow(
                    title = stringResource(R.string.backup_restore_phone),
                    subtitle = stringResource(R.string.backup_restore_phone_sub),
                    onClick = { opener = restorePhoneFocus; viewModel.openPhone(saving = false) },
                    modifier = Modifier.focusRequester(restorePhoneFocus)
                )
            }
            SettingsGroupCard(title = stringResource(R.string.drive_sync_title)) {
                val label = when (result) {
                    DriveJournalOutcome.IN_SYNC -> R.string.drive_sync_current
                    DriveJournalOutcome.AWAITING_REMOTE -> R.string.backup_working
                    DriveJournalOutcome.CAPACITY -> R.string.drive_sync_capacity
                    DriveJournalOutcome.INVALID -> R.string.drive_sync_invalid
                    DriveJournalOutcome.CONFLICT -> R.string.drive_sync_conflict
                    DriveJournalOutcome.UNAVAILABLE -> R.string.drive_sync_error
                    null -> when {
                        drive.connected -> R.string.drive_sync_connected
                        drive.available -> R.string.drive_sync_connect
                        else -> R.string.drive_sync_unavailable
                    }
                }
                SettingsActionRow(
                    title = stringResource(label),
                    subtitle = stringResource(R.string.drive_sync_summary),
                    value = lastSuccess?.let { dateTime(context, it) },
                    modifier = Modifier.focusRequester(driveFocus),
                    onClick = {
                        when {
                            !drive.available -> Unit
                            !drive.connected -> viewModel.connectDrive()
                            else -> viewModel.syncDriveNow()
                        }
                    }
                )
                if (drive.connected) {
                    SettingsActionRow(stringResource(R.string.drive_sync_now), subtitle = null, onClick = viewModel::syncDriveNow)
                    SettingsToggleRow(stringResource(R.string.drive_sync_auto), subtitle = null, checked = drive.automatic,
                        onToggle = { viewModel.driveAutomatic(!drive.automatic) })
                    if (result == DriveJournalOutcome.CONFLICT) {
                        SettingsActionRow(stringResource(R.string.drive_sync_resolve), subtitle = null, onClick = viewModel::openDriveConflict)
                    }
                    SettingsActionRow(stringResource(R.string.drive_sync_disconnect), subtitle = null, onClick = { viewModel.disconnectDrive() })
                    SettingsActionRow(stringResource(R.string.drive_sync_revoke), subtitle = null, onClick = viewModel::confirmDriveRevoke)
                }
            }
        }
        BackupPhoneOverlay(state, viewModel, Modifier.clip(settingsGroupShape()))
    }
    BackupFlow(state, viewModel)
    dialog?.let { current ->
        val title = when (current) {
            BackupViewModel.DriveDialog.Working -> stringResource(R.string.backup_working)
            is BackupViewModel.DriveDialog.Code -> stringResource(R.string.drive_sync_connect)
            is BackupViewModel.DriveDialog.Conflicts -> stringResource(R.string.drive_sync_resolve)
            is BackupViewModel.DriveDialog.ConfirmRevoke -> stringResource(R.string.drive_sync_revoke)
        }
        val subtitle = when (current) {
            is BackupViewModel.DriveDialog.Code -> stringResource(R.string.drive_sync_code, current.url, current.code)
            is BackupViewModel.DriveDialog.ConfirmRevoke -> stringResource(R.string.drive_sync_revoke_warning)
            else -> null
        }
        NuvioDialog(onDismiss = viewModel::closeDriveDialog, title = title, subtitle = subtitle) {
            val cancel = remember { FocusRequester() }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SettingsDialogActionButton(stringResource(R.string.action_cancel), viewModel::closeDriveDialog,
                    modifier = Modifier.focusRequester(cancel))
                when (current) {
                    is BackupViewModel.DriveDialog.Conflicts -> {
                        SettingsDialogActionButton(stringResource(R.string.drive_sync_keep_local), {
                            viewModel.chooseDriveConflict(current, null)
                        })
                        current.preview.remote.forEach { (revision, values) ->
                            val yes = stringResource(R.string.drive_sync_yes)
                            val no = stringResource(R.string.drive_sync_no)
                            SettingsDialogActionButton(stringResource(R.string.drive_sync_branch,
                                revision.take(8), values.favoriteGenre?.toString() ?: "–",
                                if (values.scoutInMenu) yes else no, if (values.scoutListEmojis) yes else no,
                                if (values.scoutShowWatched) yes else no), {
                                viewModel.chooseDriveConflict(current, revision)
                            })
                        }
                    }
                    is BackupViewModel.DriveDialog.ConfirmRevoke ->
                        SettingsDialogActionButton(stringResource(R.string.drive_sync_revoke), { viewModel.disconnectDrive(true) })
                    else -> Unit
                }
            }
            LaunchedEffect(current) { runCatching { cancel.requestFocus() } }
        }
    }
}
