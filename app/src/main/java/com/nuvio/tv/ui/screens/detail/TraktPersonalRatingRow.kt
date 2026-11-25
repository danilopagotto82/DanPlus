@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.tmdb.TmdbApiKey
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.TraktAuthDataStore
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbSeasonResponse
import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.repository.TraktPersonalRatingRepository
import com.nuvio.tv.data.repository.TraktPersonalRatingTarget
import com.nuvio.tv.data.repository.TraktRatingOwner
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.screens.scout.ScoutChip
import com.nuvio.tv.ui.theme.NuvioTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class TraktPersonalRatingViewModel @Inject constructor(
    private val profiles: ProfileManager,
    private val auth: TraktAuthDataStore,
    private val repository: TraktPersonalRatingRepository,
    private val tmdbIds: TmdbService,
    private val tmdbApi: TmdbApi
) : ViewModel() {
    val activeProfile = profiles.activeProfileId
    internal val revision = profiles.profileSelectionRevision
    internal val authorization = auth.state

    internal suspend fun target(meta: Meta, video: Video?, owner: TraktRatingOwner): TraktPersonalRatingTarget {
        repository.checkOwner(owner)
        val imdb = (meta.imdbId ?: meta.id).takeIf { it.matches(Regex("tt[0-9]+")) }
        if (meta.apiType == "movie" && imdb != null) return TraktPersonalRatingTarget.Movie(TraktIdsDto(imdb = imdb))
        val id = checkNotNull(tmdbIds.ensureTmdbId(meta.id, meta.apiType, imdb)?.toIntOrNull()?.takeIf { it > 0 })
        repository.checkOwner(owner)
        if (meta.apiType == "movie") return TraktPersonalRatingTarget.Movie(TraktIdsDto(tmdb = id))
        val season = checkNotNull(video?.season).also { check(it >= 0) }
        val episode = checkNotNull(video?.episode).also { check(it > 0) }
        val response = tmdbApi.getTvSeasonDetails(id, season, TmdbApiKey.value)
        repository.checkOwner(owner)
        val episodeId = exactRatingEpisodeId(response.body().takeIf { response.isSuccessful }, season, episode)
        return TraktPersonalRatingTarget.Episode(TraktIdsDto(tmdb = checkNotNull(episodeId)))
    }

    internal suspend fun read(owner: TraktRatingOwner, target: TraktPersonalRatingTarget): Int? =
        repository.read(owner, target).also { repository.checkOwner(owner) }

    internal suspend fun save(owner: TraktRatingOwner, target: TraktPersonalRatingTarget, rating: Int?) =
        repository.set(owner, target, rating).also { repository.checkOwner(owner) }
}

internal fun exactRatingEpisodeId(details: TmdbSeasonResponse?, season: Int, episode: Int): Int? =
    details?.takeIf { season >= 0 && episode > 0 && it.seasonNumber == season }
        ?.episodes?.singleOrNull { it.episodeNumber == episode && (it.seasonNumber == null || it.seasonNumber == season) }
        ?.id?.takeIf { it in 1L..Int.MAX_VALUE.toLong() }?.toInt()

@Composable
internal fun TraktPersonalRatingRow(meta: Meta, episodes: List<Video>, viewModel: TraktPersonalRatingViewModel = hiltViewModel()) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val revision by viewModel.revision.collectAsStateWithLifecycle()
    val authorization by viewModel.authorization.collectAsStateWithLifecycle(initialValue = null)
    val session = authorization?.takeIf { it.isAuthenticated } ?: return
    val owner = TraktRatingOwner(profile, revision, session.authorizationGeneration)
    val choices = episodes.filter { (it.season ?: -1) >= 0 && (it.episode ?: 0) > 0 }.distinctBy { it.season to it.episode }
    if (meta.apiType != "movie" && choices.isEmpty()) return

    val scope = rememberCoroutineScope()
    var open by remember(owner, meta.id, meta.apiType) { mutableStateOf(false) }
    var selected by remember(owner, meta.id, meta.apiType) { mutableStateOf<Video?>(null) }
    val chosen = selected?.let { episode ->
        choices.firstOrNull { it.id == episode.id && it.season == episode.season && it.episode == episode.episode }
    }
    val ratingKey = Triple(meta.id, meta.apiType, chosen)
    var score by remember(owner, ratingKey) { mutableStateOf<Int?>(null) }
    var current by remember(owner, ratingKey) { mutableStateOf<Int?>(null) }
    var target by remember(owner, ratingKey) { mutableStateOf<TraktPersonalRatingTarget?>(null) }
    var failed by remember(owner, ratingKey) { mutableStateOf(false) }
    var saved by remember(owner, ratingKey) { mutableStateOf(false) }
    var writing by remember(owner, ratingKey) { mutableStateOf(false) }
    var attempt by remember(owner, ratingKey) { mutableStateOf(0) }
    val available = meta.apiType == "movie" || chosen != null
    LaunchedEffect(open, owner, ratingKey, attempt) {
        if (!open || !available) return@LaunchedEffect
        failed = false
        saved = false
        target = null
        try {
            val resolved = viewModel.target(meta, chosen, owner)
            current = viewModel.read(owner, resolved)
            score = current
            target = resolved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    Button(
        onClick = { open = true },
        modifier = Modifier.padding(horizontal = NuvioTheme.spacing.xxxl, vertical = NuvioTheme.spacing.sm)
    ) {
        Text(stringResource(R.string.e05_rating_none))
    }
    if (open) NuvioDialog(
        title = stringResource(R.string.e05_rating_title),
        subtitle = chosen?.title ?: meta.name,
        onDismiss = { if (!writing) { open = false; selected = null } },
        width = 660.dp
    ) {
        if (choices.isNotEmpty() && !available) {
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(choices, key = { it.id }) { episode ->
                    ScoutChip("${episode.season} × ${episode.episode} · ${episode.title}", { selected = episode })
                }
            }
        } else if (available) {
            if (failed) Text(stringResource(R.string.e05_rating_error), color = NuvioTheme.colors.Error)
            if (saved) Text(stringResource(R.string.e05_rating_saved))
            if (target == null) {
                if (failed) Button(onClick = { attempt++ }) { Text(stringResource(R.string.action_retry)) }
                else com.nuvio.tv.ui.components.LoadingIndicator()
            } else {
                current?.let { Text(stringResource(R.string.e05_rating_value, it)) }
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(10) { index ->
                        val value = index + 1
                        ScoutChip(value.toString(), { score = value; saved = false }, on = score == value, enabled = !writing)
                    }
                }
                fun persist(value: Int?) {
                    val requested = target ?: return
                    if (writing) return
                    writing = true
                    failed = false
                    saved = false
                    scope.launch {
                        try {
                            viewModel.save(owner, requested, value)
                            current = value
                            score = value
                            saved = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            writing = false
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { persist(score) }, enabled = score != null && !writing) {
                        Text(stringResource(R.string.action_save))
                    }
                    Button(onClick = { persist(null) }, enabled = current != null && !writing) {
                        Text(stringResource(R.string.e05_remove))
                    }
                    if (chosen != null) {
                        Button(onClick = { selected = null }, enabled = !writing) { Text(stringResource(R.string.action_back)) }
                    }
                }
            }
        }
    }
}
