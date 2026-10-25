package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.compose.material3.Icon
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
    onAnimeClick: ((Int) -> Unit)? = null
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

    // Switch Chapter Source
    fun selectSource(sourceInfo: AvailableSourceInfo) {
        if (activeSource == sourceInfo.source && chapters.isNotEmpty()) return
        scope.launch {
            switchingSource = true
            activeSource = sourceInfo.source
            try {
                val newChapters = withContext(Dispatchers.IO) {
                    MangaRepository.getChaptersForSource(sourceInfo.source, sourceInfo.matchedId)
                }
                if (newChapters.isNotEmpty()) {
                    chapters = newChapters
                    currentMangaId = sourceInfo.matchedId
                    info = info?.copy(source = sourceInfo.source.id)
                }
            } catch (_: Exception) {}
            switchingSource = false
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

    val backdropUrl = info?.banner ?: info?.poster

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(Modifier.fillMaxSize()) {
            if (loading && info == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accentA, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                }
            } else if (error != null && info == null) {
                Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Error", color = p.textSub, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier
                                .homeGlass(p, radius = 999.dp)
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

                                        // Total Chapters info
                                        if (info?.totalChapters != null && info?.totalChapters != 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "${info?.totalChapters} total chapters",
                                                color = p.textMuted,
                                                fontSize = 12.sp
                                            )
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
                                                    .homeGlass(p, radius = 999.dp)
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
                                            .homeGlass(p, radius = 999.dp)
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
                                            .homeGlass(p, radius = 999.dp)
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

                                // Related Anime Adaptation Button (if available)
                                if (info?.relatedAnimeId != null && onAnimeClick != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .homeGlass(p, radius = 14.dp)
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

                    // 4. AVAILABLE SOURCES SECTION (Interactive Liquid Glass Switcher)
                    item("available_sources") {
                        val avail = info?.availableSources ?: emptyList()
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
                                    text = "Available Sources",
                                    color = p.text,
                                    fontSize = 16.sp,
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

                            if (avail.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(avail) { srcInfo ->
                                        val isSelected = activeSource == srcInfo.source
                                        Row(
                                            modifier = Modifier
                                                .then(
                                                    if (isSelected) {
                                                        Modifier
                                                            .clip(CircleShape)
                                                            .background(p.accentBrush)
                                                    } else {
                                                        Modifier.homeGlass(p, radius = 999.dp)
                                                    }
                                                )
                                                .clickable { selectSource(srcInfo) }
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = srcInfo.source.displayName,
                                                color = if (isSelected) p.onAccent else p.text,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            if (srcInfo.chapterCount != null) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "(${srcInfo.chapterCount} ch)",
                                                    color = if (isSelected) p.onAccent.copy(alpha = 0.8f) else p.textMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Source: ${activeSource?.displayName ?: "Default"}",
                                    color = p.textMuted,
                                    fontSize = 12.5.sp
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
                                text = "Chapters (${chapters.size})",
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
                                        .homeGlass(p, radius = 999.dp)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, null, tint = p.textMuted, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        BasicTextField(
                                            value = chapterSearch,
                                            onValueChange = { chapterSearch = it },
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
                                        .homeGlass(p, radius = 999.dp)
                                        .clickable { sortAscending = !sortAscending }
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

                    // 6. CHAPTERS LIST
                    if (chapters.isEmpty()) {
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
                        itemsIndexed(filteredChapters, key = { _, ch -> ch.chapterId }) { _, chapter ->
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

            // Top Sticky Back Button
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier
                        .homeGlass(p, radius = 999.dp)
                        .clickable { onBack() }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = p.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Detail Blurred Backdrop Art ──────────────────────────────────────────────

@Composable
private fun DetailBackdropArt(url: String, p: HomePalette, modifier: Modifier) {
    val ctx = LocalContext.current
    val req = remember(url) { ImageRequest.Builder(ctx).data(url).size(96, 144).build() }

    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0.35f to Color.Black, 0.90f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        AsyncImage(
            model = req,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(26.dp)
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.25f
                    alpha = if (p.dark) 0.50f else 0.40f
                }
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(p.gradTop.copy(alpha = 0.35f), Color.Transparent))
                )
        )
    }
}
