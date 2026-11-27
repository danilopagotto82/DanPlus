package com.nuvio.tv.core.backup

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private const val DRIVE_APP_DATA = "https://www.googleapis.com/auth/drive.appdata"
private const val DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"

internal class DriveDeviceCode(
    internal val deviceCode: String,
    val userCode: String,
    val verificationUrl: HttpUrl,
    internal val expiresAtMs: Long,
    internal val intervalMs: Long
)

internal class DriveDeviceTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

internal class DriveAuthorizationException(val status: Int) : Exception("Drive authorization failed (HTTP $status)")

/** The owner supplies the OAuth client privately. This protocol is independent of Play Services. */
internal class GoogleDriveDeviceAuthorization(
    private val api: DeviceAuthorizationApi = googleDeviceAuthorizationApi(),
    private val monotonicMs: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) {
    suspend fun start(clientId: String): DriveDeviceCode {
        require(clientId.isNotBlank())
        val startedAt = monotonicMs()
        val response = api.start(clientId, DRIVE_APP_DATA)
        val body = response.json()
        if (!response.isSuccessful) throw DriveAuthorizationException(response.code())
        val url = body.optString("verification_url").toHttpUrlOrNull()
        require(url != null && url.isHttps && url.host in setOf("google.com", "www.google.com", "accounts.google.com") &&
            url.username.isEmpty() && url.password.isEmpty())
        return DriveDeviceCode(
            deviceCode = body.requiredNonBlank("device_code"),
            userCode = body.requiredNonBlank("user_code"),
            verificationUrl = url,
            expiresAtMs = startedAt + responseSeconds(body.getLong("expires_in")),
            intervalMs = responseSeconds(body.getLong("interval"))
        )
    }

    /** Null means the code expired. Denials and wrong scopes never yield tokens. */
    suspend fun awaitGrant(clientId: String, clientSecret: String, code: DriveDeviceCode): DriveDeviceTokens? {
        require(clientId.isNotBlank() && clientSecret.isNotBlank())
        val remaining = code.expiresAtMs - monotonicMs()
        if (remaining <= 0) return null
        return withTimeoutOrNull(remaining) {
            var interval = code.intervalMs
            while (true) {
                delay(interval)
                val response = api.poll(clientId, clientSecret, code.deviceCode, DEVICE_GRANT)
                val body = response.json()
                if (response.isSuccessful) return@withTimeoutOrNull body.tokens(response.code())
                when (body.optString("error")) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval += 5_000L
                    "expired_token" -> return@withTimeoutOrNull null
                    else -> throw DriveAuthorizationException(response.code())
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
    }

    suspend fun refresh(clientId: String, refreshToken: String): DriveDeviceTokens {
        require(clientId.isNotBlank() && refreshToken.isNotBlank())
        val response = api.refresh(clientId, refreshToken, "refresh_token")
        val body = response.json()
        if (!response.isSuccessful) throw DriveAuthorizationException(response.code())
        return body.tokens(response.code(), refreshToken)
    }

    suspend fun revoke(refreshToken: String) {
        require(refreshToken.isNotBlank())
        val response = api.revoke(refreshToken)
        (response.body() ?: response.errorBody())?.close()
        if (!response.isSuccessful) throw DriveAuthorizationException(response.code())
    }

    private fun Response<ResponseBody>.json(): JSONObject {
        val body = body() ?: errorBody() ?: throw DriveAuthorizationException(code())
        return body.use {
            try {
                val maxSize = 16_384L
                val source = it.source()
                if (source.request(maxSize + 1)) throw DriveAuthorizationException(code())
                JSONObject(source.readUtf8())
            } catch (_: org.json.JSONException) {
                throw DriveAuthorizationException(code())
            }
        }
    }

    private fun JSONObject.tokens(status: Int, existingRefresh: String? = null): DriveDeviceTokens {
        val scopes = optString("scope").split(' ').filter { it.isNotBlank() }.toSet()
        if (scopes != setOf(DRIVE_APP_DATA) || !optString("token_type").equals("Bearer", ignoreCase = true)) {
            throw DriveAuthorizationException(status)
        }
        val expires = getLong("expires_in")
        responseSeconds(expires)
        val refresh = optString("refresh_token").takeIf(String::isNotBlank)
            ?: existingRefresh ?: throw DriveAuthorizationException(status)
        return DriveDeviceTokens(requiredNonBlank("access_token"), refresh, expires)
    }

    private fun JSONObject.requiredNonBlank(name: String): String = getString(name).also {
        require(it.isNotBlank() && it.length <= 4096)
    }

    private fun responseSeconds(seconds: Long): Long {
        require(seconds in 1..86_400L)
        return seconds * 1000L
    }
}

internal interface DeviceAuthorizationApi {
    @FormUrlEncoded
    @POST("device/code")
    suspend fun start(@Field("client_id") clientId: String, @Field("scope") scope: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("token")
    suspend fun poll(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("token")
    suspend fun refresh(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("revoke")
    suspend fun revoke(@Field("token") refreshToken: String): Response<ResponseBody>
}

private fun googleDeviceAuthorizationApi(): DeviceAuthorizationApi = Retrofit.Builder()
    .baseUrl("https://oauth2.googleapis.com/")
    .client(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(15, TimeUnit.SECONDS).build())
    .build()
    .create(DeviceAuthorizationApi::class.java)
