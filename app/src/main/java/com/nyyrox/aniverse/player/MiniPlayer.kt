package com.nyyrox.aniverse.player

import android.os.SystemClock
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.roundToInt

private val MiniWidth = 208.dp
private val MiniHeight = 117.dp

/** Mount once at the app root, above the NavHost. */
@Composable
fun MiniPlayerHost() {
    val parked = MiniPlayerManager.parked
    val inPip by MainActivity.isInPipMode
    val density = LocalDensity.current
    var pos by remember { mutableStateOf<Offset?>(null) }
    val t = remember { Animatable(0f) } // 0 = mini window, 1 = video slot of the player page
    val contentAlpha = remember { Animatable(1f) }
    var wasFull by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Reset alpha whenever a new player is parked (not in handoff)
    LaunchedEffect(parked) {
        if (parked != null && !MiniPlayerManager.isHandingOff) {
            contentAlpha.snapTo(1f)
        }
    }

    // Smooth handoff crossfade: fade out overlay once claimed by full player screen
    LaunchedEffect(MiniPlayerManager.isHandingOff) {
        if (MiniPlayerManager.isHandingOff) {
            contentAlpha.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
            MiniPlayerManager.finishHandoff()
            contentAlpha.snapTo(1f)
            t.snapTo(0f)
        }
    }

    // back from the player page: shrink from its video slot into the mini window
    LaunchedEffect(MiniPlayerManager.fullScreenOpen) {
        if (MiniPlayerManager.fullScreenOpen) {
            wasFull = true
        } else if (wasFull) {
            wasFull = false
            contentAlpha.snapTo(1f)
            t.snapTo(1f)
            t.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(parked == null) {
        if (parked == null && !MiniPlayerManager.expanding && !MiniPlayerManager.fullScreenOpen) {
            t.snapTo(0f)
            contentAlpha.snapTo(1f)
        }
    }

    if (parked == null) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val wPx = with(density) { MiniWidth.toPx() }
        val hPx = with(density) { MiniHeight.toPx() }
        val home = Offset(
            screenW - wPx - with(density) { 12.dp.toPx() },
            screenH - hPx - with(density) { 110.dp.toPx() }
        )
        val f = t.value
        val base = pos ?: home
        val to = MiniPlayerManager.videoSlot ?: Rect(0f, 0f, screenW, screenW * 9f / 16f)
        MiniPlayerContent(
            p = parked,
            pip = inPip,
            f = f,
            contentAlpha = contentAlpha.value,
            x = lerp(base.x, to.left, f), y = lerp(base.y, to.top, f),
            w = lerp(wPx, to.width, f), h = lerp(hPx, to.height, f),
            onDrag = { d ->
                val c = pos ?: home // read fresh per event, a captured value made dragging stick
                pos = Offset(
                    (c.x + d.x).coerceIn(0f, (screenW - wPx).coerceAtLeast(0f)),
                    (c.y + d.y).coerceIn(0f, (screenH - hPx).coerceAtLeast(0f))
                )
            },
            onExpand = {
                if (!MiniPlayerManager.expanding) scope.launch {
                    MiniPlayerManager.videoSlot = null
                    MiniPlayerManager.handoffUntil = SystemClock.elapsedRealtime() + 8000L
                    MiniPlayerManager.expanding = true
                    try {
                        MiniPlayerManager.onExpand?.invoke(parked.routeAnimeId, parked.episode, parked.providerId)
                        // player page reports where its video box is, then we fly there
                        val ok = withTimeoutOrNull(600) { while (MiniPlayerManager.videoSlot == null) delay(16) }
                        if (ok != null) {
                            t.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
                        } else {
                            t.snapTo(1f)
                        }
                    } finally {
                        MiniPlayerManager.expanding = false // page now adopts the same player, no reload
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
    onDrag: (Offset) -> Unit,
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
        // re-render the current frame on the new surface when paused
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

    // progress ticker + history saving (PlayerScreen's tick loop is gone while mini)
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
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = (if (pip) Modifier.fillMaxSize() else Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
            .shadow((10f * (1f - f)).dp, RoundedCornerShape((12f + 2f * f).dp))
            .clip(RoundedCornerShape((12f + 2f * f).dp)))
            .alpha(contentAlpha)
            .background(Color.Black)
            .then(if (mini) Modifier
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onDrag(drag) } }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onExpand() },
                        onTap = { showControls = !showControls }
                    )
                }
            else Modifier)
    ) {
        AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())

        AnimatedVisibility(visible = showControls && mini, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 6.dp, end = 34.dp)
                        .clickable { onExpand() }
                ) {
                    Text(p.title, color = Color.White, fontSize = 11.sp, maxLines = 1)
                    Text(p.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1)
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
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
                            .padding(7.dp)
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
            // thin progress bar
            val frac = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.25f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(frac).height(2.dp).background(Color.White))
        }
    }
}
