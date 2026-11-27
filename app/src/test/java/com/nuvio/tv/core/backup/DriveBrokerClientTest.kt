package com.nuvio.tv.core.backup

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DriveBrokerClientTest {
    private val endpoint = checkNotNull(driveBrokerEndpoint("https://example.supabase.co"))
    private val identity = DriveCinemaScope("drive-a", "owner-a", "profile-a")

    @Test
    fun endpointAndNamespaceAreBoundToExpectedIdentity() {
        assertNull(driveBrokerEndpoint("http://example.supabase.co"))
        assertNull(driveBrokerEndpoint("https://example.supabase.co.evil.test"))
        assertNull(driveBrokerEndpoint("https://example.supabase.co/other"))
        assertEquals("/functions/v1/danplus-drive-auth", endpoint.encodedPath)
        assertEquals(64, driveNamespace(identity).length)
        assertNotEquals(driveNamespace(identity), driveNamespace(identity.copy(profile = "profile-b")))
        assertNotEquals(driveNamespace(identity), driveNamespace(identity.copy(appAccount = "owner-b")))
        assertNotEquals(driveNamespace(identity), driveNamespace(identity.copy(driveAccount = "drive-b")))
    }

    @Test
    fun deviceGrantUsesOnlyBrokerAndChecksGoogleAccountIdentity() = runTest {
        var polls = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            if (request.url.host == "www.googleapis.com") {
                assertEquals("/drive/v3/about", request.url.encodedPath)
                assertEquals("Bearer fake-access", request.header("Authorization"))
                return@addInterceptor reply(request, 200, """{"user":{"permissionId":"drive-a"}}""")
            }
            assertEquals(endpoint, request.url)
            assertEquals("Bearer fixture-bearer", request.header("Authorization"))
            assertEquals("fixture-key", request.header("apikey"))
            val b = Buffer()
            checkNotNull(request.body).writeTo(b)
            val body = JSONObject(b.readUtf8())
            assertFalse(body.has("client_secret"))
            when (body.getString("operation")) {
                "start" -> reply(request, 200, """{"device_code":"signed-ticket","user_code":"ABCD-EFGH","verification_url":"https://www.google.com/device","expires_in":600,"interval":5}""")
                "poll" -> {
                    assertEquals("signed-ticket", body.getString("device_code"))
                    if (++polls == 1) reply(request, 400, """{"error":"authorization_pending"}""")
                    else reply(request, 200, """{"client_id":"fake-client","access_token":"fake-access","refresh_token":"fake-refresh","scope":"https://www.googleapis.com/auth/drive.file","token_type":"Bearer","expires_in":3600}""")
                }
                else -> error("Unexpected operation")
            }
        }.build()
        val client = DriveBrokerClient(http, endpoint, { DriveBrokerHeaders("fixture-key", "fixture-nuvio-key", "fixture-bearer") },
            { true }, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler), { testScheduler.currentTime })
        val code = client.start()
        assertEquals("ABCD-EFGH", code.userCode)
        val grant = checkNotNull(client.awaitGrant(code))
        assertEquals(2, polls)
        assertEquals("fake-client", grant.clientId)
        assertEquals("drive-a", driveAccountId(http, grant.tokens.accessToken) { true })
    }

    @Test
    fun storedGrantMatchesOnlyItsAccountAndStableProfile() {
        val saved = DriveStoredGrant("actor", identity, "fixture-client", "fixture-refresh", "531602947284")
        val recovered = DriveStoredGrant.decode(saved.encode())
        assertTrue(recovered.matches("actor", "owner-a", "profile-a"))
        assertFalse(recovered.matches("actor", "owner-b", "profile-a"))
        assertFalse(recovered.matches("actor-b", "owner-a", "profile-a"))
        assertFalse(recovered.matches("actor", "owner-a", "profile-b"))
        assertEquals("fixture-refresh", recovered.refresh)
        assertEquals(identity, recovered.scope)
    }

    private fun reply(request: okhttp3.Request, code: Int, text: String) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
        .body(text.toResponseBody()).build()
}
