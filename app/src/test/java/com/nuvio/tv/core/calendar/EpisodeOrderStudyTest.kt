package com.nuvio.tv.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Documentary fixture for evaluating mappings before adding a production adapter. */
class EpisodeOrderStudyTest {
    private data class Playback(val source: String, val order: String, val slot: Int)
    private data class Tracking(val provider: String, val series: Int, val episode: Int)
    private data class Evidence(val playback: Playback, val targets: List<Tracking>)

    private val dvd = Playback("fictional-addon", "dvd", 3)
    private val aired = dvd.copy(order = "aired")
    private val trakt = Tracking("trakt", 12, 51)
    private val wetrakr = Tracking("wetrakr", 12, 51)

    @Test
    fun dvdSlotDoesNotImplyAiredOrProviderIdentity() {
        val evidence = listOf(Evidence(dvd, listOf(trakt)))
        assertEquals(trakt, singleEpisode(dvd, "trakt", evidence))
        assertNull(singleEpisode(aired, "trakt", evidence))
        assertNull(singleEpisode(dvd, "wetrakr", evidence))
    }

    @Test
    fun combinedStreamAndSplitEpisodeBothRequireAnExplicitPolicy() {
        val combined = listOf(Evidence(dvd, listOf(trakt, trakt.copy(episode = 52))))
        val split = listOf(Evidence(dvd, listOf(trakt)), Evidence(dvd.copy(slot = 4), listOf(trakt)))
        assertNull(singleEpisode(dvd, "trakt", combined))
        assertNull(singleEpisode(dvd, "trakt", split))
    }

    @Test
    fun independentProviderEvidenceCanResolveTheSamePlayback() {
        val evidence = listOf(Evidence(dvd, listOf(trakt)), Evidence(dvd, listOf(wetrakr)))
        assertEquals(wetrakr, singleEpisode(dvd, "wetrakr", evidence))
    }

    private fun singleEpisode(playback: Playback, provider: String, evidence: List<Evidence>): Tracking? {
        val mappings = evidence.mapNotNull { row ->
            val targets = row.targets.filter { it.provider == provider }
            if (targets.isEmpty()) null else row.playback to targets
        }
        val target = mappings.filter { it.first == playback }.singleOrNull()?.second?.singleOrNull()
            ?: return null
        return target.takeIf { mappings.count { (_, targets) -> target in targets } == 1 }
    }
}
