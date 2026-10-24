package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import com.nyyrox.aniverse.ui.screens.HomePalette
import com.nyyrox.aniverse.ui.screens.rememberHomePalette
import kotlin.math.abs
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
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary

/**
 * Watches the vertical scroll direction of whatever list is on screen (through nested scroll) so the top bar and
 * the bottom dock can collapse / expand without any screen having to expose its scroll state.
 * Attach [connection] with Modifier.nestedScroll around the screen container.
 */
@Stable
class ChromeScrollState {
    /** True while the user reads down the page (content moving up): dock collapses, top bar goes compact. */
    var scrollingDown by mutableStateOf(false)
        private set

    private var offsetPx = 0f
    private var runPx = 0f

    val bottomCollapsed: Boolean get() = scrollingDown

    /** Full top bar (menu, logo, profile) is visible at the top and whenever the user swipes back up. */
    val topExpanded: Boolean get() = !scrollingDown

    fun reset() {
        offsetPx = 0f
        runPx = 0f
        scrollingDown = false
    }

    fun expandBottom() {
        runPx = 0f
        scrollingDown = false
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            if (abs(dy) < 0.5f) return Offset.Zero
            // dy > 0: finger moving down (content reveals the top). dy < 0: finger moving up (reading on).
            offsetPx = (offsetPx - dy).coerceAtLeast(0f)
            if (dy * runPx < 0f) runPx = 0f
            runPx += dy
            if (offsetPx < 48f) {
                if (scrollingDown) scrollingDown = false
            } else if (runPx < -64f && !scrollingDown) {
                scrollingDown = true
            } else if (runPx > 40f && scrollingDown) {
                scrollingDown = false
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberChromeScrollState(): ChromeScrollState = remember { ChromeScrollState() }

/** Glass (or flat when glass is off) surface that follows the light / dark palette. */
private fun Modifier.ottGlass(p: HomePalette, shape: Shape): Modifier = this
    .clip(shape)
    .background(
        if (p.glassOn) {
            Brush.verticalGradient(
                if (p.dark) listOf(Color(0x38FFFFFF), Color(0x16FFFFFF))
                else listOf(Color(0xD9FFFFFF), Color(0x99FFFFFF))
            )
        } else {
            SolidColor(if (p.dark) Color(0xFF1A1D26) else Color(0xFFF1F2F6))
        }
    )
    .border(1.dp, p.glassBorder, shape)

@Composable
private fun OttGlassCircle(
    p: HomePalette,
    size: Dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ottCirclePress"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = s; scaleY = s }
            .ottGlass(p, CircleShape)
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun AvatarOrIcon(
    tab: NavTabItem,
    selected: Boolean,
    p: HomePalette,
    userAvatarUrl: String?,
    avatarDecorationId: String?,
    avatarFrameId: String?,
    avatarSize: Dp,
    iconSize: Dp
) {
    if (tab.route == "profile") {
        AvatarDecorationRenderer(
            decorationId = avatarDecorationId,
            frameId = avatarFrameId,
            size = avatarSize
        ) {
            if (!userAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userAvatarUrl,
                    contentDescription = tab.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 1.5.dp else 0.5.dp,
                            color = if (selected) p.text else p.text.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) p.text else p.textMuted,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    } else {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) p.text else p.textMuted,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Top bar. Visible at the top of the page: hamburger + ZENKAI logo on the left, profile bubble on the right.
 * While reading down the page ([expanded] = false) the menu and logo fade away and only the profile bubble stays.
 * Driven by one slow spring so nothing jumps; follows the glass toggle and light / dark palette.
 */
@Composable
fun OttTopBar(
    expanded: Boolean = true,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null,
    watchlistCount: Int = 0,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onWatchlistClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    val fraction by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 170f),
        label = "topBarExpand"
    )
    val scrim = p.gradTop

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to scrim.copy(alpha = 0.92f),
                        0.55f to scrim.copy(alpha = 0.55f),
                        1f to scrim.copy(alpha = 0f)
                    ),
                    alpha = fraction.coerceIn(0f, 1f)
                )
            }
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fraction > 0.01f) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        val f = fraction.coerceIn(0f, 1f)
                        alpha = f
                        translationX = -(1f - f) * 28.dp.toPx()
                        val sc = 0.88f + 0.12f * f
                        scaleX = sc
                        scaleY = sc
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OttGlassCircle(p, 40.dp, onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = p.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    AniverseLogo(showIcon = false, tint = if (p.dark) null else p.text)
                }
            }

            Spacer(Modifier.weight(1f))

            OttGlassCircle(p, 40.dp, onProfileClick) {
                AvatarDecorationRenderer(
                    decorationId = avatarDecorationId,
                    frameId = avatarFrameId,
                    size = 32.dp
                ) {
                    if (!userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = p.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Premium liquid glass icon button with spring press haptics and glass gradient refraction. */
@Composable
fun OttGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
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
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0x48FFFFFF), Color(0x32FFFFFF))
                    } else {
                        listOf(Color(0x30FFFFFF), Color(0x18FFFFFF))
                    }
                )
            )
            .border(
                1.dp,
                if (isPressed) Color(0x60FFFFFF) else Color(0x38FFFFFF),
                CircleShape
            )
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
    activeColor: Color = Color(0xFF34C759),
    inactiveColor: Color = Color(0x38FFFFFF),
    enabled: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth spring animation for switch position (0f = off, 1f = on)
    val positionFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "switchPosition"
    )

    // Damped press / hold scale animation (expanding thumb slightly while interacting)
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switchPress"
    )

    // Animated track background color
    val trackBgColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchTrackBg"
    )

    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize = 24.dp
    val maxTravelPx = with(density) { (trackWidth - thumbSize - 6.dp).toPx() }
    val startPaddingPx = with(density) { 3.dp.toPx() }

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(trackBgColor)
            .border(
                1.dp,
                if (checked) Color(0x60FFFFFF) else Color(0x28FFFFFF),
                RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Subtle inner glow / highlight on track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x25FFFFFF), Color.Transparent)
                    )
                )
        )

        // Refracting liquid glass thumb with velocity squash/stretch and specular rim
        val currentThumbX = startPaddingPx + (maxTravelPx * positionFraction)
        val thumbScaleX = 1f + (pressProgress * 0.28f)
        val thumbScaleY = 1f - (pressProgress * 0.12f)

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = currentThumbX
                    scaleX = thumbScaleX
                    scaleY = thumbScaleY
                }
                .size(thumbSize)
                .shadow(elevation = if (isPressed) 4.dp else 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color.White, Color(0xFFF1F5F9))
                        } else {
                            listOf(Color.White, Color(0xFFE2E8F0))
                        }
                    )
                )
                .border(0.8.dp, Color(0x50FFFFFF), CircleShape)
        ) {
            // Subtle glass specular top-shine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0x33FFFFFF))
            )
        }
    }
}

