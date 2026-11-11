package com.nyyrox.aniverse.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.runtime.collectAsState
import com.nyyrox.aniverse.data.download.DownloadStatus
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.extractor.StreamServerOption
import com.nyyrox.aniverse.data.movie.MovieDetails
import com.nyyrox.aniverse.data.movie.MovieEpisode
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.data.movie.MovieStream
import com.nyyrox.aniverse.data.movie.MovieStreamResult
import com.nyyrox.aniverse.data.movie.toAnime
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import com.nyyrox.aniverse.player.CinePlayerAudioOption
import com.nyyrox.aniverse.player.CinePlayerProps
import com.nyyrox.aniverse.player.CinePlayerQualityOption
import com.nyyrox.aniverse.player.CinePlayerView
import com.nyyrox.aniverse.player.IframePlayerView
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MoviePlayerScreen(
    id: Int,
    type: String, // "movie" or "tv"
    initialSeason: Int = 1,
    initialEpisode: Int = 1,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val p = rememberHomePalette()
    val scope = rememberCoroutineScope()
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    var currentSeason by remember { mutableIntStateOf(initialSeason) }
    var currentEpisode by remember { mutableIntStateOf(initialEpisode) }
    var selectedDub by remember { mutableStateOf<String?>(null) }
    var selectedQuality by remember { mutableStateOf<String?>(null) }
    var showDownloadOptionsSheet by remember { mutableStateOf(false) }
    var downloadTargetSeason by remember { mutableIntStateOf(1) }
    var downloadTargetEpisode by remember { mutableIntStateOf(1) }
    var downloadTargetEpTitle by remember { mutableStateOf<String?>(null) }
    val storage = remember { com.nyyrox.aniverse.data.StorageManager(context) }
    val downloadManager = remember { com.nyyrox.aniverse.data.download.DownloadManager.getInstance(context) }
    val initialSavedPosition = remember(id, currentSeason, currentEpisode) {
        storage.getMoviePosition(id, currentSeason, currentEpisode)
    }

    val isTv = type.equals("tv", ignoreCase = true)

    // All available servers
    val allServers = remember(id, type, currentSeason, currentEpisode) {
        MovieRepository.getMovieServers(id, type, currentSeason, currentEpisode)
    }
    var selectedServerId by remember { mutableStateOf("moviebox") }
    val currentServer = allServers.find { it.id == selectedServerId } ?: allServers.first()

    // Tabs below video matching Anime PlayerScreen: EPISODES (if TV) / OVERVIEW / CAST / SIMILAR
    var selectedTab by remember(isTv) { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<MovieDetails?>(null) }
    val tabs = remember(isTv, details?.collection) {
        val hasColl = details?.collection?.isNotEmpty() == true
        if (isTv) listOf("Episodes", "Overview", "Cast", "Similar")
        else if (hasColl) listOf("Overview", "Franchise", "Cast", "Similar")
        else listOf("Overview", "Cast", "Similar")
    }
    var episodes by remember { mutableStateOf<List<MovieEpisode>>(emptyList()) }
    var streamResult by remember { mutableStateOf<MovieStreamResult?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isLoadingStream by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

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

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            onBack()
        }
    }

    // Load Details
    LaunchedEffect(id, type) {
        isLoadingDetails = true
        details = MovieRepository.getDetails(id, type)
        isLoadingDetails = false
    }

    // Load Episodes for TV
    LaunchedEffect(id, type, currentSeason) {
        if (isTv) {
            episodes = MovieRepository.getEpisodes(id, currentSeason)
        }
    }

    // Load Stream when MovieBox is active
    fun loadMovieBoxStream() {
        if (currentServer.id != "moviebox") return
        scope.launch {
            isLoadingStream = true
            streamError = null
            try {
                if (selectedDub == null && downloadManager.isMovieEpisodeDownloaded(id, currentSeason, currentEpisode)) {
                    val downloadedEp = downloadManager.getDownloadedMovieEpisode(id, currentSeason, currentEpisode)
                    if (downloadedEp != null && java.io.File(downloadedEp.filePath).exists()) {
                        streamResult = MovieStreamResult(
                            success = true,
                            server = "Offline Download",
                            title = downloadedEp.movieTitle,
                            streamUrl = downloadedEp.filePath,
                            downloadUrl = downloadedEp.filePath,
                            quality = downloadedEp.quality,
                            format = "mp4",
                            dubs = emptyList(),
                            streams = listOf(
                                MovieStream(
                                    quality = downloadedEp.quality,
                                    resolutionNum = 1080,
                                    format = "mp4",
                                    size = downloadedEp.sizeBytes,
                                    streamUrl = downloadedEp.filePath,
                                    downloadUrl = downloadedEp.filePath,
                                    downloadFileName = "video.mp4"
                                )
                            ),
                            subtitles = emptyList()
                        )
                        selectedQuality = downloadedEp.quality
                        isLoadingStream = false
                        return@launch
                    }
                }
                val res = MovieRepository.fetchMovieStream(
                    id = id,
                    type = type,
                    season = currentSeason,
                    episode = currentEpisode,
                    title = details?.title,
                    year = details?.year,
                    dubId = selectedDub,
                    imdbId = details?.imdbId
                )
                if (res.success && res.streamUrl.isNotBlank()) {
                    streamResult = res
                    if (selectedQuality == null) {
                        selectedQuality = res.quality
                    }
                } else if (selectedDub != null) {
                    Toast.makeText(context, "Dub unavailable, playing original audio", Toast.LENGTH_SHORT).show()
                    selectedDub = null
                    loadMovieBoxStream()
                    return@launch
                } else {
                    val fallback = allServers.find { !it.isDirect && it.embedUrl.isNotBlank() }
                    if (fallback != null) {
                        selectedServerId = fallback.id
                        Toast.makeText(context, "MovieBox unavailable, switched to ${fallback.label}", Toast.LENGTH_SHORT).show()
                    } else {
                        streamError = res.error ?: "Stream not available on MovieBox"
                    }
                }
            } catch (e: Exception) {
                val fallback = allServers.find { !it.isDirect && it.embedUrl.isNotBlank() }
                if (fallback != null) {
                    selectedServerId = fallback.id
                    Toast.makeText(context, "MovieBox unavailable, switched to ${fallback.label}", Toast.LENGTH_SHORT).show()
                } else {
                    streamError = e.message ?: "Playback error"
                }
            } finally {
                isLoadingStream = false
            }
        }
    }

    LaunchedEffect(id, type, currentSeason, currentEpisode, selectedDub, selectedServerId, details?.title) {
        if (currentServer.id == "moviebox") {
            loadMovieBoxStream()
        }
    }

    // Active MP4/HLS stream selection
    val activeStream: MovieStream? = remember(streamResult, selectedQuality) {
        val list = streamResult?.streams ?: emptyList()
        if (selectedQuality != null) {
            list.find { it.quality.equals(selectedQuality, ignoreCase = true) } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }

    val currentStreamUrl = activeStream?.streamUrl?.ifBlank { streamResult?.streamUrl.orEmpty() } ?: streamResult?.streamUrl.orEmpty()

    val subtitleTracks: List<SubtitleTrack> = remember(streamResult) {
        streamResult?.subtitles?.map { sub ->
            SubtitleTrack(
                id = sub.id,
                url = sub.url,
                label = sub.label,
                language = sub.language,
                isDefault = false
            )
        } ?: emptyList()
    }

    // AUDIO TRACKS: Convert MovieBox dubs into CinePlayerAudioOption
    val externalAudioTracks = remember(streamResult?.dubs, selectedDub) {
        val dubs = streamResult?.dubs ?: emptyList()
        dubs.map { dub ->
            CinePlayerAudioOption(
                id = dub.id,
                label = dub.label,
                isSelected = if (selectedDub != null) dub.id == selectedDub else dub.selected
            )
        }
    }

    // QUALITY TRACKS: Convert MovieBox MP4 qualities into CinePlayerQualityOption
    val externalQualityTracks = remember(streamResult?.streams, selectedQuality, currentStreamUrl) {
        val streams = streamResult?.streams ?: emptyList()
        streams.map { s ->
            CinePlayerQualityOption(
                id = s.quality,
                label = s.quality + (s.sizeLabel?.let { " ($it)" } ?: ""),
                isSelected = (selectedQuality != null && s.quality.equals(selectedQuality, ignoreCase = true)) || (currentStreamUrl == s.streamUrl),
                streamUrl = s.streamUrl
            )
        }
    }

    // Server options for CinePlayer Server menu
    val playerServerOptions = remember(allServers) {
        allServers.map { srv ->
            StreamServerOption(
                serverId = srv.id,
                serverName = srv.label,
                streamUrl = if (srv.isDirect) currentStreamUrl else srv.embedUrl,
                isHls = srv.isDirect
            )
        }
    }

    fun switchToServer(serverId: String) {
        selectedServerId = serverId
        val s = allServers.find { it.id == serverId }
        Toast.makeText(context, "Switched to ${s?.label ?: "server"}", Toast.LENGTH_SHORT).show()
    }

    val epName = if (isTv) episodes.find { it.episodeNumber == currentEpisode }?.name else null
    val similarAnime = remember(details?.similar) { details?.similar?.map { it.toAnime() } ?: emptyList() }
    val collectionAnime = remember(details?.collection) { details?.collection?.map { it.toAnime() } ?: emptyList() }

    val movieTasks by downloadManager.movieTasks.collectAsState()
    val downloadedMovies by downloadManager.downloadedMovies.collectAsState()

    val currentTaskKey = if (isTv) "$id:s${currentSeason}_e$currentEpisode" else "$id:movie"
    val currentMovieTask = movieTasks[currentTaskKey]
    val isCurrentDownloading = currentMovieTask != null && (currentMovieTask.status == DownloadStatus.DOWNLOADING || currentMovieTask.status == DownloadStatus.QUEUED)
    val isCurrentDownloaded = remember(downloadedMovies, id, currentSeason, currentEpisode) {
        downloadManager.isMovieEpisodeDownloaded(id, currentSeason, currentEpisode)
    }

    fun downloadCurrentEpisodeOrMovie() {
        if (isCurrentDownloaded) {
            Toast.makeText(context, "Already downloaded (Available for offline playback)", Toast.LENGTH_SHORT).show()
            return
        }
        if (isCurrentDownloading) {
            val rawP = currentMovieTask?.progress ?: 0f
            val safeP = if (rawP.isNaN() || !rawP.isFinite()) 0f else rawP
            val pct = (safeP * 100f).toInt()
            Toast.makeText(context, "Download in progress: $pct%", Toast.LENGTH_SHORT).show()
            return
        }
        downloadTargetSeason = currentSeason
        downloadTargetEpisode = currentEpisode
        val epDot = epName?.let { " · $it" } ?: ""
        downloadTargetEpTitle = if (isTv) "S$currentSeason E$currentEpisode$epDot" else details?.title
        showDownloadOptionsSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (!isFullscreen) Modifier.navigationBarsPadding() else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Strip behind the status bar in portrait
            if (!isFullscreen) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                )
            }

            // Video Player Container (Single persistent instance - survives fullscreen toggle & resize)
            Box(
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }
            ) {
                if (currentServer.isDirect) {
                    if (isLoadingStream && streamResult == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                                Text(text = "Resolving direct stream…", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    } else if (streamError != null && currentStreamUrl.isBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
                                Text(text = streamError ?: "Stream error", color = Color.White, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF2A2A32))
                                            .clickable { loadMovieBoxStream() }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Retry MovieBox", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    val fallback = allServers.find { !it.isDirect && it.embedUrl.isNotBlank() }
                                    if (fallback != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(p.accentBrush)
                                                .clickable { switchToServer(fallback.id) }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Switch to ${fallback.label}", color = p.onAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        key(currentSeason, currentEpisode, selectedServerId, selectedDub) {
                            CinePlayerView(
                                props = CinePlayerProps(
                                    title = details?.title ?: "Playing",
                                    episode = currentEpisode,
                                    episodeTitle = epName,
                                    streamUrl = currentStreamUrl,
                                    isDirectStream = true,
                                    isFullscreen = isFullscreen,
                                    onToggleFullscreen = { toggleFullscreen() },
                                    onBack = {
                                        if (isFullscreen) {
                                            toggleFullscreen()
                                        } else {
                                            onBack()
                                        }
                                    },
                                    externalSubtitles = subtitleTracks,
                                    initialTimeMs = initialSavedPosition,
                                    externalAudioTracks = externalAudioTracks,
                                    onSelectAudioTrack = { opt ->
                                        selectedDub = opt.id
                                        loadMovieBoxStream()
                                    },
                                    externalQualityTracks = externalQualityTracks,
                                    onSelectQualityTrack = { opt ->
                                        selectedQuality = opt.id
                                    },
                                    availableServers = playerServerOptions,
                                    onSelectServer = { opt ->
                                        switchToServer(opt.serverId)
                                    },
                                    serverName = currentServer.label,
                                    onDownload = { downloadCurrentEpisodeOrMovie() },
                                    isDownloaded = isCurrentDownloaded,
                                    isDownloading = isCurrentDownloading,
                                    onProgressUpdate = { currentMs, durationMs ->
                                        if (currentMs > 1000L) {
                                            storage.saveMovieProgress(
                                                id = id,
                                                type = type,
                                                title = details?.title ?: "Playing",
                                                poster = details?.poster,
                                                backdrop = details?.backdrop,
                                                season = currentSeason,
                                                episode = currentEpisode,
                                                positionMs = currentMs,
                                                durationMs = durationMs
                                            )
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    key(currentSeason, currentEpisode, selectedServerId) {
                        IframePlayerView(
                            embedUrl = currentServer.embedUrl,
                            modifier = Modifier.fillMaxSize(),
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { toggleFullscreen() }
                        )
                    }
                }
            }

            // Below Video Section (Hidden in Fullscreen)
            if (!isFullscreen) {
                // Below Video Section (Exact Anime PlayerScreen design: Pure black/white, server chips, sliding tabs)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(p.pageBrush),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    // 1. Title, Episode, and Meta
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .padding(top = 18.dp)
                        ) {
                            Text(
                                text = details?.title ?: "Playing Title",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = p.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isTv) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Season $currentSeason · Ep $currentEpisode${epName?.let { " · $it" } ?: ""}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.accentA,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            val metaList = listOfNotNull(
                                details?.rating?.let { "★ " + String.format(Locale.US, "%.1f", it) },
                                if (isTv) "Series" else "Movie",
                                details?.year,
                                details?.runtime?.let { "${it}m" }
                            )
                            Text(
                                text = metaList.joinToString("  ·  "),
                                fontSize = 12.5.sp,
                                color = p.textMuted
                            )

                            Spacer(Modifier.height(14.dp))

                            // Download Action Pill
                            val rawCurP = currentMovieTask?.progress ?: 0f
                            val safeCurP = if (rawCurP.isNaN() || !rawCurP.isFinite()) 0f else rawCurP
                            val dlBtnText = when {
                                isCurrentDownloading -> "Downloading (${(safeCurP * 100f).toInt()}%)"
                                isCurrentDownloaded -> "Downloaded (Available Offline)"
                                isTv -> "Download Episode $currentEpisode"
                                else -> "Download Movie"
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (isCurrentDownloaded) Modifier.background(p.chipBg) else Modifier.background(p.accentBrush))
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrentDownloaded) p.accentA else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { downloadCurrentEpisodeOrMovie() }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when {
                                        isCurrentDownloading -> {
                                            CircularProgressIndicator(
                                                progress = { val rawP = currentMovieTask?.progress ?: 0f; if (rawP.isNaN() || !rawP.isFinite()) 0.05f else rawP.coerceIn(0.05f, 1f) },
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = p.onAccent,
                                                trackColor = p.onAccent.copy(alpha = 0.3f)
                                            )
                                            Text(
                                                text = dlBtnText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.onAccent
                                            )
                                        }
                                        isCurrentDownloaded -> {
                                            Icon(
                                                imageVector = Icons.Default.DownloadDone,
                                                contentDescription = "Downloaded",
                                                tint = p.accentA,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Text(
                                                text = dlBtnText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.accentA
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.FileDownload,
                                                contentDescription = "Download",
                                                tint = p.onAccent,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Text(
                                                text = dlBtnText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.onAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Server Selector Chips
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                            Text(text = "SERVER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.textMuted, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allServers.forEach { srv ->
                                    val isCur = srv.id == selectedServerId
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .then(if (isCur) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                            .border(1.dp, if (isCur) Color.Transparent else p.glassBorder, RoundedCornerShape(999.dp))
                                            .clickable { switchToServer(srv.id) }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = srv.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCur) p.onAccent else p.text
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Sliding Tabs Switcher
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.chipBg)
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, tabName ->
                                val isSel = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(999.dp))
                                        .then(if (isSel) Modifier.background(p.accentBrush) else Modifier)
                                        .clickable { selectedTab = index }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) p.onAccent else p.textSub
                                    )
                                }
                            }
                        }
                    }

                    // 4. Tab Content
                    val curTab = tabs.getOrNull(selectedTab) ?: "Overview"

                    when (curTab) {
                        "Episodes" -> {
                            // Season switcher
                            if (details?.seasons?.isNotEmpty() == true) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        details!!.seasons.forEach { s ->
                                            val isCur = s.seasonNumber == currentSeason
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .then(if (isCur) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                                    .clickable { currentSeason = s.seasonNumber }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "Season ${s.seasonNumber}",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCur) p.onAccent else p.text
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(episodes, key = { _, ep -> "p_ep_${ep.episodeNumber}" }) { _, ep ->
                                val isPlayingThis = (ep.episodeNumber == currentEpisode)
                                val epKey = "$id:s${currentSeason}_e${ep.episodeNumber}"
                                val epTask = movieTasks[epKey]
                                val isEpDownloading = epTask != null && (epTask.status == DownloadStatus.DOWNLOADING || epTask.status == DownloadStatus.QUEUED)
                                val isEpDownloaded = remember(downloadedMovies, id, currentSeason, ep.episodeNumber) {
                                    downloadManager.isMovieEpisodeDownloaded(id, currentSeason, ep.episodeNumber)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentEpisode = ep.episodeNumber
                                        }
                                        .padding(horizontal = 18.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(115.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(p.flatFill)
                                    ) {
                                        if (!ep.stillUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ep.stillUrl,
                                                contentDescription = ep.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xBB000000))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "EP ${ep.episodeNumber}",
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${ep.episodeNumber}. ${ep.name}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPlayingThis) p.accentA else p.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (ep.runtime != null) {
                                            Text(text = "${ep.runtime}m", fontSize = 11.5.sp, color = p.textMuted)
                                        }
                                        if (ep.overview.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = ep.overview,
                                                fontSize = 11.5.sp,
                                                color = p.textSub,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Episode Download Button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isEpDownloaded) p.accentA.copy(alpha = 0.15f) else p.chipBg)
                                            .clickable {
                                                if (isEpDownloaded) {
                                                    Toast.makeText(context, "Ep ${ep.episodeNumber} is already downloaded", Toast.LENGTH_SHORT).show()
                                                } else if (isEpDownloading) {
                                                    val epPct = ((epTask?.progress ?: 0f) * 100f).toInt()
                                                    Toast.makeText(context, "Ep ${ep.episodeNumber} download in progress ($epPct%)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    downloadTargetSeason = currentSeason
                                                    downloadTargetEpisode = ep.episodeNumber
                                                    val epDot = if (ep.name.isNotBlank()) " · ${ep.name}" else ""
                                                    downloadTargetEpTitle = "S${currentSeason} E${ep.episodeNumber}$epDot"
                                                    showDownloadOptionsSheet = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isEpDownloading -> {
                                                CircularProgressIndicator(
                                                    progress = { val rawEpP = epTask?.progress ?: 0f; if (rawEpP.isNaN() || !rawEpP.isFinite()) 0.05f else rawEpP.coerceIn(0.05f, 1f) },
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = p.accentA,
                                                    trackColor = p.glassBorder
                                                )
                                            }
                                            isEpDownloaded -> {
                                                Icon(
                                                    imageVector = Icons.Default.DownloadDone,
                                                    contentDescription = "Downloaded",
                                                    tint = p.accentA,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    imageVector = Icons.Default.FileDownload,
                                                    contentDescription = "Download Episode",
                                                    tint = p.textMuted,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Overview" -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    Text(text = "Synopsis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.text)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = details?.overview?.ifBlank { "No synopsis available." } ?: "No synopsis available.",
                                        fontSize = 13.sp,
                                        color = p.textSub,
                                        lineHeight = 19.sp
                                    )

                                    if (details?.genres?.isNotEmpty() == true) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(text = "Genres", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.text)
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            details!!.genres.forEach { g ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(p.chipBg)
                                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                                ) {
                                                    Text(text = g.name, fontSize = 11.5.sp, color = p.text)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Cast" -> {
                            if (details?.cast?.isEmpty() != false) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text("No cast details available", color = p.textMuted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                items(details!!.cast) { c ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(p.flatFill)
                                        ) {
                                            if (!c.photoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = c.photoUrl,
                                                    contentDescription = c.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Column {
                                            Text(text = c.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = p.text)
                                            Text(text = c.character, fontSize = 11.5.sp, color = p.textMuted)
                                        }
                                    }
                                }
                            }
                        }

                        "Franchise" -> {
                            if (collectionAnime.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text("No franchise relations found", color = p.textMuted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        SectionTitle("Franchise & Sequels", p)
                                        AnimeRow(
                                            list = collectionAnime,
                                            p = p,
                                            onAnimeClick = { animeId ->
                                                val movie = details?.collection?.find { it.id == animeId }
                                                Toast.makeText(context, "Selected ${movie?.title ?: "Movie"}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "Similar" -> {
                            if (similarAnime.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text("No similar titles found", color = p.textMuted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        SectionTitle("You May Also Like", p)
                                        AnimeRow(
                                            list = similarAnime,
                                            p = p,
                                            onAnimeClick = { animeId ->
                                                val sim = details?.similar?.find { it.id == animeId }
                                                Toast.makeText(context, "Selected ${sim?.title ?: "Movie"}", Toast.LENGTH_SHORT).show()
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
    }

    if (showDownloadOptionsSheet) {
        com.nyyrox.aniverse.ui.components.DownloadMovieOptionsSheet(
            movieId = id,
            title = details?.title ?: "Movie",
            type = type,
            season = downloadTargetSeason,
            episode = downloadTargetEpisode,
            episodeTitle = downloadTargetEpTitle ?: if (isTv) "S$downloadTargetSeason E$downloadTargetEpisode" else null,
            poster = details?.poster,
            backdrop = details?.backdrop,
            overview = details?.overview,
            year = details?.year,
            initialQuality = selectedQuality,
            initialDubId = selectedDub,
            availableDubs = streamResult?.dubs,
            availableStreams = streamResult?.streams,
            onDismiss = { showDownloadOptionsSheet = false },
            onStartDownload = { quality, dubId ->
                downloadManager.downloadMovie(
                    id = id,
                    title = details?.title ?: "Movie",
                    poster = details?.poster,
                    backdrop = details?.backdrop,
                    overview = details?.overview,
                    year = details?.year,
                    type = type,
                    season = downloadTargetSeason,
                    episode = downloadTargetEpisode,
                    episodeTitle = downloadTargetEpTitle ?: if (isTv) "S$downloadTargetSeason E$downloadTargetEpisode" else null,
                    quality = quality,
                    dubId = dubId
                )
                Toast.makeText(
                    context,
                    "Download queued: ${downloadTargetEpTitle ?: (details?.title ?: "Movie")} ($quality)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
