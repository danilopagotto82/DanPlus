package com.nuvio.tv.ui.v2.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureGlassTest {
    @Test fun savedTransparencyMapsToSmokeOpacityIncludingBothEnds() {
        assertEquals(1f, signatureGlassAlpha(0), .0001f)
        assertEquals(.40f, signatureGlassAlpha(60), .0001f)
        assertEquals(0f, signatureGlassAlpha(100), .0001f)
        assertEquals(1f, signatureGlassAlpha(-10), .0001f)
        assertEquals(0f, signatureGlassAlpha(120), .0001f)
        assertTrue(signatureGlassAlpha(20) > signatureGlassAlpha(80))
    }
}
