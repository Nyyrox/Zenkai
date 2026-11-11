package com.nyyrox.aniverse.ui.screens

import android.widget.Toast

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.aspectRatio

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

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.AutoStories

import androidx.compose.material.icons.filled.Bookmark

import androidx.compose.material.icons.filled.BookmarkBorder

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.DeleteOutline

import androidx.compose.material.icons.filled.History

import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Star

import androidx.compose.material.icons.filled.Sync

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.material3.ModalBottomSheet

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.ScrollableTabRow

import androidx.compose.material3.Tab

import androidx.compose.material3.TabRow

import androidx.compose.material3.TabRowDefaults

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.material3.Text

import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil3.compose.AsyncImage

import com.nyyrox.aniverse.data.supabase.AuthState

import com.nyyrox.aniverse.data.WatchHistoryItem

import com.nyyrox.aniverse.data.WatchlistItem

import com.nyyrox.aniverse.data.WatchlistStatus

import com.nyyrox.aniverse.data.manga.MangaBookmarkItem

import com.nyyrox.aniverse.data.manga.MangaHistoryItem

import com.nyyrox.aniverse.data.novel.NovelHistoryItem

import com.nyyrox.aniverse.ui.theme.LocalAppColors

import com.nyyrox.aniverse.viewmodel.MainViewModel

data class StatusFilterOption(val status: WatchlistStatus?, val label: String)

enum class MediaTab(val label: String) {

    ANIME("Anime"),

    MANGA("Manga"),

    NOVELS("Novels")

}

