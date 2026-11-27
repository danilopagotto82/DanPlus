package com.nuvio.tv.updater

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@Singleton
class ApkDownloader @Inject constructor(private val okHttpClient: OkHttpClient) {
    suspend fun download(
        url: String,
        destinationFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<File> = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).build()
        val call = okHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        check(it.isSuccessful) { "Download failed: HTTP " + it.code }
                        val body = it.body ?: error("Empty download body")
                        val total = body.contentLength().takeIf { length -> length > 0 }
                        check(continuation.isActive)
                        destinationFile.parentFile?.mkdirs()
                        body.byteStream().use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var downloaded = 0L
                                while (continuation.isActive) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    onProgress(downloaded, total)
                                }
                                check(continuation.isActive)
                                output.flush()
                                check(total == null || downloaded == total) { "Incomplete update download" }
                            }
                        }
                    }
                    destinationFile
                }
                if (result.isFailure || !continuation.isActive) destinationFile.delete()
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }
}
