package com.nyyrox.aniverse.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import com.nyyrox.aniverse.ui.screens.HomePalette
import com.nyyrox.aniverse.ui.screens.rememberHomePalette
import com.styropyr0.prismal.LocalPrismalBackdrop
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.emptyGlassSource
import kotlin.math.roundToInt

private const val DISCORD_INVITE_URL = "https://discord.gg/kHxHzZtH7"
private const val TELEGRAM_URL = "https://t.me/StreamverseOfc"


@Composable
fun SidebarButton(
    color: Color,
    openProgress: () -> Float,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebarBtnScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = enabled) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val progress = openProgress()
        Canvas(modifier = Modifier.size(20.dp)) {
            val strokeWidth = 1.8.dp.toPx()
            val w = this.size.width
            val h = this.size.height
            val r = 3.dp.toPx()

            if (progress > 0.5f) {
                val pad = 4.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(pad, pad),
                    end = Offset(w - pad, h - pad),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w - pad, pad),
                    end = Offset(pad, h - pad),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                val padX = 2.dp.toPx()
                val padY = 3.dp.toPx()
                val rectW = w - padX * 2
                val rectH = h - padY * 2
                drawRoundRect(
                    color = color,
                    topLeft = Offset(padX, padY),
                    size = androidx.compose.ui.geometry.Size(rectW, rectH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
                val divX = padX + rectW * 0.35f
                drawLine(
                    color = color,
                    start = Offset(divX, padY + strokeWidth * 0.5f),
                    end = Offset(divX, padY + rectH - strokeWidth * 0.5f),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

private data class DrawerEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val target: String
)

private val DrawerEntries = listOf(
    DrawerEntry(Icons.Default.Explore, "Browse Catalog", "Genres, seasons & year", "browse"),
    DrawerEntry(Icons.Default.Schedule, "Airing Schedule", "Weekly release calendar", "airing"),
    DrawerEntry(Icons.Default.Bookmark, "My Library", "Saved anime, manga & novels", "library"),
    DrawerEntry(Icons.Default.DownloadDone, "Offline Downloads", "Read and watch offline", "downloads"),
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
    val fraction by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "topBarMorph"
    )

    val scrim = p.gradTop

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Top gradient scrim for status bar readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to scrim.copy(alpha = 0.90f),
                            0.55f to scrim.copy(alpha = 0.50f),
                            1f to scrim.copy(alpha = 0f)
                        ),
                        alpha = fraction.coerceIn(0f, 1f)
                    )
                }
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 16.dp
                )
        ) {
            val fullWidth = maxWidth
            val barHeight = 44.dp
            val pillWidth = lerp(barHeight, fullWidth, fraction.coerceIn(0f, 1f))

            // Solid black / white shade (NO liquid glass)
            val solidBarBg = if (p.dark) Color(0xFF101013) else Color(0xFFFFFFFF)
            val solidBarBorder = if (p.dark) Color(0x33FFFFFF) else Color(0x1F000000)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(pillWidth)
                    .height(barHeight)
                    .shadow(if (p.dark) 8.dp else 4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(solidBarBg)
                    .border(1.dp, solidBarBorder, CircleShape)
            ) {
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
                        SidebarButton(
                            color = p.text,
                            openProgress = { if (isMenuOpen) 1f else 0f },
                            onClick = onMenuClick,
                            enabled = expanded
                        )
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


/**
 * Real Left Sidebar Drawer: slides smoothly from the left edge of the screen.
 * Solid black / white shade (NO liquid glass).
 */
@Composable
fun AniverseSidebarDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    userAvatarUrl: String?,
    avatarDecorationId: String?,
    avatarFrameId: String?,
    displayName: String = "Zenkai User",
    username: String = "",
    onNavigate: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val p = rememberHomePalette()
    val context = LocalContext.current

    BackHandler(enabled = isOpen) { onClose() }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (p.dark) 0.65f else 0.40f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(295.dp)
                    .align(Alignment.CenterStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .shadow(16.dp)
                    .background(if (p.dark) Color(0xFF101013) else Color(0xFFFFFFFF))
                    .border(
                        1.dp,
                        if (p.dark) Color(0x2EFFFFFF) else Color(0x1A000000)
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: User identity & close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onClose()
                                    onProfileClick()
                                }
                        ) {
                            OttProfileBubble(
                                p = p,
                                avatarUrl = userAvatarUrl,
                                decorationId = avatarDecorationId,
                                frameId = avatarFrameId,
                                onClick = {
                                    onClose()
                                    onProfileClick()
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName.ifBlank { "Zenkai" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (username.isNotBlank()) "@${username.removePrefix("@")}" else "View Profile",
                                    fontSize = 12.sp,
                                    color = p.textSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(p.text.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Sidebar",
                                tint = p.text,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = p.glassBorder, thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "EXPLORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textSub,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    val navItems = listOf(
                        DrawerEntry(Icons.Default.Explore, "Home", "Featured & trending anime", "home"),
                        DrawerEntry(Icons.Default.Movie, "Movies & TV", "Full anime film catalogue", "movies"),
                        DrawerEntry(Icons.Default.Explore, "Browse", "Filter by genres, season & year", "browse"),
                        DrawerEntry(Icons.Default.Schedule, "Schedule", "Upcoming & airing schedule", "airing"),
                        DrawerEntry(Icons.Default.Bookmark, "Library", "Saved watchlist & watch history", "library"),
                        DrawerEntry(Icons.Default.Explore, "Manga", "Read chapters & bookmarks", "manga"),
                        DrawerEntry(Icons.Default.Explore, "Light Novels", "Read translated light novels", "novel")
                    )

                    navItems.forEach { entry ->
                        SidebarNavRow(
                            entry = entry,
                            p = p,
                            onClick = {
                                onClose()
                                onNavigate(entry.target)
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = p.glassBorder, thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "COMMUNITY & LINKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textSub,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CommunityButton(
                            p = p,
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = "Discord",
                            modifier = Modifier.weight(1f),
                            onClick = { openUrl(context, DISCORD_INVITE_URL) }
                        )
                        CommunityButton(
                            p = p,
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = "Telegram",
                            modifier = Modifier.weight(1f),
                            onClick = { openUrl(context, TELEGRAM_URL) }
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SidebarNavRow(
    entry: DrawerEntry,
    p: HomePalette,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebarRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) p.text.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.text.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.icon, contentDescription = null, tint = p.text, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.subtitle,
                fontSize = 11.sp,
                color = p.textSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
