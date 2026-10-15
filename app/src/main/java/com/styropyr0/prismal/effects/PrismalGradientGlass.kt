package com.styropyr0.prismal.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.internal.PrismalGradientGlassShader
import com.styropyr0.prismal.isAGSLShaderSupported
import kotlin.math.sign

/**
 * Progressive blur + vertically-weighted refraction for gradient glass panels.
 *
 * [refractionTopWeight], [refractionMiddleWeight], and [refractionBottomWeight] control where
 * refraction peaks. When middle weight dominates, refraction fades in both directions from center.
 *
 * When [adaptiveLuminance] is true, [blurRadiusPx] is the **base** blur scaled by
 * [adaptiveTuning] (same contract as [applyPrismalGlassEffects]).
 */
fun PrismalGlassEffectProvider.prismalGradientGlass(
    density: Density,
    adaptiveLuminance: Boolean,
    luminance: Float,
    blurRadiusPx: Float,
    refractionHeightPx: Float,
    refractionAmountPx: Float,
    refractionTopWeight: Float = 0f,
    refractionMiddleWeight: Float = 0f,
    refractionBottomWeight: Float = 1f,
    blurFadeStart: Float = 0f,
    blurFadeEnd: Float = 0.75f,
    tint: Color = Color.Transparent,
    tintIntensity: Float = 0f,
    chromaticAberration: Float = 0f,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
) {
    var effectiveBlur = blurRadiusPx
    if (adaptiveLuminance) {
        val l = (luminance * 2f - 1f).let { sign(it) * it * it }
        val tuning = adaptiveTuning
        val baseBlur =
            if (blurRadiusPx > 0f) blurRadiusPx
            else with(density) { 8.dp.toPx() }
        val maxBlur = baseBlur * tuning.maxBlurScale
        val minBlur = (baseBlur * tuning.minBlurScale).coerceAtLeast(1f)
        effectiveBlur =
            if (l > 0f) lerp(baseBlur, maxBlur, l)
            else lerp(baseBlur, minBlur, -l)
    }

    if (effectiveBlur > 0f) {
        prismalBlur(effectiveBlur)
    }

    if (!isAGSLShaderSupported()) {
        if (tintIntensity > 0f && tint.alpha > 0f) {
            legacyFrostStrength = maxOf(legacyFrostStrength, tintIntensity * 0.35f)
        }
        return
    }

    prismalAGSLShaderEffect(
        key = "GradientGlass",
        shaderString = PrismalGradientGlassShader,
        uniformShaderName = "content"
    ) {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("refractionHeight", refractionHeightPx)
        setFloatUniform("refractionAmount", refractionAmountPx)
        setFloatUniform(
            "topWeight",
            refractionTopWeight.coerceAtLeast(0f)
        )
        setFloatUniform(
            "middleWeight",
            refractionMiddleWeight.coerceAtLeast(0f)
        )
        setFloatUniform(
            "bottomWeight",
            refractionBottomWeight.coerceAtLeast(0f)
        )
        setFloatUniform("blurFadeStart", blurFadeStart.coerceIn(0f, 1f))
        setFloatUniform("blurFadeEnd", blurFadeEnd.coerceIn(0f, 1f))
        setColorUniform("tint", tint)
        setFloatUniform("tintIntensity", tintIntensity.coerceIn(0f, 1f))
    }
}
