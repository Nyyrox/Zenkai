package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import com.nyyrox.aniverse.ui.animation.mediaCardPhysics
import com.nyyrox.aniverse.ui.animation.staggeredEntrance
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nyyrox.aniverse.data.manga.MangaCard
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.viewmodel.MainViewModel

@Composable
fun MangaHomeScreen(
    vm: MainViewModel,
    onMangaClick: (String) -> Unit,
    onReadClick: (mangaId: String, chapterId: String) -> Unit,
    onDownloadsClick: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadMangaHome()
        vm.refreshStorageData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 58.dp)
        ) {
            // Header: Clean Search Bar right below navbar with source selector chips
            MangaHeaderSearch(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    vm.searchManga(it)
                },
                activeSource = vm.activeMangaSource,
                onSourceSelect = { src ->
                    vm.setMangaSource(src)
                },
                onDownloadsClick = onDownloadsClick
            )

            // Content Area
            if (searchQuery.isNotBlank()) {
                // Search Results Grid
                MangaSearchResults(
                    isLoading = vm.mangaSearchLoading,
                    results = vm.mangaSearchResults,
                    onCardClick = onMangaClick
                )
            } else {
                // Home Feed
                MangaHomeFeed(
                    vm = vm,
                    onMangaClick = onMangaClick,
                    onReadClick = onReadClick
                )
            }
        }
    }
}

@Composable
private fun MangaHeaderSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    activeSource: com.nyyrox.aniverse.data.manga.MangaSource,
    onSourceSelect: (com.nyyrox.aniverse.data.manga.MangaSource) -> Unit,
    onDownloadsClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index = 0)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search ${activeSource.displayName}...",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = OttPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OttSurfaceElevated,
                    unfocusedContainerColor = OttSurface,
                    focusedBorderColor = OttPrimary,
                    unfocusedBorderColor = Color(0x1EFFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (onDownloadsClick != null) {
                IconButton(
                    onClick = onDownloadsClick,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Downloads",
                        tint = OttAccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Manga Sources Chip Row (MangaDex, ComicK, Manhua, Asura Scans, WeebCentral)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(com.nyyrox.aniverse.data.manga.MangaSource.entries.toTypedArray()) { src ->
                val isSelected = src == activeSource
                val (activeBg, activeBorder, activeText) = when (src) {
                    com.nyyrox.aniverse.data.manga.MangaSource.MANGADEX -> Triple(Color(0xFFFF6740), Color(0xFFFF8A65), Color.Black)
                    com.nyyrox.aniverse.data.manga.MangaSource.COMICK -> Triple(Color(0xFF3B82F6), Color(0xFF60A5FA), Color.White)
                    com.nyyrox.aniverse.data.manga.MangaSource.MANHUA -> Triple(Color(0xFFEAB308), Color(0xFFFACC15), Color.Black)
                    com.nyyrox.aniverse.data.manga.MangaSource.ASURA -> Triple(Color(0xFF10B981), Color(0xFF34D399), Color.Black)
                    com.nyyrox.aniverse.data.manga.MangaSource.WEEBCENTRAL -> Triple(Color(0xFF8B5CF6), Color(0xFFA78BFA), Color.White)
                }
                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(if (isSelected) activeBg else Color(0x14FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) activeBorder else Color(0x22FFFFFF),
                            shape = OttPillShape
                        )
                        .clickable { onSourceSelect(src) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = src.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) activeText else Color.White
                        )
                        if (src.badge != null) {
                            Text(
                                text = "• ${src.badge}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isSelected) activeText.copy(alpha = 0.8f) else Color(0xAAFFFFFF)
                            )
                        }
                    }
                }
            }
        }

        }
    }
}

@Composable
private fun MangaSearchResults(
    isLoading: Boolean,
    results: List<MangaCard>,
    onCardClick: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            CircularProgressIndicator(color = OttPrimary, strokeWidth = 3.dp)
        }
    } else if (results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "No manga found matching your query",
                color = OttSecondary,
                fontSize = 14.sp
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.id }) { card ->
                MangaGridItem(card = card, onClick = { onCardClick(card.id) })
            }
        }
    }
}

@Composable
fun rememberMangaImageRequest(url: String?): Any? {
    val context = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else {
            val ref = when {
                url.contains("asura") -> "https://asuracomic.net/"
                url.contains("comick") || url.contains("meo") -> "https://comick.art/"
                url.contains("mangadex") -> "https://mangadex.org/"
                url.contains("weebcentral") || url.contains("compsci88") -> "https://weebcentral.com/"
                else -> null
            }
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .apply {
                    if (ref != null) {
                        val origin = when {
                            ref.contains("asura") -> "https://asuracomic.net"
                            ref.contains("comick") -> "https://comick.art"
                            ref.contains("mangadex") -> "https://mangadex.org"
                            else -> "https://weebcentral.com"
                        }
                        httpHeaders(
                            NetworkHeaders.Builder()
                                .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                .set("Referer", ref)
                                .set("Origin", origin)
                                .build()
                        )
                    }
                }
                .build()
        }
    }
}

