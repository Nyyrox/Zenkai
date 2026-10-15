package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.animateColorAsState
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.styropyr0.prismal.components.PrismalGlassBottomTabs
import com.styropyr0.prismal.components.PrismalGlassBottomTab
import com.styropyr0.prismal.components.LocalPrismalBottomTabHighlightedIndex
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.LocalPrismalBackdrop
import com.styropyr0.prismal.sources.emptyGlassSource
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import androidx.compose.ui.draw.scale
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
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttOnPrimary
import com.nyyrox.aniverse.ui.theme.OttAccentRed
import dev.chrisbanes.haze.hazeEffect
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.ui.theme.OttOnMediaDim

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
                        OttBg.copy(alpha = 0.90f),
                        OttBg.copy(alpha = 0.55f),
                        OttBg.copy(alpha = 0.15f),
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
            OttGlassIconButton(
                onClick = onMenuClick,
                size = 40.dp
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = OttTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // OTT Brand Typography (Logo icon removed on Homescreen)
            AniverseLogo(showIcon = false)

            Spacer(Modifier.weight(1f))

            // Glass Search Action
            OttGlassIconButton(
                onClick = onSearchClick,
                size = 40.dp
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Anime",
                    tint = OttTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Icon button. Ghost by default (no fill, no border); pass [filled] only where it sits on busy artwork. */
@Composable
fun OttGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    filled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ottBtnPress"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(OttInk(if (isPressed) 0.16f else 0.10f)) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Premium Liquid Glass Toggle Switch inspired by Prismal AGSL toggle physics.
 * Features spring animated thumb, damped drag/press scale, velocity elongation,
 * smooth color lerp, and specular highlights.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = OttPrimary,
    inactiveColor: Color = OttInk(0.22f),
    enabled: Boolean = true
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = OttOnPrimary,
            checkedTrackColor = activeColor,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = OttTextMuted,
            uncheckedTrackColor = inactiveColor,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

private data class NavTabItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * Floating nav bar. One blurred Haze capsule (frosted material over the live screen), no per-tab bubbles:
 * the selected tab is just full-contrast with a small accent bar. Falls back to a solid panel when glass is off.
 */
@OptIn(dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)
@Composable
fun OttBottomDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            NavTabItem("home", Icons.Default.Home, "Anime"),
            NavTabItem("airing", Icons.Default.CalendarMonth, "Schedule"),
            NavTabItem("manga", Icons.Default.AutoStories, "Manga"),
            NavTabItem("novel", Icons.AutoMirrored.Filled.MenuBook, "Novel"),
            NavTabItem("profile", Icons.Default.Person, "Profile")
        )
    }
    val shape = RoundedCornerShape(30.dp)
    val glass = hazeState != null && LocalThemeSettings.current.isGlassmorphismEnabled
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(shape)
            .then(
                if (glass) Modifier.hazeEffect(
                    state = hazeState!!,
                    style = dev.chrisbanes.haze.materials.HazeMaterials.regular(containerColor = OttBg)
                ) else Modifier.background(OttPanelHigh)
            )
            .border(0.5.dp, OttInk(0.12f), shape)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == currentRoute
            val tint by animateColorAsState(if (selected) OttTextPrimary else OttTextMuted, label = "navTint")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                        onNavigate(tab.route)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (tab.route == "profile" && !userAvatarUrl.isNullOrBlank()) {
                    AvatarDecorationRenderer(decorationId = avatarDecorationId, frameId = avatarFrameId, size = 24.dp) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = tab.label,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(if (selected) 1.5.dp else 0.dp, OttTextPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .size(width = 14.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(if (selected) OttAccentRed else Color.Transparent)
                )
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
                color = OttTextPrimary
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
                    color = OttTextPrimary
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OttTextPrimary,
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
                .background(OttPanelHigh)
                .then(if (com.nyyrox.aniverse.ui.theme.ThemeRuntime.cardStyle) Modifier.border(1.dp, Color(0x22FFFFFF), OttCardShape) else Modifier)
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
                    color = OttOnMediaDim,
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
                                color = OttOnMediaDim
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
                                    .background(OttPanelHigh),
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
                                color = OttOnMediaDim,
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
            .background(if (isSelected) OttTextPrimary else OttInk(0.09f))
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
fun EpisodeFillerBadge(
    modifier: Modifier = Modifier,
    isMixed: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (isMixed) Color(0x356366F1) else Color(0x35F59E0B))
            .border(
                0.7.dp,
                if (isMixed) Color(0x70818CF8) else Color(0x75FBBF24),
                RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 5.5.dp, vertical = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isMixed) Color(0xFFA5B4FC) else Color(0xFFFDE68A))
            )
            Text(
                text = if (isMixed) "MIXED" else "FILLER",
                color = if (isMixed) Color(0xFFC7D2FE) else Color(0xFFFDE68A),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
        }
    }
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
    isFiller: Boolean = false,
    isMixed: Boolean = false,
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "epCardPress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Color(0x321E2335) else Color(0x16131824))
            .border(
                1.dp,
                if (isSelected) Color(0x55FFFFFF) else Color(0x18FFFFFF),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Episode Still Thumbnail (player_design_2.jpg)
        Box(
            modifier = Modifier
                .size(118.dp, 66.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x18FFFFFF))
                .border(0.8.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp))
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f))
                        )
                    )
            )
            if (isFiller || isMixed) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMixed) Color(0xD04338CA) else Color(0xD0B45309))
                        .border(
                            0.6.dp,
                            if (isMixed) Color(0x90A5B4FC) else Color(0x90FDE68A),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isMixed) "MIXED" else "FILLER",
                        color = if (isMixed) Color(0xFFE0E7FF) else Color(0xFFFEF3C7),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // 2. Title, Episode Info & Downloaded Status (Middle Column)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val mainTitle = episodeTitle?.takeIf { it.isNotBlank() } ?: "Episode $episode"
            val subLabel = "Episode $episode · ${anime.duration ?: 24} min"

            Text(
                text = mainTitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = subLabel,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isFiller || isMixed) {
                    EpisodeFillerBadge(isMixed = isMixed)
                }
            }

            // Status: Green checkmark Downloaded (or downloading / download action)
            if (isDownloaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Downloaded",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34D399)
                    )
                }
            } else if (isDownloading) {
                val progress = downloadTask?.progress ?: 0f
                val percent = (progress * 100).toInt()
                val isQueued = downloadTask?.status == DownloadStatus.QUEUED
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { if (progress > 0f) progress else 0.05f },
                        modifier = Modifier.size(12.dp),
                        color = OttPrimary,
                        trackColor = Color(0x33FFFFFF),
                        strokeWidth = 1.8.dp
                    )
                    Text(
                        text = if (isQueued) "Queued" else "Downloading: $percent%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isQueued) Color(0xFFFBBF24) else Color.White
                    )
                }
            } else if (onDownload != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDownload)
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Download",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // 3. Circular Play Button (player_design_2.jpg)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSelected) OttPrimary else Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Episode $episode",
                tint = if (isSelected) Color.White else Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

