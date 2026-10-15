package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttOnPrimary
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import android.net.Uri
import android.os.Build
import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
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
import com.nyyrox.aniverse.data.filler.AnimeFillerInfo
import com.nyyrox.aniverse.data.filler.AnimeFillerRepository
import com.nyyrox.aniverse.ui.components.DetailScreenSkeleton
import com.nyyrox.aniverse.ui.components.EpisodeFillerBadge
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
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.util.Locale

// The tabs item sits at this index in the LazyColumn (hero, meta, tabs).
private const val TABS_INDEX = 2
private val TabsHeight = 54.dp // pill height 38dp + 8dp padding top/bottom

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

    // Loading card: title/meta come from whatever the app already knows about this show.
    val knownAnime = remember(animeId) { vm.findKnownAnime(animeId) }
    val knownTitle = knownAnime?.displayTitle
    val knownMeta = remember(knownAnime) {
        knownAnime?.let {
            listOfNotNull(
                it.score?.let { s -> "★ $s%" },
                it.displayFormat,
                it.displayYear?.takeIf { y -> y.isNotBlank() }
            )
        }.orEmpty()
    }
    var loaderShown by remember(animeId) { mutableStateOf(false) }
    var curtainDone by remember(animeId) { mutableStateOf(false) }

    var tmdbEpisodes by remember(animeId) { mutableStateOf<Map<Int, TmdbEpisodeStill>>(emptyMap()) }
    var tmdbDetails by remember(animeId) { mutableStateOf<TmdbDetails?>(null) }
    var tmdbGrace by remember(animeId) { mutableStateOf(false) }
    LaunchedEffect(animeId) { delay(500L); tmdbGrace = true }
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
                    tint = OttTextPrimary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = OttPrimary, contentColor = OttOnPrimary),
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
    val isReady = isAniListReady && (isTmdbSettled || tmdbGrace)

    if (!isReady || anime == null) {
        DetailLoadingCard(
            cachedPoster = cachedPoster,
            title = knownTitle,
            meta = knownMeta,
            onBack = handleBack,
            coordinator = coordinator,
            onShown = { loaderShown = true }
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

    var fillerInfo by remember(anime.id) {
        mutableStateOf(AnimeFillerRepository.getCachedFillerInfo(anime.id) ?: AnimeFillerInfo(anime.id))
    }
    LaunchedEffect(anime.id, anime.title) {
        fillerInfo = AnimeFillerRepository.getFillerInfo(anime.id, anime.title, anime.romajiTitle)
    }

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
    val themeSettings = LocalThemeSettings.current
    val reduceMotion = themeSettings.isPowerSavingEnabled || themeSettings.isReducedMotionEnabled
    // ponytail: Ken Burns is skipped under battery saver / reduced motion (it redraws the full hero every frame).
    val heroScaleState = if (reduceMotion) null else infiniteTransition.animateFloat(
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

    // Glass follows Settings > Liquid Glassmorphism (and device capability). It is kept for small controls only;
    // big cards are flat translucent surfaces. null backdrop = flat AMOLED controls.
    // ponytail: per-card backdrop sampling was the main render cost; upgrade path = playerGlass(backdrop, r) on cards again.
    val glassOn = rememberGlassEnabled()
    val glassLayer = rememberPrismalGlassLayer()
    val backdrop = if (glassOn) glassLayer else null
    val shareAnime: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Watch ${anime.displayTitle} on ZENKAI: https://zenkai-auth-worker.ammhfoo.workers.dev/s/anime/${anime.id}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Anime"))
    }

    val density = LocalDensity.current
    val barHeightPx = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 48.dp).toPx() }
    val barStartPx = with(density) { 220.dp.toPx() }
    val barRangePx = with(density) { 140.dp.toPx() }
    val heroFadePx = with(density) { 380.dp.toPx() }
    val barBg = if (glassOn) Color(0xF2050507) else OttBg
    // Episode cards start tilting/fading away once they slide under the pinned tabs.
    val episodeTopLimitPx = barHeightPx + with(density) { TabsHeight.toPx() + 60.dp.toPx() }

    val lazyListState = rememberLazyListState()
    val scrollOffset by remember {
        derivedStateOf {
            val item0 = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            if (item0 != null) (-item0.offset).toFloat().coerceAtLeast(0f) else 100_000f
        }
    }
    // Continuous 0..1 progress so the top bar fades in instead of popping at a threshold.
    // Only read inside graphicsLayer/draw lambdas, so scrolling never recomposes this screen.
    val barProgress by remember(barStartPx, barRangePx) {
        derivedStateOf { ((scrollOffset - barStartPx) / barRangePx).coerceIn(0f, 1f) }
    }
    val barVisible by remember { derivedStateOf { barProgress > 0.02f } }
    // Tabs are an overlay that follows the in-list spacer and clamps under the top bar (no sticky-header jump).
    val tabsY by remember(barHeightPx) {
        derivedStateOf {
            val info = lazyListState.layoutInfo
            val tabsItem = info.visibleItemsInfo.firstOrNull { it.key == "tabs" }
            val raw = when {
                tabsItem != null -> tabsItem.offset.toFloat()
                lazyListState.firstVisibleItemIndex > TABS_INDEX -> 0f
                else -> info.viewportEndOffset.toFloat()
            }
            maxOf(raw, barHeightPx)
        }
    }
    val tabsPin by remember(barHeightPx) {
        derivedStateOf { ((barHeightPx + 36f - tabsY) / 36f).coerceIn(0f, 1f) }
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

    // ---- Tabs ----
    val tabs = if (anime.isMovie) {
        listOf("Characters", "Casts", "Videos", "How to Watch", "More Like This")
    } else {
        listOf("Episodes", "Characters", "Casts", "Videos", "How to Watch", "More Like This")
    }
    val activeTabIndex = selectedTab.coerceIn(0, tabs.lastIndex)
    val currentTabTitle = tabs[activeTabIndex]
    val tabLabels = tabs.map { title ->
        when (title) {
            "Episodes" -> "$title ($maxEpisode)"
            "Characters" -> if (anime.characters.isNotEmpty()) "$title (${anime.characters.size})" else title
            "How to Watch" -> if (anime.relations.isNotEmpty()) "$title (${anime.relations.size})" else title
            else -> title
        }
    }

    // Switching tabs while pinned: land just under the tabs instead of deep inside the previous list.
    LaunchedEffect(selectedTab) {
        if (tabsY <= barHeightPx + 1f && lazyListState.firstVisibleItemIndex >= TABS_INDEX) {
            lazyListState.scrollToItem(TABS_INDEX)
            lazyListState.scrollBy(-barHeightPx)
        }
    }

    // ---- Per-tab data (computed here because LazyColumn content is not composable) ----
    val pageEpisodes = remember(episodeSearchQuery, selectedPage, isOldestFirst, maxEpisode, tmdbEpisodes) {
        val query = episodeSearchQuery.trim()
        val raw = if (query.isNotEmpty()) {
            (1..maxEpisode).filter { epNum ->
                epNum.toString() == query || tmdbEpisodes[epNum]?.name.orEmpty().contains(query, ignoreCase = true)
            }
        } else {
            val pStart = selectedPage * pageSize + 1
            (pStart..minOf(pStart + pageSize - 1, maxEpisode)).toList()
        }
        if (isOldestFirst) raw else raw.reversed()
    }
    val rangeLabels = List(pageCount) { p ->
        val s = p * pageSize + 1
        "$s–${minOf(s + pageSize - 1, maxEpisode)}"
    }

    // Entrance animations that already played, so lazy items don't replay them on every scroll-in.
    val seen = remember(anime.id) { HashSet<String>() }

    val characters = anime.characters
    var charFilter by remember(anime.id) { mutableIntStateOf(0) }
    val charRoles = listOf("ALL") + characters.map { it.role.uppercase() }.distinct()
        .sortedBy { r -> listOf("MAIN", "SUPPORTING", "BACKGROUND").indexOf(r).let { if (it < 0) 99 else it } }
    val charIdx = charFilter.coerceIn(0, charRoles.lastIndex)
    val shownCharacters = if (charIdx == 0) characters else characters.filter { it.role.uppercase() == charRoles[charIdx] }
    val charLabels = charRoles.mapIndexed { i, r ->
        if (i == 0) "All (${characters.size})"
        else "${r.lowercase().replaceFirstChar { it.uppercase() }} (${characters.count { c -> c.role.uppercase() == r }})"
    }

    val staffList = anime.staff
    val tmdbCast = tmdbDetails?.cast.orEmpty()

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
            if (list.none { it.key == vid.key }) list.add(vid)
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

    val relationGroups = remember(anime.relations) {
        val rels = anime.relations
        val prequels = rels.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
        val sequels = rels.filter { it.relationType.equals("SEQUEL", ignoreCase = true) }
        val adaptations = rels.filter { it.relationType.equals("ADAPTATION", ignoreCase = true) || it.relationType.equals("SOURCE", ignoreCase = true) }
        val others = rels.filter { it !in prequels && it !in sequels && it !in adaptations }
        listOf(
            "PREQUEL · WATCH BEFORE" to prequels,
            "SEQUEL · WATCH NEXT" to sequels,
            "ORIGINAL SOURCE & ADAPTATION" to adaptations,
            "SIDE STORIES & ALTERNATIVES" to others
        ).filter { it.second.isNotEmpty() }
    }

    val recs: List<AnimeRecommendation> = when {
        currentTabTitle != "More Like This" -> emptyList()
        anime.recommendations.isNotEmpty() -> anime.recommendations
        else -> (vm.popular + vm.trending).filter { it.id != anime.id }.take(12).map {
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

    // Opening: if the loading card was on screen, the page rises + settles in while the card zooms away over it.
    val open = remember(anime.id) { Animatable(if (loaderShown) 0f else 1f) }
    LaunchedEffect(anime.id) {
        if (open.value < 1f) open.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = revealAlpha *
                    (if (coordinator?.phase == SharedTransitionPhase.BACKWARD) coordinator.progress.value else 1f)
            }
            .background(OttBg) // after graphicsLayer so the bg fades too (else it blacks out Home on back)
    ) {
        // Live ambient backdrop: only the small glass controls sample it. Draws nothing when glass is off.
        PlayerAmbientArt(layer = backdrop, model = heroImageUrl, modifier = Modifier.fillMaxSize())

        // === 1. HERO IMAGE (PARALLAX + OPTIONAL KEN BURNS) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .graphicsLayer {
                    translationY = -scrollOffset * 0.42f
                    alpha = (1f - scrollOffset / heroFadePx).coerceIn(0f, 1f)
                    clip = true
                    compositingStrategy = if (glassOn) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
                .then(
                    // Glass on: hero fades out into the ambient layer instead of a hard OttBg edge
                    if (glassOn) Modifier.drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.55f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    } else Modifier
                )
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
                            val s = (heroScaleState?.value ?: 1f) * (1.12f - 0.12f * open.value)
                            scaleX = s
                            scaleY = s
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
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero title overlay space at bottom of hero area
            item(key = "hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer {
                            // Title block shrinks + fades toward its corner as you scroll into the content.
                            val t = (scrollOffset / heroFadePx * 1.6f).coerceIn(0f, 1f)
                            alpha = contentAlpha * (1f - t)
                            translationY = t * 24.dp.toPx()
                            val sc = 1f - t * 0.10f
                            scaleX = sc
                            scaleY = sc
                            transformOrigin = TransformOrigin(0f, 1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val logoUrl = tmdbDetails?.logoUrl?.takeIf { it.isNotBlank() }
                                Crossfade(targetState = logoUrl, animationSpec = tween(400), label = "heroLogo") { logoUrl ->
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
                                        color = OttTextPrimary,
                                        maxLines = 2,
                                        lineHeight = 28.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                }
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

            // === METADATA HEADER & DETAILS ===
            item(key = "meta") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val isReleasing = anime.status.equals("RELEASING", ignoreCase = true) ||
                            anime.status.equals("AIRING", ignoreCase = true) ||
                            (anime.nextAt != null && (anime.nextAt * 1000L) > System.currentTimeMillis())
                    val scorePercent = anime.score ?: tmdbDetails?.voteAverage?.let { (it * 10).toInt() }

                    // Quick facts
                    FlowRow(
                        modifier = Modifier.staggerIn(0, seen, "meta_0"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val seasonYearText = anime.displaySeason.ifBlank { anime.displayYear }
                        if (seasonYearText.isNotBlank()) {
                            TagChip(seasonYearText.uppercase(), OttTextPrimary, OttInk(0.12f), OttInk(0.22f))
                        }
                        TagChip(
                            if (anime.isMovie) "MOVIE" else anime.displayFormat.uppercase(),
                            Color(0xFFE2E8F0), OttInk(0.08f), OttInk(0.18f)
                        )
                        if (!anime.isMovie) {
                            TagChip("$maxEpisode EPISODES", Color(0xFFE2E8F0), OttInk(0.08f), OttInk(0.18f))
                        }
                        if (scorePercent != null && scorePercent > 0) {
                            TagChip("★ $scorePercent%", Color(0xFFFDE68A), Color(0x26F59E0B), Color(0x66FBBF24))
                        }
                        if (isReleasing) {
                            TagChip("AIRING", Color(0xFFFCA5A5), Color(0x26E50914), Color(0x66E50914), dot = true)
                        }
                    }

                    // Primary actions: Play (wide) + Watchlist + Share
                    val playLabel = when {
                        anime.isMovie -> "Play"
                        historyItem != null -> "Continue Ep $selectedEpisode"
                        else -> "Play Ep $selectedEpisode"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggerIn(1, seen, "meta_1"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroPlayButton(
                            label = playLabel,
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (anime.id <= 0) showMissingAnilistDialog = true
                            else onPlay(anime.id, selectedEpisode, selectedProvider)
                        }
                        PlayerGlassButton(backdrop, onClick = { showWatchlistStatusDialog = true }, height = 50.dp, circle = true) {
                            PopOnChange(isBookmarked) {
                                val bmTint by animateColorAsState(
                                    targetValue = if (isBookmarked) OttAccentGold else OttTextPrimary,
                                    animationSpec = tween(220),
                                    label = "bookmarkTint"
                                )
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = bmTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        PlayerGlassButton(backdrop, onClick = shareAnime, height = 50.dp, circle = true) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = OttTextPrimary, modifier = Modifier.size(21.dp))
                        }
                    }

                    // Next airing
                    if (isReleasing && anime.nextAt != null) {
                        val nextEpNum = anime.nextEpisode ?: (maxEpisode + 1)
                        val countdown = anime.getAiringCountdown()
                        val formattedDate = anime.getFormattedAiringDate()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggerIn(2, seen, "meta_2")
                                .cardSurface(glassOn, 18.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .graphicsLayer { alpha = redBlinkAlpha }
                                    .clip(CircleShape)
                                    .background(Color(0xFFE50914))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = buildString {
                                        append("Episode ")
                                        append(nextEpNum)
                                        append(" airs")
                                        if (!countdown.isNullOrBlank()) {
                                            append(" in ")
                                            append(countdown)
                                        }
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OttTextPrimary
                                )
                                if (!formattedDate.isNullOrBlank()) {
                                    Text(text = formattedDate, fontSize = 11.5.sp, color = OttTextMuted)
                                }
                            }
                        }
                    }

                    // Synopsis
                    val displayOverview = tmdbDetails?.overview?.ifBlank { null } ?: anime.cleanDescription
                    if (displayOverview.isNotBlank()) {
                        val isLongOverview = displayOverview.length > 170
                        DetailCard(
                            glassOn, "SYNOPSIS",
                            modifier = Modifier
                                .staggerIn(3, seen, "meta_3")
                                .then(
                                    if (isLongOverview) Modifier.pressable(0.985f, haptic = false) { isSynopsisExpanded = !isSynopsisExpanded }
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = displayOverview,
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp,
                                color = OttInk(0.90f),
                                maxLines = if (isSynopsisExpanded || !isLongOverview) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLongOverview) {
                                Text(
                                    text = if (isSynopsisExpanded) "Show less" else "Read more",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OttTextPrimary,
                                    modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                )
                            }
                        }
                    }

                    // Details: genres + facts grid
                    val combinedGenres = (anime.genres + tmdbDetails?.genres.orEmpty()).distinct()
                    val runtime = tmdbDetails?.episodeRunTime ?: anime.duration ?: 24
                    val facts = listOf(
                        "Status" to anime.formattedStatus,
                        "Format" to (if (anime.isMovie) "Movie" else anime.displayFormat),
                        (if (anime.isMovie) "Runtime" else "Episodes") to (if (anime.isMovie) "$runtime min" else "$maxEpisode"),
                        "Per episode" to (if (anime.isMovie) "" else "$runtime min"),
                        "Studio" to (anime.studios.firstOrNull()?.ifBlank { null } ?: "Unknown"),
                        "Source" to anime.displaySource
                    ).filter { it.second.isNotBlank() }
                    DetailCard(glassOn, "DETAILS", modifier = Modifier.staggerIn(4, seen, "meta_4")) {
                        if (combinedGenres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                combinedGenres.take(8).forEach { g ->
                                    TagChip(g.uppercase(), Color(0xFFE2E8F0), OttInk(0.08f), OttInk(0.18f))
                                }
                            }
                        }
                        facts.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { (label, value) ->
                                    DetailFact(label, value, Modifier.weight(1f))
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Placeholder the pinned tab overlay sits on (see tabsY).
            item(key = "tabs") { Spacer(Modifier.height(TabsHeight)) }

            // === TAB CONTENTS ===
            when (currentTabTitle) {
                // --- EPISODES: rich 16:9 cards with swipe-up 3D scroll effect, lazily composed ---
                "Episodes" -> {
                    item(key = "ep_controls") {
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
                                BasicTextField(
                                    value = episodeSearchQuery,
                                    onValueChange = { episodeSearchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = OttTextPrimary, fontSize = 13.sp),
                                    cursorBrush = SolidColor(OttTextPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    decorationBox = { innerTextField ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .cardSurface(glassOn, 20.dp)
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
                                                    Text("Search episode...", fontSize = 13.sp, color = OttTextMuted)
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
                                                        contentDescription = "Clear search",
                                                        tint = OttTextPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )

                                IconButton(
                                    onClick = { isOldestFirst = !isOldestFirst },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .cardSurface(glassOn, 20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Sort Order",
                                        tint = if (!isOldestFirst) OttAccentGold else OttTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { isEpisodeGridView = !isEpisodeGridView },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .cardSurface(glassOn, 20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                                        contentDescription = "Toggle View",
                                        tint = OttTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Episode range selector (1-25, 26-50, ...) with the same sliding pill as the tabs
                            if (pageCount > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SlidingPillTabs(
                                        items = rangeLabels,
                                        selectedIndex = selectedPage,
                                        onSelect = { selectedPage = it },
                                        modifier = Modifier.weight(1f),
                                        edge = 0.dp,
                                        vPad = 2.dp,
                                        tabHeight = 32.dp,
                                        tabHPad = 12.dp,
                                        fontSize = 12.sp
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    val curStart = selectedPage * pageSize + 1
                                    val curEnd = minOf(curStart + pageSize - 1, maxEpisode)
                                    PlayerGlassButton(
                                        backdrop = backdrop,
                                        onClick = {
                                            com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                            showDownloadRangeSheet = curStart..curEnd
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download All",
                                            tint = OttTextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Download All",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OttTextPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (pageEpisodes.isEmpty()) {
                        item(key = "ep_empty") {
                            EmptyNote(
                                if (episodeSearchQuery.isNotBlank()) "No episodes match \"$episodeSearchQuery\"" else "No episodes available"
                            )
                        }
                    } else if (isEpisodeGridView) {
                        itemsIndexed(pageEpisodes.chunked(5), key = { i, _ -> "ep_grid_$i" }) { _, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { epNum ->
                                    val isCurrent = epNum == selectedEpisode
                                    val isEpFiller = fillerInfo.isFiller(epNum)
                                    val isEpMixed = fillerInfo.isMixed(epNum)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .pressable(0.88f) {
                                                selectedEpisode = epNum
                                                if (anime.id <= 0) showMissingAnilistDialog = true
                                                else onPlay(anime.id, epNum, selectedProvider)
                                            }
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCurrent) OttTextPrimary
                                                else if (isEpFiller) Color(0x24F59E0B)
                                                else if (isEpMixed) Color(0x246366F1)
                                                else OttInk(0.09f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrent) OttTextPrimary
                                                else if (isEpFiller) Color(0x60FBBF24)
                                                else if (isEpMixed) Color(0x60818CF8)
                                                else OttInk(0.15f),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$epNum",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.Black else if (isEpFiller) Color(0xFFFDE68A) else OttTextPrimary
                                        )
                                        if (isEpFiller || isEpMixed) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isEpMixed) Color(0xFFA5B4FC) else Color(0xFFFBBF24))
                                            )
                                        }
                                    }
                                }
                                repeat(5 - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(pageEpisodes, key = { _, ep -> "ep_$ep" }) { epIdx, epNum ->
                            val still = tmdbEpisodes[epNum]
                            val isDownloaded = vm.isAnimeEpisodeDownloaded(anime.id, epNum)
                            val isDownloading = activeAnimeTasks.containsKey("${anime.id}:$epNum")
                            val savedPos = vm.getSavedPosition(anime.id, epNum)
                            val durMs = (anime.duration ?: 24) * 60 * 1000L
                            val progressRatio = if (durMs > 0 && savedPos > 0) (savedPos.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f
                            RichEpisodeCard(
                                index = epIdx,
                                seen = seen,
                                topLimitPx = episodeTopLimitPx,
                                fallbackThumb = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                                epNum = epNum,
                                title = still?.name ?: "Episode $epNum",
                                overview = still?.overview?.takeIf { it.isNotBlank() }
                                    ?: "sit back and enjoy Episode $epNum of ${anime.displayTitle}.",
                                thumb = still?.stillUrl?.takeIf { it.isNotBlank() }
                                    ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                                    ?: tmdbDetails?.backdropUrl?.takeIf { it.isNotBlank() }
                                    ?: anime.banner?.takeIf { it.isNotBlank() }
                                    ?: anime.cover,
                                runMin = tmdbDetails?.episodeRunTime ?: anime.duration ?: 24,
                                progress = progressRatio,
                                isSelected = epNum == selectedEpisode,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                isFiller = fillerInfo.isFiller(epNum),
                                isMixed = fillerInfo.isMixed(epNum),
                                glassOn = glassOn,
                                onClick = {
                                    selectedEpisode = epNum
                                    if (anime.id <= 0) showMissingAnilistDialog = true
                                    else onPlay(anime.id, epNum, selectedProvider)
                                },
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Watch ${anime.displayTitle} Ep $epNum on ZENKAI: https://zenkai-auth-worker.ammhfoo.workers.dev/s/anime/${anime.id}?ep=$epNum"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Episode"))
                                },
                                onDownload = {
                                    if (!isDownloaded && !isDownloading) {
                                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                        showDownloadSheetEpisode = epNum to (still?.name ?: "Episode $epNum")
                                    }
                                }
                            )
                        }
                    }
                }

                // --- CHARACTERS: role filter + full-width rows (portrait, role, voice actor) ---
                "Characters" -> {
                    if (characters.isEmpty()) {
                        if (vm.detailLoading) {
                            items(3, key = { "ch_skel_$it" }) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .padding(horizontal = 18.dp)
                                        .fillMaxWidth()
                                        .height(108.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        } else {
                            item(key = "ch_empty") { EmptyNote("No character information available.") }
                        }
                    } else {
                        if (charRoles.size > 2) {
                            item(key = "ch_filters") {
                                SlidingPillTabs(
                                    items = charLabels,
                                    selectedIndex = charIdx,
                                    onSelect = { charFilter = it },
                                    edge = 18.dp,
                                    vPad = 2.dp,
                                    tabHeight = 32.dp,
                                    tabHPad = 12.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        itemsIndexed(shownCharacters, key = { i, c -> "ch_${c.id}_$i" }) { i, c ->
                            Box(Modifier.staggerIn(i, seen, "ch_$i")) { CharacterRow(char = c, glassOn = glassOn) }
                        }
                    }
                }

                // --- CASTS: 3-column grids so the whole crew is visible at a glance ---
                "Casts" -> {
                    if (staffList.isEmpty() && tmdbCast.isEmpty()) {
                        item(key = "cast_empty") { EmptyNote("No cast or staff information available.") }
                    }
                    if (staffList.isNotEmpty()) {
                        item(key = "staff_h") { SectionHeader("PRODUCTION STAFF", staffList.size) }
                        itemsIndexed(
                            staffList.map { Triple(it.name, it.image, it.role) }.chunked(3),
                            key = { i, _ -> "staff_$i" }
                        ) { i, row ->
                            Box(Modifier.staggerIn(i, seen, "pp_${row.firstOrNull()?.first}_$i")) { PeopleRow(row, glassOn) }
                        }
                    }
                    if (tmdbCast.isNotEmpty()) {
                        item(key = "cast_h") { SectionHeader("VOICE CAST (TMDB)", tmdbCast.size) }
                        itemsIndexed(
                            tmdbCast.map { Triple(it.name, it.profileUrl, it.character) }.chunked(3),
                            key = { i, _ -> "cast_$i" }
                        ) { i, row ->
                            Box(Modifier.staggerIn(i, seen, "pp_${row.firstOrNull()?.first}_$i")) { PeopleRow(row, glassOn) }
                        }
                    }
                }

                // --- VIDEOS: one featured card, the rest as compact rows ---
                "Videos" -> {
                    if (allVideos.isEmpty()) {
                        item(key = "vid_empty") { EmptyNote("No videos or trailers available.") }
                    } else {
                        itemsIndexed(allVideos, key = { i, v -> "vid_${v.key}_$i" }) { i, video ->
                            Box(Modifier.staggerIn(i, seen, "vid_$i")) {
                                VideoItem(
                                    video = video,
                                    featured = i == 0,
                                    glassOn = glassOn,
                                    onClick = { openYoutube(context, video.key) }
                                )
                            }
                        }
                    }
                }

                // --- HOW TO WATCH: grouped vertical rows (prequel / sequel / source / side stories) ---
                "How to Watch" -> {
                    if (relationGroups.isEmpty()) {
                        item(key = "rel_empty") { EmptyNote("No watch order or relation data available.") }
                    } else {
                        relationGroups.forEach { (groupTitle, list) ->
                            item(key = "rel_h_$groupTitle") { SectionHeader(groupTitle, list.size) }
                            itemsIndexed(list, key = { i, r -> "rel_${groupTitle}_${r.id}_$i" }) { ri, rel ->
                                Box(Modifier.staggerIn(ri, seen, "rel_${rel.id}_$ri")) {
                                    RelationRow(rel = rel, glassOn = glassOn, vm = vm)
                                }
                            }
                        }
                    }
                }

                // --- MORE LIKE THIS: 3-column poster grid ---
                "More Like This" -> {
                    if (recs.isEmpty()) {
                        item(key = "rec_empty") { EmptyNote("No recommendations found.") }
                    } else {
                        itemsIndexed(recs.chunked(3), key = { i, _ -> "rec_row_$i" }) { rowIdx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEachIndexed { colIdx, rec ->
                                    Box(modifier = Modifier.weight(1f).staggerIn(rowIdx + colIdx, seen, "rec_${rec.id}")) {
                                        RecommendationPosterItem(rec = rec, onClick = { vm.openDetail(rec.id) })
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                else -> Unit
            }
        }

        // === 3. PINNED TABS (overlay; follows the list, then clamps under the top bar) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TabsHeight)
                .graphicsLayer { translationY = tabsY }
                .drawBehind { drawRect(barBg.copy(alpha = tabsPin)) }
        ) {
            SlidingPillTabs(
                items = tabLabels,
                selectedIndex = activeTabIndex,
                onSelect = { selectedTab = it }
            )
        }

        // === 4. TOP BAR: scrim over the hero that cross-fades to a solid bar (continuous, no pop) ===
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 1f - barProgress }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = barProgress }
                    .background(barBg)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerGlassButton(backdrop, onClick = handleBack, height = 40.dp, circle = true) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OttTextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))

                if (barVisible) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { alpha = barProgress },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(OttTextPrimary)
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
                                color = OttTextPrimary,
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
                    Row(
                        modifier = Modifier.graphicsLayer { alpha = barProgress },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerGlassButton(backdrop, onClick = shareAnime, height = 40.dp, circle = true) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = OttTextPrimary, modifier = Modifier.size(19.dp))
                        }
                        PlayerGlassButton(backdrop, onClick = { showWatchlistStatusDialog = true }, height = 40.dp, circle = true) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (isBookmarked) OttAccentGold else OttTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Opening curtain: the loading card zooms through the screen and dissolves into the page.
        if (loaderShown && !curtainDone) {
            val exit = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(60L)
                exit.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                curtainDone = true
            }
            DetailLoadingCard(
                cachedPoster = cachedPoster,
                title = knownTitle,
                meta = knownMeta,
                onBack = handleBack,
                coordinator = coordinator,
                exit = exit.value,
                showBack = false,
                reportBounds = false
            )
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
                    Text("Download Stopped", fontWeight = FontWeight.Bold, color = OttTextPrimary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = OttPrimary, contentColor = OttOnPrimary),
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
            containerColor = OttPanelHigh,
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
                    tint = OttTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Source Link Unavailable",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
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
                        containerColor = OttPrimary,
                        contentColor = OttOnPrimary
                    ),
                    shape = OttPillShape
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = OttPanelHigh,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = OttTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = OttSecondary
        )
        if (count != null) {
            Text(text = "$count", fontSize = 11.sp, color = OttTextMuted)
        }
    }
}

@Composable
private fun MiniChip(text: String, color: Color = Color(0xFFCBD5E1), bg: Color = OttInk(0.08f), border: Color = OttInk(0.13f)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.6.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

/**
 * Rich 16:9 episode card. Slides up + tilts in on first appearance, grows toward you as it scrolls up,
 * then tilts/fades away under the pinned tabs (the "swipe-up deck" feel). Has Share + Download on the thumbnail.
 */
@Composable
private fun RichEpisodeCard(
    index: Int,
    seen: MutableSet<String>,
    topLimitPx: Float,
    fallbackThumb: String?,
    epNum: Int,
    title: String,
    overview: String,
    thumb: String?,
    runMin: Int,
    progress: Float,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isFiller: Boolean,
    isMixed: Boolean,
    glassOn: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    val seenKey = "ep_$epNum"

    // Entrance: one progress value drives alpha, scale, tilt and rise (plays once per card).
    var entered by remember { mutableStateOf(seenKey in seen) }
    LaunchedEffect(Unit) {
        if (!entered) {
            entered = true
            seen.add(seenKey)
        }
    }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = (index * 35).coerceAtMost(280), easing = FastOutSlowInEasing),
        label = "epEnter"
    )

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "epPress"
    )
    val playScale by animateFloatAsState(
        targetValue = if (pressed) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "epPlayPress"
    )

    // Selected episode breathes. Only the selected card runs the infinite transition.
    val glow: State<Float> = if (isSelected) {
        rememberInfiniteTransition(label = "epGlow").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "epGlowV"
        )
    } else remember { mutableStateOf(0f) }

    // Window position, read only inside graphicsLayer lambdas so scrolling never recomposes the card.
    var topPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var hPx by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .onGloballyPositioned { c ->
                topPx = c.boundsInWindow().top
                hPx = c.size.height.toFloat()
            }
            .graphicsLayer {
                var sc = (0.92f + 0.08f * enter) * pressScale
                var rx = (1f - enter) * 14f
                var a = enter
                var ty = (1f - enter) * 36.dp.toPx()
                val h = hPx.coerceAtLeast(1f)
                if (topPx < topLimitPx) {
                    // Sliding away under the tabs
                    val p = ((topLimitPx - topPx) / (h * 0.7f)).coerceIn(0f, 1f)
                    sc *= 1f - p * 0.12f
                    rx += p * 15f
                    a *= (1f - p * 0.85f)
                    ty -= p * 22.dp.toPx()
                } else {
                    val zoomZone = topLimitPx + h * 0.9f
                    if (topPx < zoomZone) {
                        // Swiping up into focus
                        val f = ((zoomZone - topPx) / (h * 0.9f)).coerceIn(0f, 1f)
                        sc *= 0.95f + f * 0.05f
                        rx -= (1f - f) * 4f
                    } else {
                        sc *= 0.95f
                    }
                }
                scaleX = sc
                scaleY = sc
                rotationX = rx
                alpha = a.coerceIn(0f, 1f)
                translationY = ty
                cameraDistance = 12f * density
            }
            .clip(shape)
            .then(
                if (glassOn) Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isSelected) OttInk(0.20f) else OttInk(0.14f),
                                OttInk(0.05f)
                            )
                        ),
                        shape
                    )
                    .border(1.dp, Brush.verticalGradient(listOf(OttInk(0.27f), OttInk(0.06f))), shape)
                else Modifier
                    .background(if (isSelected) OttPanelHigh else OttPanelHigh)
                    .border(1.dp, OttInk(0.09f), shape)
            )
            .then(
                if (isSelected) Modifier.drawWithContent {
                    drawContent()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f + 0.55f * glow.value),
                        style = Stroke(width = 1.5.dp.toPx()),
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                } else Modifier
            )
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(OttPanelHigh)
        ) {
            // Thumbnail drifts slightly against the scroll for depth.
            SubcomposeAsyncImage(
                model = thumb,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.14f
                        scaleY = 1.14f
                        val p = ((topPx - topLimitPx) / 1400f).coerceIn(0f, 1f)
                        translationY = (p - 0.5f) * size.height * 0.12f
                    },
                contentScale = ContentScale.Crop,
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                error = {
                    AsyncImage(
                        model = fallbackThumb,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000))))
            )

            // Play badge (pulses on the selected episode, pops on press)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .graphicsLayer {
                        val s = playScale * (1f + 0.08f * glow.value)
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(if (glassOn) OttInk(0.25f) else Color(0x60000000))
                    .border(1.dp, OttInk(0.31f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(25.dp))
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(OttTextPrimary)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (progress > 0.05f) "CONTINUE" else "SELECTED",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = Color.Black
                    )
                }
            }

            // Episode badge (+ filler/mixed) bottom-left
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .border(0.6.dp, OttInk(0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = "Episode $epNum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OttTextPrimary)
                }
                if (isFiller || isMixed) EpisodeFillerBadge(isMixed = isMixed)
            }

            // Duration bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(text = "${runMin}m", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OttInk(0.87f))
            }

            // Share + Download, top-right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .pressable(0.82f, onClick = onShare)
                        .clip(CircleShape)
                        .background(Color(0x99101420))
                        .border(1.dp, OttInk(0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share episode $epNum", tint = OttTextPrimary, modifier = Modifier.size(15.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .pressable(0.82f, haptic = false, onClick = onDownload) // onDownload already ticks
                        .clip(CircleShape)
                        .background(Color(0x99101420))
                        .border(1.dp, OttInk(0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDownloaded -> Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        isDownloading -> Icon(Icons.Default.Downloading, contentDescription = "Downloading", tint = OttAccentGold, modifier = Modifier.size(16.dp))
                        else -> Icon(Icons.Default.Download, contentDescription = "Download episode $epNum", tint = OttTextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (progress > 0.05f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = OttTextPrimary,
                    trackColor = OttInk(0.25f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OttTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = overview,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = OttTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Full-width character row: portrait, role, native name and voice actor in one glance. */
@Composable
private fun CharacterRow(char: AnimeCharacter, glassOn: Boolean) {
    val isMain = char.role.equals("MAIN", ignoreCase = true)
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .cardSurface(glassOn, 16.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = char.image,
            contentDescription = char.name,
            modifier = Modifier
                .width(66.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OttInk(0.13f)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = char.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!char.nativeName.isNullOrBlank()) {
                Text(
                    text = char.nativeName,
                    fontSize = 11.sp,
                    color = OttTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniChip(
                text = char.role.lowercase().replaceFirstChar { it.uppercase() },
                color = if (isMain) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                bg = if (isMain) Color(0x26E50914) else OttInk(0.08f),
                border = if (isMain) Color(0x66E50914) else OttInk(0.13f)
            )
            if (!char.voiceActorName.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = char.voiceActorImage,
                        contentDescription = char.voiceActorName,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(OttInk(0.13f))
                            .border(0.6.dp, OttInk(0.19f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = char.voiceActorName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OttInk(0.93f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Japanese VA", fontSize = 9.5.sp, color = OttTextMuted)
                    }
                }
            }
        }
    }
}

/** One row of up to 3 people (name, photo, subtitle). */
@Composable
private fun PeopleRow(people: List<Triple<String, String?, String?>>, glassOn: Boolean) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        people.forEach { (name, image, sub) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .cardSurface(glassOn, 14.dp)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.13f))
                        .border(1.dp, OttInk(0.19f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!sub.isNullOrBlank()) {
                    Text(
                        text = sub,
                        fontSize = 10.sp,
                        color = OttTextMuted,
                        maxLines = 2,
                        lineHeight = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        repeat(3 - people.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun VideoItem(video: TmdbVideo, featured: Boolean, glassOn: Boolean, onClick: () -> Unit) {
    val thumb = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg"
    if (featured) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .cardSurface(glassOn, 16.dp)
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OttPanelHigh)
            ) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x30000000), Color(0x75000000))))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xDD000000))
                        .border(1.5.dp, OttTextPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = OttTextPrimary, modifier = Modifier.size(28.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    MiniChip(video.type.uppercase(Locale.US), OttTextPrimary, Color(0xD0000000), OttInk(0.25f))
                }
            }
            Text(
                text = video.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .cardSurface(glassOn, 14.dp)
                .clickable(onClick = onClick)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OttPanelHigh)
            ) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play video", tint = OttTextPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = video.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                MiniChip(video.type.uppercase(Locale.US))
            }
        }
    }
}

