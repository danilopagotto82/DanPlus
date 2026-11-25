package com.nuvio.tv.ui.screens.player

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response

internal object BlurayPlaylistResolver {
    private const val MAX_METADATA_BYTES = 1 shl 20
    private const val MAX_TITLES = 64
    private const val MAX_PLAY_ITEMS = 128
    private val clipNumber = Regex("[0-9]{5}")
    private val playlistFile = Regex("([0-9]{5})\\.mpls", RegexOption.IGNORE_CASE)
    private val playlistLink = Regex("""href\s*=\s*["'](?:\./)?([0-9]{5}\.mpls)["']""", RegexOption.IGNORE_CASE)
    private val byteRange = Regex("""bytes ([0-9]+)-([0-9]+)/([0-9]+)""", RegexOption.IGNORE_CASE)

    data class Location(val root: HttpUrl, val explicitPlaylist: String?, val playlistDirectory: String) {
        fun resource(directory: String, file: String = ""): HttpUrl =
            checkNotNull(root.resolve("$directory/$file")).newBuilder()
                .encodedQuery(root.encodedQuery).build()
    }

    data class PlayItem(val clip: String, val stcId: Int, val inTicks: Long, val outTicks: Long)
    data class Title(val file: String, val items: List<PlayItem>) {
        val id: String get() = file.substringBefore('.')
        val durationMs: Long get() = items.sumOf { it.outTicks - it.inTicks } / 45
    }
    data class Disc(val location: Location, val titles: List<Title>, val initialTitle: Title?)
    data class Clip(val url: String, val startUs: Long, val endUs: Long, val clockStartTicks: Long)
    data class Clock(val startTicks: Long, val endTicks: Long)

    class DiscException(val reason: Reason) : IOException()
    enum class Reason { DIRECTORY, PLAYLIST, CLIP_INFO, RANGE, TIMEOUT }

    private val metadataClient by lazy {
        PlayerPlaybackNetworking.playbackHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun locate(url: String): Location? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        val path = parsed.pathSegments.dropLastWhile(String::isEmpty)
        val root = path.indexOfLast { it.equals("BDMV", ignoreCase = true) }
        if (root < 0) return null
        val tail = path.drop(root + 1)
        val explicit = tail.getOrNull(1)?.takeIf {
            tail.size == 2 && tail[0].equals("PLAYLIST", ignoreCase = true) && playlistFile.matches(it)
        }
        if (explicit == null && tail.isNotEmpty() &&
            !(tail.size == 1 && (tail[0].equals("index.bdmv", true) || tail[0].equals("PLAYLIST", true)))
        ) return null
        val rootPath = parsed.encodedPathSegments.take(root + 1).joinToString("/", prefix = "/", postfix = "/")
        val playlistDirectory = tail.firstOrNull()?.takeIf { it.equals("PLAYLIST", true) } ?: "PLAYLIST"
        return Location(parsed.newBuilder().encodedPath(rootPath).fragment(null).build(), explicit, playlistDirectory)
    }

    suspend fun resolve(location: Location, headers: Map<String, String>): Disc {
        return withTimeoutOrNull(30_000) {
            val explicit = location.explicitPlaylist?.let { file ->
                readTitle(location, file, headers) ?: throw DiscException(Reason.PLAYLIST)
            }
            if (explicit == null) {
                val (titles, candidateCount) = discoverTitles(location, headers)
                if (titles.isEmpty()) throw DiscException(Reason.PLAYLIST)
                Disc(location, titles, initialTitle(titles, requireSelection = candidateCount > 1))
            } else {
                // An explicit feature can play even when the server hides the directory or the extras are slow.
                val extras = try {
                    withTimeoutOrNull(8_000) { discoverTitles(location, headers, explicit.id).first }.orEmpty()
                } catch (_: IOException) {
                    emptyList()
                }
                Disc(location, listOf(explicit) + extras, explicit)
            }
        } ?: throw DiscException(Reason.TIMEOUT)
    }

    private fun initialTitle(titles: List<Title>, requireSelection: Boolean): Title? =
        titles.singleOrNull()?.takeUnless { requireSelection }

    private suspend fun discoverTitles(
        location: Location,
        headers: Map<String, String>,
        excludedId: String? = null
    ): Pair<List<Title>, Int> {
        val listing = fetchBytes(location.resource(location.playlistDirectory), headers)
            ?: throw DiscException(Reason.DIRECTORY)
        val files = playlistNames(listing.toString(Charsets.UTF_8))
            .distinctBy { it.substringBefore('.') }
            .filter { it.substringBefore('.') != excludedId }
        if (files.size > MAX_TITLES) throw DiscException(Reason.DIRECTORY)
        return files.mapNotNull { file -> readTitle(location, file, headers) } to files.size
    }

    private suspend fun readTitle(location: Location, file: String, headers: Map<String, String>): Title? =
        fetchBytes(location.resource(location.playlistDirectory, file), headers)?.let { parsePlaylist(file, it) }

    suspend fun resolveClips(location: Location, title: Title, headers: Map<String, String>): List<Clip> {
        return withTimeoutOrNull(30_000) {
            val clipInfo = mutableMapOf<String, ByteArray>()
            val urls = mutableMapOf<String, String>()
            title.items.map { item ->
                val info = clipInfo[item.clip] ?: (
                    fetchBytes(location.resource("CLIPINF", "${item.clip}.clpi"), headers)
                        ?: fetchBytes(location.resource("CLIPINF", "${item.clip}.CLPI"), headers)
                        ?: throw DiscException(Reason.CLIP_INFO)
                    ).also { clipInfo[item.clip] = it }
                val clock = parseClipClock(info, item.stcId) ?: throw DiscException(Reason.CLIP_INFO)
                val interval = clipInterval(item, clock) ?: throw DiscException(Reason.CLIP_INFO)
                val clipUrl = urls[item.clip] ?: findRangeClip(location, item.clip, headers)
                    .also { urls[item.clip] = it }
                Clip(clipUrl, interval.first, interval.second, clock.startTicks)
            }
        } ?: throw DiscException(Reason.TIMEOUT)
    }

