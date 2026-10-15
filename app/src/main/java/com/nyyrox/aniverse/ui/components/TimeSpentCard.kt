package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.AnimatedContent
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.ActivityTimeStats
import com.nyyrox.aniverse.data.TimeSpentRecord
import com.nyyrox.aniverse.data.getAllTimeStats
import com.nyyrox.aniverse.data.getDailyStats
import com.nyyrox.aniverse.data.getWeeklyStats
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextSecondary

enum class TimeSpentTab(val label: String, val subtitle: String) {
    TODAY("Today", "Today's Activity"),
    WEEKLY("This Week", "Weekly Reset Tracker"),
    ALL_TIME("All Time", "Lifetime Zenkai Journey")
}

@Composable
fun TimeSpentSectionCard(
    record: TimeSpentRecord,
    onResetStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TimeSpentTab.TODAY) }
    var showResetDialog by remember { mutableStateOf(false) }

    val activeStats: ActivityTimeStats = remember(record, selectedTab) {
        when (selectedTab) {
            TimeSpentTab.TODAY -> record.getDailyStats()
            TimeSpentTab.WEEKLY -> record.getWeeklyStats()
            TimeSpentTab.ALL_TIME -> record.getAllTimeStats()
        }
    }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Reset Usage Stats?", color = OttTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "This will reset your tracked active app time, anime, manga, and novel reading time to 0 for this account locally and on the cloud. This cannot be undone.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetStats?.invoke()
                    }
                ) {
                    Text("Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OttInk(0.08f))
            .border(1.dp, OttInk(0.10f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Header Row: Title and Timeframe Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.09f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time Spent",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TIME SPENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = selectedTab.subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Segment Tabs Pill (Glassmorphic & No line wrapping)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OttInk(0.09f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSpentTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) Color(0xFFE11D48) else Color.Transparent
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OttTextPrimary else Color(0xFF94A3B8),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Time Counter Hero
        AnimatedContent(
            targetState = activeStats,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "TotalTimeAnim"
        ) { stats ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = stats.formattedTotal,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = OttTextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Total Active Time",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Scaled EXP Indicator (Subtle Glass)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OttInk(0.09f))
                            .border(0.8.dp, OttInk(0.16f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Active EXP On",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ratio Segment Progress Bar
                if (stats.totalSeconds > 0) {
                    val animeWeight = stats.animeRatio.coerceAtLeast(0.001f)
                    val mangaWeight = stats.mangaRatio.coerceAtLeast(0.001f)
                    val novelWeight = stats.novelRatio.coerceAtLeast(0.001f)
                    val appWeight = stats.appRatio.coerceAtLeast(0.001f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OttInk(0.14f))
                    ) {
                        Row(modifier = Modifier.matchParentSize()) {
                            if (stats.animeRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(animeWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFFE11D48))
                                )
                            }
                            if (stats.mangaRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(mangaWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF94A3B8))
                                )
                            }
                            if (stats.novelRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(novelWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF64748B))
                                )
                            }
                            if (stats.appRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(appWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF475569))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4 Category Metric Tiles (2x2 Grid with Clean Neutral Glassmorphism)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityStatTile(
                    title = "Anime Player",
                    timeText = activeStats.formattedAnime,
                    expRate = "+20 EXP/m",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
                ActivityStatTile(
                    title = "Manga Reader",
                    timeText = activeStats.formattedManga,
                    expRate = "+15 EXP/m",
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityStatTile(
                    title = "Novel Reader",
                    timeText = activeStats.formattedNovel,
                    expRate = "+15 EXP/m",
                    icon = Icons.Default.AutoStories,
                    modifier = Modifier.weight(1f)
                )
                ActivityStatTile(
                    title = "Exploring App",
                    timeText = activeStats.formattedApp,
                    expRate = "+4 EXP/m",
                    icon = Icons.Default.Explore,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (onResetStats != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OttInk(0.08f))
                        .border(1.dp, OttInk(0.13f), RoundedCornerShape(10.dp))
                        .clickable { showResetDialog = true }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Stats",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Reset Stats",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityStatTile(
    title: String,
    timeText: String,
    expRate: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OttInk(0.06f))
            .border(1.dp, OttInk(0.09f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(OttInk(0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )

                Text(
                    text = expRate,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun PublicTimeSpentShowcase(
    timeSpentAnime: Long,
    timeSpentManga: Long,
    timeSpentNovel: Long,
    timeSpentApp: Long,
    timeSpentTotal: Long,
    weeklyTimeSpent: Long,
    dailyTimeSpent: Long,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TimeSpentTab.ALL_TIME) }

    val totalSec = when (selectedTab) {
        TimeSpentTab.TODAY -> dailyTimeSpent
        TimeSpentTab.WEEKLY -> weeklyTimeSpent
        TimeSpentTab.ALL_TIME -> if (timeSpentTotal > 0) timeSpentTotal else (timeSpentAnime + timeSpentManga + timeSpentNovel + timeSpentApp)
    }

    val formattedTotal = com.nyyrox.aniverse.data.formatTimeDuration(totalSec)
    val formattedAnime = com.nyyrox.aniverse.data.formatTimeDuration(timeSpentAnime)
    val formattedManga = com.nyyrox.aniverse.data.formatTimeDuration(timeSpentManga)
    val formattedNovel = com.nyyrox.aniverse.data.formatTimeDuration(timeSpentNovel)
    val formattedApp = com.nyyrox.aniverse.data.formatTimeDuration(timeSpentApp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OttInk(0.08f))
            .border(1.dp, OttInk(0.10f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.09f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time Spent",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "TIME SPENT STATS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = selectedTab.subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Tabs (Glassmorphic & No line wrapping)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OttInk(0.09f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSpentTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) Color(0xFFE11D48) else Color.Transparent
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OttTextPrimary else Color(0xFF94A3B8),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column {
            Text(
                text = formattedTotal,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = OttTextPrimary
            )
            Text(
                text = "Total Recorded Activity",
                fontSize = 10.5.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Breakdown tiles (Neutral Glassmorphic)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityStatTile(
                    title = "Anime Player",
                    timeText = formattedAnime,
                    expRate = "Active",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
                ActivityStatTile(
                    title = "Manga Reader",
                    timeText = formattedManga,
                    expRate = "Active",
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityStatTile(
                    title = "Novel Reader",
                    timeText = formattedNovel,
                    expRate = "Active",
                    icon = Icons.Default.AutoStories,
                    modifier = Modifier.weight(1f)
                )
                ActivityStatTile(
                    title = "Exploring App",
                    timeText = formattedApp,
                    expRate = "Active",
                    icon = Icons.Default.Explore,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}