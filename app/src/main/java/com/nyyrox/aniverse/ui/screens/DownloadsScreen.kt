package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nyyrox.aniverse.ui.components.iosGlass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.download.DownloadStatus
import com.nyyrox.aniverse.data.download.DownloadTaskInfo
import com.nyyrox.aniverse.data.download.DownloadType
import com.nyyrox.aniverse.data.download.DownloadedAnime
import com.nyyrox.aniverse.data.download.DownloadedAnimeEpisode
import com.nyyrox.aniverse.data.download.DownloadedManga
import com.nyyrox.aniverse.data.download.DownloadedNovel
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel

data class DeleteConfirmationTarget(
    val title: String,
    val subtitle: String,
    val itemType: String,
    val sizeFormatted: String? = null,
    val onConfirm: () -> Unit
)

@Composable
fun DownloadsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onPlayAnimeEpisode: (animeId: Int, episode: Int) -> Unit = { _, _ -> },
    onReadNovelChapter: (novelSlug: String, chapterSlug: String) -> Unit = { _, _ -> },
    onReadMangaChapter: (mangaId: String, chapterId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Anime, 1 = Manga, 2 = Novels
    val downloadedAnime by vm.downloadManager.downloadedAnime.collectAsState()
    val downloadedManga by vm.downloadManager.downloadedManga.collectAsState()
    val downloadedNovels by vm.downloadManager.downloadedNovels.collectAsState()
    val allTasks by vm.downloadManager.tasks.collectAsState()
    val activeTasks = remember(allTasks) {
        allTasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    }
    val failedTasks = remember(allTasks) {
        allTasks.filter { it.status == DownloadStatus.FAILED }
    }
    var retrySheetTask by remember { mutableStateOf<DownloadTaskInfo?>(null) }
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmationTarget?>(null) }
    var isDownloadingExpanded by remember { mutableStateOf(true) }
    var isFailedExpanded by remember { mutableStateOf(false) }
    var showDownloadSettingsSheet by remember { mutableStateOf(false) }

    val totalStorageBytes = remember(downloadedAnime, downloadedNovels, downloadedManga) {
        val aSize = downloadedAnime.sumOf { it.totalSizeBytes }
        val nSize = downloadedNovels.sumOf { it.totalSizeBytes }
        val mSize = downloadedManga.sumOf { it.totalSizeBytes }
        aSize + nSize + mSize
    }

    val formattedTotalStorage = remember(totalStorageBytes) {
        formatBytes(totalStorageBytes)
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
        ) {
            // Header with Download Logo Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Prominent Download Logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloads",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Library",
                        color = OttTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$formattedTotalStorage used across device",
                        color = OttTextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), OttPillShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedTotalStorage,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { showDownloadSettingsSheet = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Download Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Active Downloads Banner
            if (activeTasks.isNotEmpty()) {
                val avgProgress = remember(activeTasks) {
                    if (activeTasks.isNotEmpty()) activeTasks.map { it.progress }.average().toFloat() else 0f
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(OttCardShape)
                        .background(Color(0x12FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), OttCardShape)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDownloadingExpanded = !isDownloadingExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Downloading,
                                contentDescription = null,
                                tint = OttPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Downloading (${activeTasks.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isDownloadingExpanded) {
                                LinearProgressIndicator(
                                    progress = { avgProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = OttPrimary,
                                    trackColor = Color(0x28FFFFFF)
                                )
                                Text(
                                    text = "${(avgProgress * 100).toInt()}%",
                                    color = OttTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (isDownloadingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isDownloadingExpanded) "Minimize" else "Expand",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isDownloadingExpanded) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            activeTasks.forEach { task ->
                                ActiveDownloadTaskRow(
                                    task = task,
                                    onCancel = { vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Failed Downloads Banner / Issue Resolution Card
            if (failedTasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(OttCardShape)
                        .background(Color(0x18F44336))
                        .border(1.dp, Color(0x45F44336), OttCardShape)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFailedExpanded = !isFailedExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Failed Downloads (${failedTasks.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = if (isFailedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isFailedExpanded) "Minimize" else "Expand",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isFailedExpanded) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            failedTasks.forEach { task ->
                                FailedDownloadTaskRow(
                                    task = task,
                                    onRetrySameServer = {
                                        if (task.type == DownloadType.ANIME) {
                                            val ep = task.chapterId.toIntOrNull() ?: 1
                                            val retryAnime = com.nyyrox.aniverse.data.Anime(
                                                id = task.itemId.toIntOrNull() ?: 0,
                                                title = task.itemTitle.ifBlank { task.title },
                                                cover = task.cover ?: "",
                                                banner = task.cover ?: "",
                                                description = ""
                                            )
                                            vm.downloadManager.retryAnimeDownload(
                                                anime = retryAnime,
                                                episode = ep,
                                                provider = null,
                                                episodeTitle = task.title,
                                                quality = task.quality ?: "Auto HD",
                                                audio = task.audio ?: "Default"
                                            )
                                        } else {
                                            vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId)
                                        }
                                    },
                                    onChangeServer = {
                                        if (task.type == DownloadType.ANIME) {
                                            retrySheetTask = task
                                        } else {
                                            vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId)
                                        }
                                    },
                                    onDismiss = {
                                        vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId)
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Tabs (Anime vs Manga vs Novels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(OttPillShape)
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x1EFFFFFF), OttPillShape)
                    .padding(4.dp)
            ) {
                // Tab 0: Anime
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(OttPillShape)
                        .background(if (selectedTab == 0) Color.White else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.Black else OttTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Anime (${downloadedAnime.size})",
                            color = if (selectedTab == 0) Color.Black else OttTextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab 1: Manga
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(OttPillShape)
                        .background(if (selectedTab == 1) Color.White else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.Black else OttTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Manga (${downloadedManga.size})",
                            color = if (selectedTab == 1) Color.Black else OttTextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab 2: Novels
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(OttPillShape)
                        .background(if (selectedTab == 2) Color.White else Color.Transparent)
                        .clickable { selectedTab = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (selectedTab == 2) Color.Black else OttTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Novels (${downloadedNovels.size})",
                            color = if (selectedTab == 2) Color.Black else OttTextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Content List
            when (selectedTab) {
                0 -> {
                    // Anime List
                    if (downloadedAnime.isEmpty()) {
                        EmptyDownloadsView(type = "anime")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(downloadedAnime, key = { it.animeId }) { anime ->
                                DownloadedAnimeCard(
                                    anime = anime,
                                    vm = vm,
                                    onPlayEpisode = { ep -> onPlayAnimeEpisode(anime.animeId, ep) },
                                    onDeleteRequest = { deleteConfirmation = it }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Manga List
                    if (downloadedManga.isEmpty()) {
                        EmptyDownloadsView(type = "manga")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(downloadedManga, key = { it.mangaId }) { manga ->
                                DownloadedMangaCard(
                                    manga = manga,
                                    vm = vm,
                                    onReadChapter = { chId -> onReadMangaChapter(manga.mangaId, chId) },
                                    onDeleteRequest = { deleteConfirmation = it }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Novels List
                    if (downloadedNovels.isEmpty()) {
                        EmptyDownloadsView(type = "novels")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(downloadedNovels, key = { it.novelSlug }) { novel ->
                                DownloadedNovelCard(
                                    novel = novel,
                                    vm = vm,
                                    onReadChapter = { chSlug -> onReadNovelChapter(novel.novelSlug, chSlug) },
                                    onDeleteRequest = { deleteConfirmation = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    retrySheetTask?.let { task ->
        val retryAnime = remember(task) {
            com.nyyrox.aniverse.data.Anime(
                id = task.itemId.toIntOrNull() ?: 0,
                title = task.itemTitle.ifBlank { task.title },
                cover = task.cover ?: ""
            )
        }
        val ep = task.chapterId.toIntOrNull() ?: 1
        val initialProv = remember(task) {
            com.nyyrox.aniverse.data.ALL_PROVIDERS.find { it.id == task.providerId }
        }

        com.nyyrox.aniverse.ui.components.DownloadAnimeOptionsSheet(
            anime = retryAnime,
            episode = ep,
            episodeTitle = task.title,
            initialProvider = initialProv,
            onDismiss = { retrySheetTask = null },
            onStartDownload = { prov, quality, audio ->
                vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId)
                vm.downloadAnimeEpisode(
                    anime = retryAnime,
                    episode = ep,
                    provider = prov,
                    episodeTitle = task.title,
                    quality = quality,
                    audio = audio
                )
                retrySheetTask = null
            }
        )
    }

    deleteConfirmation?.let { target ->
        IosDeleteConfirmationDialog(
            target = target,
            onDismiss = { deleteConfirmation = null }
        )
    }

    if (showDownloadSettingsSheet) {
        com.nyyrox.aniverse.ui.components.DownloadSettingsSheet(
            onDismiss = { showDownloadSettingsSheet = false }
        )
    }
}

@Composable
private fun DownloadedAnimeCard(
    anime: DownloadedAnime,
    vm: MainViewModel,
    onPlayEpisode: (episode: Int) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val episodes = remember(expanded) {
        if (expanded) vm.downloadManager.getDownloadedEpisodesForAnime(anime.animeId) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), OttCardShape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x20FFFFFF))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = anime.title,
                    color = OttTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${anime.episodesCount} episodes • ${formatBytes(anime.totalSizeBytes)}",
                    color = OttTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Offline Video Storage",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                onDeleteRequest(
                    DeleteConfirmationTarget(
                        title = "Delete ${anime.title}?",
                        subtitle = "This will permanently delete all ${anime.episodesCount} downloaded episode(s) and free up storage on your device.",
                        itemType = "Anime",
                        sizeFormatted = formatBytes(anime.totalSizeBytes),
                        onConfirm = { vm.deleteDownloadedAnime(anime.animeId) }
                    )
                )
            }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF6B6B)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x18FFFFFF))
                )
                Spacer(Modifier.height(8.dp))
                episodes.forEach { ep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(0.8.dp, Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Episode ${ep.episode}${if (!ep.episodeTitle.isNullOrBlank()) ": ${ep.episodeTitle}" else ""}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${formatBytes(ep.sizeBytes)}${if (ep.providerName.isNotBlank()) " • ${ep.providerName}" else ""}",
                                color = OttTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onPlayEpisode(ep.episode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    onDeleteRequest(
                                        DeleteConfirmationTarget(
                                            title = "Delete Episode ${ep.episode}?",
                                            subtitle = "Remove '${ep.episodeTitle?.ifBlank { "Episode ${ep.episode}" } ?: "Episode ${ep.episode}"}' from offline storage?",
                                            itemType = "Episode",
                                            sizeFormatted = formatBytes(ep.sizeBytes),
                                            onConfirm = { vm.deleteDownloadedAnimeEpisode(anime.animeId, ep.episode) }
                                        )
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadedNovelCard(
    novel: DownloadedNovel,
    vm: MainViewModel,
    onReadChapter: (chapterSlug: String) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chapters = remember(expanded) {
        if (expanded) vm.downloadManager.getDownloadedChaptersForNovel(novel.novelSlug) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), OttCardShape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = novel.cover,
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x20FFFFFF))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.title,
                    color = OttTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${novel.chaptersCount} chapters • ${formatBytes(novel.totalSizeBytes)}",
                    color = OttTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Source: ${novel.source.replaceFirstChar { it.uppercase() }}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                onDeleteRequest(
                    DeleteConfirmationTarget(
                        title = "Delete ${novel.title}?",
                        subtitle = "This will permanently remove all ${novel.chaptersCount} downloaded chapter(s) from offline storage.",
                        itemType = "Novel",
                        sizeFormatted = formatBytes(novel.totalSizeBytes),
                        onConfirm = { vm.downloadManager.deleteDownloadedNovel(novel.novelSlug) }
                    )
                )
            }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF6B6B)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x18FFFFFF))
                )
                Spacer(Modifier.height(8.dp))
                chapters.forEach { ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReadChapter(ch.chapterSlug) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ch.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatBytes(ch.sizeBytes),
                                color = OttTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                onDeleteRequest(
                                    DeleteConfirmationTarget(
                                        title = "Delete Chapter?",
                                        subtitle = "Remove '${ch.title}' from offline reading?",
                                        itemType = "Chapter",
                                        sizeFormatted = formatBytes(ch.sizeBytes),
                                        onConfirm = { vm.downloadManager.deleteDownloadedNovelChapter(novel.novelSlug, ch.chapterSlug) }
                                    )
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedMangaCard(
    manga: DownloadedManga,
    vm: MainViewModel,
    onReadChapter: (chapterId: String) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chapters = remember(expanded) {
        if (expanded) vm.downloadManager.getDownloadedChaptersForManga(manga.mangaId) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OttCardShape)
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), OttCardShape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = manga.poster,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x20FFFFFF))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    color = OttTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${manga.chaptersCount} chapters • ${formatBytes(manga.totalSizeBytes)}",
                    color = OttTextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                onDeleteRequest(
                    DeleteConfirmationTarget(
                        title = "Delete ${manga.title}?",
                        subtitle = "This will permanently remove all ${manga.chaptersCount} downloaded chapter(s) from offline storage.",
                        itemType = "Manga",
                        sizeFormatted = formatBytes(manga.totalSizeBytes),
                        onConfirm = { vm.downloadManager.deleteDownloadedManga(manga.mangaId) }
                    )
                )
            }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF6B6B)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x18FFFFFF))
                )
                Spacer(Modifier.height(8.dp))
                chapters.forEach { ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReadChapter(ch.chapterId) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chapter ${ch.chapterNumber}${if (!ch.chapterTitle.isNullOrBlank()) ": ${ch.chapterTitle}" else ""}",
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ch.pageCount} pages • ${formatBytes(ch.sizeBytes)}",
                                color = OttTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                onDeleteRequest(
                                    DeleteConfirmationTarget(
                                        title = "Delete Chapter ${ch.chapterNumber}?",
                                        subtitle = "Remove '${ch.chapterTitle?.ifBlank { "Chapter ${ch.chapterNumber}" } ?: "Chapter ${ch.chapterNumber}"}' from offline reading?",
                                        itemType = "Chapter",
                                        sizeFormatted = formatBytes(ch.sizeBytes),
                                        onConfirm = { vm.downloadManager.deleteDownloadedMangaChapter(manga.mangaId, ch.chapterId) }
                                    )
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadTaskRow(
    task: DownloadTaskInfo,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x14000000))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!task.cover.isNullOrBlank()) {
                AsyncImage(
                    model = task.cover,
                    contentDescription = task.itemTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 36.dp, height = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (task.itemTitle.isNotBlank()) "${task.itemTitle} • " else ""}${task.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusText = if (task.type == DownloadType.ANIME) {
                        val sizeInfo = if (task.totalBytes > 0L) " • ${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}" else if (task.downloadedBytes > 0L) " • ${formatBytes(task.downloadedBytes)}" else ""
                        if (task.speed.isNotBlank()) "${task.speed}$sizeInfo" else "Downloading...$sizeInfo"
                    } else if (task.type == DownloadType.MANGA && task.totalCount > 0) {
                        "${task.downloadedCount}/${task.totalCount} pages"
                    } else if (task.status == DownloadStatus.QUEUED) {
                        "Queued..."
                    } else {
                        "Downloading..."
                    }
                    Text(
                        text = statusText,
                        color = OttTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${task.percentage}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { task.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = Color.White,
            trackColor = Color(0x22FFFFFF)
        )
    }
}

@Composable
private fun FailedDownloadTaskRow(
    task: DownloadTaskInfo,
    onRetrySameServer: () -> Unit,
    onChangeServer: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x22000000))
            .border(1.dp, Color(0x35F44336), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!task.cover.isNullOrBlank()) {
                AsyncImage(
                    model = task.cover,
                    contentDescription = task.itemTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 36.dp, height = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (task.itemTitle.isNotBlank()) "${task.itemTitle} • " else ""}${task.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = task.error ?: "Download stopped. Tap Retry to resume.",
                    color = Color(0xFFFF8A80),
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onChangeServer,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Change Server",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRetrySameServer,
                colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
                shape = OttPillShape,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Retry",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadsView(type: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x0CFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x35FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "No Downloaded ${type.replaceFirstChar { it.uppercase() }}",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Episodes or chapters you save offline will appear here for viewing anytime without an internet connection.",
                color = OttTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}

@Composable
fun IosDeleteConfirmationDialog(
    target: DeleteConfirmationTarget,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x75000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.86f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .iosGlass(
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color(0xE81C1D24),
                        borderWidth = 1.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FF453A))
                        .border(1.dp, Color(0x40FF453A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = target.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = target.subtitle,
                    fontSize = 13.sp,
                    color = Color(0xCCFFFFFF),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                if (!target.sizeFormatted.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Frees up ${target.sizeFormatted}",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color(0x28FFFFFF))
                )

                // iOS 2-action buttons row (Cancel & Delete)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xEEFFFFFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.6.dp)
                            .fillMaxSize()
                            .background(Color(0x28FFFFFF))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable {
                                onDismiss()
                                target.onConfirm()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete",
                            color = Color(0xFFFF453A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
