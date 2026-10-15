package com.styropyr0.prismal.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPlainPrismalGlass
import com.styropyr0.prismal.effects.PrismalAdaptiveTuning
import com.styropyr0.prismal.effects.prismalGradientGlass

/**
 * Progressive blur panel with vertically-weighted refraction.
 *
 * Refraction peaks are controlled by [refractionTopWeight], [refractionMiddleWeight], and
 * [refractionBottomWeight]. With middle weight dominant, refraction fades away in both directions.
 */
@Composable
fun PrismalGradientGlassPanel(
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f },
    blurRadiusDp: Dp = 20.dp,
    refractionHeightDp: Dp = 28.dp,
    refractionAmountDp: Dp = 36.dp,
    refractionTopWeight: Float = 0f,
    refractionMiddleWeight: Float = 0f,
    refractionBottomWeight: Float = 1f,
    blurFadeStart: Float = 0f,
    blurFadeEnd: Float = 0.8f,
    chromaticAberration: Float = 0f,
    tint: Color = Color.Unspecified,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
) {
    val density = LocalDensity.current
    val isLightTheme = !isSystemInDarkTheme()
    val surfaceTint =
        when {
            tint != Color.Unspecified -> tint
            isLightTheme -> Color.White.copy(alpha = 0.2f)
            else -> Color.Black.copy(alpha = 0.15f)
        }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawPlainPrismalGlass(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    prismalGradientGlass(
                        density = density,
                        adaptiveLuminance = adaptiveLuminance,
                        luminance = luminance(),
                        blurRadiusPx = with(density) { blurRadiusDp.toPx() },
                        refractionHeightPx = with(density) { refractionHeightDp.toPx() },
                        refractionAmountPx = with(density) { refractionAmountDp.toPx() },
                        refractionTopWeight = refractionTopWeight,
                        refractionMiddleWeight = refractionMiddleWeight,
                        refractionBottomWeight = refractionBottomWeight,
                        blurFadeStart = blurFadeStart,
                        blurFadeEnd = blurFadeEnd,
                        tint = surfaceTint,
                        tintIntensity = if (surfaceTint.alpha > 0f) 0.6f else 0f,
                        chromaticAberration = chromaticAberration,
                        adaptiveTuning = adaptiveTuning,
                    )
                }
            )
    )
}
