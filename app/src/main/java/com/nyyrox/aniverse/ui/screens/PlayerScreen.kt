@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import com.nyyrox.aniverse.player.CineStreamLoader
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.TextStyle
import com.styropyr0.prismal.components.PrismalHorizontalSelector
import com.styropyr0.prismal.components.PrismalSegmentedControl
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nyyrox.aniverse.data.filler.AnimeFillerInfo
import com.nyyrox.aniverse.data.filler.AnimeFillerRepository
import com.nyyrox.aniverse.ui.components.EpisodeFillerBadge
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.DUB_PROVIDERS
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.SUB_PROVIDERS
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
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
import com.nyyrox.aniverse.data.download.DownloadStatus
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.LocalIsDark

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    animeId: Int,
    initialEpisode: Int,
    initialProviderId: String?,
    onBack: () -> Unit,
    liquidGlass: Boolean = true // wire to your settings toggle; false = flat AMOLED cards
) {
    val context = LocalContext.current
    val c = LocalAppColors.current
    val isDark = LocalIsDark.current

    // Mini player: hide it while this screen is open; drop a parked player that isn't this episode
    DisposableEffect(Unit) {
        com.nyyrox.aniverse.player.MiniPlayerManager.fullScreenOpen = true
        com.nyyrox.aniverse.player.MiniPlayerManager.parked?.let {
            if (it.routeAnimeId != animeId || it.episode != initialEpisode) {
                com.nyyrox.aniverse.player.MiniPlayerManager.release(context)
            }
        }
        onDispose { com.nyyrox.aniverse.player.MiniPlayerManager.fullScreenOpen = false }
    }

    DisposableEffect(Unit) {
        vm.setActivityMode(com.nyyrox.aniverse.data.CurrentActivityMode.WATCHING_ANIME)
        onDispose {
            vm.setActivityMode(com.nyyrox.aniverse.data.CurrentActivityMode.EXPLORING_APP)
            vm.updateCurrentActivity(null, null, null)
            vm.syncTimeSpentAndReadingToCloud()
        }
    }

    LaunchedEffect(animeId) {
        if (vm.selected?.id != animeId) {
            vm.loadAnimeForPlayer(animeId)
        }
    }

    val downloadedEpForFallback = remember(animeId, initialEpisode) {
        vm.getDownloadedAnimeEpisode(animeId, initialEpisode)
    }
    val downloadedEpisodesForAnime = remember(animeId) {
        vm.downloadManager.getDownloadedEpisodesForAnime(animeId)
    }
    val anime = vm.selected?.takeIf { it.id == animeId } ?: downloadedEpForFallback?.let { ep ->
        val maxDownloadedEp = downloadedEpisodesForAnime.maxOfOrNull { it.episode } ?: ep.episode
        Anime(
            id = ep.animeId,
            title = ep.animeTitle.ifBlank { "Anime #$animeId" },
            cover = ep.animeCover,
            banner = ep.animeCover,
            episodes = maxDownloadedEp,
            status = "FINISHED",
            format = "TV"
        )
    }

    if (anime == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(c.bg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 8.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(c.surfaceElevated)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = c.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (vm.detailError != null || animeId <= 0) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Missing AniList ID",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please check if the requested show is missing an AniList ID on the server. If we made a mistake, please contact us in the Community tab for fixes!",
                        color = c.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                        shape = OttPillShape
                    ) {
                        Text("Go Back", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                CircularProgressIndicator(color = c.accent)
            }
        }
        return
    }

    val maxEpisode = maxOf(anime.episodes ?: 1, anime.nextEpisode ?: 1)
    var currentEpisode by remember(animeId, initialEpisode) { mutableIntStateOf(initialEpisode) }

    var fillerInfo by remember(anime.id) {
        mutableStateOf(AnimeFillerRepository.getCachedFillerInfo(anime.id) ?: AnimeFillerInfo(anime.id))
    }
    LaunchedEffect(anime.id, anime.title) {
        fillerInfo = AnimeFillerRepository.getFillerInfo(anime.id, anime.title, anime.romajiTitle)
    }

    LaunchedEffect(anime.title, currentEpisode) {
        val title = anime.title ?: "Anime"
        vm.updateCurrentActivity(
            activity = "Watching $title Ep. $currentEpisode",
            activityType = "ANIME",
            targetId = anime.id.toString()
        )
    }
    val initialProv = ALL_PROVIDERS.firstOrNull { it.id == initialProviderId }
        ?: vm.preferredServerId?.let { prefId -> ALL_PROVIDERS.firstOrNull { it.id == prefId } }
        ?: com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders("sub", anime.id).first()
    val isAcdnMapped = remember(anime.id) {
        com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.isMapped(anime.id)
    }
    val safeInitialProv = if (!isAcdnMapped && (initialProv.id.contains("animesalt") || initialProv.id.contains("acdn"))) {
        com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders("sub", anime.id).first()
    } else {
        initialProv
    }
    var currentProvider by remember(animeId, initialProviderId) { mutableStateOf(safeInitialProv) }
    var selectedSourceMode by remember { mutableStateOf(if (initialProv.isDirectStream) "DIRECT" else "EMBED") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialProv.isDirectStream) {
                if (initialProv.isDub) "DUB" else "SUB"
            } else {
                if (initialProv.isMulti) "MULTI" else if (initialProv.isDub) "DUB" else "SUB"
            }
        )
    }
    var isServerDropdownExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var sniffedStreamUrl by remember(currentProvider.id, currentEpisode) { mutableStateOf<String?>(null) }
    var sniffedStreamHeaders by remember(currentProvider.id, currentEpisode) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sniffedStreamSubs by remember(currentProvider.id, currentEpisode) { mutableStateOf<List<com.nyyrox.aniverse.data.subtitles.SubtitleTrack>>(emptyList()) }
    var playInZenkaiPlayer by remember(currentProvider.id, currentEpisode) { mutableStateOf(false) }
    var isSniffingStream by remember(currentProvider.id, currentEpisode) { mutableStateOf(false) }
    var showDownloadSheetEpisode by remember { mutableStateOf<Pair<Int, String?>?>(null) }
    var showDownloadRangeSheet by remember { mutableStateOf<IntRange?>(null) }

    // Dynamically query available direct servers for this anime & episode
    var availableDirectServers by remember(anime.id, selectedCategory, currentEpisode) {
        mutableStateOf(com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory, anime.id))
    }

    // Dynamically track episode audio availability (SUB vs DUB)
    var audioAvailability by remember(anime.id, currentEpisode) {
        mutableStateOf(
            com.nyyrox.aniverse.data.extractor.EpisodeAudioAvailability(
                hasSub = true,
                hasDub = isAcdnMapped,
                hasAcdn = isAcdnMapped
            )
        )
    }

    LaunchedEffect(anime.id, currentEpisode) {
        try {
            val avail = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.checkEpisodeAudioAvailability(
                anilistId = anime.id,
                episode = currentEpisode,
                animeTitle = anime.title
            )
            audioAvailability = avail
            // If current episode does not have DUB available, auto fallback to SUB
            if (!avail.hasDub && selectedCategory == "DUB") {
                selectedCategory = "SUB"
                if (currentProvider.isDub || currentProvider.id.contains("dub", ignoreCase = true)) {
                    val subProv = availableDirectServers.firstOrNull { !it.isDub && !it.isMulti }
                        ?: ALL_PROVIDERS.firstOrNull { it.isEmbedOnly && !it.isDub && !it.isMulti }
                    if (subProv != null) {
                        currentProvider = subProv
                        vm.setPreferredServer(subProv.id)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(anime.id, selectedCategory, currentEpisode) {
        try {
            val servers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getAvailableDirectProviders(
                anilistId = anime.id,
                audioMode = selectedCategory,
                episode = currentEpisode
            )
            val filtered = if (!isAcdnMapped) {
                servers.filter { !it.id.contains("animesalt") && !it.id.contains("acdn") }
            } else {
                servers
            }
            if (filtered.isNotEmpty()) {
                availableDirectServers = filtered
                // If in DIRECT mode and current provider is not in the available list, smoothly select the first available
                if (selectedSourceMode == "DIRECT" && filtered.none { it.id == currentProvider.id }) {
                    currentProvider = filtered.first()
                    vm.setPreferredServer(filtered.first().id)
                }
            } else {
                availableDirectServers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory, anime.id)
            }
        } catch (_: Exception) {}
    }

    fun switchToNextServer() {
        val currentList = if (selectedSourceMode == "DIRECT") {
            availableDirectServers
        } else {
            when (selectedCategory) {
                "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub && (isAcdnMapped || !it.id.contains("animesalt")) }
                "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
                else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
            }
        }
        if (currentList.isEmpty()) return
        val currentIndex = currentList.indexOfFirst { it.id == currentProvider.id }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % currentList.size else 0
        val nextProv = currentList[nextIndex]
        if (nextProv.id != currentProvider.id) {
            currentProvider = nextProv
            vm.setPreferredServer(nextProv.id)
        }
    }

    val season = remember(anime.id, anime.title) { com.nyyrox.aniverse.data.mapping.AnimeSeasonMapper.resolveSeason(anime.title, anime.romajiTitle) }

    // Fetch direct captions and OpenSubtitles from SubtitleRepository in background
    var externalSubtitles by remember { mutableStateOf<List<com.nyyrox.aniverse.data.subtitles.SubtitleTrack>>(emptyList()) }
    val isCurrentDub = currentProvider.isDub || selectedCategory == "DUB"
    LaunchedEffect(anime.id, currentEpisode, isCurrentDub) {
        externalSubtitles = emptyList()
        try {
            val isMovie = anime.format?.equals("MOVIE", ignoreCase = true) == true
            val audioMode = if (isCurrentDub) "dub" else "sub"
            val subs = com.nyyrox.aniverse.data.subtitles.SubtitleRepository.fetchAllSubtitles(
                anilistId = anime.id,
                title = anime.title,
                episode = currentEpisode,
                season = season,
                isMovie = isMovie,
                audio = audioMode
            )
            externalSubtitles = subs
        } catch (_: Exception) {}
    }
    var tmdbEpisodes by remember(anime.id) { mutableStateOf<Map<Int, com.nyyrox.aniverse.data.tmdb.TmdbEpisodeStill>>(emptyMap()) }
    LaunchedEffect(anime.id, anime.title, season) {
        val eps = com.nyyrox.aniverse.data.tmdb.TmdbRepository.fetchEpisodesMapped(
            anilistId = anime.id,
            title = anime.title,
            romajiTitle = anime.romajiTitle,
            season = season,
            totalEpisodes = anime.episodes ?: anime.nextEpisode,
            format = anime.format
        )
        if (eps.isNotEmpty()) {
            tmdbEpisodes = eps
            val currentStill = eps[currentEpisode]?.stillUrl
            if (!currentStill.isNullOrBlank()) {
                vm.updateHistoryEpisodeStill(anime.id, currentEpisode, currentStill)
            }
        }
    }

    // Build / cache AnimeSalt relationship once on player initialization (AniList episode-order authority)
    var animeSaltRel by remember(anime.id) {
        mutableStateOf<com.nyyrox.aniverse.data.animesalt.AnimeSaltSeriesRelationship?>(null)
    }
    // true once the lookup coroutine has finished (whether it found a mapping or not)
    var animeSaltRelChecked by remember(anime.id) { mutableStateOf(false) }
    LaunchedEffect(anime.id) {
        animeSaltRelChecked = false
        animeSaltRel = com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.getOrBuildRelationship(
            anilistId = anime.id,
            title = anime.title,
            romajiTitle = anime.romajiTitle,
            tmdbTitle = null,
            format = anime.format,
            totalEpisodes = maxEpisode
        )
        animeSaltRelChecked = true
    }

    // No mapping in anime-mapping.json — show "Unavailable" instead of a spinner
    val animeSaltNoMapping = (!isAcdnMapped || (animeSaltRelChecked && animeSaltRel == null)) &&
            (currentProvider.id.startsWith("animesalt") || currentProvider.id.contains("acdn"))

    val currentAnimeSaltSlug = remember(animeSaltRel, currentEpisode) {
        com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.resolveSlug(
            relationship = animeSaltRel,
            title = anime.title,
            romajiTitle = anime.romajiTitle,
            format = anime.format,
            episode = currentEpisode,
            anilistId = anime.id
        )
    }

    var animeSaltStreamUrl by remember(currentProvider.id, currentEpisode) { mutableStateOf<String?>(null) }
    var animeSaltStreamResolved by remember(currentProvider.id, currentEpisode) { mutableStateOf(false) }

    LaunchedEffect(currentProvider.id, currentEpisode, animeSaltRel) {
        if (currentProvider.id.startsWith("animesalt") && !currentProvider.isDirectStream) {
            animeSaltStreamUrl = null
            animeSaltStreamResolved = false
            val isAbyssServer = currentProvider.id.contains("dub", ignoreCase = true) || currentProvider.id.contains("abyss", ignoreCase = true)
            val resUrl = com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.resolveEpisodeStreamUrl(
                relationship = animeSaltRel,
                title = anime.title,
                romajiTitle = anime.romajiTitle,
                format = anime.format,
                episode = currentEpisode,
                isAbyss = isAbyssServer,
                anilistId = anime.id
            )
            animeSaltStreamUrl = resUrl
            animeSaltStreamResolved = true
        } else {
            animeSaltStreamResolved = false
        }
    }

    var directStreamResult by remember(currentProvider.id, currentEpisode) {
        mutableStateOf<com.nyyrox.aniverse.data.extractor.DirectStreamResult?>(null)
    }
    var isDirectStreamLoading by remember(currentProvider.id, currentEpisode) {
        mutableStateOf(false)
    }
    var directStreamFailed by remember(currentProvider.id, currentEpisode) {
        mutableStateOf(false)
    }
    var manualSelectedServer by remember(currentProvider.id, currentEpisode) {
        mutableStateOf<com.nyyrox.aniverse.data.extractor.StreamServerOption?>(null)
    }

    LaunchedEffect(currentProvider.id, currentEpisode) {
        if (currentProvider.isDirectStream) {
            isDirectStreamLoading = true
            directStreamFailed = false
            try {
                val res = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.resolveDirectStream(
                    anilistId = anime.id,
                    episode = currentEpisode,
                    providerId = currentProvider.id,
                    isDub = currentProvider.isDub,
                    animeTitle = anime.title
                )
                directStreamResult = res
                directStreamFailed = (res == null)
            } catch (_: Exception) {
                directStreamFailed = true
            } finally {
                isDirectStreamLoading = false
            }
        } else {
            directStreamResult = null
            directStreamFailed = false
            isDirectStreamLoading = false
        }
    }

    var malIdState by remember(anime.id) { mutableStateOf<Int?>(anime.idMal) }
    var isResolvingMalId by remember(anime.id) { mutableStateOf(malIdState == null && currentProvider.requiresMalId) }

    LaunchedEffect(anime.id) {
        if (malIdState == null) {
            isResolvingMalId = true
            var id = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(anime.id)?.malId
            if (id == null || id <= 0) {
                id = com.nyyrox.aniverse.data.AniListRepository().fetchMalId(anime.id)
            }
            if (id != null && id > 0) {
                malIdState = id
            }
            isResolvingMalId = false
        }
    }

    // Resolve TMDB ID for TMDB-based embed providers
    var tmdbIdState by remember(anime.id) { mutableStateOf<Int?>(null) }
    var tmdbTypeState by remember(anime.id) { mutableStateOf<String?>(null) }
    var isResolvingTmdbId by remember(anime.id) { mutableStateOf(false) }
    LaunchedEffect(anime.id) {
        if (tmdbIdState == null) {
            isResolvingTmdbId = true
            try {
                val resolved = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(anime.id)
                if (resolved?.tmdbId != null && resolved.tmdbId > 0) {
                    tmdbIdState = resolved.tmdbId
                    tmdbTypeState = resolved.tmdbType ?: if (anime.format.equals("MOVIE", ignoreCase = true)) "movie" else "tv"
                } else {
                    val tmdbResult = com.nyyrox.aniverse.data.tmdb.TmdbRepository.resolveTmdbId(
                        anilistId = anime.id,
                        title = anime.title,
                        year = anime.seasonYear,
                        format = anime.format
                    )
                    if (tmdbResult != null && tmdbResult.id > 0) {
                        tmdbIdState = tmdbResult.id
                        tmdbTypeState = tmdbResult.type
                    }
                }
            } catch (_: Exception) {}
            isResolvingTmdbId = false
        }
    }

    val isMovie = remember(anime.format, tmdbTypeState) {
        anime.format.equals("MOVIE", ignoreCase = true) || tmdbTypeState == "movie"
    }

    val animeSaltFailed = currentProvider.id.startsWith("animesalt") &&
            !currentProvider.isDirectStream &&
            animeSaltStreamResolved &&
            animeSaltStreamUrl.isNullOrBlank()

    val isAnimeSaltLoading = currentProvider.id.startsWith("animesalt") &&
            !currentProvider.isDirectStream &&
            !animeSaltNoMapping &&
            !animeSaltStreamResolved

    val isMalIdLoading = currentProvider.requiresMalId && malIdState == null && isResolvingMalId
    val isTmdbIdLoading = currentProvider.requiresTmdbId && tmdbIdState == null && isResolvingTmdbId
    val tmdbFailed = currentProvider.requiresTmdbId && !isResolvingTmdbId && tmdbIdState == null


    val downloadedAnimeList by vm.downloadManager.downloadedAnime.collectAsState()
    val activeAnimeTasks by vm.downloadManager.animeTasks.collectAsState()
    val isCurrentEpDownloaded = remember(anime.id, currentEpisode, downloadedAnimeList) {
        vm.isAnimeEpisodeDownloaded(anime.id, currentEpisode)
    }
    val downloadedEpInfo = remember(anime.id, currentEpisode, downloadedAnimeList) {
        if (isCurrentEpDownloaded) vm.getDownloadedAnimeEpisode(anime.id, currentEpisode) else null
    }
    val isCurrentEpDownloading = remember(anime.id, currentEpisode, activeAnimeTasks) {
        activeAnimeTasks.containsKey("${anime.id}:$currentEpisode")
    }
    val failedTaskForCurrentAnime = remember(activeAnimeTasks, anime.id) {
        activeAnimeTasks.values.firstOrNull { it.animeId == anime.id && it.status == com.nyyrox.aniverse.data.download.DownloadStatus.FAILED }
    }

    var playOfflineMode by remember(anime.id, currentEpisode, isCurrentEpDownloaded) {
        mutableStateOf(isCurrentEpDownloaded)
    }

    val isPlayingOffline = isCurrentEpDownloaded && playOfflineMode && downloadedEpInfo != null

    val isStreamLoading = !isPlayingOffline && ((currentProvider.isDirectStream && isDirectStreamLoading) || isAnimeSaltLoading || isMalIdLoading || isTmdbIdLoading)

    // Mini player handoff: keep the parked stream URL (a re-fetched URL differs and would restart the video)
    val adoptUrl = remember(currentEpisode, currentProvider.id) {
        com.nyyrox.aniverse.player.MiniPlayerManager.parked
            ?.takeIf { it.routeAnimeId == animeId && it.episode == currentEpisode && it.providerId == currentProvider.id }
            ?.streamUrl
    }
    val adopting = adoptUrl != null && manualSelectedServer == null

    val activeStreamUrl = when {
        isPlayingOffline -> {
            val targetFile = java.io.File(downloadedEpInfo!!.filePath)
            val epDir = targetFile.parentFile
            val playlistFile = if (epDir != null) java.io.File(epDir, "playlist.m3u8") else null
            if (playlistFile != null && playlistFile.exists() && playlistFile.length() > 0) {
                playlistFile.absolutePath
            } else {
                downloadedEpInfo.filePath
            }
        }
        manualSelectedServer != null -> manualSelectedServer!!.streamUrl
        currentProvider.isDirectStream -> directStreamResult?.streamUrl.orEmpty()
        currentProvider.id.startsWith("animesalt") -> animeSaltStreamUrl.orEmpty()
        else -> currentProvider.url(anime.id, currentEpisode, currentAnimeSaltSlug, malIdState, tmdbIdState, isMovie = isMovie)
    }

    val activeDirectHeaders = if (isPlayingOffline) emptyMap() else (manualSelectedServer?.headers ?: directStreamResult?.headers ?: emptyMap())
    val activeDirectSubs = if (isPlayingOffline) (downloadedEpInfo?.subtitles ?: emptyList()) else (directStreamResult?.subtitles ?: emptyList())
    val isDirectStreamActive = isPlayingOffline || (manualSelectedServer?.isHls == true) || (directStreamResult?.isDirect == true)

    // Pre-extract next episode iframe in background so episode switching is instantaneous
    LaunchedEffect(currentEpisode, animeSaltRel) {
        val nextEp = currentEpisode + 1
        if (nextEp <= maxEpisode) {
            val nextSlug = animeSaltRel?.episodeMap?.get(nextEp)?.slug
            if (!nextSlug.isNullOrBlank()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val isMovie = anime.format?.equals("MOVIE", ignoreCase = true) == true
                        com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.extractServers(nextSlug, isMovie)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Fullscreen state & Activity resolution
    var isFullscreen by remember { mutableStateOf(false) }
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    // Top strip above the video is always black, so force light status-bar icons here and restore on exit.
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    fun toggleFullscreen() {
        val target = !isFullscreen
        isFullscreen = target
        activity?.let { act ->
            val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (target) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Episode selection & pagination states
    val pageSize = 25
    val pageCount = (maxEpisode + pageSize - 1) / pageSize
    var selectedPage by remember(animeId) { mutableIntStateOf((currentEpisode - 1) / pageSize) }
    var isEpisodeGridView by remember { mutableStateOf(false) }
    // Below-video tab: EPISODES / OVERVIEW / SERVERS (survives fullscreen toggles)
    var detailTab by remember(animeId) { mutableStateOf("EPISODES") }

    // Keep page in sync when episode changes
    LaunchedEffect(currentEpisode) {
        val targetPage = (currentEpisode - 1) / pageSize
        if (targetPage != selectedPage) {
            selectedPage = targetPage
        }
    }

    // Record watch history automatically on load with episode still
    val currentStillUrl = tmdbEpisodes[currentEpisode]?.stillUrl
        ?: vm.tmdbEpisodeStills["${anime.id}_$currentEpisode"]
    LaunchedEffect(currentEpisode, currentStillUrl) {
        vm.recordHistory(anime, currentEpisode, episodeStill = currentStillUrl)
    }

    var isScreenExiting by remember { mutableStateOf(false) }

    // Broadcast initial Discord RPC status immediately on episode/provider mount
    LaunchedEffect(anime.id, currentEpisode, currentProvider.id) {
        val coverUrl = anime.cover.takeIf { it.isNotBlank() } ?: anime.banner
        if (!coverUrl.isNullOrBlank()) {
            com.nyyrox.aniverse.data.discord.DiscordRpcService.preFetchAsset(coverUrl)
        }
        com.nyyrox.aniverse.data.discord.DiscordRpcService.updatePresence(
            animeTitle = anime.title,
            episodeNum = currentEpisode,
            isDub = currentProvider.isDub,
            currentPositionMs = 0L,
            durationMs = 0L,
            isPlaying = true,
            coverUrl = coverUrl
        )
    }

    // Clean up orientation on disposal
    DisposableEffect(Unit) {
        onDispose {
            isScreenExiting = true
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
        }
    }

    val dragProgress = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenW = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenH = with(density) { configuration.screenHeightDp.dp.toPx() }
    val miniW = with(density) { 208.dp.toPx() }
    val miniH = miniW / (16f / 9f)
    val horizontalMargin = with(density) { 12.dp.toPx() }
    val bottomClearance = with(density) { 110.dp.toPx() }
    val targetMiniX = (screenW - miniW - horizontalMargin).coerceAtLeast(0f)
    val targetMiniY = (screenH - miniH - bottomClearance).coerceAtLeast(0f)
    val targetScale = (miniW / screenW.coerceAtLeast(1f)).coerceIn(0.35f, 0.70f)
    val maxDragPx = with(density) { 260.dp.toPx() }

    LaunchedEffect(animeId, currentEpisode) {
        dragProgress.snapTo(0f)
        isScreenExiting = false
    }

    val handleExit = {
        if (!isScreenExiting) {
            isScreenExiting = true
            coroutineScope.launch {
                // Park player into MiniPlayerManager immediately; MiniPlayerHost takes over from full videoSlot bounds to mini window
                com.nyyrox.aniverse.player.MiniPlayerManager.startAtCollapsed = false
                com.nyyrox.aniverse.player.MiniPlayerManager.parkRequest =
                    com.nyyrox.aniverse.player.MiniPlayerManager.ParkRequest(animeId, currentEpisode, currentProvider.id)
                com.nyyrox.aniverse.player.MiniPlayerManager.parkNow?.invoke()
                com.nyyrox.aniverse.player.MiniPlayerManager.fullScreenOpen = false
                com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                onBack()
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            handleExit()
        }
    }


    // ── Source / audio / Zenkai-player actions (shared by the Servers tab and the header) ──
    fun selectSourceMode(modeKey: String) {
        com.nyyrox.aniverse.util.AppHaptics.tick(context)
        if (selectedSourceMode == modeKey) return
        selectedSourceMode = modeKey
        if (modeKey == "DIRECT") {
            vm.setPlayEmbed(false)
            if (selectedCategory == "MULTI") selectedCategory = "SUB"
            val nextDirect = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory)
            availableDirectServers = nextDirect
            val prov = nextDirect.first()
            currentProvider = prov
            vm.setPreferredServer(prov.id)
        } else {
            vm.setPlayEmbed(true)
            val embedList = when (selectedCategory) {
                "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub && (isAcdnMapped || !it.id.contains("animesalt")) }
                "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
                else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
            }
            val prov = embedList.firstOrNull() ?: ALL_PROVIDERS.first { it.isEmbedOnly }
            currentProvider = prov
            vm.setPreferredServer(prov.id)
        }
    }

    fun selectCategory(catKey: String) {
        com.nyyrox.aniverse.util.AppHaptics.tick(context)
        if (selectedCategory == catKey) return
        selectedCategory = catKey
        if (selectedSourceMode == "DIRECT") {
            val nextDirect = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(catKey, anime.id)
            availableDirectServers = nextDirect
            val newProv = nextDirect.first()
            currentProvider = newProv
            vm.setPreferredServer(newProv.id)
        } else {
            val embedServers = when (catKey) {
                "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub && (isAcdnMapped || !it.id.contains("animesalt")) }
                "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
                else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti && (isAcdnMapped || !it.id.contains("animesalt")) }
            }
            val newProv = embedServers.firstOrNull() ?: ALL_PROVIDERS.first { it.isEmbedOnly }
            currentProvider = newProv
            vm.setPreferredServer(newProv.id)
        }
    }

    fun toggleZenkaiPlayer() {
        com.nyyrox.aniverse.util.AppHaptics.tick(context)
        if (playInZenkaiPlayer) {
            playInZenkaiPlayer = false
            return
        }
        if (!sniffedStreamUrl.isNullOrBlank()) {
            playInZenkaiPlayer = true
            return
        }
        coroutineScope.launch {
            isSniffingStream = true
            try {
                val sniffed = com.nyyrox.aniverse.player.StreamSniffer.sniff(
                    context = context,
                    embedUrl = activeStreamUrl,
                    timeoutMs = 12000L
                )
                if (sniffed != null && sniffed.streamUrl.isNotBlank()) {
                    sniffedStreamUrl = sniffed.streamUrl
                    sniffedStreamHeaders = sniffed.headers
                    sniffedStreamSubs = sniffed.subtitles
                    playInZenkaiPlayer = true
                } else {
                    android.widget.Toast.makeText(context, "Direct stream could not be extracted", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "Extraction failed", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isSniffingStream = false
            }
        }
    }

    // One backdrop (ambient artwork) sampled by every glass surface. null = glass off.
    val glassLayer = rememberPrismalGlassLayer()
    // Shader glass needs Android 13+ (AGSL) and a device with headroom; everything else gets the flat AMOLED cards.
    val glassOk = remember {
        android.os.Build.VERSION.SDK_INT >= 33 &&
            context.getSystemService(android.app.ActivityManager::class.java)?.isLowRamDevice != true
    }
    // Follows Profile > Liquid Glassmorphism, same switch the details page uses.
    val glassSetting = vm.themeSettings.isGlassmorphismEnabled
    val backdrop = if (liquidGlass && glassSetting && glassOk) glassLayer else null

    val progress = dragProgress.value
    val bgAlpha = if (!isFullscreen) (1f - progress * 1.6f).coerceIn(0f, 1f) else 1f

    val playerBg = if (isDark) Color(0xFF000000) else Color.White
    val playerSurface = if (isDark) Color(0xFF101012) else Color(0xFFF4F4F5)
    val playerSurfaceElevated = if (isDark) Color(0xFF18181B) else Color(0xFFE4E4E7)
    val playerBorder = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
    val playerHairline = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
    val playerTextPrimary = if (isDark) Color.White else Color(0xFF09090B)
    val playerTextSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val playerTextMuted = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    val playerActiveBg = if (isDark) Color.White else Color.Black
    val playerActiveContent = if (isDark) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else playerBg.copy(alpha = bgAlpha))
            .then(if (!isFullscreen) Modifier.navigationBarsPadding() else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Strip behind the status bar
            if (!isFullscreen) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(if (isDark) Color.Black.copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha))
                )
            }
            // Video Player Container (Morphs into floating mini-player box at bottom-right)
            Box(
                modifier = (if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .graphicsLayer {
                            if (progress > 0f) {
                                val currentScale = lerp(1f, targetScale, progress)
                                scaleX = currentScale
                                scaleY = currentScale
                                transformOrigin = TransformOrigin(0f, 0f)
                                translationX = lerp(0f, targetMiniX, progress)
                                translationY = lerp(0f, targetMiniY, progress)
                                shadowElevation = (progress * 16f).dp.toPx()
                                shape = RoundedCornerShape((progress * 16f).dp)
                                clip = progress > 0.04f
                            }
                        }
                        .background(Color.Black)
                }).onGloballyPositioned {
                    if (progress == 0f && !isScreenExiting && !isFullscreen) {
                        com.nyyrox.aniverse.player.MiniPlayerManager.videoSlot = it.boundsInRoot()
                    }
                }
            ) {
            key(currentEpisode, currentProvider.id) {
                val playingEpisode = currentEpisode
                val savedPos = remember(anime.id, playingEpisode) {
                    vm.getSavedPosition(anime.id, playingEpisode)
                }
                if (com.nyyrox.aniverse.player.MiniPlayerManager.expanding) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) // mini player is flying in over this
                } else if (isStreamLoading && !adopting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CineStreamLoader(subtitle = if (currentProvider.isDirectStream) "Direct · HD" else null)
                    }
                } else if (!adopting && ((directStreamFailed && currentProvider.isDirectStream) || animeSaltFailed || animeSaltNoMapping || tmdbFailed)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Stream currently unavailable on ${currentProvider.name}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This episode or title is not available on this server. Try another server.",
                                color = Color(0xFFA1A1AA),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (currentProvider.isDirectStream) {
                                            isDirectStreamLoading = true
                                            directStreamFailed = false
                                        } else {
                                            animeSaltStreamResolved = false
                                            animeSaltStreamUrl = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val otherProvider = ALL_PROVIDERS.firstOrNull { it.id != currentProvider.id && (availableDirectServers.contains(it) || it.isEmbedOnly) }
                                if (otherProvider != null) {
                                    OutlinedButton(
                                        onClick = {
                                            currentProvider = otherProvider
                                            vm.setPreferredServer(otherProvider.id)
                                        },
                                        border = BorderStroke(1.dp, Color(0x40FFFFFF)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Switch to ${otherProvider.name}", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (!isCurrentEpDownloaded && vm.isPlayEmbed && (currentProvider.isEmbedOnly || !currentProvider.isDirectStream) && !playInZenkaiPlayer) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        com.nyyrox.aniverse.player.IframePlayerView(
                            embedUrl = activeStreamUrl,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { toggleFullscreen() },
                            onNextEpisode = {
                                if (currentEpisode < maxEpisode) {
                                    currentEpisode += 1
                                }
                            },
                            onStreamDetected = { detectedUrl, headers ->
                                sniffedStreamUrl = detectedUrl
                                sniffedStreamHeaders = headers
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    val combinedSubtitles = if (isPlayingOffline) {
                        (downloadedEpInfo?.subtitles ?: emptyList()).ifEmpty { externalSubtitles }
                    } else {
                        ((if (playInZenkaiPlayer && sniffedStreamSubs.isNotEmpty()) sniffedStreamSubs else activeDirectSubs) + externalSubtitles)
                            .distinctBy { it.url }
                            .sortedWith(
                                compareByDescending<com.nyyrox.aniverse.data.subtitles.SubtitleTrack> {
                                    it.source.startsWith("Direct") || it.source == "Anikoto"
                                }.thenByDescending {
                                    it.isDefault
                                }.thenByDescending {
                                    it.language.equals("English", ignoreCase = true)
                                }.thenBy {
                                    it.label
                                }
                            )
                    }
                    com.nyyrox.aniverse.player.CinePlayerView(
                        title = anime.title,
                        episode = playingEpisode,
                        episodeTitle = tmdbEpisodes[playingEpisode]?.name,
                        nextEpisodeTitle = tmdbEpisodes[playingEpisode + 1]?.name,
                        nextEpisodeImage = tmdbEpisodes[playingEpisode + 1]?.stillUrl?.takeIf { it.isNotBlank() }
                            ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                            ?: anime.banner?.takeIf { it.isNotBlank() }
                            ?: anime.cover,
                        streamUrl = if (playInZenkaiPlayer && !sniffedStreamUrl.isNullOrBlank()) sniffedStreamUrl!! else (adoptUrl.takeIf { adopting } ?: activeStreamUrl),
                        isDirectStream = if (playInZenkaiPlayer) true else isDirectStreamActive,
                        isDub = currentProvider.isDub || selectedCategory == "DUB",
                        headers = if (playInZenkaiPlayer && sniffedStreamHeaders.isNotEmpty()) sniffedStreamHeaders else activeDirectHeaders,
                        externalSubtitles = combinedSubtitles,
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { toggleFullscreen() },
                        onBack = handleExit,
                        onCollapseToMiniPlayer = handleExit,
                        onDragDownDelta = { dy ->
                            if (!isScreenExiting) {
                                val next = (dy / maxDragPx).coerceIn(0f, 1f)
                                coroutineScope.launch { dragProgress.snapTo(next) }
                            }
                        },
                        onDragDownRelease = { dy ->
                            if (!isScreenExiting) {
                                val releaseProg = (dy / maxDragPx).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    if (releaseProg > 0.35f) {
                                        isScreenExiting = true
                                        dragProgress.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                                        com.nyyrox.aniverse.player.MiniPlayerManager.startAtCollapsed = true
                                        com.nyyrox.aniverse.player.MiniPlayerManager.parkRequest =
                                            com.nyyrox.aniverse.player.MiniPlayerManager.ParkRequest(animeId, currentEpisode, currentProvider.id)
                                        com.nyyrox.aniverse.player.MiniPlayerManager.parkNow?.invoke()
                                        com.nyyrox.aniverse.player.MiniPlayerManager.fullScreenOpen = false
                                        com.nyyrox.aniverse.data.discord.DiscordRpcService.clearPresence()
                                        onBack()
                                    } else {
                                        dragProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        initialTimeMs = savedPos,
                        startPaused = false,
                        malId = anime.idMal,
                        anilistId = anime.id,
                        season = season,
                        maxEpisode = maxEpisode,
                        onSelectEpisode = { newEp ->
                            currentEpisode = newEp
                        },
                        hasPreviousEpisode = playingEpisode > 1,
                        onPreviousEpisode = {
                            if (currentEpisode > 1) {
                                currentEpisode -= 1
                            }
                        },
                        hasNextEpisode = playingEpisode < maxEpisode,
                        onNextEpisode = {
                            if (currentEpisode < maxEpisode) {
                                currentEpisode += 1
                            }
                        },
                        isAutoPlayNext = vm.isAutoPlayNext,
                        onToggleAutoPlayNext = { vm.setAutoPlayNext(it) },
                        isAutoSkipIntroOutro = vm.isAutoSkipIntroOutro,
                        onToggleAutoSkipIntroOutro = { vm.setAutoSkipIntroOutro(it) },
                        onProgressUpdate = { pos, dur ->
                            val epStill = tmdbEpisodes[playingEpisode]?.stillUrl
                                ?: vm.tmdbEpisodeStills["${anime.id}_$playingEpisode"]
                            vm.recordHistory(anime, playingEpisode, pos, dur, episodeStill = epStill)
                            if (!isScreenExiting) {
                                val coverUrl = anime.cover.takeIf { it.isNotBlank() } ?: anime.banner
                                com.nyyrox.aniverse.data.discord.DiscordRpcService.updatePresence(
                                    animeTitle = anime.title,
                                    episodeNum = playingEpisode,
                                    isDub = currentProvider.isDub,
                                    currentPositionMs = pos,
                                    durationMs = dur,
                                    isPlaying = true,
                                    coverUrl = coverUrl
                                )
                            }
                        },
                        onPlaybackFailed = {
                            // User explicitly requested: do NOT auto switch servers
                            android.util.Log.d("PlayerScreen", "Playback issue encountered; staying on selected server: ${currentProvider.name}")
                        },
                        availableServers = directStreamResult?.availableServers ?: emptyList(),
                        onSelectServer = { srv ->
                            manualSelectedServer = srv
                        }
                    )
                }
            }
        }

        // Below Video Section (Hidden in Fullscreen) — pure black & white, no gradients, no ambient blur
        if (!isFullscreen) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(playerBg)
                    .graphicsLayer {
                        if (progress > 0f) {
                            alpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
                            translationY = progress * with(density) { 50.dp.toPx() }
                            scaleX = 1f - progress * 0.08f
                            scaleY = 1f - progress * 0.08f
                        }
                    }
            ) {

                val currentEpInfo = tmdbEpisodes[currentEpisode]
                val currentEpTask = if (isCurrentEpDownloading) activeAnimeTasks["${anime.id}:$currentEpisode"] else null
                val isEmbedModeActive = !isCurrentEpDownloaded && vm.isPlayEmbed &&
                    (currentProvider.isEmbedOnly || !currentProvider.isDirectStream)
                val customEpName = currentEpInfo?.name?.takeIf {
                    it.isNotBlank() && !it.equals("Episode $currentEpisode", ignoreCase = true)
                }
                val isDirect = selectedSourceMode == "DIRECT"

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // ───────── Header: title, meta, actions ─────────
                    item(key = "header") {
                        val isDubActive = currentProvider.isDub || selectedCategory == "DUB"
                        val audioTag = when {
                            selectedCategory == "MULTI" -> "Multi audio"
                            isDubActive -> "English dub"
                            else -> "Japanese sub"
                        }
                        val metaLine = listOfNotNull(
                            anime.score?.let { "★ $it%" },
                            anime.format?.takeIf { it.isNotBlank() },
                            anime.duration?.let { "$it min" },
                            audioTag
                        ).joinToString("  ·  ")
                        val isCurrentFiller = fillerInfo.isFiller(currentEpisode)
                        val isCurrentMixed = fillerInfo.isMixed(currentEpisode)
                        val hasPrev = !isMovie && currentEpisode > 1
                        val hasNext = !isMovie && currentEpisode < maxEpisode
                        val dlQueued = currentEpTask?.status == DownloadStatus.QUEUED
                        val dlProgress = currentEpTask?.progress ?: 0f
                        val dlConnecting = currentEpTask != null && !dlQueued &&
                            (dlProgress <= 0.02f || currentEpTask.speedFormatted.contains("Connecting", ignoreCase = true))
                        val dlLabel = when {
                            currentEpTask != null && dlQueued -> "Queued"
                            currentEpTask != null && dlConnecting -> "Starting"
                            currentEpTask != null -> "${(dlProgress * 100).toInt()}%"
                            isCurrentEpDownloaded -> if (playOfflineMode) "Offline" else "Saved"
                            else -> "Download"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = if (isMovie) "MOVIE" else "NOW PLAYING  ·  EPISODE $currentEpisode OF $maxEpisode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp,
                                color = playerTextMuted
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = anime.displayTitle,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = playerTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isMovie) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = customEpName ?: "Episode $currentEpisode",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = playerTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                anime.score?.let { score ->
                                    MonochromeBadge(text = "★ $score%", isDark = isDark)
                                }
                                anime.format?.takeIf { it.isNotBlank() }?.let { fmt ->
                                    MonochromeBadge(text = fmt, isDark = isDark)
                                }
                                anime.duration?.let { dur ->
                                    MonochromeBadge(text = "${dur}m", isDark = isDark)
                                }
                                MonochromeBadge(text = audioTag, isDark = isDark)
                                if (isCurrentFiller) {
                                    MonochromeBadge(text = "FILLER", isDark = isDark, outlined = true)
                                } else if (isCurrentMixed) {
                                    MonochromeBadge(text = "MIXED", isDark = isDark, outlined = true)
                                }
                            }



                            // Embed mode Zenkai Player switch
                            if (isEmbedModeActive || playInZenkaiPlayer) {
                                Spacer(Modifier.height(18.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(playerSurfaceElevated)
                                        .border(1.dp, playerBorder, RoundedCornerShape(12.dp))
                                        .clickable { toggleZenkaiPlayer() }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (isSniffingStream) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = playerTextPrimary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (playInZenkaiPlayer) Icons.Default.OpenInBrowser else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = playerTextPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = when {
                                                    isSniffingStream -> "Extracting stream…"
                                                    playInZenkaiPlayer -> "Back to embed player"
                                                    else -> "Play in Zenkai Player"
                                                },
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = playerTextPrimary
                                            )
                                            Text(
                                                text = if (playInZenkaiPlayer) "Return to the web player" else "Native player · ad-free",
                                                fontSize = 11.5.sp,
                                                color = playerTextMuted
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = playerTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // ───────── 1. SERVERS (Compact, high-quality, not taking whole place) ─────────
                    item(key = "servers_card") {
                        val isAcdnSupported = isAcdnMapped
                        val hasDub = audioAvailability.hasDub

                        val servers = if (isDirect) {
                            when (selectedCategory) {
                                "DUB" -> availableDirectServers.filter { (it.isDub || it.isMulti) && (isAcdnSupported || !it.id.contains("animesalt")) }
                                "MULTI" -> availableDirectServers.filter { it.isMulti && (isAcdnSupported || !it.id.contains("animesalt")) }
                                else -> availableDirectServers.filter { !it.isDub && !it.isMulti }
                            }.ifEmpty { availableDirectServers.filter { isAcdnSupported || !it.id.contains("animesalt") } }
                        } else {
                            when (selectedCategory) {
                                "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub && (isAcdnSupported || !it.id.contains("animesalt")) }
                                "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti && (isAcdnSupported || !it.id.contains("animesalt")) }
                                else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti && (isAcdnSupported || !it.id.contains("animesalt")) }
                            }
                        }
                        val audioKeys = if (isDirect) {
                            if (hasDub) listOf("SUB", "DUB") else listOf("SUB")
                        } else {
                            if (hasDub) listOf("SUB", "DUB", "MULTI") else listOf("SUB", "MULTI")
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .padding(top = 10.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0xFF141418) else Color(0xFFF1F5F9))
                                .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x18000000), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Header Row: "SERVERS" label + active indicator + Audio & Source toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                var serverDropdownExpanded by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Server",
                                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "SERVERS",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        color = playerTextPrimary
                                    )
                                    // Top dropdown selector for active server
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF222228) else Color(0xFFE2E8F0))
                                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(8.dp))
                                                .clickable { serverDropdownExpanded = !serverDropdownExpanded }
                                                .padding(horizontal = 8.dp, vertical = 3.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) Color(0xFF34D399) else Color(0xFF059669))
                                            )
                                            Text(
                                                text = currentProvider.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = playerTextPrimary
                                            )
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = playerTextMuted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = serverDropdownExpanded,
                                            onDismissRequest = { serverDropdownExpanded = false },
                                            modifier = Modifier.background(if (isDark) Color(0xFF18181D) else Color.White)
                                        ) {
                                            servers.forEach { prov ->
                                                val isCur = prov.id == currentProvider.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isCur) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else Color(0xFF71717A))
                                                            )
                                                            Text(
                                                                text = prov.name,
                                                                fontSize = 13.sp,
                                                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isCur) playerTextPrimary else playerTextMuted
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                        currentProvider = prov
                                                        vm.setPreferredServer(prov.id)
                                                        serverDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Audio tabs & Source toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Source mode toggle (Direct HD vs Embed)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF222228) else Color(0xFFE2E8F0))
                                            .border(1.dp, if (isDirect) (if (isDark) Color(0x44FFFFFF) else Color(0x33000000)) else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectSourceMode(if (isDirect) "EMBED" else "DIRECT") }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isDirect) "Direct HD" else "Embed",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDirect) playerTextPrimary else playerTextMuted
                                        )
                                    }

                                    // Audio category tabs (SUB / DUB)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0xFF222228) else Color(0xFFE2E8F0))
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        audioKeys.forEach { key ->
                                            val isCatSelected = selectedCategory == key
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isCatSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent)
                                                    .clickable { selectCategory(key) }
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isCatSelected) FontWeight.Black else FontWeight.SemiBold,
                                                    color = if (isCatSelected) (if (isDark) Color.Black else Color.White) else playerTextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Horizontal Server Chips (High-quality, compact, scrollable)
                            if (servers.isEmpty()) {
                                Text(
                                    text = "No servers available for this audio category.",
                                    fontSize = 12.sp,
                                    color = playerTextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(servers, key = { it.id }) { prov ->
                                        val isSelected = prov.id == currentProvider.id
                                        val activeBg = if (isDark) Color.White else Color(0xFF0F172A)
                                        val activeFg = if (isDark) Color.Black else Color.White
                                        val inactiveBg = if (isDark) Color(0xFF1C1C22) else Color(0xFFE2E8F0)
                                        val activeBorder = if (isDark) Color.White else Color(0xFF0F172A)
                                        val inactiveBorder = if (isDark) Color(0x33FFFFFF) else Color(0x18000000)
                                        val badgeText = prov.serverBadge.replace("Direct HD", "HD").replace("Direct", "HD")

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) activeBg else inactiveBg)
                                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) activeBorder else inactiveBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                    currentProvider = prov
                                                    vm.setPreferredServer(prov.id)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) (if (isDark) Color(0xFF10B981) else Color(0xFF059669)) else Color(0xFF71717A))
                                            )
                                            Text(
                                                text = prov.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) activeFg else playerTextPrimary,
                                                maxLines = 1
                                            )
                                            if (badgeText.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isSelected) (if (isDark) Color(0x22000000) else Color(0x33FFFFFF)) else (if (isDark) Color(0x33FFFFFF) else Color(0x18000000)))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isSelected) activeFg else playerTextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Offline notice if available
                            if (isPlayingOffline) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.DownloadDone, contentDescription = null, tint = playerTextPrimary, modifier = Modifier.size(14.dp))
                                        Text("Playing offline copy", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = playerTextPrimary)
                                    }
                                    Text(
                                        text = "Switch to online",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = playerTextPrimary,
                                        modifier = Modifier.clickable { playOfflineMode = false }
                                    )
                                }
                            }
                        }
                    }

                    // ───────── 2. EPISODES DOWN BELOW ─────────
                    if (!isMovie) {
                        val curStart = selectedPage * pageSize + 1
                        val curEnd = minOf(curStart + pageSize - 1, maxEpisode)
                        val episodeNumbers = (curStart..curEnd).toList()

                        item(key = "ep-toolbar") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(top = 10.dp, bottom = 8.dp)
                            ) {
                                if (pageCount > 1) {
                                    val rangeLabels = remember(pageCount, pageSize, maxEpisode) {
                                        List(pageCount) { p ->
                                            val s = p * pageSize + 1
                                            "$s–${minOf(s + pageSize - 1, maxEpisode)}"
                                        }
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(pageCount) { page ->
                                            val selected = selectedPage == page
                                            val activeBg = if (isDark) Color.White else Color.Black
                                            val activeFg = if (isDark) Color.Black else Color.White
                                            val inactiveBg = playerSurfaceElevated
                                            val inactiveFg = playerTextSecondary
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(if (selected) activeBg else inactiveBg)
                                                    .border(1.dp, if (selected) activeBg else playerBorder, CircleShape)
                                                    .clickable { selectedPage = page }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = rangeLabels[page],
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) activeFg else inactiveFg
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Episodes",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = playerTextPrimary
                                        )
                                        Text(
                                            text = "$curStart–$curEnd of $maxEpisode",
                                            fontSize = 12.sp,
                                            color = playerTextMuted
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(playerSurfaceElevated)
                                            .border(1.dp, playerBorder, CircleShape)
                                            .clickable { showDownloadRangeSheet = curStart..curEnd }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = playerTextPrimary, modifier = Modifier.size(15.dp))
                                            Text(
                                                text = "Download all",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = playerTextPrimary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(playerSurfaceElevated)
                                            .border(1.dp, playerBorder, CircleShape)
                                            .clickable { isEpisodeGridView = !isEpisodeGridView },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                                            contentDescription = "Toggle episode view",
                                            tint = playerTextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (isEpisodeGridView) {
                            item(key = "ep-grid") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    episodeNumbers.chunked(5).forEach { rowEps ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowEps.forEach { epNum ->
                                                val isCurrent = epNum == currentEpisode
                                                val isEpFiller = fillerInfo.isFiller(epNum)
                                                val isEpMixed = fillerInfo.isMixed(epNum)
                                                val activeBg = if (isDark) Color.White else Color.Black
                                                val activeFg = if (isDark) Color.Black else Color.White
                                                val inactiveBg = playerSurfaceElevated
                                                val inactiveFg = playerTextPrimary
                                                val borderColor = if (isCurrent) activeBg else playerBorder
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isCurrent) activeBg else inactiveBg)
                                                        .border(if (isCurrent) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                            currentEpisode = epNum
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$epNum",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCurrent) activeFg else inactiveFg
                                                    )
                                                    if (isEpFiller || isEpMixed) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(5.dp)
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isCurrent) activeFg else playerTextMuted)
                                                        )
                                                    }
                                                }
                                            }
                                            repeat(5 - rowEps.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(episodeNumbers, key = { it }) { epNum ->
                                val epStillInfo = tmdbEpisodes[epNum]
                                val isEpDl = remember(anime.id, epNum, downloadedAnimeList) {
                                    vm.isAnimeEpisodeDownloaded(anime.id, epNum)
                                }
                                val isEpDling = remember(anime.id, epNum, activeAnimeTasks) {
                                    activeAnimeTasks.containsKey("${anime.id}:$epNum")
                                }
                                PlayerEpisodeRow(
                                    episode = epNum,
                                    title = epStillInfo?.name,
                                    still = epStillInfo?.stillUrl?.takeIf { it.isNotBlank() }
                                        ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                                        ?: anime.banner?.takeIf { it.isNotBlank() }
                                        ?: anime.cover,
                                    durationLabel = anime.duration?.let { "$it min" } ?: "24 min",
                                    isSelected = epNum == currentEpisode,
                                    isDownloaded = isEpDl,
                                    isDownloading = isEpDling,
                                    downloadProgress = activeAnimeTasks["${anime.id}:$epNum"]?.progress,
                                    isFiller = fillerInfo.isFiller(epNum),
                                    isMixed = fillerInfo.isMixed(epNum),
                                    onDownload = {
                                        if (!isEpDl && !isEpDling) {
                                            showDownloadSheetEpisode = epNum to epStillInfo?.name
                                        } else if (isEpDl) {
                                            android.widget.Toast.makeText(context, "Ep $epNum is already downloaded", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onClick = {
                                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                        currentEpisode = epNum
                                    },
                                    overview = epStillInfo?.overview?.takeIf { it.isNotBlank() }
                                )
                            }
                        }
                    }

                    // ───────── 3. OVERVIEW SECTION ─────────
                    item(key = "overview_section") {
                        Spacer(Modifier.height(14.dp))
                        PlayerOverview(
                            anime = anime,
                            episode = currentEpisode,
                            episodeName = customEpName,
                            episodeOverview = currentEpInfo?.overview,
                            isMovie = isMovie,
                            maxEpisode = maxEpisode
                        )
                    }
                }
            }
        }
    }
}

    showDownloadSheetEpisode?.let { (ep, epTitle) ->
        com.nyyrox.aniverse.ui.components.DownloadAnimeOptionsSheet(
            anime = anime,
            episode = ep,
            episodeTitle = epTitle,
            initialProvider = currentProvider,
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
                android.widget.Toast.makeText(context, "Downloading Ep $ep ($quality • $audio)...", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    showDownloadRangeSheet?.let { range ->
        com.nyyrox.aniverse.ui.components.DownloadAnimeOptionsSheet(
            anime = anime,
            episodesRange = range,
            initialProvider = currentProvider,
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
                    "Downloading ${eps.size} episodes (${eps.first()}–${eps.last()}) on ${prov.name} ($quality • $audio)...",
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
                    Text("Download Stopped", fontWeight = FontWeight.Bold, color = c.textPrimary)
                }
            },
            text = {
                Text(
                    text = failedTask.error ?: "Download stopped on ${failedTask.providerName}. Would you like to retry or change server?",
                    color = c.textSecondary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry (${failedTask.providerName})", color = c.onAccent, fontWeight = FontWeight.Bold)
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
                        Text("Change Server", color = c.textPrimary)
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
                        Text("Dismiss", color = c.textSecondary)
                    }
                }
            },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GlassmorphicControlChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeAccent: Color = Color.White
) {
    val bgColor = if (isActive) Color.White else Color(0x18FFFFFF)
    val borderColor = if (isActive) Color.White else Color(0x28FFFFFF)
    val contentColor = if (isActive) Color.Black else Color(0xFFCAD5E2)

    Row(
        modifier = modifier
            .clip(OttPillShape)
            .background(bgColor)
            .border(1.dp, borderColor, OttPillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(4.dp))

        // Clean indicator dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.Black else Color(0x35FFFFFF))
        )
    }
}


