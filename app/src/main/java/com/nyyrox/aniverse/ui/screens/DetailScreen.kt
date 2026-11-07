package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import coil3.request.ImageRequest

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
    // Same palette Home uses: follows light / dark and the Liquid Glass toggle.
    val p = rememberHomePalette()

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
                .background(p.pageBrush)
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
                    tint = p.text,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Unable to load details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = vm.detailError ?: "Error occurred",
                    fontSize = 13.sp,
                    color = p.textSub,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.openDetail(animeId) },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accentA, contentColor = p.onAccent),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = p.onAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry", color = p.onAccent, fontWeight = FontWeight.Bold)
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
    // Pinned bar / tabs use the top colour of the page gradient so they melt into the page in every theme.
    val barBg = p.gradTop
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

    // Everything on screen lives in this lambda so it can sit inside the glass backdrop host (glass on)
    // or directly on the flat page backdrop (glass off), exactly like Home.
    val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {

            // === 1. HERO IMAGE (parallax + optional Ken Burns). Melts into the page gradient, no hard edge. ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .graphicsLayer {
                        translationY = -scrollOffset * 0.42f
                        alpha = (1f - scrollOffset / heroFadePx).coerceIn(0f, 1f)
                        clip = true
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.30f to Color.Black,
                                0.74f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
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
                                val s = (heroScaleState?.value ?: 1f) * (1.12f - 0.12f * open.value)
                                scaleX = s
                                scaleY = s
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                // Soft scrim behind the status bar / top buttons so they read on any artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color(0x80000000), Color.Transparent)))
                )
            }

            // === 2. SCROLLABLE FOREGROUND CONTENT ===
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title block at the bottom of the hero area (theme text colour: it sits on the faded part)
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
                                val t = (scrollOffset / heroFadePx * 1.6f).coerceIn(0f, 1f)
                                alpha = contentAlpha * (1f - t)
                                translationY = t * 24.dp.toPx()
                                val sc = 1f - t * 0.10f
                                scaleX = sc
                                scaleY = sc
                                transformOrigin = TransformOrigin(0f, 1f)
                            }
                        ) {
                            val logoUrl = tmdbDetails?.logoUrl?.takeIf { it.isNotBlank() }
                            Crossfade(targetState = logoUrl, animationSpec = tween(400), label = "heroLogo") { logo ->
                                if (logo != null) {
                                    AsyncImage(
                                        model = logo,
                                        contentDescription = anime.displayTitle,
                                        modifier = Modifier
                                            .heightIn(max = 58.dp)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.CenterStart,
                                        // Most TMDB logos are white: on the light page they become a solid dark silhouette
                                        colorFilter = if (p.dark) null else ColorFilter.tint(p.text)
                                    )
                                } else {
                                    Text(
                                        text = tmdbDetails?.title?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: anime.displayTitle,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        color = p.text,
                                        maxLines = 2,
                                        lineHeight = 31.sp,
                                        overflow = TextOverflow.Ellipsis
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
                                    color = p.textMuted,
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
                                    color = p.textSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // === META: info line, genres, actions, synopsis, facts. Plain sections, no cards. ===
                item(key = "meta") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        val isReleasing = anime.status.equals("RELEASING", ignoreCase = true) ||
                            anime.status.equals("AIRING", ignoreCase = true) ||
                            (anime.nextAt != null && (anime.nextAt * 1000L) > System.currentTimeMillis())
                        val scorePercent = anime.score ?: tmdbDetails?.voteAverage?.let { (it * 10).toInt() }
                        val combinedGenres = (anime.genres + tmdbDetails?.genres.orEmpty()).distinct()
                        val seasonYearText = anime.displaySeason.ifBlank { anime.displayYear }

                        val infoParts = buildList<Pair<String, Color>> {
                            if (seasonYearText.isNotBlank()) add(seasonYearText to p.textSub)
                            add((if (anime.isMovie) "Movie" else anime.displayFormat) to p.textSub)
                            if (!anime.isMovie) add("$maxEpisode episodes" to p.textSub)
                            if (scorePercent != null && scorePercent > 0) add("★ $scorePercent%" to p.rating)
                            if (isReleasing) add("Airing" to p.rating)
                        }

                        // Info line + genres (plain text, wraps naturally)
                        Column(
                            modifier = Modifier.staggerIn(0, seen, "meta_0"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    infoParts.forEachIndexed { i, part ->
                                        if (i > 0) withStyle(SpanStyle(color = p.textMuted)) { append("   •   ") }
                                        withStyle(SpanStyle(color = part.second, fontWeight = FontWeight.SemiBold)) { append(part.first) }
                                    }
                                },
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            if (combinedGenres.isNotEmpty()) {
                                Text(
                                    text = combinedGenres.take(6).joinToString("  ·  "),
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = p.textMuted
                                )
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
                                p = p,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (anime.id <= 0) showMissingAnilistDialog = true
                                else onPlay(anime.id, selectedEpisode, selectedProvider)
                            }
                            GlassCircleButton(p, 50.dp, onClick = { showWatchlistStatusDialog = true }) {
                                PopOnChange(isBookmarked) {
                                    val bmTint by animateColorAsState(
                                        targetValue = if (isBookmarked) p.warm else p.text,
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
                            GlassCircleButton(p, 50.dp, onClick = shareAnime) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = p.text, modifier = Modifier.size(21.dp))
                            }
                        }

                        // Next airing (a line, not a card)
                        if (isReleasing && anime.nextAt != null) {
                            val nextEpNum = anime.nextEpisode ?: (maxEpisode + 1)
                            val countdown = anime.getAiringCountdown()
                            val formattedDate = anime.getFormattedAiringDate()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggerIn(2, seen, "meta_2"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer { alpha = redBlinkAlpha }
                                        .clip(CircleShape)
                                        .background(p.rating)
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
                                        color = p.text
                                    )
                                    if (!formattedDate.isNullOrBlank()) {
                                        Text(text = formattedDate, fontSize = 11.5.sp, color = p.textMuted)
                                    }
                                }
                            }
                        }

                        // Synopsis
                        val displayOverview = tmdbDetails?.overview?.ifBlank { null } ?: anime.cleanDescription
                        if (displayOverview.isNotBlank()) {
                            val isLongOverview = displayOverview.length > 170
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggerIn(3, seen, "meta_3")
                                    .animateContentSize()
                                    .then(
                                        if (isLongOverview) Modifier.pressable(0.985f, haptic = false) { isSynopsisExpanded = !isSynopsisExpanded }
                                        else Modifier
                                    ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SectionLabel("SYNOPSIS", p)
                                Text(
                                    text = displayOverview,
                                    fontSize = 13.5.sp,
                                    lineHeight = 21.sp,
                                    color = p.textSub,
                                    maxLines = if (isSynopsisExpanded || !isLongOverview) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLongOverview) {
                                    Text(
                                        text = if (isSynopsisExpanded) "Show less" else "Read more",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.text
                                    )
                                }
                            }
                        }

                        // Facts: hairline-separated grid, no container
                        val runtime = tmdbDetails?.episodeRunTime ?: anime.duration ?: 24
                        val facts = listOf(
                            "Status" to anime.formattedStatus,
                            "Format" to (if (anime.isMovie) "Movie" else anime.displayFormat),
                            (if (anime.isMovie) "Runtime" else "Episodes") to (if (anime.isMovie) "$runtime min" else "$maxEpisode"),
                            "Per episode" to (if (anime.isMovie) "" else "$runtime min"),
                            "Studio" to (anime.studios.firstOrNull()?.ifBlank { null } ?: "Unknown"),
                            "Source" to anime.displaySource
                        ).filter { it.second.isNotBlank() }
                        Column(modifier = Modifier.fillMaxWidth().staggerIn(4, seen, "meta_4")) {
                            SectionLabel("DETAILS", p)
                            Spacer(Modifier.height(10.dp))
                            facts.chunked(2).forEach { pair ->
                                HairLine(p)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { (label, value) ->
                                        DetailFact(label, value, p, Modifier.weight(1f))
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            HairLine(p)
                        }
                    }
                }

                // Placeholder the pinned tab overlay sits on (see tabsY).
                item(key = "tabs") { Spacer(Modifier.height(TabsHeight)) }

                // === TAB CONTENTS ===
                when (currentTabTitle) {
                    // --- EPISODES: cardless 16:9 rows with the swipe-up 3D scroll effect, lazily composed ---
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
                                        textStyle = TextStyle(color = p.text, fontSize = 13.sp),
                                        cursorBrush = SolidColor(p.text),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        decorationBox = { innerTextField ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(if (p.dark) Color(0xCC161B26) else Color(0xEBFFFFFF))
                                                    .border(1.dp, if (p.dark) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(20.dp))
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = p.textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                                    if (episodeSearchQuery.isEmpty()) {
                                                        Text("Search episode...", fontSize = 13.sp, color = p.textMuted)
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
                                                            tint = p.text,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    GlassCircleButton(p, 40.dp, onClick = { isOldestFirst = !isOldestFirst }) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Sort Order",
                                            tint = if (!isOldestFirst) p.warm else p.text,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    GlassCircleButton(p, 40.dp, onClick = { isEpisodeGridView = !isEpisodeGridView }) {
                                        Icon(
                                            imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                                            contentDescription = "Toggle View",
                                            tint = p.text,
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
                                            p = p,
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
                                        Row(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .pressable(0.94f) {
                                                    val eps = (curStart..curEnd).toList()
                                                    val dlQuality = vm.getLastDownloadQuality() ?: "Auto HD"
                                                    val dlAudio = if (selectedAudioMode.equals("DUB", ignoreCase = true)) "English (Dub)" else "Japanese (Sub)"
                                                    vm.downloadAnimeEpisodes(
                                                        anime = anime,
                                                        episodes = eps,
                                                        provider = selectedProvider,
                                                        quality = dlQuality,
                                                        audio = dlAudio
                                                    )
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Downloading ${eps.size} episodes (${curStart}–${curEnd})...",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .clip(CircleShape)
                                                .background(if (p.dark) Color(0xCC161B26) else Color(0xEBFFFFFF))
                                                .border(1.dp, if (p.dark) Color(0x22FFFFFF) else Color(0x14000000), CircleShape)
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download All",
                                                tint = p.text,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Download All",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.text,
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
                                    if (episodeSearchQuery.isNotBlank()) "No episodes match \"$episodeSearchQuery\"" else "No episodes available",
                                    p
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
                                        val cellShape = RoundedCornerShape(12.dp)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .pressable(0.88f) {
                                                    selectedEpisode = epNum
                                                    if (anime.id <= 0) showMissingAnilistDialog = true
                                                    else onPlay(anime.id, epNum, selectedProvider)
                                                }
                                                .clip(cellShape)
                                                .background(
                                                    if (isCurrent) p.accentA
                                                    else if (isEpFiller) Color(0x24F59E0B)
                                                    else if (isEpMixed) Color(0x246366F1)
                                                    else p.text.copy(alpha = 0.07f)
                                                )
                                                .border(
                                                    0.8.dp,
                                                    if (isCurrent) p.accentA
                                                    else if (isEpFiller) Color(0x60FBBF24)
                                                    else if (isEpMixed) Color(0x60818CF8)
                                                    else p.glassBorder,
                                                    cellShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$epNum",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) p.onAccent
                                                else if (isEpFiller) (if (p.dark) Color(0xFFFDE68A) else Color(0xFF92400E))
                                                else p.text
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
                                    p = p,
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
                                            val dlQuality = vm.getLastDownloadQuality() ?: "Auto HD"
                                            val dlAudio = if (selectedAudioMode.equals("DUB", ignoreCase = true)) "English (Dub)" else "Japanese (Sub)"
                                            vm.downloadAnimeEpisode(
                                                anime = anime,
                                                episode = epNum,
                                                provider = selectedProvider,
                                                episodeTitle = still?.name ?: "Episode $epNum",
                                                quality = dlQuality,
                                                audio = dlAudio
                                            )
                                            android.widget.Toast.makeText(context, "Downloading Ep $epNum...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // --- CHARACTERS: role filter + rows (portrait, role, voice actor) ---
                    "Characters" -> {
                        if (characters.isEmpty()) {
                            if (vm.detailLoading) {
                                items(3, key = { "ch_skel_$it" }) {
                                    ShimmerBox(
                                        modifier = Modifier
                                            .padding(horizontal = 18.dp)
                                            .fillMaxWidth()
                                            .height(92.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                            } else {
                                item(key = "ch_empty") { EmptyNote("No character information available.", p) }
                            }
                        } else {
                            if (charRoles.size > 2) {
                                item(key = "ch_filters") {
                                    SlidingPillTabs(
                                        p = p,
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
                                Box(Modifier.staggerIn(i, seen, "ch_$i")) { CharacterRow(char = c, p = p) }
                            }
                        }
                    }

                    // --- CASTS: 3-column grids so the whole crew is visible at a glance ---
                    "Casts" -> {
                        if (staffList.isEmpty() && tmdbCast.isEmpty()) {
                            item(key = "cast_empty") { EmptyNote("No cast or staff information available.", p) }
                        }
                        if (staffList.isNotEmpty()) {
                            item(key = "staff_h") { SectionHeader("PRODUCTION STAFF", staffList.size, p) }
                            itemsIndexed(
                                staffList.map { Triple(it.name, it.image, it.role) }.chunked(3),
                                key = { i, _ -> "staff_$i" }
                            ) { i, row ->
                                Box(Modifier.staggerIn(i, seen, "pp_${row.firstOrNull()?.first}_$i")) { PeopleRow(row, p) }
                            }
                        }
                        if (tmdbCast.isNotEmpty()) {
                            item(key = "cast_h") { SectionHeader("VOICE CAST (TMDB)", tmdbCast.size, p) }
                            itemsIndexed(
                                tmdbCast.map { Triple(it.name, it.profileUrl, it.character) }.chunked(3),
                                key = { i, _ -> "cast_$i" }
                            ) { i, row ->
                                Box(Modifier.staggerIn(i, seen, "pp_${row.firstOrNull()?.first}_$i")) { PeopleRow(row, p) }
                            }
                        }
                    }

                    // --- VIDEOS: one featured, the rest as compact rows ---
                    "Videos" -> {
                        if (allVideos.isEmpty()) {
                            item(key = "vid_empty") { EmptyNote("No videos or trailers available.", p) }
                        } else {
                            itemsIndexed(allVideos, key = { i, v -> "vid_${v.key}_$i" }) { i, video ->
                                Box(Modifier.staggerIn(i, seen, "vid_$i")) {
                                    VideoItem(
                                        video = video,
                                        featured = i == 0,
                                        p = p,
                                        onClick = { openYoutube(context, video.key) }
                                    )
                                }
                            }
                        }
                    }

                    // --- HOW TO WATCH: grouped vertical rows (prequel / sequel / source / side stories) ---
                    "How to Watch" -> {
                        if (relationGroups.isEmpty()) {
                            item(key = "rel_empty") { EmptyNote("No watch order or relation data available.", p) }
                        } else {
                            relationGroups.forEach { (groupTitle, list) ->
                                item(key = "rel_h_$groupTitle") { SectionHeader(groupTitle, list.size, p) }
                                itemsIndexed(list, key = { i, r -> "rel_${groupTitle}_${r.id}_$i" }) { ri, rel ->
                                    Box(Modifier.staggerIn(ri, seen, "rel_${rel.id}_$ri")) {
                                        RelationRow(rel = rel, p = p, vm = vm)
                                    }
                                }
                            }
                        }
                    }

                    // --- MORE LIKE THIS: 3-column poster grid ---
                    "More Like This" -> {
                        if (recs.isEmpty()) {
                            item(key = "rec_empty") { EmptyNote("No recommendations found.", p) }
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
                                            RecommendationPosterItem(rec = rec, p = p, onClick = { vm.openDetail(rec.id) })
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
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to barBg,
                                0.78f to barBg,
                                1f to barBg.copy(alpha = 0f)
                            ),
                            alpha = tabsPin
                        )
                    }
            ) {
                SlidingPillTabs(
                    p = p,
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
                                listOf(Color(0x8C000000), Color(0x33000000), Color.Transparent)
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
                    GlassCircleButton(p, 40.dp, onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = p.text, modifier = Modifier.size(20.dp))
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
                                    .pressable(0.9f) {
                                        if (anime.id <= 0) showMissingAnilistDialog = true
                                        else onPlay(anime.id, selectedEpisode, selectedProvider)
                                    }
                                    .clip(CircleShape)
                                    .background(p.accentA),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = p.onAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.displayTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (anime.isMovie) "Movie" else "Ep $selectedEpisode of $maxEpisode",
                                    fontSize = 11.sp,
                                    color = p.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.graphicsLayer { alpha = barProgress },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassCircleButton(p, 40.dp, onClick = shareAnime) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = p.text, modifier = Modifier.size(19.dp))
                            }
                            GlassCircleButton(p, 40.dp, onClick = { showWatchlistStatusDialog = true }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isBookmarked) p.warm else p.text,
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
                val prioritizedPoster = tmdbDetails?.posters?.firstOrNull() ?: tmdbDetails?.backdropUrl ?: cachedPoster ?: anime.cover
                DetailLoadingCard(
                    cachedPoster = prioritizedPoster,
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = revealAlpha *
                    (if (coordinator?.phase == SharedTransitionPhase.BACKWARD) coordinator.progress.value else 1f)
            }
            .background(p.bg) // after graphicsLayer so the bg fades too (else it blacks out Home on back)
    ) {
        DetailBackdrop(p, heroImageUrl)
        content()
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
                        tint = p.rating,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Download Stopped", fontWeight = FontWeight.Bold, color = p.text)
                }
            },
            text = {
                Text(
                    text = failedTask.error ?: "Download stopped on ${failedTask.providerName}. Would you like to retry or change server?",
                    color = p.textSub,
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
                    colors = ButtonDefaults.buttonColors(containerColor = p.accentA, contentColor = p.onAccent),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = p.onAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry (${failedTask.providerName})", color = p.onAccent, fontWeight = FontWeight.Bold)
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
                            val dlQuality = failedTask.quality ?: vm.getLastDownloadQuality() ?: "Auto HD"
                            val dlAudio = failedTask.audio ?: if (selectedAudioMode.equals("DUB", ignoreCase = true)) "English (Dub)" else "Japanese (Sub)"
                            vm.downloadAnimeEpisode(
                                anime = anime,
                                episode = ep,
                                provider = selectedProvider,
                                episodeTitle = epTitle,
                                quality = dlQuality,
                                audio = dlAudio
                            )
                            android.widget.Toast.makeText(context, "Retrying Ep $ep...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Retry with Best Server", color = p.text, fontWeight = FontWeight.Bold)
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
                        Text("Cancel", color = p.textMuted)
                    }
                }
            },
            containerColor = p.sheet,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showMissingAnilistDialog) {
        AlertDialog(
            onDismissRequest = { showMissingAnilistDialog = false },
            icon = {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = p.text,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Source Link Unavailable",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.text
                )
            },
            text = {
                Text(
                    text = "This title could not be resolved on AniList. Streaming is unavailable for this entry.",
                    fontSize = 13.5.sp,
                    color = p.textSub,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showMissingAnilistDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.accentA,
                        contentColor = p.onAccent
                    ),
                    shape = OttPillShape
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = p.sheet,
            shape = RoundedCornerShape(22.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Theme helpers: everything reads the same HomePalette as Home (light / dark / liquid glass).
// ─────────────────────────────────────────────────────────────────────────────

/** Amber that stays readable on both themes (bookmarked, sort active, downloading). */
private val HomePalette.warm: Color get() = if (dark) Color(0xFFFFD166) else Color(0xFFB45309)

/** Dialog / sheet surface. */
private val HomePalette.sheet: Color get() = if (dark) Color(0xFF1E1619) else Color(0xFFFFFFFF)

// Text / icons sitting on artwork get a soft shadow instead of a background (same rule as Home).
private val ImgShadow = Shadow(color = Color(0xAA000000), offset = Offset(0f, 2f), blurRadius = 8f)

/** White text with a soft shadow, for labels placed straight on thumbnails. */
@Composable
private fun ShadowText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            shadow = ImgShadow
        )
    )
}

/** Icon with no background: a soft dark copy underneath keeps it readable on any image. */
@Composable
private fun ShadowGlyph(
    icon: ImageVector,
    desc: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color(0x80000000), modifier = Modifier.size(size).offset(0.dp, 1.5.dp))
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

/** Round control: liquid glass when glass is on, a flat themed fill when off. Icon colour comes from the palette. */
@Composable
private fun GlassCircleButton(
    p: HomePalette,
    size: Dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .pressable(0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(if (p.dark) Color(0xCC161B26) else Color(0xEBFFFFFF))
            .border(1.dp, if (p.dark) Color(0x22FFFFFF) else Color(0x14000000), CircleShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Page gradient + (glass only) a blurred copy of the artwork for the glass controls to refract. */
@Composable
private fun DetailBackdrop(p: HomePalette, art: String?) {
    Box(Modifier.fillMaxSize()) {
        HomeBackdrop(p)
    }
}

@Composable
private fun HairLine(p: HomePalette) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(p.glassBorder)
    )
}

@Composable
private fun SectionLabel(title: String, p: HomePalette) {
    Text(
        text = title,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = p.textMuted
    )
}

@Composable
private fun EmptyNote(text: String, p: HomePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = p.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null, p: HomePalette) {
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
            color = p.text
        )
        if (count != null) {
            Text(text = "$count", fontSize = 11.sp, color = p.textMuted)
        }
    }
}

/** Small outlined label (role, format, relation). Not a filled card. */
@Composable
private fun MiniTag(text: String, p: HomePalette, highlight: Boolean = false) {
    val c = if (highlight) p.rating else p.textSub
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .border(0.7.dp, if (highlight) p.rating.copy(alpha = 0.5f) else p.glassBorder, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
    }
}

@Composable
private fun DetailFact(label: String, value: String, p: HomePalette, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = p.textMuted
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Cardless episode row: 16:9 thumbnail, then title + overview as plain text.
 * Keeps the swipe-up scroll animation: slides up + tilts in on first appearance, grows toward you as it scrolls up,
 * then tilts/fades away under the pinned tabs. No centre play badge (tap the row to play); share / download are bare
 * glyphs with a soft shadow instead of dark circles.
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
    p: HomePalette,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val thumbShape = RoundedCornerShape(16.dp)
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

    // Selected episode breathes. Only the selected row runs the infinite transition.
    val glow: State<Float> = if (isSelected) {
        rememberInfiniteTransition(label = "epGlow").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "epGlowV"
        )
    } else remember { mutableStateOf(0f) }

    // Window position, read only inside graphicsLayer lambdas so scrolling never recomposes the row.
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
                    val pr = ((topLimitPx - topPx) / (h * 0.7f)).coerceIn(0f, 1f)
                    sc *= 1f - pr * 0.12f
                    rx += pr * 15f
                    a *= (1f - pr * 0.85f)
                    ty -= pr * 22.dp.toPx()
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
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .background(p.skeleton)
                .then(
                    if (isSelected) Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = p.accentA.copy(alpha = 0.40f + 0.55f * glow.value),
                            style = Stroke(width = 3.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    } else Modifier
                )
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
                        val pr = ((topPx - topLimitPx) / 1400f).coerceIn(0f, 1f)
                        translationY = (pr - 0.5f) * size.height * 0.12f
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
            // Top + bottom scrims so the bare glyphs / labels read on any frame (no badges needed)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x59000000),
                            0.35f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f to Color(0x99000000)
                        )
                    )
            )

            if (isSelected) {
                ShadowText(
                    text = if (progress > 0.05f) "CONTINUE" else "SELECTED",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 11.dp)
                )
            }

            // Episode label (+ filler/mixed) bottom-left
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShadowText("Episode $epNum", 12.sp)
                if (isFiller || isMixed) EpisodeFillerBadge(isMixed = isMixed)
            }

            // Duration bottom-right
            ShadowText(
                text = "${runMin}m",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xEEFFFFFF),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
            )

            // Share + Download, top-right: bare glyphs, generous tap targets
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .pressable(0.8f, onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    ShadowGlyph(Icons.Default.Share, "Share episode $epNum", 18.dp)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .pressable(0.8f, haptic = false, onClick = onDownload), // onDownload already ticks
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDownloaded -> ShadowGlyph(Icons.Default.DownloadDone, "Downloaded", 20.dp, tint = Color(0xFF34D399))
                        isDownloading -> ShadowGlyph(Icons.Default.Downloading, "Downloading", 20.dp, tint = Color(0xFFFFD166))
                        else -> ShadowGlyph(Icons.Default.Download, "Download episode $epNum", 20.dp)
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
                    color = Color.White,
                    trackColor = Color(0x40FFFFFF)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = overview,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = p.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/** Character row: portrait, role, native name and voice actor in one glance. No card, just spacing. */
@Composable
private fun CharacterRow(char: AnimeCharacter, p: HomePalette) {
    val isMain = char.role.equals("MAIN", ignoreCase = true)
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = char.image,
            contentDescription = char.name,
            modifier = Modifier
                .width(66.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(p.skeleton),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = char.name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!char.nativeName.isNullOrBlank()) {
                Text(
                    text = char.nativeName,
                    fontSize = 11.sp,
                    color = p.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniTag(
                text = char.role.lowercase().replaceFirstChar { it.uppercase() },
                p = p,
                highlight = isMain
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
                            .background(p.skeleton)
                            .border(0.6.dp, p.glassBorder, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = char.voiceActorName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.textSub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Japanese VA", fontSize = 9.5.sp, color = p.textMuted)
                    }
                }
            }
        }
    }
}

/** One row of up to 3 people (photo, name, subtitle). No card. */
@Composable
private fun PeopleRow(people: List<Triple<String, String?, String?>>, p: HomePalette) {
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
                    .heightIn(min = 140.dp)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model = image,
                    contentDescription = name,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(p.skeleton)
                        .border(0.8.dp, p.glassBorder, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.text,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!sub.isNullOrBlank()) {
                    Text(
                        text = sub,
                        fontSize = 10.sp,
                        color = p.textMuted,
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
private fun VideoItem(video: TmdbVideo, featured: Boolean, p: HomePalette, onClick: () -> Unit) {
    val thumb = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg"
    if (featured) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .pressable(0.98f, onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.skeleton)
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
                        .background(Brush.verticalGradient(listOf(Color(0x26000000), Color(0x80000000))))
                )
                ShadowGlyph(
                    Icons.Default.PlayArrow, "Play video", 52.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                ShadowText(
                    text = video.type.uppercase(Locale.US),
                    fontSize = 10.5.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                )
            }
            Text(
                text = video.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    } else {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .pressable(0.98f, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.skeleton)
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
                        .background(Color(0x33000000))
                )
                ShadowGlyph(
                    Icons.Default.PlayArrow, "Play video", 26.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = video.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                MiniTag(video.type.uppercase(Locale.US), p)
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
private fun RelationRow(rel: MediaRelation, p: HomePalette, vm: MainViewModel) {
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
                if (isNovel) vm.openNovelDetail(rel.title, rel.title, rel.cover)
                else if (isManga) vm.openMangaDetail(rel.id.toString(), rel.title, rel.cover)
                else vm.openDetail(rel.id)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = rel.cover,
            contentDescription = null,
            modifier = Modifier
                .width(58.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(10.dp))
                .background(p.skeleton),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = rel.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTag(displayBadge.uppercase(Locale.US), p)
                if (relationLabel.isNotBlank()) MiniTag(relationLabel, p)
            }
        }
        Text(
            text = if (isNovel || isManga) "Read ➔" else "Watch ➔",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.text
        )
    }
}

@Composable
private fun RecommendationPosterItem(
    rec: AnimeRecommendation,
    p: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(0.94f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(14.dp))
                .background(p.skeleton)
        ) {
            AsyncImage(
                model = rec.cover,
                contentDescription = rec.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Score sits straight on the poster with a soft shadow (no badge box)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to Color(0x66000000), 0.3f to Color.Transparent))
            )
            rec.score?.let { score ->
                ShadowText(
                    text = "★ $score%",
                    fontSize = 10.5.sp,
                    color = Color(0xFFFFD166),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = rec.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = p.text,
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
    val p = rememberHomePalette()

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
            .background(p.pageBrush) // after graphicsLayer so the bg dissolves with the curtain
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
                .background(Brush.verticalGradient(listOf(p.bg.copy(alpha = 0.25f), p.bg.copy(alpha = 0.80f), p.bg)))
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
                            color = p.text.copy(alpha = glow * 0.10f),
                            topLeft = Offset(-pad, -pad),
                            size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx() + pad)
                        )
                    }
                    .clip(cardShape)
                    .background(p.skeleton)
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
                                    colors = listOf(Color.Transparent, Color(0x30FFFFFF), Color.Transparent),
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
                            Brush.linearGradient(listOf(Color(0x80FFFFFF), Color(0x08FFFFFF), Color(0x40FFFFFF))),
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
                        color = p.text,
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
                        color = p.textSub,
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
                        tint = p.text,
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
    val p = rememberHomePalette()
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(p.text.copy(alpha = 0.08f))
            .drawBehind {
                val w = size.width
                val x = -w + sweep * w * 2.5f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, p.text.copy(alpha = 0.14f), Color.Transparent),
                        Offset(x, 0f),
                        Offset(x + w * 0.6f, 0f)
                    )
                )
            }
    )
}

