package com.nuvio.tv.ui.v2.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.nuvio.tv.domain.model.AppTheme
import com.nuvio.tv.domain.model.GlassTintMode
import com.nuvio.tv.domain.model.VisualStyle
import com.nuvio.tv.ui.theme.NuvioTheme

internal data class CinematicTones(val base: Color, val top: Color, val opacity: Float) {
    fun surfaceBrush() = Brush.verticalGradient(listOf(top, base))
    fun surfaceBrush(alpha: Float) = Brush.verticalGradient(listOf(top.copy(alpha = alpha), base.copy(alpha = alpha)))
}

/** A contrast floor under text, independent of blur quality and selected artwork. */
internal fun cinematicSurfaceOpacity(transparency: Int): Float =
    .72f + .28f * (100 - transparency.coerceIn(0, 100)) / 100f

@Composable
internal fun cinematicTones(): CinematicTones {
    val appearance = LocalV2Appearance.current
    val theme = NuvioTheme.colors
    val liquid = appearance?.visualStyle == VisualStyle.PURE_LIQUID_DARK
    val neutral = NuvioTheme.currentTheme == AppTheme.GLASS || appearance?.glassTintMode == GlassTintMode.NEUTRAL
    val accent = when {
        neutral -> Color.Unspecified
        appearance?.glassTintMode == GlassTintMode.ARTWORK ->
            LocalArtworkAccent.current?.color?.value ?: theme.Secondary
        else -> theme.Secondary
    }
    val opacity = cinematicSurfaceOpacity(appearance?.glassTransparencyPercent ?: 60)
    val dark = if (liquid) Color(0xFF040D16) else Color(0xFF090C11)
    val base = lerp(dark, theme.Background, .24f)
    val top = if (neutral) lerp(base, Color.White, .045f) else lerp(base, accent, .10f * opacity)
    return CinematicTones(base, top, opacity)
}
