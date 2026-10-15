package com.nyyrox.aniverse.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Material 3 emphasized curves: fast launch, long soft landing (out) / quick tuck (back).
private val EmphasizedOut = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedBack = CubicBezierEasing(0.2f, 0f, 0f, 1f)

enum class SharedTransitionPhase {
    IDLE,
    FORWARD,
    PRESENTED,
    BACKWARD
}

@Stable
class SharedPosterCoordinator(
    private val scope: CoroutineScope
) {
    var phase by mutableStateOf(SharedTransitionPhase.IDLE)
        private set

    var activeItemKey by mutableStateOf<String?>(null)
        private set

    val activeAnimeId: Int?
        get() = activeItemKey?.removePrefix("anime_")?.toIntOrNull()

    var activePosterUrl by mutableStateOf<String?>(null)
        private set

    var sourceBounds by mutableStateOf<Rect?>(null)
        private set

    var targetBounds by mutableStateOf<Rect?>(null)
        private set

    val progress = Animatable(0f)

    // 1→0 after landing: overlay dissolves over the real screen instead of vanishing (no handoff pop).
    val settle = Animatable(0f)

    val isTransitioning: Boolean
        get() = phase == SharedTransitionPhase.FORWARD || phase == SharedTransitionPhase.BACKWARD

    fun isItemActive(itemKey: String): Boolean {
        return activeItemKey == itemKey && isTransitioning
    }

    fun isItemActive(animeId: Int): Boolean {
        return isItemActive("anime_$animeId")
    }

    fun startForwardTransition(
        itemKey: String,
        posterUrl: String?,
        bounds: Rect,
        estimatedTargetBounds: Rect? = null,
        onNavigate: () -> Unit
    ) {
        if (bounds.width <= 0 || bounds.height <= 0 || posterUrl.isNullOrBlank()) {
            onNavigate()
            return
        }

        activeItemKey = itemKey
        activePosterUrl = posterUrl
        sourceBounds = bounds
        targetBounds = estimatedTargetBounds
        phase = SharedTransitionPhase.FORWARD

        onNavigate()

        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = EmphasizedOut
                )
            )
            settle.snapTo(1f)
            phase = SharedTransitionPhase.PRESENTED
            settle.animateTo(0f, tween(180))
        }
    }

    fun startForwardTransition(
        animeId: Int,
        posterUrl: String?,
        bounds: Rect,
        estimatedTargetBounds: Rect? = null,
        onNavigate: () -> Unit
    ) {
        startForwardTransition(
            itemKey = "anime_$animeId",
            posterUrl = posterUrl,
            bounds = bounds,
            estimatedTargetBounds = estimatedTargetBounds,
            onNavigate = onNavigate
        )
    }

    fun updateTargetBounds(bounds: Rect) {
        if (bounds.width > 0 && bounds.height > 0) {
            if (targetBounds == null || (phase == SharedTransitionPhase.FORWARD && progress.value < 0.05f)) {
                targetBounds = bounds
            }
        }
    }

    fun returnBack(onComplete: () -> Unit) {
        if (phase != SharedTransitionPhase.PRESENTED && phase != SharedTransitionPhase.FORWARD) {
            reset()
            onComplete()
            return
        }

        val src = sourceBounds
        if (src == null || src.width <= 0 || src.height <= 0) {
            reset()
            onComplete()
            return
        }

        phase = SharedTransitionPhase.BACKWARD
        scope.launch {
            settle.snapTo(0f)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 380,
                    easing = EmphasizedBack
                )
            )
            reset()
            onComplete()
        }
    }

    fun reset() {
        phase = SharedTransitionPhase.IDLE
        activeItemKey = null
        activePosterUrl = null
        sourceBounds = null
        targetBounds = null
        scope.launch {
            progress.snapTo(0f)
            settle.snapTo(0f)
        }
    }
}

val LocalSharedPosterCoordinator = compositionLocalOf<SharedPosterCoordinator?> {
    null
}

@Composable
fun rememberSharedPosterCoordinator(): SharedPosterCoordinator {
    val scope = rememberCoroutineScope()
    return remember { SharedPosterCoordinator(scope) }
}

/**
 * High-performance window-level overlay that renders the shared anime poster
 * traveling smoothly between card position and hero banner without any recomposition
 * jank, real-time blurs, or duplicate image decodes.
 */
@Composable
fun SharedPosterOverlay(
    coordinator: SharedPosterCoordinator,
    modifier: Modifier = Modifier
) {
    if (!coordinator.isTransitioning && coordinator.settle.value <= 0f) return

    val src = coordinator.sourceBounds ?: return
    val posterUrl = coordinator.activePosterUrl ?: return
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val heroHeightPx = with(density) { 360.dp.toPx() }

    val isMediaCard = coordinator.activeItemKey?.let { it.startsWith("manga_") || it.startsWith("novel_") } == true
    val defaultTarget = if (isMediaCard) {
        val cardWidthPx = with(density) { 125.dp.toPx() }
        val cardHeightPx = with(density) { 180.dp.toPx() }
        val leftPx = with(density) { 16.dp.toPx() }
        val topPx = with(density) { 86.dp.toPx() }
        Rect(leftPx, topPx, leftPx + cardWidthPx, topPx + cardHeightPx)
    } else {
        Rect(0f, 0f, screenWidthPx, heroHeightPx)
    }

    val tgt = coordinator.targetBounds ?: defaultTarget
    val p = coordinator.progress.value.coerceIn(0f, 1f)

    // Smooth interpolated bounds
    val currentLeft = src.left + (tgt.left - src.left) * p
    val currentTop = src.top + (tgt.top - src.top) * p
    val currentWidth = (src.width + (tgt.width - src.width) * p).coerceAtLeast(1f)
    val currentHeight = (src.height + (tgt.height - src.height) * p).coerceAtLeast(1f)
    val tgtRadius = if (isMediaCard) 14f else if (tgt.height > tgt.width) 28f else 0f // loader card vs hero
    val currentCornerRadius = (12f + (tgtRadius - 12f) * p).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .graphicsLayer {
                alpha = when (coordinator.phase) {
                    SharedTransitionPhase.PRESENTED -> coordinator.settle.value
                    // fade in as it leaves the hero so the poster doesn't pop over the banner
                    SharedTransitionPhase.BACKWARD -> ((1f - coordinator.progress.value) / 0.2f).coerceIn(0f, 1f)
                    else -> 1f
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(currentLeft.roundToInt(), currentTop.roundToInt()) }
                .size(
                    width = with(density) { currentWidth.toDp() },
                    height = with(density) { currentHeight.toDp() }
                )
                .clip(RoundedCornerShape(currentCornerRadius.dp))
                .background(Color(0xFF0F1117))
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Subtle cinematic bottom scrim as the poster morphs into hero
            if (!isMediaCard && p > 0.3f) {
                val scrimAlpha = ((p - 0.3f) / 0.7f).coerceIn(0f, 0.9f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x8004060A).copy(alpha = scrimAlpha * 0.5f),
                                    Color(0xFF04060A).copy(alpha = scrimAlpha)
                                )
                            )
                        )
                )
            }
        }
    }
}
