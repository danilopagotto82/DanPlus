package com.nuvio.tv.ui.screens.player

import android.widget.Toast
import com.nuvio.tv.R
import com.nuvio.tv.data.local.InternalPlayerEngine
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

data class DiscTitleOption(val id: String, val durationMs: Long)

internal class DiscExtraSession<T : Any> {
    private var generation = 0L
    private var snapshot: T? = null
    private var returning = false
    val active: Boolean get() = snapshot != null
    val ticket: Long? get() = generation.takeIf { active }

    fun begin(feature: T): Long? {
        if (active) return null
        snapshot = feature
        returning = false
        return ++generation
    }

    fun takeReturn(ticket: Long): T? {
        if (ticket != this.ticket || returning) return null
        returning = true
        return snapshot
    }

    fun finish(ticket: Long) {
        if (ticket == this.ticket && returning) snapshot = null
    }
}

internal data class DiscFeatureSnapshot(
    val positionMs: Long,
    val paused: Boolean,
    val engine: InternalPlayerEngine,
    val selectedTracks: PlayerRuntimeController.PendingEngineSwitchTrackPreference?,
    val rememberedTracks: PlayerRuntimeController.TrackPreference?,
    val persistedTracks: PlayerRuntimeController.TrackPreference?,
    val subtitleDelayMs: Int,
    val speed: Float
)

internal class DiscPlaybackSession(
    val url: String,
    val headers: Map<String, String>,
    val disc: BlurayPlaylistResolver.Disc,
    val startPaused: Boolean,
    val startPositionMs: Long?
) {
    var feature = disc.initialTitle
    var extra: BlurayPlaylistResolver.Title? = null
    val extraSession = DiscExtraSession<DiscFeatureSnapshot>()
    val clips = mutableMapOf<String, List<BlurayPlaylistResolver.Clip>>()
}

internal data class DiscPlaybackSource(
    val titleId: String,
    val clips: List<BlurayPlaylistResolver.Clip>,
    val extraTicket: Long?
)

internal val PlayerRuntimeController.isPlayingDiscExtra: Boolean
    get() = discPlaybackSession?.extraSession?.active == true

internal fun PlayerRuntimeController.clearDiscPlayback() {
    discPreparationJob?.cancel()
    discPreparationJob = null
    if (discPlaybackSession == null) return
    discPlaybackSession = null
    _uiState.update {
        it.copy(discTitles = emptyList(), showDiscTitlePicker = false, discFeatureId = null, discExtraId = null)
    }
}

internal suspend fun PlayerRuntimeController.prepareDiscPlayback(
    location: BlurayPlaylistResolver.Location,
    url: String,
    headers: Map<String, String>,
    generation: Long,
    startPaused: Boolean,
    startPositionMs: Long?
): DiscPlaybackSource? {
    val job = currentCoroutineContext()[Job]
    discPreparationJob = job
    try {
        val session = discPlaybackSession ?: DiscPlaybackSession(
            url, headers.toMap(), BlurayPlaylistResolver.resolve(location, headers), startPaused, startPositionMs
        ).also {
            if (!playbackRecoveryGate.isCurrent(generation, url, headers)) return null
            discPlaybackSession = it
        }
        val title = session.extra ?: session.feature
        _uiState.update {
            it.copy(
                discTitles = session.disc.titles.map { item -> DiscTitleOption(item.id, item.durationMs) },
                discFeatureId = session.feature?.id,
                discExtraId = session.extra?.id,
                showDiscTitlePicker = title == null
            )
        }
        if (title == null) return null
        val clips = session.clips[title.id] ?: BlurayPlaylistResolver.resolveClips(location, title, headers)
        if (!playbackRecoveryGate.isCurrent(generation, url, headers) || discPlaybackSession !== session) return null
        session.clips[title.id] = clips
        return DiscPlaybackSource(title.id, clips, session.extraSession.ticket)
    } catch (error: IOException) {
        if (!playbackRecoveryGate.isCurrent(generation, url, headers)) return null
        if (returnToDiscFeature(discPlaybackSession?.extraSession?.ticket, failed = true)) return null
        val message = when ((error as? BlurayPlaylistResolver.DiscException)?.reason) {
            BlurayPlaylistResolver.Reason.RANGE -> R.string.player_disc_range_required
            BlurayPlaylistResolver.Reason.CLIP_INFO -> R.string.player_disc_clip_info_error
            else -> R.string.player_disc_open_error
        }
        _uiState.update { it.copy(error = context.getString(message), showLoadingOverlay = false) }
        return null
    } finally {
        if (discPreparationJob === job) discPreparationJob = null
    }
}

