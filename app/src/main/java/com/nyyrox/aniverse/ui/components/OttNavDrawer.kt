package com.nyyrox.aniverse.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.ui.screens.HomePalette
import com.nyyrox.aniverse.ui.screens.rememberHomePalette
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
    DrawerEntry(Icons.Default.Explore, "Browse Catalog", "Filter anime by genres, seasons & year", "browse"),
    DrawerEntry(Icons.Default.Schedule, "Airing Schedule", "Weekly broadcast calendar & release times", "airing"),
    DrawerEntry(Icons.Default.Bookmark, "Watchlist & Library", "Saved anime, manga & light novels", "watchlist"),
    DrawerEntry(Icons.Default.DownloadDone, "Offline Downloads", "Read novels and manga chapters offline", "downloads"),
    DrawerEntry(Icons.Default.Forum, "Discord Community", "Join our Discord server & enable Rich Presence", "discord_hub"),
    DrawerEntry(Icons.Default.Groups, "Watch Together", "Host or join live anime sync watch rooms", "watchtogether"),
    DrawerEntry(Icons.Default.Person, "Profile & Settings", "Display themes, playback and preferences", "profile")
)

/**
 * Menu. The hamburger pill in the top bar grows downward into this card (pill -> card) while its three lines
 * turn into an X; closing plays the same motion backwards into the pill. Rows slide in one after another.
 * Glass on -> translucent liquid-glass card, glass off -> flat card. Both follow light / dark.
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
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = if (p.dark) 0.55f else 0.32f))
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

private fun Modifier.panelSurface(p: HomePalette, shape: Shape): Modifier = this
    .clip(shape)
    .background(
        if (p.glassOn) {
            Brush.verticalGradient(
                if (p.dark) listOf(Color(0xF01B1E2A), Color(0xF00D0F16))
                else listOf(Color(0xF8FFFFFF), Color(0xF3F1F2F8))
            )
        } else {
            SolidColor(if (p.dark) Color(0xFF12141C) else Color(0xFFF7F8FB))
        }
    )
    .border(
        1.dp,
        if (p.glassOn) {
            Brush.verticalGradient(
                if (p.dark) listOf(Color(0x55FFFFFF), Color(0x0FFFFFFF))
                else listOf(Color(0xFFFFFFFF), Color(0x22000000))
            )
        } else {
            SolidColor(p.glassBorder)
        },
        shape
    )

/** Fades and lifts a row in once the panel is far enough open; [index] sets the order. */
private fun Modifier.stagger(progress: State<Float>, index: Int): Modifier = graphicsLayer {
    val a = ((progress.value - 0.15f - index * 0.04f) / 0.3f).coerceIn(0f, 1f)
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
    // 28dp is more than half of the 44dp collapsed height, so it renders as a full pill that turns into a card.
    val shape = RoundedCornerShape(28.dp)

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 24.dp.toPx() * progress.value.coerceIn(0f, 1f)
                this.shape = shape
                clip = false
            }
            .panelSurface(p, shape)
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
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("EXPLORE & SECTIONS", Modifier.stagger(progress, 0), p)

                    DrawerEntries.forEachIndexed { i, entry ->
                        DrawerNavItem(
                            p = p,
                            entry = entry,
                            modifier = Modifier.stagger(progress, i + 1),
                            onClick = {
                                onDismiss()
                                onNavigate(entry.target)
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    SectionLabel("OFFICIAL COMMUNITY", Modifier.stagger(progress, DrawerEntries.size + 1), p)

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
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Black,
        color = p.textMuted,
        letterSpacing = 1.sp,
        modifier = modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
    )
}

/** Glass (or flat) row with a spring press. Used for menu rows and community buttons. */
@Composable
private fun PressRow(
    p: HomePalette,
    shape: Shape,
    modifier: Modifier,
    onClick: () -> Unit,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "drawerRowPress"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .ottGlass(p, shape)
            .clickable(interactionSource = source, indication = null) {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            },
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun DrawerNavItem(
    p: HomePalette,
    entry: DrawerEntry,
    modifier: Modifier,
    onClick: () -> Unit
) {
    PressRow(
        p = p,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(p.text.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.icon, contentDescription = null, tint = p.text, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.text)
            Text(text = entry.subtitle, fontSize = 11.5.sp, color = p.textSub)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = p.textMuted,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .size(15.dp)
        )
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
    PressRow(
        p = p,
        shape = CircleShape,
        modifier = modifier.height(50.dp),
        onClick = onClick,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = p.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = p.text)
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
