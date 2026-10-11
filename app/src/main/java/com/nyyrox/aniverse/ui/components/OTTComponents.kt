package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.painterResource
import com.nyyrox.aniverse.R
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.ui.theme.LocalThemeColors
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import androidx.compose.material3.LinearProgressIndicator
import com.nyyrox.aniverse.ui.animation.mediaCardPhysics
import androidx.compose.material3.Text
import com.nyyrox.aniverse.data.download.AnimeDownloadTask
import com.nyyrox.aniverse.data.download.DownloadStatus
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.WatchHistoryItem
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttCardBottomGradient
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

@Composable
fun OttTopBar(
    watchlistCount: Int = 0,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE6040406),
                        Color(0x99040406),
                        Color(0x33040406),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Three-line Hamburger Navigation Action
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x24FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // OTT Brand Typography (Logo icon removed on Homescreen)
            AniverseLogo(showIcon = false)

            Spacer(Modifier.weight(1f))

            // Glass Search Action
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x24FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Anime",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class NavTabItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun OttBottomDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalThemeColors.current
    val themeSettings = LocalThemeSettings.current

    val tabs = remember {
        listOf(
            NavTabItem("home", Icons.Default.Home, "Anime"),
            NavTabItem("airing", Icons.Default.CalendarMonth, "Schedule"),
            NavTabItem("manga", Icons.Default.AutoStories, "Manga"),
            NavTabItem("novel", Icons.AutoMirrored.Filled.MenuBook, "Novel"),
            NavTabItem("profile", Icons.Default.Person, "Profile")
        )
    }

    val activeIndex = tabs.indexOfFirst { it.route == currentRoute }.let { if (it >= 0) it else 0 }

    // Fluid Liquid Pill Spring Animation (respects Reduced Motion)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = if (themeSettings.isReducedMotionEnabled) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                dampingRatio = 0.65f, // Bouncy liquid spring feel
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "liquid_pill_spring"
    )

    val isGlassEnabled = themeSettings.isGlassmorphismEnabled

    // Floating Glassmorphism Pill Navigation Dock
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = if (isGlassEnabled) 20.dp else 4.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color(0x80000000),
                spotColor = Color(0xB3000000)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                if (isGlassEnabled) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEB0A0B0E),
                            Color(0xF5060709)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F121C),
                            Color(0xFF0B0D14)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isGlassEnabled) {
                        listOf(
                            Color(0x40FFFFFF),
                            Color(0x14FFFFFF),
                            Color(0x10FFFFFF)
                        )
                    } else {
                        listOf(
                            Color(0x22FFFFFF),
                            Color(0x11FFFFFF)
                        )
                    }
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / tabs.size
            val pillPaddingH = 3.dp
            val pillWidth = itemWidth - (pillPaddingH * 2)

            // Glassmorphism Highlight Active Pill Indicator
            Box(
                modifier = Modifier
                    .offset(x = (itemWidth * animatedIndex) + pillPaddingH)
                    .width(pillWidth)
                    .height(46.dp)
                    .shadow(if (isGlassEnabled) 8.dp else 2.dp, RoundedCornerShape(24.dp), spotColor = Color(0x50000000), ambientColor = Color(0x20FFFFFF))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isGlassEnabled) {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x2AFFFFFF),
                                    Color(0x14FFFFFF),
                                    Color(0x1EFFFFFF)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x30FFFFFF),
                                    Color(0x18FFFFFF)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            if (isGlassEnabled) {
                                listOf(
                                    Color(0x80FFFFFF),
                                    Color(0x40FFFFFF),
                                    Color(0x12FFFFFF)
                                )
                            } else {
                                listOf(
                                    Color(0x38FFFFFF),
                                    Color(0x18FFFFFF)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            // Interactive Navigation Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isActive = index == activeIndex
                    val iconColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color(0x80A0A5B0),
                        label = "tab_icon_color"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color(0x80A0A5B0),
                        label = "tab_text_color"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 300f
                        ),
                        label = "tab_scale"
                    )

                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(46.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigate(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (tab.route == "profile") {
                            AvatarDecorationRenderer(
                                decorationId = avatarDecorationId,
                                frameId = avatarFrameId,
                                size = 22.dp
                            ) {
                                if (!userAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = tab.label,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isActive) 1.5.dp else 0.5.dp,
                                                color = if (isActive) Color.White else Color(0x66FFFFFF),
                                                shape = CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OttSectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = OttTextSecondary
                )
            }
        }
        if (onSeeAllClick != null) {
            Row(
                modifier = Modifier
                    .clip(OttPillShape)
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun OttPosterCard(
    anime: Anime,
    modifier: Modifier = Modifier.width(118.dp),
    onClickWithBounds: ((Rect) -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    val coordinator = LocalSharedPosterCoordinator.current
    val isSharedActive = coordinator?.activeAnimeId == anime.id && coordinator.isTransitioning

    Column(
        modifier = modifier
            .mediaCardPhysics(interactionSource = interactionSource, pressScale = 0.95f, enableTilt = true)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val bounds = cardBounds ?: Rect.Zero
                    if (onClickWithBounds != null) {
                        onClickWithBounds(bounds)
                    } else if (coordinator != null && bounds.width > 0) {
                        coordinator.startForwardTransition(
                            animeId = anime.id,
                            posterUrl = anime.cover,
                            bounds = bounds,
                            onNavigate = onClick
                        )
                    } else {
                        onClick()
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .onGloballyPositioned { coords ->
                    cardBounds = coords.boundsInWindow()
                }
                .clip(OttCardShape)
                .background(Color(0xFF131620))
                .border(1.dp, Color(0x22FFFFFF), OttCardShape)
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (isSharedActive) 0f else 1f
                    },
                contentScale = ContentScale.Crop
            )

            // Lightweight bottom gradient inside Card (bottom 75dp only, prevents full-surface overdraw)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .align(Alignment.BottomCenter)
                    .background(OttCardBottomGradient)
            )

            // Quality Badge (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x8A000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = anime.format ?: "HD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    softWrap = false,
                    maxLines = 1
                )
            }

            // Score Pill (Top Right) — glass pill with frosted bg
            anime.score?.let { score ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x8A000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = OttAccentGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(2.5.dp))
                    Text(
                        text = "${(score / 10f).let { String.format(java.util.Locale.US, "%.1f", it) }}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            // Bottom Info inside Card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = anime.displayTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anime.episodes?.let { "$it Episodes" } ?: anime.formattedStatus,
                    fontSize = 10.sp,
                    color = OttTextSecondary,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun OttTop10Row(
    list: List<Anime>,
    onAnimeClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(list.take(10), key = { _, anime -> anime.id }) { index, anime ->
            val isTen = index == 9
            Row(
                modifier = Modifier
                    .width(if (isTen) 178.dp else 152.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Large Stylized Monochrome Rank Number with ample spacing
                Text(
                    text = "${index + 1}",
                    fontSize = if (isTen) 68.sp else 74.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0x26FFFFFF),
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .width(if (isTen) 62.dp else 36.dp),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    letterSpacing = if (isTen) (-2).sp else 0.sp
                )

                // Poster
                OttPosterCard(
                    anime = anime,
                    modifier = Modifier.width(114.dp),
                    onClick = { onAnimeClick(anime.id) }
                )
            }
        }
    }
}

@Composable
fun OttContinueWatchingRow(
    history: List<WatchHistoryItem>,
    tmdbEpisodeStills: Map<String, String?> = emptyMap(),
    onPlay: (animeId: Int, episode: Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column {
        OttSectionHeader(
            title = "Continue Watching",
            subtitle = "Pick up right where you left off"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(history.take(10), key = { it.animeId }) { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val hasTimeline = item.durationMs > 0L && item.positionMs > 0L
                val timelineProgress = if (hasTimeline) {
                    (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.04f, 1f)
                } else {
                    val total = item.totalEpisodes ?: (item.lastEpisode + 5)
                    (item.lastEpisode.toFloat() / total.toFloat()).coerceIn(0.1f, 1f)
                }

                val remainingText = if (hasTimeline) {
                    val remMs = (item.durationMs - item.positionMs).coerceAtLeast(0L)
                    val remMin = (remMs / (1000 * 60)).toInt()
                    "Ep ${item.lastEpisode} • ${remMin}m left"
                } else {
                    "Episode ${item.lastEpisode}" + (item.totalEpisodes?.let { " of $it" } ?: "")
                }

                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .mediaCardPhysics(interactionSource = interactionSource, pressScale = 0.95f, enableTilt = true)
                        .clip(OttCardShape)
                        .background(Color(0x12FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onPlay(item.animeId, item.lastEpisode) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        val epStill = item.episodeStill?.takeIf { it.isNotBlank() }
                            ?: tmdbEpisodeStills["${item.animeId}_${item.lastEpisode}"]?.takeIf { it.isNotBlank() }
                        val displayCover = epStill
                            ?: item.banner?.takeIf { it.isNotBlank() }
                            ?: item.cover
                        AsyncImage(
                            model = displayCover,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55000000))
                        )
                        // Centered Play button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x8A000000))
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Timeline progress bar
                        LinearProgressIndicator(
                            progress = { timelineProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = OttPrimary,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }

                    // Content info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = remainingText,
                                fontSize = 11.sp,
                                color = OttTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class UnifiedHistoryEntry(val timestamp: Long, val key: String) {
    data class Anime(val data: WatchHistoryItem) : UnifiedHistoryEntry(data.timestamp, "anime_${data.animeId}")
    data class Manga(val data: MangaHistoryItem) : UnifiedHistoryEntry(data.updatedAt, "manga_${data.mangaId}")
    data class Novel(val data: NovelHistoryItem) : UnifiedHistoryEntry(data.timestamp, "novel_${data.slug}")
}

private data class HistoryDisplayMeta(
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val typeBadge: String,
    val badgeColor: Color,
    val progressFloat: Float
)

@Composable
fun OttUnifiedContinueWatchingRow(
    animeHistory: List<WatchHistoryItem>,
    mangaHistory: List<MangaHistoryItem>,
    novelHistory: List<NovelHistoryItem>,
    tmdbEpisodeStills: Map<String, String?> = emptyMap(),
    onLoadEpisodeStill: ((animeId: Int, episode: Int) -> Unit)? = null,
    onPlayAnime: (animeId: Int, episode: Int) -> Unit,
    onReadManga: (mangaId: String, chapterId: String) -> Unit,
    onReadNovel: (novelSlug: String, chapterSlug: String) -> Unit,
    onRemoveAnime: (Int) -> Unit,
    onRemoveManga: (String) -> Unit,
    onRemoveNovel: (String) -> Unit
) {
    val combined = remember(animeHistory, mangaHistory, novelHistory) {
        val list = mutableListOf<UnifiedHistoryEntry>()
        animeHistory.forEach { list.add(UnifiedHistoryEntry.Anime(it)) }
        mangaHistory.forEach { list.add(UnifiedHistoryEntry.Manga(it)) }
        novelHistory.forEach { list.add(UnifiedHistoryEntry.Novel(it)) }
        list.sortedByDescending { it.timestamp }.take(15)
    }

    if (combined.isEmpty()) return

    Column {
        OttSectionHeader(
            title = "Continue Watching & Reading",
            subtitle = "Pick up right where you left off"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(combined, key = { it.key }) { entry ->
                if (entry is UnifiedHistoryEntry.Anime && entry.data.episodeStill.isNullOrBlank()) {
                    LaunchedEffect(entry.data.animeId, entry.data.lastEpisode) {
                        onLoadEpisodeStill?.invoke(entry.data.animeId, entry.data.lastEpisode)
                    }
                }
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val meta = when (entry) {
                    is UnifiedHistoryEntry.Anime -> {
                        val item = entry.data
                        val hasTimeline = item.durationMs > 0L && item.positionMs > 0L
                        val prog = if (hasTimeline) {
                            (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.04f, 1f)
                        } else {
                            val total = item.totalEpisodes ?: (item.lastEpisode + 5)
                            (item.lastEpisode.toFloat() / total.toFloat()).coerceIn(0.1f, 1f)
                        }
                        val sub = if (hasTimeline) {
                            val remMs = (item.durationMs - item.positionMs).coerceAtLeast(0L)
                            val remMin = (remMs / (1000 * 60)).toInt()
                            "Ep ${item.lastEpisode} • ${remMin}m left"
                        } else {
                            "Episode ${item.lastEpisode}" + (item.totalEpisodes?.let { " of $it" } ?: "")
                        }
                        val epStill = item.episodeStill?.takeIf { it.isNotBlank() }
                            ?: tmdbEpisodeStills["${item.animeId}_${item.lastEpisode}"]?.takeIf { it.isNotBlank() }
                        val displayCover = epStill
                            ?: item.banner?.takeIf { it.isNotBlank() }
                            ?: item.cover
                        HistoryDisplayMeta(item.title, sub, displayCover, "ANIME", Color(0xCCE50914), prog)
                    }
                    is UnifiedHistoryEntry.Manga -> {
                        val item = entry.data
                        val prog = if (item.pageCount > 0) {
                            ((item.pageIndex + 1).toFloat() / item.pageCount.toFloat()).coerceIn(0.05f, 1f)
                        } else 0.5f
                        val chPrefix = if (item.chapterNumber.isNotBlank()) "Ch. ${item.chapterNumber}" else (item.chapterTitle?.takeIf { it.isNotBlank() } ?: "Continue Reading")
                        val sub = chPrefix + if (item.pageCount > 0) " • Pg ${item.pageIndex + 1}/${item.pageCount}" else ""
                        HistoryDisplayMeta(item.title, sub, item.poster, "MANGA", Color(0xCC7C3AED), prog)
                    }
                    is UnifiedHistoryEntry.Novel -> {
                        val item = entry.data
                        val sub = item.lastChapterTitle.ifBlank { "Continue Reading" }
                        HistoryDisplayMeta(item.title, sub, item.cover.takeIf { it.isNotBlank() }, "NOVEL", Color(0xCC0284C7), 0.75f)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(196.dp)
                        .mediaCardPhysics(interactionSource = interactionSource, pressScale = 0.95f, enableTilt = true)
                        .clip(OttCardShape)
                        .background(Color(0x12FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            when (entry) {
                                is UnifiedHistoryEntry.Anime -> onPlayAnime(entry.data.animeId, entry.data.lastEpisode)
                                is UnifiedHistoryEntry.Manga -> onReadManga(entry.data.mangaId, entry.data.chapterId)
                                is UnifiedHistoryEntry.Novel -> onReadNovel(entry.data.slug, entry.data.lastChapterSlug)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        if (!meta.coverUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val coverRequest = remember(meta.coverUrl) {
                                val url = meta.coverUrl
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
                                            val orig = when {
                                                ref.contains("asura") -> "https://asuracomic.net"
                                                ref.contains("comick") -> "https://comick.art"
                                                ref.contains("mangadex") -> "https://mangadex.org"
                                                else -> "https://weebcentral.com"
                                            }
                                            httpHeaders(
                                                NetworkHeaders.Builder()
                                                    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                                    .set("Referer", ref)
                                                    .set("Origin", orig)
                                                    .build()
                                            )
                                        }
                                    }
                                    .build()
                            }
                            AsyncImage(
                                model = coverRequest,
                                contentDescription = meta.title,
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
                                    tint = Color(0x55FFFFFF),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55000000))
                        )

                        // Type badge at top-left
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(meta.badgeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = meta.typeBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Remove button at top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .clickable {
                                    when (entry) {
                                        is UnifiedHistoryEntry.Anime -> onRemoveAnime(entry.data.animeId)
                                        is UnifiedHistoryEntry.Manga -> onRemoveManga(entry.data.mangaId)
                                        is UnifiedHistoryEntry.Novel -> onRemoveNovel(entry.data.slug)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xCCFFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Center Action Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC000000))
                                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            when (entry) {
                                is UnifiedHistoryEntry.Anime -> {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                is UnifiedHistoryEntry.Manga -> {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Read",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                is UnifiedHistoryEntry.Novel -> {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        contentDescription = "Read",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Progress indicator at bottom
                        LinearProgressIndicator(
                            progress = { meta.progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter),
                            color = OttPrimary,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }

                    // Content info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meta.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = meta.subtitle,
                                fontSize = 11.sp,
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
}

@Composable
fun OttGenrePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(OttPillShape)
            .background(if (isSelected) Color.White else Color(0x18FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else OttTextSecondary
        )
    }
}

@Composable
fun ZenkaiLogo(
    modifier: Modifier = Modifier,
    size: Int = 22,
    showIcon: Boolean = false
) {
    val logoHeight = size.dp
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Image(
            painter = painterResource(id = R.drawable.zenkai_navbar_logo),
            contentDescription = "ZENKAI",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(logoHeight)
        )
    }
}

@Composable
fun AniverseLogo(
    modifier: Modifier = Modifier,
    size: Int = 22,
    showIcon: Boolean = true
) {
    ZenkaiLogo(modifier = modifier, size = size, showIcon = showIcon)
}

@Composable
fun OttEpisodeCardItem(
    anime: Anime,
    episode: Int,
    isSelected: Boolean,
    episodeStill: String? = null,
    episodeTitle: String? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadTask: AnimeDownloadTask? = null,
    onDownload: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val bgBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color(0x351E2235),
                Color(0x20141825)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x221E2230),
                Color(0x10121620)
            )
        )
    }

    val borderBrush = if (isSelected) {
        Brush.verticalGradient(
            listOf(
                Color(0x95FFFFFF),
                Color(0x40FF1E27),
                Color(0x25FFFFFF)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x35FFFFFF),
                Color(0x0EFFFFFF)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(OttCardShape)
            .background(bgBrush)
            .border(1.dp, borderBrush, OttCardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with EP badge — 16dp global card radius
        Box(
            modifier = Modifier
                .size(105.dp, 68.dp)
                .clip(OttCardShape)
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))
                    ),
                    OttCardShape
                )
        ) {
            AsyncImage(
                model = episodeStill?.takeIf { it.isNotBlank() } ?: anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                contentDescription = "Episode $episode",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x44000000))
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color(0xCC000000))
                    .border(
                        1.dp,
                        if (isSelected) Color.White else Color(0x40FFFFFF),
                        CircleShape
                    )
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            val mainTitle = episodeTitle?.takeIf { it.isNotBlank() } ?: "Episode $episode"
            val subLabel = if (episodeTitle.isNullOrBlank()) {
                anime.duration?.let { "$it mins" } ?: "24 mins"
            } else {
                "Episode $episode · ${anime.duration?.let { "$it mins" } ?: "24 mins"}"
            }
            Text(
                text = mainTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else OttTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subLabel,
                fontSize = 11.sp,
                color = OttTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Optional Download Action / Status
        if (onDownload != null) {
            if (isDownloading) {
                val status = downloadTask?.status ?: DownloadStatus.DOWNLOADING
                val progress = downloadTask?.progress ?: 0f
                val percent = (progress * 100).toInt()
                val isQueued = status == DownloadStatus.QUEUED
                val isConnecting = !isQueued && (progress <= 0.02f || downloadTask?.speedFormatted?.contains("Connecting", ignoreCase = true) == true)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .clickable(onClick = onDownload)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isQueued) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Queued",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Queued",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    } else if (isConnecting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = OttPrimary,
                            strokeWidth = 1.8.dp
                        )
                        Text(
                            text = "Connecting",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(15.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { if (progress > 0f) progress else 0.05f },
                                modifier = Modifier.fillMaxSize(),
                                color = OttPrimary,
                                trackColor = Color(0x33FFFFFF),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = "$percent%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            } else {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isDownloaded) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xAAFFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
        }

        // Status badge — unified chip shape (8dp radius, matches metadata chips)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF12121C))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0x18FFFFFF), Color(0x0AFFFFFF))
                        )
                    }
                )
                .border(
                    1.dp,
                    if (isSelected) {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF1E27), Color(0x80FF1E27))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0x25FFFFFF), Color(0x0AFFFFFF))
                        )
                    },
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isSelected) "PLAYING" else "EP $episode",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color(0xFFFF1E27) else OttTextSecondary
            )
        }
    }
}