private fun openYoutube(context: Context, key: String) {
    val ytAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$key")).apply {
        putExtra("force_fullscreen", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(ytAppIntent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}

@Composable
private fun RelationRow(rel: MediaRelation, glassOn: Boolean, vm: MainViewModel) {
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
    val relationLabel = rel.relationType.orEmpty().replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .pressable(0.97f) {
                if (isNovel) vm.openNovelDetail(rel.title)
                else if (isManga) vm.openMangaDetail(rel.id.toString(), rel.title)
                else vm.openDetail(rel.id)
            }
            .cardSurface(glassOn, 14.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = rel.cover,
            contentDescription = null,
            modifier = Modifier
                .width(54.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(8.dp))
                .background(OttInk(0.13f)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = rel.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniChip(displayBadge.uppercase(Locale.US))
                if (relationLabel.isNotBlank()) MiniChip(relationLabel)
            }
        }
        Text(
            text = if (isNovel || isManga) "Read ➔" else "Watch ➔",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OttSecondary
        )
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
            .pressable(0.94f, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(OttInk(0.09f))
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
                            color = OttTextPrimary
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
            color = OttTextPrimary,
            maxLines = 2,
            lineHeight = 15.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Loading screen: the poster rests over its own blurred artwork (slow light sweep, breathing glow) while
 * skeleton placeholders hint at the page that's about to appear. No spinner, no status text.
 * [exit] (0→1) plays the opening: the card zooms through the screen and the layer dissolves into the real page.
 * The overlay copy used for that ([showBack] / [reportBounds] false) never touches the back button or shared-poster bounds.
 */
@Composable
private fun DetailLoadingCard(
    cachedPoster: String?,
    title: String?,
    meta: List<String>,
    onBack: () -> Unit,
    coordinator: SharedPosterCoordinator?,
    exit: Float = 0f,
    showBack: Boolean = true,
    reportBounds: Boolean = true,
    onShown: () -> Unit = {}
) {
    LaunchedEffect(Unit) { onShown() }

    val fx = rememberInfiniteTransition(label = "detailLoaderFx")
    val sweep by fx.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "sweep")
    val glow by fx.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val drift by fx.animateFloat(-1f, 1f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift")

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    // The overlay poster lands on this card's full-size bounds, so don't start it shrunken.
    val flown = remember { reportBounds && coordinator?.phase == SharedTransitionPhase.FORWARD }
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (coordinator?.phase == SharedTransitionPhase.BACKWARD) coordinator.progress.value
                else (1f - exit).coerceIn(0f, 1f)
            }
            .background(OttBg) // after graphicsLayer so the bg dissolves with the curtain
    ) {
        if (!cachedPoster.isNullOrBlank()) {
            AsyncImage(
                model = cachedPoster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp)
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                        translationX = drift * 40f
                        alpha = 0.5f
                    }
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xCC000000), OttBg)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Poster card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(2f / 3f)
                    .then(
                        if (reportBounds) Modifier.onGloballyPositioned { coords ->
                            coordinator?.updateTargetBounds(coords.boundsInWindow())
                        } else Modifier
                    )
                    .graphicsLayer {
                        val s = (if (flown) 1f else 0.9f + 0.1f * enter.value) * (1f + 0.9f * exit)
                        scaleX = s
                        scaleY = s
                        alpha = enter.value
                    }
                    .drawBehind {
                        val pad = 18.dp.toPx()
                        drawRoundRect(
                            color = Color.White.copy(alpha = glow * 0.14f),
                            topLeft = Offset(-pad, -pad),
                            size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx() + pad)
                        )
                    }
                    .clip(cardShape)
                    .background(OttPanelHigh)
            ) {
                if (!cachedPoster.isNullOrBlank()) {
                    AsyncImage(
                        model = cachedPoster,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (coordinator?.phase == SharedTransitionPhase.FORWARD && reportBounds) 0f else 1f
                            }
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xAA000000)))
                )
                // Light sweep
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val w = size.width
                            val x = -w + sweep * w * 3f
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.Transparent, OttInk(0.19f), Color.Transparent),
                                    start = Offset(x, 0f),
                                    end = Offset(x + w * 0.7f, size.height * 0.6f)
                                )
                            )
                        }
                )
                // Rim light
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(OttInk(0.50f), OttInk(0.03f), OttInk(0.25f))),
                            cardShape
                        )
                )
            }

            // ── Title, meta and page skeleton: leaves first on exit, rises in on enter ──
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = (enter.value * (1f - exit * 3.5f)).coerceIn(0f, 1f)
                    translationY = (1f - enter.value) * 12.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = OttTextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    SkeletonBar(Modifier.fillMaxWidth(0.5f).height(22.dp), sweep)
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = meta.joinToString("  •  "),
                        fontSize = 12.5.sp,
                        color = OttTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBar(Modifier.width(64.dp).height(22.dp), sweep)
                    SkeletonBar(Modifier.width(48.dp).height(22.dp), sweep)
                    SkeletonBar(Modifier.width(72.dp).height(22.dp), sweep)
                }
                Spacer(Modifier.height(14.dp))
                SkeletonBar(Modifier.fillMaxWidth().height(44.dp), sweep)
                Spacer(Modifier.height(14.dp))
                SkeletonBar(Modifier.fillMaxWidth(0.9f).height(10.dp), sweep)
                Spacer(Modifier.height(8.dp))
                SkeletonBar(Modifier.fillMaxWidth(0.65f).height(10.dp), sweep)
            }
        }

        if (showBack) {
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
                        tint = OttTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Pill placeholder with a soft light sweep, driven by the loader's shared [sweep] (0→1). */
@Composable
private fun SkeletonBar(modifier: Modifier, sweep: Float) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(OttInk(0.08f))
            .drawBehind {
                val w = size.width
                val x = -w + sweep * w * 2.5f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, OttInk(0.13f), Color.Transparent),
                        Offset(x, 0f),
                        Offset(x + w * 0.6f, 0f)
                    )
                )
            }
    )
}


