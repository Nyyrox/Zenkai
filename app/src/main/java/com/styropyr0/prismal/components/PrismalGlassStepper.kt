package com.styropyr0.prismal.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.interactive.PrismalPressRipple
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val StepperDecrement = 0
private const val StepperIncrement = 1

private const val StepperRepeatDelayMs = 400L
private const val StepperRepeatIntervalMs = 80L

/**
 * iOS-style − / + stepper on a glass capsule.
 *
 * Pressing a half lights the glass with a glow centered on the touch, like
 * [PrismalGlassButton]. Holding a half repeats the step until release when
 * [repeatOnHold] is true.
 *
 * @param value Current value.
 * @param onValueChange Called with the new value, already clamped to [valueRange].
 * @param backdrop Source sampled through the stepper glass.
 * @param valueRange Allowed values; the half at a bound is shown disabled.
 * @param step Amount added or subtracted per press.
 * @param repeatOnHold Keep stepping while a half is held down.
 * @param glyphColor Color of the − and + glyphs. Defaults to black / white by theme.
 * @param effects Replaces the capsule's built-in effect stack when non-null.
 * @param specular Edge highlight for the capsule; pass `null` to disable.
 */
@Composable
fun PrismalGlassStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    valueRange: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
    step: Int = 1,
    repeatOnHold: Boolean = true,
    glyphColor: Color = Color.Unspecified,
    width: Dp = 104.dp,
    height: Dp = 40.dp,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f },
    effects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
) {
    val density = LocalDensity.current
    val isLightTheme = !isSystemInDarkTheme()
    val resolvedGlyphColor =
        if (glyphColor.isSpecified) glyphColor
        else if (isLightTheme) Color.Black else Color.White
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val currentValue by rememberUpdatedState(value)
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val canDecrement = value > valueRange.first
    val canIncrement = value < valueRange.last

    val animationScope = rememberCoroutineScope()
    val decrementRipple = remember(animationScope, isLtr) {
        stepperRipple(animationScope, onRightHalf = !isLtr)
    }
    val incrementRipple = remember(animationScope, isLtr) {
        stepperRipple(animationScope, onRightHalf = isLtr)
    }

    val parentGlassLayer = LocalPrismalParentGlassLayer.current
    val underlayBackdrop = parentGlassLayer ?: backdrop
    parentGlassLayer?.readSamplingState()

    fun stepFrom(from: Int, side: Int): Int {
        val next = if (side == StepperIncrement) from.toLong() + step else from.toLong() - step
        return next.coerceIn(valueRange.first.toLong(), valueRange.last.toLong()).toInt()
    }

    fun halfModifier(side: Int, enabled: Boolean): Modifier {
        val ripple = if (side == StepperIncrement) incrementRipple else decrementRipple
        return Modifier
            .semantics {
                role = Role.Button
                contentDescription = if (side == StepperIncrement) "Increase" else "Decrease"
                if (!enabled) disabled()
                onClick {
                    val next = stepFrom(currentValue, side)
                    if (next != currentValue) onValueChangeState.value(next)
                    true
                }
            }
            .pointerInput(side, enabled, repeatOnHold, step, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        var latest = currentValue
                        var repeated = false
                        val repeatJob: Job? =
                            if (repeatOnHold) {
                                animationScope.launch {
                                    delay(StepperRepeatDelayMs.milliseconds)
                                    while (isActive) {
                                        val next = stepFrom(latest, side)
                                        if (next == latest) break
                                        repeated = true
                                        latest = next
                                        onValueChangeState.value(next)
                                        delay(StepperRepeatIntervalMs.milliseconds)
                                    }
                                }
                            } else {
                                null
                            }

                        val released = tryAwaitRelease()
                        repeatJob?.cancel()
                        if (released && !repeated) {
                            val next = stepFrom(currentValue, side)
                            if (next != currentValue) onValueChangeState.value(next)
                        }
                    }
                )
            }
            .then(if (enabled) ripple.gestureModifier else Modifier)
    }

    Box(modifier.size(width, height)) {
        Row(
            Modifier
                .fillMaxSize()
                .drawPrismalGlass(
                    backdrop = underlayBackdrop,
                    shape = { PrismalCapsule() },
                    effects = {
                        val currentLuminance = luminance()
                        if (effects != null) {
                            effects(currentLuminance)
                        } else {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = adaptiveLuminance,
                                luminance = currentLuminance,
                                blurRadiusPx = with(density) { 8.dp.toPx() },
                                refractionHeightPx = with(density) { 10.dp.toPx() },
                                refractionAmountPx = with(density) { 16.dp.toPx() }
                            )
                        }
                    },
                    specular = specular,
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(decrementRipple.modifier)
                .then(incrementRipple.modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperGlyph(
                plus = false,
                color = resolvedGlyphColor,
                enabled = canDecrement,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(halfModifier(StepperDecrement, canDecrement))
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.45f)
                    .background(resolvedGlyphColor.copy(alpha = 0.18f))
            )
            StepperGlyph(
                plus = true,
                color = resolvedGlyphColor,
                enabled = canIncrement,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(halfModifier(StepperIncrement, canIncrement))
            )
        }
    }
}

private fun stepperRipple(animationScope: CoroutineScope, onRightHalf: Boolean) =
    PrismalPressRipple(animationScope) { size, offset ->
        if (onRightHalf) offset + Offset(size.width / 2f, 0f) else offset
    }

@Composable
private fun StepperGlyph(
    plus: Boolean,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(14.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
        ) {
            val strokeWidth = 2.dp.toPx()
            val center = size.width / 2f
            drawLine(
                color = color,
                start = Offset(0f, center),
                end = Offset(size.width, center),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            if (plus) {
                drawLine(
                    color = color,
                    start = Offset(center, 0f),
                    end = Offset(center, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}