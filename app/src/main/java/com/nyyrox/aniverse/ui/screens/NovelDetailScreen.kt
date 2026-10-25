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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
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
import com.nyyrox.aniverse.data.MediaRelation
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedTransitionPhase
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NovelDetailScreen(
    vm: MainViewModel,
    novelSlug: String,
    onBack: () -> Unit,
    onReadChapter: (novelSlug: String, chapterSlug: String) -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
    onMangaClick: ((String) -> Unit)? = null,
    onNovelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coordinator = LocalSharedPosterCoordinator.current
    val p = rememberHomePalette()
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
    val knownNovel = remember(novelSlug) { vm.findKnownNovel(novelSlug) }
    val effectiveCover = novel?.cover?.takeIf { it.isNotBlank() && !it.contains("/series/") }
        ?: knownNovel?.cover
        ?: novel?.cover
    val isBookmarked = vm.novelBookmarks.contains(novelSlug)
    val lastReadChapter = vm.novelHistory[novelSlug] ?: vm.novelHistoryList.find { it.slug == novelSlug }?.lastChapterSlug
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    var synopsisExpanded by remember { mutableStateOf(false) }
    var chapterSearch by remember { mutableStateOf("") }
    var isReversedOrder by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val isScrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
        }
    }

    val navbarAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastHero) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
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

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (coordinator?.phase == SharedTransitionPhase.BACKWARD) {
                        Modifier.graphicsLayer { alpha = coordinator.progress.value }
                    } else Modifier
                )
        ) {
            if (vm.novelDetailLoading && novel == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accentA, modifier = Modifier.size(36.dp), strokeWidth = 2.5.dp)
                }
                return@Box
            }

            if (vm.novelDetailError != null && novel == null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to Load", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = p.text)
                        Spacer(Modifier.height(6.dp))
                        Text(vm.novelDetailError ?: "Unable to fetch novel details", fontSize = 13.sp, color = p.textSub, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.chipBg)
                                .clickable { vm.loadNovelDetail(novelSlug) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = p.text, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold, color = p.text)
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

                val backdropUrl = novel.banner ?: effectiveCover
                val relAnime = novel.relations.firstOrNull { it.type.equals("ANIME", ignoreCase = true) }
                val relManga = novel.relations.firstOrNull { it.type.equals("MANGA", ignoreCase = true) }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // 1. Hero Section
                    item(key = "novel_hero") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (backdropUrl != null) {
                                NovelDetailBackdropArt(backdropUrl, p, Modifier.matchParentSize())
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(top = 52.dp, start = 18.dp, end = 18.dp, bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Poster Cover (Clean, rounded, no card border)
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(175.dp)
                                            .onGloballyPositioned { coords ->
                                                coordinator?.updateTargetBounds(coords.boundsInWindow())
                                            }
                                            .clip(RoundedCornerShape(14.dp))
                                            .shadow(12.dp, RoundedCornerShape(14.dp))
                                            .shimmer(p)
                                    ) {
                                        AsyncImage(
                                            model = rememberNovelImageRequest(effectiveCover),
                                            contentDescription = novel.title,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    this.alpha = if (coordinator?.phase == SharedTransitionPhase.FORWARD) 0f else 1f
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Metadata Column
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = novel.title,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ZenkaiFontFamily,
                                            color = p.text,
                                            maxLines = 3,
                                            lineHeight = 22.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!novel.author.isNullOrBlank()) {
                                            Text(
                                                text = "By ${novel.author}",
                                                fontSize = 12.sp,
                                                color = p.textSub,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Badges Row
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(p.chipBg)
                                                    .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    text = novel.source.uppercase(),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = p.accentText
                                                )
                                            }

                                            if (!novel.status.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(p.chipBg)
                                                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                                ) {
                                                    Text(
                                                        text = novel.status.uppercase(),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = p.textSub
                                                    )
                                                }
                                            }

                                            if (!novel.chaptersCount.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(p.chipBg)
                                                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                                ) {
                                                    Text(
                                                        text = "${novel.chaptersCount} CHS",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = p.textSub
                                                    )
                                                }
                                            }

                                            if (!novel.rating.isNullOrBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(p.chipBg)
                                                        .padding(horizontal = 8.dp, vertical = 2.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = p.rating,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = novel.rating,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = p.rating
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Primary Action: Start / Continue Reading Button
                                val targetChapterSlug = lastReadChapter ?: displayChapters.firstOrNull()?.slug ?: novel.firstChapterSlug
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.accentBrush)
                                        .clickable(enabled = targetChapterSlug != null) {
                                            targetChapterSlug?.let { onReadChapter(novelSlug, it) }
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = p.onAccent
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (lastReadChapter != null) "Continue Reading" else "Start Reading Ch. 1",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = p.onAccent
                                    )
                                }

                                // Quick Action CTA: Watch Anime Adaptation (if available)
                                if (relAnime != null && onAnimeClick != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(p.chipBg)
                                            .clickable {
                                                onAnimeClick(relAnime.id)
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

                                // Quick Action CTA: Read Manga Adaptation (if available)
                                if (relManga != null && onMangaClick != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(p.chipBg)
                                            .clickable {
                                                onMangaClick(relManga.id.toString())
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = p.accentText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Read Manga Adaptation",
                                            color = p.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Expandable Synopsis
                                if (novel.synopsis.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = novel.synopsis,
                                        fontSize = 12.5.sp,
                                        color = p.textSub,
                                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = if (synopsisExpanded) "Show Less" else "Read More...",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.accentText,
                                        modifier = Modifier.clickable { synopsisExpanded = !synopsisExpanded }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Related Media Section (Anime adaptations, Manga adaptations, Related Novels)
                    if (novel.relations.isNotEmpty()) {
                        item(key = "novel_relations") {
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
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(novel.relations, key = { "${it.type}_${it.id}" }) { rel ->
                                        NovelRelatedMediaTile(
                                            item = rel,
                                            p = p,
                                            onClick = {
                                                when {
                                                    rel.type.equals("ANIME", ignoreCase = true) -> onAnimeClick?.invoke(rel.id)
                                                    rel.type.equals("MANGA", ignoreCase = true) -> onMangaClick?.invoke(rel.id.toString())
                                                    else -> onNovelClick?.invoke(rel.title)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Sticky Tabs & Chapter Filter Bar
                    stickyHeader(key = "novel_sticky_tabs") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(p.bg)
                                .padding(top = stickyTopOffset)
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isVolumeBased = novel.source == "lnori" || displayChapters.any { it.title.startsWith("Volume", ignoreCase = true) }
                                val chaptersTabTitle = if (isVolumeBased) "Volumes (${displayChapters.size})" else "Chapters (${displayChapters.size})"
                                listOf(chaptersTabTitle, "Overview & Info").forEachIndexed { index, tabTitle ->
                                    val isSelected = selectedTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(999.dp))
                                            .then(
                                                if (isSelected) Modifier.background(p.chipActiveBg)
                                                else Modifier.background(p.chipBg)
                                            )
                                            .clickable { selectedTab = index }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            color = if (isSelected) p.chipActiveText else p.textSub,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Search & Sort Bar (in Chapters tab)
                            if (selectedTab == 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Search Filter Input
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.chipBg)
                                            .padding(horizontal = 12.dp),
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
                                                tint = p.textMuted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (chapterSearch.isEmpty()) {
                                                    Text(
                                                        text = "Filter chapters...",
                                                        color = p.textMuted,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                BasicTextField(
                                                    value = chapterSearch,
                                                    onValueChange = { chapterSearch = it },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = p.text,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    cursorBrush = SolidColor(p.accentA),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                )
                                            }
                                            if (chapterSearch.isNotEmpty()) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    tint = p.textMuted,
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
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.chipBg)
                                            .clickable { isReversedOrder = !isReversedOrder }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SwapVert,
                                                contentDescription = null,
                                                tint = p.accentText,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (!isReversedOrder) "1 → N" else "N → 1",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.text
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Tab 0: Chapters List Content
                    if (selectedTab == 0) {
                        // Batch Download Controls Row
                        item(key = "novel_batch_downloads") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.chipBg)
                                        .clickable {
                                            val targetChapters = displayChapters.take(10).map { it.slug to it.title }
                                            vm.downloadNovelChaptersBatch(novelSlug, novel.title, effectiveCover ?: "", targetChapters)
                                        }
                                        .padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = p.text, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download 10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.text)
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(p.chipBg)
                                        .clickable {
                                            val targetChapters = displayChapters.map { it.slug to it.title }
                                            vm.downloadNovelChaptersBatch(novelSlug, novel.title, effectiveCover ?: "", targetChapters)
                                        }
                                        .padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = p.text, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download All (${displayChapters.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.text)
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
                                        color = p.textMuted,
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isCurrent) Modifier.background(p.chipBg)
                                            else Modifier
                                        )
                                        .clickable { onReadChapter(novelSlug, ch.slug) }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Volume Cover Artwork Thumbnail (when present)
                                    if (!ch.cover.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .width(44.dp)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .shimmer(p)
                                        ) {
                                            AsyncImage(
                                                model = rememberNovelImageRequest(ch.cover),
                                                contentDescription = ch.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) p.accentText else p.text
                                        )
                                        if (!ch.releaseTime.isNullOrBlank()) {
                                            Text(
                                                text = ch.releaseTime,
                                                fontSize = 11.sp,
                                                color = p.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Share Chapter
                                        IconButton(
                                            onClick = {
                                                val chNumMatch = Regex("\\d+").find(ch.slug) ?: Regex("\\d+").find(ch.title)
                                                val chNum = chNumMatch?.value?.toIntOrNull()
                                                val chParam = if (chNum != null) "?ch=$chNum" else ""
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "${novel.title} - ${ch.title}")
                                                    putExtra(Intent.EXTRA_TEXT, "Read ${novel.title} ${ch.title} on Zenkai:\nhttps://zenkai.app/novel/$novelSlug$chParam")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Chapter"))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = p.textMuted, modifier = Modifier.size(15.dp))
                                        }

                                        // Download Status
                                        IconButton(
                                            onClick = {
                                                if (!isDownloaded && !isDownloading) {
                                                    vm.downloadNovelChapter(novelSlug, novel.title, effectiveCover ?: "", ch.slug, ch.title)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            when {
                                                isDownloading -> CircularProgressIndicator(color = p.accentA, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                isDownloaded -> Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = p.rating, modifier = Modifier.size(16.dp))
                                                else -> Icon(Icons.Default.Download, contentDescription = "Download", tint = p.textMuted, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Pagination: Load More Chapters Button
                            if (vm.novelChaptersPage < vm.novelChaptersMaxPage) {
                                item(key = "novel_load_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(p.chipBg)
                                            .clickable(enabled = !vm.novelChaptersLoading) {
                                                vm.loadMoreNovelChapters(novelSlug)
                                            }
                                            .padding(vertical = 11.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (vm.novelChaptersLoading) {
                                            CircularProgressIndicator(color = p.accentA, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                text = "Load More Chapters (Page ${vm.novelChaptersPage + 1} of ${vm.novelChaptersMaxPage})",
                                                color = p.text,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 5. Tab 1: Overview & Genres Content
                        if (novel.synopsis.isNotBlank()) {
                            item(key = "novel_synopsis_card") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(p.chipBg)
                                        .clickable { synopsisExpanded = !synopsisExpanded }
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SYNOPSIS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = p.accentText
                                    )
                                    Text(
                                        text = novel.synopsis,
                                        fontSize = 12.5.sp,
                                        color = p.textSub,
                                        lineHeight = 18.sp,
                                        maxLines = if (synopsisExpanded) Int.MAX_VALUE else 6,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (synopsisExpanded) "Show Less" else "Read More...",
                                        color = p.accentText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (novel.genres.isNotEmpty()) {
                            item(key = "novel_genres_card") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "GENRES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = p.textMuted
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        novel.genres.forEach { genre ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(p.chipBg)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = genre,
                                                    fontSize = 11.sp,
                                                    color = p.text,
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

                // Smooth Sticky Top Bar
                Box(
                    modifier = Modifier
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
                            .graphicsLayer { alpha = if (isScrolledPastHero) 1f else 0f }
                            .background(p.bg.copy(alpha = 0.96f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(52.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(p.chipBg)
                                .clickable { handleBack() },
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
                                text = novel.title,
                                color = p.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(p.chipBg)
                                .clickable { vm.toggleNovelBookmark(novelSlug) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) p.accentText else p.text,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelRelatedMediaTile(
    item: MediaRelation,
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

            // Relation Type Badge
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

            // Media Type Badge
            val mediaType = when {
                item.type.equals("ANIME", ignoreCase = true) -> "ANIME"
                item.type.equals("MANGA", ignoreCase = true) -> "MANGA"
                else -> "NOVEL"
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

// ── Detail Blurred Backdrop Art ──────────────────────────────────────────────

@Composable
private fun NovelDetailBackdropArt(url: String, p: HomePalette, modifier: Modifier) {
    val ctx = LocalContext.current
    val req = remember(url) { coil3.request.ImageRequest.Builder(ctx).data(url).size(96, 144).build() }

    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0.40f to Color.Black, 0.95f to Color.Transparent),
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
