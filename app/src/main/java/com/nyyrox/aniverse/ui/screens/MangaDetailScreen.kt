package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nyyrox.aniverse.data.download.DownloadStatus
import com.nyyrox.aniverse.data.manga.AvailableSourceInfo
import com.nyyrox.aniverse.data.manga.MangaChapter
import com.nyyrox.aniverse.data.manga.MangaCharacter
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.manga.MangaInfo
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.data.manga.MangaSource
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MangaDetailScreen(
    vm: MainViewModel,
    mangaId: String,
    onBack: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String) -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
    onNovelClick: ((String) -> Unit)? = null,
    onMangaClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val p = rememberHomePalette()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        onBack()
    }

    val cleanManga = MangaRepository.cleanWeebId(mangaId)
    val cachedManga = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[cleanManga]
    var currentMangaId by remember { mutableStateOf(cachedManga?.resolvedId ?: cleanManga) }
    var info by remember { mutableStateOf<MangaInfo?>(cachedManga?.info) }
    var chapters by remember { mutableStateOf<List<MangaChapter>>(cachedManga?.chapters ?: emptyList()) }
    var loading by remember { mutableStateOf(cachedManga == null) }
    var error by remember { mutableStateOf<String?>(null) }

    var activeSource by remember { mutableStateOf<MangaSource?>(null) }
    var switchingSource by remember { mutableStateOf(false) }

    var isBookmarked by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) } // latest first by default
    var chapterSearch by remember { mutableStateOf("") }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    // Load Manga Details from AniList + Available Sources
    LaunchedEffect(mangaId) {
        val cleanCurrent = MangaRepository.cleanWeebId(mangaId)
        val cached = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[currentMangaId] ?: vm.mangaDetailCache[cleanCurrent]
        if (cached != null) {
            info = cached.info
            chapters = cached.chapters
            currentMangaId = cached.resolvedId
            activeSource = MangaSource.fromId(cached.info.source)
            loading = false
            error = null
        } else {
            loading = true
            error = null
        }

        try {
            isBookmarked = vm.isMangaBookmarked(mangaId) || vm.isMangaBookmarked(currentMangaId) || vm.isMangaBookmarked(cleanCurrent)
            val fallbackTitle = vm.pendingMangaTitleHint?.also { vm.pendingMangaTitleHint = null }
                ?: vm.findKnownManga(mangaId)?.title
                ?: vm.findKnownManga(currentMangaId)?.title
                ?: vm.findKnownManga(cleanCurrent)?.title
                ?: vm.getMangaHistoryItem(mangaId)?.title
                ?: vm.getMangaHistoryItem(currentMangaId)?.title
                ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == currentMangaId }?.title

            val (resolvedId, fetchedInfo, fetchedChapters) = MangaRepository.resolveMangaDetails(mangaId, fallbackTitle)
            val effectiveId = resolvedId.ifBlank { cleanCurrent }
            if (effectiveId.isNotBlank()) {
                currentMangaId = effectiveId
            }

            if (fetchedInfo != null) {
                info = fetchedInfo
                chapters = fetchedChapters
                activeSource = MangaSource.fromId(fetchedInfo.source)
                val cacheEntry = MainViewModel.CachedMangaDetail(currentMangaId, fetchedInfo, fetchedChapters)
                vm.mangaDetailCache[mangaId] = cacheEntry
                vm.mangaDetailCache[cleanCurrent] = cacheEntry
                vm.mangaDetailCache[currentMangaId] = cacheEntry
            } else if (info == null) {
                error = "Unable to load manga details."
            }
        } catch (e: Exception) {
            if (info == null) {
                error = e.message ?: "Failed to load manga details"
            }
        } finally {
            loading = false
        }
    }

    val knownTitle = remember(mangaId, cleanManga) {
        vm.pendingMangaTitleHint
            ?: vm.findKnownManga(mangaId)?.title
            ?: vm.findKnownManga(cleanManga)?.title
            ?: vm.getMangaHistoryItem(cleanManga)?.title
            ?: vm.getMangaHistoryItem(mangaId)?.title
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == cleanManga }?.title
    }
    val cachedPoster = remember(mangaId, cleanManga) {
        vm.findKnownManga(mangaId)?.poster
            ?: vm.findKnownManga(cleanManga)?.poster
            ?: vm.getMangaHistoryItem(cleanManga)?.poster
            ?: vm.getMangaHistoryItem(mangaId)?.poster
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == cleanManga }?.poster
    }

    var missingServer by remember { mutableStateOf<MangaSource?>(null) }
    var switchingSourceId by remember { mutableStateOf<MangaSource?>(null) }

    fun switchToServer(target: MangaSource) {
        if (activeSource == target && chapters.isNotEmpty()) return
        scope.launch {
            switchingSource = true
            switchingSourceId = target
            missingServer = null
            try {
                val avail = info?.availableSources?.find { it.source == target }
                if (avail != null) {
                    val newChapters = withContext(Dispatchers.IO) {
                        MangaRepository.getChaptersForSource(target, avail.matchedId)
                    }
                    if (newChapters.isNotEmpty()) {
                        chapters = newChapters
                        currentMangaId = avail.matchedId
                        activeSource = target
                        info = info?.copy(source = target.id)
                    } else {
                        missingServer = target
                    }
                } else {
                    val searchTitle = info?.title ?: knownTitle ?: ""
                    val repo = MangaRepository.getRepoForSource(target)
                    val results = withContext(Dispatchers.IO) { repo.search(searchTitle) }
                    if (results.isNotEmpty()) {
                        val hit = results.find { it.title.equals(searchTitle, ignoreCase = true) }
                            ?: results.find { it.title.contains(searchTitle, ignoreCase = true) }
                            ?: results.first()
                        val newChapters = withContext(Dispatchers.IO) { repo.getChapters(hit.id) }
                        if (newChapters.isNotEmpty()) {
                            chapters = newChapters
                            currentMangaId = hit.id
                            activeSource = target
                            info = info?.copy(source = target.id)
                        } else {
                            missingServer = target
                        }
                    } else {
                        missingServer = target
                    }
                }
            } catch (_: Exception) {
                missingServer = target
            } finally {
                switchingSource = false
                switchingSourceId = null
            }
        }
    }

    val historyItem: MangaHistoryItem? = remember(vm.mangaHistory, mangaId, currentMangaId) {
        val cleanCurrent = MangaRepository.cleanWeebId(currentMangaId)
        val cleanM = MangaRepository.cleanWeebId(mangaId)
        vm.getMangaHistoryItem(cleanCurrent) ?: vm.getMangaHistoryItem(cleanM) ?: vm.getMangaHistoryItem(currentMangaId) ?: vm.getMangaHistoryItem(mangaId)
    }

    // Filter & Sort Chapters
    val filteredChapters = remember(chapters, chapterSearch, sortAscending) {
        val list = if (chapterSearch.isNotBlank()) {
            chapters.filter {
                it.number.contains(chapterSearch.trim(), ignoreCase = true) ||
                    (it.title != null && it.title.contains(chapterSearch.trim(), ignoreCase = true))
            }
        } else {
            chapters
        }

        if (sortAscending) {
            list.sortedBy { it.number.toFloatOrNull() ?: Float.MAX_VALUE }
        } else {
            list.sortedByDescending { it.number.toFloatOrNull() ?: Float.MIN_VALUE }
        }
    }

    val CHAPTER_CHUNK_SIZE = 50
    var selectedChunkIndex by remember(filteredChapters.size, sortAscending) { mutableIntStateOf(0) }
    val chunkCount = (filteredChapters.size + CHAPTER_CHUNK_SIZE - 1) / CHAPTER_CHUNK_SIZE
    var showChunkDropdown by remember { mutableStateOf(false) }

    val liveTransition = rememberInfiniteTransition(label = "liveDot")
    val redBlinkAlpha by liveTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redBlink"
    )

    val currentChunkChapters = remember(filteredChapters, selectedChunkIndex) {
        if (filteredChapters.size <= CHAPTER_CHUNK_SIZE) {
            filteredChapters
        } else {
            val start = (selectedChunkIndex * CHAPTER_CHUNK_SIZE).coerceIn(0, filteredChapters.size)
            val end = (start + CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
            filteredChapters.subList(start, end)
        }
    }

    val backdropUrl = info?.banner ?: info?.poster

    val isScrolledPastHero by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 180 }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHero) 1f else 0f,
        animationSpec = tween(220),
        label = "mangaTopBarAlpha"
    )

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(Modifier.fillMaxSize()) {
            if (loading && info == null) {
                MangaDetailLoadingSkeleton(
                    cachedPoster = cachedPoster,
                    title = knownTitle,
                    onBack = onBack
                )
            } else if (error != null && info == null) {
                Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Error", color = p.textSub, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.chipBg)
                                .clickable {
                                    loading = true
                                    error = null
                                    scope.launch {
                                        val (rid, fi, fc) = MangaRepository.resolveMangaDetails(mangaId)
                                        if (fi != null) {
                                            info = fi
                                            chapters = fc
                                            currentMangaId = rid
                                        }
                                        loading = false
                                    }
                                }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Retry", color = p.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Hero Artwork Backdrop & Poster Header
                    item("hero_header") {
                        Box(Modifier.fillMaxWidth()) {
                            // Blurred Backdrop
                            if (backdropUrl != null) {
                                DetailBackdropArt(backdropUrl, p, Modifier.matchParentSize())
                            }

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .padding(top = 54.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Poster Tile (Clean, rounded, shadow, no clunky box)
                                    Box(
                                        modifier = Modifier
                                            .width(135.dp)
                                            .height(195.dp)
                                            .shadow(16.dp, RoundedCornerShape(16.dp), clip = false)
                                            .clip(RoundedCornerShape(16.dp))
                                            .shimmer(p)
                                    ) {
                                        AsyncImage(
                                            model = info?.poster,
                                            contentDescription = info?.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(Modifier.width(18.dp))

                                    // Main Title & Metadata
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = info?.title ?: "Manga",
                                            color = p.text,
                                            fontSize = 20.sp,
                                            lineHeight = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!info?.altTitle.isNullOrBlank()) {
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = info?.altTitle ?: "",
                                                color = p.textMuted,
                                                fontSize = 12.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // Rating, Status, Format
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (info?.rating != null) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = p.rating,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    info?.rating ?: "",
                                                    color = p.rating,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text("  ·  ", color = p.textMuted, fontSize = 12.sp)
                                            }

                                            Text(
                                                (info?.type ?: "MANGA").uppercase(),
                                                color = p.textSub,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            if (!info?.status.isNullOrBlank()) {
                                                Text("  ·  ", color = p.textMuted, fontSize = 12.sp)
                                                Text(
                                                    info?.status ?: "",
                                                    color = p.textMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Total Chapters info (AniList + Source)
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (info?.totalChapters != null && info?.totalChapters != 0) {
                                                Text(
                                                    "AniList: ${info?.totalChapters} ch.",
                                                    color = p.textSub,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (chapters.isNotEmpty()) {
                                                    Text("  ·  ", color = p.textMuted, fontSize = 11.sp)
                                                }
                                            }
                                            if (chapters.isNotEmpty()) {
                                                Text(
                                                    "${chapters.size} ch. on ${activeSource?.displayName ?: info?.source ?: "ComicK"}",
                                                    color = p.accentText,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                // Genres Pills
                                if (!info?.genres.isNullOrEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(info?.genres ?: emptyList()) { genre ->
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(p.chipBg)
                                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = genre,
                                                    color = p.textSub,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(18.dp))
                                }

                                // Action Buttons: Start Reading + Bookmark + Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val resumeChapter = historyItem?.chapterId ?: chapters.firstOrNull()?.chapterId
                                    val readLabel = if (historyItem != null) "Resume Ch. ${historyItem.chapterNumber}" else "Start Reading"

                                    // Primary Reading CTA Pill
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(p.accentBrush)
                                            .clickable {
                                                if (resumeChapter != null) {
                                                    onReadChapter(currentMangaId, resumeChapter)
                                                } else if (chapters.isNotEmpty()) {
                                                    onReadChapter(currentMangaId, chapters.first().chapterId)
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = p.onAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = readLabel,
                                            color = p.onAccent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Bookmark Toggle
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(p.chipBg)
                                            .clickable {
                                                val added = vm.toggleMangaBookmark(
                                                    mangaId = currentMangaId,
                                                    title = info?.title ?: "Manga",
                                                    poster = info?.poster,
                                                    type = info?.type ?: "Manga"
                                                )
                                                isBookmarked = added
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) p.rating else p.text,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(p.chipBg)
                                            .clickable {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Read ${info?.title} on Zenkai!")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Manga"))
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = p.text,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Related Anime Adaptation & Next Airing Indicator
                                if (info?.relatedAnimeId != null && onAnimeClick != null) {
                                    Spacer(Modifier.height(10.dp))
                                    val nextEp = info?.relatedAnimeNextAiringEpisode
                                    val nextTime = info?.relatedAnimeNextAiringTime
                                    if (nextEp != null && nextTime != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(p.chipBg)
                                                .clickable {
                                                    info?.relatedAnimeId?.let { onAnimeClick(it) }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .graphicsLayer { alpha = redBlinkAlpha }
                                                        .background(Color(0xFFFF3B30), CircleShape)
                                                )
                                                Text(
                                                    text = "Anime Ep. $nextEp airing in ${formatAiringCountdown(nextTime)}",
                                                    color = p.text,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Watch", color = p.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(2.dp))
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = p.accentText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(p.chipBg)
                                                .clickable {
                                                    info?.relatedAnimeId?.let { onAnimeClick(it) }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = p.rating,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Watch Anime Adaptation",
                                                color = p.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Synopsis / Description
                    if (!info?.description.isNullOrBlank()) {
                        item("synopsis") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Synopsis",
                                    color = p.text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = info?.description ?: "",
                                    color = p.textSub,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                    color = p.textMuted,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                )
                            }
                        }
                    }

                    // 3. Characters Rail (if provided by AniList)
                    if (!info?.characters.isNullOrEmpty()) {
                        item("characters") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Characters",
                                    color = p.text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(info?.characters ?: emptyList()) { ch ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .clip(CircleShape)
                                                    .shimmer(p)
                                            ) {
                                                AsyncImage(
                                                    model = ch.image,
                                                    contentDescription = ch.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = ch.name,
                                                color = p.textSub,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3.5 Related Media Section (Anime adaptations, Light Novels, Spinoffs, Sequels/Prequels)
                    if (!info?.relations.isNullOrEmpty()) {
                        item("relations") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Related Media",
                                    color = p.text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(info?.relations ?: emptyList(), key = { "${it.type}_${it.id}" }) { rel ->
                                        RelatedMediaTile(
                                            item = rel,
                                            p = p,
                                            onClick = {
                                                when {
                                                    rel.type.equals("ANIME", ignoreCase = true) -> onAnimeClick?.invoke(rel.id)
                                                    rel.type.equals("NOVEL", ignoreCase = true) || rel.format.equals("NOVEL", ignoreCase = true) || rel.format.equals("LIGHT_NOVEL", ignoreCase = true) -> onNovelClick?.invoke(rel.title)
                                                    else -> onMangaClick?.invoke(rel.id.toString())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. AVAILABLE SOURCES SECTION (Server Switcher matching zenkai web)
                    item("available_sources") {
                        val avail = info?.availableSources ?: emptyList()
                        val allSources = listOf(
                            MangaSource.COMICK,
                            MangaSource.MANHUA,
                            MangaSource.ASURA,
                            MangaSource.DEMONIC_SCANS,
                            MangaSource.WEEBCENTRAL,
                            MangaSource.MANGADEX
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Server",
                                    color = p.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (switchingSource) {
                                    Spacer(Modifier.width(10.dp))
                                    CircularProgressIndicator(
                                        color = p.accentA,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allSources) { targetSource ->
                                    val isSelected = activeSource == targetSource
                                    val isCurrentSwitching = switchingSourceId == targetSource
                                    val matchedInfo = avail.find { it.source == targetSource }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .then(
                                                if (isSelected) Modifier.background(p.accentBrush)
                                                else Modifier.background(p.chipBg)
                                            )
                                            .clickable(enabled = !switchingSource) {
                                                switchToServer(targetSource)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isCurrentSwitching) {
                                            CircularProgressIndicator(
                                                color = if (isSelected) p.onAccent else p.accentA,
                                                strokeWidth = 1.8.dp,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = targetSource.displayName,
                                            color = if (isSelected) p.onAccent else p.text,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (matchedInfo?.chapterCount != null) {
                                            Text(
                                                text = "(${matchedInfo.chapterCount})",
                                                color = if (isSelected) p.onAccent.copy(alpha = 0.8f) else p.textMuted,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (missingServer != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Couldn't find \"${info?.title ?: knownTitle}\" on ${missingServer?.displayName}.",
                                    color = p.textMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    // 5. CHAPTERS HEADER: Search + Sort Toggle
                    item("chapters_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Chapters (${filteredChapters.size})",
                                color = p.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search filter in chapters
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.chipBg)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, null, tint = p.textMuted, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        BasicTextField(
                                            value = chapterSearch,
                                            onValueChange = {
                                                chapterSearch = it
                                                selectedChunkIndex = 0
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(color = p.text, fontSize = 12.sp),
                                            cursorBrush = SolidColor(p.accentA),
                                            decorationBox = { inner ->
                                                if (chapterSearch.isEmpty()) {
                                                    Text("Search...", color = p.textMuted, fontSize = 12.sp)
                                                }
                                                inner()
                                            }
                                        )
                                    }
                                }

                                // Asc / Desc sort button
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.chipBg)
                                        .clickable {
                                            sortAscending = !sortAscending
                                            selectedChunkIndex = 0
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = p.text,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (sortAscending) "Oldest" else "Newest",
                                        color = p.text,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Chapter chunk tabs (range selector for large chapter lists)
                    if (chunkCount > 1) {
                        item("chapter_chunk_picker") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Dropdown Range Selector
                                Box {
                                    val curStart = selectedChunkIndex * CHAPTER_CHUNK_SIZE + 1
                                    val curEnd = ((selectedChunkIndex + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.chipBg)
                                            .clickable { showChunkDropdown = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Range: Ch. $curStart – $curEnd",
                                            color = p.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = p.textSub,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showChunkDropdown,
                                        onDismissRequest = { showChunkDropdown = false },
                                        modifier = Modifier.background(p.bg)
                                    ) {
                                        for (idx in 0 until chunkCount) {
                                            val s = idx * CHAPTER_CHUNK_SIZE + 1
                                            val e = ((idx + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Ch. $s – $e",
                                                        color = if (idx == selectedChunkIndex) p.accentText else p.text,
                                                        fontWeight = if (idx == selectedChunkIndex) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.5.sp
                                                    )
                                                },
                                                onClick = {
                                                    selectedChunkIndex = idx
                                                    showChunkDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Chunk Prev / Next Steppers
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (selectedChunkIndex > 0) selectedChunkIndex-- },
                                        enabled = selectedChunkIndex > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Range",
                                            tint = if (selectedChunkIndex > 0) p.text else p.textMuted.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "${selectedChunkIndex + 1} / $chunkCount",
                                        color = p.textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { if (selectedChunkIndex < chunkCount - 1) selectedChunkIndex++ },
                                        enabled = selectedChunkIndex < chunkCount - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Next Range",
                                            tint = if (selectedChunkIndex < chunkCount - 1) p.text else p.textMuted.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item("chapter_chunk_tabs") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(chunkCount) { idx ->
                                    val startNum = idx * CHAPTER_CHUNK_SIZE + 1
                                    val endNum = ((idx + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                    val isSelected = selectedChunkIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .then(
                                                if (isSelected) Modifier.background(p.accentBrush)
                                                else Modifier.background(p.chipBg)
                                            )
                                            .clickable { selectedChunkIndex = idx }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$startNum – $endNum",
                                            color = if (isSelected) p.onAccent else p.textSub,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. CHAPTERS LIST
                    if (filteredChapters.isEmpty()) {
                        item("empty_chapters") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (switchingSource) "Loading chapters..." else "No chapters found for this source.",
                                    color = p.textMuted,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    } else {
                        itemsIndexed(currentChunkChapters, key = { _, ch -> ch.chapterId }) { _, chapter ->
                            val isRead = historyItem != null && historyItem.chapterId == chapter.chapterId
                            val task = downloadTasks.find { it.chapterId == chapter.chapterId }
                            val isDownloaded = task?.status == DownloadStatus.COMPLETED

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onReadChapter(currentMangaId, chapter.chapterId) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Chapter ${chapter.number}",
                                            color = if (isRead) p.textMuted else p.text,
                                            fontSize = 14.sp,
                                            fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold
                                        )
                                        if (isRead) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "• Read",
                                                color = p.textMuted,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    if (!chapter.title.isNullOrBlank() && chapter.title != chapter.number) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = chapter.title,
                                            color = p.textSub,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!chapter.releaseDate.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = chapter.releaseDate,
                                            color = p.textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Download Icon Action Button (No border box)
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) p.rating else p.textMuted,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            if (!isDownloaded && task == null) {
                                                vm.downloadMangaChapter(
                                                    mangaId = currentMangaId,
                                                    mangaTitle = info?.title ?: "Manga",
                                                    mangaPoster = info?.poster,
                                                    chapter = chapter
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }

                    // Bottom Padding
                    item("bottom_pad") {
                        Spacer(Modifier.height(90.dp))
                    }
                }
            }

            // Smooth Sticky Top Bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Top scrim for contrast when at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x99000000), Color(0x33000000), Color.Transparent)
                            )
                        )
                )

                // Background fade on scroll
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = topBarAlpha }
                        .background(p.bg.copy(alpha = 0.96f))
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(p.chipBg)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = p.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isScrolledPastHero) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = info?.title ?: "",
                            color = p.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = topBarAlpha }
                        )
                    }
                }
            }
        }
    }
}

// ── Detail Blurred Backdrop Art ──────────────────────────────────────────────

@Composable
private fun DetailBackdropArt(url: String, p: HomePalette, modifier: Modifier) {
    val ctx = LocalContext.current
    val req = remember(url) { ImageRequest.Builder(ctx).data(url).size(120, 180).build() }

    Box(modifier) {
        AsyncImage(
            model = req,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (p.dark) 0.35f else 0.25f
                }
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to p.gradTop.copy(alpha = 0.85f),
                        0.45f to p.gradMid.copy(alpha = 0.95f),
                        1f to p.gradBot
                    )
                )
        )
    }
}

@Composable
private fun RelatedMediaTile(
    item: com.nyyrox.aniverse.data.MediaRelation,
    p: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(168.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmer(p)
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Relation Type Badge (top-start)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xB3000000))
                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
            ) {
                Text(
                    text = item.displayRelationType.uppercase(),
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Media Type Badge (bottom-end)
            val mediaType = when {
                item.type.equals("ANIME", ignoreCase = true) -> "ANIME"
                item.type.equals("NOVEL", ignoreCase = true) || item.format.equals("NOVEL", ignoreCase = true) || item.format.equals("LIGHT_NOVEL", ignoreCase = true) -> "NOVEL"
                else -> "MANGA"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = mediaType,
                    color = p.accentText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.displayTitle,
            color = p.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        val meta = item.format ?: item.type
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                color = p.textSub,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatAiringCountdown(airingAtSeconds: Long): String {
    val nowSeconds = System.currentTimeMillis() / 1000
    val diff = airingAtSeconds - nowSeconds
    if (diff <= 0) return "Airing soon"
    val days = diff / 86400
    val hours = (diff % 86400) / 3600
    val minutes = (diff % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
private fun MangaDetailLoadingSkeleton(
    cachedPoster: String?,
    title: String?,
    meta: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val p = rememberHomePalette()

    val fx = rememberInfiniteTransition(label = "mangaDetailLoaderFx")
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
                    MangaSkeletonBar(Modifier.fillMaxWidth(0.5f).height(22.dp), sweep)
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
                    MangaSkeletonBar(Modifier.width(64.dp).height(22.dp), sweep)
                    MangaSkeletonBar(Modifier.width(48.dp).height(22.dp), sweep)
                    MangaSkeletonBar(Modifier.width(72.dp).height(22.dp), sweep)
                }
                Spacer(Modifier.height(14.dp))
                MangaSkeletonBar(Modifier.fillMaxWidth().height(44.dp), sweep)
                Spacer(Modifier.height(14.dp))
                MangaSkeletonBar(Modifier.fillMaxWidth(0.9f).height(10.dp), sweep)
                Spacer(Modifier.height(8.dp))
                MangaSkeletonBar(Modifier.fillMaxWidth(0.65f).height(10.dp), sweep)
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
private fun MangaSkeletonBar(modifier: Modifier, sweep: Float) {
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
