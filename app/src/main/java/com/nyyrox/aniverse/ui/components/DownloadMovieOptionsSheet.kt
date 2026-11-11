package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.data.movie.MovieDub
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.data.movie.MovieStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Amoled = Color(0xFF0F131C)
private val Muted = Color(0xFF94A3B8)
private val Soft = Color(0xFFCBD5E1)

data class MovieQualityOption(
    val id: String,
    val label: String,
    val resolution: String,
    val approxSize: String
)

val MOVIE_QUALITY_OPTIONS = listOf(
    MovieQualityOption("4K", "4K", "3840x2160", "~4.5 GB"),
    MovieQualityOption("1080p", "1080p", "1920x1080", "~1.8 GB"),
    MovieQualityOption("720p", "720p", "1280x720", "~950 MB"),
    MovieQualityOption("480p", "480p", "854x480", "~500 MB"),
    MovieQualityOption("360p", "360p", "640x360", "~260 MB"),
    MovieQualityOption("Auto HD", "Auto HD", "Best Quality", "Optimal")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadMovieOptionsSheet(
    movieId: Int,
    title: String,
    type: String = "movie",
    season: Int = 1,
    episode: Int = 1,
    episodeTitle: String? = null,
    poster: String? = null,
    backdrop: String? = null,
    overview: String? = null,
    year: String? = null,
    initialQuality: String? = null,
    initialDubId: String? = null,
    availableDubs: List<MovieDub>? = null,
    availableStreams: List<MovieStream>? = null,
    onDismiss: () -> Unit,
    onStartDownload: (quality: String, dubId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val storage = remember { StorageManager(context) }

    val qualityOptions = remember(availableStreams) {
        if (!availableStreams.isNullOrEmpty()) {
            val streamOpts = availableStreams.map { s ->
                val label = s.quality
                val res = when {
                    (s.resolutionNum != null && s.resolutionNum >= 2160) || s.quality.contains("4k", ignoreCase = true) -> "3840x2160"
                    s.quality.contains("1080") -> "1920x1080"
                    s.quality.contains("720") -> "1280x720"
                    s.quality.contains("480") -> "854x480"
                    s.quality.contains("360") -> "640x360"
                    else -> "${s.resolutionNum ?: 1080}p"
                }
                MovieQualityOption(
                    id = label,
                    label = label,
                    resolution = res,
                    approxSize = s.sizeLabel ?: "~1.5 GB"
                )
            }.distinctBy { it.id }
            val hasAuto = streamOpts.any { it.id.equals("Auto HD", ignoreCase = true) }
            if (hasAuto) streamOpts else streamOpts + MovieQualityOption("Auto HD", "Auto HD", "Best Quality", "Optimal")
        } else {
            MOVIE_QUALITY_OPTIONS
        }
    }

    val savedQuality = remember { storage.getLastDownloadQuality() }
    var selectedQuality by remember(qualityOptions) {
        mutableStateOf(
            initialQuality
                ?: savedQuality?.takeIf { sq -> qualityOptions.any { it.id.equals(sq, ignoreCase = true) } }
                ?: qualityOptions.firstOrNull { it.id.equals("1080p", ignoreCase = true) }?.id
                ?: qualityOptions.firstOrNull()?.id
                ?: "1080p"
        )
    }

    var resolvedDubs by remember(availableDubs) {
        mutableStateOf(availableDubs ?: emptyList())
    }
    var isLoadingDubs by remember {
        mutableStateOf(availableDubs == null)
    }

    var selectedDub by remember(initialDubId, resolvedDubs) {
        mutableStateOf(
            initialDubId ?: resolvedDubs.firstOrNull { it.selected }?.id
        )
    }

    // Resolve dubs from MovieRepository if not supplied
    LaunchedEffect(movieId, type, season, episode) {
        if (availableDubs == null) {
            isLoadingDubs = true
            withContext(Dispatchers.IO) {
                try {
                    val streamRes = MovieRepository.fetchMovieStream(
                        id = movieId,
                        type = type,
                        season = season,
                        episode = episode,
                        title = title,
                        year = year
                    )
                    withContext(Dispatchers.Main) {
                        resolvedDubs = streamRes.dubs
                        if (selectedDub == null) {
                            selectedDub = streamRes.dubs.firstOrNull { it.selected }?.id
                        }
                        isLoadingDubs = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoadingDubs = false
                    }
                }
            }
        }
    }

    val isTv = type.equals("tv", ignoreCase = true)
    val headerTitle = if (isTv) "Download Episode $episode" else "Download Movie"
    val subtitleText = if (isTv) "S$season E$episode · $title" else "$title${if (!year.isNullOrBlank()) " ($year)" else ""}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Amoled,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 2.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            )
        }
    ) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                val bobT = rememberInfiniteTransition(label = "bob")
                val bob by bobT.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "bobY"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = (bob - 0.5f) * 5.dp.toPx() }
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = headerTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleText,
                            color = Muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Audio Track Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.GraphicEq, null, tint = Muted, modifier = Modifier.size(15.dp))
                            Text(
                                text = "AUDIO TRACK",
                                color = Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        if (isLoadingDubs) {
                            CircularProgressIndicator(color = Soft, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (resolvedDubs.isEmpty() && !isLoadingDubs) {
                        // Default / Original audio option
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1FFFFFFF))
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Original Audio", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text("Default track", color = Soft, fontSize = 11.sp)
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Original / Default" Option
                            item {
                                val isSelected = selectedDub == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0x26FFFFFF) else Color(0x0DFFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedDub = null }
                                        .padding(horizontal = 14.dp, vertical = 11.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Original",
                                            color = if (isSelected) Color.White else Soft,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = "Default",
                                            color = if (isSelected) Soft else Muted,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }

                            items(resolvedDubs, key = { it.id }) { dub ->
                                val isSelected = selectedDub == dub.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0x26FFFFFF) else Color(0x0DFFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedDub = dub.id }
                                        .padding(horizontal = 14.dp, vertical = 11.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = dub.label,
                                            color = if (isSelected) Color.White else Soft,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = "Dubbed",
                                            color = if (isSelected) Soft else Muted,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Video Quality Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.HighQuality, null, tint = Muted, modifier = Modifier.size(15.dp))
                        Text(
                            text = "VIDEO QUALITY",
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(qualityOptions, key = { it.id }) { opt ->
                            val isSel = selectedQuality.equals(opt.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) Color(0x26FFFFFF) else Color(0x0DFFFFFF))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) Color.White else Color(0x1AFFFFFF),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedQuality = opt.id }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = opt.label,
                                        color = if (isSel) Color.White else Soft,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = opt.approxSize,
                                        color = if (isSel) Soft else Muted,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Subtitles & Offline Notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Subtitles, null, tint = Soft, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Multi-audio & offline cues enabled", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Audio tracks and subtitles download directly with the video file for offline playback", color = Muted, fontSize = 10.5.sp, lineHeight = 14.sp)
                    }
                }

                // Action Download Button
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.97f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
                    label = "dlPress"
                )

                val selectedDubLabel = resolvedDubs.firstOrNull { it.id == selectedDub }?.label ?: "Original Audio"
                val actionLabel = "Download ($selectedDubLabel • $selectedQuality)"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .clickable(interactionSource = source, indication = null) {
                            storage.saveDownloadSettings(selectedDubLabel, selectedQuality, "moviebox")
                            onStartDownload(selectedQuality, selectedDub)
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(19.dp))
                    Text(
                        text = actionLabel,
                        color = Color.Black,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
