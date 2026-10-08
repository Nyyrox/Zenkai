package com.nyyrox.aniverse.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * ZenkaiMotion: Centralized high-performance motion, timeline stagger, and physics suite.
 */

/**
 * Coordinated Staggered Entrance Timeline:
 * Drives an entrance animation where components emerge along a sequential timeline:
 * - translationY: slides upward into position
 * - scale: emerges from subtle depth (e.g. 0.95 -> 1.0)
 * - alpha: smoothly fades in
 */
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Long = 45L,
    initialOffsetY: Dp = 24.dp,
    initialScale: Float = 0.95f,
    durationMs: Int = 340,
    // Set to false for items appearing during scroll (lazy list recycling) so they pop
    // in immediately instead of starting invisible and animating.
    animate: Boolean = true
): Modifier = composed {
    val density = LocalDensity.current
    val offsetYPx = remember(initialOffsetY, density) {
        with(density) { initialOffsetY.toPx() }
    }

    // Start at 1f (fully visible) when animation is disabled — avoids the one-frame
    // invisible flash that occurs when LazyList recycles a composable during fast scroll.
    val animProgress = remember(animate) { Animatable(if (animate) 0f else 1f) }

    if (animate) {
        LaunchedEffect(Unit) {
            val totalDelay = (index.coerceAtLeast(0) * baseDelayMs).coerceAtMost(500L)
            if (totalDelay > 0L) {
                delay(totalDelay)
            }
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val progress = animProgress.value
    graphicsLayer {
        alpha = progress
        scaleX = initialScale + (1f - initialScale) * progress
        scaleY = initialScale + (1f - initialScale) * progress
        translationY = (1f - progress) * offsetYPx
    }
}

/**
 * Interactive MediaCard Physics & Swipe Tilt:
 * - Spring-loaded scale bounce on press
 * - Dynamic tilt response when dragged/swiped
 */
fun Modifier.mediaCardPhysics(
    interactionSource: MutableInteractionSource? = null,
    pressScale: Float = 0.95f,
    enableTilt: Boolean = true
): Modifier = composed {
    val localSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by localSource.collectIsPressedAsState()
    val isDragged by localSource.collectIsDraggedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> pressScale
            isDragged -> 0.97f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_scale_physics"
    )

    val rotationZ by animateFloatAsState(
        targetValue = when {
            isDragged && enableTilt -> -1.8f
            isPressed && enableTilt -> -0.8f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_tilt_physics"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.rotationZ = rotationZ
        cameraDistance = 12f * density
    }
}

/**
 * Dynamic Scroll Parallax Effect for Top Hero Banners:
 * - Gently shifts the hero banner downward at a fractional rate when scrolling down
 * - Subtly scales and fades the banner as it exits the viewport
 */
fun Modifier.heroScrollParallax(
    lazyListState: LazyListState,
    maxParallaxDp: Dp = 120.dp
): Modifier = composed {
    val density = LocalDensity.current
    val maxOffsetPx = remember(maxParallaxDp, density) {
        with(density) { maxParallaxDp.toPx() }
    }

    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }

    val (parallaxY, parallaxAlpha, parallaxScale) = remember(firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        if (firstVisibleItemIndex == 0) {
            val offsetFraction = (firstVisibleItemScrollOffset.toFloat() / maxOffsetPx).coerceIn(0f, 1f)
            val y = (firstVisibleItemScrollOffset * 0.42f).coerceAtMost(maxOffsetPx)
            val alpha = (1f - offsetFraction * 0.45f).coerceIn(0.55f, 1f)
            val scale = (1f - offsetFraction * 0.05f).coerceIn(0.95f, 1f)
            Triple(y, alpha, scale)
        } else {
            Triple(maxOffsetPx, 0.55f, 0.95f)
        }
    }

    graphicsLayer {
        translationY = parallaxY
        alpha = parallaxAlpha
        scaleX = parallaxScale
        scaleY = parallaxScale
    }
}
