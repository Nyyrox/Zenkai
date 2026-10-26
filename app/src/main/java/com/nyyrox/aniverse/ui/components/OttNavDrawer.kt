package com.nyyrox.aniverse.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.ui.screens.HomePalette
import com.nyyrox.aniverse.ui.screens.rememberHomePalette
import com.styropyr0.prismal.LocalPrismalBackdrop
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.emptyGlassSource
import kotlin.math.roundToInt

private const val DISCORD_INVITE_URL = "https://discord.gg/kHxHzZtH7"
private const val TELEGRAM_URL = "https://t.me/StreamverseOfc"

private data class DrawerEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val target: String
)

private val DrawerEntries = listOf(
    DrawerEntry(Icons.Default.Explore, "Browse Catalog", "Genres, seasons & year", "browse"),
    DrawerEntry(Icons.Default.Schedule, "Airing Schedule", "Weekly release calendar", "airing"),
    DrawerEntry(Icons.Default.Bookmark, "Watchlist & Library", "Saved anime, manga & novels", "watchlist"),
    DrawerEntry(Icons.Default.DownloadDone, "Offline Downloads", "Read and watch offline", "downloads"),
    DrawerEntry(Icons.Default.Forum, "Discord Community", "Join the server, Rich Presence", "discord_hub"),
    DrawerEntry(Icons.Default.Groups, "Watch Together", "Sync watch rooms with friends", "watchtogether"),
    DrawerEntry(Icons.Default.Person, "Profile & Settings", "Themes, playback & preferences", "profile")
)

private val DrawerGlassShape: () -> Shape = { PrismalRoundedRectangle(28.dp) }

/**
 * Unified Expandable Top Bar & Navigation Drawer.
 * When closed, it is the sleek 44dp top bar pill with hamburger icon, ZENKAI logo and profile bubble.
 * When scrolling down, the profile bubble stays pinned at the top-right corner.
 * When menu is clicked, that exact bar smoothly expands downwards into the liquid-glass navigation menu,
 * and the hamburger lines morph into an 'X'.
 */
