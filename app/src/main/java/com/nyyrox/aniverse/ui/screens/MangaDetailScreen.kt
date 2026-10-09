package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.launch
import com.nyyrox.aniverse.data.manga.MangaSource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.manga.MangaChapter
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.manga.MangaInfo
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedTransitionPhase
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttHeroGradient
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MangaDetailScreen(
    vm: MainViewModel,
    mangaId: String,
    onBack: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String) -> Unit
) {
    val context = LocalContext.current
    val coordinator = LocalSharedPosterCoordinator.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isSwitchingSource by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (coordinator != null && (coordinator.phase == SharedTransitionPhase.PRESENTED || coordinator.phase == SharedTransitionPhase.FORWARD)) {
            coordinator.returnBack { onBack() }
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBack()
    }

    val cleanManga = MangaRepository.cleanWeebId(mangaId)
    val cachedManga = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[cleanManga]
    var currentMangaId by remember { mutableStateOf(cachedManga?.resolvedId ?: cleanManga) }
    var info by remember { mutableStateOf<MangaInfo?>(cachedManga?.info) }
    var chapters by remember { mutableStateOf<List<MangaChapter>>(cachedManga?.chapters ?: emptyList()) }
    var loading by remember { mutableStateOf(cachedManga == null) }
    var error by remember { mutableStateOf<String?>(null) }

    var isBookmarked by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }
    var chapterSearch by remember { mutableStateOf("") }
    var selectedVolume by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var visibleChapterCount by remember { mutableIntStateOf(50) }
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    val cachedPoster = remember(mangaId, currentMangaId) {
        if (coordinator?.activeAnimeId == mangaId.hashCode()) coordinator.activePosterUrl
        else vm.findKnownManga(mangaId)?.poster
            ?: vm.findKnownManga(currentMangaId)?.poster
            ?: vm.getMangaHistoryItem(mangaId)?.poster
            ?: vm.getMangaHistoryItem(currentMangaId)?.poster
            ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == currentMangaId }?.poster
    }

    // Header collapse tracking for sticky top app bar
    val isScrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280
        }
    }

    val navbarAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHero) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "mangaNavbarAlpha"
    )

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isStickyPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val stickyTopOffset by animateDpAsState(
        targetValue = if (isStickyPinned) statusBarTop + 54.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "mangaStickyTopOffset"
    )

    LaunchedEffect(mangaId) {
        val cleanCurrent = MangaRepository.cleanWeebId(mangaId)
        val cached = vm.mangaDetailCache[mangaId] ?: vm.mangaDetailCache[currentMangaId] ?: vm.mangaDetailCache[cleanCurrent]
        if (cached != null) {
            info = cached.info
            chapters = cached.chapters
            currentMangaId = cached.resolvedId
            loading = false
            error = null
        } else {
            loading = true
            error = null
        }
        selectedVolume = null
        try {
            isBookmarked = vm.isMangaBookmarked(mangaId) || vm.isMangaBookmarked(currentMangaId) || vm.isMangaBookmarked(cleanCurrent)
            val fallbackTitle = vm.pendingMangaTitleHint?.also { vm.pendingMangaTitleHint = null }
                ?: vm.findKnownManga(mangaId)?.title
                ?: vm.findKnownManga(currentMangaId)?.title
                ?: vm.findKnownManga(cleanCurrent)?.title
                ?: vm.getMangaHistoryItem(mangaId)?.title
                ?: vm.getMangaHistoryItem(currentMangaId)?.title
                ?: vm.getMangaHistoryItem(cleanCurrent)?.title
                ?: vm.mangaBookmarks.find { it.mangaId == mangaId || it.mangaId == currentMangaId || it.mangaId == cleanCurrent }?.title
                ?: MangaRepository.extractSlugTitle(mangaId)
                ?: if (!mangaId.all { it.isDigit() }) mangaId else null

            val (resolvedId, fetchedInfo, fetchedChapters) = MangaRepository.resolveMangaDetails(mangaId, fallbackTitle)
            val effectiveId = resolvedId.ifBlank { cleanCurrent }
            if (effectiveId.isNotBlank()) {
                currentMangaId = effectiveId
            }

            if (fetchedInfo != null) {
                info = fetchedInfo
                chapters = fetchedChapters
                val cacheEntry = MainViewModel.CachedMangaDetail(currentMangaId, fetchedInfo, fetchedChapters)
                vm.mangaDetailCache[mangaId] = cacheEntry
                vm.mangaDetailCache[cleanCurrent] = cacheEntry
                vm.mangaDetailCache[currentMangaId] = cacheEntry
            } else if (info == null) {
                error = "Unable to load manga. Please try searching from the Manga tab."
            }
        } catch (e: Exception) {
            if (info == null) {
                error = e.message ?: "Failed to load manga details"
            }
        } finally {
            loading = false
        }
    }

    val historyItem: MangaHistoryItem? = remember(vm.mangaHistory, mangaId, currentMangaId) {
        val cleanCurrent = MangaRepository.cleanWeebId(currentMangaId)
        val cleanM = MangaRepository.cleanWeebId(mangaId)
        vm.getMangaHistoryItem(cleanCurrent) ?: vm.getMangaHistoryItem(cleanM) ?: vm.getMangaHistoryItem(currentMangaId) ?: vm.getMangaHistoryItem(mangaId)
    }

    val availableVolumes = remember(chapters) {
        chapters.mapNotNull { it.volume?.takeIf { v -> !v.equals("null", ignoreCase = true) } }.distinct()
    }

    LaunchedEffect(chapterSearch, sortAscending, selectedVolume, mangaId) {
        visibleChapterCount = 50
    }

    val displayChapters = remember(chapters, sortAscending, chapterSearch, selectedVolume) {
        var list = if (sortAscending) chapters else chapters.reversed()
        if (selectedVolume != null) {
            list = list.filter { it.volume == selectedVolume }
        }
        if (chapterSearch.isNotBlank()) {
            val q = chapterSearch.trim().lowercase()
            list = list.filter {
                it.number.lowercase().contains(q) || (it.title?.lowercase()?.contains(q) == true)
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .then(
                if (coordinator?.phase == SharedTransitionPhase.BACKWARD) {
                    Modifier.graphicsLayer { alpha = coordinator.progress.value }
                } else Modifier
            )
    ) {
        if (loading) {
            MangaWaveSkeleton(
                cachedPoster = cachedPoster,
                onBack = onBack,
                coordinator = coordinator
            )
            return@Box
        }

        if (info == null && error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = error ?: "Failed to load manga", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = OttPrimary, contentColor = Color.Black),
                        shape = OttPillShape
                    ) {
                        Text(text = "Go Back", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Box
        }

        val currentInfo = info ?: return@Box

        // ── 1. MAIN SCROLLABLE CONTAINER ──────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // ── Hero Section (Spans ~55-60% of viewport with generous spacing) ───────
            item(key = "manga_hero") {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Ambient Backdrop Cover with Vignette
                    AsyncImage(
                        model = currentInfo.banner ?: currentInfo.poster,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .graphicsLayer {
                                val offset = listState.firstVisibleItemScrollOffset
                                translationY = offset * 0.45f
                                alpha = (1f - (offset / 550f)).coerceIn(0.12f, 1f)
                            },
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .background(OttHeroGradient)
                    )

                    // Hero Foreground Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 54.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        // Artwork + Meta Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prominent Poster Card (Target of shared-element transition)
                            Box(
                                modifier = Modifier
                                    .width(125.dp)
                                    .height(180.dp)
                                    .onGloballyPositioned { coords ->
                                        coordinator?.updateTargetBounds(coords.boundsInWindow())
                                    }
                                    .clip(OttCardShape)
                                    .background(Color(0x20FFFFFF))
                                    .border(1.dp, Color(0x35FFFFFF), OttCardShape)
                                    .shadow(12.dp, OttCardShape)
                            ) {
                                AsyncImage(
                                    model = currentInfo.poster,
                                    contentDescription = currentInfo.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            this.alpha = if (coordinator?.phase == SharedTransitionPhase.FORWARD) 0f else 1f
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Metadata Details Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        if (coordinator != null && (coordinator.phase == SharedTransitionPhase.FORWARD || coordinator.phase == SharedTransitionPhase.BACKWARD)) {
                                            val p = coordinator.progress.value
                                            alpha = p
                                            translationX = (1f - p) * 32.dp.toPx()
                                        } else {
                                            alpha = 1f
                                            translationX = 0f
                                        }
                                    },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentInfo.title,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OttTextPrimary,
                                    maxLines = 3,
                                    lineHeight = 23.sp,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (!currentInfo.altTitle.isNullOrBlank()) {
                                    Text(
                                        text = currentInfo.altTitle,
                                        fontSize = 12.sp,
                                        color = OttSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                 // Badges FlowRow
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val activeSource = MangaRepository.getSourceForId(currentMangaId)
                                    val (badgeBg, badgeBorder, badgeText) = when (activeSource) {
                                        MangaSource.MANGADEX -> Triple(Color(0x25FF6740), Color(0x60FF6740), Color(0xFFFF8A65))
                                        MangaSource.COMICK -> Triple(Color(0x253B82F6), Color(0x603B82F6), Color(0xFF60A5FA))
                                        MangaSource.ASURA -> Triple(Color(0x2510B981), Color(0x6010B981), Color(0xFF34D399))
                                        MangaSource.WEEBCENTRAL -> Triple(Color(0x258B5CF6), Color(0x608B5CF6), Color(0xFFA78BFA))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(badgeBg)
                                            .border(0.8.dp, badgeBorder, OttPillShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = activeSource.displayName.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeText,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(Color(0x25E50914))
                                            .border(0.8.dp, Color(0x60E50914), OttPillShape)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = (currentInfo.type ?: "MANGA").uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF8A8A),
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }

                                    if (!currentInfo.rating.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x25F59E0B))
                                                .border(0.8.dp, Color(0x60F59E0B), OttPillShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = OttAccentGold,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = currentInfo.rating,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OttAccentGold,
                                                    softWrap = false,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    if (!currentInfo.status.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x2510B981))
                                                .border(0.8.dp, Color(0x6010B981), OttPillShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = currentInfo.status.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6EE7B7),
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (chapters.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x18FFFFFF))
                                                .border(0.8.dp, Color(0x25FFFFFF), OttPillShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${chapters.size} CHS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OttTextMuted,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Primary Action Button (Start / Continue Reading)
                        val targetChapter = if (historyItem?.chapterId != null) {
                            chapters.find { MangaRepository.cleanChapterId(it.chapterId) == MangaRepository.cleanChapterId(historyItem.chapterId) }
                                ?: chapters.find { it.number == historyItem.chapterNumber }
                        } else {
                            chapters.find { it.number == "1" || it.number == "1.0" } ?: chapters.firstOrNull()
                        }
                        val targetChapterId = targetChapter?.chapterId?.let { MangaRepository.cleanChapterId(it) }
                            ?: historyItem?.chapterId?.let { MangaRepository.cleanChapterId(it) }

                        Button(
                            onClick = {
                                targetChapterId?.let { onReadChapter(MangaRepository.cleanWeebId(currentMangaId), it) }
                            },
                            enabled = targetChapterId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = OttPillShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            val continueBtnText = if (historyItem?.chapterNumber != null) {
                                val pageInfo = if (historyItem.pageCount > 0) " • Pg ${historyItem.pageIndex + 1}/${historyItem.pageCount}" else ""
                                "Continue Ch. ${historyItem.chapterNumber}$pageInfo"
                            } else "Start Reading Ch. 1"
                            Text(
                                text = continueBtnText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        // Interactive Source Switcher Chips Row
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeSrc = MangaRepository.getSourceForId(currentMangaId)
                            MangaSource.entries.forEach { srcOption ->
                                val isSelected = activeSrc == srcOption
                                val (activeBg, activeBorder, activeTextColor) = when (srcOption) {
                                    MangaSource.MANGADEX -> Triple(Color(0x35FF6740), Color(0x90FF6740), Color(0xFFFF8A65))
                                    MangaSource.COMICK -> Triple(Color(0x353B82F6), Color(0x903B82F6), Color(0xFF60A5FA))
                                    MangaSource.ASURA -> Triple(Color(0x3510B981), Color(0x9010B981), Color(0xFF34D399))
                                    MangaSource.WEEBCENTRAL -> Triple(Color(0x358B5CF6), Color(0x908B5CF6), Color(0xFFA78BFA))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(OttPillShape)
                                        .background(if (isSelected) activeBg else Color(0x12FFFFFF))
                                        .border(
                                            1.dp,
                                            if (isSelected) activeBorder else Color(0x20FFFFFF),
                                            OttPillShape
                                        )
                                        .clickable(enabled = !isSwitchingSource && !isSelected) {
                                            scope.launch {
                                                isSwitchingSource = true
                                                try {
                                                    val targetRepo = MangaRepository.getRepoForSource(srcOption)
                                                    val searchResults = targetRepo.search(currentInfo.title)
                                                    val match = searchResults.find { it.title.equals(currentInfo.title, ignoreCase = true) }
                                                        ?: searchResults.firstOrNull()
                                                    if (match != null) {
                                                        val (resolvedId, newInfo, newChapters) = MangaRepository.resolveMangaDetails(match.id, currentInfo.title)
                                                        if (newInfo != null) {
                                                            currentMangaId = resolvedId
                                                            info = newInfo
                                                            chapters = newChapters
                                                            val cacheEntry = MainViewModel.CachedMangaDetail(resolvedId, newInfo, newChapters)
                                                            vm.mangaDetailCache[resolvedId] = cacheEntry
                                                            vm.mangaDetailCache[mangaId] = cacheEntry
                                                        }
                                                    } else {
                                                        android.widget.Toast.makeText(context, "No match found on ${srcOption.displayName}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSwitchingSource = false
                                                }
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = srcOption.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeTextColor else OttTextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Expandable Synopsis Snippet
                        if (!currentInfo.description.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = currentInfo.description,
                                fontSize = 12.sp,
                                color = OttTextSecondary,
                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp,
                                modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            )
                            Text(
                                text = if (isSynopsisExpanded) "Show Less" else "Read More...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OttPrimary,
                                modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            )
                        }
                    }
                }
            }

            // ── Sticky Header: Segmented Tabs & Chapter Filters ──────────────────
            stickyHeader(key = "manga_sticky_tabs") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OttBg)
                        .padding(top = stickyTopOffset)
                        .padding(vertical = 6.dp)
                ) {
                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Chapters (${chapters.size})", "Overview & Info").forEachIndexed { index, tabTitle ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(OttPillShape)
                                    .background(if (isSelected) Color.White else Color(0x14FFFFFF))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color(0x22FFFFFF),
                                        OttPillShape
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Search & Sort Bar (Only in Chapters tab)
                    if (selectedTab == 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Field
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x16FFFFFF))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = OttSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (chapterSearch.isEmpty()) {
                                            Text(
                                                text = "Filter chapters...",
                                                color = OttTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = chapterSearch,
                                            onValueChange = { chapterSearch = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            cursorBrush = SolidColor(OttPrimary),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )
                                    }
                                    if (chapterSearch.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable { chapterSearch = "" }
                                        )
                                    }
                                }
                            }

                            // Sort Order Toggle
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x16FFFFFF))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { sortAscending = !sortAscending }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = OttPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (sortAscending) "1 → N" else "N → 1",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Volumes Row (if multiple volumes available)
                        if (availableVolumes.size > 1) {
                            Spacer(Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    val isAll = selectedVolume == null
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAll) Color.White else Color(0x14FFFFFF))
                                            .border(1.dp, if (isAll) Color.White else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { selectedVolume = null }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "All Volumes",
                                            fontSize = 11.sp,
                                            fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAll) Color.Black else Color.White
                                        )
                                    }
                                }
                                items(availableVolumes) { vol ->
                                    val isSelected = selectedVolume == vol
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color.White else Color(0x14FFFFFF))
                                            .border(1.dp, if (isSelected) Color.White else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { selectedVolume = if (isSelected) null else vol }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = vol,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Tab 0: Chapters List Content ──────────────────────────────────────
            if (selectedTab == 0) {
                val pagedChapters = displayChapters.take(visibleChapterCount)

                // Batch Download Controls Row
                item(key = "manga_batch_downloads") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(OttPillShape)
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x25FFFFFF), OttPillShape)
                                .clickable {
                                    val targetChapters = pagedChapters.take(5).map { it.chapterId to "Chapter ${it.number}" }
                                    vm.downloadMangaChaptersBatch(currentMangaId, info?.title ?: currentMangaId, info?.poster ?: "", targetChapters)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text("Download 5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(OttPillShape)
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x25FFFFFF), OttPillShape)
                                .clickable {
                                    val targetChapters = pagedChapters.map { it.chapterId to "Chapter ${it.number}" }
                                    vm.downloadMangaChaptersBatch(currentMangaId, info?.title ?: currentMangaId, info?.poster ?: "", targetChapters)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text("Download Loaded (${pagedChapters.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                if (displayChapters.isEmpty()) {
                    item(key = "manga_no_chapters") {
                        val activeSrc = MangaRepository.getSourceForId(currentMangaId)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 32.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x10FFFFFF))
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (chapterSearch.isNotBlank()) "No chapters matching '$chapterSearch'" else "No readable chapters on ${activeSrc.displayName}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (chapterSearch.isBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Some titles are restricted or licensed here. Tap another source chip above (ComicK, WeebCentral, or Asura Scans) to load chapters.",
                                        color = OttTextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(pagedChapters, key = { it.chapterId }) { ch ->
                        val isCurrent = historyItem?.chapterId == ch.chapterId || historyItem?.chapterNumber == ch.number
                        val isChBookmarked = vm.isChapterBookmarked(currentMangaId, ch.chapterId) || vm.isChapterBookmarked(mangaId, ch.chapterId)
                        val isDownloaded = remember(downloadTasks, ch.chapterId) {
                            vm.downloadManager.isMangaChapterDownloaded(currentMangaId, ch.chapterId)
                        }
                        val activeTask = remember(downloadTasks, ch.chapterId) {
                            downloadTasks.find { (it.itemId == currentMangaId || it.itemId == mangaId) && it.chapterId == ch.chapterId }
                        }
                        val isDownloading = activeTask?.status == com.nyyrox.aniverse.data.download.DownloadStatus.DOWNLOADING || activeTask?.status == com.nyyrox.aniverse.data.download.DownloadStatus.QUEUED

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isChBookmarked -> Color(0x280284C7)
                                        isCurrent -> Color(0x18FFFFFF)
                                        else -> Color(0x0EFFFFFF)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isChBookmarked -> Color(0xFF38BDF8)
                                        isCurrent -> Color(0x80E50914)
                                        else -> Color(0x16FFFFFF)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onReadChapter(MangaRepository.cleanWeebId(currentMangaId), MangaRepository.cleanChapterId(ch.chapterId)) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isChBookmarked) {
                                            Icon(
                                                Icons.Default.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = "Chapter ${ch.number}",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChBookmarked) Color(0xFF38BDF8) else Color.White
                                        )
                                        val cleanChId = MangaRepository.cleanChapterId(ch.chapterId)
                                        val savedChPage = vm.getMangaChapterPageProgress(currentMangaId, cleanChId)
                                        val chapterPageText = if (isCurrent && historyItem != null && historyItem.pageCount > 0) {
                                            " • Pg ${historyItem.pageIndex + 1}/${historyItem.pageCount}"
                                        } else if (savedChPage > 0) {
                                            " • Pg ${savedChPage + 1}"
                                        } else null
                                        if (chapterPageText != null) {
                                            Text(
                                                text = chapterPageText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isCurrent) OttPrimary else OttSecondary
                                            )
                                        }
                                        val cleanVol = ch.volume?.takeIf { !it.equals("null", ignoreCase = true) && it.isNotBlank() }
                                        if (cleanVol != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x18FFFFFF))
                                                    .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = cleanVol,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                            }
                                        }
                                    }
                                    val cleanTitle = ch.title?.takeIf { !it.equals("null", ignoreCase = true) && it.isNotBlank() }
                                    if (cleanTitle != null) {
                                        Text(
                                            text = cleanTitle,
                                            fontSize = 11.5.sp,
                                            color = OttTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isDownloading && activeTask != null) {
                                        Text(
                                            text = "${activeTask.percentage}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OttAccentGold
                                        )
                                        CircularProgressIndicator(
                                            color = OttAccentGold,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (!isDownloaded) {
                                                    vm.downloadMangaChapter(currentMangaId, info?.title ?: currentMangaId, info?.poster ?: "", ch.chapterId, "Chapter ${ch.number}")
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DownloadDone,
                                                contentDescription = if (isDownloaded) "Downloaded" else "Download",
                                                tint = if (isDownloaded) OttAccentGold else Color(0x55FFFFFF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (isChBookmarked && !isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x300284C7))
                                                .border(1.dp, Color(0xFF38BDF8), OttPillShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "BOOKMARKED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }

                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x30E50914))
                                                .border(1.dp, Color(0x70E50914), OttPillShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "READING",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF8A8A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Load More Chapters Button
                    if (displayChapters.size > visibleChapterCount) {
                        item(key = "manga_load_more") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { visibleChapterCount += 50 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .border(1.dp, Color(0x30FFFFFF), OttPillShape),
                                    shape = OttPillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0x20FFFFFF),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "Load More Chapters (+50)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                if (displayChapters.size > visibleChapterCount + 50) {
                                    Button(
                                        onClick = { visibleChapterCount = displayChapters.size },
                                        modifier = Modifier
                                            .height(42.dp)
                                            .border(1.dp, Color(0x22FFFFFF), OttPillShape),
                                        shape = OttPillShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0x14FFFFFF),
                                            contentColor = Color(0xFFCBD5E1)
                                        )
                                    ) {
                                        Text(
                                            text = "Show All (${displayChapters.size})",
                                            color = Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Tab 1: Overview & Info Content ────────────────────────────────────
                if (currentInfo.description.isNotBlank()) {
                    item(key = "manga_synopsis_card") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x10FFFFFF))
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "SYNOPSIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = OttSecondary
                                )
                                Text(
                                    text = currentInfo.description,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFFCAD5E2),
                                    lineHeight = 18.sp,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSynopsisExpanded) "Show Less" else "Read More...",
                                    color = OttPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (currentInfo.genres.isNotEmpty()) {
                    item(key = "manga_genres_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "GENRES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OttSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentInfo.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(Color(0x18FFFFFF))
                                            .border(1.dp, Color(0x28FFFFFF), OttPillShape)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentInfo.authors.isNotEmpty()) {
                    item(key = "manga_authors_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AUTHORS & ARTISTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OttSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentInfo.authors.forEach { author ->
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(Color(0x18FFFFFF))
                                            .border(1.dp, Color(0x28FFFFFF), OttPillShape)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = author,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "manga_source_card") {
                    val activeSrc = MangaRepository.getSourceForId(currentMangaId)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x10FFFFFF))
                            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "METADATA & SOURCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OttSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Current Source", fontSize = 12.sp, color = OttTextMuted)
                                Text(activeSrc.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            if (currentInfo.anilistId != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AniList Mapping", fontSize = 12.sp, color = OttTextMuted)
                                    Text("#${currentInfo.anilistId} (Mapped)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                                }
                            }
                            if (!currentInfo.rating.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Community Rating", fontSize = 12.sp, color = OttTextMuted)
                                    Text(currentInfo.rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttAccentGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 2. COLLAPSING TOP APP BAR WITH SHOW NAME & CHAPTER COUNT ──────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color(0xF00A0C14).copy(alpha = navbarAlpha))
                .drawBehind {
                    if (navbarAlpha > 0.05f) {
                        drawLine(
                            color = Color(0x18FFFFFF).copy(alpha = navbarAlpha),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                }
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x60000000))
                        .border(1.dp, Color(0x35FFFFFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Centered Show Name + Chapter Count in Navbar
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .graphicsLayer { alpha = navbarAlpha },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentInfo.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${displayChapters.size} Chapters",
                        fontSize = 11.sp,
                        color = OttAccentGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Action Buttons (Share & Bookmark)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Read ${currentInfo.title} on ZENKAI: https://aniverse-kt.vercel.app/manga/$currentMangaId"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Manga"))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x60000000))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isBookmarked = vm.toggleMangaBookmark(
                                mangaId = currentMangaId,
                                title = currentInfo.title,
                                poster = currentInfo.poster,
                                type = currentInfo.type,
                                latestChapter = chapters.lastOrNull()?.number
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBookmarked) Color(0x30E50914) else Color(0x60000000))
                            .border(
                                1.dp,
                                if (isBookmarked) Color(0x80E50914) else Color(0x35FFFFFF),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFFF4D4D) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaWaveSkeleton(
    cachedPoster: String?,
    onBack: () -> Unit,
    coordinator: SharedPosterCoordinator?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mangaWaveTransition")

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val waveBreathe by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveBreathe"
    )

    val waveBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131722).copy(alpha = 0.65f),
            Color(0xFF1E2638).copy(alpha = 0.85f),
            Color(0xFF323F58).copy(alpha = 0.95f),
            Color(0xFF1E2638).copy(alpha = 0.85f),
            Color(0xFF131722).copy(alpha = 0.65f),
        ),
        start = Offset((wavePhase * 1600f) - 600f, (wavePhase * 600f) - 200f),
        end = Offset((wavePhase * 1600f) + 400f, (wavePhase * 600f) + 400f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .then(
                if (coordinator?.phase == SharedTransitionPhase.BACKWARD) {
                    Modifier.graphicsLayer { alpha = coordinator.progress.value }
                } else Modifier
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Skeleton
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Ambient Backdrop
                    if (!cachedPoster.isNullOrBlank()) {
                        AsyncImage(
                            model = cachedPoster,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(460.dp)
                                .background(waveBrush)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .background(OttHeroGradient)
                    )

                    // Hero Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 54.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Poster Card
                            Box(
                                modifier = Modifier
                                    .width(125.dp)
                                    .height(180.dp)
                                    .clip(OttCardShape)
                                    .background(waveBrush)
                                    .border(1.dp, Color(0x35FFFFFF), OttCardShape)
                                    .shadow(12.dp, OttCardShape)
                            ) {
                                if (!cachedPoster.isNullOrBlank()) {
                                    AsyncImage(
                                        model = cachedPoster,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            // Meta column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(waveBrush)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(waveBrush)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(54.dp, 22.dp).clip(OttPillShape).background(waveBrush))
                                    Box(modifier = Modifier.size(60.dp, 22.dp).clip(OttPillShape).background(waveBrush))
                                    Box(modifier = Modifier.size(48.dp, 22.dp).clip(OttPillShape).background(waveBrush))
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Full width read button placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(OttPillShape)
                                .background(waveBrush)
                        )
                    }
                }
            }

            // 2. Source chips placeholder
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(100.dp, 34.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(90.dp, 34.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(95.dp, 34.dp).clip(OttPillShape).background(waveBrush))
                    Box(modifier = Modifier.size(105.dp, 34.dp).clip(OttPillShape).background(waveBrush))
                }
            }

            // 3. Synopsis lines placeholder
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                    Box(modifier = Modifier.fillMaxWidth(0.92f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                    Box(modifier = Modifier.fillMaxWidth(0.70f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(waveBrush))
                }
            }

            // 4. Chapter search / controls bar placeholder
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(130.dp, 20.dp).clip(RoundedCornerShape(6.dp)).background(waveBrush))
                    Box(modifier = Modifier.size(36.dp, 36.dp).clip(CircleShape).background(waveBrush))
                }
            }

            // 5. Chapter rows placeholders (6 chapters)
            items(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(waveBrush)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp, 16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(waveBrush)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(waveBrush)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(waveBrush)
                        )
                    }
                }
            }
        }

        // Top App Bar skeleton overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x60000000))
                    .border(1.dp, Color(0x35FFFFFF), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
