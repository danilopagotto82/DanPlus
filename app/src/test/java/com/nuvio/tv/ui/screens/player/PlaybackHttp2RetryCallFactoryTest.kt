package com.nuvio.tv.ui.screens.player

import okhttp3.Call
import okhttp3.Callback
import okhttp3.CipherSuite
import okhttp3.Handshake
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.TlsVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PlaybackHttp2RetryCallFactoryTest {
    @Test
    fun retriesAQualified403OnceWithTheOriginalHeadersAndRange() {
        val h = Harness()
        h.primary.single().respond(403)
        val retry = h.retries.single()
        assertSame(h.request, retry.request())
        assertEquals("bytes=4096-", retry.request().header("Range"))
        assertEquals("Bearer synthetic", retry.request().header("Authorization"))
        retry.respond(206, Protocol.HTTP_2)
        assertEquals(206, h.status)
        assertEquals(1, h.retries.size)
    }

    @Test
    fun unsuccessfulRetryKeepsTheOriginal403() {
        for (code in listOf(403, 500)) {
            val h = Harness()
            h.primary.single().respond(403)
            h.retries.single().respond(code, Protocol.HTTP_2)
            assertEquals(403, h.status)
            assertEquals(1, h.retries.size)
        }
        val h = Harness()
        h.primary.single().respond(403)
        h.retries.single().respond(200, Protocol.HTTP_1_1)
        assertEquals(403, h.status)
    }

    @Test
    fun doesNotReplayChallengesRedirectsOtherOriginsOrNon403Errors() {
        val challenge = Harness()
        challenge.primary.single().respond(403, challenge = true)
        assertTrue(challenge.retries.isEmpty())
        val redirect = Harness()
        redirect.primary.single().respond(403, redirected = true)
        assertTrue(redirect.retries.isEmpty())
        val other = Harness("https://other.invalid/video")
        other.primary.single().respond(403)
        assertTrue(other.retries.isEmpty())
        val serverError = Harness()
        serverError.primary.single().respond(500)
        assertTrue(serverError.retries.isEmpty())
    }

    @Test
    fun cancellationDuringRetryReachesTheActiveCallAndReportsNoRequestData() {
        val h = Harness()
        h.primary.single().respond(403)
        h.call.cancel()
        val retry = h.retries.single()
        assertTrue(retry.cancelled)
        retry.respond(206, Protocol.HTTP_2)
        assertNull(h.status)
        assertEquals("Playback call cancelled", h.error?.message)
    }

    private class Harness(url: String = "https://cdn.invalid/video") {
        val request = Request.Builder().url(url)
            .header("Range", "bytes=4096-")
            .header("Authorization", "Bearer synthetic")
            .build()
        val primary = mutableListOf<Pending>()
        val retries = mutableListOf<Pending>()
        var status: Int? = null
        var error: IOException? = null
        val call = PlaybackHttp2RetryCallFactory(
            Call.Factory { Pending(it).also(primary::add) },
            Call.Factory { Pending(it).also(retries::add) },
            "https://cdn.invalid/video".toHttpUrl()
        ).newCall(request)

        init {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { error = e }
                override fun onResponse(call: Call, response: Response) { response.use { status = it.code } }
            })
        }
    }

    private class Pending(private val original: Request) : Call by client.newCall(original) {
        lateinit var callback: Callback
        var cancelled = false
        override fun enqueue(responseCallback: Callback) { callback = responseCallback }
        override fun cancel() { cancelled = true }

        fun respond(
            code: Int,
            protocol: Protocol = Protocol.HTTP_1_1,
            challenge: Boolean = false,
            redirected: Boolean = false
        ) {
            val response = Response.Builder().request(original).code(code).protocol(protocol)
                .message("Synthetic response").handshake(tls).body("example body".toResponseBody())
            if (challenge) response.header("WWW-Authenticate", "Bearer")
            if (redirected) response.priorResponse(
                Response.Builder().request(original).code(302).protocol(protocol).message("Redirect").build()
            )
            callback.onResponse(this, response.build())
        }
    }

    companion object {
        private val client = OkHttpClient()
        private val tls = Handshake.get(TlsVersion.TLS_1_2, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, emptyList(), emptyList())
    }
}
