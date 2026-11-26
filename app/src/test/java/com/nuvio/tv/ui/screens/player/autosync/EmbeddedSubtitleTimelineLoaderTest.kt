package com.nuvio.tv.ui.screens.player.autosync

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedSubtitleTimelineLoaderTest {
    @Test
    fun concurrentIndexLoadsShareOneDownload(): Unit = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setHeadersDelay(300L, TimeUnit.MILLISECONDS),
            )
            val url = server.url("/shared.mkv").toString()

            val prefetch = async { EmbeddedSubtitleTimelineLoader.load(url, emptyMap()) }
            val autoSync = async { EmbeddedSubtitleTimelineLoader.load(url, emptyMap()) }

            withTimeout(3_000L) {
                assertNull(prefetch.await())
                assertNull(autoSync.await())
            }
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun joinedIndexLoadRestartsWhenOwningLoadIsCancelled(): Unit = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val url = server.url("/handoff.mkv").toString()

            val prefetch = async { EmbeddedSubtitleTimelineLoader.load(url, emptyMap()) }
            assertNotNull(
                withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) },
            )
            val autoSync = async { EmbeddedSubtitleTimelineLoader.load(url, emptyMap()) }
            yield()

            server.enqueue(MockResponse().setResponseCode(404))
            withTimeout(1_000L) { prefetch.cancelAndJoin() }

            assertNull(withTimeout(3_000L) { autoSync.await() })
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun readFailureDoesNotBecomeACachedMissingTrack() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setBody("not a supported container"))
            val url = server.url("/video.mkv").toString()
            val headers = mapOf("Authorization" to "Bearer fixture")
            assertNull(EmbeddedSubtitleTimelineLoader.load(url, headers))
            assertNull(EmbeddedSubtitleTimelineLoader.load(url, headers))
            assertEquals(2, server.requestCount)
            repeat(2) {
                val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                assertTrue(request.getHeader("Range").orEmpty().startsWith("bytes=0-"))
                assertEquals("Bearer fixture", request.getHeader("Authorization"))
            }
        }
    }

    @Test
    fun cancellationDoesNotPublishANegativeResultForTheNextLoad() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            // An unambiguous HTTP failure avoids triggering normal container probing.
            server.enqueue(MockResponse().setResponseCode(404))
            val url = server.url("/cancel.mkv").toString()
            val running = launch(start = CoroutineStart.UNDISPATCHED) { EmbeddedSubtitleTimelineLoader.load(url) }
            assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
            running.cancelAndJoin()
            assertNull(EmbeddedSubtitleTimelineLoader.load(url))
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun legacyAssIsRecognizedWithoutClassifyingSrtOrPgsAsAss() {
        listOf("S_TEXT/ASS", "S_TEXT/SSA", "s_ass", "s_ssa").forEach {
            assertTrue(EmbeddedSubtitleTimelineLoader.isMatroskaAssCodec(it))
        }
        assertEquals(false, EmbeddedSubtitleTimelineLoader.isMatroskaAssCodec("S_HDMV/PGS"))
        assertEquals(false, EmbeddedSubtitleTimelineLoader.isMatroskaAssCodec("S_TEXT/UTF8"))
    }
}
