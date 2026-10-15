package com.nyyrox.aniverse.ui.screens

import android.os.Build
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.download.DownloadStatus
import com.nyyrox.aniverse.data.download.DownloadTaskInfo
import com.nyyrox.aniverse.data.download.DownloadType
import com.nyyrox.aniverse.data.download.DownloadedAnime
import com.nyyrox.aniverse.data.download.DownloadedManga
import com.nyyrox.aniverse.data.download.DownloadedNovel
import com.nyyrox.aniverse.ui.components.iosGlass
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

data class DeleteConfirmationTarget(
    val title: String,
    val subtitle: String,
    val itemType: String,
    val sizeFormatted: String? = null,
    val onConfirm: () -> Unit
)

private val AnimeAccent = OttPrimary
private val MangaAccent = Color(0xFFF59E0B)
private val NovelAccent = Color(0xFF60A5FA)
private val DangerRed = Color(0xFFFF6B6B)
private val ItemPad = Modifier.padding(horizontal = 16.dp)

/**
 * Follows Settings > Liquid Glassmorphism (AGSL glass needs API 33+), same rule as the detail page.
 * Glass is kept for the small header controls; cards are cheap translucent surfaces (on) or solid AMOLED (off).
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val failedTasks = remember(allTasks) { allTasks.filter { it.status == DownloadStatus.FAILED } }
    var retrySheetTask by remember { mutableStateOf<DownloadTaskInfo?>(null) }
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmationTarget?>(null) }
    var isDownloadingExpanded by remember { mutableStateOf(true) }
    var isFailedExpanded by remember { mutableStateOf(false) }
    var showDownloadSettingsSheet by remember { mutableStateOf(false) }

    val animeBytes = remember(downloadedAnime) { downloadedAnime.sumOf { it.totalSizeBytes } }
    val mangaBytes = remember(downloadedManga) { downloadedManga.sumOf { it.totalSizeBytes } }
    val novelBytes = remember(downloadedNovels) { downloadedNovels.sumOf { it.totalSizeBytes } }
    val totalItems = downloadedAnime.size + downloadedManga.size + downloadedNovels.size

    val glassOn = LocalThemeSettings.current.isGlassmorphismEnabled && Build.VERSION.SDK_INT >= 33
    val glassLayer = rememberPrismalGlassLayer()
    val backdrop = if (glassOn) glassLayer else null // null = flat AMOLED controls
    val context = LocalContext.current
    val ambient = when (selectedTab) {
        0 -> downloadedAnime.firstOrNull()?.cover
        1 -> downloadedManga.firstOrNull()?.poster
        else -> downloadedNovels.firstOrNull()?.cover
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg)
    ) {
        if (glassOn) {
            // What the glass controls refract: accent wash + blurred artwork of the current tab.
            Box(Modifier.fillMaxSize().prismalGlassLayer(glassLayer)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(OttPrimary.copy(alpha = 0.35f), OttSecondary.copy(alpha = 0.18f), Color.Transparent)))
                )
                if (!ambient.isNullOrBlank()) {
                    AsyncImage(
                        model = ambient,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(50.dp)
                            .graphicsLayer {
                                alpha = 0.55f
                                scaleX = 1.3f
                                scaleY = 1.3f
                            }
                    )
                }
            }
            // Keeps text readable over the art.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(OttBg.copy(alpha = 0.55f), OttBg.copy(alpha = 0.94f))))
            )
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerGlassButton(backdrop, onClick = onBack, height = 40.dp, circle = true) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OttTextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Offline Library",
                        color = OttTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = if (totalItems == 0) "Nothing saved yet" else "$totalItems title${if (totalItems == 1) "" else "s"} saved on this device",
                        color = OttTextSecondary,
                        fontSize = 12.sp
                    )
                }
                PlayerGlassButton(backdrop, onClick = { showDownloadSettingsSheet = true }, height = 40.dp, circle = true) {
                    Icon(Icons.Default.Tune, contentDescription = "Download settings", tint = OttTextPrimary, modifier = Modifier.size(20.dp))
                }
            }

            // One scrolling list so banners never squeeze the library on small screens; tabs stay pinned.
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "storage") {
                    StorageCard(glassOn, animeBytes, mangaBytes, novelBytes, ItemPad)
                }

                if (activeTasks.isNotEmpty()) {
                    item(key = "active") {
                        val avg = activeTasks.map { it.progress }.average().toFloat().coerceIn(0f, 1f)
                        TaskGroupCard(
                            glass = glassOn,
                            title = "Downloading (${activeTasks.size})",
                            icon = Icons.Default.Downloading,
                            accent = OttPrimary,
                            expanded = isDownloadingExpanded,
                            onToggle = { isDownloadingExpanded = !isDownloadingExpanded },
                            modifier = ItemPad,
                            collapsedHint = {
                                ProgressBar(avg, Modifier.width(70.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${(avg * 100).toInt()}%", color = OttTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                            }
                        ) {
                            activeTasks.forEach { task ->
                                ActiveDownloadTaskRow(
                                    task = task,
                                    onCancel = { vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId) }
                                )
                            }
                        }
                    }
                }

                if (failedTasks.isNotEmpty()) {
                    item(key = "failed") {
                        TaskGroupCard(
                            glass = glassOn,
                            title = "Failed Downloads (${failedTasks.size})",
                            icon = Icons.Default.ErrorOutline,
                            accent = Color(0xFFFF5252),
                            expanded = isFailedExpanded,
                            onToggle = { isFailedExpanded = !isFailedExpanded },
                            tint = if (glassOn) Color(0x18F44336) else OttPanelHigh,
                            modifier = ItemPad
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
                                        if (task.type == DownloadType.ANIME) retrySheetTask = task
                                        else vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId)
                                    },
                                    onDismiss = { vm.downloadManager.cancelTask(task.type, task.itemId, task.chapterId) }
                                )
                            }
                        }
                    }
                }

                stickyHeader(key = "tabs") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(OttBg.copy(alpha = 0.96f), OttBg.copy(alpha = 0.96f), Color.Transparent)))
                            .padding(top = 4.dp, bottom = 10.dp)
                    ) {
                        DownloadTabs(
                            glass = glassOn,
                            selected = selectedTab,
                            tabs = listOf(
                                Triple(Icons.Default.Movie, "Anime", downloadedAnime.size),
                                Triple(Icons.Default.Book, "Manga", downloadedManga.size),
                                Triple(Icons.Default.MenuBook, "Novels", downloadedNovels.size)
                            ),
                            onSelect = {
                                if (it != selectedTab) com.nyyrox.aniverse.util.AppHaptics.tick(context)
                                selectedTab = it
                            },
                            modifier = ItemPad
                        )
                    }
                }

                when (selectedTab) {
                    0 -> if (downloadedAnime.isEmpty()) {
                        item(key = "empty_anime") { EmptyDownloadsView("anime", glassOn) }
                    } else {
                        items(downloadedAnime, key = { "anime_${it.animeId}" }) { anime ->
                            DownloadedAnimeCard(
                                anime = anime,
                                vm = vm,
                                glass = glassOn,
                                onPlayEpisode = { ep -> onPlayAnimeEpisode(anime.animeId, ep) },
                                onDeleteRequest = { deleteConfirmation = it },
                                modifier = ItemPad
                            )
                        }
                    }
                    1 -> if (downloadedManga.isEmpty()) {
                        item(key = "empty_manga") { EmptyDownloadsView("manga", glassOn) }
                    } else {
                        items(downloadedManga, key = { "manga_${it.mangaId}" }) { manga ->
                            DownloadedMangaCard(
                                manga = manga,
                                vm = vm,
                                glass = glassOn,
                                onReadChapter = { chId -> onReadMangaChapter(manga.mangaId, chId) },
                                onDeleteRequest = { deleteConfirmation = it },
                                modifier = ItemPad
                            )
                        }
                    }
                    else -> if (downloadedNovels.isEmpty()) {
                        item(key = "empty_novels") { EmptyDownloadsView("novels", glassOn) }
                    } else {
                        items(downloadedNovels, key = { "novel_${it.novelSlug}" }) { novel ->
                            DownloadedNovelCard(
                                novel = novel,
                                vm = vm,
                                glass = glassOn,
                                onReadChapter = { chSlug -> onReadNovelChapter(novel.novelSlug, chSlug) },
                                onDeleteRequest = { deleteConfirmation = it },
                                modifier = ItemPad
                            )
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
        IosDeleteConfirmationDialog(target = target, onDismiss = { deleteConfirmation = null })
    }

    if (showDownloadSettingsSheet) {
        com.nyyrox.aniverse.ui.components.DownloadSettingsSheet(
            onDismiss = { showDownloadSettingsSheet = false }
        )
    }
}

// ── Shared surfaces ─────────────────────────────────────────────────────────────

/** Glass on: faint translucent fill + lit rim. Glass off: solid AMOLED card + flat hairline. */
private fun Modifier.surface(glass: Boolean, shape: Shape = OttCardShape, tint: Color? = null): Modifier = this
    .clip(shape)
    .background(tint ?: if (glass) OttInk(0.08f) else OttPanelHigh)
    .border(
        1.dp,
        if (glass) Brush.verticalGradient(listOf(OttInk(0.20f), OttInk(0.05f))) else SolidColor(OttInk(0.10f)),
        shape
    )

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(5.dp).clip(CircleShape).background(OttInk(0.13f))) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(listOf(OttPrimary, OttTextPrimary)))
        )
    }
}

