package com.nuvio.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.tmdb.TmdbEnrichment
import com.nuvio.tv.core.tmdb.TmdbMetadataService
import com.nuvio.tv.core.tmdb.TmdbPosterArt
import com.nuvio.tv.data.repository.TraktRelatedService
import com.nuvio.tv.data.repository.parseContentIds
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.domain.model.ContentType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowseArtworkEntryPoint {
    fun tmdbMetadata(): TmdbMetadataService
    fun tmdbIds(): TmdbService
    fun profileManager(): ProfileManager
    fun traktRelated(): TraktRelatedService
}

internal data class BrowseArtwork(
    val backdrop: String?,
    val logo: String? = null,
    val details: TmdbPosterArt? = null,
    val ageRating: String? = null,
    val cachedCredits: TmdbEnrichment? = null
)

/** Resolves only the admitted title. The existing TMDB cache and Coil still own metadata and images. */
@Composable
internal fun rememberBrowseArtwork(
    id: String,
    mediaType: String,
    backdrop: String?,
    imdbId: String? = null,
    withLogo: Boolean = false,
    resolve: Boolean = true,
    tmdbId: Int? = null,
    traktId: Int? = null
): BrowseArtwork {
    val app = LocalContext.current.applicationContext
    val services = remember(app) { EntryPointAccessors.fromApplication(app, BrowseArtworkEntryPoint::class.java) }
    val profiles = services.profileManager()
    val profileId by profiles.activeProfileId.collectAsState()
    val type = when (ContentType.fromString(mediaType)) {
        ContentType.TV -> ContentType.SERIES
        else -> ContentType.fromString(mediaType)
    }
    val supplied = backdrop?.takeIf(String::isNotBlank)
    var artwork by remember(profileId, id, type, imdbId, tmdbId, traktId, supplied, withLogo, resolve) {
        mutableStateOf(BrowseArtwork(supplied))
    }
    LaunchedEffect(app, profileId, id, type, imdbId, tmdbId, traktId, supplied, withLogo, resolve) {
        if (!resolve || (supplied != null && !withLogo)) return@LaunchedEffect
        if (type != ContentType.MOVIE && type != ContentType.SERIES) return@LaunchedEffect
        val owner = profileId
        if (withLogo) delay(180)
        if (profiles.activeProfileId.value != owner) return@LaunchedEffect
        val explicitTrakt = id.trim().startsWith("trakt:", ignoreCase = true)
        val legacyTrakt = traktId != null && id.trim().substringBefore(':').toIntOrNull() != null
        val titleId = if (explicitTrakt || legacyTrakt) imdbId.orEmpty() else id.trim()
        val canonicalId = tmdbId?.takeIf { it > 0 }?.toString()
            ?: services.tmdbIds().ensureTmdbId(titleId, type.toApiString(), imdbId)?.validBrowseTmdbId()
            ?: (traktId ?: parseContentIds(id).trakt.takeIf { explicitTrakt })
                ?.takeIf { it > 0 }?.let { trakt ->
                    val ids = services.traktRelated().resolveBrowseIds(trakt, type)
                    ids?.tmdb?.toString()?.validBrowseTmdbId()
                        ?: ids?.imdb?.let { services.tmdbIds().ensureTmdbId(it, type.toApiString()) }?.validBrowseTmdbId()
                }
            ?: return@LaunchedEffect
        val metadata = services.tmdbMetadata()
        val art = metadata.fetchPosterArt(canonicalId, type, "pt-BR")
        coroutineContext.ensureActive()
        if (profiles.activeProfileId.value != owner) return@LaunchedEffect
        val cached = if (withLogo) metadata.cachedBrowseCredits(canonicalId, type) else null
        artwork = BrowseArtwork(supplied ?: art?.backdrop, details = art, ageRating = cached?.ageRating, cachedCredits = cached)
        if (withLogo && (art != null || cached != null)) coroutineScope {
            val logo = async { metadata.fetchFocusDockLogo(canonicalId, type, art?.originalLanguage ?: cached?.language) }
            val ageRating = async { cached?.ageRating ?: metadata.fetchHomeAgeRating(canonicalId, type, "pt-BR") }
            val resolvedLogo = logo.await()
            val resolvedRating = ageRating.await()
            coroutineContext.ensureActive()
            if (profiles.activeProfileId.value == owner) {
                artwork = artwork.copy(logo = resolvedLogo, ageRating = resolvedRating)
            }
        }
    }
    return artwork
}

private fun String.validBrowseTmdbId(): String? = toIntOrNull()?.takeIf { it > 0 }?.toString()
