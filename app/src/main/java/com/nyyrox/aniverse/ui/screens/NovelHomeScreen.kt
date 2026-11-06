package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.data.novel.NovelItem
import com.nyyrox.aniverse.data.novel.NovelSource
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.viewmodel.MainViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import java.util.Locale

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

private val HeroCardW = 200.dp
private val HeroCardH = 292.dp
private val HeroCardShape = RoundedCornerShape(14.dp)
private const val TWO_PI = (2.0 * Math.PI).toFloat()

@Composable
fun rememberNovelImageRequest(url: String?): Any? {
    val context = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else {
            val ref = when {
                url.contains("royalroad") || url.contains("royalroadcdn") -> "https://www.royalroad.com/"
                url.contains("wuxiaworld") -> "https://wuxiaworld.site/"
                url.contains("novelfire") -> "https://novelfire.net/"
                url.contains("novelfull") -> "https://novelfull.com/"
                url.contains("freewebnovel") -> "https://freewebnovel.com/"
                url.contains("lnori") -> "https://lnori.com/"
                else -> null
            }
            val builder = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)

            val headers = NetworkHeaders.Builder()
                .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            if (ref != null) {
                headers.set("Referer", ref)
            }
            builder.httpHeaders(headers.build()).build()
        }
    }
}

@Composable
fun NovelHomeScreen(
    vm: MainViewModel,
    onNovelClick: (String, String?, String?) -> Unit,
    onReadClick: ((novelSlug: String, chapterSlug: String) -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.loadNovelHome()
        vm.refreshStorageData()
    }

    val heroNovels = remember(vm.novelRankings, vm.novelPopular, vm.novelLightNovels, vm.novelLatest) {
        vm.novelRankings.ifEmpty { vm.novelPopular }
            .ifEmpty { vm.novelLightNovels }
            .ifEmpty { vm.novelLatest }
    }

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Loading / Error states
                if (vm.novelHomeLoading && vm.novelRankings.isEmpty() && vm.novelPopular.isEmpty()) {
                    item("skeleton") {
                        NovelFeedSkeleton(p)
                    }
                } else if (vm.novelHomeError != null && vm.novelRankings.isEmpty()) {
                    item("error") {
                        NovelFeedError(
                            error = vm.novelHomeError ?: "Unable to fetch novels",
                            p = p,
                            onRetry = { vm.loadNovelHome() }
                        )
                    }
                } else {
                    // 1. 3D Coverflow Hero Banner (covers full top area behind status bars)
                    if (heroNovels.isNotEmpty()) {
                        item("hero") {
                            NovelHero(
                                shows = heroNovels,
                                p = p,
                                listScrolling = listState.isScrollInProgress,
                                onNovelClick = onNovelClick,
                                onBookmark = { novel ->
                                    vm.toggleNovelBookmark(novel.slug)
                                }
                            )
                        }
                    } else {
                        item("top_clearance") {
                            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(64.dp))
                        }
                    }

                    // 2. Liquid Glass Source Switcher (No card containers, no borders)
                    item("source_switcher") {
                        NovelSourceSwitcher(
                            activeSource = vm.activeNovelSource,
                            onSourceSelect = { src ->
                                vm.setNovelSource(src)
                            },
                            p = p
                        )
                    }

                    // 3. Continue Reading (if user has active novel history)
                    if (vm.novelHistoryList.isNotEmpty()) {
                        item("continue_reading") {
                            NovelContinueRow(
                                history = vm.novelHistoryList,
                                p = p,
                                onNovelClick = onNovelClick,
                                onReadClick = { nSlug, chSlug ->
                                    val item = vm.novelHistoryList.find { it.slug == nSlug }
                                    onReadClick?.invoke(nSlug, chSlug) ?: onNovelClick(nSlug, item?.title, item?.cover)
                                },
                                onRemove = { vm.removeNovelHistory(it) }
                            )
                        }
                    }

                    // 4. Top Web Novels (Rankings 1..10 with numerals)
                    if (vm.novelRankings.isNotEmpty()) {
                        item("rankings") {
                            NovelRankingSection(
                                title = "Top Web Novels",
                                subtitle = "Highest ranked across ${vm.activeNovelSource.displayName}",
                                novels = vm.novelRankings,
                                p = p,
                                onNovelClick = onNovelClick
                            )
                        }
                    }

                    // 5. Light Novels Section
                    if (vm.novelLightNovels.isNotEmpty()) {
                        item("light_novels") {
                            NovelRailSection(
                                title = "Light Novels",
                                subtitle = "Japanese & Korean translated series",
                                novels = vm.novelLightNovels,
                                p = p,
                                onNovelClick = onNovelClick
                            )
                        }
                    }

                    // 6. Popular Novels
                    if (vm.novelPopular.isNotEmpty()) {
                        item("popular") {
                            NovelRailSection(
                                title = "Popular Reads",
                                subtitle = "Most read by the community",
                                novels = vm.novelPopular,
                                p = p,
                                onNovelClick = onNovelClick
                            )
                        }
                    }

                    // 7. Latest Releases
                    if (vm.novelLatest.isNotEmpty()) {
                        item("latest") {
                            NovelRailSection(
                                title = "Latest Updates",
                                subtitle = "Freshly updated chapters",
                                novels = vm.novelLatest,
                                p = p,
                                onNovelClick = onNovelClick
                            )
                        }
                    }

                    // Bottom padding to clear dock
                    item("bottom_pad") {
                        Spacer(Modifier.height(110.dp))
                    }
                }
            }
        }
    }
}

