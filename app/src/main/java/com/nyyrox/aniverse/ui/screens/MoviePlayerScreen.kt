package com.nyyrox.aniverse.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.movie.MovieDetails
import com.nyyrox.aniverse.data.movie.MovieEpisode
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.data.movie.MovieStream
import com.nyyrox.aniverse.data.movie.MovieStreamResult
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import com.nyyrox.aniverse.player.CinePlayerProps
import com.nyyrox.aniverse.player.CinePlayerView
import kotlinx.coroutines.launch

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

    var currentSeason by remember { mutableIntStateOf(initialSeason) }
    var currentEpisode by remember { mutableIntStateOf(initialEpisode) }
    var selectedDub by remember { mutableStateOf<String?>(null) }
    var selectedQuality by remember { mutableStateOf<String?>(null) }

    var details by remember { mutableStateOf<MovieDetails?>(null) }
    var episodes by remember { mutableStateOf<List<MovieEpisode>>(emptyList()) }
    var streamResult by remember { mutableStateOf<MovieStreamResult?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var isLoadingStream by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val activity = context as? Activity

    // Reset orientation on exit
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onBack()
        }
    }

    // Load Movie / Show Details
    LaunchedEffect(id, type) {
        isLoadingDetails = true
        details = MovieRepository.getDetails(id, type)
        isLoadingDetails = false
    }

    // Load Episodes for TV
    LaunchedEffect(id, type, currentSeason) {
        if (type == "tv") {
            episodes = MovieRepository.getEpisodes(id, currentSeason)
        }
    }

    // Load MovieBox Stream
    fun loadStream() {
        scope.launch {
            isLoadingStream = true
            streamError = null
            try {
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
                    selectedQuality = res.quality
                } else {
                    streamError = res.error ?: "Unable to resolve stream from MovieBox"
                }
            } catch (e: Exception) {
                streamError = e.message ?: "Playback error"
            } finally {
                isLoadingStream = false
            }
        }
    }

    LaunchedEffect(id, type, currentSeason, currentEpisode, selectedDub, details?.title) {
        if (details != null) {
            loadStream()
        }
    }

    // Determine current active stream URL based on selected quality
    val activeStream: MovieStream? = remember(streamResult, selectedQuality) {
        val list = streamResult?.streams ?: emptyList()
        if (selectedQuality != null) {
            list.find { it.quality.equals(selectedQuality, ignoreCase = true) } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }
    val currentStreamUrl = activeStream?.streamUrl ?: streamResult?.streamUrl ?: ""

    // Subtitles conversion
    val subtitleTracks: List<SubtitleTrack> = remember(streamResult) {
        streamResult?.subtitles?.map { sub ->
            SubtitleTrack(
                id = sub.id,
                url = sub.url,
                label = sub.label,
                language = sub.language
            )
        } ?: emptyList()
    }

    fun downloadCurrentMedia(url: String, fileName: String) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
                .setTitle(fileName)
                .setDescription("Downloading via Zenkai")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            dm.enqueue(request)
            Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullscreen) {
            // Fullscreen View
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentStreamUrl.isNotBlank()) {
                    CinePlayerView(
                        props = CinePlayerProps(
                            title = details?.title ?: "Playing",
                            episode = currentEpisode,
                            episodeTitle = if (type == "tv") episodes.find { it.episodeNumber == currentEpisode }?.name else null,
                            streamUrl = currentStreamUrl,
                            isDirectStream = true,
                            isFullscreen = true,
                            onToggleFullscreen = {
                                isFullscreen = false
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            },
                            onBack = {
                                isFullscreen = false
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            },
                            externalSubtitles = subtitleTracks
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Portrait / Regular Layout (Player on top, Audio Dubs, Quality, Details & Episodes below)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                // Top Header: Back & Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = details?.title ?: "Loading…",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (type == "tv") {
                                val epName = episodes.find { it.episodeNumber == currentEpisode }?.name
                                Text(
                                    text = "S$currentSeason E$currentEpisode${if (!epName.isNullOrBlank()) " · $epName" else ""}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFAAAAAA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ── Player Container (16:9 Aspect Ratio) ──
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        if (currentStreamUrl.isNotBlank() && !isLoadingStream) {
                            CinePlayerView(
                                props = CinePlayerProps(
                                    title = details?.title ?: "Playing",
                                    episode = currentEpisode,
                                    episodeTitle = if (type == "tv") episodes.find { it.episodeNumber == currentEpisode }?.name else null,
                                    streamUrl = currentStreamUrl,
                                    isDirectStream = true,
                                    isFullscreen = false,
                                    onToggleFullscreen = {
                                        isFullscreen = true
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    },
                                    onBack = onBack,
                                    externalSubtitles = subtitleTracks
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Stream Loading Spinner / Backdrop
                        if (isLoadingStream || isLoadingDetails) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (details?.backdropUrl != null) {
                                    AsyncImage(
                                        model = details!!.backdropUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.65f))
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = p.rating,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Connecting MovieBox Direct Stream…",
                                        fontSize = 12.5.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Error Banner
                        if (streamError != null && !isLoadingStream) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xEE111111))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = streamError ?: "Stream failed",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White)
                                            .clickable { loadStream() }
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Retry",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Controls Strip: AUDIO & QUALITY (Exact match to web screenshots!) ──
                if (streamResult != null && !isLoadingStream) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1115))
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: AUDIO Dub Pills
                            if (streamResult!!.dubs.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFFAAAAAA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "AUDIO:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFAAAAAA),
                                            letterSpacing = 0.8.sp
                                        )
                                    }

                                    streamResult!!.dubs.forEach { dub ->
                                        val isSelected = (selectedDub == null && dub.id == "original") || selectedDub == dub.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) Color.White else Color(0x22FFFFFF))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable {
                                                    if (!isSelected) {
                                                        selectedDub = dub.id
                                                        Toast.makeText(context, "Switching to ${dub.label}…", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = dub.label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Row 2: QUALITY Selector Pills & Direct MP4 Download Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = Color(0xFFAAAAAA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "QUALITY:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFAAAAAA),
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                streamResult!!.streams.forEach { stream ->
                                    val isSelected = selectedQuality == stream.quality || (selectedQuality == null && stream == streamResult!!.streams.firstOrNull())
                                    val label = "${stream.quality}${if (!stream.sizeLabel.isNullOrBlank()) " ${stream.sizeLabel}" else ""}"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) Color.White else Color(0x22FFFFFF))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable {
                                                selectedQuality = stream.quality
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }

                                // Download MP4 Action Pill
                                activeStream?.downloadUrl?.takeIf { it.isNotBlank() }?.let { dlUrl ->
                                    val dlLabel = "Download MP4${if (!activeStream.sizeLabel.isNullOrBlank()) " ${activeStream.sizeLabel}" else ""}"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                                            .clickable {
                                                val safeTitle = (details?.title ?: "Movie").replace("[^a-zA-Z0-9]".toRegex(), "_")
                                                val fName = "$safeTitle-${activeStream.quality}.mp4"
                                                downloadCurrentMedia(dlUrl, fName)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = dlLabel,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Media Info (Title, Year, Runtime, Rating, Overview) ──
                details?.let { d ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = d.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                d.year?.let {
                                    Text(
                                        text = it,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFAAAAAA),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                d.runtime?.let {
                                    Text(
                                        text = "· ${it} min",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFAAAAAA),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                d.rating?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "· ", color = Color(0xFFAAAAAA))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "$it",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (d.overview.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = d.overview,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFFCCCCCC),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // ── TV Series Seasons & Episodes ──
                if (type == "tv" && (details?.seasons?.isNotEmpty() == true || episodes.isNotEmpty())) {
                    // Season Picker Chips
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "SEASONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAAAAAA),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(details?.seasons ?: emptyList()) { season ->
                                    val isSelected = season.seasonNumber == currentSeason
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) p.rating else Color(0x22FFFFFF))
                                            .clickable {
                                                if (currentSeason != season.seasonNumber) {
                                                    currentSeason = season.seasonNumber
                                                    currentEpisode = 1
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = season.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Episode Grid / List
                    item {
                        Text(
                            text = "EPISODES (${episodes.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAAAAAA),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(episodes) { ep ->
                        val isPlaying = ep.episodeNumber == currentEpisode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentEpisode != ep.episodeNumber) {
                                        currentEpisode = ep.episodeNumber
                                    }
                                }
                                .background(if (isPlaying) Color(0x33FF3366) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Still thumbnail
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E2026))
                            ) {
                                if (ep.stillUrl != null) {
                                    AsyncImage(
                                        model = ep.stillUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Ep title & desc
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Episode ${ep.episodeNumber}: ${ep.name}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isPlaying) p.rating else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (ep.runtime != null) {
                                    Text(
                                        text = "${ep.runtime} min",
                                        fontSize = 11.sp,
                                        color = Color(0xFFAAAAAA)
                                    )
                                }
                                if (ep.overview.isNotBlank()) {
                                    Text(
                                        text = ep.overview,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF888888),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
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