// ponytail: reads the Profile toggle directly; AGSL glass needs API 33+. Move to a shared file if other screens need it.
@Composable
private fun rememberGlassEnabled(): Boolean =
    LocalThemeSettings.current.isGlassmorphismEnabled && Build.VERSION.SDK_INT >= 33

/** Flat translucent card surface: no backdrop sampling, so it stays cheap while scrolling. */
private fun Modifier.cardSurface(glassOn: Boolean, radius: Dp = 18.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(if (glassOn) OttInk(0.08f) else OttPanelHigh)
        .border(1.dp, OttInk(0.10f), shape)
}

/** Section card (flat). Grows/shrinks smoothly with its content. */
@Composable
private fun DetailCard(
    glassOn: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .cardSurface(glassOn, 20.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFF8E94A3)
        )
        content()
    }
}

@Composable
private fun DetailFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = Color(0xFF8E94A3)
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OttTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Horizontally scrollable pill tabs with one white pill that slides (spring) to the selected tab,
 * and the row auto-centers the selection. Used for the main tabs, episode ranges and character roles.
 */
@Composable
private fun SlidingPillTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edge: Dp = 18.dp,
    vPad: Dp = 8.dp,
    tabHeight: Dp = 38.dp,
    tabHPad: Dp = 16.dp,
    fontSize: TextUnit = 13.sp
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val lefts = remember(items.size) { IntArray(items.size) }
    val widths = remember(items.size) { IntArray(items.size) }
    var tick by remember(items.size) { mutableIntStateOf(0) }
    var viewportW by remember { mutableIntStateOf(0) }
    val sel = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    LaunchedEffect(sel, tick, viewportW) {
        if (tick > 0 && viewportW > 0) {
            val center = with(density) { edge.roundToPx() } + lefts[sel] + widths[sel] / 2
            scroll.animateScrollTo((center - viewportW / 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { viewportW = it.width }
            .horizontalScroll(scroll)
    ) {
        Box(modifier = Modifier.padding(horizontal = edge, vertical = vPad)) {
            // Indicator is only composed after the first measure so it never slides in from x = 0.
            if (tick > 0) {
                val indX by animateIntAsState(
                    targetValue = lefts[sel],
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    label = "pillX"
                )
                val indW by animateIntAsState(
                    targetValue = widths[sel],
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    label = "pillW"
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indX, 0) }
                        .width(with(density) { indW.toDp() })
                        .height(tabHeight)
                        .clip(CircleShape)
                        .background(OttTextPrimary)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { i, label ->
                    val isSel = i == sel
                    val textColor by animateColorAsState(
                        targetValue = if (isSel) Color.Black else Color(0xFFCBD5E1),
                        animationSpec = tween(180),
                        label = "tabText"
                    )
                    val pillSource = remember { MutableInteractionSource() }
                    val pillPressed by pillSource.collectIsPressedAsState()
                    val pillScale by animateFloatAsState(
                        targetValue = if (pillPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "pillPress"
                    )
                    Box(
                        modifier = Modifier
                            .height(tabHeight)
                            .graphicsLayer { scaleX = pillScale; scaleY = pillScale }
                            .onGloballyPositioned { c ->
                                val x = c.positionInParent().x.roundToInt()
                                val w = c.size.width
                                if (lefts[i] != x || widths[i] != w) {
                                    lefts[i] = x
                                    widths[i] = w
                                    tick++
                                }
                            }
                            .clip(CircleShape)
                            .background(if (isSel) Color.Transparent else OttInk(0.08f))
                            .border(1.dp, if (isSel) Color.Transparent else OttInk(0.18f), CircleShape)
                            .semantics { selected = isSel }
                            .clickable(interactionSource = pillSource, indication = null, role = Role.Tab) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = fontSize,
                            fontWeight = FontWeight.SemiBold, // constant weight so selecting never reflows the row
                            color = textColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = tabHPad)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Interaction helpers
// ---------------------------------------------------------------------------------------------

/** Bouncy press-in feedback + haptic tick. Use instead of clickable on cards/buttons; put it BEFORE clip/background so the whole surface scales. */
private fun Modifier.pressable(
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .clickable(interactionSource = source, indication = null) {
            if (haptic) com.nyyrox.aniverse.util.AppHaptics.tick(context)
            onClick()
        }
}

/** Rise + fade + settle entrance with a small per-index delay. Plays once per [key] (tracked in [seen]). */
private fun Modifier.staggerIn(index: Int, seen: MutableSet<String>, key: String): Modifier = composed {
    val p = remember(key) { Animatable(if (key in seen) 1f else 0f) }
    LaunchedEffect(key) {
        if (p.value < 1f) {
            delay((index * 50L).coerceAtMost(350L))
            p.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 260f))
            seen.add(key)
        }
    }
    graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 32.dp.toPx()
        val sc = 0.94f + 0.06f * v
        scaleX = sc
        scaleY = sc
    }
}

/** Primary play button: liquid glass pill (same as the player page buttons) when glass is on, white pill when off. */
@Composable
private fun HeroPlayButton(
    label: String,
    backdrop: PrismalGlassLayer?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (backdrop != null) GlassPlayButton(label, backdrop, modifier, onClick)
    else FlatPlayButton(label, modifier, onClick)
}

/** Glass play pill with a soft breathing halo; label slides when the episode changes. */
@Composable
private fun GlassPlayButton(
    label: String,
    backdrop: PrismalGlassLayer,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val inf = rememberInfiniteTransition(label = "glassPlayFx")
    val halo by inf.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glassPlayHalo"
    )
    PlayerGlassButton(
        backdrop = backdrop,
        onClick = {
            com.nyyrox.aniverse.util.AppHaptics.tick(context)
            onClick()
        },
        modifier = modifier.drawBehind {
            val pad = 4.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = halo * 0.45f),
                topLeft = Offset(-pad, -pad),
                size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                cornerRadius = CornerRadius(size.height / 2f + pad)
            )
        },
        height = 50.dp
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(24.dp))
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(120)))
            },
            label = "glassPlayLabel"
        ) { text ->
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OttTextPrimary, maxLines = 1, softWrap = false)
        }
    }
}

