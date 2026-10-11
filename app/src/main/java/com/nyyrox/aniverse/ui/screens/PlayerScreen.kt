package com.nyyrox.aniverse.ui.screens

import com.nyyrox.aniverse.player.CineStreamLoader
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
import com.nyyrox.aniverse.ui.components.OttEpisodeCardItem
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    vm: MainViewModel,
    animeId: Int,
    initialEpisode: Int,
    initialProviderId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
                .background(OttBg),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 8.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(OttSurfaceElevated)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
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
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please check if the requested show is missing an AniList ID on the server. If we made a mistake, please contact us in the Community tab for fixes!",
                        color = OttTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = OttPillShape
                    ) {
                        Text("Go Back", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                CircularProgressIndicator(color = Color.White)
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
        ?: com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders("sub").first()
    var currentProvider by remember(animeId, initialProviderId) { mutableStateOf(initialProv) }
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
        mutableStateOf(com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory))
    }

    LaunchedEffect(anime.id, selectedCategory, currentEpisode) {
        try {
            val servers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getAvailableDirectProviders(
                anilistId = anime.id,
                audioMode = selectedCategory,
                episode = currentEpisode
            )
            if (servers.isNotEmpty()) {
                availableDirectServers = servers
                // If in DIRECT mode and current provider is not in the available list, smoothly select the first available
                if (selectedSourceMode == "DIRECT" && servers.none { it.id == currentProvider.id }) {
                    currentProvider = servers.first()
                    vm.setPreferredServer(servers.first().id)
                }
            } else {
                availableDirectServers = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory)
            }
        } catch (_: Exception) {}
    }

    fun switchToNextServer() {
        val currentList = if (selectedSourceMode == "DIRECT") {
            availableDirectServers
        } else {
            when (selectedCategory) {
                "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub }
                "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti }
                else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti }
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
    val animeSaltNoMapping = animeSaltRelChecked && animeSaltRel == null &&
            currentProvider.id.startsWith("animesalt")

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

    val progress = dragProgress.value
    val bgAlpha = if (!isFullscreen) (1f - progress * 1.6f).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else OttBg.copy(alpha = bgAlpha))
            .then(if (!isFullscreen) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                                tint = Color(0xFFE50914),
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
                                color = Color.White.copy(alpha = 0.65f),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
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

        // Below Video Section (Hidden in Fullscreen)
        if (!isFullscreen) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (progress > 0f) {
                            alpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
                            translationY = progress * with(density) { 50.dp.toPx() }
                            scaleX = 1f - progress * 0.08f
                            scaleY = 1f - progress * 0.08f
                        }
                    }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Zenkai Player / Embed Mode Toggle Bar (Option down below the player with proper spacing)
                val isEmbedModeActive = !isCurrentEpDownloaded && vm.isPlayEmbed && (currentProvider.isEmbedOnly || !currentProvider.isDirectStream)
                if (isEmbedModeActive || playInZenkaiPlayer) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (playInZenkaiPlayer) Color(0xFF161B26) else Color(0xFF121622))
                                .border(
                                    1.dp,
                                    if (playInZenkaiPlayer) OttPrimary.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                    if (playInZenkaiPlayer) {
                                        playInZenkaiPlayer = false
                                    } else {
                                        if (!sniffedStreamUrl.isNullOrBlank()) {
                                            playInZenkaiPlayer = true
                                        } else {
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
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Direct stream could not be extracted",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } catch (_: Exception) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Extraction failed",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } finally {
                                                    isSniffingStream = false
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSniffingStream) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = OttPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extracting Stream for Zenkai Player...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                } else if (playInZenkaiPlayer) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        tint = OttPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Switch back to Embed Mode",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Return to Web Player)",
                                        fontSize = 11.sp,
                                        color = OttTextMuted
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = OttPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Play in Zenkai Player",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(Native Player & Ad-Free)",
                                        fontSize = 11.sp,
                                        color = OttTextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // 0. Active Episode Details & Header (Spacious, modern OTT design)
                item {
                    val currentEpInfo = tmdbEpisodes[currentEpisode]
                    val currentEpTask = if (isCurrentEpDownloading) activeAnimeTasks["${anime.id}:$currentEpisode"] else null

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Anime Title & Subtitle Info (Single, clean top header)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = anime.displayTitle,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = OttTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 24.sp
                            )

                            val subtitleText = if (isMovie) {
                                (anime.format ?: "Movie") + (anime.duration?.let { " • $it min" } ?: "") + " • ${currentProvider.name}"
                            } else {
                                (anime.format ?: "TV Series") + (anime.duration?.let { " • $it min" } ?: " • 24 min") + (anime.score?.let { " • ★ $it%" } ?: "") + " • ${currentProvider.name}"
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = OttTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 2. The Video Showcase Card: Image on left, Details & Episode Info properly beside it!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F121C))
                                .border(0.8.dp, Color(0x20FFFFFF), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // High-Res Episode Still with 16:9 Aspect Ratio
                            Box(
                                modifier = Modifier
                                    .width(136.dp)
                                    .aspectRatio(16f / 9.5f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = currentEpInfo?.stillUrl?.takeIf { it.isNotBlank() }
                                        ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                                        ?: anime.banner?.takeIf { it.isNotBlank() }
                                        ?: anime.cover,
                                    contentDescription = "Episode $currentEpisode",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(5.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isMovie) "MOVIE" else "HD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Details of the video properly beside the image!
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Episode Title (Shown ONCE here!)
                                val customEpName = currentEpInfo?.name?.takeIf { it.isNotBlank() && !it.equals("Episode $currentEpisode", ignoreCase = true) }
                                val epDisplayHeading = when {
                                    isMovie -> anime.title
                                    customEpName != null -> "$currentEpisode. $customEpName"
                                    else -> "Episode $currentEpisode"
                                }
                                Text(
                                    text = epDisplayHeading,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Badges row beside the image
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    // Episode Number badge
                                    if (!isMovie) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "EP $currentEpisode / $maxEpisode",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Audio Badge
                                    val audioLabel = if (currentProvider.isDub || selectedCategory == "DUB") "DUB" else "SUB"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0x18FFFFFF))
                                            .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(5.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = audioLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }

                                    // Filler Badge
                                    val isCurrentFiller = fillerInfo.isFiller(currentEpisode)
                                    val isCurrentMixed = fillerInfo.isMixed(currentEpisode)
                                    if (isCurrentFiller || isCurrentMixed) {
                                        EpisodeFillerBadge(isMixed = isCurrentMixed)
                                    }

                                    // Embed Toggle Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(if (vm.isPlayEmbed) Color.White.copy(alpha = 0.2f) else Color(0x18FFFFFF))
                                            .border(0.8.dp, if (vm.isPlayEmbed) Color.White else Color(0x28FFFFFF), OttPillShape)
                                            .clickable {
                                                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                val targetEmbed = !vm.isPlayEmbed
                                                vm.setPlayEmbed(targetEmbed)
                                                if (targetEmbed) {
                                                    selectedSourceMode = "EMBED"
                                                    val embedList = when (selectedCategory) {
                                                        "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub }
                                                        "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti }
                                                        else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti }
                                                    }
                                                    val prov = embedList.firstOrNull() ?: ALL_PROVIDERS.first { it.isEmbedOnly }
                                                    currentProvider = prov
                                                    vm.setPreferredServer(prov.id)
                                                } else {
                                                    selectedSourceMode = "DIRECT"
                                                    if (selectedCategory == "MULTI") selectedCategory = "SUB"
                                                    val nextDirect = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory)
                                                    availableDirectServers = nextDirect
                                                    val prov = nextDirect.first()
                                                    currentProvider = prov
                                                    vm.setPreferredServer(prov.id)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (vm.isPlayEmbed) "Embed" else "Direct",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Synopsis text
                                val synopsis = currentEpInfo?.overview?.takeIf { it.isNotBlank() }
                                    ?: "Stream episode in HD. Select download or toggle servers below."
                                Text(
                                    text = synopsis,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Real-time Download Action Container
                                if (isCurrentEpDownloading && currentEpTask != null) {
                                    val dlProgress = currentEpTask.progress
                                    val percentInt = (dlProgress * 100).toInt()
                                    val isQueued = currentEpTask.status == DownloadStatus.QUEUED
                                    val isConnecting = !isQueued && (dlProgress <= 0.02f || currentEpTask.speedFormatted.contains("Connecting", ignoreCase = true))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x22FFFFFF))
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when {
                                                    isQueued -> "Queued"
                                                    isConnecting -> "Connecting..."
                                                    else -> "Downloading: $percentInt%"
                                                },
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isQueued) Color(0xFFFBBF24) else Color.White
                                            )
                                            if (currentEpTask.speedFormatted.isNotBlank() && !isQueued) {
                                                Text(
                                                    text = currentEpTask.speedFormatted,
                                                    fontSize = 9.sp,
                                                    color = OttTextSecondary
                                                )
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { if (dlProgress > 0f) dlProgress else 0.05f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = OttPrimary,
                                            trackColor = Color(0x33FFFFFF)
                                        )
                                    }
                                } else if (isCurrentEpDownloaded) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1A10B981))
                                            .border(0.8.dp, Color(0x4010B981), RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (!playOfflineMode) playOfflineMode = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (playOfflineMode) "Playing Offline" else "Play Offline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x18FFFFFF))
                                            .border(0.8.dp, Color(0x30FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable {
                                                showDownloadSheetEpisode = currentEpisode to currentEpInfo?.name
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Episode",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Download",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Previous / Next Fast Navigator Controls (Shown ONLY in EMBED mode)
                val isEmbedPlayerActive = !isCurrentEpDownloaded && vm.isPlayEmbed && (currentProvider.isEmbedOnly || !currentProvider.isDirectStream)
                if (isEmbedPlayerActive) {
                    item {
                        val hasPrev = currentEpisode > 1
                        val hasNext = currentEpisode < maxEpisode

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Previous Episode Card
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(OttCardShape)
                                    .background(if (hasPrev) OttSurface else Color(0x3312151E))
                                    .border(1.dp, if (hasPrev) OttSurfaceBorder else Color.Transparent, OttCardShape)
                                    .clickable(enabled = hasPrev) {
                                        if (currentEpisode > 1) {
                                            currentEpisode -= 1
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = if (hasPrev) OttTextPrimary else OttTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = "PREVIOUS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OttTextMuted
                                    )
                                    Text(
                                        text = if (hasPrev) "Episode ${currentEpisode - 1}" else "First Episode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasPrev) OttTextPrimary else OttTextMuted
                                    )
                                }
                            }

                            // Next Episode Card
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(OttCardShape)
                                    .background(if (hasNext) OttSurface else Color(0x3312151E))
                                    .border(1.dp, if (hasNext) OttSurfaceBorder else Color.Transparent, OttCardShape)
                                    .clickable(enabled = hasNext) {
                                        currentEpisode += 1
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "NEXT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (hasNext) "Episode ${currentEpisode + 1}" else "Latest Episode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasNext) OttTextPrimary else OttTextMuted
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (hasNext) Color.White else OttTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. SERVER SECTION (UP) - Streaming Server & Audio Controls (Glassmorphic Container)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(OttCardShape)
                            .background(Color(0xFF0F1117))
                            .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                            .padding(14.dp)
                    ) {
                        if (isPlayingOffline) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "PLAYING DOWNLOADED VIDEO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Offline Stream • ${downloadedEpInfo?.providerName?.ifBlank { "Local" } ?: "Local"} · Local Storage",
                                        fontSize = 10.sp,
                                        color = OttTextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(OttPillShape)
                                        .background(Color(0x18FFFFFF))
                                        .border(1.dp, Color(0x33FFFFFF), OttPillShape)
                                        .clickable { playOfflineMode = false }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Switch to Online",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            if (isCurrentEpDownloaded && downloadedEpInfo != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x18FFFFFF))
                                        .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { playOfflineMode = true }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Offline download available (${downloadedEpInfo?.providerName?.ifBlank { "Local" } ?: "Local"})",
                                        fontSize = 10.5.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Switch to Offline ➔",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Column {
                                Text(
                                    text = "STREAMING SERVERS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = OttTextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (selectedSourceMode == "DIRECT") "Native Direct Playback" else "Web Embed Sources",
                                    fontSize = 10.sp,
                                    color = OttTextMuted
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // DIRECT vs EMBED Clean Mode Toggle Pill
                            Row(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x22FFFFFF), OttPillShape)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf("DIRECT" to "DIRECT", "EMBED" to "EMBED").forEach { (modeKey, label) ->
                                    val isModeSelected = selectedSourceMode == modeKey
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(if (isModeSelected) Color.White else Color.Transparent)
                                            .clickable {
                                                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                if (selectedSourceMode != modeKey) {
                                                    selectedSourceMode = modeKey
                                                    if (modeKey == "DIRECT") {
                                                        vm.setPlayEmbed(false)
                                                        if (selectedCategory == "MULTI") {
                                                            selectedCategory = "SUB"
                                                        }
                                                        val nextDirect = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedCategory)
                                                        availableDirectServers = nextDirect
                                                        val prov = nextDirect.first()
                                                        currentProvider = prov
                                                        vm.setPreferredServer(prov.id)
                                                    } else {
                                                        vm.setPlayEmbed(true)
                                                        val embedList = when (selectedCategory) {
                                                            "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub }
                                                            "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti }
                                                            else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti }
                                                        }
                                                        val prov = embedList.firstOrNull() ?: ALL_PROVIDERS.first { it.isEmbedOnly }
                                                        currentProvider = prov
                                                        vm.setPreferredServer(prov.id)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isModeSelected) Color.Black else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // SUB / DUB / MULTI-AUDIO Selection Bar (Direct mode only shows SUB / DUB)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x14FFFFFF))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val audioCategories = if (selectedSourceMode == "DIRECT") {
                                listOf(
                                    "SUB" to "SUB",
                                    "DUB" to "DUB"
                                )
                            } else {
                                listOf(
                                    "SUB" to "SUB",
                                    "DUB" to "DUB",
                                    "MULTI" to "MULTI-AUDIO"
                                )
                            }
                            audioCategories.forEach { (catKey, label) ->
                                val isCatSelected = selectedCategory == catKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCatSelected) Color.White else Color.Transparent)
                                        .clickable {
                                            com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                            if (selectedCategory != catKey) {
                                                selectedCategory = catKey
                                                if (selectedSourceMode == "DIRECT") {
                                                    val nextDirect = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(catKey)
                                                    availableDirectServers = nextDirect
                                                    val newProv = nextDirect.first()
                                                    currentProvider = newProv
                                                    vm.setPreferredServer(newProv.id)
                                                } else {
                                                    val embedServers = when (catKey) {
                                                        "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub }
                                                        "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti }
                                                        else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti }
                                                    }
                                                    val newProv = embedServers.firstOrNull() ?: ALL_PROVIDERS.first { it.isEmbedOnly }
                                                    currentProvider = newProv
                                                    vm.setPreferredServer(newProv.id)
                                                }
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCatSelected) Color.Black else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Clean Server Dropdown Trigger Selector Button
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentServerTitle = currentProvider.name
                            val currentServerBadge = currentProvider.serverBadge.replace("Direct HD", "HD")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161922))
                                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { isServerDropdownExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Clean active status dot (white)
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = currentServerTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x1FFFFFFF))
                                        .border(0.8.dp, Color(0x35FFFFFF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentServerBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Server",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Clean Server Dropdown Menu
                            DropdownMenu(
                                expanded = isServerDropdownExpanded,
                                onDismissRequest = { isServerDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .background(Color(0xFF141722))
                                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                            ) {
                                if (selectedSourceMode == "DIRECT") {
                                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "DIRECT SERVERS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    if (availableDirectServers.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "No direct servers available for this audio category.",
                                                fontSize = 12.sp,
                                                color = OttTextMuted
                                            )
                                        }
                                    } else {
                                        val filteredDirectServers = when (selectedCategory) {
                                            "DUB" -> availableDirectServers.filter { it.isDub || it.isMulti }
                                            "MULTI" -> availableDirectServers.filter { it.isMulti }
                                            else -> availableDirectServers.filter { !it.isDub && !it.isMulti }
                                        }.ifEmpty { availableDirectServers }
                                        filteredDirectServers.forEach { prov ->
                                            val isSelected = prov.id == currentProvider.id
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = prov.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Color.White else OttTextSecondary
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(if (isSelected) Color(0x33FFFFFF) else Color(0x15FFFFFF))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = prov.serverBadge.replace("Direct HD", "HD"),
                                                                fontSize = 9.sp,
                                                                color = if (isSelected) Color.White else OttTextMuted
                                                            )
                                                        }
                                                    }
                                                },
                                                leadingIcon = {
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    } else {
                                                        Spacer(Modifier.size(18.dp))
                                                    }
                                                },
                                                onClick = {
                                                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                    currentProvider = prov
                                                    vm.setPreferredServer(prov.id)
                                                    isServerDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    val embedServers = when (selectedCategory) {
                                        "DUB" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isDub }
                                        "MULTI" -> ALL_PROVIDERS.filter { it.isEmbedOnly && it.isMulti }
                                        else -> ALL_PROVIDERS.filter { it.isEmbedOnly && !it.isDub && !it.isMulti }
                                    }

                                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "EMBED SOURCES",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    embedServers.forEach { prov ->
                                        val isSelected = prov.id == currentProvider.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = prov.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else OttTextSecondary
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSelected) Color(0x33FFFFFF) else Color(0x15FFFFFF))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = prov.serverBadge.replace("Direct HD", "HD"),
                                                            fontSize = 9.sp,
                                                            color = if (isSelected) Color.White else OttTextMuted
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Spacer(Modifier.size(18.dp))
                                                }
                                            },
                                            onClick = {
                                                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                currentProvider = prov
                                                vm.setPreferredServer(prov.id)
                                                isServerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


                // 4. MOVIE DETAILS (if Movie) OR EPISODE SECTION (if Series)
                if (isMovie) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Movie Title & Meta Card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = anime.cover,
                                    contentDescription = anime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = anime.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .border(0.8.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "MOVIE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        anime.duration?.let { dur ->
                                            Text(
                                                text = "$dur min",
                                                fontSize = 12.sp,
                                                color = OttTextSecondary
                                            )
                                        }

                                        anime.score?.let { score ->
                                            Text(
                                                text = "★ ${score}%",
                                                fontSize = 12.sp,
                                                color = OttTextSecondary
                                            )
                                        }
                                    }

                                    if (!anime.genres.isNullOrEmpty()) {
                                        Text(
                                            text = anime.genres.take(3).joinToString(" • "),
                                            fontSize = 11.5.sp,
                                            color = OttTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Download Movie Button
                                    val isMovieDl = remember(anime.id, downloadedAnimeList) {
                                        vm.isAnimeEpisodeDownloaded(anime.id, 1)
                                    }
                                    val isMovieDling = remember(anime.id, activeAnimeTasks) {
                                        activeAnimeTasks.containsKey("${anime.id}:1")
                                    }

                                    Row(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isMovieDl) Color(0x22FFFFFF) else Color.White)
                                            .clickable {
                                                if (!isMovieDl && !isMovieDling) {
                                                    showDownloadSheetEpisode = 1 to anime.title
                                                } else if (isMovieDl) {
                                                    android.widget.Toast.makeText(context, "Movie is already downloaded", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMovieDl) Icons.Default.Check else Icons.Default.Download,
                                            contentDescription = "Download Movie",
                                            tint = if (isMovieDl) Color.White else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isMovieDl) "Downloaded" else if (isMovieDling) "Downloading..." else "Download Movie",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMovieDl) Color.White else Color.Black
                                        )
                                    }
                                }
                            }

                            // Synopsis
                            if (!anime.description.isNullOrBlank()) {
                                val cleanDesc = remember(anime.description) {
                                    anime.description.replace(Regex("<[^>]*>"), "").trim()
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "SYNOPSIS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OttTextMuted,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = cleanDesc,
                                        fontSize = 12.5.sp,
                                        color = OttTextSecondary,
                                        lineHeight = 18.sp,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // EPISODE SECTION FOR SERIES
                    // Range Selector Pills & View Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pageCount > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(pageCount) { page ->
                                        val pStart = page * pageSize + 1
                                        val pEnd = minOf(pStart + pageSize - 1, maxEpisode)
                                        val isSelected = selectedPage == page

                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(if (isSelected) Color.White else Color(0x14FFFFFF))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color.White else Color(0x22FFFFFF),
                                                    OttPillShape
                                                )
                                                .clickable { selectedPage = page }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
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
                            } else {
                                Text(
                                    text = "Episodes 1 – $maxEpisode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            val curStart = selectedPage * pageSize + 1
                            val curEnd = minOf(curStart + pageSize - 1, maxEpisode)

                            // Download All in active page range (e.g. 1-25, 26-50)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        showDownloadRangeSheet = curStart..curEnd
                                    }
                                    .padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download All",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Download All",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            // View Toggle (Grid vs Cards)
                            IconButton(
                                onClick = { isEpisodeGridView = !isEpisodeGridView },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x25FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                                    contentDescription = "Toggle Episode View",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 4. Episode Content (Grid or Rich Cards)
                    val startEp = selectedPage * pageSize + 1
                    val endEp = minOf(startEp + pageSize - 1, maxEpisode)
                    val episodeNumbers = (startEp..endEp).toList()

                    if (isEpisodeGridView) {
                        // Compact fast-selection grid (5 columns)
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val chunked = episodeNumbers.chunked(5)
                                chunked.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { epNum ->
                                            val isCurrent = epNum == currentEpisode
                                            val isEpFiller = fillerInfo.isFiller(epNum)
                                            val isEpMixed = fillerInfo.isMixed(epNum)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isCurrent) Color.White
                                                        else if (isEpFiller) Color(0x24F59E0B)
                                                        else if (isEpMixed) Color(0x246366F1)
                                                        else OttSurface
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isCurrent) Color.White
                                                        else if (isEpFiller) Color(0x60FBBF24)
                                                        else if (isEpMixed) Color(0x60818CF8)
                                                        else OttSurfaceBorder,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                                        currentEpisode = epNum
                                                    },
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
                            }
                        }
                    } else {
                        // Rich Episode Cards (Netflix / Crunchyroll style matching Details page)
                        items(episodeNumbers, key = { it }) { epNum ->
                            val epStillInfo = tmdbEpisodes[epNum]
                            val isEpDl = remember(anime.id, epNum, downloadedAnimeList) {
                                vm.isAnimeEpisodeDownloaded(anime.id, epNum)
                            }
                            val isEpDling = remember(anime.id, epNum, activeAnimeTasks) {
                                activeAnimeTasks.containsKey("${anime.id}:$epNum")
                            }
                            OttEpisodeCardItem(
                                anime = anime,
                                episode = epNum,
                                isSelected = epNum == currentEpisode,
                                episodeStill = epStillInfo?.stillUrl?.takeIf { it.isNotBlank() }
                                    ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                                    ?: anime.banner?.takeIf { it.isNotBlank() }
                                    ?: anime.cover,
                                episodeTitle = epStillInfo?.name,
                                isDownloaded = isEpDl,
                                isDownloading = isEpDling,
                                downloadTask = activeAnimeTasks["${anime.id}:$epNum"],
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
                                }
                            )
                        }
                    }
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
                    colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry (${failedTask.providerName})", color = Color.White, fontWeight = FontWeight.Bold)
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
                        Text("Change Server", color = Color.White.copy(alpha = 0.8f))
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
                        Text("Dismiss", color = OttTextSecondary)
                    }
                }
            },
            containerColor = OttSurfaceElevated,
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
