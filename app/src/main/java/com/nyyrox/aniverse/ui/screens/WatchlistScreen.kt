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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.WatchHistoryItem
import com.nyyrox.aniverse.data.WatchlistItem
import com.nyyrox.aniverse.data.WatchlistStatus
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun WatchlistScreen(
    vm: MainViewModel,
    initialTab: Int = 0,
    onAnimeClick: (Int) -> Unit,
    onPlay: (animeId: Int, episode: Int) -> Unit,
    onMangaClick: (String) -> Unit = {},
    onReadManga: (mangaId: String, chapterId: String) -> Unit = { _, _ -> },
    onNovelClick: (String) -> Unit = {},
    onReadNovel: (novelSlug: String, chapterSlug: String) -> Unit = { _, _ -> },
    onBack: (() -> Unit)? = null
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        "Anime List (${vm.watchlist.size})",
        "Anime History (${vm.history.size})",
        "Saved Manga (${vm.mangaBookmarks.size})",
        "Manga History (${vm.mangaHistory.size})",
        "Novel History (${vm.novelHistoryList.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .statusBarsPadding()
            .padding(top = if (onBack != null) 4.dp else 48.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x35FFFFFF), Color(0x12FFFFFF))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x60FFFFFF), Color(0x18FFFFFF))
                            ),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = "Library & Watchlist",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = OttTextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Tracker Sync Quick Action
            val hasConnectedTrackers = vm.aniListAccount != null || vm.malAccount != null
            if (hasConnectedTrackers) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), OttPillShape)
                        .clickable(enabled = !vm.isTrackerSyncing) {
                            vm.syncTrackers()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (vm.isTrackerSyncing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Syncing...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Sync Trackers",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // OTT Tabs (Scrollable to comfortably display both Anime & Manga categories)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = OttPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = OttPrimary,
                        height = 3.dp
                    )
                }
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OttSurfaceBorder)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) OttTextPrimary else OttTextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    // Anime Watchlist with Categorized Filters
                    var selectedFilter by remember { mutableStateOf(WatchlistStatus.ALL) }
                    var statusDialogItem by remember { mutableStateOf<WatchlistItem?>(null) }

                    val filteredList = remember(vm.watchlist, selectedFilter) {
                        if (selectedFilter == WatchlistStatus.ALL) {
                            vm.watchlist
                        } else {
                            vm.watchlist.filter { it.watchlistStatus == selectedFilter }
                        }
                    }

                    if (statusDialogItem != null) {
                        val item = statusDialogItem!!
                        WatchlistStatusSelectorDialog(
                            title = item.title,
                            currentStatus = item.watchlistStatus,
                            currentEpisode = item.progressEpisode,
                            totalEpisodes = item.episodes,
                            initialCustomTags = item.customTags,
                            onDismiss = { statusDialogItem = null },
                            onSave = { newStatus, newEp, newTags ->
                                vm.updateWatchlistDetails(
                                    animeId = item.animeId,
                                    status = newStatus,
                                    progressEpisode = newEp,
                                    customTags = newTags
                                )
                                statusDialogItem = null
                            },
                            onRemove = {
                                vm.removeFromWatchlist(item.animeId)
                                statusDialogItem = null
                            }
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Category Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(WatchlistStatus.FILTERS) { filter ->
                                val count = vm.getWatchlistCount(filter)
                                val isSelected = selectedFilter == filter
                                val chipBg = if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0x55FFFFFF), Color(0x28FFFFFF))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0x18FFFFFF), Color(0x0AFFFFFF))
                                    )
                                }
                                val chipBorder = if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0x95FFFFFF), Color(0x40FFFFFF))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0x28FFFFFF), Color(0x0CFFFFFF))
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(OttPillShape)
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, OttPillShape)
                                        .clickable { selectedFilter = filter }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = filter.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else OttTextSecondary
                                    )
                                    if (count > 0 || filter == WatchlistStatus.ALL) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) Color(0x40FFFFFF)
                                                    else Color(0x18FFFFFF)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "$count",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else OttTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (vm.watchlist.isEmpty()) {
                            EmptyLibraryState(
                                icon = Icons.Default.Bookmark,
                                title = "Your Watchlist is empty",
                                subtitle = "Bookmark anime series from Home or Details to quickly watch them later."
                            )
                        } else if (filteredList.isEmpty()) {
                            EmptyLibraryState(
                                icon = Icons.Default.BookmarkBorder,
                                title = "No anime in ${selectedFilter.label}",
                                subtitle = "You don't have any series marked as '${selectedFilter.label}'."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredList, key = { it.animeId }) { item ->
                                    WatchlistGridCard(
                                        item = item,
                                        onClick = { onAnimeClick(item.animeId) },
                                        onChangeStatus = { statusDialogItem = item },
                                        onRemove = { vm.removeFromWatchlist(item.animeId) },
                                        onQuickIncrement = { vm.incrementWatchlistProgress(item.animeId) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Anime History
                    if (vm.history.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Default.History,
                            title = "No watch history",
                            subtitle = "Episodes you start watching will automatically appear here."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(vm.history, key = { it.animeId }) { item ->
                                HistoryListItem(
                                    item = item,
                                    onResume = { onPlay(item.animeId, item.lastEpisode) },
                                    onDetails = { onAnimeClick(item.animeId) },
                                    onRemove = { vm.removeHistory(item.animeId) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Saved Manga (Bookmarks)
                    if (vm.mangaBookmarks.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Default.AutoStories,
                            title = "No saved manga",
                            subtitle = "Bookmark manga titles from the Manga section to read anytime."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(150.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = vm.mangaBookmarks,
                                key = { "${it.bookmarkType.name}:${it.mangaId}:${it.chapterId ?: ""}:${it.pageIndex ?: 0}" }
                            ) { item ->
                                MangaBookmarkGridCard(
                                    item = item,
                                    onClick = {
                                        if (item.bookmarkType == com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES || item.chapterId.isNullOrBlank()) {
                                            vm.searchManga(item.title)
                                            onMangaClick(item.mangaId)
                                        } else {
                                            onReadManga(item.mangaId, item.chapterId)
                                        }
                                    },
                                    onSearch = {
                                        vm.searchManga(item.title)
                                        onMangaClick(item.mangaId)
                                    },
                                    onRemove = {
                                        vm.removeMangaBookmark(
                                            mangaId = item.mangaId,
                                            bookmarkType = item.bookmarkType,
                                            chapterId = item.chapterId,
                                            pageIndex = item.pageIndex
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Manga Reading History
                    if (vm.mangaHistory.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Default.History,
                            title = "No manga reading history",
                            subtitle = "Chapters you read will automatically be saved here with page progress."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(vm.mangaHistory, key = { it.mangaId }) { item ->
                                MangaHistoryListItem(
                                    item = item,
                                    onResume = { onReadManga(item.mangaId, item.chapterId) },
                                    onDetails = { onMangaClick(item.mangaId) },
                                    onRemove = { vm.removeMangaHistory(item.mangaId) }
                                )
                            }
                        }
                    }
                }

                4 -> {
                    if (vm.novelHistoryList.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Default.AutoStories,
                            title = "No Novel History",
                            subtitle = "Chapters you read from web novels will automatically be saved here."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(vm.novelHistoryList, key = { it.slug }) { item ->
                                NovelHistoryListItem(
                                    item = item,
                                    onResume = { onReadNovel(item.slug, item.lastChapterSlug) },
                                    onDetails = { onNovelClick(item.slug) },
                                    onRemove = { vm.removeNovelHistory(item.slug) }
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
private fun WatchlistGridCard(
    item: WatchlistItem,
    onClick: () -> Unit,
    onChangeStatus: () -> Unit,
    onRemove: () -> Unit,
    onQuickIncrement: (() -> Unit)? = null
) {
    val statusColor = when (item.watchlistStatus) {
        WatchlistStatus.WATCHING -> Color(0xFF10B981)
        WatchlistStatus.PLAN_TO_WATCH -> Color(0xFF3B82F6)
        WatchlistStatus.COMPLETED -> Color(0xFFA855F7)
        WatchlistStatus.DROPPED -> Color(0xFFEF4444)
        WatchlistStatus.REWATCHING -> Color(0xFFF59E0B)
        else -> Color.White
    }
    val statusText = when (item.watchlistStatus) {
        WatchlistStatus.WATCHING -> {
            if (item.progressEpisode > 0) {
                if (item.episodes != null) "Watching • Ep ${item.progressEpisode}/${item.episodes}"
                else "Watching • Ep ${item.progressEpisode}"
            } else {
                "Watching"
            }
        }
        WatchlistStatus.REWATCHING -> {
            if (item.progressEpisode > 0) {
                if (item.episodes != null) "Rewatching • Ep ${item.progressEpisode}/${item.episodes}"
                else "Rewatching • Ep ${item.progressEpisode}"
            } else {
                "Rewatching"
            }
        }
        WatchlistStatus.COMPLETED -> "Completed"
        WatchlistStatus.PLAN_TO_WATCH -> "Plan to Watch"
        WatchlistStatus.DROPPED -> {
            if (item.progressEpisode > 0) "Dropped • Ep ${item.progressEpisode}"
            else "Dropped"
        }
        else -> item.watchlistStatus.label
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(OttCardShape)
                .background(OttSurface)
                .border(1.dp, OttSurfaceBorder, OttCardShape)
        ) {
            AsyncImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Remove button overlay on top right
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xB3000000))
            ) {
                Icon(
                    Icons.Default.BookmarkRemove,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Score Pill (Top Left)
            item.score?.let { score ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = OttAccentGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${(score / 10f).let { String.format(Locale.US, "%.1f", it) }}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )
                }
            }

            // Status Badge (Bottom Left)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD90C0F17))
                    .border(
                        0.8.dp,
                        Brush.verticalGradient(
                            listOf(Color(0x50FFFFFF), Color(0x15FFFFFF))
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onChangeStatus)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress Bar for Watching
            if (item.progressEpisode > 0 && item.episodes != null && item.episodes > 0) {
                val progressFraction = (item.progressEpisode.toFloat() / item.episodes.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x60000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(3.dp)
                            .background(statusColor)
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = item.title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = OttTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Proper Tags Row (Custom Tags + Genre Tags)
        if (item.customTags.isNotEmpty() || item.genres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            ) {
                items(item.customTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x35FF3366))
                            .border(0.6.dp, Color(0x60FF3366), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF99AA)
                        )
                    }
                }
                items(item.genres.take(2)) { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x18FFFFFF))
                            .border(0.6.dp, Color(0x25FFFFFF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = genre,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = OttTextSecondary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val epText = if (item.progressEpisode > 0 && item.episodes != null) {
                "EP ${item.progressEpisode}/${item.episodes}"
            } else if (item.episodes != null) {
                "${item.episodes} Episodes"
            } else {
                "Anime"
            }
            Text(
                text = epText,
                fontSize = 11.sp,
                color = OttTextSecondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.watchlistStatus == WatchlistStatus.WATCHING || item.watchlistStatus == WatchlistStatus.REWATCHING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2510B981))
                            .border(0.8.dp, Color(0x6010B981), RoundedCornerShape(6.dp))
                            .clickable { onQuickIncrement?.invoke() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+1 Ep",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                item.releaseYear?.let { year ->
                    Text(
                        text = year,
                        fontSize = 10.5.sp,
                        color = OttTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun WatchlistStatusSelectorDialog(
    title: String,
    currentStatus: WatchlistStatus,
    currentEpisode: Int = 0,
    totalEpisodes: Int? = null,
    initialCustomTags: List<String> = emptyList(),
    isNewItem: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (status: WatchlistStatus, episode: Int, customTags: List<String>) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var episode by remember { mutableIntStateOf(currentEpisode) }
    var episodeText by remember(currentEpisode) { mutableStateOf(currentEpisode.toString()) }
    var tags by remember { mutableStateOf(initialCustomTags) }
    var newTagInput by remember { mutableStateOf("") }

    val presetTags = listOf("Favorite", "Masterpiece", "Comfort", "Priority", "Classic", "Guilty Pleasure")

    fun updateEpisode(newVal: Int) {
        val maxLimit = if (totalEpisodes != null && totalEpisodes > 0) totalEpisodes else 99999
        val clamped = newVal.coerceIn(0, maxLimit)
        episode = clamped
        episodeText = clamped.toString()
        if (clamped > 0 && selectedStatus == WatchlistStatus.PLAN_TO_WATCH) {
            selectedStatus = WatchlistStatus.WATCHING
        }
        if (totalEpisodes != null && totalEpisodes > 0 && clamped >= totalEpisodes) {
            selectedStatus = WatchlistStatus.COMPLETED
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF161B29), Color(0xFF0D101A))
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0x55E11D48), Color(0x20FFFFFF), Color(0x08FFFFFF))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNewItem) "ADD TO WATCHLIST" else "CUSTOMIZE WATCHLIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = OttPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Status Selector Grid (2-column layout for clean modern OTT look)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "STATUS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextSecondary,
                            letterSpacing = 0.8.sp
                        )

                        val assignable = WatchlistStatus.ASSIGNABLE
                        assignable.chunked(2).forEach { rowStatuses ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStatuses.forEach { status ->
                                    val isSelected = selectedStatus == status
                                    val statusColor = when (status) {
                                        WatchlistStatus.WATCHING -> Color(0xFF10B981)
                                        WatchlistStatus.PLAN_TO_WATCH -> Color(0xFF3B82F6)
                                        WatchlistStatus.COMPLETED -> Color(0xFFA855F7)
                                        WatchlistStatus.DROPPED -> Color(0xFFEF4444)
                                        WatchlistStatus.REWATCHING -> Color(0xFFF59E0B)
                                        else -> Color.White
                                    }

                                    val bgBrush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(statusColor.copy(alpha = 0.28f), statusColor.copy(alpha = 0.12f))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0x18FFFFFF), Color(0x0CFFFFFF))
                                        )
                                    }

                                    val borderBrush = if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(statusColor, statusColor.copy(alpha = 0.6f))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0x25FFFFFF), Color(0x10FFFFFF))
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgBrush)
                                            .border(1.dp, borderBrush, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedStatus = status
                                                if (status == WatchlistStatus.COMPLETED && totalEpisodes != null && totalEpisodes > 0) {
                                                    updateEpisode(totalEpisodes)
                                                } else if (status == WatchlistStatus.WATCHING && episode == 0) {
                                                    updateEpisode(1)
                                                }
                                            }
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = status.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else OttTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = statusColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                if (rowStatuses.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Episode Progress Stepper & Direct Input
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EPISODE PROGRESS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OttTextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            if (totalEpisodes != null && totalEpisodes > 0) {
                                Text(
                                    text = "$totalEpisodes Total",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OttTextMuted
                                )
                            }
                        }

                        // Main Stepper Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x16FFFFFF))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Direct Editable Number Field
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Watched",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OttTextSecondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Ep",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x28FFFFFF))
                                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BasicTextField(
                                                value = episodeText,
                                                onValueChange = { input ->
                                                    val filtered = input.filter { it.isDigit() }
                                                    episodeText = filtered
                                                    val parsed = filtered.toIntOrNull() ?: 0
                                                    val maxLimit = if (totalEpisodes != null && totalEpisodes > 0) totalEpisodes else 99999
                                                    val clamped = parsed.coerceIn(0, maxLimit)
                                                    episode = clamped
                                                    if (clamped > 0 && selectedStatus == WatchlistStatus.PLAN_TO_WATCH) {
                                                        selectedStatus = WatchlistStatus.WATCHING
                                                    }
                                                    if (totalEpisodes != null && totalEpisodes > 0 && clamped >= totalEpisodes) {
                                                        selectedStatus = WatchlistStatus.COMPLETED
                                                    }
                                                },
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                cursorBrush = SolidColor(Color(0xFFE11D48)),
                                                singleLine = true,
                                                modifier = Modifier.widthIn(min = 36.dp, max = 65.dp)
                                            )
                                        }
                                        if (totalEpisodes != null && totalEpisodes > 0) {
                                            Text(
                                                text = "/ $totalEpisodes",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OttTextSecondary
                                            )
                                        }
                                    }
                                }

                                // Stepper Action Buttons (Proper vector arrows, zero white blobs)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Decrement Button
                                    IconButton(
                                        onClick = { if (episode > 0) updateEpisode(episode - 1) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x22FFFFFF))
                                            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease Episode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Increment Button (Zenkai Crimson Gradient)
                                    IconButton(
                                        onClick = { updateEpisode(episode + 1) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFE11D48), Color(0xFFBE123C))
                                                )
                                            )
                                            .border(1.dp, Color(0x70E11D48), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase Episode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Jump Chips (Crucial for high episode anime like One Piece)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chips = listOf(
                                    "-10" to { updateEpisode(episode - 10) },
                                    "-1" to { updateEpisode(episode - 1) },
                                    "+1" to { updateEpisode(episode + 1) },
                                    "+10" to { updateEpisode(episode + 10) }
                                )
                                chips.forEach { (label, onClick) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x18FFFFFF))
                                            .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { onClick() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (totalEpisodes != null && totalEpisodes > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x30E11D48))
                                            .border(0.8.dp, Color(0x70E11D48), RoundedCornerShape(8.dp))
                                            .clickable { updateEpisode(totalEpisodes) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Max",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFB3C1)
                                        )
                                    }
                                }
                            }

                            // Real-time Progress Bar
                            if (totalEpisodes != null && totalEpisodes > 0) {
                                val progress = (episode.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f)
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0x22FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFE11D48), Color(0xFFFF4D6D))
                                                    )
                                                )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OttTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // List Roles & Custom Tags Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "LIST ROLES & TAGS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextSecondary,
                            letterSpacing = 0.8.sp
                        )

                        // Current Assigned Tags
                        if (tags.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(tags) { tag ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x35E11D48))
                                            .border(0.8.dp, Color(0x75E11D48), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(text = tag, fontSize = 11.sp, color = Color(0xFFFFB3C1), fontWeight = FontWeight.Bold)
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove role tag",
                                            tint = Color(0xFFFFB3C1),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { tags = tags - tag }
                                        )
                                    }
                                }
                            }
                        }

                        // Preset Role Suggestions (NO EMOJIS)
                        val availablePresets = presetTags.filterNot { tags.contains(it) }
                        if (availablePresets.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                items(availablePresets) { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x16FFFFFF))
                                            .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                                            .clickable { tags = tags + preset }
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "+ $preset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = OttTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Add Custom Tag Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x16FFFFFF))
                                .border(0.8.dp, Color(0x28FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newTagInput,
                                onValueChange = { newTagInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (newTagInput.isEmpty()) {
                                        Text(
                                            text = "Add custom role or tag...",
                                            color = OttTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (newTagInput.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE11D48))
                                        .clickable {
                                            val trimmed = newTagInput.trim()
                                            if (trimmed.isNotEmpty() && !tags.contains(trimmed)) {
                                                tags = tags + trimmed
                                            }
                                            newTagInput = ""
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Action Buttons (Save & Remove)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSave(selectedStatus, episode, tags)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (isNewItem) "Add to Watchlist" else "Save Changes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        if (onRemove != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1DEF4444))
                                    .border(1.dp, Color(0x35EF4444), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onRemove()
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Remove from Watchlist",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B6B)
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
fun WatchlistStatusSelectorDialog(
    item: WatchlistItem,
    onDismiss: () -> Unit,
    onSelectStatus: (WatchlistStatus) -> Unit,
    onRemove: () -> Unit
) {
    WatchlistStatusSelectorDialog(
        title = item.title,
        currentStatus = item.watchlistStatus,
        currentEpisode = item.progressEpisode,
        totalEpisodes = item.episodes,
        initialCustomTags = item.customTags,
        isNewItem = false,
        onDismiss = onDismiss,
        onSave = { newStatus, _, _ -> onSelectStatus(newStatus) },
        onRemove = onRemove
    )
}

@Composable
fun WatchlistStatusSelectorDialog(
    title: String,
    currentStatus: WatchlistStatus,
    onDismiss: () -> Unit,
    onSelectStatus: (WatchlistStatus) -> Unit,
    onRemove: () -> Unit
) {
    WatchlistStatusSelectorDialog(
        title = title,
        currentStatus = currentStatus,
        currentEpisode = 0,
        totalEpisodes = null,
        initialCustomTags = emptyList(),
        isNewItem = false,
        onDismiss = onDismiss,
        onSave = { newStatus, _, _ -> onSelectStatus(newStatus) },
        onRemove = onRemove
    )
}

@Composable
private fun HistoryListItem(
    item: WatchHistoryItem,
    onResume: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(OttSurface)
            .border(1.dp, OttSurfaceBorder, OttCardShape)
            .clickable(onClick = onResume)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with play button
        Box(
            modifier = Modifier
                .size(105.dp, 68.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.episodeStill?.takeIf { it.isNotBlank() } ?: item.banner ?: item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = OttPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            val total = item.totalEpisodes ?: (item.lastEpisode + 4)
            val progress = (item.lastEpisode.toFloat() / total.toFloat()).coerceIn(0.1f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = OttPrimary,
                trackColor = Color(0x66FFFFFF)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last watched: Episode ${item.lastEpisode}",
                fontSize = 12.sp,
                color = OttSecondary
            )
            item.totalEpisodes?.let {
                Text(
                    text = "$it Total Episodes",
                    fontSize = 11.sp,
                    color = OttTextMuted
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Remove from history",
                tint = OttTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OttTextMuted,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = OttTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MangaBookmarkGridCard(
    item: MangaBookmarkItem,
    onClick: () -> Unit,
    onSearch: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(OttCardShape)
                .background(OttSurface)
                .border(1.dp, OttSurfaceBorder, OttCardShape)
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Bookmark indicator top-left (Custom badge depending on type)
            val badgeColor = when (item.bookmarkType) {
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE -> Color(0xFF4FC3F7)
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER -> OttAccentGold
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES -> OttPrimary
            }
            val badgeText = when (item.bookmarkType) {
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE -> "PAGE ${(item.pageIndex ?: 0) + 1}"
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER -> "CH ${item.chapterNumber ?: ""}"
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES -> item.type?.uppercase() ?: "MANGA"
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD9000000))
                    .border(0.8.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            // Action buttons overlay top-right (Search & Remove)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000))
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Manga",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000))
                ) {
                    Icon(
                        Icons.Default.BookmarkRemove,
                        contentDescription = "Remove",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OttTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val subtitleText = when (item.bookmarkType) {
            com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE ->
                item.chapterNumber?.let { "Ch. $it • Pg ${(item.pageIndex ?: 0) + 1}" } ?: "Page Bookmark"
            com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER ->
                item.chapterNumber?.let { "Chapter $it" } ?: (item.latestChapter ?: "Chapter Bookmark")
            com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES ->
                item.latestChapter?.let { "Ch. $it" } ?: "Series Bookmark"
        }

        Text(
            text = subtitleText,
            fontSize = 11.sp,
            color = OttTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MangaHistoryListItem(
    item: MangaHistoryItem,
    onResume: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(OttSurface)
            .border(1.dp, OttSurfaceBorder, OttCardShape)
            .clickable(onClick = onResume)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(72.dp, 96.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000))
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = OttPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last read: Chapter ${item.chapterNumber}",
                fontSize = 12.sp,
                color = OttSecondary
            )
            if (item.pageCount > 0 || item.pageIndex > 0) {
                Text(
                    text = "Page ${item.pageIndex + 1}${if (item.pageCount > 0) " of ${item.pageCount}" else ""}",
                    fontSize = 11.sp,
                    color = OttTextMuted
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(OttPrimary.copy(alpha = 0.2f))
                    .clickable(onClick = onResume)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Continue Reading",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttPrimary
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Remove from history",
                tint = OttTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NovelHistoryListItem(
    item: NovelHistoryItem,
    onResume: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(OttSurface)
            .border(1.dp, OttSurfaceBorder, OttCardShape)
            .clickable(onClick = onResume)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(72.dp, 96.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (item.cover.isNotBlank()) {
                AsyncImage(
                    model = item.cover,
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
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color(0x40FFFFFF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000))
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = OttPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OttTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last read: ${item.lastChapterTitle}",
                fontSize = 12.sp,
                color = OttSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(OttPillShape)
                    .background(OttPrimary.copy(alpha = 0.2f))
                    .clickable(onClick = onResume)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Continue Reading",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttPrimary
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Remove from history",
                tint = OttTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
