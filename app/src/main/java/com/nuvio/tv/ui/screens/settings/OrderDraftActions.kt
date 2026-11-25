package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R

internal data class OrderDraft(val owner: Pair<Int, Long>, val baseline: List<String>, val keys: List<String> = baseline) {
    fun move(key: String, destination: Int): OrderDraft {
        val index = keys.indexOf(key)
        if (index < 0 || destination !in keys.indices || index == destination) return this
        return copy(keys = keys.toMutableList().apply { add(destination, removeAt(index)) })
    }

    fun <T> display(items: List<T>, keyOf: (T) -> String): List<T> {
        if (items.map(keyOf) != baseline) return items
        val byKey = items.associateBy(keyOf)
        return keys.mapNotNull(byKey::get)
    }
}

@Composable
internal fun OrderDraftActions(onSave: () -> Unit, onCancel: () -> Unit, enabled: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsDialogActionButton(stringResource(R.string.action_save), onSave, primary = true, enabled = enabled)
        SettingsDialogActionButton(stringResource(R.string.action_cancel), onCancel, enabled = enabled)
    }
}
