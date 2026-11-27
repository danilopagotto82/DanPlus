package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.R
import com.nuvio.tv.core.player.thumbnail.SeekThumbnails
import com.nuvio.tv.core.player.thumbnail.SeekrPreviewTrack
import com.nuvio.tv.ui.components.FocusScrollingText
import com.nuvio.tv.ui.components.NuvioScrollDefaults
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.v2.appearance.LocalV2Appearance
import com.nuvio.tv.ui.v2.components.nuvioControlSurface
import com.nuvio.tv.ui.v2.player.v2PlayerPanel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import tv.seekr.previews.android.SeekrThumbnail

/** "Chapter 3: Name", or "Chapter 3" when the file only numbers it. */
@Composable
internal fun chapterLabel(chapter: PlayerChapter): String =
    if (chapter.title != null) stringResource(R.string.player_chapter_named, chapter.number, chapter.title)
    else stringResource(R.string.player_chapter_number, chapter.number)

internal fun chapterLabel(context: android.content.Context, chapter: PlayerChapter): String =
    if (chapter.title != null) context.getString(R.string.player_chapter_named, chapter.number, chapter.title)
    else context.getString(R.string.player_chapter_number, chapter.number)

/** The chapter at the shown (or seek preview) position; null without chapters or before the first. */
@Composable
internal fun rememberCurrentChapter(viewModel: PlayerViewModel, chapters: List<PlayerChapter>): PlayerChapter? {
    if (chapters.isEmpty()) return null
    val timeline by viewModel.playbackTimeline.collectAsState()
    val index by remember(chapters) {
        derivedStateOf {
            val position = if (ScrubGlide.active) ScrubGlide.positionMs else timeline.currentPosition
            PlayerChapters.indexAt(chapters, position)
        }
    }
    return chapters.getOrNull(index)
}

