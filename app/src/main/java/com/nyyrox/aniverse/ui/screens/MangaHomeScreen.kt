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
    onMangaClick: (String) -> Unit,
    onReadClick: (mangaId: String, chapterId: String) -> Unit,
    onDownloadsClick: (() -> Unit)? = null
) {
    val p = rememberHomePalette()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.loadMangaHome()
        vm.refreshStorageData()
    }

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(Modifier.fillMaxSize()) {
            if (searchQuery.isNotBlank()) {
                // Search Overlay / Results Mode
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp)
                ) {
                    MangaSearchInput(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            vm.searchManga(it)
                        },
                        p = p
                    )
                    Spacer(Modifier.height(14.dp))
                    MangaSearchResultsView(
                        isLoading = vm.mangaSearchLoading,
                        results = vm.mangaSearchResults,
                        p = p,
                        onMangaClick = onMangaClick
                    )
                }
            } else {
                // Normal Feed with 3D Hero Banner + AniList Rails
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Floating Search Header
                    item("search_header") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 16.dp)
                        ) {
                            MangaSearchInput(
                                query = searchQuery,
                                onQueryChange = {
                                    searchQuery = it
                                    vm.searchManga(it)
                                },
                                p = p
                            )
                        }
                    }

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
}

// ── Search Input ─────────────────────────────────────────────────────────────

@Composable
private fun MangaSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    p: HomePalette
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .homeGlass(p, radius = 999.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = p.textMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = p.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(p.accentA),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            "Search manga, manhwa & comics...",
                            color = p.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    innerTextField()
                }
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = p.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

// ── Search Results Grid ──────────────────────────────────────────────────────

