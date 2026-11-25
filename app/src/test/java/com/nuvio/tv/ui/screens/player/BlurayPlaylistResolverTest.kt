package com.nuvio.tv.ui.screens.player

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurayPlaylistResolverTest {
    private val resolver = BlurayPlaylistResolver

    @Test
    fun `disc paths keep their origin and ignore links that escape the playlist directory`() {
        val location = requireNotNull(resolver.locate("https://example.test/Movies/BDMV/PLAYLIST/00002.mpls?t=mock"))
        assertEquals("00002.mpls", location.explicitPlaylist)
        assertEquals("https://example.test/Movies/BDMV/STREAM/00003.m2ts?t=mock", location.resource("STREAM", "00003.m2ts").toString())
        assertEquals(
            listOf("00012.MPLS"),
            resolver.playlistNames("""<a href="../00001.mpls"></a><a href="//elsewhere.test/00002.mpls"></a><a href="00012.MPLS"></a>""")
        )
        assertNull(resolver.locate("https://example.test/Movies/feature.mkv"))
        assertNull(resolver.locate("https://example.test/Movies/BDMV/STREAM/00001.m2ts"))
        assertNull(resolver.locate("file:///Movies/BDMV/PLAYLIST/00002.mpls"))
    }

    @Test
    fun `valid playlists keep item order and convert in and out times against the clip clock`() {
        val title = requireNotNull(resolver.parsePlaylist("00003.mpls", playlist("00002", "00001", "00002")))
        assertEquals(listOf("00002", "00001", "00002"), title.items.map { it.clip })
        assertEquals(3_000L, title.durationMs)
        val clock = requireNotNull(resolver.parseClipClock(clipInfo(), 2))
        assertEquals(500_000L to 1_500_000L, resolver.clipInterval(title.items[0], clock))
        assertEquals(1_500_000L to 2_500_000L, resolver.clipInterval(title.items[1], clock))
        assertNull(resolver.parseClipClock(clipInfo(), 3))
    }

    @Test
    fun `truncation malformed sections and out of clock ranges fail closed`() {
        val mpls = playlist("00002")
        assertNull(resolver.parsePlaylist("00003.mpls", mpls.copyOf(mpls.size - 1)))
        assertNull(resolver.parsePlaylist("00003.mpls", mpls.copyOf().also { ByteBuffer.wrap(it).putInt(8, -1) }))
        assertNull(resolver.parsePlaylist("00003.mpls", mpls.copyOf().also { it[62] = 0x10 }))
        val badItem = BlurayPlaylistResolver.PlayItem("00002", 2, 44_999L, 60_000L)
        assertNull(resolver.clipInterval(badItem, BlurayPlaylistResolver.Clock(45_000L, 140_000L)))
        assertNull(resolver.parseClipClock(clipInfo().copyOf(65), 2))
    }

    @Test
    fun `an ambiguous directory requires a choice and an absent playlist fails`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""<a href="00001.mpls"></a><a href="00002.mpls"></a>"""))
            server.enqueue(binaryResponse(playlist("00001")))
            server.enqueue(binaryResponse(playlist("00002")))
            val disc = resolver.resolve(requireNotNull(resolver.locate(server.url("/test/BDMV/").toString())), emptyMap())
            assertEquals(listOf("00001", "00002"), disc.titles.map { it.id })
            assertNull(disc.initialTitle)
            server.enqueue(binaryResponse(playlist("00002")))
            server.enqueue(MockResponse().setResponseCode(403))
            val selected = requireNotNull(resolver.locate(server.url("/test/BDMV/PLAYLIST/00002.mpls").toString()))
            assertEquals("00002", resolver.resolve(selected, emptyMap()).initialTitle?.id)
            server.enqueue(MockResponse().setResponseCode(404))
            val explicit = requireNotNull(resolver.locate(server.url("/test/BDMV/PLAYLIST/00003.mpls").toString()))
            val error = runCatching { resolver.resolve(explicit, emptyMap()) }.exceptionOrNull()
            assertEquals(BlurayPlaylistResolver.Reason.PLAYLIST, (error as? BlurayPlaylistResolver.DiscException)?.reason)
        }
    }

    @Test
    fun `multi clip URLs and windows keep the playlist order rather than clip filename order`() = runBlocking {
        MockWebServer().use { server ->
            val url = server.url("/test/BDMV/PLAYLIST/00003.mpls?token=mock").toString()
            val location = requireNotNull(resolver.locate(url))
            val title = requireNotNull(resolver.parsePlaylist("00003.mpls", playlist("00002", "00001", "00002")))
            repeat(2) {
                server.enqueue(binaryResponse(clipInfo()))
                server.enqueue(MockResponse().setResponseCode(206).addHeader("Content-Range", "bytes 1-1/1000").setBody("x"))
            }
            val clips = resolver.resolveClips(location, title, mapOf("Authorization" to "Bearer fixture"))
            assertEquals(listOf("00002.m2ts", "00001.m2ts", "00002.m2ts"), clips.map { it.url.substringAfterLast('/').substringBefore('?') })
            assertEquals(listOf(500_000L, 1_500_000L, 2_500_000L), clips.map { it.startUs })
            assertEquals(listOf(1_000_000L, 1_000_000L, 1_000_000L), clips.map { it.endUs - it.startUs })
            repeat(2) {
                val meta = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
                val range = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
                assertNull(meta.getHeader("Range"))
                assertEquals("bytes=1-1", range.getHeader("Range"))
                assertEquals("Bearer fixture", range.getHeader("Authorization"))
            }
            assertEquals(4, server.requestCount)
        }
    }

    @Test
    fun `a server ignoring Range and invalid Content-Range replies are rejected`() = runBlocking {
        MockWebServer().use { server ->
            val location = requireNotNull(resolver.locate(server.url("/BDMV/").toString()))
            val title = requireNotNull(resolver.parsePlaylist("00003.mpls", playlist("00002")))
            server.enqueue(binaryResponse(clipInfo()))
            server.enqueue(MockResponse().setBody("ignored Range"))
            val error = runCatching { resolver.resolveClips(location, title, emptyMap()) }.exceptionOrNull()
            assertEquals(BlurayPlaylistResolver.Reason.RANGE, (error as? BlurayPlaylistResolver.DiscException)?.reason)
        }
        assertFalse(resolver.supportsRange(206, "bytes 0-0/30"))
        assertFalse(resolver.supportsRange(206, "bytes 1-1/1"))
        assertFalse(resolver.supportsRange(301, "bytes 1-1/30"))
        assertTrue(resolver.supportsRange(206, "bytes 1-1/30"))
        assertTrue(resolver.supportsRange(206, "bytes 384-767/1000", requiredStart = 384, requiredEnd = null))
    }

    @Test
    fun `playlist discovery does not follow redirects to another origin`() = runBlocking {
        MockWebServer().use { authorized ->
            MockWebServer().use { other ->
                other.enqueue(MockResponse().setBody("unexpected"))
                authorized.enqueue(MockResponse().setResponseCode(302).addHeader("Location", other.url("/PLAYLIST/").toString()))
                val location = requireNotNull(resolver.locate(authorized.url("/BDMV/").toString()))
                val error = runCatching { resolver.resolve(location, mapOf("Cookie" to "session=fixture")) }.exceptionOrNull()
                assertEquals(BlurayPlaylistResolver.Reason.DIRECTORY, (error as? BlurayPlaylistResolver.DiscException)?.reason)
                assertEquals(0, other.requestCount)
            }
        }
    }

    private fun binaryResponse(bytes: ByteArray) = MockResponse().setBody(Buffer().write(bytes))

    private fun playlist(vararg clips: String): ByteArray {
        val bytes = ByteBuffer.allocate(50 + clips.size * 34)
        bytes.put("MPLS0200".toByteArray()).putInt(40)
        bytes.position(40)
        bytes.putInt(6 + clips.size * 34).putShort(0).putShort(clips.size.toShort()).putShort(0)
        clips.forEachIndexed { index, clip ->
            val item = bytes.position()
            bytes.putShort(32).put(clip.toByteArray()).put("M2TS".toByteArray())
            val start = 67_500 + index * 45_000
            bytes.putShort(0).put(2).putInt(start).putInt(start + 45_000)
            bytes.position(item + 34)
        }
        return bytes.array()
    }

    private fun clipInfo(): ByteArray {
        val bytes = ByteBuffer.allocate(66)
        bytes.put("HDMV0200".toByteArray()).putInt(40)
        bytes.position(40)
        bytes.putInt(22).put(0).put(1).putInt(0).put(1).put(2)
        bytes.putShort(0x1001).putInt(0).putInt(45_000).putInt(360_000)
        return bytes.array()
    }
}
