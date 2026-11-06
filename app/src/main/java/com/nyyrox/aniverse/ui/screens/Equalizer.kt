package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Equalizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = PlayerTextActive,
    height: Dp = 14.dp,
    barWidth: Dp = 2.5.dp,
    spacing: Dp = 2.5.dp
) {
    val context = LocalContext.current
    val animationsDisabled = isAnimationScaleZero(context)

    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val scale1 by if (!animationsDisabled && isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.30f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.30f) }
    }

    val scale2 by if (!animationsDisabled && isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, delayMillis = 120, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.50f) }
    }

    val scale3 by if (!animationsDisabled && isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.40f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, delayMillis = 240, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.30f) }
    }

    Row(
        modifier = modifier
            .height(height)
            .size(width = (barWidth * 3) + (spacing * 2), height = height),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleY = scale1
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleY = scale2
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleY = scale3
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(color)
        )
    }
}
