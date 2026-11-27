@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nuvio.tv.R
import com.nuvio.tv.core.image.sizedHomeArtworkUrl
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.ui.components.CollectionFeatureArtwork
import com.nuvio.tv.ui.components.MediaSynopsis
import com.nuvio.tv.ui.components.MediaGenres
import com.nuvio.tv.ui.components.MediaMetadataChips
import com.nuvio.tv.ui.screens.scout.ScoutChip
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.util.localizedContentType
import com.nuvio.tv.ui.v2.appearance.cinematicTones

@Composable
internal fun FolderSidePreview(
    item: MetaPreview?,
    credits: FolderPreviewCredits?,
    modifier: Modifier = Modifier
) {
    val colors = NuvioTheme.colors
    val tones = cinematicTones()
    val shape = RoundedCornerShape(NuvioTheme.radii.lg)
    val description = item?.description?.takeIf(String::isNotBlank)
    BoxWithConstraints(
        modifier.clip(shape)
            .background(tones.surfaceBrush(tones.opacity))
            .border(1.dp, colors.TextPrimary.copy(alpha = .17f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (item == null) return@BoxWithConstraints
        val compact = maxHeight < 340.dp
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            FolderTitleArtwork(item, logoHeight = if (compact) 45.dp else 62.dp,
                centered = true, preferredLogo = credits?.logo)
            MediaMetadataChips(item, episodeCount = credits?.episodeCount)
            MediaGenres(item.genres.ifEmpty { listOf(localizedContentType(item.apiType)) })
            if (description != null) {
                FolderPreviewDivider()
                Box(Modifier.weight(1f, fill = false)) {
                    MediaSynopsis(item.name, description, maxLines = Int.MAX_VALUE,
                        adaptiveHeight = true, style = MaterialTheme.typography.bodyLarge)
                }
            }
            credits?.creator?.takeIf(String::isNotBlank)?.let {
                Text(stringResource(R.string.dan_folder_creator, it), color = colors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            val director = credits?.director ?: item.director.firstOrNull(String::isNotBlank)
                ?.takeIf { item.apiType == "movie" }
            director?.let {
                Text(stringResource(R.string.dan_folder_director, it), color = colors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
            }
            val cast = credits?.cast.orEmpty()
            if (cast.isNotEmpty()) {
                FolderPreviewDivider()
                Text(stringResource(R.string.pause_cast_label), color = colors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cast.forEach { person ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val shape = RoundedCornerShape(6.dp)
                            val portraitHeight = if (compact) 40.dp else 56.dp
                            Box(Modifier.width(if (compact) 34.dp else 44.dp).height(portraitHeight)
                                .clip(shape).background(colors.TextPrimary.copy(alpha = .09f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.size(20.dp), tint = colors.TextSecondary)
                                AsyncImage(
                                    model = person.photo?.takeIf(String::isNotBlank)
                                        ?.replace("https://image.tmdb.org/t/p/w500/", "https://image.tmdb.org/t/p/w185/"),
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter
                                )
                            }
                            Text(person.name, color = colors.TextPrimary, style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPreviewDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NuvioTheme.colors.TextSecondary.copy(alpha = .22f)))
}

@Composable
internal fun FolderShowcase(
    item: MetaPreview,
    addon: String,
    onWatch: (MetaPreview, String) -> Unit,
    onOpen: (MetaPreview, String) -> Unit
) {
    val colors = NuvioTheme.colors
    val tones = cinematicTones()
    val context = LocalContext.current
    val shape = RoundedCornerShape(NuvioTheme.radii.xl)
    val image = item.backdropUrl?.takeIf(String::isNotBlank)
    val request = remember(context, image) {
        image?.let { ImageRequest.Builder(context).data(sizedHomeArtworkUrl(it, 780)).size(780, 440).build() }
    }
    Box(
        Modifier.fillMaxWidth().padding(horizontal = NuvioTheme.spacing.xxxl)
            .height(270.dp).clip(shape).background(tones.base)
            .border(1.dp, colors.Secondary.copy(alpha = .35f), shape)
    ) {
        CollectionFeatureArtwork(request, tones.base,
            Modifier.align(Alignment.CenterEnd).fillMaxWidth(.82f).fillMaxHeight())
        Box(Modifier.matchParentSize()
            .background(Brush.horizontalGradient(
                0f to tones.base, .36f to tones.base.copy(alpha = .82f), .68f to tones.base.copy(alpha = .70f),
                .88f to tones.base.copy(alpha = .10f), 1f to Color.Transparent
            ))
            .background(Brush.verticalGradient(
                0f to tones.top.copy(alpha = .13f), .44f to Color.Transparent, 1f to tones.base.copy(alpha = .76f)
            )))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
        ) {
            Column(Modifier.fillMaxWidth(.68f), verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                FolderTitleArtwork(item, logoHeight = 64.dp)
                MediaMetadataChips(item, onArtwork = true)
                MediaGenres(item.genres, onArtwork = true)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    MediaSynopsis(item.name, it, color = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                if (item.apiType == "movie" || item.apiType == "series" || item.apiType == "tv") {
                    ScoutChip(stringResource(R.string.dan_folder_watch), onClick = { onWatch(item, addon) }, primary = true)
                }
                ScoutChip(stringResource(R.string.tmdb_details_title), onClick = { onOpen(item, addon) })
            }
        }
    }
}

@Composable
private fun FolderTitleArtwork(
    item: MetaPreview,
    logoHeight: Dp,
    centered: Boolean = false,
    preferredLogo: String? = null
) {
    val logo = preferredLogo ?: item.logo?.takeIf(String::isNotBlank)
    var loaded by remember(logo, item.id, item.apiType) { mutableStateOf(false) }
    val alignment = if (centered) Alignment.Center else Alignment.CenterStart
    Box(Modifier.fillMaxWidth().heightIn(min = logoHeight), contentAlignment = alignment) {
        if (!loaded) {
            Text(item.name, style = MaterialTheme.typography.headlineMedium, color = NuvioTheme.colors.TextPrimary,
                modifier = Modifier.fillMaxWidth(), textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (logo != null) {
            AsyncImage(
                model = sizedHomeArtworkUrl(logo, 500),
                contentDescription = item.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Fit,
                alignment = alignment,
                onSuccess = { loaded = true },
                onError = { loaded = false }
            )
        }
    }
}
