package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.data.manga.MangaCard
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.viewmodel.MainViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.FastOutSlowInEasing
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
fun MangaHomeScreen(
    vm: MainViewModel,
    onMangaClick: (String, String?, String?) -> Unit,
    onReadClick: (mangaId: String, chapterId: String) -> Unit,
    onDownloadsClick: (() -> Unit)? = null
) {
    val p = rememberHomePalette()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.loadMangaHome()
        vm.refreshStorageData()
    }

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Loading & Error states
                    if (vm.mangaHomeLoading && vm.mangaHome.trending.isEmpty()) {
                        item("skeleton") {
                            MangaFeedSkeleton(p)
                        }
                    } else if (vm.mangaHomeError != null && vm.mangaHome.trending.isEmpty()) {
                        item("error") {
                            MangaFeedError(
                                error = vm.mangaHomeError ?: "Failed to load manga",
                                p = p,
                                onRetry = { vm.loadMangaHome(forceRefresh = true) }
                            )
                        }
                    } else {
                        // 1. Hero 3D Coverflow Stack
                        if (vm.mangaHome.trending.isNotEmpty()) {
                            item("hero") {
                                MangaHero(
                                    shows = vm.mangaHome.trending,
                                    p = p,
                                    listScrolling = listState.isScrollInProgress,
                                    onMangaClick = onMangaClick,
                                    onBookmark = { manga ->
                                        vm.toggleMangaBookmark(
                                            mangaId = manga.id,
                                            title = manga.title,
                                            poster = manga.poster,
                                            type = manga.type ?: "Manga"
                                        )
                                    }
                                )
                            }
                        }

                        // 2. Continue Reading (if user has read manga)
                        if (vm.mangaHistory.isNotEmpty()) {
                            item("continue_reading") {
                                MangaContinueRow(
                                    history = vm.mangaHistory,
                                    p = p,
                                    onMangaClick = onMangaClick,
                                    onReadClick = onReadClick
                                )
                            }
                        }

                        // 3. Top 10 Trending Manga (Big number typography + poster)
                        val trendingList = vm.mangaHome.trending
                        if (trendingList.isNotEmpty()) {
                            item("trending_top10") {
                                SectionHeader("Trending Manga", p, subtitle = "Most popular in the community")
                                Top10MangaRow(
                                    list = trendingList,
                                    p = p,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // 4. All-Time Popular Manga
                        val popularList = vm.mangaHome.popular
                        if (popularList.isNotEmpty()) {
                            item("popular_rail") {
                                SectionHeader("Popular Manga", p, subtitle = "All-time community favorites")
                                MangaRail(
                                    list = popularList,
                                    p = p,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // 5. Top Rated Manga
                        val topRatedList = vm.mangaHome.topRated
                        if (topRatedList.isNotEmpty()) {
                            item("top_rated_rail") {
                                SectionHeader("Top Rated Manga", p, subtitle = "Critically acclaimed stories")
                                MangaRail(
                                    list = topRatedList,
                                    p = p,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // 6. Trending Manhwa & Webtoons
                        val manhwaList = vm.mangaHome.manhwa
                        if (manhwaList.isNotEmpty()) {
                            item("manhwa_rail") {
                                SectionHeader("Trending Manhwa", p, subtitle = "Top Korean webtoons & action")
                                MangaRail(
                                    list = manhwaList,
                                    p = p,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // 7. Bookmarks Rail (if any exist)
                        if (vm.mangaBookmarks.isNotEmpty()) {
                            item("bookmarks_rail") {
                                SectionHeader("Your Bookmarks", p)
                                MangaBookmarkRail(
                                    bookmarks = vm.mangaBookmarks,
                                    p = p,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // Bottom Spacer for Navigation Bar
                        item("bottom_pad") {
                            Spacer(Modifier.height(110.dp))
                        }
                    }
                }
            }
        }
    }

// ── Hero Banner: 3D Coverflow Stack ──────────────────────────────────────────

@Composable
private fun MangaHero(
    shows: List<MangaCard>,
    p: HomePalette,
    listScrolling: Boolean,
    onMangaClick: (String, String?, String?) -> Unit,
    onBookmark: (MangaCard) -> Unit
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
            val manga = shows[page]
            val backdrop = manga.banner?.takeIf { it.isNotBlank() } ?: manga.poster

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = manga.title,
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
        val currentManga = shows[pagerState.currentPage % count]

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentManga.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Metadata row: MANGA | ★ 8.8 | Ch. 120 | Action, Adventure
            val fmt = currentManga.format ?: currentManga.type ?: "MANGA"
            val rating = currentManga.rating ?: currentManga.score?.let { String.format(Locale.US, "%.1f", it) }
            val chapter = currentManga.chapter?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("Ch", true)) it else "Ch. $it" }
            val genres = currentManga.genres.take(2).joinToString(", ")
            val metaList = listOfNotNull(
                fmt.uppercase(),
                rating?.let { "\u2605 $it" },
                chapter,
                genres.takeIf { it.isNotBlank() }
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

            currentManga.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = desc.replace(Regex("<[^>]*>"), "").trim(),
                    fontSize = 12.5.sp,
                    color = Color(0xB3FFFFFF),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Action Buttons: White pill "Read now" + Circular buttons for Save and Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Read Now Button - Solid pure white pill
                Button(
                    onClick = { onMangaClick(currentManga.id, currentManga.title, currentManga.poster) },
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
                    onClick = { onBookmark(currentManga) },
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
                    onClick = { onMangaClick(currentManga.id, currentManga.title, currentManga.poster) },
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
private fun MangaContinueRow(
    history: List<MangaHistoryItem>,
    p: HomePalette,
    onMangaClick: (String, String?, String?) -> Unit,
    onReadClick: (mangaId: String, chapterId: String) -> Unit
) {
    val items = remember(history) { history.distinctBy { it.mangaId }.take(8) }
    SectionHeader("Continue Reading", p)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(items, key = { i, h -> "cont_${h.mangaId}_$i" }) { _, item ->
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { onReadClick(item.mangaId, item.chapterId) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmer(p)
                ) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Resume pill overlay at bottom of poster
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Ch. ${item.chapterNumber}",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = p.text,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Resume reading",
                    color = p.textMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Top 10 Manga Row (Giant typographic numbers + poster) ────────────────────

@Composable
private fun Top10MangaRow(
    list: List<MangaCard>,
    p: HomePalette,
    onMangaClick: (String, String?, String?) -> Unit
) {
    val top = remember(list) { list.take(10) }
    val st = rememberLazyListState()
    LazyRow(
        state = st,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(top, key = { i, a -> "top10_${a.id}_$i" }) { idx, manga ->
            Box(
                Modifier
                    .graphicsLayer {
                        val info = st.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == idx }
                        if (item != null) {
                            val vw = (info.viewportEndOffset - info.viewportStartOffset).toFloat().coerceAtLeast(1f)
                            val x = ((item.offset + item.size / 2f - (info.viewportStartOffset + vw / 2f)) / (vw / 2f)).coerceIn(-1.2f, 1.2f)
                            rotationY = -x * 22f
                            val s = 1f - 0.08f * abs(x)
                            scaleX = s
                            scaleY = s
                        }
                    }
                    .width(if (idx >= 9) 196.dp else 164.dp)
                    .height(184.dp)
                    .clickable { onMangaClick(manga.id, manga.title, manga.poster) }
            ) {
                // Giant Outline / Number
                Text(
                    text = "${idx + 1}",
                    color = p.text.copy(alpha = 0.28f),
                    fontSize = 104.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-6).sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 4.dp, y = 10.dp)
                )
                // Floating Poster Tile
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .width(118.dp)
                        .height(176.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmer(p)
                ) {
                    AsyncImage(
                        model = manga.poster,
                        contentDescription = manga.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// ── Standard Manga Rail ──────────────────────────────────────────────────────

@Composable
private fun MangaRail(
    list: List<MangaCard>,
    p: HomePalette,
    onMangaClick: (String, String?, String?) -> Unit
) {
    val st = rememberLazyListState()
    LazyRow(
        state = st,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(list, key = { i, m -> "rail_${m.id}_$i" }) { idx, manga ->
            PosterTile(
                title = manga.title,
                image = manga.poster,
                meta = manga.rating?.let { "★ $it" } ?: (manga.format ?: manga.type),
                p = p,
                onClick = { onMangaClick(manga.id, manga.title, manga.poster) }
            )
        }
    }
}

// ── Bookmark Rail ────────────────────────────────────────────────────────────

@Composable
private fun MangaBookmarkRail(
    bookmarks: List<MangaBookmarkItem>,
    p: HomePalette,
    onMangaClick: (String, String?, String?) -> Unit
) {
    val items = remember(bookmarks) { bookmarks.distinctBy { it.mangaId } }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(items, key = { i, b -> "bm_${b.mangaId}_$i" }) { _, bm ->
            PosterTile(
                title = bm.title,
                image = bm.poster,
                meta = bm.type ?: "Manga",
                p = p,
                onClick = { onMangaClick(bm.mangaId, bm.title, bm.poster) }
            )
        }
    }
}

// ── Shared PosterTile (No card style, no border boxes) ──────────────────────

@Composable
private fun PosterTile(
    title: String,
    image: String?,
    meta: String?,
    p: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmer(p)
        ) {
            AsyncImage(
                model = image,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = p.text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                color = if (meta.startsWith("★")) p.rating else p.textMuted,
                fontSize = 12.sp,
                fontWeight = if (meta.startsWith("★")) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    p: HomePalette,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            color = p.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = p.textMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ── Skeletons & Error ────────────────────────────────────────────────────────

@Composable
private fun MangaFeedSkeleton(p: HomePalette) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(HeroCardH)
                .clip(RoundedCornerShape(16.dp))
                .shimmer(p)
        )
        Spacer(Modifier.height(28.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(4) {
                Column(Modifier.width(140.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmer(p)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmer(p)
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaFeedError(
    error: String,
    p: HomePalette,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = p.textMuted,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = p.textSub,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(p.chipBg)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retry",
                    color = p.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
