package com.nuvio.tv.core.backup

import android.content.Context
import com.nuvio.tv.core.profile.ProtectedProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal fun encodeDriveJournalFile(entry: DriveJournalEntry): JSONObject =
    JSONObject().put("version", 1).put("parents", JSONArray(entry.parents.sorted()))
        .put("kdf", entry.sealed.kdf).put("iterations", entry.sealed.iterations)
        .put("salt", entry.sealed.salt).put("iv", entry.sealed.iv).put("data", entry.sealed.data)

internal fun parseDriveJournalFile(json: JSONObject): DriveJournalEntry {
    require(json.getInt("version") == 1)
    val parentIds = json.getJSONArray("parents")
    require(parentIds.length() <= 256)
    val parents = (0 until parentIds.length()).map { parentIds.getString(it) }.toSet()
    require(parents.all { it.matches(REVISION_ID) } && parents.size == parentIds.length())
    val data = json.getString("data")
    require(data.length <= 8192)
    return DriveJournalEntry(parents, SealedBox(json.getString("kdf"), json.getInt("iterations"),
        json.getString("salt"), json.getString("iv"), data))
}

internal class ProtectedDriveJournalProgress(
    context: Context,
    private val localProfileId: Int,
    private val isCurrent: () -> Boolean
) : DriveJournalProgressStore {
    private val vault = ProtectedProfilePreferences(context, "drive_journal_progress", "drive_journal_progress_v1")

    override suspend fun load(scope: DriveCinemaScope): DriveJournalProgress = withContext(Dispatchers.IO) {
        check(isCurrent())
        vault.read(localProfileId)?.let { decodeProgress(scope, it) } ?: DriveJournalProgress()
    }

    override suspend fun save(scope: DriveCinemaScope, progress: DriveJournalProgress) = withContext(Dispatchers.IO) {
        check(isCurrent())
        vault.write(localProfileId, encodeProgress(scope, progress))
    }
}

internal fun encodeProgress(scope: DriveCinemaScope, progress: DriveJournalProgress): String = JSONObject()
    .put("drive", scope.driveAccount)
    .put("account", scope.appAccount)
    .put("profile", scope.profile)
    .apply {
        progress.baseline?.let { baseline ->
            put("baseline", encodeDriveSettings(scope, baseline.settings).put("revision", baseline.revision))
        }
        progress.pending?.let { pending ->
            val json = encodeDriveJournalFile(pending.entry).put("uploadId", pending.fileId)
            pending.resolutionExpectedLocal?.let { json.put("expectedLocal", encodeDriveSettings(scope, it)) }
            put("pending", json)
        }
    }.toString()

internal fun decodeProgress(scope: DriveCinemaScope, text: String): DriveJournalProgress {
    val json = JSONObject(text)
    if (json.getString("drive") != scope.driveAccount || json.getString("account") != scope.appAccount ||
        json.getString("profile") != scope.profile) return DriveJournalProgress()
    val baseline = json.optJSONObject("baseline")?.let {
        val revision = it.getString("revision")
        require(revision.matches(REVISION_ID))
        DriveJournalBaseline(revision, readDriveSettings(it))
    }
    val pending = json.optJSONObject("pending")?.let {
        val fileId = it.getString("uploadId")
        require(fileId.matches(Regex("[a-zA-Z0-9_-]{1,256}")))
        DriveJournalPending(fileId, parseDriveJournalFile(it), it.optJSONObject("expectedLocal")?.let(::readDriveSettings))
    }
    return DriveJournalProgress(baseline, pending)
}

private val REVISION_ID = Regex("[a-f0-9]{64}")
