package com.nuvio.tv.ui.screens.scout

import com.nuvio.tv.core.ratinglimit.RatingLimit
import com.nuvio.tv.core.scout.ScoutSelection
import com.nuvio.tv.core.scout.scoutReadPolicy
import com.nuvio.tv.domain.model.ScoutPick
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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

    @Test
    fun adultsRowAndHeroAreRemovedBeforeTheRestrictedResultArrives() {
        val pick = ScoutPick(5, false, "Old film")
        val adult = ScoutAudience(ScoutSelection(1, 7L), null, emptySet(), false)
        val state = ScoutScreenState(
            row = listOf(pick), hero = ScoutShownPick(pick), heroLogo = "old logo", audience = adult
        )
        val restricted = listOf(
            ScoutAudience(ScoutSelection(2, 8L), RatingLimit.PRESCHOOL, emptySet(), false),
            adult.copy(limit = RatingLimit.PG, others = setOf(2), family = true),
            adult.copy(limit = RatingLimit.PRESCHOOL),
            adult.copy(owner = ScoutSelection(1, 9L)),
            scoutReadPolicy<ScoutAudience> { error("Rating gate unavailable") }
        )
        restricted.forEach {
            val visible = state.visibleTo(it)
            assertTrue(visible.row.isEmpty())
            assertNull(visible.hero)
            assertNull(visible.heroLogo)
        }
        assertEquals(state, state.visibleTo(adult))
    }

    @Test
    fun reselectingTheSameRestrictedProfileInvalidatesItsPreviousRecommendationResult() {
        val audience = ScoutAudience(ScoutSelection(2, 8L), RatingLimit.PG, setOf(3), true)
        val pick = ScoutPick(5, false, "Previous result")
        val state = ScoutScreenState(row = listOf(pick), hero = ScoutShownPick(pick), audience = audience)
        val reselected = audience.copy(owner = ScoutSelection(2, 10L))
        assertTrue(state.visibleTo(reselected).row.isEmpty())
        assertNull(state.visibleTo(reselected).hero)
        assertFalse(RatingLimit.PG.allows(null))
    }

    @Test
    fun aDelayedPinFromTheClosedDialogCannotUnlockTheReopenedMember() = runBlocking {
        val owner = ScoutSelection(1, 3L)
        val requests = ScoutPinRequests()
        val old = requests.begin(owner)
        val delayedReply = CompletableDeferred<Boolean>()
        var unlocked = false
        val validation = launch {
            if (delayedReply.await() && requests.accepts(old, owner)) unlocked = true
        }
        requests.invalidate()
        val reopened = requests.begin(owner)
        delayedReply.complete(true)
        validation.join()
        assertFalse(unlocked)
        assertFalse(requests.accepts(old, owner))
        assertTrue(requests.accepts(reopened, owner))
    }

    @Test
    fun otherMemberAndProfileSelectionRejectPendingPinRequirementAndValidation() {
        val owner = ScoutSelection(1, 3L)
        val requests = ScoutPinRequests()
        val firstLookup = requests.begin(owner)
        val secondLookup = requests.begin(owner)
        assertFalse(requests.accepts(firstLookup, owner))
        assertTrue(requests.accepts(secondLookup, owner))
        val validation = requests.begin(owner)
        assertFalse(requests.accepts(validation, ScoutSelection(2, 4L)))
        assertFalse(requests.accepts(validation, ScoutSelection(1, 5L)))
        requests.invalidate()
        assertFalse(requests.accepts(validation, owner))
    }

}
