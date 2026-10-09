package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.nyyrox.aniverse.ui.animation.mediaCardPhysics
import com.nyyrox.aniverse.ui.animation.staggeredEntrance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardBottomGradient
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.viewmodel.MainViewModel

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
                headers.set("Origin", ref.removeSuffix("/"))
            }
            builder.httpHeaders(headers.build()).build()
        }
    }
}

@Composable
fun NovelHomeScreen(
    vm: MainViewModel,
    onNovelClick: (String) -> Unit,
    onReadClick: ((novelSlug: String, chapterSlug: String) -> Unit)? = null,
    onDownloadsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        vm.loadNovelHome()
        vm.refreshStorageData()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 58.dp)
        ) {
            // Novel Header & Search Bar
            NovelSearchHeader(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    vm.searchNovels(it)
                },
                onClear = {
                    searchQuery = ""
                    vm.searchNovels("")
                    focusManager.clearFocus()
                },
                activeSource = vm.activeNovelSource,
                onSourceSelect = { src ->
                    vm.setNovelSource(src)
                    if (searchQuery.isNotBlank()) {
                        vm.searchNovels(searchQuery)
                    }
                },
                onDownloadsClick = onDownloadsClick
            )

            if (searchQuery.isNotBlank()) {
                // Search Results Grid
                if (vm.novelSearchLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(36.dp))
                    }
                } else if (vm.novelSearchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No web novels found for \"$searchQuery\"",
                            color = OttTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(vm.novelSearchResults, key = { it.slug }) { item ->
                            NovelGridCard(novel = item, onClick = { onNovelClick(item.slug) })
                        }
                    }
                }
            } else {
                // Main Novel Content (Rankings, Popular, Latest, Genres)
                if (vm.novelHomeLoading && vm.novelRankings.isEmpty() && vm.novelPopular.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(36.dp))
                    }
                } else if (vm.novelHomeError != null && vm.novelRankings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Connection Issue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OttTextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(vm.novelHomeError ?: "Unable to fetch novels from ${vm.activeNovelSource.displayName}", fontSize = 12.sp, color = OttTextSecondary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { vm.loadNovelHome() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = OttPillShape
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = vm.novelLazyListState,
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 0. Continue Reading (if user has active novel history)
                        if (vm.novelHistoryList.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.staggeredEntrance(index = 0)) {
                                    NovelContinueReadingRow(
                                        history = vm.novelHistoryList,
                                        onNovelClick = onNovelClick,
                                        onReadClick = { nSlug, chSlug ->
                                            onReadClick?.invoke(nSlug, chSlug) ?: onNovelClick(nSlug)
                                        },
                                        onRemove = { vm.removeNovelHistory(it) }
                                    )
                                }
                            }
                        }

                        // 1. Featured Top Novel Banner (Top 1 in Rankings)
                        if (vm.novelRankings.isNotEmpty()) {
                            val topNovel = vm.novelRankings.first()
                            item {
                                Box(modifier = Modifier.staggeredEntrance(index = 1)) {
                                    NovelHeroCard(novel = topNovel, onClick = { onNovelClick(topNovel.slug) })
                                }
                            }
                        }

                        // 2. Rankings Carousel / Row
                        if (vm.novelRankings.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.staggeredEntrance(index = 2)) {
                                    NovelSectionHeader(
                                        title = "Top Web Novels",
                                        subtitle = "Highest ranked across ${vm.activeNovelSource.displayName}"
                                    )
                                    NovelRankingsRow(novels = vm.novelRankings, onNovelClick = onNovelClick)
                                }
                            }
                        }

                        // 2.5 Light Novels Section
                        if (vm.novelLightNovels.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.staggeredEntrance(index = 3)) {
                                    NovelSectionHeader(
                                        title = "Light Novels",
                                        subtitle = "Japanese & Korean translated light novels"
                                    )
                                    NovelHorizontalRow(novels = vm.novelLightNovels, onNovelClick = onNovelClick)
                                }
                            }
                        }

                        // 3. Genre Filter Chips
                        item {
                            Box(modifier = Modifier.staggeredEntrance(index = 4)) {
                                NovelGenreChips(
                                    selectedGenre = vm.selectedNovelGenre ?: "All",
                                    onSelectGenre = { vm.filterNovelsByGenre(it) }
                                )
                            }
                        }

                        // 4. Popular Novels
                        if (vm.novelPopular.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.staggeredEntrance(index = 5)) {
                                    NovelSectionHeader(
                                        title = if (vm.selectedNovelGenre == "All") "Popular Reads" else "${vm.selectedNovelGenre} Novels",
                                        subtitle = "Most read by community"
                                    )
                                    NovelHorizontalRow(novels = vm.novelPopular, onNovelClick = onNovelClick)
                                }
                            }
                        }

                        // 5. Latest Releases
                        if (vm.novelLatest.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.staggeredEntrance(index = 6)) {
                                    NovelSectionHeader(
                                        title = "Latest Updates",
                                        subtitle = "Freshly released chapters"
                                    )
                                    NovelHorizontalRow(novels = vm.novelLatest, onNovelClick = onNovelClick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    activeSource: NovelSource,
    onSourceSelect: (NovelSource) -> Unit,
    onDownloadsClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                placeholder = { Text("Search ${activeSource.displayName}...", fontSize = 13.sp, color = OttTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = OttTextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = OttPillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x22FFFFFF),
                    unfocusedContainerColor = Color(0x14FFFFFF),
                    focusedBorderColor = Color(0x55FFFFFF),
                    unfocusedBorderColor = Color(0x20FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f).height(50.dp)
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

        Spacer(Modifier.height(10.dp))

        // Redesigned Novel Sources Floating Glass Segmented Dock Bar
        val displaySources = remember {
            NovelSource.entries.filter { it != NovelSource.WEB_NOVEL }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x18FFFFFF))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(displaySources) { src ->
                    val isSelected = src == activeSource
                    val sourceGradient = when (src) {
                        NovelSource.NOVEL_FULL -> Brush.horizontalGradient(listOf(Color(0xFFFF7A45), Color(0xFFFF5722)))
                        NovelSource.NOVEL_FIRE -> Brush.horizontalGradient(listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
                        NovelSource.ROYAL_ROAD -> Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                        NovelSource.WUXIA_WORLD -> Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF047857)))
                        else -> Brush.horizontalGradient(listOf(OttPrimary, OttPrimary))
                    }
                    val iconColor = when (src) {
                        NovelSource.NOVEL_FULL -> Color(0xFFFF7A45)
                        NovelSource.NOVEL_FIRE -> Color(0xFFE50914)
                        NovelSource.ROYAL_ROAD -> Color(0xFF3B82F6)
                        NovelSource.WUXIA_WORLD -> Color(0xFF10B981)
                        else -> OttPrimary
                    }
                    val icon = when (src) {
                        NovelSource.NOVEL_FULL -> Icons.Default.MenuBook
                        NovelSource.NOVEL_FIRE -> Icons.Default.Whatshot
                        NovelSource.ROYAL_ROAD -> Icons.Default.Star
                        NovelSource.WUXIA_WORLD -> Icons.Default.AutoStories
                        else -> Icons.Default.MenuBook
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) Modifier.background(sourceGradient)
                                else Modifier.background(Color(0x10FFFFFF))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0x60FFFFFF) else Color(0x18FFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSourceSelect(src) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = src.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = Color.White
                            )
                            if (src.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isSelected) Color(0x35000000) else Color(0x20FFFFFF))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = src.badge,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xB3FFFFFF)
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