// ───────── Player UI helpers (theme-aware: LocalAppColors + LocalIsDark, light and dark) ─────────

/**
 * Glass surface kept for other screens. [backdrop] == null means glass is off: a flat themed surface is drawn instead.
 * The player page itself no longer wraps content in this (no cards); it's only for callers that still want a panel.
 */
@Composable
fun Modifier.playerGlass(backdrop: PrismalGlassLayer?, radius: Dp = 22.dp): Modifier {
    val c = LocalAppColors.current
    val isDark = LocalIsDark.current
    val shape = RoundedCornerShape(radius)
    if (backdrop == null) {
        return this
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.glass.border, shape)
    }
    val density = LocalDensity.current
    val effects = remember(density) { prismalGlassEffects(density) }
    val scrim = if (isDark) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.5f)
    return this.drawPrismalGlass(
        backdrop = backdrop,
        shape = { PrismalRoundedRectangle(radius) },
        effects = effects,
        onDrawSurface = { drawRect(scrim) }
    )
}

/**
 * PlayerAmbientArt kept as a no-op to eliminate gradients, blurred artwork blobs, and color bleeding.
 */
@Composable
fun PlayerAmbientArt(layer: PrismalGlassLayer?, model: Any?, modifier: Modifier = Modifier) {
    // Intentionally no-op: pure black & white design with no artwork blur or gradients
}

