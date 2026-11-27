package com.nuvio.tv.updater

import android.content.Context
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.updater.model.AppUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the downloaded update APK in app storage and remembers which release it belongs to, so a download made in the
 * background can be installed later, from the banner or by the idle install job.
 */
@Singleton
class UpdateDownloadStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updatePreferences: UpdatePreferences,
    private val apkDownloader: ApkDownloader
) {
    // Files storage instead of cache, which the system may clear before the update is installed.
    private val updatesDir: File get() = File(context.filesDir, "updates")

    // The banner and the background job can ask for the same download; the second one waits and reuses the file.
    private val downloadMutex = Mutex()
    private val _transfer = MutableStateFlow<UpdateTransfer?>(null)
    internal val transfer = _transfer.asStateFlow()

    fun destinationFor(update: AppUpdate): File {
        val safeName = update.assetName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(updatesDir, safeName)
    }

    /** Returns the already downloaded APK for this release, or null when it is missing or incomplete. */
    suspend fun downloadedFileFor(update: AppUpdate): File? {
        val (tag, path, code) = updatePreferences.downloadedUpdate.first() ?: return null
        if (tag != update.tag || code != update.versionCode) return null
        val file = File(path)
        val expectedSize = update.assetSizeBytes
        return withContext(Dispatchers.IO) {
            ownedApk(file)?.takeIf {
                runCatching {
                    ApkInstaller.validateUpdate(context, it, update.versionCode, expectedSize)
                    validateUpdateDigest(it, update.assetDigest)
                }.isSuccess
            }
        }
    }

    /** Returns the downloaded APK when it is for a version newer than the installed one. */
    suspend fun pendingInstall(): PendingInstall? {
        val downloaded = updatePreferences.downloadedUpdate.first() ?: return null
        val file = ownedApk(File(downloaded.apkPath)) ?: return null
        if (!file.exists() || !downloaded.isNewerThanInstalled()) return null
        return PendingInstall(downloaded.tag, file, downloaded.versionCode)
    }

    suspend fun download(
        update: AppUpdate,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
    ): Result<File> = downloadMutex.withLock {
        downloadedFileFor(update)?.let { return@withLock Result.success(it) }
        val previous = updatePreferences.downloadedUpdate.first()
        if (previous != null && installerBusy()) {
            return@withLock Result.failure(IllegalStateException("Update installer is busy"))
        }
        updatesDir.mkdirs()
        val temporary = File.createTempFile("ota-", ".part", updatesDir)
        var unpublished: File? = null
        try {
            val start = android.os.SystemClock.elapsedRealtime()
            var reportedAt = start
            val file = apkDownloader.download(update.assetUrl, temporary) { bytes, total ->
                val now = android.os.SystemClock.elapsedRealtime()
                val expected = total ?: update.assetSizeBytes?.takeIf { it > 0 }
                if (_transfer.value == null || now - reportedAt >= 150 || bytes == expected) {
                    reportedAt = now
                    _transfer.value = UpdateTransfer(update.dismissalKey, bytes, expected,
                        (now - start).takeIf { it >= 1_000L }?.let { (bytes * 1000.0 / it).toLong() })
                }
                onProgress(bytes, total)
            }.getOrThrow()
            val destination = File(updatesDir, file.nameWithoutExtension + "-" + destinationFor(update).name)
            check(file.renameTo(destination)) { "Could not save validated update" }
            unpublished = destination
            validateUpdateDigest(destination, update.assetDigest)
            ApkInstaller.validateUpdate(context, destination, update.versionCode, update.assetSizeBytes)
            withContext(kotlinx.coroutines.NonCancellable) {
                updatePreferences.setDownloadedUpdate(update.tag, destination.absolutePath, update.versionCode)
                unpublished = null
            }
            Result.success(destination)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            _transfer.value = null
            temporary.delete()
            unpublished?.delete()
        }
    }

    private fun installerBusy(): Boolean =
        runCatching { context.packageManager.packageInstaller.mySessions.isNotEmpty() }.getOrDefault(true)

    private fun ownedApk(file: File): File? = privateUpdateApk(updatesDir, file)

    private suspend fun discardTracked(downloaded: DownloadedUpdate): Long {
        val file = ownedApk(File(downloaded.apkPath))
        val bytes = file?.takeIf(File::isFile)?.length() ?: 0L
        if (file == null || !file.exists() || file.delete()) {
            updatePreferences.setDownloadedUpdate(null, null)
            return bytes
        }
        return 0L
    }

    /** Keeps the file in private storage until the installed version confirms the handoff completed. */
    suspend fun stageForInstaller(expectedPath: String): File = downloadMutex.withLock {
        val current = checkNotNull(updatePreferences.downloadedUpdate.first())
        check(current.apkPath == expectedPath) { "Update is no longer available" }
        val file = ownedApk(File(expectedPath))?.takeIf(File::isFile) ?: error("Update file is no longer available")
        if (file.name.startsWith("install-")) return@withLock file
        val handoff = File(updatesDir, "install-" + file.name)
        check(!handoff.exists() && file.renameTo(handoff)) { "Could not reserve update for installer" }
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                updatePreferences.setDownloadedUpdate(current.tag, handoff.absolutePath, current.versionCode)
            }
        } catch (error: Exception) {
            handoff.renameTo(file)
            throw error
        }
        handoff
    }

    /** Returns the number of bytes removed from the private update directory. */
    suspend fun clear(): Long = downloadMutex.withLock {
        if (installerBusy()) return@withLock 0L
        updatePreferences.downloadedUpdate.first()
            ?.takeUnless { File(it.apkPath).name.startsWith("install-") }
            ?.let { discardTracked(it) } ?: 0L
    }

    suspend fun discardReady(expectedPath: String): Long? = downloadMutex.withLock {
        if (installerBusy()) return@withLock null
        val current = updatePreferences.downloadedUpdate.first() ?: return@withLock null
        val file = ownedApk(File(expectedPath)) ?: return@withLock null
        if (current.apkPath != expectedPath || file.name.startsWith("install-")) return@withLock null
        discardTracked(current).takeIf { updatePreferences.downloadedUpdate.first() == null }
    }

    suspend fun clearIfInstalled(): Long = downloadMutex.withLock {
        if (installerBusy()) return@withLock 0L
        val downloaded = updatePreferences.downloadedUpdate.first()
        var bytes = if (downloaded != null && !downloaded.isNewerThanInstalled()) discardTracked(downloaded) else 0L
        updatesDir.listFiles()?.filter {
            it.isFile && it.name.startsWith("ota-") && it.extension == "part" &&
                it.canonicalFile == it.absoluteFile
        }?.forEach {
            val size = it.length()
            if (it.delete()) bytes += size
        }
        bytes
    }
}

data class DownloadedUpdate(val tag: String, val apkPath: String, val versionCode: Long? = null) {
    /** By version code when the release had one, as the update check decides; by name otherwise. */
    fun isNewerThanInstalled(
        installedName: String = BuildConfig.VERSION_NAME,
        installedCode: Long = BuildConfig.VERSION_CODE.toLong()
    ): Boolean = VersionUtils.isRemoteNewer(tag, installedName, versionCode, installedCode)
}

data class PendingInstall(val tag: String, val file: File, val versionCode: Long?)

internal data class UpdateTransfer(val release: String, val bytes: Long, val total: Long?, val bytesPerSecond: Long?)

internal fun privateUpdateApk(directory: File, file: File): File? = runCatching {
    file.canonicalFile.takeIf {
        it.parentFile == directory.canonicalFile && it.name == file.name && it.extension.equals("apk", true)
    }
}.getOrNull()
