package com.nyyrox.aniverse.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.collectAsState
import com.nyyrox.aniverse.data.download.DownloadStatus
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.movie.MovieCast
import com.nyyrox.aniverse.data.movie.MovieDetails
import com.nyyrox.aniverse.data.movie.MovieEpisode
import com.nyyrox.aniverse.data.movie.MovieItem
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.data.movie.toAnime
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MovieDetailsScreen(
    id: Int,
    type: String, // "movie" or "tv"
    onBack: () -> Unit,
    onPlay: (id: Int, type: String, season: Int, episode: Int) -> Unit,
    onMovieClick: ((id: Int, type: String, title: String?, poster: String?) -> Unit)? = null,
    cachedTitle: String? = null,
    cachedPoster: String? = null
) {
    val context = LocalContext.current
    val p = rememberHomePalette()
    val scope = rememberCoroutineScope()
    val isTv = type.equals("tv", ignoreCase = true)
    val storage = remember { com.nyyrox.aniverse.data.StorageManager(context) }
    val downloadManager = remember { com.nyyrox.aniverse.data.download.DownloadManager.getInstance(context) }
    val movieTasks by downloadManager.movieTasks.collectAsState()
    val downloadedMovies by downloadManager.downloadedMovies.collectAsState()

    var details by remember { mutableStateOf<MovieDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedSeason by remember { mutableIntStateOf(1) }
    var seasonEpisodes by remember { mutableStateOf<List<MovieEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    var selectedTab by remember(isTv) { mutableIntStateOf(0) }
    var isBookmarked by remember { mutableStateOf(false) }
    var showDownloadOptionsSheet by remember { mutableStateOf(false) }
    var downloadTargetSeason by remember { mutableIntStateOf(1) }
    var downloadTargetEpisode by remember { mutableIntStateOf(1) }
    var downloadTargetEpTitle by remember { mutableStateOf<String?>(null) }

    val savedHistory = remember(id, type) {
        storage.getMovieHistory().firstOrNull { it.id == id && it.type.equals(type, ignoreCase = true) }
    }
    val knownTitle = cachedTitle ?: savedHistory?.title
    val rawPoster = cachedPoster ?: savedHistory?.posterUrl ?: savedHistory?.poster
    val knownPoster = rawPoster?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }
    val knownMeta = listOfNotNull(if (isTv) "Series" else "Movie")
    val resumeSeason = savedHistory?.season ?: 1
    val resumeEpisode = savedHistory?.episode ?: 1
    val hasResume = savedHistory != null && savedHistory.positionMs > 2000L

    fun loadDetails() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val d = MovieRepository.getDetails(id, type)
                details = d
                if (d == null) errorMessage = "Could not load title information."
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load details"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(id, type) {
        loadDetails()
    }

    LaunchedEffect(id, isTv, selectedSeason) {
        if (isTv) {
            isLoadingEpisodes = true
            try {
                seasonEpisodes = MovieRepository.getEpisodes(id, selectedSeason)
            } catch (_: Exception) {}
            isLoadingEpisodes = false
        }
    }

    val tabs = remember(isTv, details?.collection) {
        val hasColl = details?.collection?.isNotEmpty() == true
        if (isTv) listOf("Episodes", "Overview", "Cast", "Similar")
        else if (hasColl) listOf("Overview", "Franchise", "Cast", "Similar")
        else listOf("Overview", "Cast", "Similar")
    }

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(p.pageBrush)) {
            if (isLoading && details == null) {
                MovieDetailLoadingSkeleton(
                    cachedPoster = knownPoster,
                    title = knownTitle,
                    meta = knownMeta,
                    onBack = onBack
                )
            } else if (errorMessage != null && details == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(text = errorMessage ?: "Failed to load", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.accentBrush)
                                .clickable { loadDetails() }
                                .padding(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Text("Retry", color = p.onAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (details != null) {
                val d = details!!
                val similarAnime = remember(d.similar) { d.similar.map { it.toAnime() } }
                val collectionAnime = remember(d.collection) { d.collection.map { it.toAnime() } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    // 1. HERO BACKDROP (Anime DetailScreen parallax backdrop with multi-stop dark gradient)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            val art = d.backdropUrl ?: d.posterUrl
                            if (!art.isNullOrBlank()) {
                                AsyncImage(
                                    model = art,
                                    contentDescription = d.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Top Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xD0000000), Color.Transparent)
                                        )
                                    )
                            )
                            // Ambient Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x33000000))
                            )
                            // Multi-stop cinematic bottom gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0x40000000),
                                                Color(0x99000000),
                                                Color(0xFA000000),
                                                Color(0xFF000000)
                                            )
                                        )
                                    )
                            )

                            // Top Back Button & Share Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x55000000))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        .clickable { onBack() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x55000000))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                        .clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                this.type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Check out ${d.title} on ZENKAI!\nhttps://www.themoviedb.org/$type/$id")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share ${d.title}"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. HERO CARD & METADATA (Poster + Title + Logo + Facts pills)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-45).dp)
                                .padding(horizontal = 18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Floating Poster Card
                                Box(
                                    modifier = Modifier
                                        .width(125.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                        .shadow(12.dp, RoundedCornerShape(14.dp))
                                        .background(p.flatFill)
                                ) {
                                    AsyncImage(
                                        model = d.posterUrl,
                                        contentDescription = d.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Title & Tagline & Logo
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!d.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = d.logoUrl,
                                            contentDescription = d.title,
                                            modifier = Modifier
                                                .heightIn(max = 52.dp)
                                                .fillMaxWidth(),
                                            contentScale = ContentScale.Fit,
                                            alignment = Alignment.BottomStart
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    } else {
                                        Text(
                                            text = d.title,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            lineHeight = 27.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }

                                    if (!d.tagline.isNullOrBlank()) {
                                        Text(
                                            text = "\"${d.tagline}\"",
                                            color = p.textMuted,
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }

                                    // Facts Pills
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (d.rating != null && d.rating > 0.0) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x33E5A00D))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Star, null, tint = p.rating, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", d.rating),
                                                    color = p.rating,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(p.chipBg)
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isTv) "Series" else "Movie",
                                                color = p.text,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (!d.year.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(p.chipBg)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = d.year,
                                                    color = p.textSub,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        val durationLabel = if (isTv) {
                                            val sc = d.seasons.size.coerceAtLeast(1)
                                            "$sc Season${if (sc > 1) "s" else ""}"
                                        } else if (d.runtime != null && d.runtime > 0) {
                                            val h = d.runtime / 60
                                            val m = d.runtime % 60
                                            if (h > 0) "${h}h ${m}m" else "${m}m"
                                        } else null

                                        if (durationLabel != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(p.chipBg)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = durationLabel,
                                                    color = p.textSub,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            // 3. ACTION BUTTONS: Big Hero Play Button + Bookmark + Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Watch / Resume Play Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.accentBrush)
                                        .clickable {
                                            onPlay(id, type, resumeSeason, resumeEpisode)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = p.onAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = if (hasResume) "Resume ${if (isTv) "S$resumeSeason E$resumeEpisode" else "Movie"}" else "Watch Now",
                                            color = p.onAccent,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Bookmark Toggle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(p.chipBg)
                                        .border(1.dp, p.glassBorder, CircleShape)
                                        .clickable { isBookmarked = !isBookmarked },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isBookmarked) p.accentA else p.text,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Download Action Button
                                val currentTaskKey = if (isTv) "$id:s${resumeSeason}_e$resumeEpisode" else "$id:movie"
                                val currentMovieTask = movieTasks[currentTaskKey]
                                val isCurrentDownloading = currentMovieTask != null && (currentMovieTask.status == DownloadStatus.DOWNLOADING || currentMovieTask.status == DownloadStatus.QUEUED)
                                val isMovieDownloaded = remember(downloadedMovies, id, resumeSeason, resumeEpisode) {
                                    downloadManager.isMovieEpisodeDownloaded(id, resumeSeason, resumeEpisode)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(p.chipBg)
                                        .border(1.dp, p.glassBorder, CircleShape)
                                        .clickable {
                                            if (isMovieDownloaded) {
                                                android.widget.Toast.makeText(context, "Already downloaded", android.widget.Toast.LENGTH_SHORT).show()
                                            } else if (isCurrentDownloading) {
                                                val pct = ((currentMovieTask?.progress ?: 0f) * 100f).toInt()
                                                android.widget.Toast.makeText(context, "Download in progress: $pct%", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                downloadTargetSeason = resumeSeason
                                                downloadTargetEpisode = resumeEpisode
                                                downloadTargetEpTitle = if (isTv) "S$resumeSeason E$resumeEpisode" else (details?.title ?: cachedTitle)
                                                showDownloadOptionsSheet = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isCurrentDownloading -> {
                                            CircularProgressIndicator(
                                                progress = { val rawP = currentMovieTask?.progress ?: 0f; if (rawP.isNaN() || !rawP.isFinite()) 0.05f else rawP.coerceIn(0.05f, 1f) },
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.dp,
                                                color = p.accentA,
                                                trackColor = p.glassBorder
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = if (isMovieDownloaded) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                                                contentDescription = "Download",
                                                tint = if (isMovieDownloaded) p.accentA else p.text,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. SLIDING PILL TABS (Episodes | Overview | Cast | Similar)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp)
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
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) p.onAccent else p.textSub
                                    )
                                }
                            }
                        }
                    }

                    // 5. TAB CONTENT
                    val currentTabName = tabs.getOrNull(selectedTab) ?: "Overview"

                    when (currentTabName) {
                        "Episodes" -> {
                            // Season switcher
                            if (d.seasons.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        d.seasons.forEach { s ->
                                            val isCur = s.seasonNumber == selectedSeason
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .then(if (isCur) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                                    .clickable { selectedSeason = s.seasonNumber }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
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

                            if (isLoadingEpisodes) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = p.accentA, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                    }
                                }
                            } else {
                                itemsIndexed(seasonEpisodes, key = { _, ep -> "ep_${ep.episodeNumber}" }) { idx, ep ->
                                    val isCurEp = (selectedSeason == resumeSeason && ep.episodeNumber == resumeEpisode)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onPlay(id, type, selectedSeason, ep.episodeNumber)
                                            }
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Still Thumbnail with Play Badge
                                        Box(
                                            modifier = Modifier
                                                .width(128.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(10.dp))
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
                                                    .size(28.dp)
                                                    .align(Alignment.Center)
                                                    .clip(CircleShape)
                                                    .background(Color(0x99000000)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
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
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Title & Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${ep.episodeNumber}. ${ep.name}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurEp) p.accentA else p.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!ep.airDate.isNullOrBlank() || ep.runtime != null) {
                                                Spacer(Modifier.height(3.dp))
                                                val metaStr = listOfNotNull(ep.airDate, ep.runtime?.let { "${it}m" }).joinToString(" · ")
                                                Text(text = metaStr, fontSize = 11.5.sp, color = p.textMuted)
                                            }
                                            if (ep.overview.isNotBlank()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = ep.overview,
                                                    fontSize = 12.sp,
                                                    color = p.textSub,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        // Episode Download Button
                                        val epKey = "$id:s${selectedSeason}_e${ep.episodeNumber}"
                                        val epTask = movieTasks[epKey]
                                        val isEpDownloading = epTask != null && (epTask.status == DownloadStatus.DOWNLOADING || epTask.status == DownloadStatus.QUEUED)
                                        val isEpDownloaded = remember(downloadedMovies, id, selectedSeason, ep.episodeNumber) {
                                            downloadManager.isMovieEpisodeDownloaded(id, selectedSeason, ep.episodeNumber)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (isEpDownloaded) p.accentA.copy(alpha = 0.15f) else p.chipBg)
                                                .clickable {
                                                    if (isEpDownloaded) {
                                                        android.widget.Toast.makeText(context, "Ep ${ep.episodeNumber} is already downloaded", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else if (isEpDownloading) {
                                                        val epPct = ((epTask?.progress ?: 0f) * 100f).toInt()
                                                        android.widget.Toast.makeText(context, "Ep ${ep.episodeNumber} download in progress ($epPct%)", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        downloadTargetSeason = selectedSeason
                                                        downloadTargetEpisode = ep.episodeNumber
                                                        downloadTargetEpTitle = "S${selectedSeason} E${ep.episodeNumber}${if (ep.name.isNotBlank()) " · ${ep.name}" else ""}"
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
                        }

                        "Overview" -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    // Synopsis
                                    Text(
                                        text = "Synopsis",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.text
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = d.overview.ifBlank { "No synopsis available for this title." },
                                        fontSize = 13.5.sp,
                                        color = p.textSub,
                                        lineHeight = 20.sp
                                    )

                                    Spacer(Modifier.height(18.dp))

                                    // Genres tags
                                    if (d.genres.isNotEmpty()) {
                                        Text(text = "Genres", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.text)
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            d.genres.forEach { g ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(p.chipBg)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text = g.name, fontSize = 12.sp, color = p.text)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(18.dp))
                                    }

                                    // Details Facts Grid
                                    Text(text = "Information", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.text)
                                    Spacer(Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(p.chipBg)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        FactRow("Type", if (isTv) "TV Series" else "Movie", p)
                                        FactRow("Status", d.status ?: "Released", p)
                                        FactRow("Release Year", d.year ?: "Unknown", p)
                                        if (d.runtime != null && d.runtime > 0) {
                                            FactRow("Duration", "${d.runtime} minutes", p)
                                        }
                                        if (d.rating != null && d.rating > 0.0) {
                                            FactRow("Score", "${String.format(Locale.US, "%.1f", d.rating)} / 10", p)
                                        }
                                    }

                                    // YouTube Trailer Button
                                    if (!d.trailerKey.isNullOrBlank()) {
                                        Spacer(Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(p.chipBg)
                                                .clickable {
                                                    val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${d.trailerKey}"))
                                                    context.startActivity(ytIntent)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(Icons.Default.Videocam, null, tint = p.accentA, modifier = Modifier.size(22.dp))
                                                Text("Watch Trailer on YouTube", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
                                            }
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
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                        SectionTitle("Franchise & Sequels", p, "All parts in this collection")
                                        AnimeRow(
                                            list = collectionAnime,
                                            p = p,
                                            onAnimeClick = { animeId ->
                                                val cItem = d.collection.find { it.id == animeId }
                                                onMovieClick?.invoke(animeId, "movie", cItem?.title, cItem?.posterUrl)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        "Cast" -> {
                            if (d.cast.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                        Text("No cast details available", color = p.textMuted, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                items(d.cast) { c ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
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
                                            Text(text = c.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.text)
                                            Spacer(Modifier.height(2.dp))
                                            Text(text = c.character, fontSize = 12.sp, color = p.textMuted)
                                        }
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
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                        SectionTitle("You May Also Like", p, "Similar movies & shows")
                                        AnimeRow(
                                            list = similarAnime,
                                            p = p,
                                            onAnimeClick = { animeId ->
                                                val simItem = d.similar.find { it.id == animeId }
                                                val simType = simItem?.type ?: type
                                                onMovieClick?.invoke(animeId, simType, simItem?.title, simItem?.posterUrl)
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
}

@Composable
private fun FactRow(label: String, value: String, p: HomePalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = p.textMuted, fontSize = 13.sp)
        Text(text = value, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MovieDetailLoadingSkeleton(
    cachedPoster: String?,
    title: String?,
    meta: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val p = rememberHomePalette()

    val fx = rememberInfiniteTransition(label = "movieDetailLoaderFx")
    val sweep by fx.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "sweep")
    val glow by fx.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val drift by fx.animateFloat(-1f, 1f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift")

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.pageBrush)
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
            // Poster card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(2f / 3f)
                    .graphicsLayer {
                        val s = 0.9f + 0.1f * enter.value
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
                        modifier = Modifier.fillMaxSize()
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

            // Title, meta and page skeleton
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = enter.value
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
                    MovieSkeletonBar(Modifier.fillMaxWidth(0.5f).height(22.dp), sweep)
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
                    MovieSkeletonBar(Modifier.width(64.dp).height(22.dp), sweep)
                    MovieSkeletonBar(Modifier.width(48.dp).height(22.dp), sweep)
                    MovieSkeletonBar(Modifier.width(72.dp).height(22.dp), sweep)
                }
                Spacer(Modifier.height(14.dp))
                MovieSkeletonBar(Modifier.fillMaxWidth().height(44.dp), sweep)
                Spacer(Modifier.height(14.dp))
                MovieSkeletonBar(Modifier.fillMaxWidth(0.9f).height(10.dp), sweep)
                Spacer(Modifier.height(8.dp))
                MovieSkeletonBar(Modifier.fillMaxWidth(0.65f).height(10.dp), sweep)
            }
        }

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
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = p.text,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MovieSkeletonBar(modifier: Modifier, sweep: Float) {
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