/**
 * Horizontally scrollable pill tabs with one solid pill that slides (spring) to the selected tab,
 * and the row auto-centers the selection. Used for the main tabs, episode ranges and character roles.
 * Selected = accent pill (white on dark, black on light); the rest are quiet outlined pills with theme text.
 */
@Composable
private fun SlidingPillTabs(
    p: HomePalette,
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
                        .background(p.accentA)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { i, label ->
                    val isSel = i == sel
                    val textColor by animateColorAsState(
                        targetValue = if (isSel) p.onAccent else p.textSub,
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
                            .background(if (isSel) Color.Transparent else p.text.copy(alpha = 0.06f))
                            .border(0.8.dp, if (isSel) Color.Transparent else p.glassBorder, CircleShape)
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

/**
 * Primary play pill. Solid accent (white on dark, black on light) like Home's main buttons, so it reads on every theme
 * and with glass on or off. Idle glow + light sweep, and the label slides when the episode changes.
 */
@Composable
private fun HeroPlayButton(
    label: String,
    p: HomePalette,
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
        initialValue = 0.12f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "playGlow"
    )
    val band = if (p.dark) Color(0x26000000) else Color(0x33FFFFFF)
    Row(
        modifier = modifier
            .height(50.dp)
            .pressable(0.95f, onClick = onClick)
            .drawBehind {
                val pad = 5.dp.toPx()
                drawRoundRect(
                    color = p.accentA.copy(alpha = glow * 0.4f),
                    topLeft = Offset(-pad, -pad),
                    size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                    cornerRadius = CornerRadius(size.height / 2f + pad)
                )
            }
            .clip(CircleShape)
            .background(p.accentA)
            .drawWithContent {
                drawContent()
                // Soft reflection band that glides across, then rests.
                if (sweep < 0.6f) {
                    val w = size.width
                    val x = -w * 0.4f + (sweep / 0.6f) * w * 1.8f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, band, Color.Transparent),
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
            tint = p.onAccent,
            modifier = Modifier.size(24.dp)
        )
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(120)))
            },
            label = "playLabel"
        ) { text ->
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.onAccent, maxLines = 1)
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
