package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.nyyrox.aniverse.data.CurrentActivityMode
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.novel.NovelChapter
import com.nyyrox.aniverse.data.novel.NovelFontFamily
import com.nyyrox.aniverse.data.novel.NovelPageStyle
import com.nyyrox.aniverse.data.novel.NovelReaderSettings
import com.nyyrox.aniverse.data.novel.NovelReaderTheme
import com.nyyrox.aniverse.data.novel.NovelTextColor
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun NovelPaperBackground(
    pageStyle: NovelPageStyle,
    theme: NovelReaderTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (pageStyle) {
            NovelPageStyle.PLAIN -> {
                // Subtle warm gradient vignette around perimeter for organic depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.04f)),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w.coerceAtLeast(h)) * 0.75f
                    ),
                    size = size
                )
            }
            NovelPageStyle.LINED -> {
                // 1. Notebook horizontal ruled lines
                val lineSpacing = 34.dp.toPx()
                val startY = 85.dp.toPx()
                val lineColor = theme.lineOrCrackColor.copy(alpha = 0.42f)
                var y = startY
                while (y < h - 40.dp.toPx()) {
                    drawLine(
                        color = lineColor,
                        start = Offset(16.dp.toPx(), y),
                        end = Offset(w - 16.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += lineSpacing
                }

                // 2. Notebook red/accent left margin rule line
                val marginX = 46.dp.toPx()
                val marginColor = if (theme == NovelReaderTheme.LINED_NOTEBOOK) {
                    Color(0x66E53935)
                } else {
                    theme.accentColor.copy(alpha = 0.38f)
                }
                drawLine(
                    color = marginColor,
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, h),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 3. Notebook binder punch holes along the far left edge
                val holeX = 14.dp.toPx()
                val holeRadius = 4.5.dp.toPx()
                val holeColor = Color.Black.copy(alpha = 0.12f)
                val holeInnerShadow = Color.Black.copy(alpha = 0.22f)
                listOf(0.12f, 0.50f, 0.88f).forEach { frac ->
                    val holeY = h * frac
                    drawCircle(
                        color = holeInnerShadow,
                        radius = holeRadius,
                        center = Offset(holeX, holeY)
                    )
                    drawCircle(
                        color = holeColor,
                        radius = holeRadius - 1.dp.toPx(),
                        center = Offset(holeX, holeY)
                    )
                }
            }
            NovelPageStyle.CRACKED -> {
                // Antique parchment texture with hairline fissures and corner aged patina
                val crackColor = theme.lineOrCrackColor.copy(alpha = 0.55f)

                // Aged border burn / vignette
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x332B1808)),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w.coerceAtLeast(h)) * 0.72f
                    ),
                    size = size
                )

                // Fissure 1: Top-Right aged branch
                val crack1 = Path().apply {
                    moveTo(w * 0.96f, 0f)
                    lineTo(w * 0.90f, h * 0.05f)
                    lineTo(w * 0.84f, h * 0.09f)
                    lineTo(w * 0.87f, h * 0.14f)
                    lineTo(w * 0.81f, h * 0.20f)
                    lineTo(w * 0.85f, h * 0.26f)
                }
                drawPath(crack1, crackColor, style = Stroke(width = 1.3f, cap = StrokeCap.Round))

                val subCrack1 = Path().apply {
                    moveTo(w * 0.90f, h * 0.05f)
                    lineTo(w * 0.98f, h * 0.08f)
                    moveTo(w * 0.84f, h * 0.09f)
                    lineTo(w * 0.76f, h * 0.11f)
                    lineTo(w * 0.73f, h * 0.15f)
                }
                drawPath(subCrack1, crackColor.copy(alpha = 0.38f), style = Stroke(width = 0.9f, cap = StrokeCap.Round))

                // Fissure 2: Bottom-Left aged branch
                val crack2 = Path().apply {
                    moveTo(0f, h * 0.86f)
                    lineTo(w * 0.08f, h * 0.83f)
                    lineTo(w * 0.14f, h * 0.87f)
                    lineTo(w * 0.22f, h * 0.82f)
                    lineTo(w * 0.29f, h * 0.85f)
                }
                drawPath(crack2, crackColor, style = Stroke(width = 1.3f, cap = StrokeCap.Round))

                val subCrack2 = Path().apply {
                    moveTo(w * 0.14f, h * 0.87f)
                    lineTo(w * 0.18f, h * 0.94f)
                    moveTo(w * 0.22f, h * 0.82f)
                    lineTo(w * 0.25f, h * 0.76f)
                }
                drawPath(subCrack2, crackColor.copy(alpha = 0.38f), style = Stroke(width = 0.9f, cap = StrokeCap.Round))

                // Fissure 3: Top-Left small hairline fracture
                val crack3 = Path().apply {
                    moveTo(w * 0.06f, 0f)
                    lineTo(w * 0.09f, h * 0.06f)
                    lineTo(w * 0.05f, h * 0.12f)
                    lineTo(w * 0.11f, h * 0.17f)
                }
                drawPath(crack3, crackColor.copy(alpha = 0.45f), style = Stroke(width = 1.1f, cap = StrokeCap.Round))

                // Fissure 4: Bottom-Right edge fracture
                val crack4 = Path().apply {
                    moveTo(w, h * 0.91f)
                    lineTo(w * 0.91f, h * 0.89f)
                    lineTo(w * 0.85f, h * 0.93f)
                    lineTo(w * 0.77f, h * 0.89f)
                }
                drawPath(crack4, crackColor.copy(alpha = 0.45f), style = Stroke(width = 1.1f, cap = StrokeCap.Round))
            }
            NovelPageStyle.DECKLED -> {
                // Vintage bound book styling: Spine curve shadow + double border rule
                // 1. Left spine vertical gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 36.dp.toPx()
                    ),
                    size = size
                )

                // Right page leaf shadow
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f)
                        ),
                        startX = w - 16.dp.toPx(),
                        endX = w
                    ),
                    size = size
                )

                // 2. Elegant double folio borders
                val outerInset = 10.dp.toPx()
                val innerInset = 14.dp.toPx()
                val folioBorderColor = theme.lineOrCrackColor.copy(alpha = 0.45f)

                drawRect(
                    color = folioBorderColor,
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(w - 2 * outerInset, h - 2 * outerInset),
                    style = Stroke(width = 1.dp.toPx())
                )

                drawRect(
                    color = folioBorderColor.copy(alpha = 0.30f),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(w - 2 * innerInset, h - 2 * innerInset),
                    style = Stroke(width = 0.75.dp.toPx())
                )

                // Corner decorative diamond marks
                val cornerOffset = 12.dp.toPx()
                listOf(
                    Offset(cornerOffset, cornerOffset),
                    Offset(w - cornerOffset, cornerOffset),
                    Offset(cornerOffset, h - cornerOffset),
                    Offset(w - cornerOffset, h - cornerOffset)
                ).forEach { pt ->
                    drawCircle(
                        color = theme.accentColor.copy(alpha = 0.4f),
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderScreen(
    vm: MainViewModel,
    novelSlug: String,
    chapterSlug: String,
    onBack: () -> Unit,
    onSelectChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        vm.setActivityMode(CurrentActivityMode.READING_NOVEL)
        onDispose {
            vm.setActivityMode(CurrentActivityMode.EXPLORING_APP)
            vm.updateCurrentActivity(null, null, null)
            vm.syncTimeSpentAndReadingToCloud()
        }
    }

    LaunchedEffect(novelSlug, chapterSlug) {
        vm.loadNovelChapter(novelSlug, chapterSlug)
    }

    val content = vm.activeChapterContent
    val settings = vm.novelReaderSettings
    val downloadTasks by vm.downloadManager.tasks.collectAsState()

    LaunchedEffect(content?.title, chapterSlug, novelSlug) {
        val novelName = novelSlug.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val chName = content?.title?.ifBlank { null } ?: chapterSlug.replace("-", " ")
        val statusText = "Reading $novelName - $chName"
        vm.updateCurrentActivity(statusText, "NOVEL", novelSlug)
    }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChapterDrawer by remember { mutableStateOf(false) }
    var hudVisible by remember { mutableStateOf(true) }

    val paragraphCount = content?.paragraphs?.size ?: 0
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { paragraphCount })

    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            showSettingsSheet -> showSettingsSheet = false
            showChapterDrawer -> showChapterDrawer = false
            !settings.isContinuousScroll && pagerState.currentPage > 0 -> {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
            settings.isContinuousScroll && listState.firstVisibleItemIndex > 0 -> {
                scope.launch {
                    listState.animateScrollToItem((listState.firstVisibleItemIndex - 3).coerceAtLeast(0))
                }
            }
            else -> onBack()
        }
    }

    LaunchedEffect(chapterSlug) {
        listState.scrollToItem(0)
        pagerState.scrollToPage(0)
    }

    val effectiveTextColor = if (settings.customTextColor.color != Color.Unspecified) {
        settings.customTextColor.color
    } else {
        settings.theme.textColor
    }

    val effectiveSecondaryColor = if (settings.customTextColor.color != Color.Unspecified) {
        settings.customTextColor.color.copy(alpha = 0.65f)
    } else {
        settings.theme.secondaryColor
    }

    // Scroll progress calculation
    val readingProgress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 0f
            else {
                val current = listState.firstVisibleItemIndex
                (current.toFloat() / (total - 1).toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(settings.theme.backgroundColor)
    ) {
        if (vm.chapterContentLoading && content == null) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x60000000))
                        .border(1.dp, Color(0x35FFFFFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                CircularProgressIndicator(
                    color = settings.theme.accentColor,
                    modifier = Modifier.size(36.dp).align(Alignment.Center)
                )
            }
            return@Box
        }

        if (vm.chapterContentError != null && content == null) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x60000000))
                        .border(1.dp, Color(0x35FFFFFF), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to Load Chapter", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = effectiveTextColor)
                        Spacer(Modifier.height(6.dp))
                        Text(vm.chapterContentError ?: "Unable to fetch chapter text", fontSize = 12.sp, color = effectiveSecondaryColor, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { vm.loadNovelChapter(novelSlug, chapterSlug) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = OttPillShape
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showChapterDrawer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = Color.White),
                                shape = OttPillShape
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Chapters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (content != null) {
            // Background Canvas for authentic page themes (lined notebook, cracked parchment, deckled book)
            NovelPaperBackground(
                pageStyle = settings.pageStyle,
                theme = settings.theme
            )

            // Reader Content Area (Tap center to toggle HUD)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        hudVisible = !hudVisible
                    }
            ) {
                if (settings.isContinuousScroll) {
                    val startPad = if (settings.pageStyle == NovelPageStyle.LINED) 54.dp else 22.dp

                    // Continuous Scroll Mode
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = startPad,
                            end = 22.dp,
                            top = 70.dp,
                            bottom = 90.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy((settings.fontSize * 0.7f).dp)
                    ) {
                        // Chapter Title Header
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = content.title,
                                    fontSize = (settings.fontSize * 1.35f).sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = settings.fontFamily.fontFamily,
                                    color = effectiveTextColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (settings.fontSize * 1.6f).sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(2.dp)
                                        .background(settings.theme.accentColor.copy(alpha = 0.6f))
                                )
                            }
                        }

                        // Paragraphs
                        items(content.paragraphs) { paragraph ->
                            val textToDisplay = if (settings.indentParagraphs) "    $paragraph" else paragraph
                            Text(
                                text = textToDisplay,
                                fontSize = settings.fontSize.sp,
                                fontFamily = settings.fontFamily.fontFamily,
                                lineHeight = (settings.fontSize * settings.lineHeight).sp,
                                color = effectiveTextColor,
                                textAlign = TextAlign.Start
                            )
                        }

                        // Bottom Navigation Footer
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                content.prevChapterSlug?.let { prev ->
                                    Button(
                                        onClick = { onSelectChapter(prev) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0x20FFFFFF),
                                            contentColor = effectiveTextColor
                                        ),
                                        shape = OttPillShape
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Prev Chapter", fontSize = 12.sp)
                                    }
                                } ?: Spacer(Modifier.width(10.dp))

                                content.nextChapterSlug?.let { next ->
                                    Button(
                                        onClick = { onSelectChapter(next) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = settings.theme.accentColor,
                                            contentColor = Color.White
                                        ),
                                        shape = OttPillShape
                                    ) {
                                        Text("Next Chapter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                } ?: Spacer(Modifier.width(10.dp))
                            }
                        }
                    }
                } else {
                    // Paged Mode (Horizontal swipe / tap)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 22.dp,
                                end = 22.dp,
                                top = 80.dp,
                                bottom = 90.dp
                            )
                    ) { pageIndex ->
                        val para = content.paragraphs.getOrNull(pageIndex).orEmpty()
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (settings.indentParagraphs) "    $para" else para,
                                fontSize = (settings.fontSize * 1.1f).sp,
                                fontFamily = settings.fontFamily.fontFamily,
                                lineHeight = (settings.fontSize * settings.lineHeight * 1.15f).sp,
                                color = effectiveTextColor
                            )
                        }
                    }
                }
            }

            // In-App Brightness Scrim Overlay (if brightness < 1.0f)
            if (settings.brightness < 1.0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (1.0f - settings.brightness) * 0.75f))
                )
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = hudVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6000000))
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        Column {
                            Text(
                                text = content.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = novelSlug.replace("-", " "),
                                fontSize = 10.sp,
                                color = OttTextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Download Chapter Button
                        val isChapterDownloaded = remember(downloadTasks, chapterSlug) {
                            vm.downloadManager.isNovelChapterDownloaded(novelSlug, chapterSlug)
                        }
                        val isChapterDownloading = remember(downloadTasks, chapterSlug) {
                            downloadTasks.any { it.itemId == novelSlug && it.chapterId == chapterSlug && it.status == com.nyyrox.aniverse.data.download.DownloadStatus.DOWNLOADING }
                        }

                        if (isChapterDownloading) {
                            CircularProgressIndicator(
                                color = OttAccentGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp).padding(2.dp)
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (!isChapterDownloaded) {
                                        vm.downloadNovelChapter(
                                            novelSlug,
                                            novelSlug.replace("-", " "),
                                            "",
                                            chapterSlug,
                                            content.title
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = if (isChapterDownloaded) "Downloaded" else "Download Chapter",
                                    tint = if (isChapterDownloaded) OttAccentGold else Color.White
                                )
                            }
                        }

                        // Chapter List Drawer Button
                        IconButton(onClick = { showChapterDrawer = true }) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Chapters",
                                tint = Color.White
                            )
                        }

                        // Reader Settings Button
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Navigation HUD Overlay
            AnimatedVisibility(
                visible = hudVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6000000))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { content.prevChapterSlug?.let { onSelectChapter(it) } },
                        enabled = content.prevChapterSlug != null
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Chapter",
                            tint = if (content.prevChapterSlug != null) Color.White else Color(0x40FFFFFF)
                        )
                    }

                    Text(
                        text = "${(readingProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = effectiveSecondaryColor
                    )

                    IconButton(
                        onClick = { content.nextChapterSlug?.let { onSelectChapter(it) } },
                        enabled = content.nextChapterSlug != null
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Chapter",
                            tint = if (content.nextChapterSlug != null) Color.White else Color(0x40FFFFFF)
                        )
                    }
                }
            }

            // ── Top Level Reader Customization Sheet ───────────────────────
            if (showSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSettingsSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF12151E),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Reader Customization",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        // 1. Page Texture & Style (Plain, Lined, Cracked, Deckled)
                        Column {
                            Text("Page Texture & Style", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NovelPageStyle.entries.forEach { style ->
                                    val isSelected = settings.pageStyle == style
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) settings.theme.accentColor.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) settings.theme.accentColor else Color(0x25FFFFFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                vm.updateNovelReaderSettings(settings.copy(pageStyle = style))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = style.displayName,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else OttTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Paper Color & Theme
                        Column {
                            Text("Paper Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NovelReaderTheme.entries.forEach { th ->
                                    val isSelected = settings.theme == th
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(th.backgroundColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) th.accentColor else Color(0x30FFFFFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                vm.updateNovelReaderSettings(settings.copy(theme = th))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = th.displayName,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = th.textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Ink / Text Color Customization
                        Column {
                            Text("Text Ink Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NovelTextColor.entries.forEach { textColorOpt ->
                                    val isSelected = settings.customTextColor == textColorOpt
                                    val displayColor = if (textColorOpt.color != Color.Unspecified) textColorOpt.color else settings.theme.textColor

                                    Row(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(if (isSelected) settings.theme.accentColor.copy(alpha = 0.22f) else Color(0x14FFFFFF))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) settings.theme.accentColor else Color(0x25FFFFFF),
                                                shape = OttPillShape
                                            )
                                            .clickable {
                                                vm.updateNovelReaderSettings(settings.copy(customTextColor = textColorOpt))
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(displayColor)
                                                .border(0.8.dp, Color(0x55FFFFFF), CircleShape)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = textColorOpt.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else OttTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Font Size (Slider + Stepper)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Font Size", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                                Text("${settings.fontSize.toInt()}sp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Slider(
                                value = settings.fontSize,
                                onValueChange = { vm.updateNovelReaderSettings(settings.copy(fontSize = it)) },
                                valueRange = 13f..28f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = settings.theme.accentColor,
                                    activeTrackColor = settings.theme.accentColor,
                                    inactiveTrackColor = Color(0x30FFFFFF)
                                )
                            )
                        }

                        // 5. Font Family Selector
                        Column {
                            Text("Font Family", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NovelFontFamily.entries.forEach { fam ->
                                    val isSelected = settings.fontFamily == fam
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(OttPillShape)
                                            .background(if (isSelected) settings.theme.accentColor else Color(0x18FFFFFF))
                                            .clickable {
                                                vm.updateNovelReaderSettings(settings.copy(fontFamily = fam))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fam.displayName,
                                            fontFamily = fam.fontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else OttTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Line Height / Spacing
                        Column {
                            Text("Line Spacing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1.4f to "Compact", 1.75f to "Standard", 2.1f to "Relaxed").forEach { (height, label) ->
                                    val isSelected = kotlin.math.abs(settings.lineHeight - height) < 0.1f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(OttPillShape)
                                            .background(if (isSelected) settings.theme.accentColor else Color(0x18FFFFFF))
                                            .clickable {
                                                vm.updateNovelReaderSettings(settings.copy(lineHeight = height))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else OttTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Reading Mode (Continuous Scroll vs Paged Mode)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Continuous Scroll", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Scroll vertically vs horizontal pages", fontSize = 10.sp, color = OttTextSecondary)
                            }
                            Switch(
                                checked = settings.isContinuousScroll,
                                onCheckedChange = {
                                    vm.updateNovelReaderSettings(settings.copy(isContinuousScroll = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = settings.theme.accentColor
                                )
                            )
                        }

                        // 8. Paragraph Indentation Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Indent Paragraphs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Traditional book paragraph indent", fontSize = 10.sp, color = OttTextSecondary)
                            }
                            Switch(
                                checked = settings.indentParagraphs,
                                onCheckedChange = {
                                    vm.updateNovelReaderSettings(settings.copy(indentParagraphs = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = settings.theme.accentColor
                                )
                            )
                        }

                        // 9. Brightness Dimmer Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Night Dimmer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OttTextSecondary)
                                Text("${(settings.brightness * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Slider(
                                value = settings.brightness,
                                onValueChange = { vm.updateNovelReaderSettings(settings.copy(brightness = it)) },
                                valueRange = 0.3f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = settings.theme.accentColor,
                                    activeTrackColor = settings.theme.accentColor,
                                    inactiveTrackColor = Color(0x30FFFFFF)
                                )
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        // ── Chapter Jump Drawer ─────────────────────────────────────────
        if (showChapterDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showChapterDrawer = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF10131B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Jump to Chapter",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(vm.novelChapters, key = { it.slug }) { ch ->
                            val isCurrent = ch.slug == chapterSlug
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0x25E50914) else Color(0x0EFFFFFF))
                                    .border(1.dp, if (isCurrent) Color(0x70E50914) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable {
                                        showChapterDrawer = false
                                        onSelectChapter(ch.slug)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color(0xFFFF8A8A) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
