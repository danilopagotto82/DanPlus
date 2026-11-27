package com.nuvio.tv.updater

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateTransferTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun bytesAreReportedFromTheBodyAndValidatedAgainstTheDigest() = runBlocking {
        val contents = "hello".encodeToByteArray()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(contents.toResponseBody()).build()
        }.build()
        val file = directory.newFile("synthetic.bin")
        val calls = mutableListOf<Pair<Long, Long?>>()
        ApkDownloader(client).download("https://example.invalid/update", file) { bytes, total ->
            calls += bytes to total
        }.getOrThrow()
        assertArrayEquals(contents, file.readBytes())
        assertEquals(5L to 5L, calls.last())
        validateUpdateDigest(file, "sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
        assertThrows(IllegalArgumentException::class.java) {
            validateUpdateDigest(file, "sha256:" + "0".repeat(64))
        }
        Unit
    }
    @Test fun cleanupPathsMustBelongToTheUpdateDirectoryAndHaveApkExtension() {
        val updates = directory.newFolder("updates")
        val backup = directory.newFolder("backup")
        val pending = java.io.File(updates, "pending.apk")
        assertEquals(pending.canonicalFile, privateUpdateApk(updates, pending))
        assertEquals(null, privateUpdateApk(updates, java.io.File(backup, "pending.apk")))
        assertEquals(null, privateUpdateApk(updates, java.io.File(updates, "manifest.json")))
    }
    @Test fun unknownLengthReportsBytesAndTruncatedKnownLengthIsRejected() = runBlocking {
        val bytes = "abc".encodeToByteArray()
        suspend fun transfer(length: Long, file: java.io.File, report: (Long, Long?) -> Unit): Result<java.io.File> {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val body = object : okhttp3.ResponseBody() {
                    private val data = okio.Buffer().write(bytes)
                    override fun contentLength() = length
                    override fun contentType(): okhttp3.MediaType? = null
                    override fun source(): okio.BufferedSource = data
                }
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("Fixture").body(body).build()
            }.build()
            return ApkDownloader(client).download("https://example.invalid/update", file, report)
        }
        var reported: Pair<Long, Long?>? = null
        assertEquals(true, transfer(-1, directory.newFile("unknown.bin")) { count, total ->
            reported = count to total
        }.isSuccess)
        assertEquals(3L to null, reported)
        val truncated = directory.newFile("truncated.bin")
        assertEquals(true, transfer(8, truncated) { _, _ -> }.isFailure)
        assertEquals(false, truncated.exists())
    }
}
