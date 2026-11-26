package com.nuvio.tv.core.sync.androidtv

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.player.PlayerSessionTracker
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.recommendations.TvRecommendationManager
import com.nuvio.tv.data.local.CachedInProgressItem
import com.nuvio.tv.data.local.CachedNextUpItem
import com.nuvio.tv.data.local.ContinueWatchingEnrichmentCache
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.data.local.TraktSettingsDataStore
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.repository.MetaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AndroidTvChannelSyncServiceTest {

    private val context: Context = mockk(relaxed = true)
    private val manager: AndroidTvChannelManager = mockk(relaxed = true)
    private val cache: ContinueWatchingEnrichmentCache = mockk(relaxed = true)
    private val layout: LayoutPreferenceDataStore = mockk(relaxed = true)
    private val trakt: TraktSettingsDataStore = mockk(relaxed = true)
    private val recommendations: TvRecommendationManager = mockk(relaxed = true)
    private val metaRepository: MetaRepository = mockk(relaxed = true)

    private val activeProfileFlow = MutableStateFlow(7)
    private val profileManager: ProfileManager = mockk {
        every { activeProfileId } returns activeProfileFlow
    }
    private var playerSession: PlayerSessionTracker.Session? = null
    private lateinit var service: AndroidTvChannelSyncService

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { manager.isSupported() } returns true
        every { trakt.continueWatchingDaysCap } returns flowOf(TraktSettingsDataStore.CONTINUE_WATCHING_DAYS_CAP_ALL)
        every { trakt.dismissedNextUpKeys } returns flowOf(emptySet())
        every { layout.useEpisodeThumbnailsInCw } returns flowOf(false)
        every { layout.customPosterUrlPattern } returns flowOf("")
        every { layout.customPosterEnabledScreens } returns flowOf(emptySet())
        coEvery { cache.getInProgressSnapshot(7) } returns listOf(inProgress("tt1", 2_000L), inProgress("tt2", 1_000L))
        coEvery { cache.getNextUpSnapshot(7) } returns listOf(nextUp("tt3", 1_500L))
        service = AndroidTvChannelSyncService(
            context, manager, cache, layout, trakt, recommendations, metaRepository, profileManager
        )
    }

    @After
    fun tearDown() {
        playerSession?.close()
        playerSession = null
        unmockkStatic(Log::class)
    }

    @Test
    fun `channel and Watch Next get the same list as the Home row`() = runTest {
        val channelItems = slot<List<WatchProgress>>()
        val watchNextItems = slot<List<WatchProgress>>()
        coEvery { manager.reconcile(capture(channelItems), any()) } returns Unit
        coEvery { recommendations.updateWatchNext(capture(watchNextItems), any()) } returns Unit

        service.reconcileFromCache()

        assertEquals(listOf("tt1", "tt3", "tt2"), channelItems.captured.map { it.contentId })
        assertEquals(channelItems.captured, watchNextItems.captured)
        verifySnapshotsFor(profileId = 7)
    }

    @Test
    fun profileSwitchDuringCacheReadDoesNotPublishMixedSnapshots() = runTest {
        val readStarted = CompletableDeferred<Unit>()
        val resumeRead = CompletableDeferred<Unit>()
        coEvery { cache.getInProgressSnapshot(7) } coAnswers {
            readStarted.complete(Unit)
            resumeRead.await()
            listOf(inProgress("tt1", 2_000L))
        }
        coEvery { cache.getInProgressSnapshot(8) } returns listOf(inProgress("tt8", 2_000L))
        coEvery { cache.getNextUpSnapshot(8) } returns listOf(nextUp("tt9", 1_500L))

        val reconciliation = async { service.reconcileFromCache() }
        readStarted.await()
        activeProfileFlow.value = 8
        resumeRead.complete(Unit)
        reconciliation.await()

        verifySnapshotsFor(profileId = 7)
        coVerify(exactly = 0) { manager.reconcile(any(), any()) }
        coVerify(exactly = 0) { recommendations.updateWatchNext(any(), any()) }

        val items = slot<List<WatchProgress>>()
        coEvery { manager.reconcile(capture(items), any()) } returns Unit
        service.reconcileFromCache()

        coVerify(exactly = 1) { cache.getInProgressSnapshot(8) }
        coVerify(exactly = 1) { cache.getNextUpSnapshot(8) }
        assertEquals(listOf("tt8", "tt9"), items.captured.map { it.contentId })
        coVerify(exactly = 1) { recommendations.updateWatchNext(items.captured, any()) }
    }

    @Test
    fun profileSwitchDuringChannelWriteSkipsWatchNext() = runTest {
        coEvery { manager.reconcile(any(), any()) } coAnswers { activeProfileFlow.value = 8 }

        service.reconcileFromCache()

        verifySnapshotsFor(profileId = 7)
        coVerify(exactly = 1) { manager.reconcile(any(), any()) }
        coVerify(exactly = 0) { recommendations.updateWatchNext(any(), any()) }
    }

    private fun verifySnapshotsFor(profileId: Int) {
        coVerify(exactly = 1) { cache.getInProgressSnapshot(profileId) }
        coVerify(exactly = 1) { cache.getNextUpSnapshot(profileId) }
        coVerify(exactly = 0) { cache.getInProgressSnapshot(match { it != profileId }) }
        coVerify(exactly = 0) { cache.getNextUpSnapshot(match { it != profileId }) }
    }

    @Test
    fun `nothing is read or published while a player session is open`() = runTest {
        playerSession = PlayerSessionTracker.open()

        service.reconcileFromCache()

        coVerify(exactly = 0) { manager.reconcile(any(), any()) }
        coVerify(exactly = 0) { recommendations.updateWatchNext(any(), any()) }
        coVerify(exactly = 0) { cache.getInProgressSnapshot(any()) }
        coVerify(exactly = 0) { cache.getNextUpSnapshot(any()) }
    }

    @Test
    fun `a box without channels reads and writes nothing`() = runTest {
        every { manager.isSupported() } returns false

        service.reconcileFromCache()

        coVerify(exactly = 0) { manager.reconcile(any(), any()) }
        coVerify(exactly = 0) { recommendations.updateWatchNext(any(), any()) }
        coVerify(exactly = 0) { cache.getInProgressSnapshot(any()) }
        coVerify(exactly = 0) { cache.getNextUpSnapshot(any()) }
    }

    @Test
    fun `the catch-up after a pause updates the channel and leaves Watch Next to the player`() = runTest {
        service.reconcileFromCache(withWatchNext = false)

        coVerify(exactly = 1) { manager.reconcile(any(), any()) }
        coVerify(exactly = 0) { recommendations.updateWatchNext(any(), any()) }
    }

    @Test
    fun `settings passed in are used as they are`() = runTest {
        val channelItems = slot<List<WatchProgress>>()
        coEvery { manager.reconcile(capture(channelItems), any()) } returns Unit

        service.reconcileFromCache(ChannelSettingsSnapshot(daysCap = 0, dismissedNextUp = setOf("tt3_s1e1"), useEpisodeThumbnails = false))

        assertEquals(listOf("tt1", "tt2"), channelItems.captured.map { it.contentId })
    }

    private fun inProgress(contentId: String, lastWatched: Long) = CachedInProgressItem(
        contentId = contentId,
        contentType = "movie",
        name = "Title $contentId",
        poster = null,
        backdrop = null,
        logo = null,
        videoId = contentId,
        season = null,
        episode = null,
        episodeTitle = null,
        position = 1_000L,
        duration = 10_000L,
        lastWatched = lastWatched,
        progressPercent = 10f
    )

    private fun nextUp(contentId: String, sortTimestamp: Long) = CachedNextUpItem(
        contentId = contentId,
        contentType = "series",
        name = "Show $contentId",
        poster = null,
        backdrop = null,
        logo = null,
        videoId = "$contentId:1:2",
        season = 1,
        episode = 2,
        episodeTitle = null,
        thumbnail = null,
        lastWatched = sortTimestamp,
        sortTimestamp = sortTimestamp,
        seedSeason = 1,
        seedEpisode = 1
    )

    @Test
    fun `cached episode synopsis reaches the channel and Watch Next`() = runTest {
        every { metaRepository.getCachedMeta(any(), any()) } returns null
        coEvery { cache.getNextUpSnapshot(7) } returns listOf(nextUp("tt3", 1_500L).copy(episodeDescription = "Episode synopsis"))
        val channelText = slot<Map<String, String>>()
        val watchNextText = slot<Map<String, String>>()
        coEvery { manager.reconcile(any(), capture(channelText)) } returns Unit
        coEvery { recommendations.updateWatchNext(any(), capture(watchNextText)) } returns Unit

        service.reconcileFromCache()

        assertEquals(mapOf("tt3:1:2" to "Episode synopsis"), channelText.captured)
        assertEquals(channelText.captured, watchNextText.captured)
    }
}