    fun clipInterval(item: PlayItem, clock: Clock): Pair<Long, Long>? {
        if (item.inTicks < clock.startTicks || item.outTicks <= item.inTicks ||
            item.outTicks > clock.endTicks
        ) return null
        return (item.inTicks - clock.startTicks) * 1_000 / 45 to
            (item.outTicks - clock.startTicks) * 1_000 / 45
    }

    fun playlistNames(listing: String): List<String> =
        playlistLink.findAll(listing).map { it.groupValues[1] }.distinct().take(MAX_TITLES + 1).toList()

    fun parsePlaylist(file: String, bytes: ByteArray): Title? {
        if (!playlistFile.matches(file) || bytes.size < 40 || bytes.ascii(0, 4) != "MPLS") return null
        val address = bytes.u32(8)
        if (address !in 40L..(bytes.size - 10L)) return null
        val base = address.toInt()
        val end = address + 4 + bytes.u32(base)
        if (end !in (address + 10)..bytes.size.toLong()) return null
        val count = bytes.u16(base + 6)
        if (count !in 1..MAX_PLAY_ITEMS) return null
        var cursor = base + 10
        val items = ArrayList<PlayItem>(count)
        repeat(count) {
            if (cursor + 2 > end) return null
            val length = bytes.u16(cursor)
            val item = cursor + 2
            if (length < 32 || item + length > end) return null
            val id = bytes.ascii(item, 5)
            if (!clipNumber.matches(id) || bytes.ascii(item + 5, 4) != "M2TS") return null
            // Multi-angle timelines need an explicit angle policy.
            if (bytes.u8(item + 10) and 0x10 != 0) return null
            val start = bytes.u32(item + 12)
            val stop = bytes.u32(item + 16)
            if (stop <= start) return null
            items += PlayItem(id, bytes.u8(item + 11), start, stop)
            cursor = item + length
        }
        return Title(file, items)
    }

    // SequenceInfo clock origin for one linear STC beginning at the file's first source packet.
    fun parseClipClock(bytes: ByteArray, stcId: Int): Clock? {
        if (bytes.size < 40 || bytes.ascii(0, 4) != "HDMV") return null
        val address = bytes.u32(8)
        if (address !in 40L..(bytes.size - 12L)) return null
        val at = address.toInt()
        val end = address + 4 + bytes.u32(at)
        if (end !in (address + 26)..bytes.size.toLong()) return null
        if (bytes.u8(at + 5) != 1 || bytes.u32(at + 6) != 0L ||
            bytes.u8(at + 10) != 1 || bytes.u8(at + 11) != stcId || bytes.u32(at + 14) != 0L
        ) return null
        val start = bytes.u32(at + 18)
        val stop = bytes.u32(at + 22)
        return if (stop > start) Clock(start, stop) else null
    }

    fun supportsRange(code: Int, contentRange: String?, requiredStart: Long = 1L, requiredEnd: Long? = 1L): Boolean {
        if (code != 206 || contentRange == null) return false
        val range = byteRange.matchEntire(contentRange.trim()) ?: return false
        val start = range.groupValues[1].toLongOrNull() ?: return false
        val end = range.groupValues[2].toLongOrNull() ?: return false
        val total = range.groupValues[3].toLongOrNull() ?: return false
        return start == requiredStart && end in start until total && (requiredEnd == null || end <= requiredEnd)
    }

    private suspend fun findRangeClip(location: Location, clip: String, headers: Map<String, String>): String {
        for (extension in listOf("m2ts", "M2TS")) {
            val url = location.resource("STREAM", "$clip.$extension")
            val request = request(url, headers).header("Range", "bytes=1-1").build()
            val status = execute(request) { response ->
                when {
                    supportsRange(response.code, response.header("Content-Range")) -> 206
                    response.code == 404 -> 404
                    else -> throw DiscException(Reason.RANGE)
                }
            }
            if (status == 206) return url.toString()
        }
        throw DiscException(Reason.RANGE)
    }

    private fun request(url: HttpUrl, headers: Map<String, String>): Request.Builder =
        Request.Builder().url(url).apply {
            PlayerMediaSourceFactory.sanitizeHeaders(headers).forEach { (name, value) -> header(name, value) }
            header("Accept-Encoding", "identity")
        }

    private suspend fun fetchBytes(url: HttpUrl, headers: Map<String, String>): ByteArray? =
        execute(request(url, headers).build()) { response ->
            if (!response.isSuccessful) return@execute null
            val body = response.body ?: return@execute null
            if (body.contentLength() > MAX_METADATA_BYTES) return@execute null
            val source = body.source()
            if (source.request(MAX_METADATA_BYTES + 1L)) return@execute null
            source.readByteArray()
        }

    private suspend fun <T> execute(request: Request, read: (Response) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val call = metadataClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use(read) }
                    continuation.resumeWith(result)
                }
            })
        }

    private fun ByteArray.ascii(start: Int, size: Int) = String(this, start, size, Charsets.US_ASCII)
    private fun ByteArray.u8(at: Int) = this[at].toInt() and 0xff
    private fun ByteArray.u16(at: Int) = (u8(at) shl 8) or u8(at + 1)
    private fun ByteArray.u32(at: Int): Long = (u16(at).toLong() shl 16) or u16(at + 2).toLong()
}
