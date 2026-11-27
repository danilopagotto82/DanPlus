package com.nuvio.tv.core.backup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertFalse
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class DriveCinemaPreferenceSyncTest {
    private val a = DriveCinemaScope("synthetic-drive", "synthetic-account", "profile-1")
    private val b = a.copy(profile = "profile-2")

    @Test fun independentDevicesMergeWithRevisionChecksAndCannotReadAnotherProfile() = runBlocking {
        val remote = StubTransport()
        val sync = DriveCinemaPreferenceSync(remote)
        val first = sync.reconcile(a, "authorized", "384921", 28, null) { true }
        assertEquals(DriveCinemaResult.InSync(28), first)
        val baseline = first as DriveCinemaResult.InSync
        remote.race = true
        assertEquals(DriveCinemaResult.Conflict, sync.reconcile(a, "authorized", "384921", 35, baseline) { true })
        remote.race = false
        assertEquals(DriveCinemaResult.InSync(35), sync.reconcile(a, "authorized", "384921", 35, baseline) { true })
        assertEquals(DriveCinemaResult.Conflict, sync.reconcile(a, "authorized", "384921", 18, baseline) { true })
        assertEquals(DriveCinemaResult.InSync(35), sync.reconcile(a, "authorized", "384921", 28, baseline) { true })
        assertEquals(DriveCinemaResult.Unavailable, sync.reconcile(a, "unapproved", "384921", 28, baseline) { true })
        assertEquals(DriveCinemaResult.Invalid, sync.reconcile(a, "authorized", "849213", 28, baseline) { true })
        remote.saved[b] = remote.saved.getValue(a)
        assertEquals(DriveCinemaResult.Invalid, sync.reconcile(b, "authorized", "384921", 28, baseline) { true })
    }

    @Test fun transportSendsEncryptedPayloadWithIfMatchAndTreats412AsConflict() = runBlocking {
        val calls = mutableListOf<okhttp3.Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().also(calls::add)
            val media = request.url.queryParameter("alt") == "media"
            val put = request.method == "PUT"
            val payload = if (media) {
                """{"kdf":"fixture","iterations":1,"salt":"fixture","iv":"fixture","data":"ciphertext"}"""
            } else """{"etag":"revision-17"}"""
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(if (put) 412 else 200).message("Fixture").body(payload.toResponseBody()).build()
        }.build()
        val transport = GoogleDriveCinemaTransport(client, a, "authorized", "file", { "fake-token" }, { true })
        assertFalse(transport.isAuthorized(b, "authorized"))
        val record = requireNotNull(transport.read(a))
        assertEquals("revision-17", record.tag)
        assertFalse(transport.compareAndSet(a, record.tag, record.sealed))
        assertEquals("revision-17", calls.single { it.method == "PUT" }.header("If-Match"))
    }

    private class StubTransport : DriveCinemaTransport {
        val saved = mutableMapOf<DriveCinemaScope, DrivePreferenceRevision>()
        var race = false
        private var sequence = 0
        override suspend fun isAuthorized(scope: DriveCinemaScope, device: String) = device == "authorized"
        override suspend fun read(scope: DriveCinemaScope) = saved[scope]
        override suspend fun compareAndSet(scope: DriveCinemaScope, expectedTag: String?, sealed: SealedBox): Boolean {
            if (race || saved[scope]?.tag != expectedTag) return false
            saved[scope] = DrivePreferenceRevision((++sequence).toString(), sealed)
            return true
        }
    }
}