/** Glass pill button (circle when [circle]); flat pill with spring press when glass is off. [active] = solid accent. */
@Composable
fun PlayerGlassButton(
    backdrop: PrismalGlassLayer?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    circle: Boolean = false,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = LocalIsDark.current
    val sized = if (circle) modifier.width(height) else modifier
    val pad = if (circle) PaddingValues(0.dp) else PaddingValues(horizontal = 14.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "btnPress"
    )
    val activeBg = if (isDark) Color.White else Color.Black
    val inactiveBg = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val borderCol = if (active) activeBg else (if (isDark) Color(0x26FFFFFF) else Color(0x18000000))
    Row(
        modifier = sized
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (active) activeBg else inactiveBg)
            .border(1.dp, borderCol, CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(pad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        content = content
    )
}

/** Round tactile action button with a caption underneath. High contrast monochrome style. */
@Composable
private fun PlayerMonochromeAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    isDark: Boolean = true,
    icon: @Composable (Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "btnScale"
    )

    val activeBg = if (isDark) Color.White else Color.Black
    val activeFg = if (isDark) Color.Black else Color.White
    val inactiveBg = if (isDark) Color(0xFF141416) else Color(0xFFF4F4F5)
    val inactiveFg = if (isDark) Color.White else Color.Black
    val borderColor = if (active) activeBg else (if (isDark) Color(0x26FFFFFF) else Color(0x1C000000))

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.3f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(if (active) activeBg else inactiveBg)
                .border(1.dp, borderColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            icon(if (active) activeFg else inactiveFg)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) (if (isDark) Color.White else Color.Black) else (if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PlayerAction(
    label: String,
    backdrop: PrismalGlassLayer?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val isDark = LocalIsDark.current
    PlayerMonochromeAction(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        active = active,
        isDark = isDark,
        icon = icon
    )
}

/** Crisp monochrome metadata badge. */
@Composable
private fun MonochromeBadge(
    text: String,
    isDark: Boolean,
    outlined: Boolean = false
) {
    val bg = if (outlined) Color.Transparent else (if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
    val fg = if (isDark) Color(0xFFE4E4E7) else Color(0xFF27272A)
    val border = if (isDark) Color(0x28FFFFFF) else Color(0x20000000)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.8.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = fg,
            maxLines = 1
        )
    }
}

/** Pure black & white segmented control. */
@Composable
private fun MonochromeSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp
) {
    val containerBg = if (isDark) Color(0xFF121214) else Color(0xFFF4F4F5)
    val containerBorder = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
    val activeBg = if (isDark) Color.White else Color.Black
    val activeFg = if (isDark) Color.Black else Color.White
    val inactiveFg = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)

    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(22.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(19.dp))
                    .background(if (selected) activeBg else Color.Transparent)
                    .clickable { onSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) activeFg else inactiveFg,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlayerSegmented(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: PrismalGlassLayer?,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    val isDark = LocalIsDark.current
    MonochromeSegmentedControl(
        labels = labels,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        isDark = isDark,
        modifier = modifier,
        height = height
    )
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    val isDark = LocalIsDark.current
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x14000000))
    )
}

