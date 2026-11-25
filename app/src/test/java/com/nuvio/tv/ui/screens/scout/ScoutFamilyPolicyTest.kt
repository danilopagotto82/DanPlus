package com.nuvio.tv.ui.screens.scout

import com.nuvio.tv.core.ratinglimit.RatingLimit
import com.nuvio.tv.domain.model.ScoutPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ScoutFamilyPolicyTest {
    @Test
    fun familyAndGroupUseTheStrictestConfiguredCeiling() {
        assertEquals(RatingLimit.PRESCHOOL, scoutRecommendationLimit(RatingLimit.TEEN, listOf(RatingLimit.PRESCHOOL), true))
        assertEquals(RatingLimit.PG, scoutRecommendationLimit(null, emptyList(), true))
        assertNull(scoutRecommendationLimit(null, emptyList(), false))
        assertFalse(RatingLimit.PG.allows(null))
    }

    @Test
    fun aWatchedSeriesIsHiddenWithoutHidingAMovieWithTheSameId() {
        val series = ScoutPick(5, true, "Series")
        val movie = ScoutPick(5, false, "Film")
        val display = ScoutWatchDisplay(1, showWatched = false, loaded = true, seriesIds = setOf(5))
        assertEquals(listOf(movie), display.visible(listOf(series, movie)))
        assertEquals(listOf(series, movie), display.copy(showWatched = true).visible(listOf(series, movie)))
    }
}