/** Thin gaps in the timeline where each chapter starts. */
internal fun Modifier.chapterGaps(fractions: List<Float>): Modifier =
    if (fractions.isEmpty()) this else composed {
        drawWithContent {
            drawContent()
            val gap = 2.dp.toPx()
            for (fraction in fractions) {
                val x = size.width * fraction - gap / 2f
                drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(x, 0f), size = Size(gap, size.height))
            }
        }
    }

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ChaptersSidePanel(
    chapters: List<PlayerChapter>,
    currentPositionMs: Long,
    onChapterSelected: (PlayerChapter) -> Unit,
    onClose: () -> Unit,
    remoteTrack: SeekrPreviewTrack?,
    previewsActive: Boolean,
    localEnabled: Boolean,
    canPrioritizeLocal: Boolean,
    modifier: Modifier = Modifier
) {
    val currentIndex = remember(chapters) { PlayerChapters.indexAt(chapters, currentPositionMs).coerceAtLeast(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0))
    var focusedPosition by remember { mutableStateOf<Long?>(null) }
    val targets by remember(chapters, currentIndex, previewsActive) {
        derivedStateOf {
            if (!previewsActive) emptyList() else chapterPreviewPositions(
                visible = listState.layoutInfo.visibleItemsInfo.mapNotNull { chapters.getOrNull(it.index)?.startMs },
                focused = focusedPosition,
                initial = chapters.getOrNull(currentIndex)?.startMs
            )
        }
    }
    val remotePreviews = remember(remoteTrack) { mutableStateMapOf<Long, SeekrThumbnail?>() }
    LaunchedEffect(remoteTrack, previewsActive) {
        val track = remoteTrack?.takeIf { previewsActive } ?: return@LaunchedEffect
        loadVisibleChapterPreviews(snapshotFlow { targets }, remotePreviews, track::thumbnailFor)
    }
    val localPriority = focusedPosition?.takeIf {
        canPrioritizeLocal && previewsActive && localEnabled &&
            (remoteTrack == null || (remotePreviews.containsKey(it) && remotePreviews[it] == null))
    }
    DisposableEffect(localPriority) {
        SeekThumbnails.noteChapterPosition(localPriority)
        onDispose { SeekThumbnails.noteChapterPosition(null) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex } }.first { it }
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
    }
    val v2 = LocalV2Appearance.current != null
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(520.dp)
            .clip(RoundedCornerShape(topStart = NuvioTheme.spacing.lg, bottomStart = NuvioTheme.spacing.lg))
            .then(if (v2) Modifier.v2PlayerPanel() else Modifier.background(NuvioTheme.colors.BackgroundElevated))
    ) {
        Column(modifier = Modifier.padding(NuvioTheme.spacing.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.player_chapters),
                    style = MaterialTheme.typography.headlineSmall,
                    color = NuvioTheme.colors.TextPrimary
                )
                DialogButton(
                    text = stringResource(R.string.episodes_panel_close),
                    onClick = onClose,
                    isPrimary = false
                )
            }
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.lg))
            // The focused chapter glides to a fixed spot instead of the list jumping by whole rows.
            CompositionLocalProvider(LocalBringIntoViewSpec provides NuvioScrollDefaults.smoothScrollSpec) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
                contentPadding = PaddingValues(top = NuvioTheme.spacing.xs, bottom = NuvioTheme.spacing.xl),
                modifier = Modifier.fillMaxHeight()
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.number }) { index, chapter ->
                    val requested = chapter.startMs in targets
                    val remoteLoading = requested && remoteTrack != null && !remotePreviews.containsKey(chapter.startMs)
                    val remote = remotePreviews[chapter.startMs]
                    ChapterItem(
                        chapter = chapter,
                        isCurrent = index == currentIndex,
                        modifier = if (index == currentIndex) Modifier.focusRequester(focusRequester) else Modifier,
                        remote = remote,
                        loading = remoteLoading,
                        loadLocal = requested && localEnabled && !remoteLoading && remote == null,
                        onFocus = { focused ->
                            if (focused) focusedPosition = chapter.startMs
                            else if (focusedPosition == chapter.startMs) focusedPosition = null
                        },
                        onClick = { onChapterSelected(chapter) }
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChapterItem(
    chapter: PlayerChapter,
    isCurrent: Boolean,
    modifier: Modifier,
    remote: SeekrThumbnail?,
    loading: Boolean,
    loadLocal: Boolean,
    onFocus: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val v2 = LocalV2Appearance.current != null
    var isFocused by remember { mutableStateOf(false) }
    var localPainter by remember(chapter.startMs, loadLocal) { mutableStateOf<Painter?>(null) }
    LaunchedEffect(chapter.startMs, loadLocal) {
        if (loadLocal) {
            snapshotFlow { SeekThumbnails.tick.intValue }.first {
                localPainter = seekThumbnailPainter(SeekThumbnails.chapterTileFor(chapter.startMs))
                localPainter != null
            }
        }
    }
    val remotePainter = remember(remote) { remote?.let { BitmapPainter(it.bitmap.asImageBitmap()) } }
    val painter = remotePainter ?: localPainter
    val shape = RoundedCornerShape(NuvioTheme.radii.xl)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
                onFocus(it.isFocused)
            }
            .nuvioControlSurface(shape),
        colors = CardDefaults.colors(
            containerColor = if (v2) {
                if (isCurrent) NuvioTheme.colors.Secondary.copy(alpha = .12f) else Color.Transparent
            } else NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = if (v2) Color.Transparent else NuvioTheme.colors.FocusBackground
        ),
        border = if (v2) CardDefaults.border(border = Border.None, focusedBorder = Border.None) else CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(NuvioTheme.spacing.xxs, Color.White), shape = shape)
        ),
        scale = CardDefaults.scale(focusedScale = if (v2) 1f else 1.01f),
        shape = CardDefaults.shape(shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(73.dp)
                    .clip(RoundedCornerShape(NuvioTheme.radii.md))
                    .background(NuvioTheme.colors.SurfaceVariant)
            ) {
                if (painter != null) {
                    Image(painter, contentDescription = null, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize())
                } else if (loading && isFocused) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).width(18.dp).height(18.dp),
                        color = NuvioTheme.colors.Secondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null,
                        modifier = Modifier.align(Alignment.Center), tint = NuvioTheme.colors.TextTertiary)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(NuvioTheme.spacing.xs)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = NuvioTheme.spacing.sm, vertical = 2.dp)
                ) {
                    Text(
                        text = formatSeekTime(chapter.startMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
            ) {
                // Fixed-height lines (number line + two title lines) keep every row as tall as its picture,
                // so each step down the list scrolls the same distance.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (chapter.title != null) stringResource(R.string.player_chapter_number, chapter.number) else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioTheme.extendedColors.textSecondary,
                        maxLines = 1
                    )
                    if (isCurrent) {
                        Text(
                            text = (if (chapter.title != null) " • " else "") + stringResource(R.string.player_chapter_now_playing),
                            style = MaterialTheme.typography.labelMedium,
                            color = NuvioTheme.colors.Secondary,
                            maxLines = 1
                        )
                    }
                }
                FocusScrollingText(
                    text = chapter.title ?: stringResource(R.string.player_chapter_number, chapter.number),
                    focused = isFocused,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    color = NuvioTheme.colors.TextPrimary,
                    startDelayMillis = 800L
                )
            }
        }
    }
}

internal fun chapterPreviewPositions(visible: List<Long>, focused: Long?, initial: Long?): List<Long> =
    buildList {
        val lead = focused?.takeIf { it in visible } ?: initial?.takeIf { visible.isEmpty() }
        lead?.let(::add)
        addAll(visible)
    }.distinct().take(4)

internal suspend fun <T> loadVisibleChapterPreviews(
    targets: Flow<List<Long>>,
    previews: MutableMap<Long, T?>,
    read: suspend (Long) -> T?
) {
    targets.collectLatest { positions ->
        previews.keys.retainAll(positions.toSet())
        delay(100)
        for (position in positions) {
            if (position !in previews) {
                previews[position] = withTimeoutOrNull(4_000) { read(position) }
            }
        }
    }
}

/** Chapter starts as shares of the timeline; empty without chapters. */
@Composable
internal fun rememberChapterFractions(viewModel: PlayerViewModel, durationMs: Long): List<Float> {
    val chapters = rememberChapters(viewModel)
    return remember(chapters, durationMs) { PlayerChapters.timelineFractions(chapters, durationMs) }
}

/** The chapter under the seek cursor while seeking with the controls hidden. */
@Composable
internal fun rememberSeekChapter(viewModel: PlayerViewModel): PlayerChapter? =
    rememberCurrentChapter(viewModel, rememberChapters(viewModel))

@Composable
private fun rememberChapters(viewModel: PlayerViewModel): List<PlayerChapter> {
    val flow = remember(viewModel) { viewModel.uiState.map { it.chapters }.distinctUntilChanged() }
    return flow.collectAsState(initial = viewModel.uiState.value.chapters).value
}
