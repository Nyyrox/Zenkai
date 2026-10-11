package com.styropyr0.prismal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.prismalBlur
import com.styropyr0.prismal.effects.prismalLens
import com.styropyr0.prismal.interactive.PrismalSpringMotion
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.sources.rememberPrismalWrappedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

private const val RangeThumbStart = 0
private const val RangeThumbEnd = 1

/**
 * Glass slider with two refracting thumbs selecting a sub-range of [valueRange].
 *
 * Drag either thumb, or tap the track to move the nearest thumb. Thumbs cannot cross and
 * stay at least [minDistance] apart.
 *
 * @param value Current range provider (read on each frame during drag).
 * @param onValueChange Called with the new range, clamped to [valueRange].
 * @param valueRange Allowed value range.
 * @param visibilityThreshold Minimum change delta before [onValueChange] fires mid-drag.
 * @param backdrop Source sampled through the track and thumb glass.
 * @param minDistance Minimum gap between the two thumbs, in value units.
 */
@Composable
fun PrismalRangeSlider(
    value: () -> ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    minDistance: Float = 0f,
    adaptiveLuminance: Boolean = false,
    luminance: () -> Float = { 0.5f }
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberPrismalGlassLayer()
    val parentGlassLayer = LocalPrismalParentGlassLayer.current
    val underlayBackdrop = parentGlassLayer ?: backdrop

    trackBackdrop.readSamplingState()
    parentGlassLayer?.readSamplingState()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val trackWidthState = remember { mutableIntStateOf(0) }
        trackWidthState.intValue = trackWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val onValueChangeState = rememberUpdatedState(onValueChange)
        val span = valueRange.endInclusive - valueRange.start
        val gap = minDistance.coerceIn(0f, span)

        val initial = value()
        var startTarget by remember { mutableFloatStateOf(initial.start.coerceIn(valueRange)) }
        var endTarget by remember { mutableFloatStateOf(initial.endInclusive.coerceIn(valueRange)) }
        var lastReportedStart by remember { mutableFloatStateOf(startTarget) }
        var lastReportedEnd by remember { mutableFloatStateOf(endTarget) }
        var draggingThumb by remember { mutableIntStateOf(-1) }
        var didDrag by remember { mutableStateOf(false) }
        var topThumb by remember { mutableIntStateOf(RangeThumbEnd) }

        fun reportIfNeeded(force: Boolean) {
            if (
                force ||
                abs(startTarget - lastReportedStart) >= visibilityThreshold ||
                abs(endTarget - lastReportedEnd) >= visibilityThreshold
            ) {
                lastReportedStart = startTarget
                lastReportedEnd = endTarget
                onValueChangeState.value(startTarget..endTarget)
            }
        }

        fun clampStart(candidate: Float): Float =
            candidate.coerceIn(valueRange.start, (endTarget - gap).coerceAtLeast(valueRange.start))

        fun clampEnd(candidate: Float): Float =
            candidate.coerceIn((startTarget + gap).coerceAtMost(valueRange.endInclusive), valueRange.endInclusive)

        fun dragDelta(dragX: Float): Float {
            val width = trackWidthState.intValue
            if (width == 0) return 0f
            return span * (dragX / width) * if (isLtr) 1f else -1f
        }

        fun thumbMotion(thumb: Int): PrismalSpringMotion =
            PrismalSpringMotion(
                animationScope = animationScope,
                initialValue = if (thumb == RangeThumbStart) startTarget else endTarget,
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {
                    draggingThumb = thumb
                    topThumb = thumb
                    didDrag = false
                },
                onDragStopped = {
                    draggingThumb = -1
                    if (didDrag) reportIfNeeded(force = true)
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) didDrag = dragAmount.x != 0f
                    val delta = dragDelta(dragAmount.x)
                    if (thumb == RangeThumbStart) {
                        startTarget = clampStart(startTarget + delta)
                        updateValue(startTarget)
                    } else {
                        endTarget = clampEnd(endTarget + delta)
                        updateValue(endTarget)
                    }
                    reportIfNeeded(force = false)
                }
            )

        val startMotion = remember(animationScope, valueRange.start, valueRange.endInclusive, gap, isLtr) {
            thumbMotion(RangeThumbStart)
        }
        val endMotion = remember(animationScope, valueRange.start, valueRange.endInclusive, gap, isLtr) {
            thumbMotion(RangeThumbEnd)
        }

        LaunchedEffect(startMotion, endMotion) {
            snapshotFlow { value() }
                .collectLatest { current ->
                    if (draggingThumb != -1) return@collectLatest
                    val newStart = current.start.coerceIn(valueRange)
                    val newEnd = current.endInclusive.coerceIn(newStart, valueRange.endInclusive)
                    lastReportedStart = newStart
                    lastReportedEnd = newEnd
                    if (newStart != startTarget) {
                        startTarget = newStart
                        startMotion.updateValue(newStart)
                    }
                    if (newEnd != endTarget) {
                        endTarget = newEnd
                        endMotion.updateValue(newEnd)
                    }
                }
        }

        Box(Modifier.prismalGlassLayer(trackBackdrop)) {
            Box(
                Modifier
                    .clip(PrismalCapsule())
                    .background(trackColor)
                    .pointerInput(animationScope, trackWidth, valueRange, isLtr, gap) {
                        detectTapGestures { position ->
                            val fraction =
                                (position.x / trackWidth).fastCoerceIn(0f, 1f).let { if (isLtr) it else 1f - it }
                            val tapped = valueRange.start + span * fraction
                            val moveStart =
                                if (tapped <= startTarget) true
                                else if (tapped >= endTarget) false
                                else abs(tapped - startTarget) <= abs(tapped - endTarget)
                            if (moveStart) {
                                startTarget = clampStart(tapped)
                                startMotion.animateToValue(startTarget)
                                topThumb = RangeThumbStart
                            } else {
                                endTarget = clampEnd(tapped)
                                endMotion.animateToValue(endTarget)
                                topThumb = RangeThumbEnd
                            }
                            reportIfNeeded(force = true)
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val fullWidth = constraints.maxWidth
                        val startPx = (fullWidth * startMotion.progress).fastRoundToInt().coerceIn(0, fullWidth)
                        val endPx = (fullWidth * endMotion.progress).fastRoundToInt().coerceIn(startPx, fullWidth)
                        val placeable = measurable.measure(
                            Constraints.fixed(endPx - startPx, constraints.maxHeight)
                        )
                        layout(fullWidth, constraints.maxHeight) {
                            placeable.placeRelative(startPx, 0)
                        }
                    }
                    .clip(PrismalCapsule())
                    .background(accentColor)
            )
        }

        RangeThumb(
            motion = startMotion,
            trackWidth = trackWidth,
            isLtr = isLtr,
            underlayBackdrop = underlayBackdrop,
            trackBackdrop = trackBackdrop,
            adaptiveLuminance = adaptiveLuminance,
            luminance = luminance,
            modifier = Modifier
                .zIndex(if (topThumb == RangeThumbStart) 1f else 0f)
                .semantics {
                    contentDescription = "Range start"
                    progressBarRangeInfo = ProgressBarRangeInfo(startTarget, valueRange)
                    setProgress { target ->
                        startTarget = clampStart(target)
                        startMotion.animateToValue(startTarget)
                        reportIfNeeded(force = true)
                        true
                    }
                }
        )
        RangeThumb(
            motion = endMotion,
            trackWidth = trackWidth,
            isLtr = isLtr,
            underlayBackdrop = underlayBackdrop,
            trackBackdrop = trackBackdrop,
            adaptiveLuminance = adaptiveLuminance,
            luminance = luminance,
            modifier = Modifier
                .zIndex(if (topThumb == RangeThumbEnd) 1f else 0f)
                .semantics {
                    contentDescription = "Range end"
                    progressBarRangeInfo = ProgressBarRangeInfo(endTarget, valueRange)
                    setProgress { target ->
                        endTarget = clampEnd(target)
                        endMotion.animateToValue(endTarget)
                        reportIfNeeded(force = true)
                        true
                    }
                }
        )
    }
}

