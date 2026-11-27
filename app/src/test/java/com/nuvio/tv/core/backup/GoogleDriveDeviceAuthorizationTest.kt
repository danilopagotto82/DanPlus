package com.nuvio.tv.core.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleDriveDeviceAuthorizationTest {
    @Test
    fun pollsWithBackoffAndAcceptsOnlyDriveAppData() = runTest {
        val api = FakeApi(mutableListOf(error(428, "authorization_pending"), error(403, "slow_down"), token()))
        val authorization = GoogleDriveDeviceAuthorization(api) { testScheduler.currentTime }
        val code = authorization.start("fake-client")
        assertEquals("TEST-123", code.userCode)

        val tokens = authorization.awaitGrant("fake-client", "fake-secret", code)
        assertNotNull(tokens)
        assertEquals(11_000, testScheduler.currentTime)
        assertEquals(3, api.polls)
        assertEquals("fake-refresh", tokens?.refreshToken)
    }

    @Test
    fun stopsWhenCodeExpires() = runTest {
        val api = FakeApi(mutableListOf(error(428, "authorization_pending")), expiresSeconds = 3)
        val authorization = GoogleDriveDeviceAuthorization(api) { testScheduler.currentTime }
        val code = authorization.start("fake-client")
        assertNull(authorization.awaitGrant("fake-client", "fake-secret", code))
        assertEquals(1, api.polls)
        assertEquals(3_000, testScheduler.currentTime)
    }

    @Test
    fun rejectedScopesDoNotProduceTokens() = runTest {
        val api = FakeApi(mutableListOf(token("https://www.googleapis.com/auth/drive")))
        val authorization = GoogleDriveDeviceAuthorization(api) { testScheduler.currentTime }
        val code = authorization.start("fake-client")
        val error = runCatching { authorization.awaitGrant("fake-client", "fake-secret", code) }.exceptionOrNull()
        assertTrue(error is DriveAuthorizationException)
    }

    @Test
    fun refreshKeepsStableTokenAndRevocationReportsFailure() = runTest {
        val api = FakeApi(mutableListOf())
        val authorization = GoogleDriveDeviceAuthorization(api)
        assertEquals("fake-refresh", authorization.refresh("fake-client", "fake-refresh").refreshToken)
        authorization.revoke("fake-refresh")
        api.revokeStatus = 400
        val result = runCatching { authorization.revoke("fake-refresh") }.exceptionOrNull()
        assertEquals(400, (result as? DriveAuthorizationException)?.status)
    }

    private class FakeApi(
        private val responses: MutableList<Response<ResponseBody>>,
        private val expiresSeconds: Int = 30
    ) : DeviceAuthorizationApi {
        var polls = 0
        var revokeStatus = 200

        override suspend fun start(clientId: String, scope: String): Response<ResponseBody> {
            assertEquals(SCOPE, scope)
            return success("""{
                "user_code":"TEST-123","device_code":"fake-device","verification_url":"https://www.google.com/device",
                "interval":2,"expires_in":$expiresSeconds
            }""")
        }

        override suspend fun poll(
            clientId: String,
            clientSecret: String,
            deviceCode: String,
            grantType: String
        ): Response<ResponseBody> {
            assertEquals("fake-device", deviceCode)
            assertEquals("urn:ietf:params:oauth:grant-type:device_code", grantType)
            polls++
            return responses.removeAt(0)
        }

        override suspend fun refresh(clientId: String, refreshToken: String, grantType: String): Response<ResponseBody> {
            assertEquals("refresh_token", grantType)
            return success("""{"access_token":"renewed","expires_in":3600,"scope":"$SCOPE","token_type":"Bearer"}""")
        }

        override suspend fun revoke(refreshToken: String): Response<ResponseBody> =
            if (revokeStatus == 200) success("{}") else error(revokeStatus, "invalid_token")
    }

    private companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

        fun success(json: String): Response<ResponseBody> = Response.success(json.toResponseBody())

        fun error(status: Int, error: String): Response<ResponseBody> =
            Response.error(status, """{"error":"$error"}""".toResponseBody())

        fun token(scope: String = SCOPE): Response<ResponseBody> = success("""{
            "access_token":"fake-access","refresh_token":"fake-refresh","expires_in":3600,
            "scope":"$scope","token_type":"Bearer"
        }""")
    }
}
