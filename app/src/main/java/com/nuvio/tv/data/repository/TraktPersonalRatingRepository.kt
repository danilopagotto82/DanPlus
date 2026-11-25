package com.nuvio.tv.data.repository

import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.TraktAuthState
import com.nuvio.tv.data.remote.api.TraktApi
import com.nuvio.tv.data.remote.dto.trakt.TraktIdsDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingEntryDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingItemDto
import com.nuvio.tv.data.remote.dto.trakt.TraktRatingsRequestDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The episode target takes identifiers of the episode itself, independently of the parent series. */
sealed interface TraktPersonalRatingTarget {
    val ids: TraktIdsDto

    data class Movie(override val ids: TraktIdsDto) : TraktPersonalRatingTarget
    data class Episode(override val ids: TraktIdsDto) : TraktPersonalRatingTarget
}

private val TraktPersonalRatingTarget.bucket: String
    get() = when (this) {
        is TraktPersonalRatingTarget.Movie -> "movies"
        is TraktPersonalRatingTarget.Episode -> "episodes"
    }

internal fun TraktPersonalRatingTarget.request(rating: Int?): TraktRatingsRequestDto {
    require(rating == null || rating in 1..10)
    val validIds = ratingIds()
    require(validIds.trakt != null || validIds.imdb != null || validIds.tmdb != null || validIds.tvdb != null)
    val entry = TraktRatingEntryDto(validIds, rating)
    return when (this) {
        is TraktPersonalRatingTarget.Movie -> TraktRatingsRequestDto(movies = listOf(entry))
        is TraktPersonalRatingTarget.Episode -> TraktRatingsRequestDto(episodes = listOf(entry))
    }
}

private val traktRatingImdbId = Regex("tt[0-9]+")

private fun TraktPersonalRatingTarget.ratingIds() = TraktIdsDto(
    trakt = ids.trakt?.takeIf { it > 0 },
    imdb = ids.imdb?.takeIf(traktRatingImdbId::matches),
    tmdb = ids.tmdb?.takeIf { it > 0 },
    tvdb = ids.tvdb?.takeIf { this is TraktPersonalRatingTarget.Episode && it > 0 }
)

internal fun TraktPersonalRatingTarget.ratingIn(item: TraktRatingItemDto): Int? {
    val remote = when (this) {
        is TraktPersonalRatingTarget.Movie -> item.movie?.ids
        is TraktPersonalRatingTarget.Episode -> item.episode?.ids
    } ?: return null
    val ids = ratingIds()
    val pairs = listOf(ids.trakt to remote.trakt, ids.imdb to remote.imdb, ids.tmdb to remote.tmdb, ids.tvdb to remote.tvdb)
        .filter { it.first != null && it.second != null }
    if (pairs.isEmpty() || pairs.any { it.first != it.second }) return null
    return item.rating?.takeIf { it in 1..10 }
}

/** Personal scores for the existing authenticated Trakt profile. Public scores are read elsewhere. */
@Singleton
class TraktPersonalRatingRepository @Inject constructor(
    private val api: TraktApi,
    private val auth: TraktAuthService,
    private val profiles: ProfileManager
) {
    private val writes = Mutex()

    internal suspend fun read(owner: TraktRatingOwner, target: TraktPersonalRatingTarget): Int? {
        target.request(null) // validate the target before contacting Trakt
        var page = 1
        while (true) {
            checkOwner(owner)
            val response = auth.executeAuthorizedRequest { authorization ->
                checkOwner(owner, authorization)
                api.getRatings(authorization, target.bucket, page, 100)
            } ?: throw IOException("Trakt ratings unavailable")
            checkOwner(owner)
            if (!response.isSuccessful) throw IOException("Trakt ratings read failed: ${response.code()}")
            response.body().orEmpty().firstNotNullOfOrNull(target::ratingIn)?.let { return it }
            val lastPage = response.headers()["X-Pagination-Page-Count"]?.toIntOrNull() ?: 1
            if (lastPage > 250) throw IOException("Trakt ratings exceed the read limit")
            if (page >= lastPage) return null
            page++
        }
    }

    /** Returns false when the existing score already matches the requested state. */
    internal suspend fun set(
        owner: TraktRatingOwner, target: TraktPersonalRatingTarget, rating: Int?
    ): Boolean = writes.withLock {
        val request = target.request(rating)
        if (read(owner, target) == rating) {
            checkOwner(owner)
            return@withLock false
        }
        val response = auth.executeAuthorizedWriteRequest { authorization ->
            checkOwner(owner, authorization)
            if (rating == null) api.deletePersonalRating(authorization, request)
            else api.putPersonalRating(authorization, request)
        } ?: throw IOException("Trakt ratings unavailable")
        checkOwner(owner)
        val receipt = response.body()
        val counts = if (rating == null) receipt?.deleted else receipt?.added
        if (!response.isSuccessful || receipt == null ||
            receipt.notFound.values.any { !it.isNullOrEmpty() } || counts?.get(target.bucket) != 1
        ) throw IOException("Trakt did not acknowledge the rating")
        true
    }

    internal suspend fun checkOwner(owner: TraktRatingOwner, authorization: String? = null) {
        val state = auth.getCurrentAuthState(owner.profile)
        currentCoroutineContext().ensureActive()
        check(owner.accepts(profiles.activeProfileId.value, profiles.profileSelectionRevision.value, state, authorization)) {
            "Trakt rating owner changed"
        }
    }
}

internal data class TraktRatingOwner(val profile: Int, val revision: Long, val generation: Long) {
    fun accepts(
        activeProfile: Int, activeRevision: Long, state: TraktAuthState, authorization: String?
    ): Boolean = profile == activeProfile && revision == activeRevision && state.isAuthenticated &&
        generation == state.authorizationGeneration &&
        (authorization == null || authorization == "Bearer ${state.accessToken}")
}