@Composable
private fun SectionLabel(text: String) {
    val isDark = LocalIsDark.current
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        color = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    )
}

/** Single-line chip. */
@Composable
fun TagChip(
    text: String,
    fg: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    dot: Boolean = false
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(0.8.dp, border, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (dot) Box(Modifier.size(5.dp).clip(CircleShape).background(fg))
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Monochrome filler/mixed tag. No gradients or colors. */
@Composable
fun FillerTag(isMixed: Boolean, modifier: Modifier = Modifier) {
    val isDark = LocalIsDark.current
    val fg = if (isDark) Color.White else Color.Black
    val border = if (isDark) Color(0x35FFFFFF) else Color(0x25000000)
    if (isMixed) {
        TagChip("MIXED", fg, Color.Transparent, border, modifier, dot = true)
    } else {
        TagChip("FILLER", fg, Color.Transparent, border, modifier, dot = true)
    }
}

@Composable
private fun Equalizer(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(420, 560, 360).forEachIndexed { i, ms ->
            val h by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Flat episode row: 16:9 thumbnail, title, overview, tags and a trailing download state,
 * separated by a hairline. Zero gradients, pure black and white.
 */
@Composable
fun PlayerEpisodeRow(
    episode: Int,
    title: String?,
    still: Any?,
    durationLabel: String,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isFiller: Boolean,
    isMixed: Boolean,
    onDownload: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: Boolean = true,
    overview: String? = null
) {
    val isDark = LocalIsDark.current
    val textPrimary = if (isDark) Color.White else Color(0xFF09090B)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val textMuted = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    val borderCol = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) (if (isDark) Color(0x18FFFFFF) else Color(0x0A000000)) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(152.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF18181B) else Color(0xFFE4E4E7))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) (if (isDark) Color.White else Color.Black) else borderCol,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImage(
                    model = still,
                    contentDescription = "Episode $episode",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$episode",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (isSelected) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Equalizer(Color.White, Modifier.width(14.dp))
                    }
                }
                if (isDownloading && downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.5.dp),
                        color = Color.White,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title?.takeIf { it.isNotBlank() } ?: "Episode $episode",
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ep $episode · $durationLabel",
                        modifier = Modifier.padding(vertical = 2.dp),
                        fontSize = 11.5.sp,
                        color = textMuted
                    )
                    if (isSelected) {
                        TagChip(
                            text = "NOW PLAYING",
                            fg = if (isDark) Color.Black else Color.White,
                            bg = if (isDark) Color.White else Color.Black,
                            border = if (isDark) Color.White else Color.Black
                        )
                    }
                    if (isFiller) {
                        TagChip(
                            text = "FILLER",
                            fg = textPrimary,
                            bg = Color.Transparent,
                            border = borderCol
                        )
                    } else if (isMixed) {
                        TagChip(
                            text = "MIXED",
                            fg = textPrimary,
                            bg = Color.Transparent,
                            border = borderCol
                        )
                    }
                }
                if (!overview.isNullOrBlank()) {
                    Text(
                        text = overview,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDownload),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDownloaded -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    isDownloading -> CircularProgressIndicator(
                        progress = { (downloadProgress ?: 0f).coerceIn(0.05f, 1f) },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = textPrimary,
                        trackColor = borderCol
                    )
                    else -> Icon(
                        Icons.Default.Download,
                        contentDescription = "Download episode $episode",
                        tint = textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Hairline(Modifier.padding(start = 18.dp))
    }
}

