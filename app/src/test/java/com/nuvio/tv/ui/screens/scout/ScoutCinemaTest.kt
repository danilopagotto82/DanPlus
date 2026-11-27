package com.nuvio.tv.ui.screens.scout

import com.nuvio.tv.core.scout.ScoutGroupMember
import com.nuvio.tv.core.scout.scoutOrderCinemaPicks
import com.nuvio.tv.domain.model.ScoutExclusions
import com.nuvio.tv.domain.model.ScoutPick
import com.nuvio.tv.domain.model.ScoutPool
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoutCinemaTest {
    private val today = LocalDate.of(2026, 11, 27)
    private val movie = ScoutPick(42, false, "Synthetic", overview = "Synopsis", released = "2026-11-27")

    @Test fun acceptsReleasedMovieWithSynopsis() {
        assertTrue(cinemaCandidateFits(movie, today, emptySet()))
        assertFalse(cinemaCandidateFits(movie.copy(released = "2026-11-28"), today, emptySet()))
        assertFalse(cinemaCandidateFits(movie.copy(released = "2026"), today, emptySet()))
        assertFalse(cinemaCandidateFits(movie.copy(overview = null), today, emptySet()))
    }

    @Test fun rejectsSeriesAndPreviouslyOfferedMovies() {
        assertFalse(cinemaCandidateFits(movie.copy(tv = true), today, emptySet()))
        assertFalse(cinemaCandidateFits(movie, today, setOf(movie.key)))
    }

    @Test fun runtimeMustSatisfyEveryLimit() {
        assertTrue(cinemaRuntimeFits(90, 60, 120, 90))
        assertFalse(cinemaRuntimeFits(91, 60, 120, 90))
        assertFalse(cinemaRuntimeFits(59, 60, 120, 90))
        assertFalse(cinemaRuntimeFits(121, null, 120, null))
        assertFalse(cinemaRuntimeFits(null, null, null, null))
        assertFalse(cinemaRuntimeFits(0, null, null, null))
    }

    @Test fun sparseMemberPreservesAffinityAndStillAppliesExclusions() {
        val preferred = movie.copy(genres = listOf(12))
        val excluded = movie.copy(tmdbId = 18, rating = 10f)
        val other = movie.copy(tmdbId = 60, rating = 7f, genres = listOf(18))
        val guest = ScoutGroupMember(null, ScoutExclusions(tmdbKeys = setOf(excluded.key)))
        assertEquals(
            listOf(preferred, other),
            scoutOrderCinemaPicks(
                listOf(excluded, other, preferred), ScoutPool(picked = listOf(preferred.copy(tmdbId = 70))),
                ScoutExclusions(), listOf(guest)
            )
        )
    }

    @Test fun balancedMovieOutranksOneThatOnlyFitsOneMember() {
        val one = movie.copy(genres = listOf(12))
        val shared = movie.copy(tmdbId = 50, genres = listOf(12, 18))
        val other = ScoutGroupMember(
            ScoutPool(picked = listOf(movie.copy(tmdbId = 70, genres = listOf(18)))),
            ScoutExclusions()
        )
        assertEquals(
            shared,
            scoutOrderCinemaPicks(
                listOf(one, shared), ScoutPool(picked = listOf(one)),
                ScoutExclusions(), listOf(other)
            ).first()
        )
    }
    @Test fun cachedRankingCanOnlyPromoteAValidatedLocalFinalist() {
        val candidates = (1..4).map { movie.copy(tmdbId = it) }
        assertEquals(candidates, cinemaRefineCached(candidates, listOf("movie:99", "movie:4")))
        assertEquals(listOf(candidates[2], candidates[0], candidates[1], candidates[3]),
            cinemaRefineCached(candidates, listOf("movie:99", "movie:3")))
        assertEquals(candidates, cinemaRefineCached(candidates, emptyList()))
    }
    @Test fun temporaryTasteAffectsEachParticipantAndGuestCannotPromoteGlobally() {
        val guestOnly = movie.copy(tmdbId = 10, genres = listOf(10749))
        val groupFit = movie.copy(tmdbId = 11, genres = listOf(10749, 53, 18))
        val missing = ScoutGroupMember(null, ScoutExclusions())
        assertEquals(groupFit, com.nuvio.tv.core.scout.scoutOrderCinemaPicks(
            listOf(guestOnly, groupFit), null, ScoutExclusions(), listOf(missing),
            temporaryGenres = listOf(setOf(53), setOf(28, 18)), guestGenres = setOf(10749)
        ).first())
        assertEquals(1.0, com.nuvio.tv.core.scout.cinemaGenreAffinity(emptyList(), emptySet()), 0.0)
    }
}
