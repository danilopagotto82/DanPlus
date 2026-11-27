package com.nuvio.tv.ui.components

import androidx.compose.ui.geometry.Size
import com.nuvio.tv.ui.v2.appearance.cinematicSurfaceOpacity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicPresentationTest {
    @Test
    fun bannerLeavesFullLandscapeAndPortraitArtworkVisibleAtTheOriginalAspectRatio() {
        val frame = Size(800f, 270f)
        assertEquals(Size(480f, 270f), collectionFeatureSize(Size(1920f, 1080f), frame))
        assertEquals(Size(180f, 270f), collectionFeatureSize(Size(1000f, 1500f), frame))
        assertEquals(Size(800f, 160f), collectionFeatureSize(Size(2000f, 400f), frame))
    }

    @Test
    fun panelTransparencyIsMonotonicAndRetainsContrastAtTheTransparentEnd() {
        assertEquals(1f, cinematicSurfaceOpacity(0), .0001f)
        assertEquals(.72f, cinematicSurfaceOpacity(100), .0001f)
        assertEquals(cinematicSurfaceOpacity(0), cinematicSurfaceOpacity(-3))
        assertEquals(cinematicSurfaceOpacity(100), cinematicSurfaceOpacity(107))
        for (percent in 1..100) {
            assertTrue(cinematicSurfaceOpacity(percent) < cinematicSurfaceOpacity(percent - 1))
        }
    }
}