/** Flat white play pill (glass off): idle glow + light sweep, bouncy press. */
@Composable
private fun FlatPlayButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "playFx")
    val sweep by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "playSweep"
    )
    val glow by inf.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "playGlow"
    )
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playIcon"
    )
    Row(
        modifier = modifier
            .height(50.dp)
            .pressable(0.95f, onClick = onClick)
            .drawBehind {
                val pad = 5.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = glow * 0.5f),
                    topLeft = Offset(-pad, -pad),
                    size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(size.height / 2f + pad)
                )
            }
            .clip(CircleShape)
            .background(OttTextPrimary)
            .drawWithContent {
                drawContent()
                // Soft reflection band that glides across, then rests.
                if (sweep < 0.6f) {
                    val w = size.width
                    val x = -w * 0.4f + (sweep / 0.6f) * w * 1.8f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0x26000000), Color.Transparent),
                            startX = x,
                            endX = x + w * 0.4f
                        )
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(120)))
            },
            label = "playLabel"
        ) { text ->
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
        }
    }
}

/** Pops its content and fires an expanding ring whenever [trigger] flips (skips the first composition). */
@Composable
private fun PopOnChange(trigger: Boolean, content: @Composable () -> Unit) {
    val pop = remember { Animatable(1f) }
    val ring = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        launch {
            pop.snapTo(0.55f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
        ring.snapTo(0f)
        ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        ring.snapTo(0f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .drawBehind {
                val r = ring.value
                if (r > 0f) {
                    drawCircle(
                        color = Color(0xFFFFD166).copy(alpha = (1f - r) * 0.85f),
                        radius = size.minDimension / 2f * (1f + r * 1.1f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
    ) { content() }
}
