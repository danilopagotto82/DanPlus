package com.nuvio.tv.ui.components

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaMetadataChipsTest {
    @Test
    fun onlyConcludedSeriesShowAnEndYear() {
        assertEquals("2014", mediaYears("2014–2022", null, false, null))
        assertEquals("2014–2022", mediaYears("2014-04-03", "2022-05-04", true, mediaSeriesStatus("Ended")))
        assertEquals("2014", mediaYears("2014–2022", null, true, mediaSeriesStatus("Em andamento")))
        assertNull(mediaYears(null, "2022", true, mediaSeriesStatus("Ended")))
    }

    @Test
    fun bannerPreservesAspectRatioAndCapsZoomForTallArtwork() {
        val banner = Size(960f, 270f)
        val tall = CollectionBannerScale.computeScaleFactor(Size(500f, 750f), banner)
        assertEquals(tall.scaleX, tall.scaleY, 0f)
        assertEquals(.4752f, tall.scaleX, .0001f)
        val matching = CollectionBannerScale.computeScaleFactor(Size(480f, 135f), banner)
        assertEquals(2f, matching.scaleX, 0f)
        assertEquals(2f, matching.scaleY, 0f)
    }
}