internal fun PlayerRuntimeController.selectDiscTitle(id: String) {
    val session = discPlaybackSession ?: return
    val title = session.disc.titles.firstOrNull { it.id == id } ?: return
    _uiState.update { it.copy(showDiscTitlePicker = false, showMoreDialog = false) }
    if (session.feature == null) {
        session.feature = title
        initializePlayer(session.url, session.headers, startPaused = session.startPaused, startPositionMs = session.startPositionMs)
        return
    }
    if (isPlayingDiscExtra) {
        if (id == session.feature?.id) returnToDiscFeature(session.extraSession.ticket)
        return
    }
    if (id == session.feature?.id || _exoPlayer == null || !hasRenderedFirstFrame) return
    if (partyBridge?.inParty == true) {
        Toast.makeText(context, R.string.player_disc_extra_party, Toast.LENGTH_SHORT).show()
        return
    }
    val position = captureSourceSwitchPosition() ?: 0L
    val paused = userPausedManually || _exoPlayer?.playWhenReady != true
    val remembered = rememberedTrackPreference
    val persisted = persistedTrackPreference
    rememberCurrentTrackPreferenceForEngineSwitch()
    val snapshot = DiscFeatureSnapshot(
        position, paused, currentInternalPlayerEngine, pendingEngineSwitchTrackPreference, remembered, persisted,
        _uiState.value.subtitleDelayMs, _uiState.value.playbackSpeed
    )
    saveWatchProgress()
    emitPauseScrobbleForCurrentProgress()
    scrobbleStartRequestGeneration++
    hasRequestedScrobbleStartForCurrentItem = false
    hasSentScrobbleStartForCurrentItem = false
    session.extraSession.begin(snapshot) ?: return
    session.extra = title
    _uiState.update {
        it.copy(discExtraId = id, pendingSeekPosition = null, playbackEnded = false, postPlayMode = null)
    }
    releasePlayer(flushPlaybackState = false)
    clearPendingEngineSwitchTrackPreference()
    rememberedTrackPreference = null
    persistedTrackPreference = null
    pendingResumeProgress = null
    userPausedManually = false
    initializePlayer(
        session.url, session.headers, overrideInternalPlayerEngine = snapshot.engine,
        allowEngineFailover = false, startPositionMs = 0L
    )
}

internal fun PlayerRuntimeController.returnToDiscFeature(ticket: Long?, failed: Boolean = false): Boolean {
    val session = discPlaybackSession ?: return false
    if (ticket == null || session.extraSession.ticket != ticket) return false
    val snapshot = session.extraSession.takeReturn(ticket) ?: return true
    scope.launch {
        // Discard duplicate callbacks and let the player finish dispatching its event before release.
        yield()
        if (discPlaybackSession !== session || session.extraSession.ticket != ticket) return@launch
        releasePlayer(flushPlaybackState = false)
        session.extra = null
        session.extraSession.finish(ticket)
        pendingEngineSwitchTrackPreference = snapshot.selectedTracks
        rememberedTrackPreference = snapshot.rememberedTracks
        persistedTrackPreference = snapshot.persistedTracks
        pendingResumeProgress = null
        userPausedManually = snapshot.paused
        _uiState.update {
            it.copy(
                discExtraId = null, showDiscTitlePicker = false, error = null, pendingSeekPosition = null,
                playbackEnded = false, postPlayMode = null, subtitleDelayMs = snapshot.subtitleDelayMs,
                playbackSpeed = snapshot.speed
            )
        }
        if (failed) Toast.makeText(context, R.string.player_disc_extra_failed, Toast.LENGTH_SHORT).show()
        initializePlayer(
            session.url, session.headers, overrideInternalPlayerEngine = snapshot.engine,
            startPaused = snapshot.paused, startPositionMs = snapshot.positionMs
        )
    }
    return true
}
