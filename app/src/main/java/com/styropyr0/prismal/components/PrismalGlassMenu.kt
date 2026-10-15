package com.styropyr0.prismal.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedCornerStyle
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlin.math.min

private val MenuMorphSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 520f)
private val MenuMinWidth = 180.dp
private val MenuMaxWidth = 280.dp
private val MenuSeedSize = 30.dp
private val MenuAnchorGap = 4.dp
private val MenuScreenMargin = 16.dp
private val MenuItemMinHeight = 44.dp
private val MenuItemHorizontalPadding = 16.dp
private val DefaultMenuItemTextStyle = TextStyle(fontSize = 17.sp)

/**
 * Reports this element's bounds in window coordinates, for use as [PrismalGlassMenu]'s
 * `anchorBounds`.
 */
fun Modifier.prismalMenuAnchor(onBoundsChanged: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates -> onBoundsChanged(coordinates.boundsInWindow()) }

/**
 * Glass popup menu that grows out of a droplet at the anchor's end edge and morphs into a
 * rounded panel.
 *
 * The menu opens below the anchor, or above it when there is not enough room. Its height
 * follows [content]; its width follows the anchor, clamped to 180–280 dp unless [width]
 * is given.
 *
 * @param expanded Whether the menu is open. The close animation plays after it turns false.
 * @param onDismissRequest Called on outside taps and back presses.
 * @param anchorBounds Anchor bounds in window coordinates, e.g. from [prismalMenuAnchor].
 * @param backdrop Source sampled through the menu glass. The menu is shown in a dialog
 *   window, so pass a backdrop that covers the whole screen.
 * @param width Fixed menu width, or `null` to derive it from the anchor.
 * @param cornerRadius Corner radius of the open panel.
 * @param surfaceColor Fill drawn on the glass for legibility. Defaults to a light / dark
 *   modal material.
 * @param effects Replaces the built-in effect stack when non-null.
 * @param specular Edge highlight; pass `null` to disable.
 * @param content Menu rows, typically [PrismalGlassMenuItem] and [PrismalGlassMenuDivider].
 */