@Composable
private fun MangaSearchResultsView(
    isLoading: Boolean,
    results: List<MangaCard>,
    p: HomePalette,
    onMangaClick: (String) -> Unit
) {
    if (isLoading && results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = p.accentA,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(34.dp)
            )
        }
    } else if (results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = p.textMuted,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No manga found",
                    color = p.textSub,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(results, key = { idx, item -> "srch_${item.id}_$idx" }) { _, card ->
                PosterTile(
                    title = card.title,
                    image = card.poster,
                    meta = card.rating?.let { "★ $it" } ?: (card.format ?: card.type),
                    p = p,
                    onClick = { onMangaClick(card.id) }
                )
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
    onMangaClick: (String) -> Unit,
    onBookmark: (MangaCard) -> Unit
) {
    val reduced = LocalThemeSettings.current.isReducedMotionEnabled
    val n = shows.size
    val scope = rememberCoroutineScope()
    var position by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    val scrollingNow by rememberUpdatedState(listScrolling)
    val center by remember { derivedStateOf { floor(position + 0.5f).toInt() } }
    val current = Math.floorMod(center, n)
    val travel = with(LocalDensity.current) { (HeroCardW * 0.70f).toPx() }

    fun animateTo(target: Float, velocity: Float = 0f) {
        animJob?.cancel()
        animJob = scope.launch {
            animate(
                initialValue = position, targetValue = target, initialVelocity = velocity,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 170f)
            ) { v, _ -> position = v }
        }
    }

    fun settle(fingerVelocityPx: Float) {
        val v = -fingerVelocityPx / travel
        val nearest = floor(position + 0.5f)
        val target = floor(position + v * 0.15f + 0.5f).coerceIn(nearest - 1f, nearest + 1f)
        animateTo(target, v)
    }

    LaunchedEffect(n, dragging, reduced) {
        if (n < 2 || dragging || reduced) return@LaunchedEffect
        while (true) {
            delay(6000)
            if (!scrollingNow) animateTo(floor(position + 0.5f) + 1f)
        }
    }

    val breath = rememberInfiniteTransition(label = "breath")
    val phase by breath.animateFloat(
        initialValue = 0f, targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(5600, easing = LinearEasing)),
        label = "phase"
    )

    Box(Modifier.fillMaxWidth()) {
        HeroPosterBackdrop(shows[current].poster, p, Modifier.matchParentSize())

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))

            // 3D Carousel Cards
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HeroCardH + 32.dp)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        enabled = n > 1,
                        state = rememberDraggableState { d -> position -= d / travel },
                        onDragStarted = { dragging = true; animJob?.cancel() },
                        onDragStopped = { v -> dragging = false; settle(v) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val order = if (n < 2) listOf(center)
                else (center - 2..center + 2).sortedByDescending { abs(it - center) }

                for (i in order) {
                    val manga = shows[Math.floorMod(i, n)]
                    key(i) {
                        Box(
                            Modifier
                                .width(HeroCardW)
                                .height(HeroCardH)
                                .graphicsLayer {
                                    val x = i - position
                                    val ax = abs(x)
                                    val a = ax.coerceAtMost(1f)
                                    val side = if (x < 0f) -1f else 1f
                                    val bw = if (reduced) 0f else sin(phase + i * 1.3f)
                                    translationX = side * 0.26f * a * size.width
                                    translationY = (a * 6f + bw * (1.5f + 2.5f * a)) * density
                                    rotationY = -side * 30f * a
                                    rotationZ = side * a * 4f + bw * 0.7f * (0.3f + a)
                                    val sc = 1f - 0.08f * a + bw * 0.006f
                                    scaleX = sc
                                    scaleY = sc
                                    alpha = when {
                                        ax <= 1f -> 1f
                                        ax >= 2f -> 0f
                                        else -> 2f - ax
                                    }
                                    cameraDistance = 18f * density
                                }
                                .shadow(12.dp, HeroCardShape, clip = false)
                                .clip(HeroCardShape)
                                .shimmer(p)
                                .clickable {
                                    if (i == center) onMangaClick(manga.id) else animateTo(i.toFloat())
                                }
                        ) {
                            AsyncImage(
                                model = manga.poster,
                                contentDescription = manga.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Dim background cards for depth
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = 0.32f * abs(i - position).coerceAtMost(1f) }
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Metadata & Action Buttons for Current Card
            Crossfade(targetState = current, label = "heroMangaInfo") { idx ->
                val manga = shows[Math.floorMod(idx, n)]
                val genres = manga.genres.take(2).joinToString(" • ")

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = manga.title,
                        color = p.text,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val fmt = manga.format ?: manga.type ?: "MANGA"
                        Text(fmt.uppercase(), color = p.textSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (manga.rating != null) {
                            Text("  ·  ", color = p.textMuted, fontSize = 12.sp)
                            Icon(Icons.Default.Star, contentDescription = null, tint = p.rating, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(manga.rating, color = p.rating, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (genres.isNotBlank()) {
                            Text("  ·  ", color = p.textMuted, fontSize = 12.sp)
                            Text(genres, color = p.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Clean Glass CTAs: "Read Now" & "Bookmark"
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(p.accentBrush)
                                .clickable { onMangaClick(manga.id) }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Read Now", color = p.onAccent, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            Modifier
                                .homeGlass(p, radius = 999.dp)
                                .clickable { onBookmark(manga) }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = p.text, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bookmark", color = p.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun HeroPosterBackdrop(poster: String?, p: HomePalette, modifier: Modifier) {
    val ctx = LocalContext.current
    Crossfade(
        targetState = poster,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0.40f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            },
        label = "heroBackdrop"
    ) { url ->
        val req = remember(url) { ImageRequest.Builder(ctx).data(url).size(96, 144).build() }
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = req,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.25f
                        alpha = if (p.dark) 0.55f else 0.45f
                    }
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(p.gradTop.copy(alpha = 0.40f), Color.Transparent))
                    )
            )
        }
    }
}

// ── Continue Reading Rail ────────────────────────────────────────────────────

@Composable
private fun MangaContinueRow(
    history: List<MangaHistoryItem>,
    p: HomePalette,
    onMangaClick: (String) -> Unit,
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
    onMangaClick: (String) -> Unit
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
                    .clickable { onMangaClick(manga.id) }
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
    onMangaClick: (String) -> Unit
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
                onClick = { onMangaClick(manga.id) }
            )
        }
    }
}

// ── Bookmark Rail ────────────────────────────────────────────────────────────

@Composable
private fun MangaBookmarkRail(
    bookmarks: List<MangaBookmarkItem>,
    p: HomePalette,
    onMangaClick: (String) -> Unit
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
                onClick = { onMangaClick(bm.mangaId) }
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
                    .homeGlass(p, radius = 999.dp)
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
