package com.nyyrox.aniverse.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import com.nyyrox.aniverse.data.CurrentActivityMode
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nyyrox.aniverse.data.manga.MangaChapter
import com.nyyrox.aniverse.data.manga.MangaInfo
import com.nyyrox.aniverse.data.manga.MangaReaderMode
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderScreen(
    vm: MainViewModel,
    mangaId: String,
    chapterId: String,
    onBack: () -> Unit,
    onSelectChapter: (newChapterId: String) -> Unit
) {
    val context = LocalContext.current

    val cleanManga = remember(mangaId) { MangaRepository.cleanWeebId(mangaId) }
    val initialCached = remember(cleanManga, mangaId) {
        vm.mangaDetailCache[cleanManga] ?: vm.mangaDetailCache[mangaId]
    }

    var info by remember { mutableStateOf<MangaInfo?>(initialCached?.info) }
    var chapters by remember { mutableStateOf<List<MangaChapter>>(initialCached?.chapters ?: emptyList()) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val cleanInputChapterId = remember(chapterId) { MangaRepository.cleanChapterId(chapterId) }
    var activeMangaId by remember(cleanManga) { mutableStateOf(cleanManga) }
    var activeChapterId by remember(cleanInputChapterId) { mutableStateOf(cleanInputChapterId) }

    fun getMangaHeadersForUrl(url: String): NetworkHeaders {
        val src = MangaRepository.getSourceForId(activeMangaId)
        val referer = when {
            url.contains("asura") || src == com.nyyrox.aniverse.data.manga.MangaSource.ASURA -> "https://asuracomic.net/"
            url.contains("comick") || url.contains("meo") || src == com.nyyrox.aniverse.data.manga.MangaSource.COMICK || src == com.nyyrox.aniverse.data.manga.MangaSource.MANHUA -> "https://comick.art/"
            url.contains("mangadex") || src == com.nyyrox.aniverse.data.manga.MangaSource.MANGADEX -> "https://mangadex.org/"
            else -> "https://weebcentral.com/"
        }
        val origin = when {
            referer.contains("asura") -> "https://asuracomic.net"
            referer.contains("comick") -> "https://comick.art"
            referer.contains("mangadex") -> "https://mangadex.org"
            else -> "https://weebcentral.com"
        }
        return NetworkHeaders.Builder()
            .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .set("Referer", referer)
            .set("Origin", origin)
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .build()
    }

    LaunchedEffect(cleanInputChapterId) {
        if (cleanInputChapterId.isNotBlank()) {
            activeChapterId = cleanInputChapterId
        }
    }

    LaunchedEffect(cleanManga) {
        if (chapters.isEmpty()) {
            val dlChs = vm.downloadManager.getDownloadedChaptersForManga(cleanManga)
            if (dlChs.isNotEmpty()) {
                chapters = dlChs.map { dl ->
                    MangaChapter(
                        chapterId = dl.chapterId,
                        number = dl.chapterNumber,
                        title = dl.chapterTitle
                    )
                }
            }
        }
    }

    LaunchedEffect(info?.title, activeChapterId, chapters.size) {
        val title = info?.title
        if (!title.isNullOrBlank()) {
            val currentCh = chapters.find { it.chapterId == activeChapterId || MangaRepository.cleanChapterId(it.chapterId) == MangaRepository.cleanChapterId(activeChapterId) }
            val chNum = currentCh?.number ?: ""
            val statusText = if (chNum.isNotBlank()) "Reading $title Ch. $chNum" else "Reading $title"
            vm.updateCurrentActivity(statusText, "MANGA", cleanManga)
        }
    }

    var readMode by remember { mutableStateOf(MangaReaderMode.VERTICAL_WEBTOON) }
    var showHud by remember { mutableStateOf(true) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // ── Reader Customizations & Persistence ───────────────────
    var bgColorIndex by remember { mutableIntStateOf(0) }
    val readerBgColor = remember(bgColorIndex) {
        listOf(Color.Black, Color(0xFF0C0E14), Color(0xFF2C1F0E), Color(0xFFF5F0E8))[bgColorIndex]
    }
    var pageGapIndex by remember { mutableIntStateOf(1) }
    val pageGapDp = remember(pageGapIndex) { listOf(0.dp, 2.dp, 8.dp)[pageGapIndex] }
    var isSavingPage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val savedModeStr = com.nyyrox.aniverse.util.UserPreferences.getMangaReaderMode(context).first()
            val savedBg = com.nyyrox.aniverse.util.UserPreferences.getMangaBgColorIndex(context).first()
            val savedGap = com.nyyrox.aniverse.util.UserPreferences.getMangaPageGapIndex(context).first()

            readMode = try {
                if (savedModeStr == "HORIZONTAL_PAGED") MangaReaderMode.HORIZONTAL_RTL
                else MangaReaderMode.valueOf(savedModeStr)
            } catch (_: Exception) { MangaReaderMode.VERTICAL_WEBTOON }
            bgColorIndex = savedBg.coerceIn(0, 3)
            pageGapIndex = savedGap.coerceIn(0, 2)
        } catch (_: Exception) {}
    }

    LaunchedEffect(readMode, bgColorIndex, pageGapIndex) {
        try {
            com.nyyrox.aniverse.util.UserPreferences.saveMangaReaderSettings(
                context, readMode.name, bgColorIndex, pageGapIndex
            )
        } catch (_: Exception) {}
    }

    // Interactive Zoom State for Manga Pages
    var readerScale by remember { mutableFloatStateOf(1f) }
    var readerOffsetX by remember { mutableFloatStateOf(0f) }
    var readerOffsetY by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()

    // Determine saved chapter page for initial state
    val initialSavedPage = remember(activeChapterId, mangaId) {
        val cleanM = MangaRepository.cleanWeebId(mangaId)
        val cleanCh = MangaRepository.cleanChapterId(activeChapterId)
        if (cleanCh.isNotBlank()) {
            vm.getMangaChapterPageProgress(cleanM, cleanCh)
        } else {
            val hist = vm.getMangaHistoryItem(cleanM) ?: vm.getMangaHistoryItem(mangaId)
            hist?.pageIndex ?: 0
        }
    }
    val verticalListState = remember(activeChapterId) { LazyListState(initialSavedPage, 0) }
    val pagerState = key(activeChapterId) {
        rememberPagerState(
            initialPage = initialSavedPage,
            pageCount = { pages.size }
        )
    }

    val currentChapterIndex = remember(chapters, activeChapterId) {
        chapters.indexOfFirst {
            it.chapterId == activeChapterId ||
            (activeChapterId.isNotBlank() && (
                MangaRepository.cleanChapterId(it.chapterId) == MangaRepository.cleanChapterId(activeChapterId) ||
                it.number == activeChapterId ||
                it.number == activeChapterId.substringAfterLast("__") ||
                it.chapterId.substringAfterLast("__") == activeChapterId.substringAfterLast("__")
            ))
        }
    }

    val currentChapter = remember(chapters, activeChapterId, currentChapterIndex) {
        if (currentChapterIndex in chapters.indices) chapters[currentChapterIndex]
        else chapters.find {
            it.chapterId == activeChapterId ||
            (activeChapterId.isNotBlank() && (
                MangaRepository.cleanChapterId(it.chapterId) == MangaRepository.cleanChapterId(activeChapterId) ||
                it.number == activeChapterId ||
                it.number == activeChapterId.substringAfterLast("__")
            ))
        }
    }

    val hasPreviousChapter = currentChapterIndex > 0
    val hasNextChapter = currentChapterIndex in 0 until (chapters.size - 1)

    var isChapterSettled by remember { mutableStateOf(false) }

    val saveCurrentProgress: () -> Unit = remember(activeMangaId, activeChapterId, chapters, pages, readMode, verticalListState, pagerState, info) {
        {
            if (activeChapterId.isNotBlank() && pages.isNotEmpty()) {
                val maxIdx = (pages.size - 1).coerceAtLeast(0)
                val targetPage = if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                    verticalListState.firstVisibleItemIndex.coerceIn(0, maxIdx)
                } else {
                    pagerState.currentPage.coerceIn(0, maxIdx)
                }
                val curCh = chapters.find {
                    it.chapterId == activeChapterId ||
                    MangaRepository.cleanChapterId(it.chapterId) == MangaRepository.cleanChapterId(activeChapterId)
                }
                val cleanM = activeMangaId.ifBlank { MangaRepository.cleanWeebId(mangaId) }
                val historyItem = vm.getMangaHistoryItem(cleanM) ?: vm.getMangaHistoryItem(mangaId)
                val bookmarkItem = vm.mangaBookmarks.firstOrNull {
                    it.mangaId == mangaId || MangaRepository.cleanWeebId(it.mangaId) == cleanM
                }
                val effectiveTitle = info?.title
                    ?: historyItem?.title
                    ?: bookmarkItem?.title
                    ?: MangaRepository.extractSlugTitle(mangaId)
                    ?: "Manga"
                val effectivePoster = info?.poster ?: historyItem?.poster ?: bookmarkItem?.poster
                val effectiveType = info?.type ?: historyItem?.type ?: "Manga"
                val effectiveChNum = curCh?.number ?: historyItem?.chapterNumber ?: "1"
                val effectiveChTitle = curCh?.title ?: historyItem?.chapterTitle

                vm.recordMangaProgress(
                    mangaId = cleanM,
                    title = effectiveTitle,
                    poster = effectivePoster,
                    type = effectiveType,
                    chapterId = activeChapterId,
                    chapterNumber = effectiveChNum,
                    chapterTitle = effectiveChTitle,
                    pageIndex = targetPage,
                    pageCount = pages.size
                )
            }
        }
    }


    DisposableEffect(Unit) {
        vm.setActivityMode(CurrentActivityMode.READING_MANGA)
        onDispose {
            saveCurrentProgress()
            vm.setActivityMode(CurrentActivityMode.EXPLORING_APP)
            vm.updateCurrentActivity(null, null, null)
            vm.syncTimeSpentAndReadingToCloud()
        }
    }

    LaunchedEffect(chapterId, readMode) {
        readerScale = 1f
        readerOffsetX = 0f
        readerOffsetY = 0f
    }

    LaunchedEffect(mangaId, chapterId, activeChapterId, retryKey) {
        isChapterSettled = false
        loading = true
        pages = emptyList()
        error = null
        readerScale = 1f
        readerOffsetX = 0f
        readerOffsetY = 0f

        val cleanM = MangaRepository.cleanWeebId(mangaId)
        val rawCh = MangaRepository.cleanChapterId(chapterId)
        activeMangaId = cleanM

        val historyItem = vm.getMangaHistoryItem(cleanM) ?: vm.getMangaHistoryItem(mangaId)
        val bookmarkItem = vm.mangaBookmarks.firstOrNull {
            it.mangaId == mangaId || MangaRepository.cleanWeebId(it.mangaId) == cleanM
        }
        val titleFallback = historyItem?.title ?: bookmarkItem?.title ?: MangaRepository.extractSlugTitle(mangaId)

        // 1. Resolve exact target chapter ID (prioritizing activeChapterId selected by user navigation)
        var chToFetch = if (activeChapterId.isNotBlank()) activeChapterId else rawCh.ifBlank { null }

        // If chapters list is currently empty, try cache or downloads
        if (chapters.isEmpty()) {
            val cached = vm.mangaDetailCache[cleanM] ?: vm.mangaDetailCache[mangaId]
            if (cached != null && cached.chapters.isNotEmpty()) {
                if (info == null) info = cached.info
                chapters = cached.chapters
            } else {
                val dlChs = vm.downloadManager.getDownloadedChaptersForManga(cleanM)
                if (dlChs.isNotEmpty()) {
                    chapters = dlChs.map { dl ->
                        MangaChapter(
                            chapterId = dl.chapterId,
                            number = dl.chapterNumber,
                            title = dl.chapterTitle
                        )
                    }
                }
            }
        }

        // If chToFetch is not a 26-char ULID (e.g. "1" or slug or empty)
        val isUlid = chToFetch != null && chToFetch.length == 26 && chToFetch.all { it in "0123456789ABCDEFGHJKMNPQRSTVWXYZabcdefghjkmnpqrstvwxyz" }
        if (!isUlid) {
            val match = if (!chToFetch.isNullOrBlank()) {
                chapters.find { 
                    it.chapterId == chToFetch || 
                    it.number == chToFetch || 
                    MangaRepository.cleanChapterId(it.chapterId) == chToFetch ||
                    it.number == chToFetch.substringAfterLast("__")
                }
            } else null

            chToFetch = match?.chapterId
                ?: chToFetch
                ?: historyItem?.chapterId?.let { MangaRepository.cleanChapterId(it) }
                ?: chapters.firstOrNull { it.number == "1" || it.number == "1.0" }?.chapterId
                ?: chapters.firstOrNull()?.chapterId
        }

        // 2. Direct fast fetch for known source prefixes, otherwise resolve
        if (info == null || chapters.isEmpty()) {
            launch(Dispatchers.IO) {
                try {
                    val hasPrefix = cleanM.startsWith("as_") || cleanM.startsWith("ck_") ||
                            cleanM.startsWith("mh_") || cleanM.startsWith("md_") || cleanM.startsWith("wc_")

                    if (hasPrefix) {
                        if (chapters.isEmpty()) {
                            val directChapters = MangaRepository.getChapters(cleanM)
                            if (directChapters.isNotEmpty()) {
                                chapters = directChapters
                            }
                        }
                        if (info == null) {
                            val directInfo = MangaRepository.getInfo(cleanM)
                            if (directInfo != null) {
                                info = directInfo
                            }
                        }
                    }

                    if (info == null || chapters.isEmpty()) {
                        val (_, resInfo, resChapters) = MangaRepository.resolveMangaDetails(cleanM, titleFallback)
                        if (resInfo != null && info == null) info = resInfo
                        if (resChapters.isNotEmpty() && chapters.isEmpty()) chapters = resChapters
                    }
                } catch (e: Exception) {
                    Log.w("MangaReaderScreen", "Background metadata resolution: ${e.message}")
                }
            }
        }

        // 3. If chToFetch is STILL blank (e.g. cold start with no cache and no downloads)
        if (chToFetch.isNullOrBlank()) {
            try {
                val (_, resInfo, resChapters) = MangaRepository.resolveMangaDetails(cleanM, titleFallback)
                if (resInfo != null) info = resInfo
                if (resChapters.isNotEmpty()) {
                    chapters = resChapters
                    chToFetch = resChapters.firstOrNull { it.number == "1" || it.number == "1.0" }?.chapterId
                        ?: resChapters.firstOrNull()?.chapterId
                }
            } catch (e: Exception) {
                Log.e("MangaReaderScreen", "Cold start chapter resolve failed: ${e.message}")
            }
        }

        if (chToFetch.isNullOrBlank()) {
            error = "Chapter not found. Please select a chapter."
            loading = false
            return@LaunchedEffect
        }

        val cleanChId = MangaRepository.cleanChapterId(chToFetch)
        activeChapterId = cleanChId

        // 4. Fetch pages (Local downloads first in 0ms, then Worker network)
        var fetchedPages = emptyList<String>()
        val localPages = vm.downloadManager.getDownloadedMangaPages(cleanM, cleanChId)
        if (localPages.isNotEmpty()) {
            Log.d("MangaReaderScreen", "Found ${localPages.size} downloaded pages for $cleanM/$cleanChId")
            fetchedPages = localPages
        } else {
            Log.d("MangaReaderScreen", "Fetching remote pages for chapter $cleanChId")
            fetchedPages = MangaRepository.getPages(cleanChId)
        }

        if (fetchedPages.isEmpty()) {
            error = "Could not load chapter pages. Tap to retry."
            loading = false
        } else {
            // Determine starting page:
            val savedChapterPage = vm.getMangaChapterPageProgress(cleanM, cleanChId)
            val isExactHistoryChapter = historyItem != null && (
                historyItem.chapterId == cleanChId ||
                MangaRepository.cleanChapterId(historyItem.chapterId) == cleanChId
            )
            val targetStartPage = when {
                savedChapterPage in fetchedPages.indices -> savedChapterPage
                isExactHistoryChapter && historyItem != null && historyItem.pageIndex in fetchedPages.indices -> historyItem.pageIndex
                else -> 0
            }

            pages = fetchedPages
            loading = false

            if (targetStartPage > 0) {
                try {
                    verticalListState.scrollToItem(targetStartPage)
                    pagerState.scrollToPage(targetStartPage)
                } catch (_: Exception) {}

                kotlinx.coroutines.delay(40)
                try {
                    verticalListState.scrollToItem(targetStartPage)
                    pagerState.scrollToPage(targetStartPage)
                } catch (_: Exception) {}
            }

            val currentCh = chapters.find { 
                it.chapterId == cleanChId || 
                MangaRepository.cleanChapterId(it.chapterId) == cleanChId 
            }
            vm.recordMangaProgress(
                mangaId = activeMangaId,
                title = info?.title ?: historyItem?.title ?: titleFallback ?: "Manga",
                poster = info?.poster ?: historyItem?.poster ?: bookmarkItem?.poster,
                type = info?.type ?: historyItem?.type ?: "Manga",
                chapterId = cleanChId,
                chapterNumber = currentCh?.number ?: historyItem?.chapterNumber ?: "1",
                chapterTitle = currentCh?.title ?: historyItem?.chapterTitle,
                pageIndex = targetStartPage,
                pageCount = fetchedPages.size
            )
            kotlinx.coroutines.delay(60)
            isChapterSettled = true
        }
    }

    // Track current page for HUD and progress saving
    val currentPageIndex by remember(activeChapterId, readMode, verticalListState, pagerState, pages.size) {
        derivedStateOf {
            val maxIdx = (pages.size - 1).coerceAtLeast(0)
            if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                verticalListState.firstVisibleItemIndex.coerceIn(0, maxIdx)
            } else {
                pagerState.currentPage.coerceIn(0, maxIdx)
            }
        }
    }

    val bookmarkedPageIndices = remember(activeMangaId, activeChapterId, vm.mangaBookmarks) {
        vm.mangaBookmarks.filter { bm ->
            bm.mangaId == activeMangaId &&
            bm.bookmarkType == com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE &&
            (bm.chapterId == activeChapterId || (activeChapterId.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(bm.chapterId.orEmpty()) == com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(activeChapterId)))
        }.mapNotNull { it.pageIndex }.toSet()
    }

    // Save reading progress continuously as page advances
    LaunchedEffect(currentPageIndex, pages.size, isChapterSettled) {
        if (isChapterSettled && pages.isNotEmpty()) {
            saveCurrentProgress()
        }
    }

    // Auto-hide HUD after 5 seconds of inactivity
    LaunchedEffect(showHud) {
        if (showHud) {
            kotlinx.coroutines.delay(5000)
            showHud = false
        }
    }

    BackHandler(enabled = true) {
        when {
            showSettingsSheet -> showSettingsSheet = false
            showChapterSheet -> showChapterSheet = false
            else -> {
                saveCurrentProgress()
                onBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
            .pointerInput(readMode, pages.size) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width.toFloat()
                        if (screenWidth > 0f) {
                            val leftThreshold = screenWidth * 0.30f
                            val rightThreshold = screenWidth * 0.70f
                            when {
                                offset.x < leftThreshold -> {
                                    if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                                        scope.launch {
                                            verticalListState.animateScrollBy(-800f)
                                        }
                                    } else if (readMode.isRtl) {
                                        // In RTL Manga, tapping left advances forward
                                        scope.launch {
                                            val next = (pagerState.currentPage + 1).coerceAtMost((pages.size - 1).coerceAtLeast(0))
                                            pagerState.animateScrollToPage(next)
                                        }
                                    } else {
                                        // In LTR Paged, tapping left goes backward
                                        scope.launch {
                                            val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                            pagerState.animateScrollToPage(prev)
                                        }
                                    }
                                }
                                offset.x > rightThreshold -> {
                                    if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                                        scope.launch {
                                            verticalListState.animateScrollBy(800f)
                                        }
                                    } else if (readMode.isRtl) {
                                        // In RTL Manga, tapping right goes backward
                                        scope.launch {
                                            val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                            pagerState.animateScrollToPage(prev)
                                        }
                                    } else {
                                        // In LTR Paged, tapping right advances forward
                                        scope.launch {
                                            val next = (pagerState.currentPage + 1).coerceAtMost((pages.size - 1).coerceAtLeast(0))
                                            pagerState.animateScrollToPage(next)
                                        }
                                    }
                                }
                                else -> {
                                    showHud = !showHud
                                }
                            }
                        } else {
                            showHud = !showHud
                        }
                    }
                )
            }
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = OttPrimary, strokeWidth = 3.dp)
                    Text(
                        text = "Extracting Manga Pages...",
                        color = OttSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "No pages available", color = Color.White, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(OttPillShape)
                                .background(Color(0x33FFFFFF))
                                .clickable {
                                    saveCurrentProgress()
                                    onBack()
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Go Back", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(OttPillShape)
                                .background(OttPrimary)
                                .clickable { retryKey++ }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // ÔöÇÔöÇ Manga Page Display with Interactive Pinch-to-Zoom ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (readerScale * zoom).coerceIn(1f, 4.5f)
                            readerScale = newScale
                            if (newScale > 1.05f) {
                                val maxOffsetX = 1200f * (newScale - 1f)
                                val maxOffsetY = 1800f * (newScale - 1f)
                                readerOffsetX = (readerOffsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                readerOffsetY = (readerOffsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                readerOffsetX = 0f
                                readerOffsetY = 0f
                            }
                        }
                    }
                    .pointerInput(readMode, pages.size) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (readerScale > 1.1f) {
                                    readerScale = 1f
                                    readerOffsetX = 0f
                                    readerOffsetY = 0f
                                } else {
                                    readerScale = 2.2f
                                }
                            },
                            onTap = { offset ->
                                val screenWidth = size.width.toFloat()
                                if (screenWidth > 0f) {
                                    val leftThreshold = screenWidth * 0.25f
                                    val rightThreshold = screenWidth * 0.75f
                                    when {
                                        offset.x < leftThreshold -> {
                                            if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                                                scope.launch {
                                                    verticalListState.animateScrollBy(-800f)
                                                }
                                            } else if (readMode.isRtl) {
                                                // In RTL Manga, tapping left advances forward
                                                scope.launch {
                                                    val next = (pagerState.currentPage + 1).coerceAtMost((pages.size - 1).coerceAtLeast(0))
                                                    pagerState.animateScrollToPage(next)
                                                }
                                            } else {
                                                // In LTR Paged, tapping left goes backward
                                                scope.launch {
                                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                                    pagerState.animateScrollToPage(prev)
                                                }
                                            }
                                        }
                                        offset.x > rightThreshold -> {
                                            if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                                                scope.launch {
                                                    verticalListState.animateScrollBy(800f)
                                                }
                                            } else if (readMode.isRtl) {
                                                // In RTL Manga, tapping right goes backward
                                                scope.launch {
                                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                                    pagerState.animateScrollToPage(prev)
                                                }
                                            } else {
                                                // In LTR Paged, tapping right advances forward
                                                scope.launch {
                                                    val next = (pagerState.currentPage + 1).coerceAtMost((pages.size - 1).coerceAtLeast(0))
                                                    pagerState.animateScrollToPage(next)
                                                }
                                            }
                                        }
                                        else -> {
                                            showHud = !showHud
                                        }
                                    }
                                } else {
                                    showHud = !showHud
                                }
                            }
                        )
                    }
                    .graphicsLayer(
                        scaleX = readerScale,
                        scaleY = readerScale,
                        translationX = readerOffsetX,
                        translationY = readerOffsetY
                    )
            ) {
                key(activeChapterId) {
                    if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                        // Mode 1: Vertical Continuous Webtoon Scroll
                        LazyColumn(
                            state = verticalListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = pages,
                                key = { index, url -> "$activeChapterId-$index-$url" }
                            ) { index, pageUrl ->
                                val pageRequest = remember(pageUrl) {
                                    pageUrl?.let { url ->
                                        val builder = ImageRequest.Builder(context)
                                        if (url.startsWith("http://") || url.startsWith("https://")) {
                                            builder.data(url)
                                            builder.httpHeaders(getMangaHeadersForUrl(url))
                                        } else {
                                            val cleanPath = url.removePrefix("file://")
                                            builder.data(java.io.File(cleanPath))
                                        }
                                        builder.crossfade(true).build()
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(readerBgColor)
                                        .padding(vertical = pageGapDp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = pageRequest,
                                        contentDescription = "Page ${index + 1}",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        loading = {
                                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(32.dp))
                                            }
                                        },
                                        error = {
                                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Default.Image, contentDescription = "Error", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                                    Text("Failed to load page", color = Color.Gray, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Next chapter trigger at the bottom of the column
                            if (hasNextChapter) {
                                item(key = "next_chapter_trigger_$activeChapterId") {
                                    val nextChapter = chapters[currentChapterIndex + 1]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = 24.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(OttSurfaceElevated)
                                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                            .clickable {
                                                saveCurrentProgress()
                                                val cleanNextId = MangaRepository.cleanChapterId(nextChapter.chapterId)
                                                activeChapterId = cleanNextId
                                                onSelectChapter(cleanNextId)
                                            }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Next: Chapter ${nextChapter.number}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OttPrimary
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = OttPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Mode 2: Horizontal Paginated Swipe — depth+fade transition
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize().background(readerBgColor),
                            pageSpacing = pageGapDp,
                            reverseLayout = readMode.isRtl,
                            key = { pageIndex -> "$activeChapterId-$pageIndex-${pages.getOrNull(pageIndex).orEmpty()}" }
                        ) { pageIndex ->
                            val pageUrl = pages.getOrNull(pageIndex)
                            val pageRequest = remember(pageUrl) {
                                pageUrl?.let { url ->
                                    val builder = ImageRequest.Builder(context)
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        builder.data(url)
                                        builder.httpHeaders(getMangaHeadersForUrl(url))
                                    } else {
                                        val cleanPath = url.removePrefix("file://")
                                        builder.data(java.io.File(cleanPath))
                                    }
                                    builder.crossfade(true).build()
                                }
                            }
                            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                            val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    alpha = 1f - absOffset * 0.35f
                                    scaleX = 1f - absOffset * 0.08f
                                    scaleY = 1f - absOffset * 0.08f
                                    cameraDistance = 20f * density
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = pageRequest,
                                    contentDescription = "Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(32.dp))
                                        }
                                    },
                                    error = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Image, contentDescription = "Error", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                                Text("Failed to load page", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } // Close Zoom Box

            // ÔöÇÔöÇ Top Header HUD ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
            AnimatedVisibility(
                visible = showHud,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xF2070A0F),
                                    Color(0xCC070A0F),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = {
                                saveCurrentProgress()
                                onBack()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info?.title ?: "Manga",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Chapter ${currentChapter?.number ?: ""} • Page ${currentPageIndex + 1}/${pages.size}",
                                    fontSize = 11.sp,
                                    color = OttPrimary,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right actions: Bookmark, Download & Save Menu, Chapters, Settings
                        val downloadTasks by vm.downloadManager.tasks.collectAsState()
                        val isChapterDownloaded = remember(downloadTasks, activeChapterId) {
                            vm.downloadManager.isMangaChapterDownloaded(activeMangaId, activeChapterId)
                        }
                        val isChapterDownloading = remember(downloadTasks, activeChapterId) {
                            downloadTasks.any { it.itemId == activeMangaId && it.chapterId == activeChapterId && it.status == com.nyyrox.aniverse.data.download.DownloadStatus.DOWNLOADING }
                        }
                        var showDownloadMenu by remember { mutableStateOf(false) }
                        var showBookmarkMenu by remember { mutableStateOf(false) }

                        val isPageBookmarked = remember(activeMangaId, activeChapterId, currentPageIndex, vm.mangaBookmarks) {
                            vm.isPageBookmarked(activeMangaId, activeChapterId, currentPageIndex)
                        }
                        val isChapterBookmarked = remember(activeMangaId, activeChapterId, vm.mangaBookmarks) {
                            vm.isChapterBookmarked(activeMangaId, activeChapterId)
                        }
                        val isSeriesBookmarked = remember(activeMangaId, vm.mangaBookmarks) {
                            vm.isMangaBookmarked(activeMangaId)
                        }
                        val hasAnyBookmark = isPageBookmarked || isChapterBookmarked || isSeriesBookmarked

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 1. 3-Tier Bookmark Dropdown Button (Page, Chapter, Series)
                            Box {
                                IconButton(onClick = { showBookmarkMenu = true }) {
                                    Icon(
                                        imageVector = if (hasAnyBookmark) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark Options",
                                        tint = when {
                                            isPageBookmarked -> Color(0xFF4FC3F7)
                                            isChapterBookmarked -> OttAccentGold
                                            isSeriesBookmarked -> OttPrimary
                                            else -> Color.White
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showBookmarkMenu,
                                    onDismissRequest = { showBookmarkMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF161B26))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                ) {
                                    // Option 1: Bookmark Current Page
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = if (isPageBookmarked) "Remove Page Bookmark" else "Bookmark Current Page",
                                                    color = if (isPageBookmarked) Color(0xFF4FC3F7) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Ch. ${currentChapter?.number ?: ""} • Pg ${currentPageIndex + 1}",
                                                    color = OttSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = Color(0xFF4FC3F7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showBookmarkMenu = false
                                            vm.toggleMangaBookmark(
                                                mangaId = activeMangaId,
                                                title = info?.title ?: "Manga",
                                                poster = info?.poster,
                                                type = info?.type ?: "Manga",
                                                bookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE,
                                                chapterId = activeChapterId,
                                                chapterNumber = currentChapter?.number ?: "",
                                                pageIndex = currentPageIndex,
                                                pageCount = pages.size,
                                                latestChapter = "Ch ${currentChapter?.number ?: ""} • Pg ${currentPageIndex + 1}"
                                            )
                                        }
                                    )

                                    // Option 2: Bookmark Current Chapter
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = if (isChapterBookmarked) "Remove Chapter Bookmark" else "Bookmark Chapter",
                                                    color = if (isChapterBookmarked) OttAccentGold else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Chapter ${currentChapter?.number ?: ""}",
                                                    color = OttSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isChapterBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = OttAccentGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showBookmarkMenu = false
                                            vm.toggleMangaBookmark(
                                                mangaId = activeMangaId,
                                                title = info?.title ?: "Manga",
                                                poster = info?.poster,
                                                type = info?.type ?: "Manga",
                                                bookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER,
                                                chapterId = activeChapterId,
                                                chapterNumber = currentChapter?.number ?: "",
                                                latestChapter = "Chapter ${currentChapter?.number ?: ""}"
                                            )
                                        }
                                    )

                                    // Option 3: Bookmark Entire Manga Series
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = if (isSeriesBookmarked) "Remove Series Bookmark" else "Bookmark Manga Series",
                                                    color = if (isSeriesBookmarked) OttPrimary else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = info?.title ?: "Series",
                                                    color = OttSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSeriesBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = OttPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showBookmarkMenu = false
                                            vm.toggleMangaBookmark(
                                                mangaId = activeMangaId,
                                                title = info?.title ?: "Manga",
                                                poster = info?.poster,
                                                type = info?.type ?: "Manga",
                                                bookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES,
                                                latestChapter = "Ch ${currentChapter?.number ?: ""}"
                                            )
                                        }
                                    )
                                }
                            }

                            // 2. Download / Save Dropdown Menu
                            Box {
                                IconButton(onClick = { showDownloadMenu = true }) {
                                    if (isSavingPage || isChapterDownloading) {
                                        CircularProgressIndicator(
                                            color = if (isSavingPage) Color(0xFF4FC3F7) else OttAccentGold,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp).padding(2.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isChapterDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                            contentDescription = "Save & Download Options",
                                            tint = if (isChapterDownloaded) OttAccentGold else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showDownloadMenu,
                                    onDismissRequest = { showDownloadMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF161B26))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                ) {
                                    // Save Current Page Option
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "Save Current Page",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Page ${currentPageIndex + 1} to Gallery",
                                                    color = OttSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = Color(0xFF4FC3F7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showDownloadMenu = false
                                            val url = pages.getOrNull(currentPageIndex)
                                            if (!url.isNullOrBlank() && !isSavingPage) {
                                                isSavingPage = true
                                                scope.launch {
                                                    saveMangaPageToGallery(
                                                        context = context,
                                                        pageUrl = url,
                                                        mangaTitle = info?.title ?: "Manga",
                                                        pageIndex = currentPageIndex,
                                                        headers = if (!url.startsWith("file://") && !url.startsWith("/"))
                                                            mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://weebcentral.com/")
                                                        else emptyMap()
                                                    )
                                                    isSavingPage = false
                                                }
                                            }
                                        }
                                    )

                                    // Save / Download Chapter Option
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = if (isChapterDownloaded) "Chapter Downloaded" else if (isChapterDownloading) "Downloading Chapter..." else "Save Chapter Offline",
                                                    color = if (isChapterDownloaded) OttAccentGold else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Chapter ${currentChapter?.number ?: ""}",
                                                    color = OttSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isChapterDownloaded) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                                                contentDescription = null,
                                                tint = if (isChapterDownloaded) OttAccentGold else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showDownloadMenu = false
                                            if (!isChapterDownloaded && !isChapterDownloading) {
                                                vm.downloadMangaChapter(
                                                    mangaId,
                                                    info?.title ?: mangaId,
                                                    info?.poster ?: "",
                                                    chapterId,
                                                    "Chapter ${currentChapter?.number ?: ""}"
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            // 3. Chapters Drawer Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x2EFFFFFF))
                                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { showChapterSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Chapters",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Chapters",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // 4. Reader Settings Button
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Bottom Footer HUD ──────────────────────────────────────
            AnimatedVisibility(
                visible = showHud,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xCC070A0F),
                                    Color(0xF2070A0F)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Highlighted Continuous Page Progress Line
                        if (pages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color(0x26FFFFFF))
                            ) {
                                val progress = ((currentPageIndex + 1).toFloat() / pages.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    OttPrimary.copy(alpha = 0.7f),
                                                    OttPrimary
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        // Slider + Prominently Highlighted Page Number Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isCurPageBookmarked = currentPageIndex in bookmarkedPageIndices

                            // Active Page Highlight Badge (turns sky blue with bookmark icon if current page is bookmarked)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurPageBookmarked) Color(0x3338BDF8) else OttPrimary.copy(alpha = 0.22f))
                                    .border(1.5.dp, if (isCurPageBookmarked) Color(0xFF38BDF8) else OttPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (isCurPageBookmarked) {
                                        Icon(
                                            Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = "${currentPageIndex + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCurPageBookmarked) Color(0xFF38BDF8) else OttPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            BoxWithConstraints(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val totalWidth = maxWidth
                                val maxPageIdx = (pages.size - 1).coerceAtLeast(1)

                                Slider(
                                    value = currentPageIndex.toFloat(),
                                    onValueChange = { targetPage ->
                                        val page = targetPage.toInt()
                                        scope.launch {
                                            if (readMode == MangaReaderMode.VERTICAL_WEBTOON) {
                                                verticalListState.scrollToItem(page)
                                            } else {
                                                pagerState.scrollToPage(page)
                                            }
                                        }
                                    },
                                    valueRange = 0f..maxPageIdx.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isCurPageBookmarked) Color(0xFF38BDF8) else OttPrimary,
                                        activeTrackColor = OttPrimary,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Highlight bookmarks on slider track
                                if (pages.size > 1 && bookmarkedPageIndices.isNotEmpty()) {
                                    val thumbPadding = 10.dp
                                    val trackUsableWidth = totalWidth - (thumbPadding * 2)
                                    bookmarkedPageIndices.forEach { bmPageIdx ->
                                        val fraction = (bmPageIdx.toFloat() / maxPageIdx.toFloat()).coerceIn(0f, 1f)
                                        val markerOffset = thumbPadding + (trackUsableWidth * fraction)
                                        Box(
                                            modifier = Modifier
                                                .offset(x = markerOffset - 2.dp)
                                                .size(width = 4.dp, height = 14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF38BDF8))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }

                            // Total Pages Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pages.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OttSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Navigation: Previous Chapter & Next Chapter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prev Chapter
                            Box(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(if (hasPreviousChapter) OttSurfaceElevated else Color.Transparent)
                                    .clickable(enabled = hasPreviousChapter) {
                                        val prev = chapters.getOrNull(currentChapterIndex - 1)
                                        if (prev != null) {
                                            saveCurrentProgress()
                                            val cleanPrev = MangaRepository.cleanChapterId(prev.chapterId)
                                            activeChapterId = cleanPrev
                                            onSelectChapter(cleanPrev)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = if (hasPreviousChapter) Color.White else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Prev Chapter",
                                        fontSize = 11.sp,
                                        color = if (hasPreviousChapter) Color.White else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Mode & Chapter Badge
                            Text(
                                text = if (currentChapter?.number?.isNotBlank() == true) {
                                    val modeName = when (readMode) {
                                        MangaReaderMode.VERTICAL_WEBTOON -> "Webtoon"
                                        MangaReaderMode.HORIZONTAL_RTL, MangaReaderMode.HORIZONTAL_PAGED -> "RTL"
                                        MangaReaderMode.HORIZONTAL_LTR -> "LTR"
                                    }
                                    "Ch. ${currentChapter.number} • $modeName"
                                } else {
                                    when (readMode) {
                                        MangaReaderMode.VERTICAL_WEBTOON -> "Webtoon • Vertical"
                                        MangaReaderMode.HORIZONTAL_RTL, MangaReaderMode.HORIZONTAL_PAGED -> "Manga • RTL"
                                        MangaReaderMode.HORIZONTAL_LTR -> "Paged • LTR"
                                    }
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OttPrimary,
                                softWrap = false,
                                maxLines = 1
                            )

                            // Next Chapter
                            Box(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(if (hasNextChapter) OttSurfaceElevated else Color.Transparent)
                                    .clickable(enabled = hasNextChapter) {
                                        val next = chapters.getOrNull(currentChapterIndex + 1)
                                        if (next != null) {
                                            saveCurrentProgress()
                                            val cleanNext = MangaRepository.cleanChapterId(next.chapterId)
                                            activeChapterId = cleanNext
                                            onSelectChapter(cleanNext)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Next Chapter",
                                        fontSize = 11.sp,
                                        color = if (hasNextChapter) Color.White else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (hasNextChapter) Color.White else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Chapter Selector Modal Bottom Sheet (Liquid Glass Styling) ────
        if (showChapterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xEB0E121B),
                scrimColor = Color(0x66000000),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x40FFFFFF))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(OttPillShape)
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${chapters.size} available",
                                fontSize = 11.sp,
                                color = OttSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(chapters) { index, ch ->
                            val isSelected = MangaRepository.cleanChapterId(ch.chapterId) == activeChapterId
                            val isChBookmarked = vm.isChapterBookmarked(activeMangaId, ch.chapterId) || vm.isChapterBookmarked(activeMangaId, MangaRepository.cleanChapterId(ch.chapterId))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0x3338BDF8)
                                            isChBookmarked -> Color(0x280284C7)
                                            else -> Color(0x1AFFFFFF)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isSelected -> OttPrimary.copy(alpha = 0.7f)
                                            isChBookmarked -> Color(0xFF38BDF8)
                                            else -> Color(0x20FFFFFF)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        saveCurrentProgress()
                                        showChapterSheet = false
                                        val cleanChId = MangaRepository.cleanChapterId(ch.chapterId)
                                        activeChapterId = cleanChId
                                        onSelectChapter(cleanChId)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isChBookmarked) {
                                            Icon(
                                                Icons.Default.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Chapter ${ch.number}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || isChBookmarked) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> OttPrimary
                                                isChBookmarked -> Color(0xFF38BDF8)
                                                else -> Color.White
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                vm.toggleMangaBookmark(
                                                    mangaId = activeMangaId,
                                                    title = info?.title ?: "Manga",
                                                    poster = info?.poster,
                                                    type = info?.type ?: "Manga",
                                                    bookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER,
                                                    chapterId = ch.chapterId,
                                                    chapterNumber = ch.number,
                                                    latestChapter = "Chapter ${ch.number}"
                                                )
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isChBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark Chapter",
                                                tint = if (isChBookmarked) Color(0xFF38BDF8) else Color(0x66FFFFFF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (isChBookmarked && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(Color(0xFF38BDF8))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "BOOKMARKED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(OttPrimary)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "READING",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
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
    }

    // ── Reader Settings Sheet ──────────────────────────────────
    if (showSettingsSheet) {
        MangaReaderSettingsSheet(
            bgColorIndex = bgColorIndex,
            onBgColorChange = { bgColorIndex = it },
            pageGapIndex = pageGapIndex,
            onPageGapChange = { pageGapIndex = it },
            readMode = readMode,
            onSelectReadMode = { readMode = it },
            onDismiss = { showSettingsSheet = false }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaReaderSettingsSheet(
    bgColorIndex: Int,
    onBgColorChange: (Int) -> Unit,
    pageGapIndex: Int,
    onPageGapChange: (Int) -> Unit,
    readMode: MangaReaderMode,
    onSelectReadMode: (MangaReaderMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF10131A),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Reader Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // 1. Reading Mode & Direction Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reading Mode & Direction", fontSize = 12.sp, color = Color(0xFF8A8FA8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWebtoon = readMode == MangaReaderMode.VERTICAL_WEBTOON
                    val isRtl = readMode == MangaReaderMode.HORIZONTAL_RTL || readMode == MangaReaderMode.HORIZONTAL_PAGED
                    val isLtr = readMode == MangaReaderMode.HORIZONTAL_LTR

                    // 1. Webtoon Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isWebtoon) OttPrimary.copy(alpha = 0.18f) else Color(0x14FFFFFF))
                            .border(
                                width = if (isWebtoon) 1.5.dp else 1.dp,
                                color = if (isWebtoon) OttPrimary else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectReadMode(MangaReaderMode.VERTICAL_WEBTOON) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = if (isWebtoon) OttPrimary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Webtoon",
                                fontSize = 11.5.sp,
                                fontWeight = if (isWebtoon) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWebtoon) OttPrimary else Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Vertical",
                                fontSize = 9.5.sp,
                                color = OttSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Right to Left (Manga RTL) Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRtl) OttPrimary.copy(alpha = 0.18f) else Color(0x14FFFFFF))
                            .border(
                                width = if (isRtl) 1.5.dp else 1.dp,
                                color = if (isRtl) OttPrimary else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectReadMode(MangaReaderMode.HORIZONTAL_RTL) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = if (isRtl) OttPrimary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Manga (RTL)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isRtl) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRtl) OttPrimary else Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Right to Left",
                                fontSize = 9.5.sp,
                                color = OttSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Left to Right (Paged LTR) Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLtr) OttPrimary.copy(alpha = 0.18f) else Color(0x14FFFFFF))
                            .border(
                                width = if (isLtr) 1.5.dp else 1.dp,
                                color = if (isLtr) OttPrimary else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectReadMode(MangaReaderMode.HORIZONTAL_LTR) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = if (isLtr) OttPrimary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Paged (LTR)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isLtr) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLtr) OttPrimary else Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "Left to Right",
                                fontSize = 9.5.sp,
                                color = OttSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. Background Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Background", fontSize = 12.sp, color = Color(0xFF8A8FA8))
                val bgLabels = listOf("Black", "Dark", "Sepia", "White")
                val bgSwatches = listOf(Color.Black, Color(0xFF0C0E14), Color(0xFF2C1F0E), Color(0xFFF5F0E8))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bgLabels.forEachIndexed { i, label ->
                        val sel = bgColorIndex == i
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgSwatches[i])
                                .border(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) OttPrimary else Color(0x33FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onBgColorChange(i) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (i >= 3) Color(0xFF1A1A1A) else Color.White
                            )
                        }
                    }
                }
            }

            // 3. Page Gap Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Page Gap", fontSize = 12.sp, color = Color(0xFF8A8FA8))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("None", "Small", "Medium").forEachIndexed { i, label ->
                        val sel = pageGapIndex == i
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) OttPrimary.copy(alpha = 0.18f) else Color(0x14FFFFFF))
                                .border(
                                    if (sel) 1.5.dp else 1.dp,
                                    if (sel) OttPrimary else Color(0x28FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onPageGapChange(i) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) OttPrimary else Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private suspend fun saveMangaPageToGallery(
    context: android.content.Context,
    pageUrl: String,
    mangaTitle: String,
    pageIndex: Int,
    headers: Map<String, String>
) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val bytes: ByteArray = if (pageUrl.startsWith("file://") || pageUrl.startsWith("/")) {
            java.io.File(pageUrl.removePrefix("file://")).readBytes()
        } else {
            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(pageUrl)
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }.build()
            client.newCall(req).execute().body?.bytes() ?: return@withContext
        }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return@withContext
        val safe = mangaTitle.replace("[^\\w\\s-]".toRegex(), "").replace("\\s+".toRegex(), "_")
        val fileName = "${safe}_page${pageIndex + 1}.jpg"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val cv = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_PICTURES}/Zenkai")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
            ) ?: return@withContext
            context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it)
            }
            cv.clear()
            cv.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, cv, null, null)
        } else {
            val dir = java.io.File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES), "Zenkai"
            ).apply { mkdirs() }
            java.io.File(dir, fileName).outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it)
            }
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            android.widget.Toast.makeText(context, "Page saved to Pictures/Zenkai",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            android.widget.Toast.makeText(context, "Failed to save page",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
