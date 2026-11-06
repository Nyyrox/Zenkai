@file:kotlin.OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.nyyrox.aniverse.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import com.nyyrox.aniverse.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    actions: PlayerActions,
    player: Player?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val animationsDisabled = isAnimationScaleZero(context)

    // Window size class for 2-pane layout (width >= 600dp)
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val isTwoPane = windowSizeClass?.widthSizeClass != null &&
            windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var showSourceSheet by remember { mutableStateOf(false) }
    var isRangeDropdownExpanded by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    // Screen enter stagger animation: info block + first 5 rows (8dp, 40ms stagger, 250ms each, runs once)
    var hasRunEnterAnimation by rememberSaveable { mutableStateOf(false) }
    val enterAnimatable = remember { Animatable(if (hasRunEnterAnimation || animationsDisabled) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!hasRunEnterAnimation && !animationsDisabled) {
            enterAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing)
            )
            hasRunEnterAnimation = true
        } else {
            hasRunEnterAnimation = true
        }
    }

    // System bars controller for fullscreen
    val window = activity?.window
    val insetsController = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    LaunchedEffect(state.isFullscreen) {
        insetsController?.let { controller ->
            if (state.isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Back handling: Closes sheet -> exits fullscreen -> minimizes player -> pops screen
    BackHandler {
        when {
            showSourceSheet -> showSourceSheet = false
            state.isFullscreen -> actions.onToggleFullscreen()
            else -> {
                actions.onMinimize()
                actions.onBack()
            }
        }
    }

    // Filter episodes by selected range
    val visibleEpisodes = remember(state.eps, state.selectedRange) {
        if (state.eps.isEmpty()) {
            emptyList()
        } else {
            state.eps.filter { it.n in state.selectedRange.start..state.selectedRange.end }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBg)
    ) {
        if (state.isFullscreen) {
            // Fullscreen Player: takes entire screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerBg)
            ) {
                PlayerSurface(
                    player = player,
                    modifier = Modifier.fillMaxSize()
                )
                PlayerControls(
                    state = state,
                    actions = actions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (isTwoPane) {
            // Landscape / width >= 600dp: Two Panes (Left 60% player, Right 40% LazyColumn)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Left 60% Player Pane
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(PlayerBg)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount > 24f) {
                                    change.consume()
                                    actions.onMinimize()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(PlayerBg)
                    ) {
                        PlayerSurface(
                            player = player,
                            modifier = Modifier.fillMaxSize()
                        )
                        PlayerControls(
                            state = state,
                            actions = actions,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Right 40% LazyColumn Pane
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(PlayerBg)
                ) {
                    PlayerEpisodesList(
                        state = state,
                        actions = actions,
                        visibleEpisodes = visibleEpisodes,
                        listState = listState,
                        isSynopsisExpanded = isSynopsisExpanded,
                        onToggleSynopsis = { isSynopsisExpanded = !isSynopsisExpanded },
                        onOpenSourceSheet = { showSourceSheet = true },
                        isRangeDropdownExpanded = isRangeDropdownExpanded,
                        onSetRangeDropdownExpanded = { isRangeDropdownExpanded = it },
                        enterAnimationProgress = enterAnimatable.value,
                        animationsDisabled = animationsDisabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Portrait: Player pinned at top (16:9), LazyColumn below it
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. Player (Pinned at top, 16:9, outside LazyColumn)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(PlayerBg)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount > 24f) {
                                    change.consume()
                                    actions.onMinimize()
                                }
                            }
                        }
                ) {
                    PlayerSurface(
                        player = player,
                        modifier = Modifier.fillMaxSize()
                    )
                    PlayerControls(
                        state = state,
                        actions = actions,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. LazyColumn (Scrolls independently below player)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(PlayerBg)
                ) {
                    PlayerEpisodesList(
                        state = state,
                        actions = actions,
                        visibleEpisodes = visibleEpisodes,
                        listState = listState,
                        isSynopsisExpanded = isSynopsisExpanded,
                        onToggleSynopsis = { isSynopsisExpanded = !isSynopsisExpanded },
                        onOpenSourceSheet = { showSourceSheet = true },
                        isRangeDropdownExpanded = isRangeDropdownExpanded,
                        onSetRangeDropdownExpanded = { isRangeDropdownExpanded = it },
                        enterAnimationProgress = enterAnimatable.value,
                        animationsDisabled = animationsDisabled,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Source Selection Bottom Sheet
        if (showSourceSheet) {
            SourceSheet(
                servers = state.servers,
                selectedServerIndex = state.serverIdx,
                currentAudio = state.audio,
                isDubAvailable = state.isDubAvailable,
                areAllServersFailed = state.areAllServersFailed,
                onSelectAudio = { actions.onSelectAudio(it) },
                onSelectServer = { actions.onSelectServer(it) },
                onRetry = { actions.onRetryServer() },
                onDismiss = { showSourceSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerEpisodesList(
    state: PlayerUiState,
    actions: PlayerActions,
    visibleEpisodes: List<EpisodeItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isSynopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onOpenSourceSheet: () -> Unit,
    isRangeDropdownExpanded: Boolean,
    onSetRangeDropdownExpanded: (Boolean) -> Unit,
    enterAnimationProgress: Float,
    animationsDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Stagger progress helper for index i (0..5)
    fun staggerTransform(index: Int): Pair<Float, Float> {
        if (animationsDisabled || enterAnimationProgress >= 1f) return Pair(1f, 0f)
        val progress = (enterAnimationProgress - (index * 0.08f)).coerceIn(0f, 1f)
        val translationY = (1f - progress) * 8f
        return Pair(progress, translationY)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        // item: Info block
        item(key = "player_info_block") {
            val (alpha, transY) = staggerTransform(0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        this.translationY = transY * density
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Title (28sp, 800 weight, -0.5sp tracking)
                Text(
                    text = state.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = PlayerTextActive,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Episode N · Title (1 line, ellipsis, Muted)
                Text(
                    text = stringResource(
                        R.string.player_episode_n_title,
                        state.ep,
                        state.currentEpisodeTitle
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlayerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Meta line: ★ 87%  ·  TV  ·  24m (Faint, plain text)
                val metaLine = "${state.metaScore}  ·  ${state.metaFormat}  ·  ${state.metaDuration}"
                Text(
                    text = metaLine,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlayerFaint
                )

                // Synopsis (2 lines, "more" expands, hide block if empty)
                if (state.synopsis.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(tween(durationMillis = 200))
                    ) {
                        Text(
                            text = state.synopsis,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = PlayerMuted,
                            maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                if (isSynopsisExpanded) R.string.player_synopsis_less else R.string.player_synopsis_more
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlayerTextActive,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onToggleSynopsis)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // item: Source row (SUB · FlixCloud HD ⌄, full width, between two hairlines)
        item(key = "player_source_row") {
            val serverName = state.currentServer?.name ?: ""
            val audioLabel = stringResource(
                if (state.audio == AudioType.SUB) R.string.player_audio_sub else R.string.player_audio_dub
            )
            val hdLabel = if (state.currentServer?.isHd == true) stringResource(R.string.player_quality_hd) else ""
            val sourceText = stringResource(R.string.player_source_row_format, audioLabel, serverName, hdLabel)

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onOpenSourceSheet)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sourceText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PlayerTextActive
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)
            }
        }

        // stickyHeader: "Episodes" (18sp/700) left; range text-dropdown "1–25 ⌄"; download-all icon
        stickyHeader(key = "player_episodes_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlayerBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // "Episodes" (18sp, 700 / Bold)
                    Text(
                        text = stringResource(R.string.player_episodes_header),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerTextActive
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Range text-dropdown "1–25 ⌄"
                        if (state.availableRanges.isNotEmpty()) {
                            Box {
                                Text(
                                    text = "${state.selectedRange.label} ⌄",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PlayerMuted,
                                    modifier = Modifier
                                        .clickable(
                                            role = Role.DropdownList,
                                            onClick = { onSetRangeDropdownExpanded(true) }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )

                                DropdownMenu(
                                    expanded = isRangeDropdownExpanded,
                                    onDismissRequest = { onSetRangeDropdownExpanded(false) },
                                    modifier = Modifier.background(PlayerSheet)
                                ) {
                                    state.availableRanges.forEach { range ->
                                        val isSelected = range == state.selectedRange
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = range.label,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PlayerTextActive else PlayerMuted
                                                )
                                            },
                                            onClick = {
                                                actions.onSelectRange(range)
                                                onSetRangeDropdownExpanded(false)
                                                scope.launch {
                                                    listState.animateScrollToItem(0)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Download-all icon
                        IconButton(
                            onClick = { actions.onDownloadAll() },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = context.getString(R.string.player_action_download_all)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.player_action_download_all),
                                tint = PlayerTextActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = PlayerHairline)
            }
        }

        // items(key = { ep.n }): Episode rows
        items(
            items = visibleEpisodes,
            key = { it.n }
        ) { ep ->
            val indexInList = ep.n - (state.selectedRange.start.takeIf { it > 0 } ?: 1) + 1
            val (alpha, transY) = if (indexInList in 1..5) {
                staggerTransform(indexInList)
            } else {
                Pair(1f, 0f)
            }

            EpisodeRow(
                episode = ep,
                isActive = ep.n == state.ep,
                isPlaying = state.isPlaying,
                onSelect = { actions.onSelectEpisode(ep.n) },
                onDownload = { actions.onDownloadEpisode(ep.n) },
                onMarkWatched = { actions.onMarkWatched(ep.n) },
                onShare = { actions.onShareEpisode(ep.n) },
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = transY * density
                }
            )
        }
    }
}

// =========================================================================
// Integration Overload: Connects ViewModel to pure PlayerScreen
// =========================================================================
@Composable
fun PlayerScreen(
    vm: com.nyyrox.aniverse.viewmodel.MainViewModel,
    animeId: Int,
    initialEpisode: Int = 1,
    initialProviderId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentEpisodeNumber by rememberSaveable(animeId) { mutableIntStateOf(initialEpisode) }
    var selectedAudioType by rememberSaveable { mutableStateOf(AudioType.SUB) }
    var selectedServerIdx by rememberSaveable { mutableIntStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlayerLocked by remember { mutableStateOf(false) }
    var isFullscreenActive by remember { mutableStateOf(false) }

    // Map ViewModel state to PlayerUiState
    val anime = vm.selected
    val title = anime?.title ?: "Anime #$animeId"
    val totalEpisodes = anime?.episodes ?: 24

    // Episode list mapping
    val episodeList = remember(totalEpisodes, currentEpisodeNumber) {
        (1..totalEpisodes).map { n ->
            EpisodeItem(
                n = n,
                title = "Episode $n",
                durationText = "24m",
                thumbnailUrl = anime?.banner ?: anime?.cover,
                isWatched = n < currentEpisodeNumber,
                progressFraction = if (n == currentEpisodeNumber) 0.45f else 0f
            )
        }
    }

    // Ranges (groups of 25)
    val availableRanges = remember(totalEpisodes) {
        val ranges = mutableListOf<EpisodeRange>()
        var start = 1
        while (start <= totalEpisodes) {
            val end = (start + 24).coerceAtMost(totalEpisodes)
            ranges.add(EpisodeRange(start, end))
            start += 25
        }
        if (ranges.isEmpty()) listOf(EpisodeRange(1, 1)) else ranges
    }
    var selectedRange by rememberSaveable {
        mutableStateOf(availableRanges.firstOrNull() ?: EpisodeRange(1, 25))
    }

    val servers = remember {
        listOf(
            ServerItem("flixcloud", "FlixCloud", isAvailable = true, isHd = true),
            ServerItem("vidstream", "VidStream", isAvailable = true, isHd = true),
            ServerItem("megacloud", "MegaCloud", isAvailable = true, isHd = true),
            ServerItem("streamhub", "StreamHub", isAvailable = false, isHd = false)
        )
    }

        val activeExo = com.nyyrox.aniverse.service.ActivePlayerHolder.player
        val currentPosition = activeExo?.currentPosition ?: 0L
        val duration = activeExo?.duration ?: (24 * 60 * 1000L)
        val progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

        val uiState = PlayerUiState(
            title = title,
            ep = currentEpisodeNumber,
            currentEpisodeTitle = "Episode $currentEpisodeNumber",
            metaScore = "★ 87%",
            metaFormat = anime?.format ?: "TV",
            metaDuration = "24m",
            synopsis = anime?.description.orEmpty(),
            eps = episodeList,
            servers = servers,
            serverIdx = selectedServerIdx,
            audio = selectedAudioType,
            isDubAvailable = true,
            availableRanges = availableRanges,
            selectedRange = selectedRange,
            isPlaying = activeExo?.isPlaying == true,
            isBuffering = false,
            isServerLoading = false,
            positionMs = currentPosition,
            durationMs = duration,
            progress = progressFraction,
            controlsVisible = isControlsVisible,
            locked = isPlayerLocked,
            playbackSpeed = 1f,
            isPipSupported = true,
            isFullscreen = isFullscreenActive
        )

        val actions = remember(vm, onBack) {
            object : PlayerActions {
                override fun onTogglePlayPause() {
                    val p = com.nyyrox.aniverse.service.ActivePlayerHolder.player
                    if (p != null) {
                        if (p.isPlaying) p.pause() else p.play()
                    }
                }

                override fun onSeekTo(fraction: Float) {
                    val p = com.nyyrox.aniverse.service.ActivePlayerHolder.player
                    if (p != null) {
                        val targetMs = (fraction * p.duration).toLong()
                        p.seekTo(targetMs)
                    }
                }

                override fun onSeekRelative(seconds: Int) {
                    val p = com.nyyrox.aniverse.service.ActivePlayerHolder.player
                    if (p != null) {
                        p.seekTo(p.currentPosition + (seconds * 1000L))
                    }
                }

                override fun onSelectEpisode(episodeNumber: Int) {
                    currentEpisodeNumber = episodeNumber
                }

                override fun onSelectServer(index: Int) {
                    selectedServerIdx = index
                }

                override fun onSelectAudio(audio: AudioType) {
                    selectedAudioType = audio
                }

                override fun onSelectRange(range: EpisodeRange) {
                    selectedRange = range
                }

                override fun onToggleControls() {
                    isControlsVisible = !isControlsVisible
                }

                override fun onSetControlsVisible(visible: Boolean) {
                    isControlsVisible = visible
                }

                override fun onToggleLock() {
                    isPlayerLocked = !isPlayerLocked
                }

                override fun onSetSpeed(speed: Float) {
                    com.nyyrox.aniverse.service.ActivePlayerHolder.player?.setPlaybackSpeed(speed)
                }

                override fun onToggleFullscreen() {
                    isFullscreenActive = !isFullscreenActive
                }

                override fun onEnterPip() {
                    (context as? Activity)?.enterPictureInPictureMode()
                }

                override fun onOpenQualitySettings() {}

                override fun onOpenSettings() {}

                override fun onDownloadAll() {}

                override fun onDownloadEpisode(episodeNumber: Int) {}

                override fun onMarkWatched(episodeNumber: Int) {}

                override fun onShareEpisode(episodeNumber: Int) {}

                override fun onRetryServer() {}

                override fun onMinimize() {
                    onBack()
                }

                override fun onBack() {
                    onBack()
                }
            }
        }

        PlayerScreen(
            state = uiState,
            actions = actions,
            player = com.nyyrox.aniverse.service.ActivePlayerHolder.player,
            modifier = modifier
        )
}
