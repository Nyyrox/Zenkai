package com.styropyr0.prismal.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.prismalLens
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val RulerHeight = 70.dp
private val RulerDropletHeight = 45.dp
private val RulerDropletExtraWidth = 50.dp
private val RulerDropletPadding = 5.dp
private val RulerValueTickGap = 4.dp
private val RulerTickSpacing = 10.dp
private val RulerMinorTickHeight = 14.dp
private val RulerMajorTickHeight = 24.dp
private val RulerTickStroke = 1.5.dp

private val RulerSnapSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 580f,
)

/**
 * Horizontal ruler selector with a fixed liquid-glass droplet and scrolling tick marks.
 *
 * The selected value is rendered inside the droplet. Ticks scroll through the glass
 * and leave a gap only under the label: `||||||| <value> |||||||`
 *
 * [onValueChange] is invoked continuously as the focused tick changes while scrolling,
 * and again when the ruler snaps after release.
 */
@Composable
fun PrismalRulerSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    step: Int = 1,
    majorTickEvery: Int = 5,
    tickSpacing: Dp = RulerTickSpacing,
    valueLabel: (Int) -> String = { it.toString() },
    textStyle: TextStyle,
    textColor: Color,
    tickColor: Color = textColor.copy(alpha = 0.55f),
    dropletExtraWidth: Dp = RulerDropletExtraWidth,
    dropletPadding: Dp = RulerDropletPadding,
    luminance: () -> Float = { 0.5f },
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    dropletEffects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    onDrawDropletSurface: (DrawScope.(luminance: Float) -> Unit)? = null,
    chromaticAberration: Float = 0.3f,
    dropletShadow: Boolean = false,
) {
    require(step > 0) { "step must be > 0" }
    require(!valueRange.isEmpty()) { "valueRange must not be empty" }

    val steps = ((valueRange.last - valueRange.first) / step).coerceAtLeast(0)
    val valueCount = steps + 1
    if (valueCount <= 0) return

    fun valueAt(index: Int): Int =
        (valueRange.first + index * step).coerceIn(valueRange.first, valueRange.last)

    fun indexOf(v: Int): Int {
        val clamped = v.coerceIn(valueRange.first, valueRange.last)
        return ((clamped - valueRange.first).toFloat() / step)
            .roundToInt()
            .coerceIn(0, valueCount - 1)
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val ticksBackdrop = rememberPrismalGlassLayer()
    val dropletBackdrop = rememberPrismalMergedSource(backdrop, ticksBackdrop)
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val valueLabelState = rememberUpdatedState(valueLabel)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = textStyle.copy(
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum",
    )
    val dropletChromaticAberration = chromaticAberration.coerceIn(0f, 1f)

    ticksBackdrop.readSamplingState()

    val tickSpacingPx = with(density) { tickSpacing.toPx() }
    val minorTickHeightPx = with(density) { RulerMinorTickHeight.toPx() }
    val majorTickHeightPx = with(density) { RulerMajorTickHeight.toPx() }
    val tickStrokePx = with(density) { RulerTickStroke.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(RulerHeight)
            .clipToBounds()
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val contentWidthPx = steps * tickSpacingPx
        val sideSpacerPx = viewportWidthPx / 2f

        fun scrollForIndex(index: Int): Int =
            (index * tickSpacingPx).roundToInt().coerceAtLeast(0)

        fun indexForScroll(scrollPx: Int): Int {
            if (valueCount <= 1) return 0
            return (scrollPx / tickSpacingPx)
                .roundToInt()
                .coerceIn(0, valueCount - 1)
        }

        var focusedIndex by remember(valueCount) {
            mutableIntStateOf(indexOf(value))
        }
        var lockedIndex by remember(valueCount) {
            mutableIntStateOf(indexOf(value))
        }
        var isScrolling by remember { mutableIntStateOf(0) }

        suspend fun snapToIndex(index: Int) {
            val targetOffset = scrollForIndex(index)
            val delta = targetOffset - scrollState.value
            if (abs(delta) > 0.5f) {
                scrollState.animateScrollBy(delta.toFloat(), RulerSnapSpring)
            }
            lockedIndex = index
            focusedIndex = index
            onValueChangeState.value(valueAt(index))
        }

        LaunchedEffect(value, valueCount, tickSpacingPx) {
            val target = indexOf(value)
            if (isScrolling == 0) {
                snapToIndex(target)
            }
        }

        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { inProgress ->
                    if (inProgress) {
                        isScrolling = 1
                    } else {
                        scope.launch {
                            snapToIndex(indexForScroll(scrollState.value))
                            isScrolling = 0
                        }
                    }
                }
        }

        LaunchedEffect(scrollState, valueCount, tickSpacingPx) {
            snapshotFlow { indexForScroll(scrollState.value) }
                .distinctUntilChanged()
                .collect { index ->
                    focusedIndex = index
                    onValueChangeState.value(valueAt(index))
                }
        }

        val displayIndex = if (isScrolling == 1) focusedIndex else lockedIndex
        val displayValue = valueAt(displayIndex)
        val displayLabel = valueLabelState.value(displayValue)

        val maxLabelWidthPx = remember(valueRange, step, valueCount, labelStyle) {
            var maxWidth = 0f
            val labelOf = valueLabelState.value
            for (index in 0 until valueCount) {
                val width = textMeasurer.measure(
                    text = labelOf(valueAt(index)),
                    style = labelStyle,
                    maxLines = 1,
                ).size.width.toFloat()
                if (width > maxWidth) maxWidth = width
            }
            maxWidth
        }
        val dropletHorizontalInsetPx = with(density) { (dropletExtraWidth + dropletPadding * 2).toPx() }
        val dropletWidthPx = maxLabelWidthPx + dropletHorizontalInsetPx
        val dropletWidth = with(density) { dropletWidthPx.toDp() }
        val tickGapPx = with(density) { RulerValueTickGap.toPx() }
        val sampleHiddenHalfWidth = maxLabelWidthPx / 2f + tickGapPx
        val visibleHiddenHalfWidth = dropletWidthPx / 2f

        fun DrawScope.drawTicks(hiddenHalfWidth: Float) {
            val centerY = size.height / 2f
            val fadeRadius = viewportWidthPx / 2f
            val viewportCenterX = viewportWidthPx / 2f

            for (index in 0 until valueCount) {
                val x = index * tickSpacingPx
                val screenX = sideSpacerPx + x - scrollState.value
                if (abs(screenX - viewportCenterX) < hiddenHalfWidth) continue

                val distanceFromCenter = abs(screenX - viewportCenterX) / fadeRadius
                val focus = (1f - distanceFromCenter.coerceIn(0f, 1f))
                val alpha = lerp(0.22f, 1f, focus)
                val isMajor = index % majorTickEvery == 0
                val tickHeight = if (isMajor) majorTickHeightPx else minorTickHeightPx

                drawLine(
                    color = tickColor.copy(alpha = tickColor.alpha * alpha),
                    start = Offset(x, centerY - tickHeight / 2f),
                    end = Offset(x, centerY + tickHeight / 2f),
                    strokeWidth = tickStrokePx,
                    cap = StrokeCap.Round,
                )
            }
        }

        @Composable
        fun TicksRow(
            hiddenHalfWidth: Float,
            rowModifier: Modifier,
        ) {
            Row(
                rowModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(with(density) { sideSpacerPx.toDp() }))

                Canvas(
                    Modifier
                        .width(with(density) { contentWidthPx.toDp().coerceAtLeast(0.dp) })
                        .fillMaxHeight(),
                    onDraw = { drawTicks(hiddenHalfWidth) },
                )

                Spacer(Modifier.width(with(density) { sideSpacerPx.toDp() }))
            }
        }

        Box(Modifier.matchParentSize()) {
            TicksRow(
                hiddenHalfWidth = sampleHiddenHalfWidth,
                rowModifier = Modifier
                    .alpha(0f)
                    .fillMaxWidth()
                    .prismalGlassLayer(ticksBackdrop)
                    .horizontalScroll(scrollState, enabled = false)
                    .align(Alignment.Center),
            )

            TicksRow(
                hiddenHalfWidth = visibleHiddenHalfWidth,
                rowModifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .align(Alignment.Center),
            )

            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(dropletWidth)
                    .height(RulerDropletHeight + dropletPadding * 2)
                    .drawPrismalGlass(
                        backdrop = dropletBackdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            val currentLuminance = luminance()
                            if (dropletEffects != null) {
                                dropletEffects(currentLuminance)
                            } else {
                                applyPrismalGlassEffects(
                                    density = density,
                                    adaptiveLuminance = false,
                                    luminance = currentLuminance,
                                    blurRadiusPx = with(density) { 0.dp.toPx() },
                                    refractionHeightPx = 0f,
                                    refractionAmountPx = 0f,
                                    useVibrancy = true,
                                )
                                prismalLens(
                                    refractionHeight = with(density) { 25.dp.toPx() },
                                    refractionAmount = with(density) { 16.dp.toPx() },
                                    chromaticAberration = dropletChromaticAberration,
                                )
                            }
                        },
                        specular = specular,
                        depthShadow = if (dropletShadow) ({ PrismalDepthShadow() }) else null,
                        depthInset = {
                            PrismalDepthInset(
                                radius = 8.dp,
                                alpha = 1f,
                            )
                        },
                        onDrawSurface = {
                            val currentLuminance = luminance().coerceIn(0f, 1f)
                            if (onDrawDropletSurface != null) onDrawDropletSurface(currentLuminance)
                            else drawRect(Color.Black.copy(alpha = 0.03f))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = displayLabel,
                    style = labelStyle,
                    color = { textColor },
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