private data class NavTabItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val DockTabs = listOf(
    NavTabItem("home", Icons.Default.Home, "Anime"),
    NavTabItem("airing", Icons.Default.CalendarMonth, "Schedule"),
    NavTabItem("manga", Icons.Default.AutoStories, "Manga"),
    NavTabItem("novel", Icons.AutoMirrored.Filled.MenuBook, "Novel")
)

@Composable
private fun DockTabContent(
    tab: NavTabItem,
    selected: Boolean,
    p: HomePalette,
    userAvatarUrl: String?,
    avatarDecorationId: String?,
    avatarFrameId: String?,
    showLabel: Boolean = true
) {
    AvatarOrIcon(tab, selected, p, userAvatarUrl, avatarDecorationId, avatarFrameId, avatarSize = 22.dp, iconSize = 20.dp)
    if (showLabel) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) p.text else p.textMuted,
            maxLines = 1
        )
    }
}

/** Single round glass button: the collapsed dock and the separate search button. */
@Composable
private fun DockSingleButton(
    p: HomePalette,
    glassOn: Boolean,
    backdrop: com.styropyr0.prismal.PrismalBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (glassOn) {
        PrismalGlassBottomTabs(
            selectedTabIndex = { 0 },
            onTabSelected = { onClick() },
            backdrop = backdrop,
            tabsCount = 1,
            specular = { PrismalSpecular.Default },
            tintDropletContent = false,
            modifier = modifier.width(64.dp)
        ) {
            PrismalGlassBottomTab(onClick = onClick) { content() }
        }
    } else {
        val context = LocalContext.current
        Box(
            modifier = modifier
                .size(58.dp)
                .shadow(4.dp, CircleShape)
                .ottGlass(p, CircleShape)
                .clickable {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

/**
 * Bottom dock: a tab pill (Anime / Schedule / Manga / Novel) plus a separate round search button, like the
 * Apple Music bar. [collapsed] shrinks the pill into one round button showing only the current tab; tapping it or
 * scrolling back up springs it open. Glass on -> liquid glass, off -> flat themed surface; both follow light / dark.
 */
@Composable
fun OttBottomDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null,
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    collapsed: Boolean = false,
    onExpand: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    val tabs = DockTabs
    // Profile lives in the top bar now; on the profile screen the pill keeps the last tab it showed.
    val found = tabs.indexOfFirst { it.route == currentRoute }
    var lastIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(found) { if (found >= 0) lastIndex = found }
    val activeIndex = if (found >= 0) found else lastIndex
    val activeTab = tabs[activeIndex]
    val glassOn = p.glassOn
    val ambientBackdrop = backdrop ?: com.styropyr0.prismal.LocalPrismalBackdrop.current ?: com.styropyr0.prismal.sources.emptyGlassSource()

    val f by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f),
        label = "dockExpand"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomStart
        ) {
            // Full pill: scales down toward the bottom-left button while it fades out.
            if (f > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.08f, 1f)
                            val sc = 0.2f + 0.8f * f.coerceIn(0f, 1f)
                            scaleX = sc
                            scaleY = sc
                            alpha = ((f - 0.05f) / 0.7f).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (glassOn) {
                        PrismalGlassBottomTabs(
                            selectedTabIndex = { activeIndex },
                            onTabSelected = { idx ->
                                if (idx in tabs.indices) onNavigate(tabs[idx].route)
                            },
                            backdrop = ambientBackdrop,
                            tabsCount = tabs.size,
                            specular = { PrismalSpecular.Default },
                            tintDropletContent = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val highlightedIndex = LocalPrismalBottomTabHighlightedIndex.current
                            for ((index, tab) in tabs.withIndex()) {
                                val isSelected = highlightedIndex() == index
                                PrismalGlassBottomTab(onClick = { onNavigate(tab.route) }) {
                                    DockTabContent(tab, isSelected, p, userAvatarUrl, avatarDecorationId, avatarFrameId)
                                }
                            }
                        }
                    } else {
                        FlatDockBar(
                            tabs = tabs,
                            activeIndex = activeIndex,
                            p = p,
                            userAvatarUrl = userAvatarUrl,
                            avatarDecorationId = avatarDecorationId,
                            avatarFrameId = avatarFrameId,
                            onNavigate = onNavigate
                        )
                    }
                }
            }

            // Collapsed: just the current tab.
            if (f < 0.99f) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        val sc = 0.6f + 0.4f * (1f - f.coerceIn(0f, 1f))
                        scaleX = sc
                        scaleY = sc
                        alpha = ((0.8f - f) / 0.6f).coerceIn(0f, 1f)
                    }
                ) {
                    DockSingleButton(p, glassOn, ambientBackdrop, onExpand) {
                        DockTabContent(activeTab, true, p, userAvatarUrl, avatarDecorationId, avatarFrameId, showLabel = false)
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // Search: its own round button next to the pill.
        DockSingleButton(p, glassOn, ambientBackdrop, onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = p.text,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Flat, cheap dock used when glassmorphism is off. Uses the palette, so light theme works too. */
@Composable
private fun FlatDockBar(
    tabs: List<NavTabItem>,
    activeIndex: Int,
    p: HomePalette,
    userAvatarUrl: String?,
    avatarDecorationId: String?,
    avatarFrameId: String?,
    onNavigate: (String) -> Unit
) {
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "flat_dock_spring"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(32.dp))
            .ottGlass(p, RoundedCornerShape(32.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth / tabs.size
            val pillPaddingH = 3.dp

            Box(
                modifier = Modifier
                    .offset(x = (itemWidth * animatedIndex) + pillPaddingH)
                    .width(itemWidth - pillPaddingH * 2)
                    .height(46.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.text.copy(alpha = 0.12f))
                    .border(1.dp, p.glassBorder, RoundedCornerShape(24.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(46.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigate(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        DockTabContent(tab, index == activeIndex, p, userAvatarUrl, avatarDecorationId, avatarFrameId)
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
    showIcon: Boolean = false,
    tint: Color? = null
) {
    val logoHeight = size.dp
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Image(
            painter = painterResource(id = R.drawable.zenkai_navbar_logo),
            contentDescription = "ZENKAI",
            contentScale = ContentScale.Fit,
            colorFilter = tint?.let { ColorFilter.tint(it) },
            modifier = Modifier.height(logoHeight)
        )
    }
}

@Composable
fun AniverseLogo(
    modifier: Modifier = Modifier,
    size: Int = 22,
    showIcon: Boolean = true,
    tint: Color? = null
) {
    ZenkaiLogo(modifier = modifier, size = size, showIcon = showIcon, tint = tint)
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

