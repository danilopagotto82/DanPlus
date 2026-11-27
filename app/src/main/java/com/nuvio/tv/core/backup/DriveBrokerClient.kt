package com.nuvio.tv.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal class DriveBrokerHeaders(val publishable: String, val nuvioKey: String, val bearer: String)
internal class DriveBrokerChallenge(
    internal val ticket: String, val userCode: String, val url: HttpUrl,
    internal val expiresAt: Long, internal val interval: Long
)
internal class DriveBrokerGrant(val clientId: String, val tokens: DriveDeviceTokens)

internal fun driveBrokerEndpoint(base: String): HttpUrl? =
    base.trim().trimEnd('/').takeIf { Regex("https://[a-z0-9-]+\\.supabase\\.co").matches(it) }
        ?.plus("/functions/v1/danplus-drive-auth")?.toHttpUrlOrNull()

internal fun driveNamespace(scope: DriveCinemaScope): String =
    MessageDigest.getInstance("SHA-256")
        .digest(listOf("danplus-drive-v3", scope.driveAccount, scope.appAccount, scope.profile)
            .joinToString("") { "${it.length}:$it" }.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

internal class DriveBrokerClient(
    private val http: OkHttpClient,
    private val endpoint: HttpUrl,
    private val headers: () -> DriveBrokerHeaders?,
    private val isCurrent: () -> Boolean,
    private val now: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) {
    init { require(endpoint == driveBrokerEndpoint("https://" + endpoint.host)) }

    suspend fun start(): DriveBrokerChallenge {
        val started = now()
        val (status, json) = request(JSONObject().put("operation", "start"))
        if (status != 200) throw DriveAuthorizationException(status)
        val url = json.getString("verification_url").toHttpUrlOrNull()
        require(url != null && url.isHttps && url.port == 443 &&
            url.host in setOf("google.com", "www.google.com", "accounts.google.com") &&
            url.username.isEmpty() && url.password.isEmpty())
        return DriveBrokerChallenge(json.bounded("device_code", 12000), json.bounded("user_code", 128),
            url, started + seconds(json, "expires_in"), seconds(json, "interval").coerceAtLeast(5000))
    }

    suspend fun awaitGrant(code: DriveBrokerChallenge): DriveBrokerGrant? {
        val remaining = code.expiresAt - now()
        if (remaining <= 0) return null
        return withTimeoutOrNull(remaining) {
            var interval = code.interval
            while (true) {
                delay(interval)
                if (!isCurrent()) return@withTimeoutOrNull null
                val (status, json) = request(JSONObject().put("operation", "poll").put("device_code", code.ticket))
                if (status == 200) {
                    require(json.getString("scope") == "https://www.googleapis.com/auth/drive.file")
                    require(json.getString("token_type").equals("Bearer", ignoreCase = true))
                    return@withTimeoutOrNull DriveBrokerGrant(json.bounded("client_id", 1024),
                        DriveDeviceTokens(json.bounded("access_token", 4096), json.bounded("refresh_token", 4096),
                            seconds(json, "expires_in") / 1000))
                }
                if (status != 400) throw DriveAuthorizationException(status)
                when (json.optString("error")) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval += 5000
                    "expired_token", "access_denied" -> return@withTimeoutOrNull null
                    else -> throw DriveAuthorizationException(status)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
    }

    private suspend fun request(body: JSONObject): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        check(isCurrent())
        val auth = checkNotNull(headers())
        val request = Request.Builder().url(endpoint)
            .header("apikey", auth.publishable)
            .header("X-Nuvio-Publishable-Key", auth.nuvioKey)
            .header("Authorization", "Bearer " + auth.bearer)
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            check(isCurrent())
            val source = checkNotNull(response.body).source()
            check(!source.request(16_385L))
            response.code to JSONObject(source.readUtf8())
        }
    }

    private fun seconds(json: JSONObject, key: String): Long =
        json.getLong(key).also { require(it in 1..86_400) } * 1000

    private fun JSONObject.bounded(key: String, max: Int): String =
        getString(key).also { require(it.isNotBlank() && it.length <= max) }
}

/** Google returns a durable Drive user permissionId, independent of the display email. */
internal suspend fun driveAccountId(http: OkHttpClient, token: String, isCurrent: () -> Boolean): String =
    withContext(Dispatchers.IO) {
        check(isCurrent())
        val request = Request.Builder().url("https://www.googleapis.com/drive/v3/about?fields=user(permissionId)")
            .header("Authorization", "Bearer $token").build()
        http.newCall(request).execute().use { response ->
            check(isCurrent() && response.isSuccessful)
            val source = checkNotNull(response.body).source()
            check(!source.request(16_385L))
            JSONObject(source.readUtf8()).getJSONObject("user").getString("permissionId")
                .also { require(it.isNotBlank() && it.length <= 256) }
        }
    }
