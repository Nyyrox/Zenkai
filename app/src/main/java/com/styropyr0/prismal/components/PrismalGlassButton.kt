package com.styropyr0.prismal.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalDefaultTintAlpha
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.drawPrismalGlassTint
import com.styropyr0.prismal.effects.PrismalAdaptiveTuning
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.interactive.PrismalPressRipple
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val DefaultButtonShape: () -> Shape = { PrismalCapsule() }
private val DefaultButtonSpecular: (() -> PrismalSpecular?) = { PrismalSpecular.Default }
private val DefaultButtonDepthShadow: (() -> PrismalDepthShadow?) = { PrismalDepthShadow.Default }
private val DefaultButtonContentPadding = PaddingValues(horizontal = 16.dp)
private val DefaultButtonArrangement =
    Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)

/**
 * Capsule-shaped glass button with press ripple, scale feedback, and optional tint.
 *
 * @param onClick Click handler.
 * @param backdrop Source sampled through the glass.
 * @param isInteractive When false, disables press animations.
 * @param adaptiveLuminance When true, blur/brightness follow [luminance].
 * @param blurRadius Backdrop blur radius. With adaptive luminance, this is the base
 *   radius scaled by [adaptiveTuning] — it is not ignored.
 * @param height Fixed row height, or `null` to let [modifier] / content drive size.
 * @param tint Optional color overlay blended onto the glass surface.
 * @param tintAlpha Overlay alpha for [tint] (default keeps colored glass see-through).
 * @param surfaceColor Optional solid fill drawn on the glass surface.
 * @param adaptiveTuning Caps / scales when [adaptiveLuminance] is true.
 * @param shape Glass outline, capsule by default.
 * @param refractionHeight Thickness of the refracting edge band.
 * @param refractionAmount How far the edge band bends the backdrop.
 * @param effects Replaces the built-in effect stack entirely when non-null.
 * @param specular / @param depthShadow / @param depthInset Pass `null` to disable a layer.
 * @param nestedGlassSource Layer this button contributes to for nested glass.
 * @param pressLift Extra scale-up applied while pressed.
 * @param contentPadding Padding around [content].
 * @param onDrawSurface Extra surface drawing, applied after tint and [surfaceColor].
 */
@Composable
fun PrismalGlassButton(
    onClick: () -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f },
    blurRadius: Dp = 8.dp,
    height: Dp? = 48.dp,
    tint: Color = Color.Unspecified,
    tintAlpha: Float = PrismalDefaultTintAlpha,
    surfaceColor: Color = Color.Unspecified,
    adaptiveTuning: PrismalAdaptiveTuning = PrismalAdaptiveTuning.Standard,
    shape: () -> Shape = DefaultButtonShape,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    brightness: Float = 0f,
    saturation: Float = 1.5f,
    chromaticAberration: Float = 0f,
    depthEffect: Boolean = false,
    useVibrancy: Boolean = true,
    effects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    specular: (() -> PrismalSpecular?)? = DefaultButtonSpecular,
    depthShadow: (() -> PrismalDepthShadow?)? = DefaultButtonDepthShadow,
    depthInset: (() -> PrismalDepthInset?)? = null,
    nestedGlassSource: PrismalGlassLayer? = null,
    pressLift: Dp = 4.dp,
    contentPadding: PaddingValues = DefaultButtonContentPadding,
    horizontalArrangement: Arrangement.Horizontal = DefaultButtonArrangement,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val pressLiftPx = with(density) { pressLift.toPx() }
    val animationScope = rememberCoroutineScope()
    val interactivePrismalSpecular = remember(animationScope, isInteractive) {
        if (isInteractive) PrismalPressRipple(animationScope = animationScope) else null
    }

    Row(
        modifier
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = shape,
                effects = {
                    val currentLuminance = luminance()
                    if (effects != null) {
                        effects(currentLuminance)
                    } else {
                        applyPrismalGlassEffects(
                            density = density,
                            adaptiveLuminance = adaptiveLuminance,
                            luminance = currentLuminance,
                            blurRadiusPx = with(density) { blurRadius.toPx() },
                            refractionHeightPx = with(density) { refractionHeight.toPx() },
                            refractionAmountPx = with(density) { refractionAmount.toPx() },
                            brightness = brightness,
                            saturation = saturation,
                            depthEffect = depthEffect,
                            chromaticAberration = chromaticAberration,
                            useVibrancy = useVibrancy,
                            adaptiveTuning = adaptiveTuning,
                        )
                    }
                },
                specular = specular,
                depthShadow = depthShadow,
                depthInset = depthInset,
                nestedGlassSource = nestedGlassSource,
                layerBlock = if (interactivePrismalSpecular != null) {
                    {
                        val width = size.width
                        val heightPx = size.height
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
                                (width / heightPx).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (heightPx / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    drawPrismalGlassTint(tint, tintAlpha)
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                    onDrawSurface?.invoke(this)
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (interactivePrismalSpecular != null) {
                    Modifier
                        .then(interactivePrismalSpecular.modifier)
                        .then(interactivePrismalSpecular.gestureModifier)
                } else {
                    Modifier
                }
            )
            .then(if (height != null) Modifier.height(height) else Modifier)
            .padding(contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}
