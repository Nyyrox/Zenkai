package com.styropyr0.prismal.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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

private val SelectorHeight = 70.dp
private val SelectorDropletHeight = 45.dp
private val SelectorItemPadding = 14.dp
private val SelectorItemSpacing = 6.dp
private val SelectorDropletExtraWidth = 10.dp
private val SelectorDropletPadding = 5.dp

private val SelectorSnapSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 580f,
)

private val SelectorDropletWidthSpring = spring<Float>(
    dampingRatio = 0.56f,
    stiffness = 280f,
)

private enum class SelectorSubcomposeSlot {
    Main,
    Measure,
}

/**
 * iOS-style horizontal selector with a fixed liquid-glass droplet and scrolling text labels.
 *
 * Scroll to move a label under the droplet, or tap a label to scroll it there.
 *
 * @param selectOnTap When true, tapping a label selects it.
 */
@Composable
fun PrismalHorizontalSelector(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    textColor: Color,
    itemSpacing: Dp = SelectorItemSpacing,
    itemPadding: Dp = SelectorItemPadding,
    dropletExtraWidth: Dp = SelectorDropletExtraWidth,
    dropletPadding: Dp = SelectorDropletPadding,
    luminance: () -> Float = { 0.5f },
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    dropletEffects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    onDrawDropletSurface: (DrawScope.(luminance: Float) -> Unit)? = null,
    boldWhenFocused: Boolean = true,
    chromaticAberration: Float = 0.3f,
    dropletShadow: Boolean = false,
    selectOnTap: Boolean = true,
) {
    if (labels.isEmpty()) return

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = textStyle.copy(fontWeight = FontWeight.SemiBold)

    val itemContentWidthsPx = remember(labels, labelStyle) {
        labels.map { label ->
            textMeasurer.measure(label, style = labelStyle, maxLines = 1).size.width.toFloat()
        }
    }
    val itemPaddingPx = with(density) { itemPadding.toPx() }
    val itemWidthsPx = remember(itemContentWidthsPx, itemPaddingPx) {
        itemContentWidthsPx.map { it + itemPaddingPx * 2f }.toFloatArray()
    }

    PrismalHorizontalSelectorBody(
        itemCount = labels.size,
        itemWidthsPx = itemWidthsPx,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        modifier = modifier,
        itemSpacing = itemSpacing,
        dropletExtraWidth = dropletExtraWidth,
        dropletPadding = dropletPadding,
        luminance = luminance,
        specular = specular,
        dropletEffects = dropletEffects,
        onDrawDropletSurface = onDrawDropletSurface,
        chromaticAberration = chromaticAberration,
        dropletShadow = dropletShadow,
        selectOnTap = selectOnTap,
    ) { index, focus ->
        val fontWeight = if (boldWhenFocused && focus > 0.88f) FontWeight.Bold else FontWeight.Medium
        BasicText(
            text = labels[index],
            style = labelStyle.copy(fontWeight = fontWeight, textAlign = TextAlign.Center),
            color = { textColor },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * iOS-style horizontal selector with a fixed liquid-glass droplet and scrolling items.
 *
 * Items are measured once from [itemContent] before the selector is laid out.
 *
 * @param selectOnTap When true, tapping an item scrolls it under the droplet and selects it.
 */
@Composable
fun PrismalHorizontalSelector(
    itemCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = SelectorItemSpacing,
    itemPadding: Dp = SelectorItemPadding,
    dropletExtraWidth: Dp = SelectorDropletExtraWidth,
    dropletPadding: Dp = SelectorDropletPadding,
    luminance: () -> Float = { 0.5f },
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    dropletEffects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    onDrawDropletSurface: (DrawScope.(luminance: Float) -> Unit)? = null,
    chromaticAberration: Float = 0.2f,
    dropletShadow: Boolean = false,
    selectOnTap: Boolean = true,
    itemContent: @Composable (index: Int, focus: Float) -> Unit,
) {
    if (itemCount <= 0) return

    SubcomposeLayout(
        modifier
            .fillMaxWidth()
            .height(SelectorHeight)
            .clipToBounds()
    ) { constraints ->
        val measureConstraints = Constraints(
            minWidth = 0,
            maxWidth = Constraints.Infinity,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val itemWidthsPx = FloatArray(itemCount) { index ->
            subcompose(SelectorSubcomposeSlot.Measure to index) {
                Box(
                    modifier = Modifier.padding(itemPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    itemContent(index, 0f)
                }
            }.first().measure(measureConstraints).width.toFloat()
        }

        val mainPlaceable = subcompose(SelectorSubcomposeSlot.Main) {
            PrismalHorizontalSelectorBody(
                itemCount = itemCount,
                itemWidthsPx = itemWidthsPx,
                selectedIndex = selectedIndex,
                onSelected = onSelected,
                backdrop = backdrop,
                itemSpacing = itemSpacing,
                dropletExtraWidth = dropletExtraWidth,
                dropletPadding = dropletPadding,
                luminance = luminance,
                specular = specular,
                dropletEffects = dropletEffects,
                onDrawDropletSurface = onDrawDropletSurface,
                chromaticAberration = chromaticAberration,
                dropletShadow = dropletShadow,
                selectOnTap = selectOnTap,
                itemContent = itemContent,
            )
        }.first().measure(constraints)

        layout(mainPlaceable.width, mainPlaceable.height) {
            mainPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun PrismalHorizontalSelectorBody(
    itemCount: Int,
    itemWidthsPx: FloatArray,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = SelectorItemSpacing,
    dropletExtraWidth: Dp = SelectorDropletExtraWidth,
    dropletPadding: Dp = SelectorDropletPadding,
    luminance: () -> Float = { 0.5f },
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    dropletEffects: (PrismalGlassEffectProvider.(luminance: Float) -> Unit)? = null,
    onDrawDropletSurface: (DrawScope.(luminance: Float) -> Unit)? = null,
    chromaticAberration: Float = 0.2f,
    dropletShadow: Boolean = false,
    selectOnTap: Boolean = true,
    itemContent: @Composable (index: Int, focus: Float) -> Unit,
) {
    val dropletChromaticAberration = chromaticAberration.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val labelsBackdrop = rememberPrismalGlassLayer()
    val dropletBackdrop = rememberPrismalMergedSource(backdrop, labelsBackdrop)
    val onSelectedState = rememberUpdatedState(onSelected)

    labelsBackdrop.readSamplingState()

    val spacingPx = with(density) { itemSpacing.toPx() }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(SelectorHeight)
            .clipToBounds()
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val startSpacerPx = viewportWidthPx / 2f - itemWidthsPx.first() / 2f
        val endSpacerPx = viewportWidthPx / 2f - itemWidthsPx.last() / 2f

        val snapOffsetsPx = remember(itemWidthsPx, spacingPx, startSpacerPx, viewportWidthPx) {
            itemWidthsPx.indices.map { index ->
                val itemCenter =
                    startSpacerPx +
                        itemWidthsPx.take(index).sum() +
                        spacingPx * index +
                        itemWidthsPx[index] / 2f
                (itemCenter - viewportWidthPx / 2f).roundToInt().coerceAtLeast(0)
            }
        }

        fun indexForScroll(scrollPx: Int): Int {
            if (snapOffsetsPx.isEmpty()) return 0
            return snapOffsetsPx.indices.minByOrNull { abs(snapOffsetsPx[it] - scrollPx) } ?: 0
        }

        fun itemCenterInContent(index: Int): Float {
            return startSpacerPx +
                itemWidthsPx.take(index).sum() +
                spacingPx * index +
                itemWidthsPx[index] / 2f
        }

        var focusedIndex by remember(itemCount) { mutableIntStateOf(selectedIndex.coerceIn(0, itemCount - 1)) }
        var lockedIndex by remember(itemCount) { mutableIntStateOf(selectedIndex.coerceIn(0, itemCount - 1)) }
        var isScrolling by remember { mutableIntStateOf(0) }

        suspend fun snapToIndex(index: Int) {
            val targetOffset = snapOffsetsPx.getOrElse(index) { 0 }
            val delta = targetOffset - scrollState.value
            if (abs(delta) > 0.5f) {
                scrollState.animateScrollBy(delta.toFloat(), SelectorSnapSpring)
            }
            lockedIndex = index
            focusedIndex = index
            if (index != selectedIndex) {
                onSelectedState.value(index)
            }
        }

        LaunchedEffect(selectedIndex, snapOffsetsPx) {
            val target = selectedIndex.coerceIn(0, itemCount - 1)
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

        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }
                .collect { scrollPx ->
                    focusedIndex = indexForScroll(scrollPx)
                }
        }

        val dropletHorizontalInsetPx = with(density) { (dropletExtraWidth + dropletPadding * 2).toPx() }
        val dropletWidthIndex = if (isScrolling == 1) focusedIndex else lockedIndex
        val targetDropletWidthPx = itemWidthsPx[dropletWidthIndex] + dropletHorizontalInsetPx
        val dropletWidthAnim = remember(itemWidthsPx, dropletHorizontalInsetPx) {
            Animatable(targetDropletWidthPx)
        }

        LaunchedEffect(targetDropletWidthPx) {
            dropletWidthAnim.animateTo(targetDropletWidthPx, SelectorDropletWidthSpring)
        }

        val dropletWidth = with(density) { dropletWidthAnim.value.toDp() }
        val dropletWidthPx = dropletWidthAnim.value
        val dropletVolumeScaleY = (targetDropletWidthPx / dropletWidthAnim.value).coerceIn(0.96f, 1.04f)
        val viewportCenterX = viewportWidthPx / 2f

        @Composable
        fun SelectorItemsRow(
            hideUnderDroplet: Boolean,
            interactive: Boolean,
            rowModifier: Modifier,
        ) {
            Row(
                rowModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(with(density) { startSpacerPx.toDp() }))

                repeat(itemCount) { index ->
                    if (index > 0) Spacer(Modifier.width(itemSpacing))

                    val itemCenterOnScreen = itemCenterInContent(index) - scrollState.value.toFloat()
                    val distanceFromCenter = abs(itemCenterOnScreen - viewportCenterX) / (viewportWidthPx / 2f)
                    val focus = (1f - distanceFromCenter.coerceIn(0f, 1f))
                    val scale = lerp(0.9f, 1f, focus)
                    val underDroplet = hideUnderDroplet && abs(itemCenterOnScreen - viewportCenterX) < dropletWidthPx / 2f
                    val alpha = if (underDroplet) 0f else lerp(0.38f, 1f, focus)

                    Box(
                        modifier = Modifier
                            .width(with(density) { itemWidthsPx[index].toDp() })
                            .then(
                                if (interactive && selectOnTap) {
                                    Modifier.selectable(
                                        selected = index == selectedIndex,
                                        interactionSource = null,
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { scope.launch { snapToIndex(index) } },
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer {
                                this.alpha = alpha
                                scaleX = scale
                                scaleY = scale
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        itemContent(index, focus)
                    }
                }

                Spacer(Modifier.width(with(density) { endSpacerPx.toDp() }))
            }
        }

        Box(Modifier.matchParentSize()) {
            SelectorItemsRow(
                hideUnderDroplet = false,
                interactive = false,
                rowModifier = Modifier
                    .alpha(0f)
                    .fillMaxWidth()
                    .prismalGlassLayer(labelsBackdrop)
                    .horizontalScroll(scrollState, enabled = false)
                    .align(Alignment.Center),
            )

            SelectorItemsRow(
                hideUnderDroplet = false,
                interactive = true,
                rowModifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .align(Alignment.Center),
            )

            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(dropletWidth)
                    .height(SelectorDropletHeight + dropletPadding * 2)
                    .graphicsLayer {
                        scaleY = dropletVolumeScaleY
                    }
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
            )
        }
    }
}
