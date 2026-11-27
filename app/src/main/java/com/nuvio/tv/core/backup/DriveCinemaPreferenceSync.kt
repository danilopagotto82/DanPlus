package com.nuvio.tv.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** The stable mapped profile, scoped to both the Drive account and the signed-in app account. */
internal data class DriveCinemaScope(
    val driveAccount: String,
    val appAccount: String,
    val profile: String
)

internal data class DrivePreferenceRevision(val tag: String, val sealed: SealedBox)

internal interface DriveCinemaTransport {
    suspend fun isAuthorized(scope: DriveCinemaScope, device: String): Boolean
    suspend fun read(scope: DriveCinemaScope): DrivePreferenceRevision?

    /** A null expected tag requires an absent destination. */
    suspend fun compareAndSet(scope: DriveCinemaScope, expectedTag: String?, sealed: SealedBox): Boolean
}

internal sealed interface DriveCinemaResult {
    data object Unavailable : DriveCinemaResult
    data object Conflict : DriveCinemaResult
    data object Invalid : DriveCinemaResult
    data class InSync(val value: Int?) : DriveCinemaResult
}

/**
 * Reconciles the one Scout preference outside the server settings catalog.
 * The caller owns the local baseline and must compare the local value before applying a remote result.
 */
internal class DriveCinemaPreferenceSync(private val transport: DriveCinemaTransport) {
    suspend fun reconcile(
        scope: DriveCinemaScope,
        device: String,
        pin: String,
        local: Int?,
        baseline: DriveCinemaResult.InSync?,
        isCurrent: () -> Boolean
    ): DriveCinemaResult = withContext(Dispatchers.IO) {
        require(valid(local) && valid(baseline?.value))
        if (!isCurrent() || !transport.isAuthorized(scope, device)) return@withContext DriveCinemaResult.Unavailable
        val revision = transport.read(scope)
        val remote = if (revision == null) null else try {
            val box = revision.sealed
            require(box.iterations in 1..BackupCrypto.MAX_ITERATIONS && box.data.length <= 8192)
            val data = JSONObject(BackupCrypto.open(box, pin))
            require(data.getInt("version") == 1 && data.getString("drive") == scope.driveAccount &&
                data.getString("account") == scope.appAccount && data.getString("profile") == scope.profile &&
                data.has(FIELD))
            (if (data.isNull(FIELD)) null else data.getInt(FIELD)).also { require(valid(it)) }
        } catch (_: WrongBackupPinException) {
            return@withContext DriveCinemaResult.Invalid
        } catch (_: org.json.JSONException) {
            return@withContext DriveCinemaResult.Invalid
        } catch (_: IllegalArgumentException) {
            return@withContext DriveCinemaResult.Invalid
        }
        if (!isCurrent()) return@withContext DriveCinemaResult.Unavailable
        if (remote == local) return@withContext DriveCinemaResult.InSync(local)
        if (revision != null && baseline == null) return@withContext DriveCinemaResult.Conflict
        if (baseline != null && local == baseline.value) {
            return@withContext if (revision == null) DriveCinemaResult.Conflict else DriveCinemaResult.InSync(remote)
        }
        if (baseline != null && remote != baseline.value) return@withContext DriveCinemaResult.Conflict

        val payload = JSONObject().put("version", 1).put("drive", scope.driveAccount)
            .put("account", scope.appAccount).put("profile", scope.profile)
            .put(FIELD, local ?: JSONObject.NULL)
        val sealed = BackupCrypto.seal(payload.toString(), pin)
        if (!isCurrent() || !transport.isAuthorized(scope, device)) return@withContext DriveCinemaResult.Unavailable
        if (transport.compareAndSet(scope, revision?.tag, sealed)) DriveCinemaResult.InSync(local)
        else DriveCinemaResult.Conflict
    }

    private fun valid(value: Int?): Boolean = value == null || value in setOf(18, 28, 35)

    private companion object {
        const val FIELD = "cinema_favorite_genre_v1"
    }
}

/** Provisioned by the Drive authorization gate. Each grant is scoped to one device and one file. */
internal class GoogleDriveCinemaTransport(
    private val http: OkHttpClient,
    private val scope: DriveCinemaScope,
    private val authorizedDevice: String,
    private val fileId: String,
    private val token: suspend () -> String?,
    private val active: () -> Boolean
) : DriveCinemaTransport {
    override suspend fun isAuthorized(scope: DriveCinemaScope, device: String): Boolean =
        scope == this.scope && device == authorizedDevice && active() && !token().isNullOrBlank()

    override suspend fun read(scope: DriveCinemaScope): DrivePreferenceRevision? {
        val bearer = authorization(scope)
        val before = version(bearer)
        val content = http.newCall(request("https://www.googleapis.com/drive/v2/files/$fileId?alt=media", bearer)).execute().use {
            check(it.isSuccessful) { "Drive media unavailable" }
            val body = it.body ?: error("Empty Drive document")
            body.source().use { source ->
                check(!source.request(16_385)) { "Drive document too large" }
                source.readUtf8()
            }
        }
        check(active() && before == version(bearer)) { "Drive document changed during download" }
        val json = JSONObject(content)
        return DrivePreferenceRevision(before, SealedBox(json.getString("kdf"), json.getInt("iterations"),
            json.getString("salt"), json.getString("iv"), json.getString("data")))
    }

    override suspend fun compareAndSet(scope: DriveCinemaScope, expectedTag: String?, sealed: SealedBox): Boolean {
        if (expectedTag.isNullOrBlank()) return false
        val bearer = authorization(scope)
        val content = JSONObject().put("kdf", sealed.kdf).put("iterations", sealed.iterations)
            .put("salt", sealed.salt).put("iv", sealed.iv).put("data", sealed.data)
        val upload = Request.Builder().url("https://www.googleapis.com/upload/drive/v2/files/$fileId?uploadType=media")
            .header("Authorization", "Bearer $bearer")
            .header("If-Match", expectedTag)
            .put(content.toString().toRequestBody("application/json".toMediaType()))
            .build()
        if (!active()) return false
        return http.newCall(upload).execute().use {
            if (it.code == 412) false else {
                check(it.isSuccessful) { "Drive upload failed" }
                true
            }
        }
    }

    private suspend fun authorization(requested: DriveCinemaScope): String {
        require(requested == scope && fileId.matches(Regex("[A-Za-z0-9_-]{1,256}")) && active())
        return token()?.takeIf(String::isNotBlank)?.also { check(active()) } ?: error("Drive authorization unavailable")
    }

    private fun version(bearer: String): String =
        http.newCall(request("https://www.googleapis.com/drive/v2/files/$fileId?fields=etag", bearer)).execute().use {
            check(it.isSuccessful) { "Drive revision unavailable" }
            val body = it.body ?: error("Missing Drive revision")
            body.source().use { source ->
                check(!source.request(4097)) { "Drive revision response too large" }
                JSONObject(source.readUtf8()).getString("etag").also { tag -> check(tag.isNotBlank()) }
            }
        }

    private fun request(url: String, bearer: String) =
        Request.Builder().url(url).header("Authorization", "Bearer $bearer").build()
}