private data class PendingDeleteTarget(
    val title: String,
    val dialogTitle: String,
    val dialogMessage: String,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)

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

    val context = LocalContext.current

    val colors = LocalAppColors.current

    val initialMediaTab = when (initialTab) {

        1 -> MediaTab.ANIME

        2 -> MediaTab.MANGA

        3 -> MediaTab.MANGA

        4 -> MediaTab.NOVELS

        else -> MediaTab.ANIME

    }

    var selectedMediaTab by remember { mutableStateOf(initialMediaTab) }

    var selectedStatusFilter by remember { mutableStateOf<WatchlistStatus?>(null) } // null = All

    // Status change dialog state

    var statusDialogAnimeItem by remember { mutableStateOf<WatchlistItem?>(null) }
    var pendingDeleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }

    val aniListAccount = vm.aniListAccount

    val authState = vm.authState

    val isSyncing = vm.isCloudSyncing || vm.isTrackerSyncing

    val syncEyebrow = when {

        aniListAccount != null -> "Synced with AniList • ${aniListAccount.username}"

        authState is AuthState.LoggedIn -> "Synced with Cloud"

        else -> "Saved on this device"

    }

                    val filteredAnimeList = remember(vm.watchlist, selectedStatusFilter) {

                        if (selectedStatusFilter == null) vm.watchlist

                        else vm.watchlist.filter { it.watchlistStatus == selectedStatusFilter }

                    }

    BoxWithConstraints(

        modifier = Modifier

            .fillMaxSize()

            .background(colors.bg)

    ) {

        val gridColumns = when {
            maxWidth >= 1200.dp -> 6
            maxWidth >= 900.dp -> 5
            maxWidth >= 600.dp -> 4
            maxWidth >= 440.dp -> 3
            else -> 2
        }

        LazyColumn(

            modifier = Modifier

                .fillMaxSize()

                .statusBarsPadding()

                .padding(horizontal = 16.dp),

            verticalArrangement = Arrangement.spacedBy(18.dp)

        ) {

            // Header

            item {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(top = 12.dp, bottom = 4.dp)

                ) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            if (onBack != null) {

                                IconButton(

                                    onClick = onBack,

                                    modifier = Modifier

                                        .size(40.dp)

                                        .clip(CircleShape)

                                        .background(colors.surface)

                                        .border(1.dp, colors.glass.border, CircleShape)

                                ) {

                                    Icon(

                                        Icons.AutoMirrored.Filled.ArrowBack,

                                        contentDescription = "Back",

                                        tint = colors.textPrimary,

                                        modifier = Modifier.size(20.dp)

                                    )

                                }

                                Spacer(Modifier.width(12.dp))

                            }

                            Column {

                                Text(

                                    text = syncEyebrow,

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    color = colors.accent

                                )

                                Text(

                                    text = "My Library",

                                    fontSize = 24.sp,

                                    fontWeight = FontWeight.Black,

                                    color = colors.textPrimary

                                )

                            }

                        }

                        // Quick Sync button

                        Row(

                            modifier = Modifier

                                .clip(CircleShape)

                                .background(colors.surface)

                                .border(1.dp, colors.glass.border, CircleShape)

                                .clickable(enabled = !isSyncing) {

                                    if (aniListAccount != null) {

                                        vm.syncTrackers { res ->

                                            Toast.makeText(context, "Synced AniList (${res.totalPulled} items)", Toast.LENGTH_SHORT).show()

                                        }

                                    } else if (authState is AuthState.LoggedIn) {

                                        vm.pushToCloud { ok ->

                                            Toast.makeText(context, if (ok) "Cloud synced" else "Sync failed", Toast.LENGTH_SHORT).show()

                                        }

                                    } else {

                                        Toast.makeText(context, "Saved locally on device", Toast.LENGTH_SHORT).show()

                                    }

                                }

                                .padding(horizontal = 12.dp, vertical = 7.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            if (isSyncing) {

                                CircularProgressIndicator(

                                    modifier = Modifier.size(13.dp),

                                    color = colors.accent,

                                    strokeWidth = 2.dp

                                )

                            } else {

                                Icon(

                                    Icons.Default.Sync,

                                    contentDescription = "Sync",

                                    tint = colors.textPrimary,

                                    modifier = Modifier.size(14.dp)

                                )

                            }

                            Spacer(Modifier.width(6.dp))

                            Text(

                                text = "Sync",

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold,

                                color = colors.textPrimary

                            )

                        }

                    }

                }

            }

            // 1. Continue Watching Shelf (Row of recently watched episodes)

            if (vm.history.isNotEmpty()) {

                item {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        Text(

                            text = "Pick up where you left off",

                            fontSize = 11.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.accent

                        )

                        Text(

                            text = "Continue Watching",

                            fontSize = 18.sp,

                            fontWeight = FontWeight.Bold,

                            color = colors.textPrimary

                        )

                        Spacer(Modifier.height(10.dp))

                        LazyRow(

                            horizontalArrangement = Arrangement.spacedBy(12.dp),

                            contentPadding = PaddingValues(end = 16.dp)

                        ) {

                            items(vm.history.take(15), key = { it.animeId }) { h ->

                                ContinueWatchingCard(

                                    item = h,

                                    onPlay = { onPlay(h.animeId, h.lastEpisode) },

                                    onRemove = {
                                        pendingDeleteTarget = PendingDeleteTarget(
                                            title = h.title,
                                            dialogTitle = "Remove from History?",
                                            dialogMessage = "Remove \"${h.title}\" from continue watching?",
                                            onConfirm = {
                                                vm.removeHistory(h.animeId)
                                                Toast.makeText(context, "Removed from continue watching", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                )

                            }

                        }

                    }

                }

            }

            // 2. Continue Reading Shelf (Manga & Novels progress)

            val hasMangaHistory = vm.mangaHistory.isNotEmpty()

            val hasNovelHistory = vm.novelHistoryList.isNotEmpty()

            if (hasMangaHistory || hasNovelHistory) {

                item {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        Text(

                            text = "Pick up your chapters",

                            fontSize = 11.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.accent

                        )

                        Text(

                            text = "Continue Reading",

                            fontSize = 18.sp,

                            fontWeight = FontWeight.Bold,

                            color = colors.textPrimary

                        )

                        Spacer(Modifier.height(10.dp))

                        LazyRow(

                            horizontalArrangement = Arrangement.spacedBy(12.dp),

                            contentPadding = PaddingValues(end = 16.dp)

                        ) {

                            items(vm.mangaHistory.take(8), key = { "manga_${it.mangaId}" }) { m ->

                                ContinueMangaCard(

                                    item = m,

                                    onRead = { onReadManga(m.mangaId, m.chapterId) },

                                    onRemove = {
                                        pendingDeleteTarget = PendingDeleteTarget(
                                            title = m.title,
                                            dialogTitle = "Remove Reading History?",
                                            dialogMessage = "Remove \"${m.title}\" from reading history?",
                                            onConfirm = {
                                                vm.removeMangaHistory(m.mangaId)
                                                Toast.makeText(context, "Removed from reading history", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                )

                            }

                            items(vm.novelHistoryList.take(8), key = { "novel_${it.slug}" }) { n ->

                                ContinueNovelCard(

                                    item = n,

                                    onRead = { onReadNovel(n.slug, n.lastChapterSlug) },

                                    onRemove = {
                                        pendingDeleteTarget = PendingDeleteTarget(
                                            title = n.title,
                                            dialogTitle = "Remove Reading History?",
                                            dialogMessage = "Remove \"${n.title}\" from reading history?",
                                            onConfirm = {
                                                vm.removeNovelHistory(n.slug)
                                                Toast.makeText(context, "Removed from reading history", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }

                                )

                            }

                        }

                    }

                }

            }

            // 3. Media Switcher: Anime / Manga / Novels

            item {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(16.dp))

                        .background(colors.surface)

                        .border(1.dp, colors.glass.border, RoundedCornerShape(16.dp))

                        .padding(4.dp),

                    horizontalArrangement = Arrangement.spacedBy(4.dp)

                ) {

                    MediaTab.entries.forEach { tab ->

                        val count = when (tab) {

                            MediaTab.ANIME -> vm.watchlist.size

                            MediaTab.MANGA -> vm.mangaBookmarks.size

                            MediaTab.NOVELS -> vm.novelHistoryList.size

                        }

                        val isSelected = selectedMediaTab == tab

                        Box(

                            modifier = Modifier

                                .weight(1f)

                                .clip(RoundedCornerShape(12.dp))

                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)

                                .border(1.dp, if (isSelected) colors.textPrimary else Color.Transparent, RoundedCornerShape(12.dp))

                                .clickable { selectedMediaTab = tab }

                                .padding(vertical = 8.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(

                                    text = tab.label,

                                    fontSize = 13.sp,

                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                    color = if (isSelected) colors.textPrimary else colors.textMuted

                                )

                                Spacer(Modifier.width(5.dp))

                                Text(

                                    text = "($count)",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Normal,

                                    color = if (isSelected) colors.textPrimary else colors.textMuted

                                )

                            }

                        }

                    }

                }

            }

            // 4. Content Area

            when (selectedMediaTab) {

                MediaTab.ANIME -> {

                    // Status filter chips

                    item {

                        val statusList = listOf(

                            Pair<WatchlistStatus?, String>(null, "All (${vm.watchlist.size})"),

                            Pair<WatchlistStatus?, String>(WatchlistStatus.WATCHING, "Watching (${vm.getWatchlistCount(WatchlistStatus.WATCHING)})"),

                            Pair<WatchlistStatus?, String>(WatchlistStatus.PLAN_TO_WATCH, "Plan to watch (${vm.getWatchlistCount(WatchlistStatus.PLAN_TO_WATCH)})"),

                            Pair<WatchlistStatus?, String>(WatchlistStatus.COMPLETED, "Completed (${vm.getWatchlistCount(WatchlistStatus.COMPLETED)})"),

                            Pair<WatchlistStatus?, String>(WatchlistStatus.REWATCHING, "Rewatching (${vm.getWatchlistCount(WatchlistStatus.REWATCHING)})"),

                            Pair<WatchlistStatus?, String>(WatchlistStatus.DROPPED, "Dropped (${vm.getWatchlistCount(WatchlistStatus.DROPPED)})")

                        )

                        LazyRow(

                            horizontalArrangement = Arrangement.spacedBy(8.dp),

                            contentPadding = PaddingValues(end = 16.dp)

                        ) {

                            items(statusList) { itemPair ->

                                val status = itemPair.first

                                val label = itemPair.second

                                val isSelected = selectedStatusFilter == status

                                Box(

                                    modifier = Modifier

                                        .clip(CircleShape)

                                        .background(if (isSelected) colors.textPrimary else colors.surface)

                                        .border(1.dp, if (isSelected) colors.textPrimary else colors.glass.border, CircleShape)

                                        .clickable { selectedStatusFilter = status }

                                        .padding(horizontal = 14.dp, vertical = 7.dp)

                                ) {

                                    Text(

                                        text = label,

                                        fontSize = 12.sp,

                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                        color = if (isSelected) colors.bg else colors.textPrimary

                                    )

                                }

                            }

                        }

                    }


                    if (filteredAnimeList.isEmpty()) {

                        item {

                            EmptyLibraryState(

                                title = if (vm.watchlist.isEmpty()) "Your list is empty" else "No anime in this section",

                                subtitle = "Save anime to your library from any poster or title page."

                            )

                        }

                    } else {

                        item {

                            Column(

                                modifier = Modifier.fillMaxWidth(),

                                verticalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                filteredAnimeList.chunked(gridColumns).forEach { rowItems ->

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.spacedBy(12.dp)

                                    ) {

                                        for (item in rowItems) {

                                            Box(modifier = Modifier.weight(1f)) {

                                                CleanAnimeGridCard(

                                                    item = item,

                                                    onClick = { onAnimeClick(item.animeId) },

                                                    onIncrementProgress = {

                                                        vm.incrementWatchlistProgress(item.animeId)

                                                        Toast.makeText(context, "Progress: Ep ${item.progressEpisode + 1}", Toast.LENGTH_SHORT).show()

                                                    },

                                                    onChangeStatus = {

                                                        statusDialogAnimeItem = item

                                                    },

                                                    onRemove = {
                                                        pendingDeleteTarget = PendingDeleteTarget(
                                                            title = item.title,
                                                            dialogTitle = "Remove from Watchlist?",
                                                            dialogMessage = "Are you sure you want to remove \"${item.title}\" from your watchlist?",
                                                            onConfirm = {
                                                                vm.removeFromWatchlist(item.animeId)
                                                                Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }

                                                )

                                            }

                                        }

                                        val emptySlots = gridColumns - rowItems.size
                                        repeat(emptySlots) {
                                            Spacer(Modifier.weight(1f))
                                        }

                                    }

                                }

                            }

                        }

                    }

                }

                MediaTab.MANGA -> {

                    if (vm.mangaBookmarks.isEmpty()) {

                        item {

                            EmptyLibraryState(

                                title = "No saved manga",

                                subtitle = "Bookmark manga while browsing to read them anytime."

                            )

                        }

                    } else {

                        item {

                            Column(

                                modifier = Modifier.fillMaxWidth(),

                                verticalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                vm.mangaBookmarks.chunked(gridColumns).forEach { rowItems ->

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.spacedBy(12.dp)

                                    ) {

                                        for (m in rowItems) {

                                            Box(modifier = Modifier.weight(1f)) {

                                                CleanMangaBookmarkCard(

                                                    item = m,

                                                    onClick = { onMangaClick(m.mangaId) },

                                                    onRemove = {
                                                        pendingDeleteTarget = PendingDeleteTarget(
                                                            title = m.title,
                                                            dialogTitle = "Remove Bookmark?",
                                                            dialogMessage = "Remove \"${m.title}\" from saved bookmarks?",
                                                            onConfirm = {
                                                                vm.toggleMangaBookmark(m.mangaId, m.title, m.poster, m.type, m.latestChapter)
                                                                Toast.makeText(context, "Removed bookmark", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }

                                                )

                                            }

                                        }

                                        val emptySlots = gridColumns - rowItems.size
                                        repeat(emptySlots) {
                                            Spacer(Modifier.weight(1f))
                                        }

                                    }

                                }

                            }

                        }

                    }

                }

                MediaTab.NOVELS -> {

                    if (vm.novelHistoryList.isEmpty()) {

                        item {

                            EmptyLibraryState(

                                title = "No reading history",

                                subtitle = "Start reading light novels to track chapters here."

                            )

                        }

                    } else {

                        item {

                            Column(

                                modifier = Modifier.fillMaxWidth(),

                                verticalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                vm.novelHistoryList.chunked(gridColumns).forEach { rowItems ->

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.spacedBy(12.dp)

                                    ) {

                                        for (n in rowItems) {

                                            Box(modifier = Modifier.weight(1f)) {

                                                CleanNovelCard(

                                                    item = n,

                                                    onClick = { onNovelClick(n.slug) },

                                                    onRead = { onReadNovel(n.slug, n.lastChapterSlug) },

                                                    onRemove = {
                                                        pendingDeleteTarget = PendingDeleteTarget(
                                                            title = n.title,
                                                            dialogTitle = "Remove Reading History?",
                                                            dialogMessage = "Remove \"${n.title}\" from novel reading history?",
                                                            onConfirm = {
                                                                vm.removeNovelHistory(n.slug)
                                                                Toast.makeText(context, "Removed history", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }

                                                )

                                            }

                                        }

                                        val emptySlots = gridColumns - rowItems.size
                                        repeat(emptySlots) {
                                            Spacer(Modifier.weight(1f))
                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            }

            item {

                Spacer(Modifier.height(36.dp))

            }

        }

        // Status Selection Bottom Sheet

        if (statusDialogAnimeItem != null) {

            val item = statusDialogAnimeItem!!

            ModalBottomSheet(

                onDismissRequest = { statusDialogAnimeItem = null },

                containerColor = colors.bg,

                dragHandle = null

            ) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(20.dp),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    Text(

                        text = "Set Status for ${item.title}",

                        fontSize = 16.sp,

                        fontWeight = FontWeight.Bold,

                        color = colors.textPrimary,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis

                    )

                    HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                    listOf(

                        WatchlistStatus.WATCHING to "Watching",

                        WatchlistStatus.PLAN_TO_WATCH to "Plan to watch",

                        WatchlistStatus.COMPLETED to "Completed",

                        WatchlistStatus.REWATCHING to "Rewatching",

                        WatchlistStatus.DROPPED to "Dropped"

                    ).forEach { (st, label) ->

                        val isSelected = item.watchlistStatus == st

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clip(RoundedCornerShape(12.dp))

                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)

                                .border(1.dp, if (isSelected) colors.textPrimary else colors.glass.border, RoundedCornerShape(12.dp))

                                .clickable {

                                    vm.setWatchlistStatus(item.animeId, st)

                                    statusDialogAnimeItem = null

                                    Toast.makeText(context, "Status: $label", Toast.LENGTH_SHORT).show()

                                }

                                .padding(horizontal = 16.dp, vertical = 12.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {

                            Text(

                                text = label,

                                fontSize = 14.sp,

                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,

                                color = colors.textPrimary

                            )

                            if (isSelected) {

                                Icon(

                                    Icons.Default.Check,

                                    contentDescription = null,

                                    tint = colors.textPrimary,

                                    modifier = Modifier.size(16.dp)

                                )

                            }

                        }

                    }

                    Spacer(Modifier.height(8.dp))

                }

            }

        }

    }

    pendingDeleteTarget?.let { target ->
        com.nyyrox.aniverse.ui.components.RemoveConfirmDialog(
            title = target.dialogTitle,
            message = target.dialogMessage,
            confirmText = "Remove",
            cancelText = "Cancel",
            onConfirm = target.onConfirm,
            onDismiss = { pendingDeleteTarget = null }
        )
    }

}

@Composable

fun ContinueWatchingCard(

    item: WatchHistoryItem,

    onPlay: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    val progressPct = if (item.durationMs > 0) {

        ((item.positionMs.toFloat() / item.durationMs.toFloat()) * 100).coerceIn(0f, 100f)

    } else 0f

    val isDone = item.durationMs > 0 && item.positionMs >= (item.durationMs * 0.9f)

    val leftMinutes = if (item.durationMs > 0) {

        val remSeconds = (item.durationMs - item.positionMs) / 1000

        (remSeconds / 60).coerceAtLeast(1)

    } else 0

    Column(

        modifier = Modifier

            .width(135.dp)

            .clickable { onPlay() }

    ) {

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(14.dp))

                .background(colors.surface)

                .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp))

        ) {

            AsyncImage(

                model = item.cover,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    Icons.Default.Close,

                    contentDescription = "Remove",

                    tint = Color.White,

                    modifier = Modifier.size(12.dp)

                )

            }

            // Play Overlay

            Box(

                modifier = Modifier

                    .align(Alignment.Center)

                    .size(36.dp)

                    .clip(CircleShape)

                    .background(Color(0xBB000000)),

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    Icons.Default.PlayArrow,

                    contentDescription = "Play",

                    tint = Color.White,

                    modifier = Modifier.size(20.dp)

                )

            }

            // Bottom episode badge

            Box(

                modifier = Modifier

                    .align(Alignment.BottomStart)

                    .fillMaxWidth()

                    .background(Color(0xAA000000))

                    .padding(horizontal = 8.dp, vertical = 4.dp)

            ) {

                Text(

                    text = "Episode ${item.lastEpisode}",

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

            // Progress Bar

            LinearProgressIndicator(

                progress = { progressPct / 100f },

                modifier = Modifier

                    .align(Alignment.BottomCenter)

                    .fillMaxWidth()

                    .height(3.dp),

                color = colors.accent,

                trackColor = Color(0x40FFFFFF)

            )

        }

        Spacer(Modifier.height(5.dp))

        Text(

            text = item.displayTitle,

            fontSize = 12.sp,

            fontWeight = FontWeight.SemiBold,

            color = colors.textPrimary,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis

        )

        Text(

            text = if (isDone) "Completed" else if (leftMinutes > 0) "${leftMinutes}m left" else "Up next",

            fontSize = 11.sp,

            color = if (isDone) colors.accent else colors.textMuted

        )

    }

}

@Composable

fun ContinueMangaCard(

    item: MangaHistoryItem,

    onRead: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .width(135.dp)

            .clickable { onRead() }

    ) {

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(14.dp))

                .background(colors.surface)

                .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp))

        ) {

            AsyncImage(

                model = item.poster,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    Icons.Default.Close,

                    contentDescription = "Remove",

                    tint = Color.White,

                    modifier = Modifier.size(12.dp)

                )

            }

            // Bottom chapter badge

            Box(

                modifier = Modifier

                    .align(Alignment.BottomStart)

                    .fillMaxWidth()

                    .background(Color(0xAA000000))

                    .padding(horizontal = 8.dp, vertical = 4.dp)

            ) {

                Text(

                    text = "Ch. ${item.chapterNumber}",

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        }

        Spacer(Modifier.height(5.dp))

        Text(

            text = item.title,

            fontSize = 12.sp,

            fontWeight = FontWeight.SemiBold,

            color = colors.textPrimary,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis

        )

        Text(

            text = "Manga",

            fontSize = 11.sp,

            color = colors.textMuted

        )

    }

}

@Composable

fun ContinueNovelCard(

    item: NovelHistoryItem,

    onRead: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .width(135.dp)

            .clickable { onRead() }

    ) {

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(14.dp))

                .background(colors.surface)

                .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp))

        ) {

            AsyncImage(

                model = item.cover,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(

                    Icons.Default.Close,

                    contentDescription = "Remove",

                    tint = Color.White,

                    modifier = Modifier.size(12.dp)

                )

            }

            // Bottom chapter badge

            Box(

                modifier = Modifier

                    .align(Alignment.BottomStart)

                    .fillMaxWidth()

                    .background(Color(0xAA000000))

                    .padding(horizontal = 8.dp, vertical = 4.dp)

            ) {

                Text(

                    text = "Ch. ${item.lastChapterTitle.ifBlank { item.lastChapterSlug }}",

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        }

        Spacer(Modifier.height(5.dp))

        Text(

            text = item.title,

            fontSize = 12.sp,

            fontWeight = FontWeight.SemiBold,

            color = colors.textPrimary,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis

        )

        Text(

            text = "Light Novel",

            fontSize = 11.sp,

            color = colors.textMuted

        )

    }

}

@Composable

fun CleanAnimeGridCard(

    item: WatchlistItem,

    onClick: () -> Unit,

    onIncrementProgress: () -> Unit,

    onChangeStatus: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(16.dp))

            .background(colors.surface)

            .border(1.dp, colors.glass.border, RoundedCornerShape(16.dp))

            .padding(8.dp)

    ) {

        // Poster with chips

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(12.dp))

                .clickable { onClick() }

        ) {

            AsyncImage(

                model = item.cover,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Score chip

            if (item.score != null && item.score > 0) {

                Row(

                    modifier = Modifier

                        .align(Alignment.TopStart)

                        .padding(6.dp)

                        .clip(CircleShape)

                        .background(Color(0xCC000000))

                        .padding(horizontal = 6.dp, vertical = 2.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))

                    Spacer(Modifier.width(3.dp))

                    Text(

                        text = "${item.score}%",

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Bold,

                        color = Color.White

                    )

                }

            }

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(13.dp))

            }

            // Progress pill

            Box(

                modifier = Modifier

                    .align(Alignment.BottomStart)

                    .padding(6.dp)

                    .clip(RoundedCornerShape(6.dp))

                    .background(Color(0xDD000000))

                    .padding(horizontal = 7.dp, vertical = 3.dp)

            ) {

                Text(

                    text = "Ep ${item.progressEpisode}${if (item.episodes != null) " / ${item.episodes}" else ""}",

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        }

        Spacer(Modifier.height(6.dp))

        // Title

        Text(

            text = item.title,

            fontSize = 13.sp,

            fontWeight = FontWeight.Bold,

            color = colors.textPrimary,

            maxLines = 2,

            overflow = TextOverflow.Ellipsis,

            lineHeight = 16.sp,

            modifier = Modifier.clickable { onClick() }

        )

        Spacer(Modifier.height(8.dp))

        // Action row: Status selector + Quick Increment

        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            // Status Chip

            Box(

                modifier = Modifier

                    .weight(1f)

                    .clip(CircleShape)

                    .background(colors.surfaceElevated)

                    .border(1.dp, colors.glass.border, CircleShape)

                    .clickable { onChangeStatus() }

                    .padding(horizontal = 8.dp, vertical = 5.dp),

                contentAlignment = Alignment.Center

            ) {

                Text(

                    text = item.watchlistStatus.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },

                    fontSize = 10.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = colors.textPrimary,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis

                )

            }

            Spacer(Modifier.width(6.dp))

            // +1 Quick Increment button

            Box(

                modifier = Modifier

                    .size(28.dp)

                    .clip(CircleShape)

                    .background(colors.textPrimary)

                    .clickable { onIncrementProgress() },

                contentAlignment = Alignment.Center

            ) {

                Text(

                    text = "+1",

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Black,

                    color = colors.bg

                )

            }

        }

    }

}

