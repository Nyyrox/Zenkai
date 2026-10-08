package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.AnimeCharacter
import com.nyyrox.aniverse.data.AnimeRecommendation
import com.nyyrox.aniverse.data.AnimeStaff
import com.nyyrox.aniverse.data.DUB_PROVIDERS
import com.nyyrox.aniverse.data.MULTI_PROVIDERS
import com.nyyrox.aniverse.data.MediaRelation
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.SUB_PROVIDERS
import com.nyyrox.aniverse.data.WatchlistStatus
import com.nyyrox.aniverse.data.tmdb.TmdbDetails
import com.nyyrox.aniverse.data.tmdb.TmdbEpisodeStill
import com.nyyrox.aniverse.data.tmdb.TmdbRepository
import com.nyyrox.aniverse.data.tmdb.TmdbVideo
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedTransitionPhase
import com.nyyrox.aniverse.ui.components.DetailScreenSkeleton
import com.nyyrox.aniverse.ui.components.OttPosterCard
import com.nyyrox.aniverse.ui.components.ShimmerBox
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttHeroGradient
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    vm: MainViewModel,
    animeId: Int,
    onBack: () -> Unit,
    onPlay: (animeId: Int, episode: Int, provider: Provider) -> Unit,
    onWatchTogether: ((anime: Anime, episode: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coordinator = LocalSharedPosterCoordinator.current

    val handleBack: () -> Unit = {
        if (coordinator != null && (coordinator.phase == SharedTransitionPhase.PRESENTED || coordinator.phase == SharedTransitionPhase.FORWARD)) {
            coordinator.returnBack { onBack() }
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    val cachedPoster = remember(animeId) {
        if (coordinator?.activeAnimeId == animeId) coordinator.activePosterUrl
        else vm.findKnownAnime(animeId)?.cover
    }

    var tmdbEpisodes by remember(animeId) { mutableStateOf<Map<Int, TmdbEpisodeStill>>(emptyMap()) }
    var tmdbDetails by remember(animeId) { mutableStateOf<TmdbDetails?>(null) }
    var isTmdbSettled by remember(animeId) { mutableStateOf(false) }

    LaunchedEffect(animeId) {
        if (vm.selected?.id != animeId) {
            vm.openDetail(animeId)
        }
    }

    // Connect TMDB data with timeout (3.5s). If TMDB takes too long or fails, fall back immediately to AniList details
    LaunchedEffect(animeId) {
        isTmdbSettled = false
        try {
            com.nyyrox.aniverse.data.mapping.AniBridgeResolver.syncLatestMappings(context)
        } catch (_: Exception) {}

        val known = vm.findKnownAnime(animeId)
        val initialTitle = known?.title ?: vm.selected?.takeIf { it.id == animeId }?.title

        try {
            withTimeoutOrNull(3500L) {
                var titleToQuery = initialTitle
                var currentAnime = known ?: vm.selected?.takeIf { it.id == animeId }
                if (titleToQuery.isNullOrBlank()) {
                    while (titleToQuery.isNullOrBlank() && (vm.detailLoading || vm.selected?.id != animeId)) {
                        delay(50L)
                        currentAnime = vm.selected?.takeIf { it.id == animeId }
                        titleToQuery = currentAnime?.title
                    }
                }

                if (!titleToQuery.isNullOrBlank()) {
                    val year = currentAnime?.seasonYear
                    val format = currentAnime?.displayFormat
                    val romaji = currentAnime?.romajiTitle
                    val totalEps = currentAnime?.episodes ?: currentAnime?.nextEpisode

                    coroutineScope {
                        val detailsJob = async(Dispatchers.IO) {
                            try {
                                TmdbRepository.getDetails(animeId, titleToQuery, year, format)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        val epsJob = async(Dispatchers.IO) {
                            try {
                                val season = com.nyyrox.aniverse.data.mapping.AnimeSeasonMapper.resolveSeason(titleToQuery, romaji)
                                TmdbRepository.fetchEpisodesMapped(
                                    anilistId = animeId,
                                    title = titleToQuery,
                                    romajiTitle = romaji,
                                    season = season,
                                    totalEpisodes = totalEps,
                                    format = format
                                )
                            } catch (_: Exception) {
                                emptyMap()
                            }
                        }

                        val fetchedDetails = detailsJob.await()
                        val fetchedEps = epsJob.await()

                        if (fetchedDetails != null) {
                            tmdbDetails = fetchedDetails
                        }
                        if (fetchedEps.isNotEmpty()) {
                            tmdbEpisodes = fetchedEps
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // TMDB timed out or encountered an issue: seamlessly proceed with AniList details only
        } finally {
            isTmdbSettled = true
        }
    }

    val anime = vm.selected?.takeIf { it.id == animeId }

    if (vm.detailError != null && anime == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = handleBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
                    .size(44.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Unable to load details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = vm.detailError ?: "Error occurred",
                    fontSize = 13.sp,
                    color = OttTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.openDetail(animeId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val isAniListReady = anime != null && !vm.detailLoading
    val isReady = isAniListReady && isTmdbSettled

    if (!isReady || anime == null) {
        DetailWaveOscillatingSkeleton(
            cachedPoster = cachedPoster,
            onBack = handleBack,
            coordinator = coordinator
        )
        return
    }

    val isBookmarked = vm.isInWatchlist(anime.id)
    val maxEpisode = maxOf(anime.episodes ?: 1, anime.nextEpisode ?: 1)
    val historyItem = remember(anime.id, vm.history) { vm.history.firstOrNull { it.animeId == anime.id } }
    val initialEpisode = historyItem?.lastEpisode ?: 1

    // Episode selection & pagination states
    val pageSize = 25
    val pageCount = (maxEpisode + pageSize - 1) / pageSize
    var selectedPage by remember(anime.id) { mutableIntStateOf(((initialEpisode - 1) / pageSize).coerceIn(0, maxOf(0, pageCount - 1))) }
    var selectedEpisode by remember(anime.id) { mutableIntStateOf(initialEpisode) }
    var showMissingAnilistDialog by remember { mutableStateOf(false) }

    val downloadedAnimeList by vm.downloadManager.downloadedAnime.collectAsState()
    val activeAnimeTasks by vm.downloadManager.animeTasks.collectAsState()

    var selectedSourceMode by remember(anime.id) { mutableStateOf("DIRECT") }
    var selectedAudioMode by remember(anime.id) { mutableStateOf("SUB") }

    var availableDirectServers by remember(anime.id, selectedAudioMode, selectedEpisode) {
        mutableStateOf(com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudioMode))
    }

    var selectedProvider by remember(anime.id, selectedAudioMode, selectedSourceMode) {
        val preferred = vm.preferredServerId?.let { prefId -> ALL_PROVIDERS.firstOrNull { it.id == prefId } }
        mutableStateOf(preferred ?: availableDirectServers.firstOrNull() ?: SUB_PROVIDERS.first())
    }

    LaunchedEffect(anime.id, selectedAudioMode, selectedEpisode) {
        try {
            val servers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getAvailableDirectProviders(
                anilistId = anime.id,
                audioMode = selectedAudioMode,
                episode = selectedEpisode
            )
            if (servers.isNotEmpty()) {
                availableDirectServers = servers
                if (selectedSourceMode == "DIRECT" && servers.none { it.id == selectedProvider.id }) {
                    selectedProvider = servers.first()
                    vm.setPreferredServer(servers.first().id)
                }
            } else {
                availableDirectServers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudioMode)
            }
        } catch (_: Exception) {}
    }

    var isServerDropdownExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEpisodeGridView by remember { mutableStateOf(false) }
    var episodeSearchQuery by remember { mutableStateOf("") }
    var isOldestFirst by remember { mutableStateOf(true) }

    var showDownloadSheetEpisode by remember { mutableStateOf<Pair<Int, String?>?>(null) }
    var showDownloadRangeSheet by remember { mutableStateOf<IntRange?>(null) }

    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showWatchlistStatusDialog by remember { mutableStateOf(false) }

    // Hero backdrops collection: all TMDB backdrops + banner + cover
    val heroImages = remember(anime.id, tmdbDetails?.backdropUrl, tmdbDetails?.backdrops, anime.banner, anime.cover) {
        val list = mutableListOf<String>()
        tmdbDetails?.backdropUrl?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        tmdbDetails?.backdrops?.filter { it.isNotBlank() && !list.contains(it) }?.let { list.addAll(it) }
        anime.banner?.takeIf { it.isNotBlank() && !list.contains(it) }?.let { list.add(it) }
        if (list.isEmpty()) {
            anime.cover?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        }
        list.distinct()
    }

    var currentHeroIndex by remember(anime.id) { mutableIntStateOf(0) }

    // Auto loop change thumbnail every 5 seconds, fading to next. If only 1 available, does not loop.
    LaunchedEffect(anime.id, heroImages) {
        if (heroImages.size > 1) {
            while (isActive) {
                delay(5000L)
                currentHeroIndex = (currentHeroIndex + 1) % heroImages.size
            }
        }
    }

    val heroImageUrl = remember(heroImages, currentHeroIndex) {
        if (heroImages.isEmpty()) null
        else heroImages[currentHeroIndex % heroImages.size]
    }

    // Continuous subtle time-based Ken Burns zoom animation
    val infiniteTransition = rememberInfiniteTransition(label = "heroZoom")
    val heroTimeScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroScale"
    )

    val redBlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redDotBlink"
    )

    val lazyListState = rememberLazyListState()
    val scrollOffset by remember {
        derivedStateOf {
            val item0 = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            if (item0 != null) {
                (-item0.offset).toFloat().coerceAtLeast(0f)
            } else {
                val item1 = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }
                if (item1 != null) {
                    (320f - item1.offset.toFloat()).coerceAtLeast(0f)
                } else {
                    600f
                }
            }
        }
    }

    val isTransitioning = coordinator != null && coordinator.activeAnimeId == anime.id && coordinator.phase == SharedTransitionPhase.FORWARD
    val isPresented = !isTransitioning

    val contentAlpha by animateFloatAsState(
        targetValue = if (isPresented) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "detailContentAlpha"
    )

    val failedTaskForCurrentAnime = remember(activeAnimeTasks, anime.id) {
        activeAnimeTasks.values.firstOrNull { it.animeId == anime.id && it.status == com.nyyrox.aniverse.data.download.DownloadStatus.FAILED }
    }

    val revealAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "detailRevealAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .graphicsLayer {
                alpha = revealAlpha * (if (coordinator?.phase == SharedTransitionPhase.BACKWARD) coordinator.progress.value else 1f)
                translationY = (1f - revealAlpha) * 16f
            }
    ) {
        // === 1. HERO IMAGE AT BG IN ITS PLACE (PARALLAX + TIME-BASED KEN BURNS) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .graphicsLayer {
                    translationY = -scrollOffset * 0.42f
                    alpha = (1f - scrollOffset / 600f).coerceIn(0f, 1f)
                    clip = true
                }
                .onGloballyPositioned { coords ->
                    coordinator?.updateTargetBounds(coords.boundsInWindow())
                }
        ) {
            Crossfade(
                targetState = heroImageUrl,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "heroThumbnailCrossfade",
                modifier = Modifier.fillMaxSize()
            ) { imgUrl ->
                AsyncImage(
                    model = imgUrl,
                    contentDescription = anime.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = heroTimeScale
                            scaleY = heroTimeScale
                        },
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x35000000),
                                Color(0x10000000),
                                Color(0x800B0E14),
                                OttBg,
                                OttBg
                            )
                        )
                    )
            )
        }

        // === 2. SCROLLABLE FOREGROUND CONTENT ===
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Title Overlay space at bottom of hero area
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer { alpha = contentAlpha }
                    ) {
                        // Title row: logo/text left, Play button right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val logoUrl = tmdbDetails?.logoUrl?.takeIf { it.isNotBlank() }
                                if (logoUrl != null) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = anime.displayTitle,
                                        modifier = Modifier
                                            .heightIn(max = 58.dp)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.CenterStart
                                    )
                                } else {
                                    Text(
                                        text = tmdbDetails?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: anime.displayTitle,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 2,
                                        lineHeight = 28.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (anime.id <= 0) showMissingAnilistDialog = true
                                        else onPlay(anime.id, selectedEpisode, selectedProvider)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        val romajiTitle = anime.romajiTitle?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals(anime.displayTitle, ignoreCase = true) }
                        if (romajiTitle != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = romajiTitle,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFA0A4B2),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        tmdbDetails?.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"$tagline\"",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = OttSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // === 2. METADATA HEADER & DETAILS ===
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Line 1: Season & Year · Episodes (hidden for movie) · Format (Series / Movie)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val seasonYearText = anime.displaySeason.ifBlank { anime.displayYear }
                        if (seasonYearText.isNotBlank()) {
                            Text(
                                text = seasonYearText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text("·", color = Color(0x88FFFFFF), fontSize = 13.sp)
                        }
                        if (!anime.isMovie) {
                            Text(
                                text = "$maxEpisode Episodes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text("·", color = Color(0x88FFFFFF), fontSize = 13.sp)
                        }
                        Text(
                            text = if (anime.isMovie) "Movie" else anime.displayFormat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = OttSecondary
                        )
                    }

                    // Line 2: Liked Percentage & Rating
                    val scorePercent = anime.score ?: tmdbDetails?.voteAverage?.let { (it * 10).toInt() }
                    if (scorePercent != null && scorePercent > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = OttAccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$scorePercent% of viewers liked this",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Line 3: Next Airing Badge (White background text box with blinking red dot)
                    val isReleasing = anime.status.equals("RELEASING", ignoreCase = true) ||
                            anime.status.equals("AIRING", ignoreCase = true) ||
                            (anime.nextAt != null && (anime.nextAt * 1000L) > System.currentTimeMillis())

                    if (isReleasing && anime.nextAt != null) {
                        val nextEpNum = anime.nextEpisode ?: (maxEpisode + 1)
                        val countdown = anime.getAiringCountdown()
                        val formattedDate = anime.getFormattedAiringDate()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Small blinking red dot on top left
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914).copy(alpha = redBlinkAlpha))
                                )
                                Column {
                                    val nextTitle = buildString {
                                        append("Next Airing: Episode ")
                                        append(nextEpNum)
                                        if (!countdown.isNullOrBlank()) {
                                            append(" · in ")
                                            append(countdown)
                                        }
                                    }
                                    Text(
                                        text = nextTitle,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    if (!formattedDate.isNullOrBlank()) {
                                        Text(
                                            text = formattedDate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF444444)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Line 4: Synopsis / Overview (Show more/less ONLY if text is long)
                    val displayOverview = tmdbDetails?.overview?.ifBlank { null } ?: anime.cleanDescription
                    val isLongOverview = displayOverview.length > 170
                    Column {
                        Text(
                            text = displayOverview,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xDDFFFFFF),
                            maxLines = if (isSynopsisExpanded || !isLongOverview) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isLongOverview) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isSynopsisExpanded) "Show Less" else "...more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            )
                        }
                    }

                    // Line 5: Detailed Info Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OttCardShape)
                            .background(Color(0x12FFFFFF))
                            .border(1.dp, Color(0x1EFFFFFF), OttCardShape)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecRow("Status", anime.formattedStatus)
                        val combinedGenres = (anime.genres + tmdbDetails?.genres.orEmpty()).distinct()
                        if (combinedGenres.isNotEmpty()) {
                            SpecRow("This show is about", combinedGenres.take(4).joinToString(", "))
                        }
                        val durVal = tmdbDetails?.episodeRunTime?.let { "$it min per episode" }
                            ?: anime.duration?.let { "$it min per episode" } ?: "24 min per episode"
                        SpecRow("Duration", durVal)
                        val studio = anime.studios.firstOrNull()?.ifBlank { null } ?: "Unknown"
                        SpecRow("Studio", studio)
                        SpecRow("Source", anime.displaySource)
                    }

                    // Action Row: Watchlist only (Play is now on hero banner)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showWatchlistStatusDialog = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x28FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isBookmarked) OttAccentGold else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isBookmarked) "In Watchlist" else "Watchlist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = OttTextMuted
                            )
                        }
                    }
                }
            }

            // === 3. CONTENT TABS (Sticky Header with Videos tab) ===
            stickyHeader {
                val tabs = if (anime.isMovie) {
                    listOf("Characters", "Casts", "Videos", "How to Watch", "More Like This")
                } else {
                    listOf("Episodes", "Characters", "Casts", "Videos", "How to Watch", "More Like This")
                }

                val activeTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OttBg)
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = activeTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 18.dp,
                        indicator = { tabPositions ->
                            if (activeTabIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTabIndex]),
                                    color = Color.White,
                                    height = 2.5.dp
                                )
                            }
                        },
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x20FFFFFF))
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = activeTabIndex == index,
                                onClick = { selectedTab = index },
                                text = {
                                    val countBadge = when (title) {
                                        "Episodes" -> " ($maxEpisode)"
                                        "Characters" -> if (anime.characters.isNotEmpty()) " (${anime.characters.size})" else ""
                                        "How to Watch" -> if (anime.relations.isNotEmpty()) " (${anime.relations.size})" else ""
                                        else -> ""
                                    }
                                    Text(
                                        text = "$title$countBadge",
                                        fontSize = 13.sp,
                                        fontWeight = if (activeTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (activeTabIndex == index) Color.White else OttTextSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // === 4. TAB CONTENTS ===
            val currentTabs = if (anime.isMovie) {
                listOf("Characters", "Casts", "Videos", "How to Watch", "More Like This")
            } else {
                listOf("Episodes", "Characters", "Casts", "Videos", "How to Watch", "More Like This")
            }
            val currentTabTitle = currentTabs.getOrElse(selectedTab.coerceIn(0, currentTabs.lastIndex)) { currentTabs.first() }

            when (currentTabTitle) {
                // --- TAB: EPISODES ---
                "Episodes" -> {
                    // Controls Row: Search, Episode Count Badge, Sort Oldest/Newest, Range Pills, View Toggle
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search Box without text cut-off (using BasicTextField)
                                BasicTextField(
                                    value = episodeSearchQuery,
                                    onValueChange = { episodeSearchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    decorationBox = { innerTextField ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(OttPillShape)
                                                .background(Color(0x18FFFFFF))
                                                .border(1.dp, Color(0x25FFFFFF), OttPillShape)
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = OttTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                                if (episodeSearchQuery.isEmpty()) {
                                                    Text(
                                                        "Search episode...",
                                                        fontSize = 13.sp,
                                                        color = OttTextMuted
                                                    )
                                                }
                                                innerTextField()
                                            }
                                            if (episodeSearchQuery.isNotBlank()) {
                                                IconButton(
                                                    onClick = { episodeSearchQuery = "" },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )

                                // Sort Oldest / Newest Toggle Button
                                IconButton(
                                    onClick = { isOldestFirst = !isOldestFirst },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(OttPillShape)
                                        .background(Color(0x15FFFFFF))
                                        .border(1.dp, Color(0x20FFFFFF), OttPillShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Sort Order",
                                        tint = if (!isOldestFirst) OttAccentGold else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Grid vs List Toggle
                                IconButton(
                                    onClick = { isEpisodeGridView = !isEpisodeGridView },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(OttPillShape)
                                        .background(Color(0x15FFFFFF))
                                        .border(1.dp, Color(0x20FFFFFF), OttPillShape)
                                ) {
                                    Icon(
                                        imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                                        contentDescription = "Toggle View",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Episode Range Selector Pills (1-25, 26-50, etc.)
                            if (pageCount > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(pageCount) { page ->
                                            val pStart = page * pageSize + 1
                                            val pEnd = minOf(pStart + pageSize - 1, maxEpisode)
                                            val isSelected = selectedPage == page

                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(if (isSelected) Color.White else Color(0x14FFFFFF))
                                                    .border(1.dp, if (isSelected) Color.White else Color(0x22FFFFFF), OttPillShape)
                                                    .clickable { selectedPage = page }
                                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "$pStart–$pEnd",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.Black else OttTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    val curStart = selectedPage * pageSize + 1
                                    val curEnd = minOf(curStart + pageSize - 1, maxEpisode)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x20FFFFFF))
                                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { showDownloadRangeSheet = curStart..curEnd }
                                            .padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("Download All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Episode List / Grid Content
                    item {
                        val rawEpisodes = if (episodeSearchQuery.isNotBlank()) {
                            (1..maxEpisode).filter { epNum ->
                                val epTitle = tmdbEpisodes[epNum]?.name.orEmpty()
                                epNum.toString() == episodeSearchQuery.trim() || epTitle.contains(episodeSearchQuery.trim(), ignoreCase = true)
                            }
                        } else {
                            val pStart = selectedPage * pageSize + 1
                            val pEnd = minOf(pStart + pageSize - 1, maxEpisode)
                            (pStart..pEnd).toList()
                        }

                        val pageEpisodes = if (isOldestFirst) rawEpisodes else rawEpisodes.reversed()

                        if (pageEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (episodeSearchQuery.isNotBlank()) "No episodes match \"$episodeSearchQuery\"" else "No episodes available",
                                    color = OttTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (isEpisodeGridView) {
                            // Compact Grid View
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val chunked = pageEpisodes.chunked(5)
                                chunked.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { epNum ->
                                            val isCurrent = epNum == selectedEpisode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isCurrent) Color.White else Color(0x18FFFFFF))
                                                    .border(1.dp, if (isCurrent) Color.White else Color(0x25FFFFFF), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedEpisode = epNum
                                                    if (anime.id <= 0) showMissingAnilistDialog = true
                                                    else onPlay(anime.id, epNum, selectedProvider)
                                                },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$epNum",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCurrent) Color.Black else Color.White
                                                )
                                            }
                                        }
                                        repeat(5 - row.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            // Anikura-style Rich 16:9 Episode Cards
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                pageEpisodes.forEachIndexed { epIndex, epNum ->
                                    val epStillInfo = tmdbEpisodes[epNum]
                                    val isDownloaded = vm.isAnimeEpisodeDownloaded(anime.id, epNum)
                                    val isDownloading = activeAnimeTasks.containsKey("${anime.id}:$epNum")
                                    val isSelected = epNum == selectedEpisode

                                    val savedPos = vm.getSavedPosition(anime.id, epNum)
                                    val dur = (anime.duration ?: 24) * 60 * 1000L
                                    val progressRatio = if (dur > 0 && savedPos > 0) (savedPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

                                    // 3D Staggered Entrance Animation
                                    val animState = remember { mutableStateOf(false) }
                                    LaunchedEffect(epNum) { animState.value = true }
                                    val cardAlpha by animateFloatAsState(
                                        targetValue = if (animState.value) 1f else 0f,
                                        animationSpec = tween(durationMillis = 300, delayMillis = (epIndex * 35).coerceAtMost(280)),
                                        label = "ep_alpha_$epNum"
                                    )
                                    val cardScale by animateFloatAsState(
                                        targetValue = if (animState.value) 1f else 0.94f,
                                        animationSpec = tween(durationMillis = 300, delayMillis = (epIndex * 35).coerceAtMost(280)),
                                        label = "ep_scale_$epNum"
                                    )
                                    val cardRotationX by animateFloatAsState(
                                        targetValue = if (animState.value) 0f else 10f,
                                        animationSpec = tween(durationMillis = 300, delayMillis = (epIndex * 35).coerceAtMost(280)),
                                        label = "ep_rot_$epNum"
                                    )

                                    var itemTopInWindow by remember { mutableStateOf(Float.MAX_VALUE) }
                                    var itemHeightPx by remember { mutableStateOf(1f) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                itemTopInWindow = coords.boundsInWindow().top
                                                itemHeightPx = coords.size.height.toFloat()
                                            }
                                            .graphicsLayer {
                                                val baseAlpha = cardAlpha
                                                val baseScale = cardScale

                                                // Top dismiss threshold near sticky tab bar
                                                val dismissThreshold = 220f * density
                                                if (itemTopInWindow < dismissThreshold) {
                                                    val progress = ((dismissThreshold - itemTopInWindow) / (itemHeightPx * 0.7f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                    scaleX = baseScale * (1f - progress * 0.12f)
                                                    scaleY = baseScale * (1f - progress * 0.12f)
                                                    rotationX = cardRotationX + progress * 15f
                                                    alpha = baseAlpha * (1f - progress * 0.85f).coerceIn(0f, 1f)
                                                    translationY = -progress * 22f * density
                                                } else {
                                                    val zoomZone = dismissThreshold + itemHeightPx * 0.9f
                                                    if (itemTopInWindow < zoomZone) {
                                                        val fwdProg = ((zoomZone - itemTopInWindow) / (itemHeightPx * 0.9f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                        scaleX = baseScale * (0.95f + fwdProg * 0.05f)
                                                        scaleY = baseScale * (0.95f + fwdProg * 0.05f)
                                                        rotationX = cardRotationX - (1f - fwdProg) * 4f
                                                        alpha = baseAlpha
                                                        translationY = 0f
                                                    } else {
                                                        scaleX = baseScale * 0.95f
                                                        scaleY = baseScale * 0.95f
                                                        rotationX = cardRotationX
                                                        alpha = baseAlpha
                                                        translationY = 0f
                                                    }
                                                }
                                                cameraDistance = 12f * density
                                            }
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) Color(0x25FFFFFF) else Color(0x10FFFFFF))
                                            .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedEpisode = epNum
                                                if (anime.id <= 0) showMissingAnilistDialog = true
                                                else onPlay(anime.id, epNum, selectedProvider)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        // 16:9 Thumbnail Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141722))
                                        ) {
                                            val epThumbnail = epStillInfo?.stillUrl?.takeIf { it.isNotBlank() }
                                                ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                                                ?: tmdbDetails?.backdropUrl?.takeIf { it.isNotBlank() }
                                                ?: heroImageUrl?.takeIf { it.isNotBlank() }
                                                ?: anime.banner?.takeIf { it.isNotBlank() }
                                                ?: anime.cover

                                            SubcomposeAsyncImage(
                                                model = epThumbnail,
                                                contentDescription = epStillInfo?.name ?: "Episode $epNum",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    ShimmerBox(
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                },
                                                error = {
                                                    AsyncImage(
                                                        model = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                                                        contentDescription = epStillInfo?.name ?: "Episode $epNum",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            )

                                            // Dark gradient overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color(0xAA000000))
                                                        )
                                                    )
                                            )

                                            // Play Icon in Center
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x60000000))
                                                    .border(1.dp, Color(0x40FFFFFF), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // Episode Badge Bottom Left (e.g. Episode 1)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xCC000000))
                                                    .border(0.6.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "Episode $epNum",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            // Duration Bottom Right (e.g. 24m)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xCC000000))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                val runMin = tmdbDetails?.episodeRunTime ?: anime.duration ?: 24
                                                Text(
                                                    text = "${runMin}m",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xDDFFFFFF)
                                                )
                                            }

                                            // Download Button Top Right
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x80000000))
                                                    .clickable {
                                                        if (!isDownloaded && !isDownloading) {
                                                            showDownloadSheetEpisode = epNum to (epStillInfo?.name ?: "Episode $epNum")
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when {
                                                    isDownloaded -> Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                    isDownloading -> Icon(Icons.Default.Downloading, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(16.dp))
                                                    else -> Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            // Watch progress bar at bottom edge
                                            if (progressRatio > 0.05f) {
                                                LinearProgressIndicator(
                                                    progress = { progressRatio },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .align(Alignment.BottomCenter),
                                                    color = Color.White,
                                                    trackColor = Color(0x40FFFFFF)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // Episode Title
                                        Text(
                                            text = epStillInfo?.name ?: "Episode $epNum",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Episode Description
                                        val desc = epStillInfo?.overview?.takeIf { it.isNotBlank() } ?: "sit back and enjoy Episode $epNum of ${anime.displayTitle}."
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = OttTextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- TAB: CHARACTERS (3D Card Swipe Deck) ---
                "Characters" -> {
                    item {
                        val characters = anime.characters
                        if (characters.isEmpty() && vm.detailLoading) {
                            // Skeleton shimmer placeholders while characters load
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp)
                            ) {
                                items(6) {
                                    Column(
                                        modifier = Modifier
                                            .width(152.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x14FFFFFF))
                                            .padding(10.dp)
                                    ) {
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.78f)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        ShimmerBox(
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        } else if (characters.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No character information available.", color = OttTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            val charListState = rememberLazyListState()
                            LazyRow(
                                state = charListState,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp)
                            ) {
                                itemsIndexed(characters, key = { index, it -> "char_${it.id}_$index" }) { charIdx, char ->
                                    val itemInfo = remember {
                                        derivedStateOf {
                                            charListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == charIdx }
                                        }
                                    }
                                    val rotY = remember {
                                        derivedStateOf {
                                            val info = itemInfo.value ?: return@derivedStateOf 0f
                                            val viewportCenter = (charListState.layoutInfo.viewportStartOffset + charListState.layoutInfo.viewportEndOffset) / 2f
                                            val itemCenter = info.offset + info.size / 2f
                                            val delta = (itemCenter - viewportCenter) / charListState.layoutInfo.viewportEndOffset.toFloat().coerceAtLeast(1f)
                                            (-delta * 30f).coerceIn(-35f, 35f)
                                        }
                                    }
                                    val cardScale = remember {
                                        derivedStateOf {
                                            val info = itemInfo.value ?: return@derivedStateOf 1f
                                            val viewportCenter = (charListState.layoutInfo.viewportStartOffset + charListState.layoutInfo.viewportEndOffset) / 2f
                                            val itemCenter = info.offset + info.size / 2f
                                            val delta = kotlin.math.abs((itemCenter - viewportCenter) / charListState.layoutInfo.viewportEndOffset.toFloat().coerceAtLeast(1f))
                                            (1f - (delta * 0.14f)).coerceIn(0.86f, 1f)
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .width(152.dp)
                                            .graphicsLayer {
                                                rotationY = rotY.value
                                                scaleX = cardScale.value
                                                scaleY = cardScale.value
                                                cameraDistance = 8f * density
                                            }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x14FFFFFF))
                                            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(14.dp))
                                            .padding(10.dp)
                                    ) {
                                        // Character Portrait + Role Badge
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.78f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0x22FFFFFF))
                                        ) {
                                            AsyncImage(
                                                model = char.image,
                                                contentDescription = char.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (char.role.equals("MAIN", ignoreCase = true)) Color(0xD0E50914) else Color(0xB0000000))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = char.role.lowercase().replaceFirstChar { it.uppercase() },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        // Character Names
                                        Text(
                                            text = char.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!char.nativeName.isNullOrBlank()) {
                                            Text(
                                                text = char.nativeName,
                                                fontSize = 10.5.sp,
                                                color = OttTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Voice Actor
                                        if (!char.voiceActorName.isNullOrBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                AsyncImage(
                                                    model = char.voiceActorImage,
                                                    contentDescription = char.voiceActorName,
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .border(0.6.dp, Color(0x30FFFFFF), CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = char.voiceActorName,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xEEFFFFFF),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text("JP VA", fontSize = 8.5.sp, color = OttTextMuted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- TAB: CASTS & STAFF (Uniform Size Cards) ---
                "Casts" -> {
                    item {
                        val staffList = anime.staff
                        val tmdbCast = tmdbDetails?.cast.orEmpty()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Key Production Staff (AniList)
                            if (staffList.isNotEmpty()) {
                                Text(
                                    text = "PRODUCTION STAFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = OttSecondary
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(staffList, key = { index, it -> "st_${it.id}_$index" }) { _, staff ->
                                        Column(
                                            modifier = Modifier
                                                .width(104.dp)
                                                .height(154.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x12FFFFFF))
                                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AsyncImage(
                                                model = staff.image,
                                                contentDescription = staff.name,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color(0x30FFFFFF), CircleShape)
                                                    .background(Color(0x22FFFFFF)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = staff.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = staff.role,
                                                    fontSize = 9.sp,
                                                    color = OttTextMuted,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // TMDB Voice & Screen Cast
                            if (tmdbCast.isNotEmpty()) {
                                Text(
                                    text = "VOICE CAST (TMDB)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = OttSecondary
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(tmdbCast, key = { index, it -> "tmdbc_${it.id}_$index" }) { _, member ->
                                        Column(
                                            modifier = Modifier
                                                .width(104.dp)
                                                .height(154.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x12FFFFFF))
                                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AsyncImage(
                                                model = member.profileUrl,
                                                contentDescription = member.name,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color(0x30FFFFFF), CircleShape)
                                                    .background(Color(0x22FFFFFF)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                member.character?.let { charName ->
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = charName,
                                                        fontSize = 9.sp,
                                                        color = OttTextSecondary,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- TAB: VIDEOS (Trailers, Teasers, Clips with safe YouTube Launch) ---
                "Videos" -> {
                    item {
                        val allVideos = remember(anime.trailerId, tmdbDetails?.videos, tmdbDetails?.trailerUrl) {
                            val list = mutableListOf<TmdbVideo>()
                            // 1. AniList official trailer
                            val rawAnimeTrailer = anime.trailerId
                            if (!rawAnimeTrailer.isNullOrBlank()) {
                                val trimmed = rawAnimeTrailer.trim()
                                val key = when {
                                    trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
                                    trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                                    trimmed.contains("embed/") -> trimmed.substringAfter("embed/").substringBefore("?").substringBefore("&")
                                    else -> trimmed
                                }.trim()
                                if (key.isNotEmpty()) {
                                    list.add(TmdbVideo(key = key, name = "Official Trailer", type = "Trailer"))
                                }
                            }
                            // 2. TMDB videos
                            tmdbDetails?.videos?.forEach { vid ->
                                if (list.none { it.key == vid.key }) {
                                    list.add(vid)
                                }
                            }
                            // 3. Fallback TMDB single trailerUrl if list is empty
                            if (list.isEmpty()) {
                                val tKey = tmdbDetails?.trailerUrl
                                if (!tKey.isNullOrBlank()) {
                                    list.add(TmdbVideo(key = tKey.trim(), name = "Trailer", type = "Trailer"))
                                }
                            }
                            list
                        }

                        if (allVideos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No videos or trailers available.", color = OttTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                allVideos.forEachIndexed { vIdx, video ->
                                    val vidKey = video.key
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x12FFFFFF))
                                            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(16.dp))
                                            .clickable {
                                                val ytAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$vidKey")).apply {
                                                    putExtra("force_fullscreen", true)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(ytAppIntent)
                                                } catch (_: Exception) {
                                                    try {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$vidKey")).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(webIntent)
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 16:9 Thumbnail Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF141722))
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = "https://img.youtube.com/vi/$vidKey/hqdefault.jpg",
                                                contentDescription = video.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    ShimmerBox(
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                }
                                            )

                                            // Dark gradient overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color(0x30000000), Color(0x75000000))
                                                        )
                                                    )
                                            )

                                            // Center Play Button
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(50.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xDD000000))
                                                    .border(1.5.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Play Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            // Video Type Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xD0000000))
                                                    .border(0.6.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = video.type.uppercase(Locale.US),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        // Video Title & Open action
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = video.name,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.OpenInNew,
                                                    contentDescription = null,
                                                    tint = OttSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Watch",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = OttSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- TAB: HOW TO WATCH (Watch Order, Prequels, Sequels, Adaptations) ---
                "How to Watch" -> {
                    item {
                        val relations = anime.relations
                        if (relations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No watch order or relation data available.", color = OttTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            val prequels = relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
                            val sequels = relations.filter { it.relationType.equals("SEQUEL", ignoreCase = true) }
                            val adaptations = relations.filter { it.relationType.equals("ADAPTATION", ignoreCase = true) || it.relationType.equals("SOURCE", ignoreCase = true) }
                            val others = relations.filter { it !in prequels && it !in sequels && it !in adaptations }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Prequels (Watch First)
                                if (prequels.isNotEmpty()) {
                                    RelationGroupSection(
                                        title = "PREQUEL (WATCH BEFORE)",
                                        relations = prequels,
                                        vm = vm
                                    )
                                }

                                // Sequels (Watch Next)
                                if (sequels.isNotEmpty()) {
                                    RelationGroupSection(
                                        title = "SEQUEL (WATCH NEXT)",
                                        relations = sequels,
                                        vm = vm
                                    )
                                }

                                // Source / Manga Adaptation
                                if (adaptations.isNotEmpty()) {
                                    RelationGroupSection(
                                        title = "ORIGINAL SOURCE & ADAPTATION",
                                        relations = adaptations,
                                        vm = vm
                                    )
                                }

                                // Side Stories & Others
                                if (others.isNotEmpty()) {
                                    RelationGroupSection(
                                        title = "SIDE STORIES & ALTERNATIVE VERSIONS",
                                        relations = others,
                                        vm = vm
                                    )
                                }
                            }
                        }
                    }
                }

                // --- TAB: MORE LIKE THIS (3D Card Swipe Deck) ---
                "More Like This" -> {
                    item {
                        val recs = if (anime.recommendations.isNotEmpty()) {
                            anime.recommendations
                        } else {
                            (vm.popular + vm.trending).filter { it.id != anime.id }.take(12).map {
                                AnimeRecommendation(
                                    id = it.id,
                                    title = it.displayTitle,
                                    romajiTitle = it.romajiTitle,
                                    cover = it.cover,
                                    banner = it.banner,
                                    score = it.score,
                                    format = it.displayFormat,
                                    episodes = it.episodes
                                )
                            }
                        }

                        if (recs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recommendations found.", color = OttTextMuted, fontSize = 13.sp)
                            }
                        } else {
                            val recListState = rememberLazyListState()
                            LazyRow(
                                state = recListState,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp)
                            ) {
                                itemsIndexed(recs, key = { index, it -> "rec_${it.id}_$index" }) { _, rec ->
                                    Box(
                                        modifier = Modifier.width(140.dp)
                                    ) {
                                        RecommendationPosterItem(rec = rec, onClick = { vm.openDetail(rec.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pinned Top Bar with Scrolled Title, Quick Play Button, and Bare Watchlist Icon
        val isScrolledPastHero = scrollOffset > 380f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isScrolledPastHero) Modifier.background(OttBg)
                    else Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color(0x66000000),
                                Color.Transparent
                            )
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Bare Back Button
            IconButton(
                onClick = handleBack,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Scrolled Title + Play Button
            AnimatedVisibility(
                visible = isScrolledPastHero,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(180)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play Logo in Top Bar
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                if (anime.id <= 0) showMissingAnilistDialog = true
                                else onPlay(anime.id, selectedEpisode, selectedProvider)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = anime.displayTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (anime.isMovie) "Movie" else "Ep $selectedEpisode of $maxEpisode",
                            fontSize = 11.sp,
                            color = OttTextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!isScrolledPastHero) {
                Spacer(Modifier.weight(1f))
            }

            // Watchlist in Top Right: BARE ICON, NO OUTER CIRCLE
            IconButton(
                onClick = { showWatchlistStatusDialog = true },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Watchlist",
                    tint = if (isBookmarked) OttAccentGold else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            }
        }
    }

    // === DIALOGS & BOTTOM SHEETS ===
    if (showWatchlistStatusDialog) {
        val watchlistItem = vm.getWatchlistItem(anime.id)
        val isItemSaved = watchlistItem != null
        val currentStatus = watchlistItem?.watchlistStatus ?: WatchlistStatus.PLAN_TO_WATCH
        val currentEp = watchlistItem?.progressEpisode ?: 0
        val currentTags = watchlistItem?.customTags ?: emptyList()
        WatchlistStatusSelectorDialog(
            title = anime.title,
            currentStatus = currentStatus,
            currentEpisode = currentEp,
            totalEpisodes = anime.episodes ?: anime.nextEpisode,
            initialCustomTags = currentTags,
            isNewItem = !isItemSaved,
            onDismiss = { showWatchlistStatusDialog = false },
            onSave = { newStatus, newEp, newTags ->
                vm.updateWatchlistDetails(
                    animeId = anime.id,
                    status = newStatus,
                    progressEpisode = newEp,
                    customTags = newTags,
                    anime = anime
                )
                showWatchlistStatusDialog = false
            },
            onRemove = if (isItemSaved) {
                {
                    vm.removeFromWatchlist(anime.id)
                    showWatchlistStatusDialog = false
                }
            } else null
        )
    }

    showDownloadSheetEpisode?.let { (ep, epTitle) ->
        com.nyyrox.aniverse.ui.components.DownloadAnimeOptionsSheet(
            anime = anime,
            episode = ep,
            episodeTitle = epTitle,
            initialProvider = selectedProvider,
            availableDirectServers = availableDirectServers,
            onDismiss = { showDownloadSheetEpisode = null },
            onStartDownload = { prov, quality, audio ->
                vm.downloadAnimeEpisode(
                    anime = anime,
                    episode = ep,
                    provider = prov,
                    episodeTitle = epTitle,
                    quality = quality,
                    audio = audio
                )
                android.widget.Toast.makeText(context, "Downloading Ep $ep ($quality · $audio)...", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    showDownloadRangeSheet?.let { range ->
        com.nyyrox.aniverse.ui.components.DownloadAnimeOptionsSheet(
            anime = anime,
            episodesRange = range,
            initialProvider = selectedProvider,
            availableDirectServers = availableDirectServers,
            onDismiss = { showDownloadRangeSheet = null },
            onStartBatchDownload = { eps, prov, quality, audio ->
                vm.downloadAnimeEpisodes(
                    anime = anime,
                    episodes = eps,
                    provider = prov,
                    quality = quality,
                    audio = audio
                )
                android.widget.Toast.makeText(
                    context,
                    "Downloading ${eps.size} episodes (${eps.first()}–${eps.last()}) on ${prov.name} ($quality · $audio)...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    failedTaskForCurrentAnime?.let { failedTask ->
        AlertDialog(
            onDismissRequest = {
                vm.downloadManager.cancelTask(
                    com.nyyrox.aniverse.data.download.DownloadType.ANIME,
                    failedTask.animeId.toString(),
                    failedTask.episode.toString()
                )
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Download Stopped", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    text = failedTask.error ?: "Download stopped on ${failedTask.providerName}. Would you like to retry or change server?",
                    color = OttTextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ep = failedTask.episode
                        val epTitle = failedTask.episodeTitle
                        vm.downloadManager.retryAnimeDownload(
                            anime = anime,
                            episode = ep,
                            provider = null,
                            episodeTitle = epTitle,
                            quality = failedTask.quality,
                            audio = failedTask.audio
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry (${failedTask.providerName})", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val ep = failedTask.episode
                            val epTitle = failedTask.episodeTitle
                            vm.downloadManager.cancelTask(
                                com.nyyrox.aniverse.data.download.DownloadType.ANIME,
                                failedTask.animeId.toString(),
                                failedTask.episode.toString()
                            )
                            showDownloadSheetEpisode = ep to epTitle
                        }
                    ) {
                        Text("Change Server", color = OttAccentGold, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            vm.downloadManager.cancelTask(
                                com.nyyrox.aniverse.data.download.DownloadType.ANIME,
                                failedTask.animeId.toString(),
                                failedTask.episode.toString()
                            )
                        }
                    ) {
                        Text("Cancel", color = OttTextMuted)
                    }
                }
            },
            containerColor = Color(0xFF141722),
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showMissingAnilistDialog) {
        AlertDialog(
            onDismissRequest = { showMissingAnilistDialog = false },
            icon = {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Source Link Unavailable",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This title could not be resolved on AniList. Streaming is unavailable for this entry.",
                    fontSize = 13.5.sp,
                    color = OttTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showMissingAnilistDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = OttPillShape
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141722),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OttTextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RelationGroupSection(
    title: String,
    relations: List<MediaRelation>,
    vm: MainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = OttSecondary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(relations, key = { index, it -> "rel_${it.id}_$index" }) { _, rel ->
                val isNovel = rel.format.equals("NOVEL", ignoreCase = true) ||
                    rel.format.equals("LIGHT_NOVEL", ignoreCase = true)
                val isManga = rel.type.equals("MANGA", ignoreCase = true) ||
                    rel.format.equals("MANGA", ignoreCase = true) ||
                    rel.format.equals("ONE_SHOT", ignoreCase = true) ||
                    rel.relationType.equals("ADAPTATION", ignoreCase = true)

                val displayBadge = when {
                    isNovel -> "NOVEL"
                    isManga -> "MANGA"
                    else -> rel.format ?: "ANIME"
                }

                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x12FFFFFF))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                        .clickable {
                            if (isNovel) {
                                vm.openNovelDetail(rel.title)
                            } else if (isManga) {
                                vm.openMangaDetail(rel.id.toString(), rel.title)
                            } else {
                                vm.openDetail(rel.id)
                            }
                        }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22FFFFFF))
                    ) {
                        AsyncImage(
                            model = rel.cover,
                            contentDescription = rel.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Format Badge on top corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(0.5.dp, Color(0x35FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = displayBadge,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = rel.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = if (isNovel || isManga) "Read $displayBadge ➔" else rel.format.orEmpty(),
                        fontSize = 10.sp,
                        fontWeight = if (isNovel || isManga) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isNovel || isManga) Color.White else OttSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationPosterItem(
    rec: AnimeRecommendation,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x18FFFFFF))
        ) {
            AsyncImage(
                model = rec.cover,
                contentDescription = rec.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            rec.score?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xB0000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = OttAccentGold,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "$score%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = rec.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            lineHeight = 15.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Oscillating wave skeleton placeholder for the Detail screen media card.
 * Slowly oscillates like a wave with gentle scale/breathing animations while AniList & TMDB data load.
 */
@Composable
private fun DetailWaveOscillatingSkeleton(
    cachedPoster: String?,
    onBack: () -> Unit,
    coordinator: SharedPosterCoordinator?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "detailWaveTransition")

    // Slow wave phase translation across the media card
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Gentle wave breathing / alpha oscillation
    val waveBreathe by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBreathe"
    )

    // Subtle fluid wave scale oscillation
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.990f,
        targetValue = 1.010f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    val waveBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131722).copy(alpha = 0.65f),
            Color(0xFF1E2638).copy(alpha = 0.85f),
            Color(0xFF323F58).copy(alpha = 0.95f),
            Color(0xFF1E2638).copy(alpha = 0.85f),
            Color(0xFF131722).copy(alpha = 0.65f),
        ),
        start = Offset((wavePhase * 1600f) - 600f, (wavePhase * 600f) - 200f),
        end = Offset((wavePhase * 1600f) + 400f, (wavePhase * 600f) + 400f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .then(
                if (coordinator?.phase == SharedTransitionPhase.BACKWARD) {
                    Modifier.graphicsLayer { alpha = coordinator.progress.value }
                } else Modifier
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Media Card oscillating like a wave
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(410.dp)
                        .graphicsLayer {
                            scaleX = waveScale
                            scaleY = waveScale
                        }
                        .onGloballyPositioned { coords ->
                            coordinator?.updateTargetBounds(coords.boundsInWindow())
                        }
                ) {
                    if (!cachedPoster.isNullOrBlank()) {
                        AsyncImage(
                            model = cachedPoster,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (coordinator?.phase == SharedTransitionPhase.FORWARD) 0f else 1f
                                },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(OttHeroGradient)
                        )
                        // Translucent oscillating wave overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = waveBreathe * 0.45f }
                                .background(waveBrush)
                        )
                    } else {
                        // Media card silhouette with wave brush
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(waveBrush)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(OttHeroGradient)
                        )
                    }

                    // Bottom overlays of Hero Card: Title skeleton, subtitle, and Play button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(waveBrush)
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.45f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(waveBrush)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(waveBrush)
                            )
                        }
                    }
                }
            }

            // 2. Metadata Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(56.dp, 24.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(68.dp, 24.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(60.dp, 24.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(52.dp, 24.dp).clip(OttPillShape).background(waveBrush))
                }
            }

            // 3. Action Buttons Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(46.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(waveBrush))
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(waveBrush))
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(waveBrush))
                }
            }

            // 4. Synopsis Skeleton Lines
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                    Box(modifier = Modifier.fillMaxWidth(0.88f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                    Box(modifier = Modifier.fillMaxWidth(0.60f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                }
            }

            // 5. Episode Tabs & Preview Cards
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(80.dp, 28.dp).clip(OttPillShape).background(waveBrush))
                        Box(modifier = Modifier.size(80.dp, 28.dp).clip(OttPillShape).background(waveBrush))
                        Box(modifier = Modifier.size(80.dp, 28.dp).clip(OttPillShape).background(waveBrush))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(120.dp, 68.dp).clip(OttCardShape).background(waveBrush))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                            Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                        }
                    }
                }
            }
        }

        // Top Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

