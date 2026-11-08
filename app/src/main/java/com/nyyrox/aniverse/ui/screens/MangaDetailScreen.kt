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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.nyyrox.aniverse.data.manga.ServerShowItem
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
import coil3.request.crossfade
import com.nyyrox.aniverse.data.download.DownloadStatus
import com.nyyrox.aniverse.data.manga.AvailableSourceInfo
import com.nyyrox.aniverse.data.manga.MatchedEdition
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
    onNovelClick: ((String, String?, String?) -> Unit)? = null,
    onMangaClick: ((String, String?, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val p = rememberHomePalette()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val cleanManga = MangaRepository.cleanWeebId(mangaId)
    val cachedManga = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[cleanManga]

    val initialTitle = remember(mangaId) {
        vm.pendingMangaTitleHint
            ?: vm.findKnownManga(mangaId)?.title
            ?: vm.findKnownManga(cleanManga)?.title
            ?: vm.getMangaHistoryItem(cleanManga)?.title
            ?: vm.getMangaHistoryItem(mangaId)?.title
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == cleanManga }?.title
    }
    val initialPoster = remember(mangaId) {
        vm.pendingMangaPosterHint
            ?: vm.findKnownManga(mangaId)?.poster
            ?: vm.findKnownManga(cleanManga)?.poster
            ?: vm.getMangaHistoryItem(cleanManga)?.poster
            ?: vm.getMangaHistoryItem(mangaId)?.poster
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == cleanManga }?.poster
    }

    val isExplicitServerShow = remember(mangaId, cleanManga) {
        cleanManga.startsWith("wc_") || cleanManga.startsWith("ck_") ||
        cleanManga.startsWith("mh_") || cleanManga.startsWith("as_") ||
        cleanManga.startsWith("ds_") || cleanManga.startsWith("md_")
    }

    var selectedShow by remember {
        mutableStateOf<ServerShowItem?>(
            if (isExplicitServerShow) {
                ServerShowItem(
                    id = cleanManga,
                    title = initialTitle ?: "Manga",
                    poster = initialPoster,
                    source = MangaRepository.getSourceForId(cleanManga)
                )
            } else null
        )
    }

    var foundServerShows by remember { mutableStateOf<List<ServerShowItem>>(emptyList()) }
    var currentMangaId by remember { mutableStateOf(cachedManga?.resolvedId ?: cleanManga) }
    var info by remember { mutableStateOf<MangaInfo?>(cachedManga?.info) }
    var chapters by remember { mutableStateOf<List<MangaChapter>>(cachedManga?.chapters ?: emptyList()) }
    var loading by remember { mutableStateOf(cachedManga == null) }
    var error by remember { mutableStateOf<String?>(null) }

    var activeSource by remember { mutableStateOf<MangaSource?>(cachedManga?.info?.let { MangaSource.fromId(it.source) }) }
    var switchingSource by remember { mutableStateOf(false) }
    var serversChecking by remember { mutableStateOf(!isExplicitServerShow) }
    var serversCheckedCount by remember { mutableIntStateOf(0) }
    var availableSourcesList by remember { mutableStateOf<List<AvailableSourceInfo>>(cachedManga?.info?.availableSources ?: emptyList()) }

    var isBookmarked by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) } // latest first by default
    var chapterSearch by remember { mutableStateOf("") }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    BackHandler(enabled = true) {
        if (selectedShow != null && foundServerShows.isNotEmpty()) {
            selectedShow = null
        } else {
            onBack()
        }
    }

    // Load Manga Details from AniList + Available Sources
    LaunchedEffect(mangaId) {
        val cleanCurrent = MangaRepository.cleanWeebId(mangaId)
        val cached = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[currentMangaId] ?: vm.mangaDetailCache[cleanCurrent]
        if (cached != null) {
            info = cached.info
            chapters = cached.chapters
            currentMangaId = cached.resolvedId
            activeSource = MangaSource.fromId(cached.info.source)
            availableSourcesList = cached.info.availableSources
            if (isExplicitServerShow && selectedShow == null) {
                selectedShow = ServerShowItem(
                    id = cached.resolvedId,
                    title = cached.info.title,
                    poster = cached.info.poster ?: initialPoster,
                    source = activeSource ?: MangaSource.WEEBCENTRAL,
                    chapterCount = cached.chapters.size
                )
            }
            loading = false
            error = null
        } else {
            loading = true
            error = null
        }

        val fallbackTitle = vm.pendingMangaTitleHint?.also { vm.pendingMangaTitleHint = null }
            ?: initialTitle
            ?: vm.findKnownManga(mangaId)?.title
            ?: vm.findKnownManga(currentMangaId)?.title
            ?: vm.findKnownManga(cleanCurrent)?.title
            ?: vm.getMangaHistoryItem(mangaId)?.title
            ?: vm.getMangaHistoryItem(currentMangaId)?.title
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == currentMangaId }?.title

        val targetSearchTitle = fallbackTitle ?: initialTitle ?: ""

        // Stream server shows concurrently from frame 0
        if (targetSearchTitle.isNotBlank() && !isExplicitServerShow) {
            serversChecking = true
            serversCheckedCount = 0
            launch {
                try {
                    MangaRepository.findServerShowsProgressive(
                        title = targetSearchTitle,
                        altTitles = listOfNotNull(info?.altTitle),
                        onShowFound = { show ->
                            val updated = (foundServerShows.filter { it.id != show.id } + show)
                            val order = MangaRepository.PREFERRED_SOURCE_ORDER
                            foundServerShows = updated.sortedWith(
                                compareByDescending<ServerShowItem> { it.score }
                                    .thenBy { item ->
                                        val idx = order.indexOf(item.source)
                                        if (idx < 0) 999 else idx
                                    }
                            )
                            val existingAvail = availableSourcesList.find { it.source == show.source }
                            if (existingAvail == null) {
                                availableSourcesList = (availableSourcesList + AvailableSourceInfo(
                                    source = show.source,
                                    matchedId = show.id,
                                    matchedTitle = show.title,
                                    chapterCount = show.chapterCount,
                                    latestChapter = show.latestChapter
                                )).sortedBy { s ->
                                    val idx = order.indexOf(s.source)
                                    if (idx < 0) 999 else idx
                                }
                                info = info?.copy(availableSources = availableSourcesList)
                            }
                        },
                        onProgress = { checked, _ ->
                            serversCheckedCount = checked
                        }
                    )
                } catch (_: Exception) {}
                finally {
                    serversChecking = false
                }
            }
        }

        try {
            isBookmarked = vm.isMangaBookmarked(mangaId) || vm.isMangaBookmarked(currentMangaId) || vm.isMangaBookmarked(cleanCurrent)

            val (resolvedId, fetchedInfo, fetchedChapters) = MangaRepository.resolveMangaDetails(mangaId, fallbackTitle)
            val effectiveId = resolvedId.ifBlank { cleanCurrent }
            if (effectiveId.isNotBlank()) {
                currentMangaId = effectiveId
            }

            if (fetchedInfo != null) {
                info = fetchedInfo
                chapters = fetchedChapters
                activeSource = MangaSource.fromId(fetchedInfo.source)
                availableSourcesList = fetchedInfo.availableSources
                if (isExplicitServerShow && selectedShow == null) {
                    selectedShow = ServerShowItem(
                        id = effectiveId,
                        title = fetchedInfo.title,
                        poster = fetchedInfo.poster ?: initialPoster,
                        source = activeSource ?: MangaSource.WEEBCENTRAL,
                        chapterCount = fetchedChapters.size,
                        rating = fetchedInfo.rating
                    )
                }
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

    val knownTitle = initialTitle
    val cachedPoster = initialPoster

    var missingServer by remember { mutableStateOf<MangaSource?>(null) }
    var switchingSourceId by remember { mutableStateOf<MangaSource?>(null) }

    fun switchToEdition(edition: MatchedEdition, target: MangaSource) {
        if (currentMangaId == edition.id && chapters.isNotEmpty()) return
        scope.launch {
            switchingSource = true
            switchingSourceId = target
            missingServer = null
            try {
                val newChapters = withContext(Dispatchers.IO) {
                    MangaRepository.getChaptersForSource(target, edition.id)
                }
                if (newChapters.isNotEmpty()) {
                    chapters = newChapters
                    currentMangaId = edition.id
                    activeSource = target
                    info = info?.copy(source = target.id)
                } else {
                    missingServer = target
                }
            } catch (_: Exception) {
                missingServer = target
            } finally {
                switchingSource = false
                switchingSourceId = null
            }
        }
    }

    fun switchToServer(target: MangaSource) {
        if (activeSource == target && chapters.isNotEmpty()) return
        scope.launch {
            switchingSource = true
            switchingSourceId = target
            missingServer = null
            try {
                val avail = availableSourcesList.find { it.source == target } ?: info?.availableSources?.find { it.source == target }
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
                    cachedPoster = initialPoster ?: info?.poster,
                    title = initialTitle ?: info?.title,
                    onBack = onBack
                )
            } else if (selectedShow == null) {
                MangaServerFinderView(
                    title = initialTitle ?: info?.title ?: "Manga",
                    poster = initialPoster ?: info?.poster,
                    serversChecking = serversChecking,
                    serversCheckedCount = serversCheckedCount,
                    totalServers = 6,
                    foundShows = foundServerShows,
                    onSelectShow = { show ->
                        selectedShow = show
                        currentMangaId = show.id
                        activeSource = show.source
                        scope.launch {
                            loading = true
                            try {
                                val chs = MangaRepository.getChaptersForSource(show.source, show.id)
                                if (chs.isNotEmpty()) {
                                    chapters = chs
                                }
                            } catch (_: Exception) {
                            } finally {
                                loading = false
                            }
                        }
                    },
                    onBack = onBack,
                    p = p
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
                                val displayPoster = selectedShow?.poster ?: info?.poster ?: initialPoster
                                val displayTitle = selectedShow?.title ?: info?.title ?: initialTitle ?: "Manga"

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
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(displayPoster)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = displayTitle,
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
                                            text = displayTitle,
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

                                        Spacer(Modifier.height(8.dp))

                                        // Subtitle Stats Row (Source Chapters · AniList count · Rating)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (chapters.isNotEmpty()) {
                                                Text(
                                                    text = "${chapters.size} chapters",
                                                    color = p.accentText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text("·", color = p.textMuted, fontSize = 12.sp)
                                            }
                                            if (info?.totalChapters != null && info?.totalChapters != 0) {
                                                Text(
                                                    text = "AniList: ${info?.totalChapters} ch.",
                                                    color = p.textSub,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text("·", color = p.textMuted, fontSize = 12.sp)
                                            }
                                            val curInfo = info
                                            val displayRating = remember(curInfo?.rating, curInfo?.score) {
                                                val sc = curInfo?.score
                                                val r = curInfo?.rating
                                                if (sc != null && sc > 0) {
                                                    val s = if (sc > 10) sc / 10.0 else sc
                                                    String.format(java.util.Locale.US, "%.1f", s)
                                                } else if (!r.isNullOrBlank()) {
                                                    val clean = r.trim().removeSuffix("%").trim()
                                                    val num = clean.toDoubleOrNull()
                                                    if (num != null && num > 0) {
                                                        val s = if (num > 10) num / 10.0 else num
                                                        String.format(java.util.Locale.US, "%.1f", s)
                                                    } else clean
                                                } else null
                                            }
                                            if (displayRating != null) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(p.rating.copy(alpha = if (p.dark) 0.18f else 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = p.rating,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = displayRating,
                                                        color = p.rating,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // Next Airing Countdown with pulsing live dot
                                        val nextEp = info?.relatedAnimeNextAiringEpisode
                                        val nextTime = info?.relatedAnimeNextAiringTime
                                        if (nextEp != null && nextTime != null) {
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x22FF3B30))
                                                    .clickable { info?.relatedAnimeId?.let { onAnimeClick?.invoke(it) } }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .graphicsLayer { alpha = redBlinkAlpha }
                                                        .background(Color(0xFFFF3B30), CircleShape)
                                                )
                                                Text(
                                                    text = "Anime Ep. $nextEp airing in ${formatAiringCountdown(nextTime)}",
                                                    color = Color(0xFFFF5252),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
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

                                // Action Buttons: Start Reading + Latest + All Servers + Bookmark
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = readLabel,
                                            color = p.onAccent,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    // Latest Chapter CTA Pill (if available and not currently on it)
                                    val latestChapter = chapters.lastOrNull()
                                    if (latestChapter != null && historyItem == null && chapters.size > 1) {
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(p.chipBg)
                                                .clickable {
                                                    onReadChapter(currentMangaId, latestChapter.chapterId)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Latest · Ch. ${latestChapter.number}",
                                                color = p.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
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
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
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

                                // SERVER Section (matching zenkai web Image 1)
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SERVER",
                                        color = p.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    if (switchingSource) {
                                        CircularProgressIndicator(
                                            color = p.accentA,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))

                                val allSources = MangaRepository.PREFERRED_SOURCE_ORDER
                                val avail = availableSourcesList.ifEmpty { info?.availableSources ?: emptyList() }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(allSources) { targetSource ->
                                        val isSelected = activeSource == targetSource
                                        val isCurrentSwitching = switchingSourceId == targetSource

                                        val serverPillBg = if (isSelected) {
                                            if (p.dark) Color.White else Color(0xFF0D0A0B)
                                        } else {
                                            p.chipBg
                                        }
                                        val serverPillText = if (isSelected) {
                                            if (p.dark) Color.Black else Color.White
                                        } else {
                                            p.text
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(serverPillBg)
                                                .clickable(enabled = !switchingSource) {
                                                    switchToServer(targetSource)
                                                }
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isCurrentSwitching) {
                                                CircularProgressIndicator(
                                                    color = if (isSelected) serverPillText else p.accentA,
                                                    strokeWidth = 1.8.dp,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                            Text(
                                                text = targetSource.displayName,
                                                color = serverPillText,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Multiple editions row (e.g. ComicK regular + color)
                                val activeMatched = avail.find { it.source == activeSource }
                                val editions = activeMatched?.editions ?: emptyList()
                                if (editions.size > 1) {
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(editions) { ed ->
                                            val isEditionActive = currentMangaId == ed.id ||
                                                MangaRepository.cleanWeebId(currentMangaId) == MangaRepository.cleanWeebId(ed.id)
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .then(
                                                        if (isEditionActive) Modifier.background(p.accentBrush)
                                                        else Modifier.background(p.chipBg)
                                                    )
                                                    .clickable(enabled = !switchingSource) {
                                                        activeSource?.let { src -> switchToEdition(ed, src) }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = ed.title,
                                                    color = if (isEditionActive) p.onAccent else p.text,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isEditionActive) FontWeight.Bold else FontWeight.Normal
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
                                                    rel.type.equals("NOVEL", ignoreCase = true) || rel.format.equals("NOVEL", ignoreCase = true) || rel.format.equals("LIGHT_NOVEL", ignoreCase = true) -> onNovelClick?.invoke(rel.title, rel.title, rel.cover)
                                                    else -> onMangaClick?.invoke(rel.id.toString(), rel.title, rel.cover)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. CHAPTERS HEADER: Eyebrow + Title + Search + Sort Toggle (matching zenkai web Image 1)
                    item("chapters_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "READ ONLINE",
                                color = p.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Chapters",
                                    color = p.text,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Search filter in chapters
                                    Box(
                                        modifier = Modifier
                                            .width(135.dp)
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
                                                        Text("Search chapter…", color = p.textMuted, fontSize = 12.sp)
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
                    }

                    // Chapter chunk tabs (range pills with actual chapter numbers & solid white active pill)
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
                                    val curChunkStart = selectedChunkIndex * CHAPTER_CHUNK_SIZE
                                    val curChunkEnd = ((selectedChunkIndex + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                    val curFirstCh = filteredChapters.getOrNull(curChunkStart)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                    val curLastCh = filteredChapters.getOrNull(curChunkEnd - 1)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                    val currentLabel = if (curFirstCh != null && curLastCh != null) "$curFirstCh–$curLastCh" else "${curChunkStart + 1}–$curChunkEnd"

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.chipBg)
                                            .clickable { showChunkDropdown = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Range: $currentLabel",
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
                                            val s = idx * CHAPTER_CHUNK_SIZE
                                            val e = ((idx + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                            val fCh = filteredChapters.getOrNull(s)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                            val lCh = filteredChapters.getOrNull(e - 1)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                            val itemLabel = if (fCh != null && lCh != null) "$fCh–$lCh" else "${s + 1}–$e"
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = itemLabel,
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
                                    val chunkStart = idx * CHAPTER_CHUNK_SIZE
                                    val chunkEnd = ((idx + 1) * CHAPTER_CHUNK_SIZE).coerceAtMost(filteredChapters.size)
                                    val firstCh = filteredChapters.getOrNull(chunkStart)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                    val lastCh = filteredChapters.getOrNull(chunkEnd - 1)?.number?.let { n -> if (n.endsWith(".0")) n.dropLast(2) else n }
                                    val pillLabel = if (firstCh != null && lastCh != null) "$firstCh–$lastCh" else "${chunkStart + 1}–$chunkEnd"
                                    val isSelected = selectedChunkIndex == idx

                                    val chunkPillBg = if (isSelected) {
                                        if (p.dark) Color.White else Color(0xFF0D0A0B)
                                    } else {
                                        p.chipBg
                                    }
                                    val chunkPillText = if (isSelected) {
                                        if (p.dark) Color.Black else Color.White
                                    } else {
                                        p.text
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(chunkPillBg)
                                            .clickable { selectedChunkIndex = idx }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = pillLabel,
                                            color = chunkPillText,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Batch Download Controls Row
                        item("manga_batch_downloads") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val effectivePoster = info?.poster ?: vm.pendingMangaPosterHint.orEmpty()
                                val effectiveTitle = info?.title ?: vm.pendingMangaTitleHint ?: "Manga"

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (p.dark) Color(0x18FFFFFF) else p.flatFill)
                                        .border(BorderStroke(1.dp, if (p.dark) Color(0x22FFFFFF) else p.glassBorder), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val targetChapters = filteredChapters.filter { ch ->
                                                !vm.downloadManager.isMangaChapterDownloaded(currentMangaId, ch.chapterId)
                                            }.take(10)
                                            if (targetChapters.isNotEmpty()) {
                                                vm.downloadMangaChaptersBatch(currentMangaId, effectiveTitle, effectivePoster, targetChapters)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = p.text, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Download 10", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = p.text)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (p.dark) Color(0x18FFFFFF) else p.flatFill)
                                        .border(BorderStroke(1.dp, if (p.dark) Color(0x22FFFFFF) else p.glassBorder), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val targetChapters = filteredChapters.filter { ch ->
                                                !vm.downloadManager.isMangaChapterDownloaded(currentMangaId, ch.chapterId)
                                            }
                                            if (targetChapters.isNotEmpty()) {
                                                vm.downloadMangaChaptersBatch(currentMangaId, effectiveTitle, effectivePoster, targetChapters)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DownloadDone, contentDescription = null, tint = p.text, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Download All (${filteredChapters.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = p.text)
                                    }
                                }
                            }
                        }
                    }

                    // 6. CHAPTERS LIST (2-Column Grid matching zenkai web Image 1)
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
                        items(currentChunkChapters.chunked(2), key = { pair -> pair.first().chapterId }) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (chapter in pair) {
                                    val isRead = historyItem != null && historyItem.chapterId == chapter.chapterId
                                    val isDownloaded = remember(downloadTasks, chapter.chapterId) {
                                        vm.downloadManager.isMangaChapterDownloaded(currentMangaId, chapter.chapterId)
                                    }
                                    val cleanM = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(currentMangaId)
                                    val cleanCh = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chapter.chapterId)
                                    val isDownloading = remember(downloadTasks, chapter.chapterId) {
                                        downloadTasks.any {
                                            (it.itemId == currentMangaId || it.itemId == cleanM) &&
                                            (it.chapterId == chapter.chapterId || it.chapterId == cleanCh) &&
                                            it.status == DownloadStatus.DOWNLOADING
                                        }
                                    }

                                    val cardBg = if (p.dark) Color(0xFF14161E) else p.flatFill
                                    val cardBorder = if (p.dark) Color(0x18FFFFFF) else p.glassBorder

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cardBg)
                                            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(10.dp))
                                            .clickable { onReadChapter(currentMangaId, chapter.chapterId) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                val cleanNum = if (chapter.number.endsWith(".0")) chapter.number.dropLast(2) else chapter.number
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Ch. $cleanNum",
                                                        color = if (isRead) p.textMuted else p.text,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (isRead) {
                                                        Spacer(Modifier.width(5.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(p.accentA)
                                                        )
                                                    }
                                                }
                                                val subText = if (!chapter.title.isNullOrBlank() && chapter.title != chapter.number) {
                                                    chapter.title
                                                } else {
                                                    "Chapter $cleanNum"
                                                }
                                                Text(
                                                    text = subText,
                                                    color = p.textMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Compact download button
                                            if (isDownloading) {
                                                CircularProgressIndicator(
                                                    color = p.accentA,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp).padding(1.dp)
                                                )
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        if (!isDownloaded) {
                                                            vm.downloadMangaChapter(
                                                                mangaId = currentMangaId,
                                                                mangaTitle = info?.title ?: vm.pendingMangaTitleHint ?: "Manga",
                                                                mangaPoster = info?.poster ?: vm.pendingMangaPosterHint,
                                                                chapter = chapter
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                                                        contentDescription = if (isDownloaded) "Downloaded" else "Download",
                                                        tint = if (isDownloaded) p.rating else p.textMuted.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
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
private fun MangaServerFinderView(
    title: String,
    poster: String?,
    serversChecking: Boolean,
    serversCheckedCount: Int,
    totalServers: Int,
    foundShows: List<ServerShowItem>,
    onSelectShow: (ServerShowItem) -> Unit,
    onBack: () -> Unit,
    p: HomePalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
    ) {
        // Top Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(p.chipBg)
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = p.text,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Back", color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Header Section (Poster + Title + Status)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!poster.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .aspectRatio(2f / 3f)
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.skeleton)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(poster)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "READ THE MANGA",
                    color = p.accentText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    color = p.text,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (serversChecking) {
                        CircularProgressIndicator(
                            color = p.accentA,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (serversCheckedCount == 0) "Searching across $totalServers servers…" else "Checked $serversCheckedCount of $totalServers servers…",
                            color = p.textMuted,
                            fontSize = 12.sp
                        )
                    } else if (foundShows.isNotEmpty()) {
                        val distinctServers = foundShows.map { it.source }.distinct().size
                        Text(
                            text = "Found on $distinctServers of $totalServers servers. Pick one to open it.",
                            color = p.textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "No server has it under this name.",
                            color = p.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 2-Column Grid of Exact Shows Found
        if (foundShows.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(foundShows, key = { "${it.source.id}_${it.id}" }) { show ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectShow(show) }
                    ) {
                        // Poster Card (NO borders, clean glass shadow)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(p.flatFill)
                        ) {
                            val cardImg = show.poster ?: poster
                            if (!cardImg.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(cardImg)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = show.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Gradient shadow at bottom of poster
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.6f to Color.Transparent,
                                            1f to Color(0x99000000)
                                        )
                                    )
                            )
                            // Top Left Rating Badge (★ 8.7) - Matching Image 3
                            if (!show.rating.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xD9000000))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "★",
                                            color = Color(0xFFFF4757),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = show.rating,
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            // Top Right Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xCC000000))
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "MANGA",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Title
                        Text(
                            text = show.title,
                            color = p.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(2.dp))

                        // Server Label / Caption
                        Text(
                            text = show.caption ?: show.source.displayName,
                            color = p.accentText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else if (serversChecking) {
            // Skeleton while servers are still checking
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(4) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(14.dp))
                                .shimmer(p)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(p)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(p)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No shows found across all servers.",
                    color = p.textMuted,
                    fontSize = 14.sp
                )
            }
        }
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cachedPoster)
                    .crossfade(true)
                    .build(),
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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cachedPoster)
                            .crossfade(true)
                            .build(),
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
