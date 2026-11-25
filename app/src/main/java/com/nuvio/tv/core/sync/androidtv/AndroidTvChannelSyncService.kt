package com.nuvio.tv.core.sync.androidtv

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.poster.CustomPosterScreen
import com.nuvio.tv.core.poster.patternForScreen
import com.nuvio.tv.core.poster.withCustomPosterArt
import com.nuvio.tv.core.recommendations.TvRecommendationManager
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ContinueWatchingEnrichmentCache
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.data.local.TraktSettingsDataStore
import com.nuvio.tv.domain.repository.MetaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nuvio.tv.core.player.PlayerSessionTracker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TvChannelSync"
private const val DEBOUNCE_MS = 2_000L

/**
 * Keeps the Android TV "Continue Watching" preview channel in sync with the app's
 * CW enrichment cache. Uses the same enriched data that the in-app Continue Watching
 * section displays, respecting user settings (days cap, show unaired, etc.).
 *
 * Items are sourced from [ContinueWatchingEnrichmentCache] which is populated by the
 * HomeViewModel CW pipeline. This ensures the launcher channel shows the same items
 * the user sees in the app UI.
 *
 * Before publishing new programs, all stale programs are removed first to avoid
 * the launcher showing outdated tiles while new ones are being inserted.
 */
@Singleton
class AndroidTvChannelSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: AndroidTvChannelManager,
    private val cwEnrichmentCache: ContinueWatchingEnrichmentCache,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore,
    private val traktSettingsDataStore: TraktSettingsDataStore,
    private val tvRecommendationManager: TvRecommendationManager,
    private val metaRepository: MetaRepository,
    private val profileManager: ProfileManager
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e -> Log.w(TAG, "channel sync failed", e) }
    )
    private val reconcileMutex = Mutex()

    // The launcher channel is only visible while the app is in the background. Reconciling on
    // every in-playback cache bump (~10s) thrashes the TvProvider and stops launchers like
    // Projectivy from repainting. We instead reconcile once when the app goes to background
    // (user returns to the launcher), see [onForegroundChanged].
    @Volatile private var appInForeground = false

    // A sync asked for while frames are on screen waits for the next pause or stop, the same
    // rule the single Watch Next write follows, so the provider call never hitches the player.
    @Volatile private var syncWaitsForPlayback = false

    /** Called from the host Activity's onStart/onStop. On background we reconcile once so the
     *  channel reflects the latest watch progress exactly as the launcher regains foreground. */
    fun onForegroundChanged(foreground: Boolean) {
        val wasForeground = appInForeground
        appInForeground = foreground
        if (wasForeground && !foreground) {
            scope.launch { reconcileFromCache() }
        }
    }

    @OptIn(FlowPreview::class)
    fun start() {
        if (!manager.isSupported()) {
            Log.d(TAG, "Non-leanback device; channel sync skipped")
            return
        }
        scope.launch { TvChannelRefreshJobService.schedulePeriodic(context) }

        scope.launch {
            PlayerSessionTracker.isSessionOpen.collect { active ->
                if (!active && syncWaitsForPlayback) {
                    syncWaitsForPlayback = false
                    // The player has just written its own fresher Watch Next row; the channel
                    // catches up now and Watch Next follows at the next background sync.
                    reconcileFromCache(withWatchNext = false)
                }
            }
        }

        scope.launch {
            // Observe cache snapshot updates and settings changes to trigger reconciliation.
            // snapshotVersion bumps every time the CW pipeline writes new data to disk cache.
            // We drop the initial value (0) to avoid reconciling with stale disk cache before
            // the pipeline has had a chance to produce fresh data.
            combine(
                cwEnrichmentCache.snapshotVersion
                    .dropWhile { it == 0 },
                traktSettingsDataStore.continueWatchingDaysCap,
                traktSettingsDataStore.dismissedNextUpKeys,
                layoutPreferenceDataStore.useEpisodeThumbnailsInCw,
                profileManager.activeProfileId
            ) { _, daysCap, dismissed, useEpisodeThumbnails, profileId ->
                profileId to ChannelSettingsSnapshot(daysCap, dismissed, useEpisodeThumbnails)
            }
                .debounce(DEBOUNCE_MS)
                .collect { (profileId, settings) ->
                    // Skip while the app is foregrounded: the launcher channel isn't visible
                    // then, and reconciling on every ~10s in-playback cache bump thrashes the
                    // provider (and stops launchers like Projectivy from repainting). The
                    // background transition (onForegroundChanged) + periodic job cover it.
                    if (appInForeground) return@collect
                    reconcileFromCache(settings, expectedProfileId = profileId)
                }
        }
    }

    /**
     * Reads the CW enrichment cache and reconciles the launcher channel.
     * Called both from the live observer and from [TvChannelRefreshJobService].
     */
    suspend fun reconcileFromCache(
        settings: ChannelSettingsSnapshot? = null,
        withWatchNext: Boolean = true,
        expectedProfileId: Int = profileManager.activeProfileId.value
    ) {
        if (!manager.isSupported()) return
        reconcileMutex.withLock {
            syncWaitsForPlayback = true
            if (PlayerSessionTracker.isOpenNow) {
                Log.d(TAG, "Player open; channel sync waits for playback to exit")
                return@withLock
            }
            syncWaitsForPlayback = false
            if (profileManager.activeProfileId.value != expectedProfileId) return@withLock
            reconcileLocked(settings, withWatchNext, expectedProfileId)
        }
    }

    private suspend fun reconcileLocked(settings: ChannelSettingsSnapshot?, withWatchNext: Boolean, profileId: Int) {
        val resolvedSettings = settings ?: runCatching {
            val daysCap = traktSettingsDataStore.continueWatchingDaysCap.first()
            val dismissed = traktSettingsDataStore.dismissedNextUpKeys.first()
            val useEpisodeThumbnails = layoutPreferenceDataStore.useEpisodeThumbnailsInCw.first()
            ChannelSettingsSnapshot(daysCap, dismissed, useEpisodeThumbnails)
        }.getOrElse {
            Log.w(TAG, "Settings read failed; channel sync skipped", it)
            return
        }

        val inProgressItems = cwEnrichmentCache.getInProgressSnapshot(profileId)
        val nextUpItems = cwEnrichmentCache.getNextUpSnapshot(profileId)

        val posterPattern = patternForScreen(
            layoutPreferenceDataStore.customPosterUrlPattern.first(),
            CustomPosterScreen.TV_RECOMMENDATIONS,
            layoutPreferenceDataStore.customPosterEnabledScreens.first()
        )
        val channelItems = buildChannelItems(inProgressItems, nextUpItems, resolvedSettings)
            .withCustomPosterArt(posterPattern)
        val descriptions = channelDescriptions(channelItems, inProgressItems, nextUpItems, metaRepository::getCachedMeta)
        if (profileManager.activeProfileId.value != profileId) return

        Log.d(
            TAG,
            "Reconciling from cache: ${channelItems.size} items " +
                "(${inProgressItems.size} in-progress, ${nextUpItems.size} next-up raw)"
        )
        manager.reconcile(channelItems, descriptions)

        if (!withWatchNext || profileManager.activeProfileId.value != profileId) return
        runCatching {
            tvRecommendationManager.updateWatchNext(channelItems, descriptions)
        }
    }
}
