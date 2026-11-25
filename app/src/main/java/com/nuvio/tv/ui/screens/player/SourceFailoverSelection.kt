package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.core.debrid.DirectDebridStreamFilter
import com.nuvio.tv.core.player.thumbnail.CopyChoice
import com.nuvio.tv.domain.model.DebridStreamPreferences
import com.nuvio.tv.domain.model.DebridStreamResolution
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Which list entry the player may switch to on its own when a source fails.
 *
 * Entries with a playable http(s) link qualify, media server entries (their link is made when they
 * are opened), and debrid entries the service reports as cached: those are added only if cached, so
 * no download starts. Plain torrents and magnets are skipped (they would play peer to peer), as are
 * uncached entries, external links (they open a browser) and Usenet entries (they fetch the release).
 */
internal object SourceFailoverSelection {
    private val defaultFacts = DebridStreamPreferences()

    /**
     * The server version, the link, or for a debrid entry not prepared yet its torrent and file,
     * so a failed entry is not tried twice.
     */
    fun failoverKey(stream: Stream): String? =
        stream.serverTarget?.key()
            ?: stream.getStreamUrl()?.takeIf { it.isNotBlank() }
            ?: stream.getEffectiveInfoHash()?.let { "$it:${stream.getEffectiveFileIdx() ?: -1}" }

    /** Use the same HTTP URL identity before and after resolution, keeping signed queries intact. */
    fun addressKey(url: String?): String? = url?.trim()
        ?.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
        ?.toHttpUrlOrNull()?.newBuilder()
        ?.username("")?.password("")?.fragment(null)?.build()?.toString()

    fun debridKey(stream: Stream): String? {
        val resolve = stream.clientResolve?.takeIf { stream.isDirectDebrid() } ?: return null
        val hash = stream.getEffectiveInfoHash()?.lowercase() ?: return null
        val fileIndex = stream.getEffectiveFileIdx()
        val file = fileIndex?.toString() ?: resolve.filename?.takeIf(String::isNotBlank) ?: return null
        return "debrid:${resolve.service?.lowercase()}:${resolve.serviceIndex}:$hash:$file"
    }

    fun isAutoFailoverPlayable(stream: Stream, season: Int?, episode: Int?): Boolean {
        if (stream.isExternal()) return false
        if (isUsenet(stream)) return false
        val resolve = stream.clientResolve
        if (resolve != null) {
            if (season != null && resolve.season != null && resolve.season != season) return false
            if (episode != null && resolve.episode != null && resolve.episode != episode) return false
            val parsed = resolve.stream?.raw?.parsed
            val parsedSeasons = parsed?.seasons.orEmpty()
            val parsedEpisodes = parsed?.episodes.orEmpty()
            if (season != null && parsedSeasons.isNotEmpty() && season !in parsedSeasons) return false
            if (episode != null && parsedEpisodes.isNotEmpty() && episode !in parsedEpisodes) return false
        }
        if (stream.serverTarget != null) return true
        when (stream.debridCacheStatus?.state) {
            null, StreamDebridCacheState.CACHED -> Unit
            StreamDebridCacheState.CHECKING,
            StreamDebridCacheState.NOT_CACHED,
            StreamDebridCacheState.UNKNOWN -> return false
        }
        val url = stream.getStreamUrl()?.takeIf(String::isNotBlank)
        if (url != null) return addressKey(url) != null
        return stream.isDirectDebrid()
    }

    /** A Usenet entry, by its add-on's marking, its link or its name (an NZB indexer or "Usenet"). */
    fun isUsenet(stream: Stream): Boolean {
        stream.clientResolve?.let { if (CopyChoice.usenetMarked(it.type, it.service)) return true }
        if (SlowStartServer.isUsenet(stream.getStreamUrl())) return true
        return SlowStartServer.USENET_NAME.containsMatchIn(stream.name?.lineSequence()?.firstOrNull().orEmpty())
    }

    /**
     * Scan after the current entry, then wrap once. A partial list can grow between calls.
     * The request identity and attempted entries are checked on every scan.
     */
    fun selectNext(
        streams: List<Stream>,
        currentIndex: Int,
        listRequestKey: String?,
        currentRequestKey: String?,
        deadUrls: Set<String>,
        season: Int?,
        episode: Int?,
        addonName: String? = null,
        attemptedKeys: Set<String> = emptySet(),
        knownBytesPerSecond: (Stream) -> Long? = { null },
        matchesProfile: (Stream) -> Boolean = { true }
    ): Stream? {
        if (currentRequestKey == null || listRequestKey != currentRequestKey) return null
        val playingIndex = currentIndex.takeIf { it in streams.indices } ?: -1
        val playingAddress = addressKey(streams.getOrNull(playingIndex)?.getStreamUrl())
        val deadAddresses = deadUrls.mapNotNullTo(HashSet(), ::addressKey)
        var preferred: Stream? = null
        var preferredSpeed = 0L
        for (offset in streams.indices) {
            val index = (playingIndex + 1 + offset) % streams.size
            if (index == playingIndex) continue
            val candidate = streams[index]
            if (addonName != null && candidate.addonName != addonName) continue
            if (!isAutoFailoverPlayable(candidate, season, episode) || !matchesProfile(candidate)) continue
            val key = failoverKey(candidate) ?: continue
            val address = addressKey(candidate.getStreamUrl())
            val triedDebridFile = debridKey(candidate)?.let { it in attemptedKeys } == true
            if (key in deadUrls || key in attemptedKeys ||
                (address != null && (address == playingAddress || address in deadAddresses)) ||
                triedDebridFile
            ) continue
            val speed = knownBytesPerSecond(candidate)?.takeIf { it > 0L } ?: return preferred ?: candidate
            val first = preferred
            if (first != null && !samePlaybackPreferences(first, candidate)) return first
            if (speed > preferredSpeed) {
                preferred = candidate
                preferredSpeed = speed
            }
        }
        return preferred
    }

    /** Speed only breaks ties inside a run of measured, equivalent choices. */
    private fun samePlaybackPreferences(first: Stream, second: Stream): Boolean {
        if (first.behaviorHints?.bingeGroup != second.behaviorHints?.bingeGroup) return false
        val left = DirectDebridStreamFilter.factsFor(first, defaultFacts)
        val right = DirectDebridStreamFilter.factsFor(second, defaultFacts)
        return left.resolution != DebridStreamResolution.UNKNOWN && left.copy(size = null) == right.copy(size = null)
    }

    fun startPositionMs(explicitSeekMs: Long?, playingPositionMs: Long?, carriedPositionMs: Long?, durationMs: Long?): Long? {
        val position = explicitSeekMs ?: playingPositionMs ?: carriedPositionMs ?: return null
        val lastPosition = durationMs?.takeIf { it > 0L }?.minus(1) ?: Long.MAX_VALUE
        return position.coerceIn(0L, lastPosition)
    }
}