@Composable
private fun MangaHomeFeed(
    vm: MainViewModel,
    onMangaClick: (String) -> Unit,
    onReadClick: (mangaId: String, chapterId: String) -> Unit
) {
    if (vm.mangaHomeLoading && vm.mangaHome.trending.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = OttPrimary, strokeWidth = 3.dp)
                Text(
                    text = "Connecting to Manga Servers...",
                    color = OttSecondary,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val isManhuaActive = vm.activeMangaSource == com.nyyrox.aniverse.data.manga.MangaSource.MANHUA
    val trendingTitle = when {
        isManhuaActive -> "Trending Manhua 🇨🇳"
        vm.activeMangaSource == com.nyyrox.aniverse.data.manga.MangaSource.ASURA -> "Trending Webtoons & Manhwa"
        else -> "Trending Manga"
    }
    val latestTitle = when {
        isManhuaActive -> "Latest Cultivation & Updates"
        else -> "Latest Updates"
    }
    val newReleaseTitle = when {
        isManhuaActive -> "New Manhua Releases"
        else -> "New Releases"
    }

    val trendingCards = vm.mangaHome.trending
    val latestCards = vm.mangaHome.latest
    val newReleaseCards = vm.mangaHome.newRelease

    LazyColumn(
        state = vm.mangaLazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Continue Reading Row
        if (vm.mangaHistory.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 1)) {
                    MangaContinueReadingSection(
                        history = vm.mangaHistory,
                        onCardClick = onMangaClick,
                        onResumeClick = onReadClick
                    )
                }
            }
        }

        // 2. Bookmarked Manga (if any)
        if (vm.mangaBookmarks.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 2)) {
                    MangaBookmarksSection(
                        bookmarks = vm.mangaBookmarks,
                        onCardClick = onMangaClick
                    )
                }
            }
        }

        // 3. Featured Hero Carousel (Top Trending)
        if (trendingCards.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 3)) {
                    MangaHeroBanner(
                        featured = trendingCards.first(),
                        onClick = { onMangaClick(trendingCards.first().id) }
                    )
                }
            }
        }

        // 4. Trending Rail
        if (trendingCards.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 4)) {
                    MangaHorizontalRail(
                        title = trendingTitle,
                        icon = Icons.Default.TrendingUp,
                        cards = trendingCards.drop(1),
                        onCardClick = onMangaClick
                    )
                }
            }
        }

        // 5. Latest Releases Rail
        if (latestCards.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 5)) {
                    MangaHorizontalRail(
                        title = latestTitle,
                        icon = Icons.Default.AutoStories,
                        cards = latestCards,
                        onCardClick = onMangaClick
                    )
                }
            }
        }

        // 6. New Releases Rail
        if (newReleaseCards.isNotEmpty()) {
            item {
                Box(modifier = Modifier.staggeredEntrance(index = 6)) {
                    MangaHorizontalRail(
                        title = newReleaseTitle,
                        icon = Icons.Default.TrendingUp,
                        cards = newReleaseCards,
                        onCardClick = onMangaClick
                    )
                }
            }
        }

        // 7. Empty State
        if (trendingCards.isEmpty() && latestCards.isEmpty() && newReleaseCards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No titles found in current source.",
                        color = OttSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaContinueReadingSection(
    history: List<MangaHistoryItem>,
    onCardClick: (String) -> Unit,
    onResumeClick: (mangaId: String, chapterId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = OttPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Continue Reading",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history.take(10), key = { it.mangaId }) { item ->
                MangaContinueCard(
                    item = item,
                    onClick = { onCardClick(item.mangaId) },
                    onResume = { onResumeClick(item.mangaId, item.chapterId) }
                )
            }
        }
    }
}

@Composable
private fun MangaContinueCard(
    item: MangaHistoryItem,
    onClick: () -> Unit,
    onResume: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(250.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OttSurfaceElevated)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(66.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OttSurface)
            ) {
                AsyncImage(
                    model = rememberMangaImageRequest(item.poster),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ch. ${item.chapterNumber}",
                            fontSize = 11.sp,
                            color = OttPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.pageCount > 0 || item.pageIndex > 0) {
                            Text(
                                text = "•  Pg ${item.pageIndex + 1}${if (item.pageCount > 0) "/${item.pageCount}" else ""}",
                                fontSize = 10.sp,
                                color = OttSecondary
                            )
                        }
                    }
                }

                // Progress Bar (if pageCount > 0)
                if (item.pageCount > 0) {
                    val progress = ((item.pageIndex + 1).toFloat() / item.pageCount.toFloat()).coerceIn(0.05f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = OttPrimary,
                        trackColor = Color(0x25FFFFFF)
                    )
                }

                // Resume Button
                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(Color.White)
                        .clickable { onResume() }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Resume",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaBookmarksSection(
    bookmarks: List<com.nyyrox.aniverse.data.manga.MangaBookmarkItem>,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color(0xFFFFD166),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Bookmarked Manga",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val uniqueBookmarks = bookmarks.distinctBy { it.mangaId }
            items(uniqueBookmarks, key = { it.mangaId }) { bm ->
                MangaCardView(
                    card = MangaCard(
                        id = bm.mangaId,
                        title = bm.title,
                        poster = bm.poster,
                        type = bm.type,
                        chapter = bm.latestChapter
                    ),
                    onClick = { onCardClick(bm.mangaId) }
                )
            }
        }
    }
}