@Composable

fun CleanMangaBookmarkCard(

    item: MangaBookmarkItem,

    onClick: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(16.dp))

            .background(colors.surface)

            .border(1.dp, colors.glass.border, RoundedCornerShape(16.dp))

            .padding(8.dp)

    ) {

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(12.dp))

                .clickable { onClick() }

        ) {

            AsyncImage(

                model = item.poster,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(13.dp))

            }

            // Latest Chapter badge

            if (!item.latestChapter.isNullOrBlank()) {

                Box(

                    modifier = Modifier

                        .align(Alignment.BottomStart)

                        .padding(6.dp)

                        .clip(RoundedCornerShape(6.dp))

                        .background(Color(0xDD000000))

                        .padding(horizontal = 7.dp, vertical = 3.dp)

                ) {

                    Text(

                        text = item.latestChapter,

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Bold,

                        color = Color.White

                    )

                }

            }

        }

        Spacer(Modifier.height(6.dp))

        Text(

            text = item.title,

            fontSize = 13.sp,

            fontWeight = FontWeight.Bold,

            color = colors.textPrimary,

            maxLines = 2,

            overflow = TextOverflow.Ellipsis,

            lineHeight = 16.sp

        )

    }

}

@Composable

fun CleanNovelCard(

    item: NovelHistoryItem,

    onClick: () -> Unit,

    onRead: () -> Unit,

    onRemove: () -> Unit

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(16.dp))

            .background(colors.surface)

            .border(1.dp, colors.glass.border, RoundedCornerShape(16.dp))

            .padding(8.dp)

    ) {

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .aspectRatio(2f / 3f)

                .clip(RoundedCornerShape(12.dp))

                .clickable { onClick() }

        ) {

            AsyncImage(

                model = item.cover,

                contentDescription = item.title,

                modifier = Modifier.fillMaxSize(),

                contentScale = ContentScale.Crop

            )

            // Remove button

            Box(

                modifier = Modifier

                    .align(Alignment.TopEnd)

                    .padding(6.dp)

                    .size(24.dp)

                    .clip(CircleShape)

                    .background(Color(0xCC000000))

                    .clickable { onRemove() },

                contentAlignment = Alignment.Center

            ) {

                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(13.dp))

            }

            Box(

                modifier = Modifier

                    .align(Alignment.BottomStart)

                    .padding(6.dp)

                    .clip(RoundedCornerShape(6.dp))

                    .background(Color(0xDD000000))

                    .padding(horizontal = 7.dp, vertical = 3.dp)

            ) {

                Text(

                    text = "Ch. ${item.lastChapterTitle.ifBlank { item.lastChapterSlug }}",

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White

                )

            }

        }

        Spacer(Modifier.height(6.dp))

        Text(

            text = item.title,

            fontSize = 13.sp,

            fontWeight = FontWeight.Bold,

            color = colors.textPrimary,

            maxLines = 2,

            overflow = TextOverflow.Ellipsis,

            lineHeight = 16.sp

        )

    }

}

