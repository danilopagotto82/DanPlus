package com.nuvio.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaCastMember

@Composable
internal fun DanPlusFocusDock(item: MetaPreview, artwork: BrowseArtwork, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val panel = Color(0xFF0B1725)
    val shape = RoundedCornerShape(14.dp)
    val backdrop = remember(context, artwork.backdrop) {
        artwork.backdrop?.let { ImageRequest.Builder(context).data(it).size(780, 320).build() }
    }
    val logo = artwork.logo
    val logoRequest = remember(context, logo) {
        logo?.let { ImageRequest.Builder(context).data(it).size(320, 108).build() }
    }
    var logoLoaded by remember(item.id, item.apiType, logoRequest) { mutableStateOf(false) }
    val details = artwork.details
    val displayItem = item.copy(
        description = details?.description ?: item.description,
        releaseInfo = details?.releaseYear ?: item.releaseInfo,
        runtime = details?.runtimeMinutes?.takeIf { it > 0 }?.toString() ?: item.runtime,
        imdbRating = item.imdbRating?.takeIf { it.isFinite() && it > 0f } ?: details?.rating,
        ageRating = artwork.ageRating ?: item.ageRating,
        genres = details?.genres.orEmpty().ifEmpty { item.genres },
        status = details?.status ?: item.status,
        seasonCount = details?.seasonCount?.takeIf { it > 0 } ?: item.seasonCount
    )
    val directors = artwork.cachedCredits?.directorMembers.orEmpty()
        .ifEmpty { item.director.filter(String::isNotBlank).map { MetaCastMember(it) } }
    val actors = artwork.cachedCredits?.castMembers.orEmpty()
    Box(
        modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 150.dp)
            .clip(shape).background(panel).border(1.dp, Color(0xFF33445B), shape)
    ) {
        AsyncImage(backdrop, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop,
            alignment = Alignment.TopEnd, alpha = .76f)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to panel, .57f to panel.copy(alpha = .94f), 1f to panel.copy(alpha = .34f)
        )))
        Column(
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(.78f)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (logoRequest != null) {
                Box(Modifier.widthIn(max = 260.dp).fillMaxWidth().heightIn(min = 38.dp), contentAlignment = Alignment.CenterStart) {
                    if (!logoLoaded) Text(item.name, color = Color.White, style = MaterialTheme.typography.titleLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AsyncImage(logoRequest, item.name, Modifier.matchParentSize(), contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart, onSuccess = { logoLoaded = true }, onError = { logoLoaded = false })
                }
            } else {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MediaMetadataChips(displayItem, episodeCount = details?.episodeCount,
                lastAirYear = details?.lastAirYear, onArtwork = true)
            MediaGenres(displayItem.genres, onArtwork = true)
            displayItem.description?.takeIf(String::isNotBlank)?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            CreditLine(stringResource(R.string.pause_direction_label), directors, 2)
            CreditLine(stringResource(R.string.pause_cast_label), actors, 3)
        }
    }
}

@Composable
private fun CreditLine(label: String, people: List<MetaCastMember>, limit: Int) {
    val credits = people.filter { it.name.isNotBlank() }.distinctBy { it.tmdbId ?: it.name }.take(limit)
    if (credits.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        val context = LocalContext.current
        credits.first().photo?.takeIf(String::isNotBlank)?.let { photo ->
            val model = remember(context, photo) {
                ImageRequest.Builder(context).data(photo).size(40, 40)
                    .networkCachePolicy(CachePolicy.DISABLED).build()
            }
            val painter = rememberAsyncImagePainter(model)
            val state by painter.state.collectAsState()
            if (state is AsyncImagePainter.State.Success) {
                Image(painter, null, Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
            }
        }
        Text(credits.joinToString(" · ") { it.name }, color = Color(0xFFDCE7F4),
            style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
