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
import androidx.compose.foundation.layout.Column
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
 * Menu. The hamburger pill in the top bar grows downward into a Prismal liquid-glass card while its three lines
 * turn into an X; closing plays the same motion backwards into the pill. Rows slide in one after another.
 * Glass on -> real liquid glass (same recipe as the dock), glass off -> flat card. Both follow light / dark.
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
    val p = rememberHomePalette()
    val progress = animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f),
        label = "drawerProgress"
    )
    BackHandler(enabled = isOpen) { onDismiss() }
    if (!isOpen && progress.value < 0.005f) return

    Box(modifier = Modifier.fillMaxSize()) {
        // Light scrim, so the glass card still sees the page behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = if (p.dark) 0.40f else 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = isOpen
                ) { onDismiss() }
        )

        // Same origin as the top bar pill: status bar + 6dp, 16dp side margins.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            DrawerPanel(
                p = p,
                progress = progress,
                onDismiss = onDismiss,
                onNavigate = onNavigate,
                avatarUrl = userAvatarUrl,
                decorationId = avatarDecorationId,
                frameId = avatarFrameId
            )
        }
    }
}

/** Fades and lifts a row in once the panel is far enough open; [index] sets the order. */
private fun Modifier.stagger(progress: State<Float>, index: Int): Modifier = graphicsLayer {
    val a = ((progress.value - 0.15f - index * 0.05f) / 0.3f).coerceIn(0f, 1f)
    alpha = a
    translationY = (1f - a) * 14.dp.toPx()
}

@Composable
private fun DrawerPanel(
    p: HomePalette,
    progress: State<Float>,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    avatarUrl: String?,
    decorationId: String?,
    frameId: String?
) {
    val context = LocalContext.current
    val barHeightPx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val backdrop = LocalPrismalBackdrop.current ?: emptyGlassSource()

    // Same material as Prismal's own glass menu: neutral, readable, and still see-through.
    val surface = if (p.dark) Color(0xFF1C1C1E).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.70f)
    val glass = rememberOttGlass(
        backdrop = backdrop,
        p = p,
        shape = DrawerGlassShape,
        refractionHeight = 22.dp,
        refractionAmount = 22.dp,
        surfaceColor = surface
    )
    val cardShape = RoundedCornerShape(28.dp)
    val cardModifier = if (p.glassOn) {
        glass.clip(cardShape)
    } else {
        Modifier
            .clip(cardShape)
            .background(if (p.dark) Color(0xFF1A1A1C) else Color(0xFFF7F7F9))
            .border(0.8.dp, p.glassBorder, cardShape)
    }

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardModifier)
            // Swallow taps on the card so they do not reach the scrim.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: lines -> X, logo, profile. Lines up with the top bar pill underneath.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuLinesButton(
                        color = p.text,
                        openProgress = { progress.value },
                        onClick = onDismiss
                    )
                    Spacer(Modifier.width(8.dp))
                    AniverseLogo(showIcon = false, tint = if (p.dark) null else p.text)
                    Spacer(Modifier.weight(1f))
                    OttProfileBubble(
                        p = p,
                        avatarUrl = avatarUrl,
                        decorationId = decorationId,
                        frameId = frameId,
                        onClick = {
                            onDismiss()
                            onNavigate("profile")
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("EXPLORE", Modifier.stagger(progress, 0), p)

                    // One grouped list instead of seven separate cards.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .stagger(progress, 1)
                            .clip(RoundedCornerShape(22.dp))
                            .background(p.text.copy(alpha = 0.06f))
                            .padding(vertical = 4.dp)
                    ) {
                        DrawerEntries.forEachIndexed { i, entry ->
                            DrawerRow(
                                p = p,
                                entry = entry,
                                showDivider = i != DrawerEntries.lastIndex,
                                modifier = Modifier.stagger(progress, 1 + i),
                                onClick = {
                                    onDismiss()
                                    onNavigate(entry.target)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    SectionLabel("COMMUNITY", Modifier.stagger(progress, DrawerEntries.size + 1), p)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .stagger(progress, DrawerEntries.size + 2),
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
        val t = progress.value.coerceIn(0f, 1f)
        val h = (collapsed + (placeable.height - collapsed) * t).roundToInt()
        layout(placeable.width, h) { placeable.place(0, 0) }
    }
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
