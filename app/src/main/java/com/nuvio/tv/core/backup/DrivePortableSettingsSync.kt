package com.nuvio.tv.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Only preferences absent from the Nuvio/ARC settings payload. */
internal data class DrivePortableSettings(
    val favoriteGenre: Int?,
    val scoutInMenu: Boolean,
    val scoutListEmojis: Boolean,
    val scoutShowWatched: Boolean = true
) {
    init {
        require(favoriteGenre == null || favoriteGenre in setOf(18, 28, 35))
    }
}

internal sealed interface DrivePortableResult {
    data object Unavailable : DrivePortableResult
    data object Conflict : DrivePortableResult
    data object Invalid : DrivePortableResult
    data class InSync(val settings: DrivePortableSettings) : DrivePortableResult
}

/**
 * Version 3 uses a separately provisioned file. The caller captures a profile session and
 * applies the result only if that session and the local snapshot still match.
 */
internal class DrivePortableSettingsSync(private val transport: DriveCinemaTransport) {
    suspend fun reconcile(
        scope: DriveCinemaScope,
        device: String,
        pin: String,
        local: DrivePortableSettings,
        baseline: DrivePortableSettings?,
        isCurrent: () -> Boolean
    ): DrivePortableResult = withContext(Dispatchers.IO) {
        if (!isCurrent() || !transport.isAuthorized(scope, device)) {
            return@withContext DrivePortableResult.Unavailable
        }

        if (!BackupCrypto.isValidPin(pin)) return@withContext DrivePortableResult.Invalid
        val revision = transport.read(scope)
        if (!isCurrent()) return@withContext DrivePortableResult.Unavailable
        val remote = revision?.let {
            try {
                decode(it.sealed, scope, pin)
            } catch (_: WrongBackupPinException) {
                return@withContext DrivePortableResult.Invalid
            } catch (_: JSONException) {
                return@withContext DrivePortableResult.Invalid
            } catch (_: IllegalArgumentException) {
                return@withContext DrivePortableResult.Invalid
            }
        }
        val merged = when {
            remote == local -> local
            remote == null && baseline == null -> local
            remote == null || baseline == null -> return@withContext DrivePortableResult.Conflict
            else -> merge(local, remote, baseline) ?: return@withContext DrivePortableResult.Conflict
        }
        if (merged == remote) {
            return@withContext if (isCurrent() && transport.isAuthorized(scope, device) && isCurrent()) {
                DrivePortableResult.InSync(merged)
            } else {
                DrivePortableResult.Unavailable
            }
        }

        val sealed = BackupCrypto.seal(encode(scope, merged).toString(), pin)
        if (!isCurrent() || !transport.isAuthorized(scope, device) || !isCurrent()) {
            return@withContext DrivePortableResult.Unavailable
        }
        val saved = transport.compareAndSet(scope, revision?.tag, sealed)
        when {
            !isCurrent() -> DrivePortableResult.Unavailable
            saved -> DrivePortableResult.InSync(merged)
            else -> DrivePortableResult.Conflict
        }
    }

    private fun merge(
        local: DrivePortableSettings,
        remote: DrivePortableSettings,
        baseline: DrivePortableSettings
    ): DrivePortableSettings? {
        val localGenreChanged = local.favoriteGenre != baseline.favoriteGenre
        val remoteGenreChanged = remote.favoriteGenre != baseline.favoriteGenre
        if (localGenreChanged && remoteGenreChanged && local.favoriteGenre != remote.favoriteGenre) {
            return null
        }
        return DrivePortableSettings(
            favoriteGenre = if (localGenreChanged) local.favoriteGenre else remote.favoriteGenre,
            scoutInMenu = if (local.scoutInMenu != baseline.scoutInMenu) local.scoutInMenu else remote.scoutInMenu,
            scoutListEmojis = if (local.scoutListEmojis != baseline.scoutListEmojis) local.scoutListEmojis else remote.scoutListEmojis,
            scoutShowWatched = if (local.scoutShowWatched != baseline.scoutShowWatched) local.scoutShowWatched else remote.scoutShowWatched
        )
    }

    private fun encode(scope: DriveCinemaScope, settings: DrivePortableSettings) = JSONObject()
        .put("version", 3)
        .put("drive", scope.driveAccount)
        .put("account", scope.appAccount)
        .put("profile", scope.profile)
        .put("cinema_favorite_genre_v1", settings.favoriteGenre ?: JSONObject.NULL)
        .put("show_in_menu", settings.scoutInMenu)
        .put("list_emojis", settings.scoutListEmojis)
        .put("show_watched", settings.scoutShowWatched)

    private fun decode(box: SealedBox, scope: DriveCinemaScope, pin: String): DrivePortableSettings {
        require(box.iterations in 1..BackupCrypto.MAX_ITERATIONS && box.data.length <= 8192)
        val payload = JSONObject(BackupCrypto.open(box, pin))
        require(payload.get("version") == 3 && payload.get("drive") == scope.driveAccount &&
            payload.get("account") == scope.appAccount && payload.get("profile") == scope.profile)
        val genre = payload.get("cinema_favorite_genre_v1")
        require(genre == JSONObject.NULL || genre is Int)
        val showInMenu = payload.get("show_in_menu")
        val listEmojis = payload.get("list_emojis")
        val showWatched = payload.get("show_watched")
        require(showInMenu is Boolean && listEmojis is Boolean && showWatched is Boolean)
        return DrivePortableSettings(genre as? Int, showInMenu, listEmojis, showWatched)
    }
}
