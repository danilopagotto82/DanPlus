package com.nuvio.tv.core.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingScrobbleCoordinatorTest {
    @Test
    fun `fanout isolates a provider failure`() = runTest {
        val successful = FakeScrobbler(TrackingProviderId.TRAKT)
        val failing = FakeScrobbler(TrackingProviderId.SIMKL, IllegalStateException("offline"))
        val mdblist = FakeScrobbler(TrackingProviderId.MDBLIST)
        val event = TrackingScrobbleEvent(
            media = TrackingMediaReference(
                kind = TrackingMediaKind.MOVIE,
                ids = TrackingExternalIds(imdb = "tt0111161")
            ),
            progressPercent = 42.5
        )

        val failures = dispatchTrackingScrobble(
            scrobblers = listOf(successful, failing, mdblist),
            action = TrackingScrobbleAction.PAUSE,
            event = event
        )

        assertEquals(1, successful.callCount)
        assertEquals(1, failing.callCount)
        assertEquals(listOf(TrackingScrobbleAction.PAUSE), mdblist.actions)
        assertEquals(listOf(TrackingScrobbleAction.PAUSE), successful.actions)
        assertEquals(listOf(TrackingScrobbleAction.PAUSE), failing.actions)
        assertEquals(listOf(TrackingProviderId.SIMKL), failures.map { it.providerId })
    }

    @Test
    fun `leaving paused only stops the trackers that save nothing on a pause`() = runTest {
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT)
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY, stopsLeftPaused = true)

        dispatchTrackingStopWhenLeftPaused(listOf(trakt, floppy), event(40.0))

        assertEquals(emptyList<TrackingScrobbleAction>(), trakt.actions)
        assertEquals(listOf(TrackingScrobbleAction.STOP), floppy.actions)
    }

    @Test
    fun `leaving paused respects a tracker the media server already scrobbles to`() = runTest {
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY, stopsLeftPaused = true)

        dispatchTrackingStopWhenLeftPaused(listOf(floppy), event(40.0).copy(skippedProviders = setOf(TrackingProviderId.FLOPPY)))

        assertEquals(0, floppy.callCount)
    }

    @Test
    fun `services a media server already scrobbles to are left out`() = runTest {
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT)
        val simkl = FakeScrobbler(TrackingProviderId.SIMKL)
        val event = TrackingScrobbleEvent(
            media = TrackingMediaReference(
                kind = TrackingMediaKind.MOVIE,
                ids = TrackingExternalIds(imdb = "tt0111161")
            ),
            progressPercent = 10.0,
            skippedProviders = setOf(TrackingProviderId.TRAKT)
        )

        dispatchTrackingScrobble(listOf(trakt, simkl), TrackingScrobbleAction.START, event)

        assertEquals(0, trakt.callCount)
        assertEquals(1, simkl.callCount)
    }

    @Test
    fun `a tracker cancelled from inside does not cancel the others`() = runTest {
        val log = mutableListOf<String>()
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT, log = log, delayMs = 5_000L)
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY, CancellationException("account changed"), log = log)

        val failures = dispatchTrackingScrobble(listOf(trakt, floppy), TrackingScrobbleAction.STOP, event(80.0))

        assertEquals(listOf("FLOPPY stop", "TRAKT stop"), log)
        assertEquals(listOf(TrackingProviderId.FLOPPY), failures.map { it.providerId })
    }

    @Test
    fun `cancelling the caller still cancels the trackers`() = runTest {
        val log = mutableListOf<String>()
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT, log = log, delayMs = 5_000L)
        val job = launch { dispatchTrackingScrobble(listOf(trakt), TrackingScrobbleAction.STOP, event(80.0)) }
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertTrue(job.isCancelled)
        assertTrue(log.isEmpty())
    }

    @Test
    fun `seek restart goes per tracker and only to trackers that restart`() = runTest {
        val log = mutableListOf<String>()
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT, log = log, delayMs = 10_000L, seekPolicy = TrackingSeekScrobblePolicy.STOP_AND_RESTART)
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY, log = log, seekPolicy = TrackingSeekScrobblePolicy.STOP_AND_RESTART)
        val simkl = FakeScrobbler(TrackingProviderId.SIMKL, log = log)

        dispatchTrackingSeekRestart(listOf(trakt, floppy, simkl), event(40.0)) { event(41.0) }

        assertEquals(listOf("FLOPPY stop", "FLOPPY start", "TRAKT stop", "TRAKT start"), log)
    }

    @Test
    fun `seek restart skips the start when playback moved on`() = runTest {
        val log = mutableListOf<String>()
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT, log = log, seekPolicy = TrackingSeekScrobblePolicy.STOP_AND_RESTART)
        val skipped = event(40.0).copy(skippedProviders = setOf(TrackingProviderId.FLOPPY))
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY, log = log, seekPolicy = TrackingSeekScrobblePolicy.STOP_AND_RESTART)

        dispatchTrackingSeekRestart(listOf(trakt, floppy), skipped) { null }

        assertEquals(listOf("TRAKT stop"), log)
    }

    @Test
    fun `an unavailable or no-send provider stays pending while a proven duplicate is accepted`() = runTest {
        val duplicate = FakeScrobbler(TrackingProviderId.TRAKT, outcome = TrackingScrobbleOutcome.ALREADY_CONFIRMED)
        val ignored = FakeScrobbler(TrackingProviderId.SIMKL, outcome = TrackingScrobbleOutcome.IGNORED)
        val offline = FakeScrobbler(TrackingProviderId.MDBLIST, outcome = TrackingScrobbleOutcome.UNAVAILABLE)
        val trackers = listOf(duplicate, ignored, offline)
        val result = dispatchTrackingCompletion(trackers, TrackingScrobbleAction.STOP, event(98.0), emptyMap()) { true }

        assertEquals(setOf(TrackingProviderId.TRAKT), result.accepted)
        assertEquals(setOf(TrackingProviderId.SIMKL, TrackingProviderId.MDBLIST), result.pending)
        assertFalse(result.complete)
        ignored.outcome = TrackingScrobbleOutcome.CONFIRMED
        // The missing provider remains pending even when hidden by the connected-provider filter.
        val retryEvent = event(98.0).copy(skippedProviders = result.accepted)
        val disconnected = dispatchTrackingCompletion(
            listOf(duplicate, ignored), TrackingScrobbleAction.STOP, retryEvent, result.accounts
        ) { true }
        assertEquals(setOf(TrackingProviderId.MDBLIST), disconnected.pending)

        offline.outcome = TrackingScrobbleOutcome.CONFIRMED
        val reconnected = dispatchTrackingCompletion(
            trackers, TrackingScrobbleAction.STOP,
            retryEvent.copy(skippedProviders = result.accepted + disconnected.accepted), result.accounts
        ) { true }
        assertTrue(reconnected.complete)
        assertEquals(listOf(1, 2, 2), trackers.map { it.callCount })
    }

    @Test
    fun `a progress-only pause and a nonterminal stop cannot confirm completion`() = runTest {
        val trakt = FakeScrobbler(TrackingProviderId.TRAKT, completesOnPause = true)
        val floppy = FakeScrobbler(TrackingProviderId.FLOPPY)
        val wetrakr = FakeScrobbler(TrackingProviderId.WETRAKR, completionPercent = 90.0)
        val trackers = listOf(trakt, floppy, wetrakr)
        val paused = dispatchTrackingCompletion(trackers, TrackingScrobbleAction.PAUSE, event(85.0), emptyMap()) { true }
        assertEquals(setOf(TrackingProviderId.TRAKT), paused.accepted)
        val stopped = dispatchTrackingCompletion(
            trackers, TrackingScrobbleAction.STOP, event(85.0).copy(skippedProviders = paused.accepted), paused.accounts
        ) { true }
        assertEquals(setOf(TrackingProviderId.FLOPPY), stopped.accepted)
        assertEquals(setOf(TrackingProviderId.WETRAKR), stopped.pending)
    }

    @Test
    fun `a retry never sends a failed completion to a replacement account or profile`() = runTest {
        val prior = TrackingScrobbleAccount(1, 8)
        val tracker = FakeScrobbler(TrackingProviderId.TRAKT, account = prior, outcome = TrackingScrobbleOutcome.RETRYABLE_FAILURE)
        val event = event(99.0).copy(profileId = 1)
        val first = dispatchTrackingCompletion(listOf(tracker), TrackingScrobbleAction.STOP, event, emptyMap()) { true }
        assertFalse(first.complete)
        tracker.outcome = TrackingScrobbleOutcome.CONFIRMED

        for (replacement in listOf(TrackingScrobbleAccount(1, 9), TrackingScrobbleAccount(2, 8))) {
            tracker.account = replacement
            val changed = dispatchTrackingCompletion(listOf(tracker), TrackingScrobbleAction.STOP, event, first.accounts) { true }
            assertTrue(changed.accepted.isEmpty())
            assertEquals(setOf(TrackingProviderId.TRAKT), changed.pending)
            assertEquals(1, tracker.callCount)
        }
    }

    @Test
    fun `an acknowledgement arriving after an account or episode change cannot complete the old attempt`() = runTest {
        for (sameEpisode in listOf(true, false)) {
            val release = CompletableDeferred<Unit>()
            val tracker = FakeScrobbler(TrackingProviderId.TRAKT, awaitReceipt = { release.await() })
            var current = true
            val pending = async {
                dispatchTrackingCompletion(listOf(tracker), TrackingScrobbleAction.STOP, event(98.0), emptyMap()) { current }
            }
            runCurrent()
            assertEquals(1, tracker.callCount)
            if (sameEpisode) tracker.account = TrackingScrobbleAccount(1, 2) else current = false
            release.complete(Unit)
            val result = pending.await()
            assertFalse(result.complete)
            assertTrue(result.accepted.isEmpty())
        }
    }

    @Test
    fun `completion while a disc extra is active is skipped before sending`() = runTest {
        val tracker = FakeScrobbler(TrackingProviderId.TRAKT)
        val extraActive = true
        val result = dispatchTrackingCompletion(listOf(tracker), TrackingScrobbleAction.STOP, event(99.0), emptyMap()) {
            !extraActive
        }
        assertEquals(0, tracker.callCount)
        assertFalse(result.complete)
    }

    private fun event(percent: Double) = TrackingScrobbleEvent(
        media = TrackingMediaReference(kind = TrackingMediaKind.MOVIE, ids = TrackingExternalIds(imdb = "tt0111161")),
        progressPercent = percent
    )

    private class FakeScrobbler(
        override val providerId: TrackingProviderId,
        private val failure: Throwable? = null,
        private val seekPolicy: TrackingSeekScrobblePolicy = TrackingSeekScrobblePolicy.NONE,
        private val log: MutableList<String>? = null,
        private val delayMs: Long = 0L,
        private val stopsLeftPaused: Boolean = false,
        var outcome: TrackingScrobbleOutcome = TrackingScrobbleOutcome.CONFIRMED,
        var account: TrackingScrobbleAccount? = TrackingScrobbleAccount(1, 1),
        override val completesOnPause: Boolean = false,
        override val completionPercent: Double = 80.0,
        private val awaitReceipt: suspend () -> Unit = {}
    ) : TrackingScrobbler {
        var callCount = 0
        val actions = mutableListOf<TrackingScrobbleAction>()
        override val seekScrobblePolicy = seekPolicy
        override val stopsWhenLeftPaused = stopsLeftPaused

        override suspend fun currentScrobbleAccount(): TrackingScrobbleAccount? = account

        override suspend fun scrobble(
            action: TrackingScrobbleAction,
            event: TrackingScrobbleEvent
        ): TrackingScrobbleOutcome {
            callCount += 1
            actions += action
            if (delayMs > 0L) delay(delayMs)
            log?.add("${providerId.name} ${action.wireValue}")
            failure?.let { throw it }
            awaitReceipt()
            return outcome
        }
    }
}
