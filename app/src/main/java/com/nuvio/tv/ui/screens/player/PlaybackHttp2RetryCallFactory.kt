package com.nuvio.tv.ui.screens.player

import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/** The Media3 playback data source enqueues calls and cancels them if its loader is interrupted. */
internal class PlaybackHttp2RetryCallFactory(
    private val primary: Call.Factory,
    private val retry: Call.Factory,
    private val mediaUrl: HttpUrl
) : Call.Factory {
    override fun newCall(request: Request): Call {
        val first = primary.newCall(request)
        if (!sameOrigin(request.url, mediaUrl) || request.method !in setOf("GET", "HEAD")) return first
        return RetryCall(first)
    }

    private inner class RetryCall(private val first: Call) : Call by first {
        private val lock = Any()
        private var active = first
        private var cancelled = false

        override fun clone(): Call = newCall(first.request())
        override fun isCanceled(): Boolean = synchronized(lock) { cancelled }
        override fun execute(): Response = throw UnsupportedOperationException("Playback calls are asynchronous")

        override fun cancel() {
            synchronized(lock) {
                cancelled = true
                active.cancel()
            }
        }

        override fun enqueue(callback: Callback) {
            first.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = callback.onFailure(this@RetryCall, e)

                override fun onResponse(call: Call, response: Response) {
                    if (!canRetry(first.request(), response)) {
                        deliver(callback, response)
                        return
                    }

                    // Keep the original status and headers for a definitive denial or failed negotiation.
                    val denied = response.newBuilder().body(ByteArray(0).toResponseBody()).build()
                    response.close()
                    val next = synchronized(lock) {
                        if (cancelled) null else retry.newCall(first.request()).also { active = it }
                    }
                    if (next == null) {
                        deliver(callback, denied)
                        return
                    }

                    next.enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) = deliver(callback, denied)

                        override fun onResponse(call: Call, response: Response) {
                            if (response.protocol == Protocol.HTTP_2 && response.isSuccessful) {
                                denied.close()
                                deliver(callback, response)
                            } else {
                                response.close()
                                deliver(callback, denied)
                            }
                        }
                    })
                }
            })
        }

        private fun deliver(callback: Callback, response: Response) {
            if (isCanceled()) {
                response.close()
                callback.onFailure(this, IOException("Playback call cancelled"))
            } else {
                callback.onResponse(this, response)
            }
        }
    }

    private fun canRetry(request: Request, response: Response): Boolean =
        response.code == 403 && response.protocol == Protocol.HTTP_1_1 && response.handshake != null &&
            response.priorResponse == null && response.request.url == request.url &&
            response.header("WWW-Authenticate") == null && response.header("Proxy-Authenticate") == null

    private fun sameOrigin(url: HttpUrl, mediaUrl: HttpUrl): Boolean =
        url.isHttps && mediaUrl.isHttps && url.host == mediaUrl.host && url.port == mediaUrl.port
}