@Composable
fun PrismalGlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorBounds: Rect,
    backdrop: PrismalBackdrop,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    cornerRadius: Dp = 24.dp,
    surfaceColor: Color = Color.Unspecified,
    scrimColor: Color = Color.Black.copy(alpha = 0.12f),
    effects: (PrismalGlassEffectProvider.() -> Unit)? = null,
    specular: (() -> PrismalSpecular?)? = { PrismalSpecular.Default },
    content: @Composable ColumnScope.() -> Unit
) {
    if (anchorBounds == Rect.Zero) return

    val density = LocalDensity.current
    val isLightTheme = !isSystemInDarkTheme()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val resolvedSurfaceColor =
        if (surfaceColor.isSpecified) surfaceColor
        else if (isLightTheme) Color.White.copy(alpha = 0.58f)
        else Color(0xFF1C1C1E).copy(alpha = 0.82f)

    backdrop.readSamplingState()

    val transition = updateTransition(expanded, label = "prismalGlassMenu")
    val morph by transition.animateFloat(
        transitionSpec = {
            if (initialState != targetState) MenuMorphSpring else snap()
        },
        label = "morph"
    ) { open -> if (open) 1f else 0f }
    val closing = !transition.targetState
    var contentHeightPx by remember { mutableIntStateOf(0) }

    if (!expanded && morph <= 0.2f) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            val marginPx = with(density) { MenuScreenMargin.toPx() }
            val gapPx = with(density) { MenuAnchorGap.toPx() }
            val seedPx = with(density) { MenuSeedSize.toPx() }

            val menuWidthPx = with(density) {
                width?.toPx()
                    ?: anchorBounds.width.coerceIn(MenuMinWidth.toPx(), MenuMaxWidth.toPx())
            }.coerceAtMost(screenWidthPx - marginPx * 2f)
            val menuHeightPx = contentHeightPx.toFloat()
                .coerceAtMost(screenHeightPx - marginPx * 2f)

            val targetX =
                (if (isLtr) anchorBounds.right - menuWidthPx else anchorBounds.left)
                    .coerceIn(marginPx, (screenWidthPx - menuWidthPx - marginPx).coerceAtLeast(marginPx))
            val opensAbove = anchorBounds.bottom + gapPx + menuHeightPx > screenHeightPx - marginPx
            val targetY =
                (if (opensAbove) anchorBounds.top - menuHeightPx - gapPx else anchorBounds.bottom + gapPx)
                    .coerceAtLeast(marginPx)

            val seedX = if (isLtr) anchorBounds.right - seedPx else anchorBounds.left
            val seedY = anchorBounds.top + (anchorBounds.height - seedPx) / 2f

            val popupX = lerp(seedX, targetX, morph)
            val popupY = lerp(seedY, targetY, morph)

            val rawWidthPx = lerp(seedPx, menuWidthPx, morph)
            val rawHeightPx = lerp(seedPx, menuHeightPx.coerceAtLeast(seedPx), morph)
            val squareBias = (1f - morph).coerceIn(0f, 1f).let { it * it }
            val squareSidePx = maxOf(rawWidthPx, rawHeightPx)
            val popupWidthPx = lerp(rawWidthPx, squareSidePx, squareBias)
            val popupHeightPx = lerp(rawHeightPx, squareSidePx, squareBias)

            val popupAlpha =
                if (closing) ((morph - 0.2f) / 0.8f).coerceIn(0f, 1f)
                else (morph / 0.35f).coerceIn(0f, 1f)

            val cornerDp = with(density) {
                lerp(MenuSeedSize.toPx() / 2f, cornerRadius.toPx(), morph)
                    .coerceAtMost(min(popupWidthPx, popupHeightPx) / 2f)
                    .toDp()
            }
            val popupShape: () -> Shape = {
                PrismalRoundedRectangle(cornerDp, PrismalRoundedCornerStyle.Continuous)
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = scrimColor.alpha * popupAlpha))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onDismissRequest
                    )
            )

            Box(
                modifier
                    .offset { IntOffset(popupX.toInt(), popupY.toInt()) }
                    .size(
                        with(density) { popupWidthPx.toDp() },
                        with(density) { popupHeightPx.toDp() }
                    )
                    .graphicsLayer { alpha = popupAlpha }
                    .drawPrismalGlass(
                        backdrop = backdrop,
                        shape = popupShape,
                        effects = effects ?: {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = false,
                                luminance = 0.3f,
                                blurRadiusPx = with(density) { 12.dp.toPx() },
                                refractionHeightPx = with(density) { 16.dp.toPx() },
                                refractionAmountPx = with(density) { 32.dp.toPx() },
                                useVibrancy = true
                            )
                        },
                        specular = specular,
                        depthShadow = { PrismalDepthShadow.Default },
                        onDrawSurface = { drawRect(resolvedSurfaceColor) }
                    )
                    .then(
                        if (closing) {
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent(PointerEventPass.Initial)
                                            .changes
                                            .forEach { it.consume() }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .width(with(density) { menuWidthPx.toDp() })
                        .onSizeChanged { contentHeightPx = it.height },
                    content = content
                )
            }
        }
    }
}

/**
 * Single row for [PrismalGlassMenu].
 *
 * @param selected Draws the row in [selectedColor] with a trailing checkmark.
 * @param leadingIcon Optional icon drawn before [text].
 */
@Composable
fun PrismalGlassMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    textStyle: TextStyle = DefaultMenuItemTextStyle,
    textColor: Color = Color.Unspecified,
    selectedColor: Color = Color.Unspecified,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val resolvedTextColor =
        if (textColor.isSpecified) textColor
        else if (isLightTheme) Color.Black else Color.White
    val resolvedSelectedColor =
        if (selectedColor.isSpecified) selectedColor
        else if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val color = when {
        !enabled -> resolvedTextColor.copy(alpha = 0.3f)
        selected -> resolvedSelectedColor
        else -> resolvedTextColor
    }

    Row(
        modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = MenuItemMinHeight)
            .padding(horizontal = MenuItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(12.dp))
        }
        BasicText(
            text = text,
            style = textStyle,
            color = { color },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            MenuCheckmark(color = resolvedSelectedColor)
        }
    }
}

/** Hairline separator between [PrismalGlassMenuItem] rows. */
@Composable
fun PrismalGlassMenuDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val resolvedColor =
        if (color.isSpecified) color
        else if (isLightTheme) Color.Black.copy(alpha = 0.12f)
        else Color.White.copy(alpha = 0.15f)
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(resolvedColor)
    )
}

@Composable
private fun MenuCheckmark(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.52f)
            lineTo(size.width * 0.4f, size.height * 0.8f)
            lineTo(size.width * 0.88f, size.height * 0.22f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
