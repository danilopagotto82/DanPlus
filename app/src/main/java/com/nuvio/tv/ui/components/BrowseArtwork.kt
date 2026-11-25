package com.nuvio.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.core.tmdb.TmdbMetadataService
import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.domain.model.ContentType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowseArtworkEntryPoint {
    fun tmdbMetadata(): TmdbMetadataService
    fun tmdbIds(): TmdbService
}

internal data class BrowseArtwork(val backdrop: String?, val logo: String? = null)

/** Resolves only the admitted title. The existing TMDB cache and Coil still own metadata and images. */
@Composable
internal fun rememberBrowseArtwork(
    id: String,
    mediaType: String,
    backdrop: String?,
    imdbId: String? = null,
    withLogo: Boolean = false,
    resolve: Boolean = true
): BrowseArtwork {
    val app = LocalContext.current.applicationContext
    val type = ContentType.fromString(mediaType)
    val supplied = backdrop?.takeIf(String::isNotBlank)
    var artwork by remember(id, type, imdbId, supplied, withLogo) { mutableStateOf(BrowseArtwork(supplied)) }
    LaunchedEffect(app, id, type, imdbId, supplied, withLogo, resolve) {
        if (!resolve || (supplied != null && !withLogo)) return@LaunchedEffect
        if (type !in setOf(ContentType.MOVIE, ContentType.SERIES, ContentType.TV)) return@LaunchedEffect
        val services = EntryPointAccessors.fromApplication(app, BrowseArtworkEntryPoint::class.java)
        val tmdbId = services.tmdbIds().ensureTmdbId(id, mediaType, imdbId) ?: return@LaunchedEffect
        val metadata = services.tmdbMetadata()
        val art = metadata.fetchPosterArt(tmdbId, type, "pt-BR")
        coroutineContext.ensureActive()
        artwork = BrowseArtwork(supplied ?: art?.backdrop)
        if (withLogo) {
            val logo = metadata.fetchFocusDockLogo(tmdbId, type, art?.originalLanguage)
            coroutineContext.ensureActive()
            artwork = artwork.copy(logo = logo)
        }
    }
    return artwork
}
