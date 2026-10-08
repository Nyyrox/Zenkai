package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedTransitionPhase
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttHeroGradient
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NovelDetailScreen(
    vm: MainViewModel,
    novelSlug: String,
    onBack: () -> Unit,
    onReadChapter: (novelSlug: String, chapterSlug: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coordinator = LocalSharedPosterCoordinator.current
    val listState = rememberLazyListState()

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

    LaunchedEffect(novelSlug) {
        vm.loadNovelDetail(novelSlug)
    }

    val novel = vm.selectedNovel
    val isBookmarked = vm.novelBookmarks.contains(novelSlug)
    val lastReadChapter = vm.novelHistory[novelSlug] ?: vm.novelHistoryList.find { it.slug == novelSlug }?.lastChapterSlug
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    var synopsisExpanded by remember { mutableStateOf(false) }
    var chapterSearch by remember { mutableStateOf("") }
    var isReversedOrder by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Header collapse tracking for sticky top app bar
    val isScrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280
        }
    }

    val navbarAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHero) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "navbarAlpha"
    )

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isStickyPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val stickyTopOffset by animateDpAsState(
        targetValue = if (isStickyPinned) statusBarTop + 54.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "novelStickyTopOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
            .then(
                if (coordinator?.phase == SharedTransitionPhase.BACKWARD) {
                    Modifier.graphicsLayer { alpha = coordinator.progress.value }
                } else Modifier
            )
    ) {
        if (vm.novelDetailLoading && novel == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            }
            return@Box
        }

        if (vm.novelDetailError != null && novel == null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to Load", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OttTextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(vm.novelDetailError ?: "Unable to fetch novel details", fontSize = 12.sp, color = OttTextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { vm.loadNovelDetail(novelSlug) },
                        colors = ButtonDefaults.buttonColors(containerColor = OttPrimary, contentColor = Color.Black),
                        shape = OttPillShape
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Box
        }

        if (novel != null) {
            val displayChapters = remember(vm.novelChapters, chapterSearch, isReversedOrder) {
                var list = vm.novelChapters
                if (chapterSearch.isNotBlank()) {
                    list = list.filter {
                        it.title.contains(chapterSearch, ignoreCase = true) || it.slug.contains(chapterSearch, ignoreCase = true)
                    }
                }
                if (isReversedOrder) list.reversed() else list
            }

            // ── 1. MAIN SCROLLABLE CONTAINER ──────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // ── Hero Section (Spans ~55-60% of viewport with generous spacing) ───────
                item(key = "novel_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Ambient Backdrop Cover with Vignette
                        AsyncImage(
                            model = novel.cover,
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
                                        model = novel.cover,
                                        contentDescription = novel.title,
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
                                        text = novel.title,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OttTextPrimary,
                                        maxLines = 3,
                                        lineHeight = 23.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!novel.author.isNullOrBlank()) {
                                        Text(
                                            text = "By ${novel.author}",
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
                                        Box(
                                            modifier = Modifier
                                                .clip(OttPillShape)
                                                .background(Color(0x2538BDF8))
                                                .border(0.8.dp, Color(0x6038BDF8), OttPillShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "LIGHT NOVEL",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7DD3FC),
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }

                                        if (!novel.status.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(Color(0x2510B981))
                                                    .border(0.8.dp, Color(0x6010B981), OttPillShape)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = novel.status.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6EE7B7),
                                                    softWrap = false,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (!novel.chaptersCount.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(Color(0x18FFFFFF))
                                                    .border(0.8.dp, Color(0x25FFFFFF), OttPillShape)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${novel.chaptersCount} CHS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OttTextMuted,
                                                    softWrap = false,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (!novel.rating.isNullOrBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(Color(0x25F59E0B))
                                                    .border(0.8.dp, Color(0x60F59E0B), OttPillShape)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
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
                                                    text = novel.rating,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OttAccentGold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Primary Action Button (Start / Continue Reading)
                            val targetChapterSlug = lastReadChapter ?: displayChapters.firstOrNull()?.slug ?: novel.firstChapterSlug
                            Button(
                                onClick = {
                                    targetChapterSlug?.let { onReadChapter(novelSlug, it) }
                                },
                                enabled = targetChapterSlug != null,
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
                                Text(
                                    text = if (lastReadChapter != null) "Continue Reading" else "Start Reading Ch. 1",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Expandable Synopsis Snippet
                            if (!novel.synopsis.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = novel.synopsis,
                                    fontSize = 12.sp,
                                    color = OttTextSecondary,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                )
                                Text(
                                    text = if (synopsisExpanded) "Show Less" else "Read More...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OttPrimary,
                                    modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                )
                            }
                        }
                    }
                }

                // ── Sticky Header: Segmented Tabs & Chapter Filters ──────────────────
                stickyHeader(key = "novel_sticky_tabs") {
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
                            listOf("Chapters (${displayChapters.size})", "Overview & Info").forEachIndexed { index, tabTitle ->
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
                                        .clickable { isReversedOrder = !isReversedOrder }
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
                                            text = if (!isReversedOrder) "1 → N" else "N → 1",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Tab 0: Chapters List Content ──────────────────────────────────────
                if (selectedTab == 0) {
                    // Batch Download Controls Row
                    item(key = "novel_batch_downloads") {
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
                                        val targetChapters = displayChapters.take(10).map { it.slug to it.title }
                                        vm.downloadNovelChaptersBatch(novelSlug, novel.title, novel.cover ?: "", targetChapters)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Download 10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(OttPillShape)
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x25FFFFFF), OttPillShape)
                                    .clickable {
                                        val targetChapters = displayChapters.map { it.slug to it.title }
                                        vm.downloadNovelChaptersBatch(novelSlug, novel.title, novel.cover ?: "", targetChapters)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Download All (${displayChapters.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    if (displayChapters.isEmpty()) {
                        item(key = "novel_no_chapters") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (chapterSearch.isNotBlank()) "No chapters matching '$chapterSearch'" else "No chapters found",
                                    color = OttTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(displayChapters, key = { it.slug }) { ch ->
                            val isCurrent = lastReadChapter == ch.slug
                            val isDownloaded = remember(downloadTasks, ch.slug) {
                                vm.downloadManager.isNovelChapterDownloaded(novelSlug, ch.slug)
                            }
                            val activeTask = remember(downloadTasks, ch.slug) {
                                downloadTasks.find { it.itemId == novelSlug && it.chapterId == ch.slug }
                            }
                            val isDownloading = activeTask?.status == com.nyyrox.aniverse.data.download.DownloadStatus.DOWNLOADING || activeTask?.status == com.nyyrox.aniverse.data.download.DownloadStatus.QUEUED

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCurrent) Color(0x20E50914)
                                        else Color(0x10FFFFFF)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) Color(0x80E50914) else Color(0x18FFFFFF),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onReadChapter(novelSlug, ch.slug) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color(0xFFFFB3B3) else Color.White
                                        )
                                        if (!ch.releaseTime.isNullOrBlank()) {
                                            Text(
                                                text = ch.releaseTime,
                                                fontSize = 11.sp,
                                                color = OttTextMuted,
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
                                                color = Color.White
                                            )
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    if (!isDownloaded) {
                                                        vm.downloadNovelChapter(novelSlug, novel.title, novel.cover ?: "", ch.slug, ch.title)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DownloadDone,
                                                    contentDescription = if (isDownloaded) "Downloaded" else "Download",
                                                    tint = if (isDownloaded) Color.White else Color(0x55FFFFFF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(OttPillShape)
                                                    .background(Color(0x30E50914))
                                                    .border(0.8.dp, Color(0x80E50914), OttPillShape)
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
                        if (vm.novelChaptersPage < vm.novelChaptersMaxPage) {
                            item(key = "novel_load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(OttPillShape)
                                            .background(Color(0x20FFFFFF))
                                            .border(1.dp, Color(0x30FFFFFF), OttPillShape)
                                            .clickable(enabled = !vm.novelChaptersLoading) {
                                                vm.loadMoreNovelChapters(novelSlug)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (vm.novelChaptersLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Load More Chapters (Page ${vm.novelChaptersPage + 1} of ${vm.novelChaptersMaxPage})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── Tab 1: Overview & Info Content ────────────────────────────────────
                    if (novel.synopsis.isNotBlank()) {
                        item(key = "novel_synopsis_card") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x10FFFFFF))
                                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { synopsisExpanded = !synopsisExpanded }
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
                                        text = novel.synopsis,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFCAD5E2),
                                        lineHeight = 18.sp,
                                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 6,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (synopsisExpanded) "Show Less" else "Read More...",
                                        color = OttPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (novel.genres.isNotEmpty()) {
                        item(key = "novel_genres_card") {
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
                                    novel.genres.forEach { genre ->
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
                            text = novel.title,
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
                                        "Read ${novel.title} on ZENKAI: https://aniverse-kt.vercel.app/novel/$novelSlug"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Novel"))
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
                            onClick = { vm.toggleNovelBookmark(novelSlug) },
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
                                tint = if (isBookmarked) Color(0xFFFF8A8A) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