@Composable
private fun NovelHeroCard(
    novel: NovelItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = rememberNovelImageRequest(novel.cover),
            contentDescription = novel.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF506080E), Color(0xCC06080E), Color(0x5506080E), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(Color(0x33FF1E27))
                    .border(0.8.dp, Color(0x88FF1E27), OttPillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF4D55), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("RANK #1 NOVEL", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = novel.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            novel.stats?.let { stats ->
                Text(
                    text = stats,
                    fontSize = 11.5.sp,
                    color = OttTextSecondary,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Read Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun NovelSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = OttTextPrimary
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = OttTextSecondary
        )
    }
}

@Composable
private fun NovelRankingsRow(
    novels: List<NovelItem>,
    onNovelClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(novels.take(10), key = { _, n -> n.slug }) { index, novel ->
            val isTen = index == 9
            Row(
                modifier = Modifier
                    .width(if (isTen) 220.dp else 185.dp)
                    .clickable { onNovelClick(novel.slug) },
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = if (isTen) 68.sp else 74.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0x38FFFFFF),
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .width(if (isTen) 76.dp else 46.dp),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    letterSpacing = if (isTen) (-2).sp else 0.sp
                )

                NovelCard(novel = novel, onClick = { onNovelClick(novel.slug) })
            }
        }
    }
}

