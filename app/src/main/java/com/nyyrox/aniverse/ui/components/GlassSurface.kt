package com.nyyrox.aniverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.ThemeRuntime
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

/**
 * Optional backdrop that in-content glass panels refract. Screens that want true liquid refraction
 * provide one via [GlassAmbient]; everywhere else panels fall back to a frosted look that needs no sampling.
 */
val LocalContentGlassLayer = compositionLocalOf<PrismalGlassLayer?> { null }

/**
 * Zenkai surface.
 *  - Card style OFF  -> no box at all (clean poster / list layout).
 *  - Otherwise -> one quiet tonal fill (no rim, no refraction).
 *
 * @param alwaysBox keep the box even when card style is off (use for dialogs, sheets, inputs, floating bars).
 */
@Composable
fun Modifier.zenkaiSurface(
    shape: Shape = OttCardShape,
    radius: Dp = 16.dp,
    tint: Color? = null,
    alwaysBox: Boolean = false
): Modifier {
    if (!ThemeRuntime.cardStyle && !alwaysBox) return this
    // Boxed surface = a quiet tonal fill. No rim, no refraction: the fill alone separates it from the page.
    return clip(shape).background(tint ?: OttInk(if (ThemeRuntime.isLight) 0.05f else 0.07f))
}

/** Pill / circular variant for chips and icon buttons. Always boxed (small controls need a hit target). */
@Composable
fun Modifier.zenkaiPill(circle: Boolean = false, tint: Color? = null): Modifier =
    zenkaiSurface(
        shape = if (circle) CircleShape else RoundedCornerShape(50),
        radius = 40.dp,
        tint = tint,
        alwaysBox = true
    )

/**
 * Soft accent-tinted, blurred artwork that sits behind a screen and feeds the liquid-glass panels.
 * Wrap a screen's content in this to enable real refraction there.
 */
@Composable
fun GlassAmbient(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val glassOn = LocalThemeSettings.current.isGlassmorphismEnabled && PrismalGlass.supportsRefraction
    val light = ThemeRuntime.isLight
    if (!glassOn) {
        Box(modifier.fillMaxSize().background(OttBg)) { content() }
        return
    }
    val layer = rememberPrismalGlassLayer()
    Box(modifier.fillMaxSize().background(OttBg)) {
        Box(Modifier.fillMaxSize().prismalGlassLayer(layer)) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            OttPrimary.copy(alpha = if (light) 0.10f else 0.30f),
                            OttSecondary.copy(alpha = if (light) 0.08f else 0.16f),
                            Color.Transparent
                        )
                    )
                )
            )
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .graphicsLayer {
                            alpha = if (light) 0.35f else 0.55f
                            scaleX = 1.3f
                            scaleY = 1.3f
                        }
                )
            }
        }
        // keeps text readable over the artwork
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(OttBg.copy(alpha = 0.55f), OttBg.copy(alpha = 0.94f)))
            )
        )
        CompositionLocalProvider(LocalContentGlassLayer provides layer) { content() }
    }
}

/** Poster / thumbnail outline that disappears when card style is off. */
fun Modifier.posterBorder(width: Dp, color: Color, shape: Shape): Modifier =
    if (ThemeRuntime.cardStyle) this.border(width, color, shape) else this
