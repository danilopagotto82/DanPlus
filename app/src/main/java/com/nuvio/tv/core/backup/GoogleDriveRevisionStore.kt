package com.nuvio.tv.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** An isolated Drive v3 namespace. The caller binds it to a verified Drive account and DanPlus profile. */
internal class GoogleDriveRevisionStore(
    private val http: OkHttpClient,
    private val namespace: String,
    private val token: suspend () -> String?,
    private val isCurrent: () -> Boolean
) : DriveJournalStore {
    init {
        require(namespace.matches(DIGEST))
    }

    /** Persist this ID alongside the pending revision before calling append, including across retries. */
    override suspend fun reserveUploadId(): String = withContext(Dispatchers.IO) {
        val body = get("files/generateIds?count=1&space=drive", authorization())
        JSONObject(body).getJSONArray("ids").getString(0).also { require(it.matches(FILE_ID)) }
    }

    override suspend fun readJournal(): DriveJournalState = withContext(Dispatchers.IO) {
        val bearer = authorization()
        val files = mutableMapOf<String, DriveJournalEntry>()
        var page: String? = null
        var total = 0
        val pages = hashSetOf<String>()
        do {
            val url = "${API}files".toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "drive")
                .addQueryParameter("q", "trashed = false and appProperties has { key='danplusJournal' and value='$namespace' }")
                .addQueryParameter("fields", "nextPageToken,files(id,name)")
                .addQueryParameter("pageSize", "100")
                .apply { page?.let { addQueryParameter("pageToken", it) } }
                .build()
            val response = JSONObject(read(Request.Builder().url(url).authorize(bearer).build(), MAX_LIST_BYTES))
            val items = response.getJSONArray("files")
            total += items.length()
            if (total > MAX_DRIVE_REVISIONS) return@withContext DriveJournalState.CapacityExceeded
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val name = item.getString("name")
                if (!name.startsWith(NAME_PREFIX)) return@withContext DriveJournalState.Invalid
                val revision = name.removePrefix(NAME_PREFIX)
                if (!revision.matches(DIGEST)) return@withContext DriveJournalState.Invalid
                val entry = fetch(item.getString("id"), bearer)
                if (entry.id != revision) return@withContext DriveJournalState.Invalid
                files[revision] = entry
            }
            page = response.optString("nextPageToken").ifBlank { null }
            if (page != null && (!pages.add(page) || pages.size > MAX_PAGES)) return@withContext DriveJournalState.Invalid
        } while (page != null)
        check(isCurrent())
        inspectDriveJournal(files)
    }

    override suspend fun append(fileId: String, entry: DriveJournalEntry) = withContext(Dispatchers.IO) {
        require(fileId.matches(FILE_ID))
        require(entry.sealed.data.length <= MAX_CIPHERTEXT && entry.parents.all { it.matches(DIGEST) })
        val bearer = authorization()
        val metadata = JSONObject().put("id", fileId).put("name", NAME_PREFIX + entry.id)
            .put("mimeType", "application/json").put("appProperties", JSONObject().put("danplusJournal", namespace))
        val payload = encodeDriveJournalFile(entry)
        val multipart = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody(JSON)).addPart(payload.toString().toRequestBody(JSON))
            .build()
        val request = Request.Builder().url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .authorize(bearer).post(multipart).build()
        check(isCurrent())
        val alreadyExists = http.newCall(request).execute().use {
            if (it.code == 409) true
            else {
                check(it.isSuccessful && JSONObject(body(it, MAX_ENTRY_BYTES)).getString("id") == fileId)
                false
            }
        }
        if (alreadyExists) check(fetch(fileId, bearer) == entry)
        check(isCurrent())
    }

    private fun fetch(fileId: String, bearer: String): DriveJournalEntry {
        require(fileId.matches(FILE_ID))
        return parseDriveJournalFile(JSONObject(get("files/$fileId?alt=media", bearer)))
    }

    private fun get(path: String, bearer: String): String =
        read(Request.Builder().url(API + path).authorize(bearer).build(), MAX_ENTRY_BYTES)

    private fun read(request: Request, max: Long): String {
        check(isCurrent())
        return http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Drive journal HTTP ${response.code}" }
            body(response, max)
        }
    }

    private fun body(response: okhttp3.Response, max: Long): String {
        val source = checkNotNull(response.body).source()
        check(!source.request(max + 1)) { "Drive journal response oversized" }
        return source.readUtf8()
    }

    private suspend fun authorization(): String {
        check(isCurrent())
        return token()?.takeIf(String::isNotBlank)?.also { check(isCurrent()) }
            ?: error("Drive authorization unavailable")
    }

    private fun Request.Builder.authorize(bearer: String) = header("Authorization", "Bearer $bearer")

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3/"
        const val NAME_PREFIX = "danplus-settings-"
        const val MAX_CIPHERTEXT = 8192
        const val MAX_ENTRY_BYTES = 16_384L
        const val MAX_LIST_BYTES = 64 * 1024L
        const val MAX_PAGES = 16
        val FILE_ID = Regex("[a-zA-Z0-9_-]{1,256}")
        val DIGEST = Regex("[a-f0-9]{64}")
        val JSON = "application/json".toMediaType()
    }
}
