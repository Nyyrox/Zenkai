package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings

/**
 * Applies authentic iOS frosted glass aesthetics with layered depth,
 * multi-stop specular border reflection, and optional shadow.
 */
fun Modifier.iosGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color(0xCC0E131F),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp
): Modifier = this
    .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
    .clip(shape)
    .background(backgroundColor)
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            listOf(
                Color(0x40FFFFFF),
                Color(0x18FFFFFF),
                Color(0x06FFFFFF),
                Color(0x22FFFFFF)
            )
        ),
        shape = shape
    )

/**
 * High-end interactive button featuring iOS glossy specular reflection,
 * fluid spring physics, and animated press deformation.
 */
@Composable
fun IosLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundBrush: Brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))),
    contentColor: Color = Color.White,
    content: @Composable RowScope.() -> Unit
) {
    val themeSettings = LocalThemeSettings.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = if (themeSettings.isReducedMotionEnabled) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "ios_liquid_press"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x60FFFFFF),
                        Color(0x15FFFFFF)
                    )
                ),
                shape = shape
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Glossy Specular Top Sheen (iOS light refraction simulation)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x38FFFFFF),
                        0.45f to Color(0x08FFFFFF),
                        1.0f to Color.Transparent
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
