package com.nuvio.tv.core.player

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.data.trailer.TrailerPlaybackSource
import com.nuvio.tv.data.trailer.TrailerSourceExpiry
import com.nuvio.tv.data.trailer.YoutubeChunkedDataSourceFactory
import com.nuvio.tv.ui.screens.settings.MemoryBudget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application-scoped singleton that holds a single ExoPlayer instance dedicated to
 * trailer/preview playback on the home screen.
 *
 * Creating and tearing down ExoPlayer for every poster focus is extremely expensive
 * (codec init, hardware decoder allocation). This pool keeps one instance alive and
 * reuses it across focus changes. An overlapping handoff can keep prepared media, while
 * a screen that leaves first releases its media and leaves a short-lived position.
 *
 * When the full-screen player needs hardware decoders, call [yield] to free
 * codec resources without destroying the instance. Call [reclaim] when returning to
 * the home screen to lazily rebuild if needed.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class TrailerPlayerPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerSettingsDataStore: PlayerSettingsDataStore,
    private val trailerMediaCache: com.nuvio.tv.data.trailer.TrailerMediaCache,
    private val profiles: ProfileManager
) {
    companion object {
        private const val TAG = "TrailerPlayerPool"
        private const val HANDOFF_MAX_AGE_MS = 30_000L

        // A trailer plays on top of the home UI and its image caches, so it gets a small byte cap
        // rather than Media3's default (~144 MB for video + audio). Low-RAM sticks get the smallest.
        private const val LOW_RAM_TRAILER_BUFFER_MB = 48
        private const val TRAILER_BUFFER_MB = 100
    }

    private var _player: ExoPlayer? = null
    private val _activeOwner = MutableStateFlow<Any?>(null)
    internal val activeOwner: StateFlow<Any?> = _activeOwner.asStateFlow()
    private var ownerRevision: Long? = null
    private var surfaceOwner: Any? = null
    private var boundView: PlayerView? = null
    private var preparedSource: TrailerPlaybackSource? = null
    private var handoff: Handoff? = null
    internal val selectionRevision: StateFlow<Long> get() = profiles.profileSelectionRevision

    private data class Handoff(
        val source: TrailerPlaybackSource,
        val positionMs: Long,
        val revision: Long,
        val elapsedMs: Long
    )

    private val yielded = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val screensaverSuppressed = AtomicBoolean(false)
    private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isTrailerPlaying = MutableStateFlow(false)
    val isTrailerPlaying: StateFlow<Boolean> = _isTrailerPlaying.asStateFlow()

    @Volatile
    private var cachedForceNative: Boolean = false

    @Volatile
    private var cachedPlayerSettings: PlayerSettings? = null

    // Kept current so a player rebuilt after [yield] picks up buffer changes made since launch.
    init {
        settingsScope.launch {
            runCatching {
                playerSettingsDataStore.nuvioPerformanceModeEnabled.collect { cachedForceNative = it }
            }
        }
        settingsScope.launch {
            runCatching {
                playerSettingsDataStore.playerSettings.collect { cachedPlayerSettings = it }
            }
        }
    }

    /**
     * Returns the shared trailer ExoPlayer, creating it lazily if needed.
     * Returns null while codecs belong to the main player, the screensaver is active,
     * the profile has changed or [release] was called.
     */
    fun acquire(owner: Any, revision: Long = selectionRevision.value): ExoPlayer? {
        if (released.get() || yielded.get() || screensaverSuppressed.get() ||
            revision != selectionRevision.value
        ) return null
        stopStaleProfile()
        val player = _player ?: createPlayer().also { _player = it }
        val previousOwner = _activeOwner.value
        if (previousOwner != null && previousOwner !== owner) player.playWhenReady = false
        ownerRevision = revision
        _activeOwner.value = owner
        return player
    }

    fun isOwner(owner: Any): Boolean = _activeOwner.value === owner && ownerRevision == selectionRevision.value

    internal fun stopStaleProfile() {
        val revision = selectionRevision.value
        if (ownerRevision?.let { it != revision } == true || handoff?.let { it.revision != revision } == true) {
            stop()
        }
    }

    internal fun attach(owner: Any, view: PlayerView) {
        if (!isOwner(owner)) return
        val player = _player ?: return
        PlayerView.switchTargetView(player, boundView, view)
        boundView = view
        surfaceOwner = owner
    }

    internal fun detach(owner: Any, view: PlayerView) {
        if (surfaceOwner === owner && boundView === view) detachSurface()
    }

    private fun detachSurface() {
        boundView?.player = null
        boundView = null
        surfaceOwner = null
    }

    internal fun pause(owner: Any) {
        if (isOwner(owner)) _player?.playWhenReady = false
    }

    internal fun isPrepared(owner: Any, source: TrailerPlaybackSource): Boolean =
        isOwner(owner) && preparedSource == source

    /** Returns whether Media3 had to prepare media for this owner. */
    internal fun prepare(owner: Any, source: TrailerPlaybackSource): Boolean {
        if (!isOwner(owner)) return false
        val player = _player ?: return false
        if (preparedSource == source && player.playerError == null &&
            player.playbackState in Player.STATE_BUFFERING..Player.STATE_READY
        ) return false

        val position = handoff?.takeIf {
            it.source == source && it.revision == ownerRevision &&
                SystemClock.elapsedRealtime() - it.elapsedMs in 0L until HANDOFF_MAX_AGE_MS &&
                TrailerSourceExpiry.isUsable(it.source)
        }?.positionMs ?: 0L
        handoff = null
        player.playWhenReady = false
        preparedSource = source
        if (!source.audioUrl.isNullOrBlank()) {
            val factory = DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
            val video = factory.createMediaSource(MediaItem.fromUri(source.videoUrl))
            val audio = factory.createMediaSource(MediaItem.fromUri(source.audioUrl))
            player.setMediaSource(MergingMediaSource(video, audio), position)
        } else {
            player.setMediaItem(MediaItem.fromUri(source.videoUrl), position)
        }
        player.prepare()
        return true
    }

    suspend fun preload(source: TrailerPlaybackSource) {
        if (!released.get() && !yielded.get() && !screensaverSuppressed.get()) trailerMediaCache.preload(source)
    }

    /** Only the current owner can leave a position for the next surface. */
    fun stop(owner: Any) {
        if (_activeOwner.value !== owner) return
        val revision = ownerRevision
        val source = preparedSource
        val player = _player
        handoff = if (isOwner(owner) && revision != null && source != null && player != null &&
            player.playerError == null && player.playbackState in Player.STATE_BUFFERING..Player.STATE_READY
        ) Handoff(source, player.currentPosition, revision, SystemClock.elapsedRealtime()) else null
        clearPlayer()
    }

    /** Stop without passing a position to another screen. */
    fun stop() {
        handoff = null
        clearPlayer()
    }

    private fun clearPlayer() {
        _activeOwner.value = null
        ownerRevision = null
        preparedSource = null
        detachSurface()
        _player?.let { player ->
            runCatching {
                player.playWhenReady = false
                player.stop()
                player.clearMediaItems()
            }
        }
        _isTrailerPlaying.value = false
    }

    /**
     * While suppressed, [acquire] returns null so every trailer start silently no-ops.
     * Set by the OLED screensaver on engage (together with [stop]) and cleared on wake.
     * Trailers resume on their next natural trigger (focus change or hero rotation).
     */
    fun setScreensaverSuppressed(suppressed: Boolean) {
        screensaverSuppressed.set(suppressed)
    }

    /**
     * Releases codec resources so the detail-screen player can claim hardware decoders.
     * The ExoPlayer instance is released here; [reclaim] will create a fresh one.
     */
    fun yield() {
        if (yielded.compareAndSet(false, true)) {
            _activeOwner.value = null
            ownerRevision = null
            handoff = null
            preparedSource = null
            detachSurface()
            Log.d(TAG, "Yielding trailer player for detail playback")
            _isTrailerPlaying.value = false
            _player?.let { player ->
                runCatching { player.stop() }
                runCatching { player.clearMediaItems() }
                runCatching { player.release() }
            }
            _player = null
        }
    }

    /**
     * Re-creates the player after a [yield]. Safe to call multiple times.
     */
    fun reclaim() {
        if (released.get()) return
        if (yielded.compareAndSet(true, false)) {
            Log.d(TAG, "Reclaiming trailer player")
            // Player will be lazily created on next acquire()
        }
    }

    /**
     * Permanently releases the player. Called on process death / Application.onTerminate.
     */
    fun release() {
        if (released.compareAndSet(false, true)) {
            _activeOwner.value = null
            ownerRevision = null
            handoff = null
            preparedSource = null
            detachSurface()
            _isTrailerPlaying.value = false
            _player?.let { player ->
                runCatching { player.stop() }
                runCatching { player.clearMediaItems() }
                runCatching { player.release() }
            }
            _player = null
        }
    }

    private fun createPlayer(): ExoPlayer {
        val forceNative = cachedForceNative
        val targetBufferMb = trailerTargetBufferMb(cachedPlayerSettings)
        Log.d(
            TAG,
            "Creating shared trailer ExoPlayer instance with forceNativeAllocation = $forceNative, " +
                "targetBufferMb = $targetBufferMb"
        )
        val loadControlBuilder = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setTargetBufferBytes(targetBufferMb * 1024 * 1024)
            // The byte cap must hold even before minBufferMs is reached, or a high-bitrate
            // trailer keeps allocating until the process is killed for low memory.
            .setPrioritizeTimeOverSizeThresholds(false)
        if (forceNative) {
            val allocator = DefaultAllocator(
                /* trimOnReset = */ true,
                /* individualAllocationSize = */ 65536,
                /* initialAllocationCount = */ 0,
                /* forceNativeAllocation = */ true
            )
            loadControlBuilder.setAllocator(allocator)
        }
        val loadControl = loadControlBuilder.build()
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE)
            )
        }
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(trailerMediaCache.playbackFactory()))
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(
                DefaultBandwidthMeter.Builder(context)
                    .setInitialBitrateEstimate(50_000_000L) // 50 Mbps – force highest HLS variant from start
                    .build()
            )
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isTrailerPlaying.value = isPlaying
                    }
                })
            }
    }

    /** Trailer cap for this device tier, lowered further when the user set a smaller Target Buffer Size. */
    private fun trailerTargetBufferMb(settings: PlayerSettings?): Int {
        val tierCapMb = if (MemoryBudget.isLowRamTier) LOW_RAM_TRAILER_BUFFER_MB else TRAILER_BUFFER_MB
        val userTargetMb = settings
            ?.takeIf { it.bufferEngineEnabled && !it.bufferBudgetManaged }
            ?.let { MemoryBudget.effectiveBufferMb(it.bufferSettings.targetBufferSizeMb) }
            ?: return tierCapMb
        return userTargetMb.coerceAtMost(tierCapMb)
    }
}