@Composable
private fun NovelHorizontalRow(
    novels: List<NovelItem>,
    onNovelClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(novels, key = { it.slug }) { novel ->
            NovelCard(novel = novel, onClick = { onNovelClick(novel.slug) })
        }
    }
}

@Composable
private fun NovelCard(
    novel: NovelItem,
    onClick: () -> Unit
) {
    val coordinator = LocalSharedPosterCoordinator.current
    val itemKey = "novel_${novel.slug}"
    val isSharedActive = coordinator?.isItemActive(itemKey) == true
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = Modifier
            .width(140.dp)
            .mediaCardPhysics(pressScale = 0.95f, enableTilt = true)
            .clickable {
                val bounds = cardBounds
                if (coordinator != null && bounds != null && bounds.width > 0) {
                    coordinator.startForwardTransition(
                        itemKey = itemKey,
                        posterUrl = novel.cover,
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
                .height(200.dp)
                .onGloballyPositioned { coords ->
                    cardBounds = coords.boundsInWindow()
                }
                .clip(OttCardShape)
                .background(Color(0x15FFFFFF))
                .border(1.dp, Color(0x22FFFFFF), OttCardShape)
        ) {
            AsyncImage(
                model = rememberNovelImageRequest(novel.cover),
                contentDescription = novel.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = if (isSharedActive) 0f else 1f
                    },
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OttCardBottomGradient)
            )

            novel.rating?.let { rate ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xB3000000))
                        .border(0.5.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(text = rate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = novel.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                novel.stats?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = OttTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelGridCard(
    novel: NovelItem,
    onClick: () -> Unit
) {
    val coordinator = LocalSharedPosterCoordinator.current
    val itemKey = "novel_${novel.slug}"
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
                        posterUrl = novel.cover,
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
                .clip(OttCardShape)
                .background(Color(0x15FFFFFF))
                .border(1.dp, Color(0x22FFFFFF), OttCardShape)
        ) {
            AsyncImage(
                model = rememberNovelImageRequest(novel.cover),
                contentDescription = novel.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = if (isSharedActive) 0f else 1f
                    },
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OttCardBottomGradient)
            )

            novel.rating?.let { rate ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(text = rate, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = novel.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NovelGenreChips(
    selectedGenre: String,
    onSelectGenre: (String) -> Unit
) {
    val genres = listOf(
        "All", "Fantasy", "Action", "Romance", "Sci-fi",
        "Martial Arts", "Slice of Life", "Supernatural", "Video Games", "Josei"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            val isSelected = genre == selectedGenre
            Box(
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(if (isSelected) Color.White else Color(0x18FFFFFF))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.White else Color(0x28FFFFFF),
                        shape = OttPillShape
                    )
                    .clickable { onSelectGenre(genre) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = genre,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else OttTextSecondary
                )
            }
        }
    }
}

@Composable
private fun NovelContinueReadingRow(
    history: List<NovelHistoryItem>,
    onNovelClick: (String) -> Unit,
    onReadClick: (novelSlug: String, chapterSlug: String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column {
        NovelSectionHeader(
            title = "Continue Reading",
            subtitle = "Jump back into your web novels"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(history, key = { it.slug }) { item ->
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(OttCardShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                        .clickable { onReadClick(item.slug, item.lastChapterSlug) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        if (item.cover.isNotBlank()) {
                            AsyncImage(
                                model = rememberNovelImageRequest(item.cover),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF161822)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0x40FFFFFF),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55000000))
                        )

                        // Center Read Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = "Read",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Remove button at top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .clickable { onRemove(item.slug) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xCCFFFFFF),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.lastChapterTitle,
                            fontSize = 10.5.sp,
                            color = OttTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
