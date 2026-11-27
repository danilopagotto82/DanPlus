package com.nuvio.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest

/** Fits the original proportions inside the artwork area, including portrait fallbacks. */
internal fun collectionFeatureSize(source: Size, available: Size): Size {
    val scale = minOf(available.width / source.width, available.height / source.height)
    return Size(source.width * scale, source.height * scale)
}

/** The fade begins at the actual bitmap edge, where it meets a matte of the same colour. */
@Composable
internal fun CollectionFeatureArtwork(
    request: ImageRequest?,
    matte: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    if (request == null) return
    val painter = rememberAsyncImagePainter(request, contentScale = ContentScale.Fit)
    val state by painter.state.collectAsState()
    val image = (state as? AsyncImagePainter.State.Success)?.result?.image ?: return
    if (image.width <= 0 || image.height <= 0) return
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterEnd) {
        val displayed = collectionFeatureSize(Size(image.width.toFloat(), image.height.toFloat()),
            Size(maxWidth.value, maxHeight.value))
        val letterboxed = displayed.height < maxHeight.value - 1f
        Box(Modifier.size(displayed.width.dp, displayed.height.dp)) {
            Image(
                painter, contentDescription = null, contentScale = ContentScale.FillBounds, alpha = alpha,
                modifier = Modifier.fillMaxSize().drawWithCache {
                    val middle = matte.copy(alpha = .15f)
                    val edge = Brush.horizontalGradient(
                        0f to matte, .10f to matte.copy(alpha = .91f),
                        .32f to matte.copy(alpha = .32f), .66f to middle, 1f to middle
                    )
                    val vertical = Brush.verticalGradient(
                        0f to matte, .14f to Color.Transparent, .82f to Color.Transparent, 1f to matte
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(edge)
                        if (letterboxed) drawRect(vertical)
                    }
                }
            )
        }
    }
}