@Composable
fun ExpandableOttTopBar(
    expanded: Boolean = true,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null,
    watchlistCount: Int = 0,
    isMenuOpen: Boolean = false,
    onMenuClick: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onWatchlistClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    val context = LocalContext.current
    val fraction by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "topBarMorph"
    )
    val menuProgress = animateFloatAsState(
        targetValue = if (isMenuOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
        label = "drawerProgress"
    )

    BackHandler(enabled = isMenuOpen) { onDismissMenu() }

    val barHeightPx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val backdrop = LocalPrismalBackdrop.current ?: emptyGlassSource()
    val scrim = p.gradTop

    val isAnimatingMenu = isMenuOpen || menuProgress.value > 0.005f
    val effectiveFraction = if (isAnimatingMenu) 1f else fraction

    Box(
        modifier = if (isAnimatingMenu) {
            Modifier.fillMaxSize()
        } else {
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
        }
    ) {
        // Scrim background when menu is opening / open
        if (isAnimatingMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = menuProgress.value.coerceIn(0f, 1f) }
                    .background(Color.Black.copy(alpha = if (p.dark) 0.40f else 0.18f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = isMenuOpen
                    ) { onDismissMenu() }
            )
        }

        // Top gradient scrim for status bar readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to scrim.copy(alpha = 0.92f),
                            0.55f to scrim.copy(alpha = 0.55f),
                            1f to scrim.copy(alpha = 0f)
                        ),
                        alpha = maxOf(fraction, menuProgress.value).coerceIn(0f, 1f)
                    )
                }
        )

        // The bar / panel that expands downwards in-place
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .then(if (isAnimatingMenu) Modifier.navigationBarsPadding() else Modifier)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = if (isAnimatingMenu) 12.dp else 16.dp
                )
        ) {
            val fullWidth = maxWidth
            val barHeight = 44.dp
            val pillWidth = lerp(barHeight, fullWidth, effectiveFraction.coerceIn(0f, 1f))

            val surface = if (p.dark) Color(0xFF1C1C1E).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.70f)
            val cardRadius = lerp(22.dp, 28.dp, menuProgress.value)
            val cardShape = RoundedCornerShape(cardRadius)

            val glass = rememberOttGlass(
                backdrop = backdrop,
                p = p,
                shape = if (isAnimatingMenu) DrawerGlassShape else OttCapsuleShape,
                refractionHeight = if (isAnimatingMenu) 22.dp else 16.dp,
                refractionAmount = if (isAnimatingMenu) 22.dp else 16.dp,
                surfaceColor = if (isAnimatingMenu) surface else null
            )

            val cardModifier = if (p.glassOn) {
                glass.clip(cardShape)
            } else {
                Modifier
                    .clip(cardShape)
                    .background(if (p.dark) Color(0xFF1A1A1C) else Color(0xFFF7F7F9))
                    .border(0.8.dp, p.glassBorder, cardShape)
            }

            if (!isAnimatingMenu) {
                // Collapsed Top Bar: exact behavior that pins the profile bubble at TopEnd when scrolling
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(pillWidth)
                        .height(barHeight)
                        .then(if (p.glassOn) glass.clip(CircleShape) else Modifier.ottGlass(p, CircleShape))
                ) {
                    // Laid out at full width and anchored to the right, so the shape only reveals / hides it.
                    // Lets the row be wider than the pill and pins its right edge to the pill's right edge,
                    // so the profile bubble stays put while the pill shrinks onto it when scrolling down.
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(Alignment.End, unbounded = true)
                            .width(fullWidth)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.graphicsLayer {
                                alpha = ((fraction - 0.45f) / 0.45f).coerceIn(0f, 1f)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MenuLinesButton(
                                color = p.text,
                                openProgress = { 0f },
                                onClick = onMenuClick,
                                enabled = expanded
                            )
                            Spacer(Modifier.width(8.dp))
                            AniverseLogo(showIcon = false, tint = if (p.dark) null else p.text)
                        }

                        Spacer(Modifier.weight(1f))

                        OttProfileBubble(
                            p = p,
                            avatarUrl = userAvatarUrl,
                            decorationId = avatarDecorationId,
                            frameId = avatarFrameId,
                            onClick = onProfileClick
                        )
                    }
                }
            } else {
                // Expanding Navigation Drawer Panel
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(pillWidth)
                ) {
                    Layout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(cardModifier)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                        content = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header Row: Hamburger Lines -> 'X', ZENKAI logo, Profile bubble
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.graphicsLayer {
                                            alpha = ((effectiveFraction - 0.45f) / 0.45f).coerceIn(0f, 1f)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MenuLinesButton(
                                            color = p.text,
                                            openProgress = { menuProgress.value },
                                            onClick = onDismissMenu,
                                            enabled = true
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        AniverseLogo(showIcon = false, tint = if (p.dark) null else p.text)
                                    }

                                    Spacer(Modifier.weight(1f))

                                    OttProfileBubble(
                                        p = p,
                                        avatarUrl = userAvatarUrl,
                                        decorationId = avatarDecorationId,
                                        frameId = avatarFrameId,
                                        onClick = {
                                            onDismissMenu()
                                            onProfileClick()
                                        }
                                    )
                                }

                                // Drawer Content: only measured and revealed when menu expands
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Spacer(Modifier.height(10.dp))
                                    SectionLabel("EXPLORE", Modifier.stagger(menuProgress, 0), p)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .stagger(menuProgress, 1)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(p.text.copy(alpha = 0.06f))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        DrawerEntries.forEachIndexed { i, entry ->
                                            DrawerRow(
                                                p = p,
                                                entry = entry,
                                                showDivider = i != DrawerEntries.lastIndex,
                                                modifier = Modifier.stagger(menuProgress, 1 + i),
                                                onClick = {
                                                    onDismissMenu()
                                                    onNavigate(entry.target)
                                                }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(14.dp))
                                    SectionLabel("COMMUNITY", Modifier.stagger(menuProgress, DrawerEntries.size + 1), p)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .stagger(menuProgress, DrawerEntries.size + 2),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CommunityButton(
                                            p = p,
                                            icon = Icons.AutoMirrored.Filled.Send,
                                            label = "Telegram",
                                            modifier = Modifier.weight(1f),
                                            onClick = { openUrl(context, TELEGRAM_URL) }
                                        )
                                        CommunityButton(
                                            p = p,
                                            icon = Icons.Default.Forum,
                                            label = "Discord",
                                            modifier = Modifier.weight(1f),
                                            onClick = { openUrl(context, DISCORD_INVITE_URL) }
                                        )
                                    }
                                    Spacer(Modifier.height(18.dp))
                                }
                            }
                        }
                    ) { measurables, constraints ->
                        val placeable = measurables.first().measure(
                            constraints.copy(minWidth = constraints.maxWidth, minHeight = 0)
                        )
                        val collapsed = barHeightPx.coerceAtMost(placeable.height)
                        val t = menuProgress.value.coerceIn(0f, 1f)
                        val h = (collapsed + (placeable.height - collapsed) * t).roundToInt()
                        layout(placeable.width, h) { placeable.place(0, 0) }
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for OttNavDrawer
 */
@Composable
fun OttNavDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    userAvatarUrl: String? = null,
    avatarDecorationId: String? = null,
    avatarFrameId: String? = null
) {
    if (isOpen) {
        ExpandableOttTopBar(
            expanded = true,
            userAvatarUrl = userAvatarUrl,
            avatarDecorationId = avatarDecorationId,
            avatarFrameId = avatarFrameId,
            isMenuOpen = true,
            onDismissMenu = onDismiss,
            onNavigate = onNavigate
        )
    }
}

/** Fades and lifts a row in once the panel is far enough open; [index] sets the order. */
private fun Modifier.stagger(progress: State<Float>, index: Int): Modifier = graphicsLayer {
    val a = ((progress.value - 0.15f - index * 0.05f) / 0.3f).coerceIn(0f, 1f)
    alpha = a
    translationY = (1f - a) * 14.dp.toPx()
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier, p: HomePalette) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = p.textMuted,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun DrawerRow(
    p: HomePalette,
    entry: DrawerEntry,
    showDivider: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val rowBg by animateColorAsState(
        targetValue = if (pressed) p.text.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "drawerRowBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "drawerRowScale"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(18.dp))
                .background(rowBg)
                .clickable(interactionSource = source, indication = null) {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    onClick()
                }
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.text.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(entry.icon, contentDescription = null, tint = p.text, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.subtitle,
                    fontSize = 12.sp,
                    color = p.textSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = p.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 66.dp, end = 14.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(p.text.copy(alpha = 0.10f))
            )
        }
    }
}

@Composable
private fun CommunityButton(
    p: HomePalette,
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "communityPress"
    )
    Row(
        modifier = modifier
            .height(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(p.text.copy(alpha = if (pressed) 0.16f else 0.09f))
            .border(0.8.dp, p.text.copy(alpha = 0.14f), CircleShape)
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.text)
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        // No browser / app can open the link.
    }
}
