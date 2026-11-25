package com.nuvio.tv.data.trailer

import android.util.Log
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.local.TmdbSettingsDataStore
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbVideoResult
import com.nuvio.tv.data.remote.api.TrailerApi
import com.nuvio.tv.data.local.TrailerSettingsDataStore
import com.nuvio.tv.data.local.TrailerSource
import java.time.Clock
import java.net.URI
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val TAG = "TrailerService"
private const val TMDB_TRAILER_FALLBACK_LANGUAGE = "en-US"
private const val TMDB_TRAILER_MAX_CANDIDATES = 3
private const val TRAILER_BRAZILIAN_LOCALE = "pt-BR"
private const val TRAILER_SELECTION_POLICY = "pt_br_evidence_v1"
private const val YOUTUBE_REFUSAL_REST_MS = 10 * 60_000L

@Singleton
class TrailerService(
    private val trailerApi: TrailerApi,
    private val tmdbApi: TmdbApi,
    private val inAppYouTubeExtractor: InAppYouTubeExtractor,
    private val tmdbSettingsDataStore: TmdbSettingsDataStore,
    private val tmdbService: TmdbService,
    private val imdbTrailerResolver: ImdbTrailerResolver,
    private val trailerSettingsDataStore: TrailerSettingsDataStore,
    private val clock: Clock,
    private val mediaCache: TrailerMediaCache? = null,
    private val backendConfigured: Boolean = BuildConfig.TRAILER_API_URL.isNotBlank()
) {
    @Inject
    constructor(
        trailerApi: TrailerApi,
        tmdbApi: TmdbApi,
        inAppYouTubeExtractor: InAppYouTubeExtractor,
        tmdbSettingsDataStore: TmdbSettingsDataStore,
        tmdbService: TmdbService,
        imdbTrailerResolver: ImdbTrailerResolver,
        trailerSettingsDataStore: TrailerSettingsDataStore,
        mediaCache: TrailerMediaCache? = null
    ) : this(
        trailerApi = trailerApi,
        tmdbApi = tmdbApi,
        inAppYouTubeExtractor = inAppYouTubeExtractor,
        tmdbSettingsDataStore = tmdbSettingsDataStore,
        tmdbService = tmdbService,
        imdbTrailerResolver = imdbTrailerResolver,
        trailerSettingsDataStore = trailerSettingsDataStore,
        clock = Clock.systemUTC(),
        mediaCache = mediaCache
    )

    private val cache = TrailerSourceCache(clock)
    @Volatile private var youTubeRefusedUntilMs = 0L
    private val lookupLocks = Array(32) { Mutex() }
    // youtubeVideoId -> resolved playback source (success-only)
    private val youtubeSourceCache = TrailerSourceCache(clock)
    // Extractions running right now, by the same key as youtubeSourceCache
    private val inFlightYoutubeSources = ConcurrentHashMap<String, CompletableDeferred<TrailerPlaybackSource?>>()

    /**
     * True while YouTube is refusing requests. A missing trailer then says nothing about the
     * title, so callers should not remember it as "no trailer".
     */
    fun isRateLimited(): Boolean = inAppYouTubeExtractor.isRateLimited()

    /** True for ten minutes after YouTube asked this device to sign in; it is not asked meanwhile. */
    fun isYouTubeResting(): Boolean = clock.millis() < youTubeRefusedUntilMs

    /**
     * Search for a trailer by title, year, tmdbId, and type.
     * Returns the trailer playback source (video URL + optional separate audio URL) or null.
     */
    suspend fun getTrailerPlaybackSource(
        title: String,
        year: String? = null,
        tmdbId: String? = null,
        type: String? = null,
        ignoreUseTrailersGate: Boolean = false
    ): TrailerPlaybackSource? = lookupTrailer(title, year, tmdbId, type, ignoreUseTrailersGate).source

    /**
     * Same lookup as [getTrailerPlaybackSource], and also says whether a miss is definite
     * (nothing to try) or may have been a timeout, rate limit or failed request.
     */
    suspend fun lookupTrailer(
        title: String,
        year: String? = null,
        tmdbId: String? = null,
        type: String? = null,
        ignoreUseTrailersGate: Boolean = false
    ): TrailerLookupResult = withContext(Dispatchers.IO) {
        val settings = try { tmdbSettingsDataStore.settings.first() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        val provider = try { trailerSettingsDataStore.settings.first().source }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { TrailerSource.YOUTUBE }
        val useYouTube = settings?.useTrailers == true || ignoreUseTrailersGate
        if (!useYouTube && provider != TrailerSource.IMDB) {
            return@withContext TrailerLookupResult.DEFINITE_MISS.copy(allowItemIds = false)
        }

        val rawId = tmdbId?.removePrefix("tmdb:")?.removePrefix("movie:")?.removePrefix("series:")
            ?.substringBefore(':')?.substringBefore('/')?.trim()
        val suppliedImdb = rawId?.takeIf { Regex("tt\\d+").matches(it) }
        val numericId = rawId?.toIntOrNull()?.toString()
        val language = normalizeTmdbTrailerLanguage(settings?.language)
        val mediaType = normalizeTmdbMediaType(type)
        val identity = suppliedImdb ?: numericId ?: "$title|$year"
        val key = "$identity|$mediaType|$provider|$TRAILER_SELECTION_POLICY|$language|$useYouTube"
        lookupLocks[(key.hashCode() and Int.MAX_VALUE) % lookupLocks.size].withLock {
            cache.get(key)?.let { entry ->
                val source = entry.source
                if (source == null) return@withLock TrailerLookupResult.DEFINITE_MISS
                if (!TrailerPlaybackFailures.hasFailed(source.videoUrl)) return@withLock TrailerLookupResult(source)
                cache.invalidate(source.videoUrl)
                imdbTrailerResolver.invalidate(source.videoUrl)
            }
            try {
                suspend fun imdbForWork(): String? = suppliedImdb ?: numericId?.toInt()
                    ?.let { id -> mediaType?.let { tmdbService.tmdbToImdb(id, it) } }
                var imdbFailed = false
                if (provider == TrailerSource.IMDB) {
                    val imdb = imdbForWork()
                    if (imdb != null) {
                        val source = imdbTrailerResolver.resolve(imdb, type)
                        if (source != null && TrailerSourceExpiry.isUsable(source, clock.millis())) {
                            mediaCache?.initialize()
                            val fresh = source.copy(validUntilMs = minOf(clock.millis() + 30 * 60_000L, TrailerSourceExpiry.expiresAtMs(source)))
                            cache.put(key, fresh)
                            return@withLock TrailerLookupResult(fresh)
                        }
                        imdbFailed = true
                    }
                }
                if (!useYouTube) return@withLock TrailerLookupResult(null, definiteMiss = !imdbFailed, allowItemIds = false)
                val resolvedTmdb = numericId ?: suppliedImdb?.let { id ->
                    type?.takeIf { mediaType != null }?.let { tmdbService.ensureTmdbId(id, it) }
                }
                val youTubeFirst = provider != TrailerSource.IMDB
                val youTubeResting = isYouTubeResting()
                val lookup = if (youTubeResting) TrailerLookupResult(null, youTubeBlocked = true)
                else lookupTmdbTrailer(resolvedTmdb, mediaType, title, year, language)
                if (!youTubeResting) {
                    if (lookup.signInDemanded) youTubeRefusedUntilMs = clock.millis() + YOUTUBE_REFUSAL_REST_MS
                    else if (lookup.source != null) youTubeRefusedUntilMs = 0L
                }
                if (youTubeFirst && lookup.source == null &&
                    (lookup.youTubeBlocked || lookup.refused || lookup.definiteMiss)
                ) {
                    val standIn = try {
                        imdbForWork()?.let { imdbTrailerResolver.resolveFromDataService(it) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                    if (standIn != null && TrailerSourceExpiry.isUsable(standIn, clock.millis())) {
                        mediaCache?.initialize()
                        val fresh = standIn.copy(
                            validUntilMs = minOf(clock.millis() + YOUTUBE_REFUSAL_REST_MS, TrailerSourceExpiry.expiresAtMs(standIn))
                        )
                        cache.put(key, fresh, YOUTUBE_REFUSAL_REST_MS)
                        return@withLock TrailerLookupResult(fresh)
                    }
                }
                val ttl = if (provider == TrailerSource.IMDB) 30_000L else 30 * 60_000L
                val source = lookup.source?.let {
                    it.copy(validUntilMs = minOf(clock.millis() + ttl, TrailerSourceExpiry.expiresAtMs(it)))
                }
                val definiteMiss = lookup.definiteMiss && !imdbFailed
                val refused = lookup.refused && !imdbFailed
                // Retry the preferred provider soon after a YouTube fallback.
                if (source != null) cache.put(key, source, ttl)
                else if (resolvedTmdb != null && definiteMiss) cache.put(key, null, 30_000L)
                TrailerLookupResult(source, definiteMiss, refused)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Trailer lookup failed", error)
                TrailerLookupResult(null, definiteMiss = false)
            }
        }
    }

    suspend fun preload(source: TrailerPlaybackSource) { mediaCache?.preload(source) }

    /**
     * Search for a trailer and return its primary video URL for existing call sites.
     */
    suspend fun getTrailerUrl(
        title: String,
        year: String? = null,
        tmdbId: String? = null,
        type: String? = null
    ): String? {
        return getTrailerPlaybackSource(
            title = title,
            year = year,
            tmdbId = tmdbId,
            type = type
        )?.videoUrl
    }

    suspend fun getExternalTrailerUrl(
        tmdbId: String?,
        type: String?,
        preferBrazilian: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        // Parse the id first so an invalid/null tmdbId short-circuits without
        // touching the settings DataStore at all.
        val numericTmdbId = tmdbId?.toIntOrNull() ?: return@withContext null
        // Read settings once and use for both the "Disable Trailers" gate and
        // the trailer language. See #1647 for the gate rationale.
        val tmdbSettings = tmdbSettingsDataStore.settings.first()
        if (!tmdbSettings.useTrailers) return@withContext null
        val mediaType = normalizeTmdbMediaType(type)
        val tmdbLanguage = normalizeTmdbTrailerLanguage(tmdbSettings.language)
        rankedTmdbVideos(numericTmdbId, mediaType, tmdbLanguage, preferBrazilian).orEmpty()
            .firstOrNull()
            ?.key
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { "https://www.youtube.com/watch?v=$it" }
    }

    private suspend fun lookupTmdbTrailer(
        tmdbId: String?,
        mediaType: String?,
        title: String,
        year: String?,
        tmdbLanguage: String
    ): TrailerLookupResult = withContext(Dispatchers.IO) {
        val numericTmdbId = tmdbId?.toIntOrNull()
            ?: return@withContext TrailerLookupResult(null, definiteMiss = false)
        Log.d(
            TAG,
            "TMDB trailer lookup start: tmdbId=$numericTmdbId type=${mediaType ?: "unknown"} language=$tmdbLanguage"
        )

        val candidates = rankedTmdbVideos(
            numericTmdbId, mediaType, tmdbLanguage, preferBrazilian = true, attemptLimit = TMDB_TRAILER_MAX_CANDIDATES
        )
            ?: return@withContext TrailerLookupResult(null, definiteMiss = false)
        Log.d(TAG, "TMDB candidate count: ${candidates.size}")

        var tried = 0
        var refused = 0
        var blocked = false
        var signIn = false
        for (candidate in candidates) {
            if (inAppYouTubeExtractor.isRateLimited()) {
                Log.w(TAG, "YouTube is rate limiting, not trying the remaining TMDB candidates")
                blocked = true
                break
            }
            val key = candidate.key?.trim().orEmpty()
            if (key.isBlank()) continue
            Log.d(
                TAG,
                "TMDB selected candidate: type=${candidate.type.orEmpty()} " +
                    "official=${candidate.official == true} key=${obfuscateYoutubeKey(key)}"
            )

            val youtubeUrl = "https://www.youtube.com/watch?v=$key"
            val source = getTrailerPlaybackSourceFromYouTubeUrl(
                youtubeUrl = youtubeUrl,
                title = title,
                year = year
            )
            if (source != null) {
                return@withContext TrailerLookupResult(source)
            }
            tried++
            val reason = inAppYouTubeExtractor.unplayableReason(youtubeUrl)
            if (reason != null) refused++

            Log.d(
                TAG,
                "TMDB candidate extraction failed, trying next: key=${obfuscateYoutubeKey(key)}"
            )
            if (reason == YouTubeUnplayableReason.SIGN_IN_REQUIRED || inAppYouTubeExtractor.isRateLimited()) {
                blocked = true
                signIn = reason == YouTubeUnplayableReason.SIGN_IN_REQUIRED
                break
            }
        }

        TrailerLookupResult(
            null,
            definiteMiss = candidates.isEmpty(),
            refused = tried > 0 && refused == tried && !inAppYouTubeExtractor.isRateLimited(),
            youTubeBlocked = blocked,
            signInDemanded = signIn
        )
    }

    /**
     * Resolve a YouTube trailer URL to a playback source (prefers in-app extraction).
     */
    suspend fun getTrailerPlaybackSourceFromYouTubeUrl(
        youtubeUrl: String,
        title: String? = null,
        year: String? = null
    ): TrailerPlaybackSource? = withContext(Dispatchers.IO) {
        try {
            val youtubeKey = youTubeVideoIdOf(youtubeUrl) ?: return@withContext null
            cachedYouTubeSource(youtubeKey)?.let { cached ->
                Log.d(TAG, "YouTube cache hit for key=${obfuscateYoutubeKey(youtubeKey)}")
                return@withContext cached
            }
            resolveYouTubeSourceOnce(youtubeKey) {
                // Re-check: another caller may have just finished this video.
                cachedYouTubeSource(youtubeKey) ?: resolveYouTubeSource(youtubeUrl, youtubeKey, title, year)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trailer from YouTube: ${e.message}", e)
            null
        }
    }

    /**
     * The home hero, the focused card and the detail screen can all ask for the same video at
     * once. Only one of them runs [resolve]; the others wait for its result. The work stays in
     * the first caller's coroutine, so cancelling it still stops the requests; a waiting caller
     * then takes the work over.
     */
    private suspend fun resolveYouTubeSourceOnce(
        youtubeKey: String,
        resolve: suspend () -> TrailerPlaybackSource?
    ): TrailerPlaybackSource? {
        while (true) {
            val pending = CompletableDeferred<TrailerPlaybackSource?>()
            val inFlight = inFlightYoutubeSources.putIfAbsent(youtubeKey, pending)
            if (inFlight == null) {
                try {
                    return resolve().also { pending.complete(it) }
                } catch (e: Throwable) {
                    pending.completeExceptionally(e)
                    throw e
                } finally {
                    inFlightYoutubeSources.remove(youtubeKey, pending)
                }
            }
            try {
                return inFlight.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Rethrows if this caller was cancelled; otherwise the one doing the work was.
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private suspend fun resolveYouTubeSource(
        youtubeUrl: String,
        youtubeKey: String,
        title: String?,
        year: String?
    ): TrailerPlaybackSource? {
        Log.d(TAG, "Attempting in-app YouTube extraction for ${summarizeUrl(youtubeUrl)}")
        val localSource = inAppYouTubeExtractor.extractPlaybackSource(youtubeUrl)
        if (localSource != null) {
            if (TrailerPlaybackFailures.hasFailed(localSource.videoUrl)) return null
            rememberYouTubeSource(youtubeKey, localSource)
            Log.d(
                TAG,
                "Using in-app YouTube source for ${summarizeUrl(youtubeUrl)} " +
                    "(audioPresent=${!localSource.audioUrl.isNullOrBlank()})"
            )
            return localSource
        }

        // A provider refusal or rate limit applies to the clip across our playback routes.
        if (!backendConfigured || inAppYouTubeExtractor.unplayableReason(youtubeUrl) != null ||
            inAppYouTubeExtractor.isRateLimited()
        ) return null

        // The configured resolver is available for failures without a provider refusal.
        Log.w(TAG, "In-app extraction failed, falling back to backend resolver for ${summarizeUrl(youtubeUrl)}")
        val response = trailerApi.getTrailer(youtubeUrl = youtubeUrl, title = title, year = year)
        if (!response.isSuccessful) {
            Log.w(TAG, "Backend trailer fallback failed (${response.code()}) for ${summarizeUrl(youtubeUrl)}")
            return null
        }

        val fallbackUrl = response.body()?.url ?: return null
        if (!isValidUrl(fallbackUrl) || TrailerPlaybackFailures.hasFailed(fallbackUrl)) return null

        val fallbackSource = TrailerPlaybackSource(videoUrl = fallbackUrl)
        rememberYouTubeSource(youtubeKey, fallbackSource)
        Log.d(TAG, "Using backend fallback source for ${summarizeUrl(youtubeUrl)}")
        return fallbackSource
    }

    /** Same as [getTrailerPlaybackSourceFromYouTubeUrl], and on a miss says why when YouTube refused the video. */
    suspend fun lookupYouTubeTrailer(
        youtubeUrl: String,
        title: String? = null,
        year: String? = null
    ): YouTubeTrailerLookup {
        val source = getTrailerPlaybackSourceFromYouTubeUrl(youtubeUrl, title, year)
        val reason = if (source == null) inAppYouTubeExtractor.unplayableReason(youtubeUrl) else null
        if (reason == YouTubeUnplayableReason.SIGN_IN_REQUIRED) {
            youTubeRefusedUntilMs = clock.millis() + YOUTUBE_REFUSAL_REST_MS
        }
        return YouTubeTrailerLookup(source, reason)
    }

    /**
     * Compatibility method for existing callers expecting a single URL.
     */
    suspend fun getTrailerFromYouTubeUrl(
        youtubeUrl: String,
        title: String? = null,
        year: String? = null
    ): String? {
        return getTrailerPlaybackSourceFromYouTubeUrl(
            youtubeUrl = youtubeUrl,
            title = title,
            year = year
        )?.videoUrl
    }

    private suspend fun rankedTmdbVideos(
        id: Int,
        type: String?,
        language: String,
        preferBrazilian: Boolean,
        attemptLimit: Int? = null
    ): List<TmdbVideoResult>? {
        if (!preferBrazilian) {
            return fetchTmdbVideos(id, type, language)?.let { rankTmdbVideoCandidates(it, language, attemptLimit = attemptLimit) }
        }
        // A numeric TMDB ID can exist in both movie and TV namespaces.
        if (type == null) return null

        val brazilian = fetchTmdbVideosOnce(id, type, TRAILER_BRAZILIAN_LOCALE)
        val originalLanguage = try {
            val response = when (type) {
                "movie" -> tmdbApi.getMovieDetails(id, tmdbService.apiKey())
                else -> tmdbApi.getTvDetails(id, tmdbService.apiKey())
            }
            response.body()?.originalLanguage?.takeIf { response.isSuccessful && it.isNotBlank() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val fallbacks = linkedSetOf<String>()
        originalLanguage?.let { fallbacks += normalizeTmdbTrailerLanguage(it) }
        fallbacks += language
        fallbacks += TMDB_TRAILER_FALLBACK_LANGUAGE
        fallbacks.remove(TRAILER_BRAZILIAN_LOCALE)
        val results = mutableListOf<TmdbVideoResult>()
        results.addAll(brazilian.orEmpty())
        var complete = brazilian != null && originalLanguage != null
        for (fallback in fallbacks) {
            val videos = fetchTmdbVideosOnce(id, type, fallback)
            if (videos == null) complete = false
            results.addAll(videos.orEmpty())
            // Once the original and a usable legacy fallback are represented, avoid another locale fetch.
            if (videos?.any { it.isPlayableTrailerCandidate() } == true &&
                (fallback == language || fallback == TMDB_TRAILER_FALLBACK_LANGUAGE)
            ) break
        }
        return rankTmdbVideoCandidates(results, language, originalLanguage, preferBrazilian = true, attemptLimit = attemptLimit)
            .takeIf { it.isNotEmpty() || complete }
    }

    private suspend fun fetchTmdbVideosOnce(id: Int, type: String?, language: String) = when (type) {
        "movie" -> fetchTmdbMovieVideosOnce(id, language)
        "tv" -> fetchTmdbTvVideosOnce(id, language)
        else -> null
    }

    /** Null when a request failed, so an empty list always means TMDB has no videos. */
    private suspend fun fetchTmdbVideos(
        tmdbId: Int,
        mediaType: String?,
        language: String
    ): List<TmdbVideoResult>? = when (mediaType) {
        "movie" -> fetchTmdbMovieVideos(tmdbId, language)
        "tv" -> fetchTmdbTvVideos(tmdbId, language)
        else -> {
            val movie = fetchTmdbMovieVideos(tmdbId, language)
            val tv = fetchTmdbTvVideos(tmdbId, language)
            if ((movie == null || tv == null) && movie.isNullOrEmpty() && tv.isNullOrEmpty()) {
                null
            } else {
                movie.orEmpty() + tv.orEmpty()
            }
        }
    }

    private suspend fun fetchTmdbMovieVideos(tmdbId: Int, preferredLanguage: String): List<TmdbVideoResult>? {
        val localized = fetchTmdbMovieVideosOnce(tmdbId, preferredLanguage)
        if (!localized.isNullOrEmpty() || preferredLanguage.equals(TMDB_TRAILER_FALLBACK_LANGUAGE, ignoreCase = true)) {
            return localized
        }
        Log.d(TAG, "TMDB movie videos localized miss for $tmdbId ($preferredLanguage), retrying $TMDB_TRAILER_FALLBACK_LANGUAGE")
        return mergeLanguageFallback(localized, fetchTmdbMovieVideosOnce(tmdbId, TMDB_TRAILER_FALLBACK_LANGUAGE))
    }

    private suspend fun fetchTmdbTvVideos(tmdbId: Int, preferredLanguage: String): List<TmdbVideoResult>? {
        val localized = fetchTmdbTvVideosOnce(tmdbId, preferredLanguage)
        if (!localized.isNullOrEmpty() || preferredLanguage.equals(TMDB_TRAILER_FALLBACK_LANGUAGE, ignoreCase = true)) {
            return localized
        }
        Log.d(TAG, "TMDB tv videos localized miss for $tmdbId ($preferredLanguage), retrying $TMDB_TRAILER_FALLBACK_LANGUAGE")
        return mergeLanguageFallback(localized, fetchTmdbTvVideosOnce(tmdbId, TMDB_TRAILER_FALLBACK_LANGUAGE))
    }

    private fun mergeLanguageFallback(
        localized: List<TmdbVideoResult>?,
        fallback: List<TmdbVideoResult>?
    ): List<TmdbVideoResult>? = when {
        fallback == null -> null
        fallback.isEmpty() && localized == null -> null
        else -> fallback
    }

    private suspend fun fetchTmdbMovieVideosOnce(tmdbId: Int, language: String): List<TmdbVideoResult>? {
        return try {
            val response = tmdbApi.getMovieVideos(
                movieId = tmdbId,
                apiKey = tmdbService.apiKey(),
                language = language
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "TMDB movie videos request failed ($tmdbId/$language): ${response.code()}")
                null
            } else {
                response.body()?.results.orEmpty()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TMDB movie videos error ($tmdbId/$language): ${e.message}")
            null
        }
    }

    private suspend fun fetchTmdbTvVideosOnce(tmdbId: Int, language: String): List<TmdbVideoResult>? {
        return try {
            val response = tmdbApi.getTvVideos(
                tvId = tmdbId,
                apiKey = tmdbService.apiKey(),
                language = language
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "TMDB tv videos request failed ($tmdbId/$language): ${response.code()}")
                null
            } else {
                response.body()?.results.orEmpty()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TMDB tv videos error ($tmdbId/$language): ${e.message}")
            null
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun summarizeUrl(url: String): String {
        return runCatching {
            val uri = URI(url)
            val host = uri.host ?: "unknown-host"
            val path = uri.path ?: "/"
            "$host$path"
        }.getOrDefault(url.take(80))
    }

    private fun obfuscateYoutubeKey(key: String): String {
        if (key.length <= 4) return "****"
        return "***${key.takeLast(4)}"
    }

    private fun rememberYouTubeSource(key: String, source: TrailerPlaybackSource) {
        val untilExpiry = source.expiresAt()?.minus(TRAILER_LINK_EXPIRY_MARGIN)?.toEpochMilli()
            ?.minus(clock.millis())
        val ttl = untilExpiry ?: TRAILER_LINK_TTL.toMillis()
        youtubeSourceCache.put(key, source, ttl)
    }

    private fun cachedYouTubeSource(key: String): TrailerPlaybackSource? {
        val source = youtubeSourceCache.get(key)?.source ?: return null
        if (!TrailerPlaybackFailures.hasFailed(source.videoUrl)) return source
        youtubeSourceCache.invalidate(source.videoUrl)
        return null
    }

    fun clearCache() {
        cache.clear()
        youtubeSourceCache.clear()
    }
}

data class TrailerLookupResult(
    val source: TrailerPlaybackSource?,
    val definiteMiss: Boolean = false,
    /** YouTube itself turned down every video that was tried (sign-in, age limit, removed). */
    val refused: Boolean = false,
    /** YouTube asked this device to sign in or is rate limiting it: nothing about the title itself. */
    val youTubeBlocked: Boolean = false,
    val signInDemanded: Boolean = false,
    val allowItemIds: Boolean = true
) {
    companion object {
        val DEFINITE_MISS = TrailerLookupResult(null, definiteMiss = true)
    }
}

data class YouTubeTrailerLookup(
    val source: TrailerPlaybackSource?,
    val unplayable: YouTubeUnplayableReason? = null
)

internal fun normalizeTmdbTrailerLanguage(language: String?): String {
    val normalized = language
        ?.trim()
        ?.replace('_', '-')
        ?.takeIf { it.isNotBlank() }
        ?: return TMDB_TRAILER_FALLBACK_LANGUAGE

    val formatted = if (normalized.contains('-')) {
        val parts = normalized.split("-", limit = 2)
        val locale = parts[0].lowercase()
        val region = parts.getOrNull(1)?.uppercase()?.takeIf { it.isNotBlank() }
        if (region != null) "$locale-$region" else locale
    } else {
        normalized.lowercase()
    }

    if (formatted == "en") return TMDB_TRAILER_FALLBACK_LANGUAGE

    // Map codes unsupported by TMDB to their closest equivalent
    return when (formatted) {
        "es-419" -> "es-MX"
        else -> formatted
    }
}

internal fun normalizeTmdbMediaType(type: String?): String? {
    return when (type?.lowercase()) {
        "movie", "film" -> "movie"
        "tv", "series", "show", "tvshow" -> "tv"
        else -> null
    }
}

/**
 * Real trailers precede teasers. With the Brazilian policy, official clips with explicit
 * track evidence precede the original-language trailer and the legacy locale fallback.
 */
internal fun rankTmdbVideoCandidates(
    results: List<TmdbVideoResult>,
    preferredLanguageCode: String = TMDB_TRAILER_FALLBACK_LANGUAGE,
    originalLanguage: String? = null,
    preferBrazilian: Boolean = false,
    attemptLimit: Int? = null
): List<TmdbVideoResult> {
    val preferredLanguage = preferredLanguageCode.substringBefore('-').lowercase()

    fun languageRank(iso6391: String?): Int {
        val lang = iso6391?.trim()?.lowercase()
        return when {
            lang == preferredLanguage -> 0
            lang == "en" -> 1
            else -> 2
        }
    }

    val ranked = results
        .asSequence()
        .filter(TmdbVideoResult::isPlayableTrailerCandidate)
        .sortedWith(
            compareBy<TmdbVideoResult> { videoTypePriority(it.type) }
                .thenBy { if (preferBrazilian) it.brazilianSelectionRank(originalLanguage) else 0 }
                .thenBy { languageRank(it.iso6391) }
                .thenBy { if (it.official == true) 0 else 1 }
                .thenByDescending { it.size ?: 0 }
                .thenByDescending { parsePublishedAtEpoch(it.publishedAt) }
        )
        .distinctBy { it.key?.trim() }
        .toList()
    val limit = attemptLimit ?: return ranked
    if (!preferBrazilian || ranked.size <= limit) return ranked.take(limit)

    // Leave room for a different trailer variant if the first one is unavailable.
    val reserved = ranked.filter { videoTypePriority(it.type) == 0 }
        .distinctBy { it.brazilianSelectionRank(originalLanguage) }
        .take(limit)
        .toSet()
    val selected = reserved + ranked.filterNot(reserved::contains).take(limit - reserved.size)
    return ranked.filter(selected::contains)
}

private val brazilianName = Regex("""\bpt[ -]br\b|\bportugu[eê]s do brasil\b""", RegexOption.IGNORE_CASE)
private val dubbedName = Regex("""\bdublad[oa]s?\b|\bdublagem\b""", RegexOption.IGNORE_CASE)
private val subtitledName = Regex("""\blegendad[oa]s?\b|\bcom legendas\b""", RegexOption.IGNORE_CASE)
private val negatedTrack = Regex("""\b(?:n[aã]o|sem)\s+(?:(?:foi|est[aá]|ser[aá])\s+)?(?:dubl|legend)""", RegexOption.IGNORE_CASE)

private fun TmdbVideoResult.isPlayableTrailerCandidate(): Boolean =
    site.equals("YouTube", ignoreCase = true) &&
        key?.trim()?.let { youTubeVideoIdOf(it) == it } == true &&
        videoTypePriority(type) < 2

private fun TmdbVideoResult.isBrazilian(): Boolean =
    iso6391.equals("pt", ignoreCase = true) && iso31661.equals("BR", ignoreCase = true)

private fun TmdbVideoResult.brazilianSelectionRank(originalLanguage: String?): Int {
    if (official != true) return 4
    val label = name.orEmpty()
    if ((isBrazilian() || brazilianName.containsMatchIn(label)) && !negatedTrack.containsMatchIn(label)) {
        val dubbed = dubbedName.containsMatchIn(label)
        val subtitled = subtitledName.containsMatchIn(label)
        if (dubbed && !subtitled) return 0
        if (subtitled && !dubbed) return 1
    }
    if (originalLanguage != null && iso6391.equals(originalLanguage, ignoreCase = true)) return 2
    return 3
}

private fun videoTypePriority(type: String?): Int {
    return when (type?.trim()?.lowercase()) {
        "trailer" -> 0
        "teaser" -> 1
        else -> 2
    }
}

private fun parsePublishedAtEpoch(value: String?): Long {
    if (value.isNullOrBlank()) return Long.MIN_VALUE
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(Long.MIN_VALUE)
}
