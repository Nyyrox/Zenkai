package com.nyyrox.aniverse.player

import android.os.SystemClock
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nyyrox.aniverse.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

private val BaseMiniWidth = 208.dp
private const val ASPECT_RATIO = 16f / 9f

/** Mount once at the app root, above the NavHost. */
@Composable
fun MiniPlayerHost() {
    val parked = MiniPlayerManager.parked
    val inPip by MainActivity.isInPipMode
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val t = remember { Animatable(0f) } // 0 = mini window, 1 = video slot of the player page
    val contentAlpha = remember { Animatable(1f) }
    val dismissOffsetY = remember { Animatable(0f) }
    val zoomScale = remember { Animatable(1f) } // 1.0 = compact mini, up to 1.55 = max cap

    // Position state: animatables for smooth corner snapping
    val posX = remember { Animatable(0f) }
    val posY = remember { Animatable(0f) }
    var isInitializedPos by remember { mutableStateOf(false) }

    var wasFull by remember { mutableStateOf(false) }

    // Reset alpha whenever a new player is parked (not in handoff)
    LaunchedEffect(parked) {
        if (parked != null && !MiniPlayerManager.isHandingOff) {
            contentAlpha.snapTo(1f)
            dismissOffsetY.snapTo(0f)
        }
    }

    // Smooth handoff crossfade: fade out overlay once claimed by full player screen
    LaunchedEffect(MiniPlayerManager.isHandingOff) {
        if (MiniPlayerManager.isHandingOff) {
            contentAlpha.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
            MiniPlayerManager.finishHandoff()
            contentAlpha.snapTo(1f)
            dismissOffsetY.snapTo(0f)
            t.snapTo(0f)
        }
    }

    // Back from the player page: shrink smoothly from its video slot into the mini window
    LaunchedEffect(MiniPlayerManager.fullScreenOpen) {
        if (MiniPlayerManager.fullScreenOpen) {
            wasFull = true
        } else if (wasFull) {
            wasFull = false
            contentAlpha.snapTo(1f)
            dismissOffsetY.snapTo(0f)
            t.snapTo(1f)
            t.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(parked == null) {
        if (parked == null && !MiniPlayerManager.expanding && !MiniPlayerManager.fullScreenOpen) {
            t.snapTo(0f)
            contentAlpha.snapTo(1f)
            dismissOffsetY.snapTo(0f)
        }
    }

    if (parked == null) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        val scale = zoomScale.value
        val currentW = BaseMiniWidth * scale
        val currentH = currentW / ASPECT_RATIO
        val wPx = with(density) { currentW.toPx() }
        val hPx = with(density) { currentH.toPx() }

        val horizontalMargin = with(density) { 12.dp.toPx() }
        val topClearance = with(density) { 48.dp.toPx() }
        // Crucial clearance: NEVER overlap or go behind the bottom dock (OttBottomDock) or navbar
        val bottomClearance = with(density) { 110.dp.toPx() }

        val minX = horizontalMargin
        val maxX = (screenW - wPx - horizontalMargin).coerceAtLeast(minX)
        val minY = topClearance
        val maxY = (screenH - hPx - bottomClearance).coerceAtLeast(minY)

        val homeX = maxX
        val homeY = maxY

        // Initialize resting corner position
        LaunchedEffect(screenW, screenH) {
            if (!isInitializedPos) {
                posX.snapTo(homeX)
                posY.snapTo(homeY)
                isInitializedPos = true
            }
        }

        // Clamp positions if window dimensions or zoom scale changes
        LaunchedEffect(scale, screenW, screenH) {
            if (isInitializedPos) {
                val clampedX = posX.value.coerceIn(minX, maxX)
                val clampedY = posY.value.coerceIn(minY, maxY)
                posX.snapTo(clampedX)
                posY.snapTo(clampedY)
            }
        }

        val f = t.value
        val basePos = Offset(posX.value, posY.value)
        val to = MiniPlayerManager.videoSlot ?: Rect(0f, 0f, screenW, screenW / ASPECT_RATIO)

        val renderedX = lerp(basePos.x, to.left, f)
        val renderedY = lerp(basePos.y, to.top, f) + dismissOffsetY.value
        val renderedW = lerp(wPx, to.width, f)
        val renderedH = lerp(hPx, to.height, f)

        val totalAlpha = contentAlpha.value * (1f - (dismissOffsetY.value / with(density) { 160.dp.toPx() })).coerceIn(0f, 1f)

        MiniPlayerContent(
            p = parked,
            pip = inPip,
            f = f,
            contentAlpha = totalAlpha,
            x = renderedX,
            y = renderedY,
            w = renderedW,
            h = renderedH,
            onDragDelta = { delta ->
                scope.launch {
                    val newX = (posX.value + delta.x).coerceIn(minX, maxX)
                    val newY = (posY.value + delta.y).coerceIn(minY, maxY)
                    posX.snapTo(newX)
                    posY.snapTo(newY)

                    // If user pulls down while already at the bottom limit, accumulate dismiss offset
                    if (delta.y > 0 && posY.value >= maxY - 4f) {
                        dismissOffsetY.snapTo((dismissOffsetY.value + delta.y).coerceAtLeast(0f))
                    }
                }
            },
            onDragEnd = {
                scope.launch {
                    val dismissThreshold = with(density) { 60.dp.toPx() }
                    if (dismissOffsetY.value > dismissThreshold) {
                        // YouTube-style swipe down to dismiss
                        dismissOffsetY.animateTo(
                            screenH - posY.value + 100f,
                            tween(220, easing = FastOutSlowInEasing)
                        )
                        MiniPlayerManager.release()
                    } else {
                        // Spring back dismiss offset
                        dismissOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                        // Corner magnetic snap: snap to closest corner (left or right)
                        val targetX = if (posX.value + wPx / 2f < screenW / 2f) minX else maxX
                        val targetY = posY.value.coerceIn(minY, maxY)
                        launch { posX.animateTo(targetX, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                        launch { posY.animateTo(targetY, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) }
                    }
                }
            },
            onPinchZoom = { zoomFactor ->
                scope.launch {
                    val newScale = (zoomScale.value * zoomFactor).coerceIn(1.0f, 1.55f)
                    zoomScale.snapTo(newScale)
                }
            },
            onToggleZoomCap = {
                scope.launch {
                    val targetScale = if (zoomScale.value > 1.25f) 1.0f else 1.55f
                    zoomScale.animateTo(targetScale, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            },
            onExpand = {
                if (!MiniPlayerManager.expanding) scope.launch {
                    MiniPlayerManager.videoSlot = null
                    MiniPlayerManager.handoffUntil = SystemClock.elapsedRealtime() + 8000L
                    MiniPlayerManager.expanding = true
                    try {
                        MiniPlayerManager.onExpand?.invoke(parked.routeAnimeId, parked.episode, parked.providerId)
                        val ok = withTimeoutOrNull(600) { while (MiniPlayerManager.videoSlot == null) delay(16) }
                        if (ok != null) {
                            t.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                        } else {
                            t.snapTo(1f)
                        }
                    } finally {
                        MiniPlayerManager.expanding = false
                    }
                }
            }
        )
    }
}

@Composable
private fun MiniPlayerContent(
    p: MiniPlayerManager.Parked,
    pip: Boolean,
    f: Float,
    contentAlpha: Float,
    x: Float, y: Float, w: Float, h: Float,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onPinchZoom: (Float) -> Unit,
    onToggleZoomCap: () -> Unit,
    onExpand: () -> Unit
) {
    val density = LocalDensity.current
    val mini = !pip && f == 0f
    val context = LocalContext.current
    val player = p.player
    var isPlaying by remember(player) { mutableStateOf(player.playWhenReady) }
    var showControls by remember(player) { mutableStateOf(false) }
    var positionMs by remember(player) { mutableLongStateOf(0L) }
    var durationMs by remember(player) { mutableLongStateOf(0L) }

    val playerView = remember(player) {
        (LayoutInflater.from(context).inflate(com.nyyrox.aniverse.R.layout.cine_player_texture, null, false) as PlayerView).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_shutter)?.visibility = android.view.View.GONE
            subtitleView?.visibility = android.view.View.GONE
        }
    }

    DisposableEffect(player) {
        playerView.player = player
        if (!player.playWhenReady) { try { player.seekTo(player.currentPosition) } catch (_: Exception) {} }
        val l = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { isPlaying = playWhenReady }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) MiniPlayerManager.release(context)
            }
            override fun onPlayerError(error: PlaybackException) { MiniPlayerManager.release(context) }
        }
        player.addListener(l)
        onDispose {
            playerView.player = null
            try { player.removeListener(l) } catch (_: Exception) {}
        }
    }

    // Progress ticker + history saving
    LaunchedEffect(player) {
        var lastSave = 0L
        while (true) {
            try {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = player.duration.coerceAtLeast(0L)
                val now = SystemClock.elapsedRealtime()
                if (durationMs > 0L && positionMs > 0L && now - lastSave >= 1000L) {
                    p.onProgress(positionMs, durationMs)
                    lastSave = now
                }
            } catch (_: Exception) {}
            delay(500)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    Box(
        modifier = (if (pip) Modifier.fillMaxSize() else Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
            .shadow((12f * (1f - f)).dp, RoundedCornerShape((14f + 2f * f).dp))
            .clip(RoundedCornerShape((14f + 2f * f).dp)))
            .alpha(contentAlpha)
            .background(Color.Black)
            .then(
                if (mini) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    onPinchZoom(zoom)
                                }
                                if (pan != Offset.Zero) {
                                    onDragDelta(pan)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() }
                            ) { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { onToggleZoomCap() },
                                onTap = { showControls = !showControls }
                            )
                        }
                } else Modifier
            )
    ) {
        AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = showControls && mini,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 8.dp, end = 36.dp)
                        .clickable { onExpand() }
                ) {
                    Text(p.title, color = Color.White, fontSize = 11.5.sp, maxLines = 1)
                    Text(p.subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 9.5.sp, maxLines = 1)
                }

                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { MiniPlayerManager.release(context) }
                )

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Expand",
                        tint = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onExpand() }
                            .padding(6.dp)
                    )
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { PlayerPlaybackPlugin.togglePlayPause(player) }
                            .padding(6.dp)
                    )
                    Box(Modifier.size(34.dp))
                }
            }
        }

        if (!pip && f == 0f) {
            // Thin accent progress bar
            val frac = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.5.dp).background(Color.White.copy(alpha = 0.28f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(frac).height(2.5.dp).background(Color(0xFFE50914)))
        }
    }
}
