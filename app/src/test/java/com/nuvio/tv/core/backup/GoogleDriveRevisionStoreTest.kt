package com.nuvio.tv.core.backup

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveRevisionStoreTest {
    private val namespace = "a".repeat(64)
    private val entry = DriveJournalEntry(emptySet(), SealedBox("test-kdf", 600_000, "salt", "iv", "opaque"))
    private val payload = """{"version":1,"parents":[],"kdf":"test-kdf","iterations":600000,"salt":"salt","iv":"iv","data":"opaque"}"""

    @Test
    fun reservationAndConflictedRetryUseTheSameFileId() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("Bearer fake-token", request.header("Authorization"))
            when {
                request.url.encodedPath.endsWith("/generateIds") -> respond(request, 200, """{"ids":["reserved"]}""")
                request.method == "POST" -> {
                    val buffer = Buffer()
                    checkNotNull(request.body).writeTo(buffer)
                    val body = buffer.readUtf8()
                    assertTrue(body.contains("\"id\":\"reserved\""))
                    assertTrue(body.contains("danplus-settings-${entry.id}"))
                    respond(request, 409, "{}")
                }
                else -> {
                    assertEquals("/drive/v3/files/reserved", request.url.encodedPath)
                    assertEquals("media", request.url.queryParameter("alt"))
                    respond(request, 200, payload)
                }
            }
        }.build()
        val store = GoogleDriveRevisionStore(client, namespace, { "fake-token" }, { true })
        val id = store.reserveUploadId()
        assertEquals("reserved", id)
        store.append(id, entry)
    }

    @Test
    fun listsOnlyTheProfileNamespaceAndReturnsTheCandidateForDecryption() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            if (request.url.encodedPath == "/drive/v3/files") {
                assertTrue(request.url.queryParameter("q").orEmpty().contains("value='$namespace'"))
                respond(request, 200, """{"files":[{"id":"the-file","name":"danplus-settings-${entry.id}"}]}""")
            } else {
                assertEquals("/drive/v3/files/the-file", request.url.encodedPath)
                respond(request, 200, payload)
            }
        }.build()
        val store = GoogleDriveRevisionStore(client, namespace, { "fake-token" }, { true })
        assertEquals(DriveJournalState.Current(entry, setOf(entry.id)), store.readJournal())
    }

    private fun respond(request: okhttp3.Request, status: Int, json: String) = Response.Builder()
        .request(request).code(status).protocol(Protocol.HTTP_1_1).message("fixture").body(json.toResponseBody()).build()
}