// ── Liquid Glass Source Switcher ───────────────────────────────────────────────

@Composable
private fun NovelSourceSwitcher(
    activeSource: NovelSource,
    onSourceSelect: (NovelSource) -> Unit,
    p: HomePalette
) {
    val displaySources = remember {
        NovelSource.entries.filter { it != NovelSource.WEB_NOVEL }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(p.chipBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(displaySources) { src ->
                    val isSelected = src == activeSource
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (isSelected) Modifier.background(p.accentBrush)
                                else Modifier
                            )
                            .clickable { onSourceSelect(src) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = src.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) p.onAccent else p.textSub
                            )
                            if (src.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) p.onAccent.copy(alpha = 0.20f) else p.chipBg)
                                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = src.badge,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) p.onAccent else p.textMuted
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

// ── 3D Coverflow Hero Banner ──────────────────────────────────────────────────

@Composable
private fun NovelHero(
    shows: List<NovelItem>,
    p: HomePalette,
    listScrolling: Boolean,
    onNovelClick: (String, String?, String?) -> Unit,
    onBookmark: (NovelItem) -> Unit
) {
    if (shows.isEmpty()) return
    val count = shows.size
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState, listScrolling) {
        while (true) {
            delay(7000)
            if (!listScrolling && !pagerState.isScrollInProgress && count > 1) {
                val next = (pagerState.currentPage + 1) % count
                pagerState.animateScrollToPage(next, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val novel = shows[page]
            val coverRequest = rememberNovelImageRequest(novel.cover)

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = novel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top scrim for status bar clearance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xD0000000), Color.Transparent)
                            )
                        )
                )

                // Subtle ambient dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x2E000000))
                )

                // Multi-stop cinematic bottom dark gradient: smoothly dissolves hero into page background with zero line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x35000000),
                                    Color(0x80000000),
                                    Color(0xDE000000),
                                    Color(0xFF000000),
                                    Color(0xFF000000)
                                )
                            )
                        )
                )
            }
        }

        // Foreground details & actions aligned to bottom
        val currentNovel = shows[pagerState.currentPage % count]

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentNovel.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Metadata row: LIGHT NOVEL | ★ 8.8 | 320 Chapters | NOVELFIRE
            val rating = currentNovel.rating?.takeIf { it.isNotBlank() }
            val stats = currentNovel.stats?.takeIf { it.isNotBlank() }
            val metaList = listOfNotNull(
                "LIGHT NOVEL",
                rating?.let { "\u2605 $it" },
                stats,
                currentNovel.source.uppercase()
            )

            Text(
                text = metaList.joinToString("  |  "),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xD9FFFFFF),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(18.dp))

            // Action Buttons: White pill "Read now" + Circular buttons for Save and Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Read Now Button - Solid pure white pill
                Button(
                    onClick = { onNovelClick(currentNovel.slug, currentNovel.title, currentNovel.cover) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Read now",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Read now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Bookmark Icon Button
                Button(
                    onClick = { onBookmark(currentNovel) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FFFFFF),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Info / Details Button
                Button(
                    onClick = { onNovelClick(currentNovel.slug, currentNovel.title, currentNovel.cover) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33FFFFFF),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Pager Indicators
            if (count > 1) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeIndex = pagerState.currentPage % count
                    repeat(count.coerceAtMost(10)) { i ->
                        if (i == activeIndex) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66FFFFFF))
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(i)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelContinueRow(
    history: List<NovelHistoryItem>,
    p: HomePalette,
    onNovelClick: (String, String?, String?) -> Unit,
    onReadClick: (novelSlug: String, chapterSlug: String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = p.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Continue Reading",
                color = p.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ZenkaiFontFamily
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(history, key = { "hist_${it.slug}" }) { item ->
                Row(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.chipBg)
                        .clickable { onReadClick(item.slug, item.lastChapterSlug) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(width = 54.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .shimmer(p)
                    ) {
                        AsyncImage(
                            model = rememberNovelImageRequest(item.cover),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = p.text,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.lastChapterTitle,
                            color = p.accentText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(p.accentBrush)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Resume", color = p.onAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Top 10 Rankings Section ───────────────────────────────────────────────────

@Composable
private fun NovelRankingSection(
    title: String,
    subtitle: String,
    novels: List<NovelItem>,
    p: HomePalette,
    onNovelClick: (String, String?, String?) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        SectionTitle(title = title, subtitle = subtitle, p = p)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(novels.take(10), key = { idx, item -> "top_${item.slug}_$idx" }) { idx, novel ->
                NovelRankTile(
                    rank = idx + 1,
                    novel = novel,
                    p = p,
                    onClick = { onNovelClick(novel.slug, novel.title, novel.cover) }
                )
            }
        }
    }
}

@Composable
private fun NovelRankTile(
    rank: Int,
    novel: NovelItem,
    p: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Large Stylized Rank Number
        Text(
            text = "$rank",
            fontSize = 58.sp,
            fontWeight = FontWeight.Black,
            fontFamily = ZenkaiFontFamily,
            color = p.textMuted.copy(alpha = 0.55f),
            modifier = Modifier.padding(end = 4.dp)
        )

        NovelPosterTile(
            title = novel.title,
            image = novel.cover,
            rating = novel.rating,
            subtitle = novel.stats ?: novel.source.uppercase(),
            p = p,
            onClick = onClick
        )
    }
}

// ── Horizontal Rail Section ───────────────────────────────────────────────────

@Composable
private fun NovelRailSection(
    title: String,
    subtitle: String,
    novels: List<NovelItem>,
    p: HomePalette,
    onNovelClick: (String, String?, String?) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        SectionTitle(title = title, subtitle = subtitle, p = p)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(novels, key = { it.slug }) { novel ->
                NovelPosterTile(
                    title = novel.title,
                    image = novel.cover,
                    rating = novel.rating,
                    subtitle = novel.stats ?: novel.source.uppercase(),
                    p = p,
                    onClick = { onNovelClick(novel.slug, novel.title, novel.cover) }
                )
            }
        }
    }
}

// ── Novel Poster Tile Component ───────────────────────────────────────────────

@Composable
private fun NovelPosterTile(
    title: String,
    image: String,
    rating: String? = null,
    subtitle: String? = null,
    p: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(175.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmer(p)
        ) {
            AsyncImage(
                model = rememberNovelImageRequest(image),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Rating pill (top-end, only for actual star ratings e.g. "★ 4.8")
            if (!rating.isNullOrBlank()) {
                val cleanRating = if (rating.startsWith("★")) rating else "★ $rating"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cleanRating,
                        color = p.rating,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            color = p.text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = p.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Section Title ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, subtitle: String, p: HomePalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = title,
            color = p.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ZenkaiFontFamily
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = p.textSub,
            fontSize = 12.sp
        )
    }
}

// ── Ambient Blurred Backdrop ──────────────────────────────────────────────────

@Composable
private fun HeroNovelPosterBackdrop(poster: String?, p: HomePalette, modifier: Modifier) {
    Crossfade(
        targetState = poster,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.55f to Color.Black.copy(alpha = 0.85f),
                        0.82f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        label = "heroNovelBg"
    ) { url ->
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = rememberNovelImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.25f
                        alpha = if (p.dark) 0.50f else 0.38f
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
}

// ── Skeletons & Errors ────────────────────────────────────────────────────────

@Composable
private fun NovelFeedSkeleton(p: HomePalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(HeroCardW)
                .height(HeroCardH)
                .clip(HeroCardShape)
                .shimmer(p)
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .width(220.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmer(p)
        )
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(175.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmer(p)
                )
            }
        }
    }
}

@Composable
private fun NovelFeedError(error: String, p: HomePalette, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 32.dp, vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connection Issue", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(error, color = p.textSub, fontSize = 13.5.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(p.chipBg)
                .clickable { onRetry() }
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = p.text, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Retry", color = p.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
