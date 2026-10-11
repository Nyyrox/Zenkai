package com.styropyr0.prismal.effects

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.PrismalGlassEffectProvider
import kotlin.math.sign

/**
 * Applies the standard Prismal glass effect chain used by glass components.
 *
 * Combines adaptive or manual blur, optional vibrancy / color controls, and edge lens
 * refraction into a single [PrismalGlassEffectProvider] block.
 *
 * @param density Current density for dp-to-px conversion.
 * @param adaptiveLuminance When true, blur and brightness are driven by [luminance]
 *   using [adaptiveTuning]. [blurRadiusPx] is still used as the **base** blur that
 *   adaptive mode scales (it is not ignored).
 * @param luminance Normalized backdrop brightness in `[0, 1]` (used when adaptive).
 * @param blurRadiusPx Manual blur radius when adaptive is off; when adaptive is on,
 *   this is the base radius scaled by [PrismalAdaptiveTuning.maxBlurScale] /
 *   [PrismalAdaptiveTuning.minBlurScale]. If `≤ 0` while adaptive, falls back to `8.dp`.
 * @param refractionHeightPx Lens zone height at the shape edge.
 * @param refractionAmountPx Lens displacement strength.
 * @param brightness Manual brightness offset when adaptive mode is off.
 * @param saturation Manual saturation when adaptive mode is off.
 * @param depthEffect Passed through to [prismalLens].
 * @param chromaticAberration Passed through to [prismalLens].
 * @param useVibrancy When true and not adaptive, applies [vibrancy] instead of [colorControls].
 * @param adaptiveTuning Caps / scales for adaptive remapping; use
 *   [PrismalAdaptiveTuning.Subtle] for quieter UI chrome.
 */
fun PrismalGlassEffectProvider.applyPrismalGlassEffects(
    density: Density,
    adaptiveLuminance: Boolean,
    luminance: Float,
    blurRadiusPx: Float,
    refractionHeightPx: Float,
    refractionAmountPx: Float,
    brightness: Float = 0f,
    saturation: Float = 1.5f,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
    useVibrancy: Boolean = true,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
) {
    if (adaptiveLuminance) {
        val l = (luminance * 2f - 1f).let { sign(it) * it * it }
        val tuning = adaptiveTuning
        colorControls(
            brightness =
                if (l > 0f) lerp(tuning.midBrightness, tuning.maxBrightness, l)
                else lerp(tuning.midBrightness, tuning.minBrightness, -l),
            contrast =
                if (l > 0f) lerp(1f, tuning.minContrast, l)
                else 1f,
            saturation = 1.5f
        )
        val baseBlur =
            if (blurRadiusPx > 0f) blurRadiusPx
            else with(density) { 8.dp.toPx() }
        val maxBlur = baseBlur * tuning.maxBlurScale
        val minBlur = (baseBlur * tuning.minBlurScale).coerceAtLeast(1f)
        prismalBlur(
            if (l > 0f) lerp(baseBlur, maxBlur, l)
            else lerp(baseBlur, minBlur, -l)
        )
    } else {
        if (useVibrancy) {
            vibrancy()
        } else {
            colorControls(brightness = brightness, saturation = saturation)
        }
        if (blurRadiusPx > 0f) {
            prismalBlur(blurRadiusPx)
        }
    }

    if (PrismalGlass.supportsRefraction && refractionHeightPx > 0f && refractionAmountPx > 0f) {
        prismalLens(
            refractionHeight = refractionHeightPx,
            refractionAmount = refractionAmountPx,
            depthEffect = depthEffect,
            chromaticAberration = chromaticAberration
        )
    }
}