/** Overview tab: what's playing, the show's facts, genres and synopsis. Pure monochrome styling. */
@Composable
private fun PlayerOverview(
    anime: Anime,
    episode: Int,
    episodeName: String?,
    episodeOverview: String?,
    isMovie: Boolean,
    maxEpisode: Int
) {
    val isDark = LocalIsDark.current
    val textPrimary = if (isDark) Color.White else Color(0xFF09090B)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val textMuted = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    val borderCol = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
    val surfaceElevated = if (isDark) Color(0xFF18181B) else Color(0xFFE4E4E7)

    val about = remember(anime.description) {
        anime.description?.replace(Regex("<[^>]*>"), "")?.trim()?.takeIf { it.isNotBlank() }
    }
    var aboutExpanded by remember(anime.id) { mutableStateOf(false) }
    var epExpanded by remember(episode) { mutableStateOf(false) }
    val genres = anime.genres.orEmpty()
    val statusText = anime.status?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
    val facts = listOfNotNull(
        anime.format?.takeIf { it.isNotBlank() }?.let { "Format" to it },
        statusText?.takeIf { it.isNotBlank() }?.let { "Status" to it },
        if (isMovie) null else maxEpisode.takeIf { it > 0 }?.let { "Episodes" to "$it" },
        anime.duration?.let { "Duration" to "$it min" },
        anime.seasonYear?.let { "Year" to "$it" },
        anime.score?.let { "Score" to "$it%" }
    )
    val romaji = anime.romajiTitle?.takeIf { it.isNotBlank() && !it.equals(anime.title, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 10.dp)
    ) {
        // Currently playing episode
        if (!isMovie && !episodeOverview.isNullOrBlank()) {
            SectionLabel("EPISODE $episode")
            Spacer(Modifier.height(8.dp))
            if (!episodeName.isNullOrBlank()) {
                Text(
                    text = episodeName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = episodeOverview,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = textSecondary,
                maxLines = if (epExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            if (episodeOverview.length > 170) {
                Text(
                    text = if (epExpanded) "Show less" else "Read more",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { epExpanded = !epExpanded }
                        .padding(vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
        }

        // Poster + facts
        SectionLabel(if (isMovie) "ABOUT THE MOVIE" else "ABOUT THE SERIES")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(surfaceElevated)
                    .border(1.dp, borderCol, RoundedCornerShape(10.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (romaji != null) {
                    Text(
                        text = romaji,
                        fontSize = 12.5.sp,
                        color = textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                facts.forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = label,
                            modifier = Modifier.width(66.dp),
                            fontSize = 12.5.sp,
                            color = textMuted
                        )
                        Text(
                            text = value,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Genres (outlined, monochrome)
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    Text(
                        text = genre,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(1.dp, borderCol, CircleShape)
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Synopsis
        if (about != null) {
            Spacer(Modifier.height(22.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            SectionLabel("SYNOPSIS")
            Spacer(Modifier.height(8.dp))
            Text(
                text = about,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = textSecondary,
                maxLines = if (aboutExpanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis
            )
            if (about.length > 260) {
                Text(
                    text = if (aboutExpanded) "Show less" else "Read more",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { aboutExpanded = !aboutExpanded }
                        .padding(vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** One row of the server list: radio dot, name, badge. Flat, separated by hairlines. */
@Composable
private fun ServerRow(name: String, badge: String, selected: Boolean, onClick: () -> Unit) {
    val isDark = LocalIsDark.current
    val textPrimary = if (isDark) Color.White else Color(0xFF09090B)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val borderCol = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (selected) (if (isDark) Color.White else Color.Black) else borderCol,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White else Color.Black)
                    )
                }
            }
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) textPrimary else textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TagChip(badge, textSecondary, Color.Transparent, borderCol)
        }
        Hairline()
    }
}
