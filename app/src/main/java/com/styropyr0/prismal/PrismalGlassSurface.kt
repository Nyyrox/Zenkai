package com.styropyr0.prismal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.effects.PrismalAdaptiveTuning
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.interactive.PrismalPressRipple
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * General-purpose glass container with optional click handling and tint overlay.
 *
 * Samples [backdrop] through blur + lens refraction, clips to [shape], and lays out
 * [content] on top. When [onClick] is non-null, the surface scales and ripples under
 * the finger via [PrismalPressRipple].
 *
 * @param backdrop Source content refracted through the glass.
 * @param shape Clip shape for the glass panel.
 * @param onClick When non-null, enables press animation and click handling.
 * @param adaptiveLuminance When true, effect tuning follows [luminance].
 * @param blurRadius Backdrop blur radius; with adaptive luminance this is the base
 *   radius scaled by [adaptiveTuning].
 * @param tint Optional color drawn on the glass surface (Hue blend + alpha overlay).
 * @param tintAlpha Overlay alpha for [tint] (default keeps colored glass see-through).
 * @param surfaceColor Optional solid fill on the glass surface.
 * @param refractionHeightPx Edge lens zone height in pixels.
 * @param refractionAmountPx Edge lens displacement in pixels.
 * @param adaptiveTuning Caps / scales when [adaptiveLuminance] is true.
 */
@Composable
fun PrismalGlassSurface(
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    shape: () -> Shape,
    onClick: (() -> Unit)? = null,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f },
    blurRadius: Dp = 8.dp,
    tint: Color = Color.Unspecified,
    tintAlpha: Float = PrismalDefaultTintAlpha,
    surfaceColor: Color = Color.Unspecified,
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1.5f,
    refractionHeightPx: Float = 16f,
    refractionAmountPx: Float = 32f,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val pressLiftPx = with(density) { 4.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val interactivePrismalSpecular = remember(onClick, animationScope) {
        if (onClick != null) {
            PrismalPressRipple(animationScope = animationScope)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = shape,
                effects = {
                    applyPrismalGlassEffects(
                        density = density,
                        adaptiveLuminance = adaptiveLuminance,
                        luminance = luminance(),
                        blurRadiusPx = with(density) { blurRadius.toPx() },
                        refractionHeightPx = refractionHeightPx,
                        refractionAmountPx = refractionAmountPx,
                        brightness = brightness,
                        saturation = saturation,
                        depthEffect = depthEffect,
                        chromaticAberration = chromaticAberration,
                        useVibrancy = !adaptiveLuminance,
                        adaptiveTuning = adaptiveTuning,
                    )
                },
                layerBlock = if (interactivePrismalSpecular != null) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactivePrismalSpecular.pressProgress
                        val scale = lerp(1f, 1f + pressLiftPx / size.height, progress)

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactivePrismalSpecular.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = pressLiftPx / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    drawPrismalGlassTint(tint, tintAlpha)
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (interactivePrismalSpecular != null) {
                    Modifier
                        .then(interactivePrismalSpecular.modifier)
                        .then(interactivePrismalSpecular.gestureModifier)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}
