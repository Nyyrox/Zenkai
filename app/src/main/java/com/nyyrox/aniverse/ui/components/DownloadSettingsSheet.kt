package com.nyyrox.aniverse.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.download.DownloadPreferences
import com.nyyrox.aniverse.data.download.DownloadStorageLocation
import com.nyyrox.aniverse.ui.theme.OttAccentRed
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val settings by DownloadPreferences.settings.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C0D12),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Download Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Text(
                            text = "Queue concurrency & storage destination",
                            fontSize = 12.sp,
                            color = OttTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OttTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Section 1: Concurrent Download Queue Limit ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0x10FFFFFF))
                    .border(1.dp, OttSurfaceBorder, OttCardShape)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = OttAccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Simultaneous Downloads",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Controls how many episodes download simultaneously. Extra episodes wait in the queue.",
                    fontSize = 11.5.sp,
                    color = OttTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                // Concurrency selector pills: 1, 2, 3, 5
                val options = listOf(1, 2, 3, 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { count ->
                        val isSelected = settings.maxConcurrentDownloads == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) OttAccentRed else Color(0x16FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) OttAccentRed else Color(0x28FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    DownloadPreferences.setMaxConcurrentDownloads(context, count)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else OttTextSecondary
                                )
                                Text(
                                    text = if (count == 1) "at a time" else "parallel",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFEE2E2) else Color(0x80FFFFFF)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                val hint = when (settings.maxConcurrentDownloads) {
                    1 -> "Best for cellular data or slower connections to ensure smooth progress."
                    2 -> "Recommended balance for good speed without saturating your network."
                    3 -> "Ideal for fast Wi-Fi connections when queueing anime seasons."
                    else -> "Maximum speed for ultra-fast fiber connections."
                }
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 2: Storage Destination ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0x10FFFFFF))
                    .border(1.dp, OttSurfaceBorder, OttCardShape)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Storage Destination",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select where downloaded anime episodes are saved on this device.",
                    fontSize = 11.5.sp,
                    color = OttTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                // Option A: App Internal Storage (Private)
                val isInternal = settings.storageLocation == DownloadStorageLocation.INTERNAL
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isInternal) Color(0x22FFFFFF) else Color(0x0CFFFFFF))
                        .border(
                            1.dp,
                            if (isInternal) Color(0x60FFFFFF) else Color(0x1FFFFFFF),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.INTERNAL)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isInternal) Color.White else OttTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Internal Storage (Private)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isInternal) Color.White else OttTextSecondary
                            )
                            Text(
                                text = "Secure, private app sandbox. Clean and automatic cleanup on uninstall.",
                                fontSize = 10.5.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }

                        if (isInternal) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Option B: External Public Storage (Movies/Zenkai)
                val isExternal = settings.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExternal) Color(0x22FFFFFF) else Color(0x0CFFFFFF))
                        .border(
                            1.dp,
                            if (isExternal) Color(0x60FFFFFF) else Color(0x1FFFFFFF),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            DownloadPreferences.setStorageLocation(context, DownloadStorageLocation.EXTERNAL_PUBLIC)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = if (isExternal) Color.White else OttTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Device Movies Folder (Public)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isExternal) Color.White else OttTextSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(OttPillShape)
                                        .background(Color(0x2A10B981))
                                        .border(0.8.dp, Color(0x6010B981), OttPillShape)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "VLC / Gallery",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                            Text(
                                text = "Saved to Movies/Zenkai. Playable in VLC, MX Player, and easy USB PC transfer.",
                                fontSize = 10.5.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }

                        if (isExternal) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 3: Notification Alerts ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0x10FFFFFF))
                    .border(1.dp, OttSurfaceBorder, OttCardShape)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (settings.notificationsEnabled) Color(0xFFFBBF24) else OttTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Download Notifications",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Live progress bar, download speed, and completion alerts",
                                fontSize = 11.sp,
                                color = OttTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = {
                            DownloadPreferences.setNotificationsEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OttAccentRed,
                            uncheckedThumbColor = Color(0xFFA0A4B2),
                            uncheckedTrackColor = Color(0x30FFFFFF)
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
