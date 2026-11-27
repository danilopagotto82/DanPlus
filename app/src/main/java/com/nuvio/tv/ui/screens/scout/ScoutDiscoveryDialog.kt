package com.nuvio.tv.ui.screens.scout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.scout.ScoutSlices
import com.nuvio.tv.domain.model.ScoutAdvancedSearch
import com.nuvio.tv.domain.model.ScoutDiscoveryHistory
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.collection.NuvioTextField
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScoutDiscoveryDialog(viewModel: ScoutViewModel, audience: ScoutAudience, onDismiss: () -> Unit) {
    var query by remember(audience) { mutableStateOf(ScoutAdvancedSearch()) }
    var year by remember(audience) { mutableStateOf("") }
    var language by remember(audience) { mutableStateOf("") }
    var keywordText by remember(audience) { mutableStateOf("") }
    var keywordNames by remember(audience) { mutableStateOf(emptyMap<Int, String>()) }
    var found by remember(audience) { mutableStateOf(emptyList<Pair<Int, String>>()) }
    var history by remember(audience) { mutableStateOf<ScoutDiscoveryHistory?>(null) }
    var page by remember(audience) { mutableStateOf(0) }
    val context = LocalContext.current
    val genres = remember {
        ScoutSlices.all.mapNotNull { slice ->
            slice.id.removePrefix("genre_").toIntOrNull()?.let { it to slice }
        }
    }
    fun current() = viewModel.discoveryAudience() == audience
    fun draft() = query.copy(year = year.toIntOrNull(), originalLanguage = language.ifBlank { null }).normalized()
    fun search(selected: ScoutAdvancedSearch) {
        if (!current()) return
        viewModel.applyAdvancedSearch(selected, audience)
        onDismiss()
    }
    LaunchedEffect(audience) { history = viewModel.readDiscovery(audience) }
    LaunchedEffect(audience, keywordText) {
        found = emptyList()
        if (keywordText.trim().length < 2) return@LaunchedEffect
        delay(300)
        found = viewModel.discoveryKeywords(audience, keywordText)
    }
    if (!current()) return
    NuvioDialog(onDismiss = onDismiss, title = stringResource(R.string.e05_scout_filters), width = 680.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoutChip(stringResource(R.string.e05_scout_filters), { page = 0 }, primary = page == 0)
            ScoutChip(stringResource(R.string.e05_scout_saved), { page = 1 }, primary = page == 1)
            ScoutChip(stringResource(R.string.e05_scout_history), { page = 2 }, primary = page == 2)
        }
        Column(
            Modifier.fillMaxWidth().heightIn(max = 314.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (page) {
                1 -> {
                    if (history?.savedSearches.isNullOrEmpty()) Text(stringResource(R.string.e05_scout_empty))
                    history?.savedSearches.orEmpty().forEach { saved ->
                        val label = listOfNotNull(
                            saved.exactPhrase.takeIf(String::isNotBlank),
                            saved.year?.toString(), saved.originalLanguage,
                            saved.genreIds.takeIf { it.isNotEmpty() }?.let {
                                stringResource(R.string.e05_scout_saved_count, it.size)
                            },
                            saved.keywordIds.takeIf { it.isNotEmpty() }?.let {
                                stringResource(R.string.e05_scout_keyword_count, it.size)
                            }
                        ).joinToString(" · ")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoutChip(label.ifBlank { stringResource(R.string.e05_scout_filters) }, { search(saved) })
                            ScoutChip(stringResource(R.string.e05_remove), {
                                if (current()) {
                                    viewModel.forgetAdvancedSearch(saved, audience)
                                    history = history?.copy(savedSearches = history?.savedSearches.orEmpty() - saved)
                                }
                            })
                        }
                    }
                }
                2 -> {
                    if (history?.suggestions.isNullOrEmpty()) Text(stringResource(R.string.e05_scout_empty))
                    history?.suggestions.orEmpty().forEach { pick ->
                        Text(pick.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    NuvioTextField(query.exactPhrase, { query = query.copy(exactPhrase = it.take(160)) },
                        Modifier.fillMaxWidth(), stringResource(R.string.cinema_search_terms))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NuvioTextField(year, { year = it.filter(Char::isDigit).take(4) },
                            Modifier.weight(1f), stringResource(R.string.e05_scout_year))
                        NuvioTextField(language, { language = it.filter(Char::isLetter).take(2) },
                            Modifier.weight(1f), stringResource(R.string.e05_scout_language))
                    }
                    Text(stringResource(R.string.e05_scout_genres))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.forEach { (id, slice) ->
                            val selected = id in query.genreIds
                            ScoutChip(slice.title { res, args -> context.getString(res, *args) }, {
                                if (selected || query.genreIds.size < 12) {
                                    query = query.copy(genreIds = if (selected) query.genreIds - id else query.genreIds + id)
                                }
                            }, primary = selected)
                        }
                    }
                    NuvioTextField(keywordText, { keywordText = it.take(80) }, Modifier.fillMaxWidth(),
                        stringResource(R.string.e05_scout_keywords))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (keywordNames.entries.map { it.key to it.value } + found).distinctBy { it.first }.forEach { (id, name) ->
                            val selected = id in query.keywordIds
                            ScoutChip(name, {
                                if (selected) {
                                    keywordNames = keywordNames - id
                                    query = query.copy(keywordIds = query.keywordIds - id)
                                } else if (query.keywordIds.size < 12) {
                                    keywordNames = keywordNames + (id to name)
                                    query = query.copy(keywordIds = query.keywordIds + id)
                                }
                            }, primary = selected)
                        }
                    }
                    if (query.keywordIds.size > 1 || query.titleTerms().size > 1) {
                        ScoutChip(
                            stringResource(if (query.matchAnyKeyword) R.string.e05_scout_any else R.string.e05_scout_all),
                            { query = query.copy(matchAnyKeyword = !query.matchAnyKeyword) }
                        )
                    }
                }
            }
        }
        if (page == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoutChip(stringResource(R.string.nav_search), { search(draft()) }, primary = true)
                ScoutChip(stringResource(R.string.action_save), {
                    if (current() && draft() != ScoutAdvancedSearch()) {
                        val saved = draft()
                        viewModel.saveAdvancedSearch(saved, audience)
                        val data = history ?: ScoutDiscoveryHistory()
                        history = data.copy(savedSearches = (listOf(saved) + data.savedSearches).distinct().take(20))
                        page = 1
                    }
                })
            }
        }
    }
}