@Composable

fun EmptyLibraryState(

    title: String,

    subtitle: String

) {

    val colors = LocalAppColors.current

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(20.dp))

            .background(colors.surface)

            .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))

            .padding(32.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        Icon(

            Icons.Default.BookmarkBorder,

            contentDescription = null,

            tint = colors.textMuted,

            modifier = Modifier.size(36.dp)

        )

        Spacer(Modifier.height(10.dp))

        Text(

            text = title,

            fontSize = 15.sp,

            fontWeight = FontWeight.Bold,

            color = colors.textPrimary

        )

        Spacer(Modifier.height(4.dp))

        Text(

            text = subtitle,

            fontSize = 12.sp,

            color = colors.textMuted,

            textAlign = TextAlign.Center

        )

    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistStatusSelectorDialog(
    title: String,
    currentStatus: WatchlistStatus,
    currentEpisode: Int,
    totalEpisodes: Int?,
    initialCustomTags: List<String> = emptyList(),
    isNewItem: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (status: WatchlistStatus, episode: Int, customTags: List<String>) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var episodeCount by remember { mutableIntStateOf(currentEpisode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRemove != null) {
                        OutlinedButton(
                            onClick = onRemove,
                            shape = CircleShape
                        ) {
                            Text("Remove", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { onSave(selectedStatus, episodeCount, initialCustomTags) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary)
                    ) {
                        Text("Save", color = colors.bg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Episode Progress: $episodeCount${if (totalEpisodes != null) " / $totalEpisodes" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.glass.border, CircleShape)
                            .clickable { if (episodeCount > 0) episodeCount-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.glass.border, CircleShape)
                            .clickable {
                                if (totalEpisodes == null || episodeCount < totalEpisodes) episodeCount++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
            }

            HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

            listOf(
                WatchlistStatus.WATCHING to "Watching",
                WatchlistStatus.PLAN_TO_WATCH to "Plan to watch",
                WatchlistStatus.COMPLETED to "Completed",
                WatchlistStatus.REWATCHING to "Rewatching",
                WatchlistStatus.DROPPED to "Dropped"
            ).forEach { (st, label) ->
                val isSelected = selectedStatus == st
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                        .border(1.dp, if (isSelected) colors.textPrimary else colors.glass.border, RoundedCornerShape(12.dp))
                        .clickable { selectedStatus = st }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