@Composable
private fun MangaHeroBanner(
    featured: MangaCard,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OttSurfaceElevated)
            .clickable { onClick() }
    ) {
        // Backdrop Image
        AsyncImage(
            model = rememberMangaImageRequest(featured.poster),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x20000000),
                            Color(0xD00A0D14),
                            Color(0xF0050709)
                        )
                    )
                )
        )

        // Banner Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(OttPrimary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = featured.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Read Chapter 1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun MangaHorizontalRail(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cards: List<MangaCard>,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OttPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cards, key = { it.id }) { card ->
                MangaCardView(card = card, onClick = { onCardClick(card.id) })
            }
        }
    }
}

@Composable
private fun MangaCardView(
    card: MangaCard,
    onClick: () -> Unit
) {
    val coordinator = LocalSharedPosterCoordinator.current
    val itemKey = "manga_${card.id}"
    val isSharedActive = coordinator?.isItemActive(itemKey) == true
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = Modifier
            .width(115.dp)
            .mediaCardPhysics(pressScale = 0.94f, enableTilt = true)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val bounds = cardBounds
                        if (coordinator != null && bounds != null && bounds.width > 0) {
                            coordinator.startForwardTransition(
                                itemKey = itemKey,
                                posterUrl = card.poster,
                                bounds = bounds,
                                onNavigate = onClick
                            )
                        } else {
                            onClick()
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(165.dp)
                .onGloballyPositioned { coords ->
                    cardBounds = coords.boundsInWindow()
                }
                .clip(RoundedCornerShape(14.dp))
                .background(OttSurfaceElevated)
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = rememberMangaImageRequest(card.poster),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = if (isSharedActive) 0f else 1f
                    }
            )

            // Bottom gradient vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x20000000), Color(0x80000000)),
                            startY = 100f
                        )
                    )
            )

            // Top Type Pill (MANHWA, MANGA, MANHUA)
            if (!card.type.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(OttPillShape)
                        .background(Color(0xD0000000))
                        .border(0.5.dp, Color(0x35FFFFFF), OttPillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = card.type.uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (card.type.lowercase()) {
                            "manhwa" -> Color(0xFF6EE7B7)
                            "manhua" -> Color(0xFF93C5FD)
                            else -> Color(0xFFFCA5A5)
                        }
                    )
                }
            }

            // Bottom Chapter Badge
            if (!card.chapter.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(OttPillShape)
                        .background(Color(0xD0000000))
                        .border(0.5.dp, Color(0x35FFFFFF), OttPillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (card.chapter.startsWith("Ch", ignoreCase = true)) card.chapter else "Ch. ${card.chapter}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = card.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        Text(
            text = card.type ?: "Manga",
            fontSize = 10.sp,
            color = OttSecondary
        )
    }
}

@Composable
private fun MangaGridItem(
    card: MangaCard,
    onClick: () -> Unit
) {
    val coordinator = LocalSharedPosterCoordinator.current
    val itemKey = "manga_${card.id}"
    val isSharedActive = coordinator?.isItemActive(itemKey) == true
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val bounds = cardBounds
                if (coordinator != null && bounds != null && bounds.width > 0) {
                    coordinator.startForwardTransition(
                        itemKey = itemKey,
                        posterUrl = card.poster,
                        bounds = bounds,
                        onNavigate = onClick
                    )
                } else {
                    onClick()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .onGloballyPositioned { coords ->
                    cardBounds = coords.boundsInWindow()
                }
                .clip(RoundedCornerShape(14.dp))
                .background(OttSurfaceElevated)
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = rememberMangaImageRequest(card.poster),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = if (isSharedActive) 0f else 1f
                    }
            )

            // Bottom gradient vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x20000000), Color(0x80000000)),
                            startY = 90f
                        )
                    )
            )

            if (!card.type.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(OttPillShape)
                        .background(Color(0xD0000000))
                        .border(0.5.dp, Color(0x35FFFFFF), OttPillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = card.type.uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (card.type.lowercase()) {
                            "manhwa" -> Color(0xFF6EE7B7)
                            "manhua" -> Color(0xFF93C5FD)
                            else -> Color(0xFFFCA5A5)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = card.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