@Composable
private fun StorageCard(glass: Boolean, anime: Long, manga: Long, novels: Long, modifier: Modifier = Modifier) {
    val total = anime + manga + novels
    val parts = listOf(
        Triple("Anime", anime, AnimeAccent),
        Triple("Manga", manga, MangaAccent),
        Triple("Novels", novels, NovelAccent)
    )
    Column(modifier.fillMaxWidth().surface(glass).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("STORAGE USED", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFF8E94A3))
                Text(formatBytes(total), color = OttTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(OttInk(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(OttInk(0.10f)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            parts.forEach { (_, size, color) ->
                if (size > 0) Box(Modifier.weight(size.toFloat()).fillMaxHeight().background(color))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { (label, size, color) ->
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(5.dp))
                    Text("$label ${formatBytes(size)}", color = OttTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Collapsible card for the Downloading / Failed groups. */
@Composable
private fun TaskGroupCard(
    glass: Boolean,
    title: String,
    icon: ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    collapsedHint: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().animateContentSize().surface(glass, tint = tint).padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = OttTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!expanded) collapsedHint()
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Minimize" else "Expand",
                tint = OttInk(0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/** Segmented control with a sliding pill. */
@Composable
private fun DownloadTabs(
    glass: Boolean,
    selected: Int,
    tabs: List<Triple<ImageVector, String, Int>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.8f, stiffness = 400f), label = "tabPos")
    Row(
        modifier
            .fillMaxWidth()
            .surface(glass, OttPillShape)
            .drawBehind {
                val pad = 4.dp.toPx()
                val w = (size.width - pad * 2) / tabs.size
                drawRoundRect(
                    color = OttTextPrimary,
                    topLeft = Offset(pad + pos * w, pad),
                    size = Size(w, size.height - pad * 2),
                    cornerRadius = CornerRadius(size.height)
                )
            }
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, (icon, label, count) ->
            val tint by animateColorAsState(if (i == selected) Color.Black else OttTextSecondary, tween(200), label = "tabTint")
            Row(
                Modifier
                    .weight(1f)
                    .clip(OttPillShape)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("$label ($count)", color = tint, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

// ── Library cards (one layout, three data types) ───────────────────────────────

@Composable
private fun LibraryItemCard(
    glass: Boolean,
    cover: String?,
    title: String,
    meta: String,
    badge: String,
    accent: Color,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    children: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().surface(glass).padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cover,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OttInk(0.13f))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = OttTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(meta, color = OttTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.clip(OttPillShape).background(accent.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(5.dp))
                    Text(badge, color = accent, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = OttInk(0.8f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(OttInk(0.09f)))
                Spacer(Modifier.height(8.dp))
                // Composed only while expanded, so children are read lazily; keyed by the parent's counts on the caller side.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = children)
            }
        }
    }
}

/** Episode / chapter row: tap anywhere to open, trash to delete. */
@Composable
private fun ChildRow(
    title: String,
    subtitle: String,
    action: String,
    actionIcon: ImageVector,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OttInk(0.05f))
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = OttTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = OttTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.padding(horizontal = 6.dp).clip(OttPillShape).background(OttTextPrimary).padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(actionIcon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(action, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DownloadedAnimeCard(
    anime: DownloadedAnime,
    vm: MainViewModel,
    glass: Boolean,
    onPlayEpisode: (episode: Int) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryItemCard(
        glass = glass,
        cover = anime.cover,
        title = anime.title,
        meta = "${anime.episodesCount} episodes • ${formatBytes(anime.totalSizeBytes)}",
        badge = "Offline video",
        accent = AnimeAccent,
        modifier = modifier,
        onDelete = {
            onDeleteRequest(
                DeleteConfirmationTarget(
                    title = "Delete ${anime.title}?",
                    subtitle = "This will permanently delete all ${anime.episodesCount} downloaded episode(s) and free up storage on your device.",
                    itemType = "Anime",
                    sizeFormatted = formatBytes(anime.totalSizeBytes),
                    onConfirm = { vm.deleteDownloadedAnime(anime.animeId) }
                )
            )
        }
    ) {
        // Keyed on the counts: deleting an episode refreshes the open list instead of leaving a stale row.
        val episodes = remember(anime.animeId, anime.episodesCount, anime.totalSizeBytes) {
            vm.downloadManager.getDownloadedEpisodesForAnime(anime.animeId)
        }
        episodes.forEach { ep ->
            ChildRow(
                title = "Episode ${ep.episode}${if (!ep.episodeTitle.isNullOrBlank()) ": ${ep.episodeTitle}" else ""}",
                subtitle = "${formatBytes(ep.sizeBytes)}${if (ep.providerName.isNotBlank()) " • ${ep.providerName}" else ""}",
                action = "Play",
                actionIcon = Icons.Default.PlayArrow,
                onOpen = { onPlayEpisode(ep.episode) },
                onDelete = {
                    onDeleteRequest(
                        DeleteConfirmationTarget(
                            title = "Delete Episode ${ep.episode}?",
                            subtitle = "Remove '${ep.episodeTitle?.ifBlank { "Episode ${ep.episode}" } ?: "Episode ${ep.episode}"}' from offline storage?",
                            itemType = "Episode",
                            sizeFormatted = formatBytes(ep.sizeBytes),
                            onConfirm = { vm.deleteDownloadedAnimeEpisode(anime.animeId, ep.episode) }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun DownloadedNovelCard(
    novel: DownloadedNovel,
    vm: MainViewModel,
    glass: Boolean,
    onReadChapter: (chapterSlug: String) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryItemCard(
        glass = glass,
        cover = novel.cover,
        title = novel.title,
        meta = "${novel.chaptersCount} chapters • ${formatBytes(novel.totalSizeBytes)}",
        badge = novel.source.replaceFirstChar { it.uppercase() },
        accent = NovelAccent,
        modifier = modifier,
        onDelete = {
            onDeleteRequest(
                DeleteConfirmationTarget(
                    title = "Delete ${novel.title}?",
                    subtitle = "This will permanently remove all ${novel.chaptersCount} downloaded chapter(s) from offline storage.",
                    itemType = "Novel",
                    sizeFormatted = formatBytes(novel.totalSizeBytes),
                    onConfirm = { vm.downloadManager.deleteDownloadedNovel(novel.novelSlug) }
                )
            )
        }
    ) {
        val chapters = remember(novel.novelSlug, novel.chaptersCount, novel.totalSizeBytes) {
            vm.downloadManager.getDownloadedChaptersForNovel(novel.novelSlug)
        }
        chapters.forEach { ch ->
            ChildRow(
                title = ch.title,
                subtitle = formatBytes(ch.sizeBytes),
                action = "Read",
                actionIcon = Icons.Default.MenuBook,
                onOpen = { onReadChapter(ch.chapterSlug) },
                onDelete = {
                    onDeleteRequest(
                        DeleteConfirmationTarget(
                            title = "Delete Chapter?",
                            subtitle = "Remove '${ch.title}' from offline reading?",
                            itemType = "Chapter",
                            sizeFormatted = formatBytes(ch.sizeBytes),
                            onConfirm = { vm.downloadManager.deleteDownloadedNovelChapter(novel.novelSlug, ch.chapterSlug) }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun DownloadedMangaCard(
    manga: DownloadedManga,
    vm: MainViewModel,
    glass: Boolean,
    onReadChapter: (chapterId: String) -> Unit,
    onDeleteRequest: (DeleteConfirmationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    LibraryItemCard(
        glass = glass,
        cover = manga.poster,
        title = manga.title,
        meta = "${manga.chaptersCount} chapters • ${formatBytes(manga.totalSizeBytes)}",
        badge = "Offline manga",
        accent = MangaAccent,
        modifier = modifier,
        onDelete = {
            onDeleteRequest(
                DeleteConfirmationTarget(
                    title = "Delete ${manga.title}?",
                    subtitle = "This will permanently remove all ${manga.chaptersCount} downloaded chapter(s) from offline storage.",
                    itemType = "Manga",
                    sizeFormatted = formatBytes(manga.totalSizeBytes),
                    onConfirm = { vm.downloadManager.deleteDownloadedManga(manga.mangaId) }
                )
            )
        }
    ) {
        val chapters = remember(manga.mangaId, manga.chaptersCount, manga.totalSizeBytes) {
            vm.downloadManager.getDownloadedChaptersForManga(manga.mangaId)
        }
        chapters.forEach { ch ->
            ChildRow(
                title = "Chapter ${ch.chapterNumber}${if (!ch.chapterTitle.isNullOrBlank()) ": ${ch.chapterTitle}" else ""}",
                subtitle = "${ch.pageCount} pages • ${formatBytes(ch.sizeBytes)}",
                action = "Read",
                actionIcon = Icons.Default.MenuBook,
                onOpen = { onReadChapter(ch.chapterId) },
                onDelete = {
                    onDeleteRequest(
                        DeleteConfirmationTarget(
                            title = "Delete Chapter ${ch.chapterNumber}?",
                            subtitle = "Remove '${ch.chapterTitle?.ifBlank { "Chapter ${ch.chapterNumber}" } ?: "Chapter ${ch.chapterNumber}"}' from offline reading?",
                            itemType = "Chapter",
                            sizeFormatted = formatBytes(ch.sizeBytes),
                            onConfirm = { vm.downloadManager.deleteDownloadedMangaChapter(manga.mangaId, ch.chapterId) }
                        )
                    )
                }
            )
        }
    }
}

// ── Task rows ───────────────────────────────────────────────────────────────────

private val TaskRowShape = RoundedCornerShape(14.dp)

@Composable
private fun ActiveDownloadTaskRow(task: DownloadTaskInfo, onCancel: () -> Unit) {
    val progress by animateFloatAsState(task.progress.coerceIn(0f, 1f), tween(400), label = "dlProgress")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TaskRowShape)
            .background(Color(0x26000000))
            .border(1.dp, OttInk(0.08f), TaskRowShape)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!task.cover.isNullOrBlank()) {
                AsyncImage(
                    model = task.cover,
                    contentDescription = task.itemTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 36.dp, height = 48.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${if (task.itemTitle.isNotBlank()) "${task.itemTitle} • " else ""}${task.title}",
                    color = OttTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(statusText, color = OttTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("${task.percentage}%", color = OttTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = OttInk(0.7f), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(progress, Modifier.fillMaxWidth())
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
        Modifier
            .fillMaxWidth()
            .clip(TaskRowShape)
            .background(Color(0x26000000))
            .border(1.dp, Color(0x35F44336), TaskRowShape)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!task.cover.isNullOrBlank()) {
                AsyncImage(
                    model = task.cover,
                    contentDescription = task.itemTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 36.dp, height = 48.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${if (task.itemTitle.isNotBlank()) "${task.itemTitle} • " else ""}${task.title}",
                    color = OttTextPrimary,
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
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = OttInk(0.7f), modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onChangeServer,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text("Change Server", color = OttInk(0.75f), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = onRetrySameServer,
                colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
                shape = OttPillShape,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = OttTextPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Retry", color = OttTextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyDownloadsView(type: String, glass: Boolean) {
    val (icon, accent, hint) = when (type) {
        "anime" -> Triple(Icons.Default.Movie, AnimeAccent, "Episodes you save offline will appear here and play without an internet connection.")
        "manga" -> Triple(Icons.Default.Book, MangaAccent, "Chapters you save offline will appear here and open without an internet connection.")
        else -> Triple(Icons.Default.MenuBook, NovelAccent, "Chapters you save offline will appear here and read without an internet connection.")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .surface(glass, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Box(
            Modifier.size(68.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)).border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
        }
        Text("No Downloaded ${type.replaceFirstChar { it.uppercase() }}", color = OttTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = OttTextSecondary, fontSize = 12.5.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
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
                    color = OttTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = target.subtitle,
                    fontSize = 13.sp,
                    color = OttInk(0.80f),
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
                            .background(OttInk(0.09f))
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
                        .background(OttInk(0.16f))
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
                            color = OttInk(0.93f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(0.6.dp)
                            .fillMaxSize()
                            .background(OttInk(0.16f))
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
