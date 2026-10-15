package com.nyyrox.aniverse.ui.components

import android.content.Intent
import com.nyyrox.aniverse.ui.theme.OttInk
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OttNavDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF50D1018),
        contentColor = OttTextPrimary,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header: Brand & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ZenkaiLogo(size = 22)

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.09f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OttTextPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Navigation Sections
            Text("EXPLORE & SECTIONS", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = OttTextSecondary, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            DrawerNavItem(
                icon = Icons.Default.Explore,
                title = "Browse Catalog",
                subtitle = "Filter anime by genres, seasons & year",
                onClick = {
                    onDismiss()
                    onNavigate("browse")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Schedule,
                title = "Airing Schedule",
                subtitle = "Weekly broadcast calendar & release times",
                onClick = {
                    onDismiss()
                    onNavigate("airing")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Bookmark,
                title = "Watchlist & Library",
                subtitle = "Saved anime, manga & light novels",
                onClick = {
                    onDismiss()
                    onNavigate("watchlist")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.DownloadDone,
                title = "Offline Downloads",
                subtitle = "Read novels and manga chapters offline",
                onClick = {
                    onDismiss()
                    onNavigate("downloads")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Forum,
                title = "Discord Community",
                subtitle = "Join our Discord server & enable Rich Presence",
                onClick = {
                    onDismiss()
                    onNavigate("discord_hub")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Groups,
                title = "Watch Together",
                subtitle = "Host or join live anime sync watch rooms",
                onClick = {
                    onDismiss()
                    onNavigate("watchtogether")
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Person,
                title = "Profile & Settings",
                subtitle = "Display themes, playback and preferences",
                onClick = {
                    onDismiss()
                    onNavigate("profile")
                }
            )

            Spacer(Modifier.height(14.dp))

            // Community Links
            Text("OFFICIAL COMMUNITY", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = OttTextSecondary, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            ZenkaiCommunityRow(
                telegramUrl = "https://t.me/StreamverseOfc",
                discordUrl = "https://discord.gg/aXTfS6XGg"
            )

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OttInk(0.06f))
            .border(0.8.dp, OttInk(0.09f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OttInk(0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = OttTextPrimary, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = OttTextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = OttTextSecondary)
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = OttInk(0.38f), modifier = Modifier.size(14.dp))
    }
}
