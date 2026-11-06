package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val TopScrimBrush = Brush.verticalGradient(
    listOf(PlayerScrim, Color.Transparent)
)
private val BottomScrimBrush = Brush.verticalGradient(
    listOf(Color.Transparent, PlayerScrim)
)

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
fun PlayerControls(
    state: PlayerUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsDisabled = isAnimationScaleZero(context)

    // Auto-hide controls: 3s timer while playing and not scrubbing
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var userInteractionCount by remember { mutableIntStateOf(0) }

    // Lock mode: unlock icon auto-fades after 2s
    var showUnlockTemporary by remember { mutableStateOf(false) }
    var unlockTapTime by remember { mutableLongStateOf(0L) }

    // Double-tap ±10s ripple state
    var showRewindArc by remember { mutableStateOf(false) }
    var showForwardArc by remember { mutableStateOf(false) }

    // 2x hold indicator state
    var isLongPress2x by remember { mutableStateOf(false) }

    // Auto-hide controls timer (3s)
    LaunchedEffect(state.controlsVisible, state.isPlaying, isScrubbing, userInteractionCount, state.locked) {
        if (!state.locked && state.controlsVisible && state.isPlaying && !isScrubbing) {
            delay(3000)
            actions.onSetControlsVisible(false)
        }
    }

    // Auto-fade temporary unlock icon (2s)
    LaunchedEffect(showUnlockTemporary, unlockTapTime) {
        if (showUnlockTemporary) {
            delay(2000)
            showUnlockTemporary = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.locked) {
                detectTapGestures(
                    onTap = { offset ->
                        userInteractionCount++
                        if (state.locked) {
                            showUnlockTemporary = true
                            unlockTapTime = System.currentTimeMillis()
                        } else {
                            actions.onToggleControls()
                        }
                    },
                    onDoubleTap = { offset ->
                        userInteractionCount++
                        if (!state.locked) {
                            val width = size.width
                            if (offset.x < width / 3f) {
                                // Left third: -10s
                                actions.onSeekRelative(-10)
                                showRewindArc = true
                            } else if (offset.x > width * 2f / 3f) {
                                // Right third: +10s
                                actions.onSeekRelative(10)
                                showForwardArc = true
                            } else {
                                actions.onTogglePlayPause()
                            }
                        }
                    },
                    onLongPress = {
                        if (!state.locked) {
                            isLongPress2x = true
                            actions.onSetSpeed(2.0f)
                        }
                    }
                )
            }
            .pointerInput(isLongPress2x) {
                if (isLongPress2x) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                isLongPress2x = false
                                actions.onSetSpeed(1.0f)
                                break
                            }
                        }
                    }
                }
            }
    ) {
        // ==========================================
        // 1. Double-Tap Ripple Arc Overlay (±10s)
        // ==========================================
        if (showRewindArc) {
            DoubleTapRippleOverlay(
                isForward = false,
                onComplete = { showRewindArc = false },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (showForwardArc) {
            DoubleTapRippleOverlay(
                isForward = true,
                onComplete = { showForwardArc = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ==========================================
        // 2. 2x Speed Hold Label (Top-Center)
        // ==========================================
        if (isLongPress2x) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.player_speed_2x_indicator),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlayerTextActive
                )
            }
        }

        // ==========================================
        // 3. Server Switch Loading Overlay
        // ==========================================
        if (state.isServerLoading || state.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerBg.copy(alpha = 0.5f))
            ) {
                IndeterminateThinLine(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // ==========================================
        // 4. Locked Mode Overlay
        // ==========================================
        if (state.locked) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showUnlockTemporary,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                IconButton(
                    onClick = {
                        userInteractionCount++
                        actions.onToggleLock()
                        showUnlockTemporary = false
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = context.getString(R.string.player_action_unlock)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = stringResource(R.string.player_action_unlock),
                        tint = PlayerTextActive,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            return@Box
        }

        // ==========================================
        // 5. Controls (Show / Hide via Fade 180ms)
        // ==========================================
        androidx.compose.animation.AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(tween(180, easing = LinearEasing)),
            exit = fadeOut(tween(180, easing = LinearEasing)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(TopScrimBrush)
                )

                // Bottom Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(BottomScrimBrush)
                )

                // Top Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back (KeyboardArrowDown)
                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(R.string.player_action_back)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.player_action_back),
                            tint = PlayerTextActive,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Top Right Icons: PiP, Quality, Lock, Settings
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isPipSupported) {
                            IconButton(
                                onClick = {
                                    userInteractionCount++
                                    actions.onEnterPip()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = context.getString(R.string.player_action_pip)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = stringResource(R.string.player_action_pip),
                                    tint = PlayerTextActive,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                userInteractionCount++
                                actions.onOpenQualitySettings()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = context.getString(R.string.player_action_quality)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = stringResource(R.string.player_action_quality),
                                tint = PlayerTextActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                userInteractionCount++
                                actions.onToggleLock()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = context.getString(R.string.player_action_lock)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.player_action_lock),
                                tint = PlayerTextActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                userInteractionCount++
                                actions.onOpenSettings()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = context.getString(R.string.player_action_settings)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.player_action_settings),
                                tint = PlayerTextActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Controls Row: Prev, -10, Play/Pause (56dp), +10, Next
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onSelectEpisode((state.ep - 1).coerceAtLeast(1))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(R.string.player_action_previous_episode)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.player_action_previous_episode),
                            tint = PlayerTextActive,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onSeekRelative(-10)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(R.string.player_action_rewind_10)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = stringResource(R.string.player_action_rewind_10),
                            tint = PlayerTextActive,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause Button (56dp target, Morph / Crossfade+Rotate 90°, 200ms)
                    val playRotation by animateFloatAsState(
                        targetValue = if (state.isPlaying) 90f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "play_rotate"
                    )

                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onTogglePlayPause()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .rotate(if (!animationsDisabled) playRotation else 0f)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(
                                    if (state.isPlaying) R.string.player_action_pause else R.string.player_action_play
                                )
                            }
                    ) {
                        Crossfade(
                            targetState = state.isPlaying,
                            animationSpec = tween(200),
                            label = "play_pause_crossfade"
                        ) { isPlaying ->
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PlayerTextActive,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onSeekRelative(10)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(R.string.player_action_fast_forward_10)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = stringResource(R.string.player_action_fast_forward_10),
                            tint = PlayerTextActive,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            userInteractionCount++
                            actions.onSelectEpisode(state.ep + 1)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = context.getString(R.string.player_action_next_episode)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.player_action_next_episode),
                            tint = PlayerTextActive,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Controls Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Row above seek bar: Title (left) & Right-aligned Subtitles, Audio-track, Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlayerTextActive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    userInteractionCount++
                                    actions.onOpenSettings()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = context.getString(R.string.player_action_subtitles)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = stringResource(R.string.player_action_subtitles),
                                    tint = PlayerTextActive,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    userInteractionCount++
                                    val nextAudio = if (state.audio == AudioType.SUB) AudioType.DUB else AudioType.SUB
                                    actions.onSelectAudio(nextAudio)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = context.getString(R.string.player_action_audio_track)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = stringResource(R.string.player_action_audio_track),
                                    tint = PlayerTextActive,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    userInteractionCount++
                                    val nextSpeed = when (state.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    actions.onSetSpeed(nextSpeed)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = context.getString(R.string.player_action_speed)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = stringResource(R.string.player_action_speed),
                                    tint = PlayerTextActive,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Seek Bar (Height 2dp->6dp, Thumb 0dp->12dp scale in on scrub, 120ms)
                    val activeProgress = if (isScrubbing) scrubFraction else state.progress
                    PlayerSeekBar(
                        progress = activeProgress,
                        isScrubbing = isScrubbing,
                        animationsDisabled = animationsDisabled,
                        onScrubStart = {
                            userInteractionCount++
                            isScrubbing = true
                            scrubFraction = it
                        },
                        onScrubProgress = {
                            scrubFraction = it
                        },
                        onScrubEnd = {
                            actions.onSeekTo(scrubFraction)
                            isScrubbing = false
                            userInteractionCount++
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    // Time labels (Left: position, Right: duration)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentMs = if (isScrubbing) {
                            (scrubFraction * state.durationMs).toLong()
                        } else {
                            state.positionMs
                        }
                        Text(
                            text = formatTime(currentMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = PlayerMuted
                        )
                        Text(
                            text = formatTime(state.durationMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = PlayerMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(
    progress: Float,
    isScrubbing: Boolean,
    animationsDisabled: Boolean,
    onScrubStart: (Float) -> Unit,
    onScrubProgress: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight by if (!animationsDisabled) {
        animateDpAsState(
            targetValue = if (isScrubbing) 6.dp else 2.dp,
            animationSpec = tween(durationMillis = 120),
            label = "bar_height"
        )
    } else {
        remember(isScrubbing) { mutableStateOf(if (isScrubbing) 6.dp else 2.dp) }
    }

    val thumbDiameter by if (!animationsDisabled) {
        animateDpAsState(
            targetValue = if (isScrubbing) 12.dp else 0.dp,
            animationSpec = tween(durationMillis = 120),
            label = "thumb_diameter"
        )
    } else {
        remember(isScrubbing) { mutableStateOf(if (isScrubbing) 12.dp else 0.dp) }
    }

    var barWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .height(24.dp)
            .onGloballyPositioned { coordinates ->
                barWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        onScrubStart(fraction)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                        onScrubProgress(fraction)
                    },
                    onDragEnd = {
                        onScrubEnd()
                    },
                    onDragCancel = {
                        onScrubEnd()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(PlayerFaint)
        )

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(barHeight)
                .background(PlayerTextActive)
        )

        // Thumb
        if (thumbDiameter > 0.dp) {
            val thumbOffsetPx = (progress.coerceIn(0f, 1f) * barWidthPx)
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = thumbOffsetPx.roundToInt() - (thumbDiameter.toPx() / 2).roundToInt(), y = 0) }
                    .size(thumbDiameter)
                    .clip(CircleShape)
                    .background(PlayerTextActive)
            )
        }
    }
}

@Composable
private fun DoubleTapRippleOverlay(
    isForward: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animAlpha.animateTo(1f, tween(150, easing = LinearEasing))
        delay(100)
        animAlpha.animateTo(0f, tween(150, easing = LinearEasing))
        onComplete()
    }

    Box(
        modifier = modifier,
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
            val width = size.width
            val height = size.height

            if (isForward) {
                // Arc on right side
                drawArc(
                    color = Color(0x22FFFFFF).copy(alpha = animAlpha.value * 0.15f),
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(-width, 0f),
                    size = Size(width * 2, height),
                    style = Fill
                )
            } else {
                // Arc on left side
                drawArc(
                    color = Color(0x22FFFFFF).copy(alpha = animAlpha.value * 0.15f),
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = Size(width * 2, height),
                    style = Fill
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .graphicsLayer { alpha = animAlpha.value },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                contentDescription = null,
                tint = PlayerTextActive,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun IndeterminateThinLine(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thin_line_load")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "line_offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val lineLength = width * 0.35f
        val startX = width * offsetX
        drawLine(
            color = PlayerTextActive,
            start = Offset(startX, 0f),
            end = Offset(startX + lineLength, 0f),
            strokeWidth = size.height
        )
    }
}
