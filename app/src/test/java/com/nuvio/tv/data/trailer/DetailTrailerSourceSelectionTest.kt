package com.nuvio.tv.data.trailer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Details uses the existing poster fallback only after its Brazilian Portuguese candidate. */
class DetailTrailerSourceSelectionTest {
    @Test
    fun `a playable Brazilian Portuguese trailer wins without calling preview fallback`() = runTest {
        var previewCalled = false
        val preferred = TrailerPlaybackSource("https://example.test/ptbr.mp4")

        val resolved = preferDetailTrailerSource(
            preferredPtBr = { preferred },
            previewFallback = {
                previewCalled = true
                TrailerPlaybackSource("https://example.test/original.mp4")
            }
        )

        assertEquals(preferred, resolved)
        assertFalse(previewCalled)
    }

    @Test
    fun `when Brazilian Portuguese is missing the focused poster source is reused`() = runTest {
        var previewCalled = false
        val fromPoster = TrailerPlaybackSource("https://example.test/original.mp4")

        val resolved = preferDetailTrailerSource(
            preferredPtBr = { null },
            previewFallback = {
                previewCalled = true
                fromPoster
            }
        )

        assertEquals(fromPoster, resolved)
        assertTrue(previewCalled)
    }

    @Test
    fun `without a usable trailer Details does not create a fake video`() = runTest {
        val resolved = preferDetailTrailerSource(
            preferredPtBr = { null },
            previewFallback = { null }
        )

        assertNull(resolved)
    }
}
