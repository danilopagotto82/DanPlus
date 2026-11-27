package com.nuvio.tv.core.ratinglimit

import org.junit.Assert.*
import org.junit.Test

class BrazilianRatingTest {
    @Test fun recognizesTheBrazilianScaleAndRejectsForeignLabels() {
        assertEquals(0, brazilianRatingRank(" Livre "))
        assertEquals(2, brazilianRatingRank("10"))
        assertEquals(3, brazilianRatingRank("12"))
        assertFalse(RatingLimit.TEEN.allows(brazilianRatingRank("14")))
        assertFalse(RatingLimit.PG.allows(brazilianRatingRank("PG")))
        assertFalse(RatingLimit.PG.allows(brazilianRatingRank(null)))
    }

    @Test fun choosesTheMostRestrictiveKnownCertification() {
        assertEquals("18", brazilianCertification(listOf("L", "16", "PG", "18")))
        assertNull(brazilianCertification(listOf("PG", null, "")))
    }
}