@Composable
private fun RangeThumb(
    motion: PrismalSpringMotion,
    trackWidth: Int,
    isLtr: Boolean,
    underlayBackdrop: PrismalBackdrop,
    trackBackdrop: PrismalBackdrop,
    adaptiveLuminance: Boolean,
    luminance: () -> Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(
        modifier
            .graphicsLayer {
                translationX =
                    (-size.width / 2f + trackWidth * motion.progress)
                        .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) *
                        if (isLtr) 1f else -1f
            }
            .then(motion.modifier)
            .drawPrismalGlass(
                backdrop = rememberPrismalMergedSource(
                    underlayBackdrop,
                    rememberPrismalWrappedSource(trackBackdrop) { drawPrismalGlass ->
                        val progress = motion.pressProgress
                        val scaleX = lerp(2f / 3f, 1f, progress)
                        val scaleY = lerp(0f, 1f, progress)
                        scale(scaleX, scaleY) {
                            drawPrismalGlass()
                        }
                    }
                ),
                shape = { PrismalCapsule() },
                effects = {
                    val progress = motion.pressProgress
                    if (adaptiveLuminance) {
                        applyPrismalGlassEffects(
                            density = density,
                            adaptiveLuminance = true,
                            luminance = luminance(),
                            blurRadiusPx = with(density) { 8.dp.toPx() } * (1f - progress),
                            refractionHeightPx = with(density) { 10.dp.toPx() } * progress,
                            refractionAmountPx = with(density) { 14.dp.toPx() } * progress,
                            chromaticAberration = 1f
                        )
                    } else {
                        prismalBlur(with(density) { 8.dp.toPx() } * (1f - progress))
                        prismalLens(
                            refractionHeight = with(density) { 10.dp.toPx() } * progress,
                            refractionAmount = with(density) { 14.dp.toPx() } * progress,
                            chromaticAberration = 1f
                        )
                    }
                },
                specular = {
                    PrismalSpecular.Ambient.copy(
                        width = PrismalSpecular.Ambient.width / 1.5f,
                        blurRadius = PrismalSpecular.Ambient.blurRadius / 1.5f,
                        alpha = motion.pressProgress
                    )
                },
                depthShadow = {
                    PrismalDepthShadow(
                        radius = 4.dp,
                        color = Color.Black.copy(alpha = 0.05f)
                    )
                },
                depthInset = {
                    val progress = motion.pressProgress
                    PrismalDepthInset(
                        radius = 4.dp * progress,
                        alpha = progress
                    )
                },
                layerBlock = {
                    scaleX = motion.scaleX
                    scaleY = motion.scaleY
                    val velocity = motion.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 1f - motion.pressProgress))
                }
            )
            .size(40.dp, 24.dp)
    )
}
