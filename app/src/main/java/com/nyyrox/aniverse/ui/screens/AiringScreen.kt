package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Airing
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import com.nyyrox.aniverse.ui.components.AiringListSkeleton
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ScheduleDay(
    val dayName: String,
    val dateNumber: String,
    val monthFormatted: String,
    val fullDateFormatted: String,
    val displayHeader: String,
    val startOfDaySec: Long,
    val endOfDaySec: Long,
    val isToday: Boolean
)

@Composable
fun AiringScreen(
    vm: MainViewModel,
    onAnimeClick: (Int) -> Unit
) {
    LaunchedEffect(Unit) {
        if (vm.airing.isEmpty()) {
            vm.loadAiring()
        }
    }

    if (vm.airingLoading && vm.airing.isEmpty()) {
        AiringListSkeleton()
        return
    }

    if (vm.airingError != null && vm.airing.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OttBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Airing Schedule Unavailable",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = vm.airingError ?: "Unable to fetch schedule.",
                    fontSize = 13.sp,
                    color = OttTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.loadAiring() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = OttPillShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // 5 days in the past (e.g. 4th, 5th, 6th, 7th...) + Today (8th) + 8 days upcoming (9th..16th)
    val days = remember {
        val list = mutableListOf<ScheduleDay>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStartSec = cal.timeInMillis / 1000

        // Start from 5 days ago
        cal.add(Calendar.DAY_OF_YEAR, -5)

        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val numFormat = SimpleDateFormat("dd", Locale.US)
        val monthFormat = SimpleDateFormat("MMM", Locale.US)
        val fullDateFormat = SimpleDateFormat("MMM dd", Locale.US)
        val headerFormat = SimpleDateFormat("EEEE, MMM d", Locale.US)

        for (i in -5..8) {
            val startSec = cal.timeInMillis / 1000
            val dayName = dayFormat.format(cal.time)
            val dateNumber = numFormat.format(cal.time)
            val monthFormatted = monthFormat.format(cal.time)
            val fullDate = fullDateFormat.format(cal.time)
            val displayHeader = headerFormat.format(cal.time)
            val isToday = startSec == todayStartSec

            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endSec = (cal.timeInMillis / 1000) - 1
            list.add(
                ScheduleDay(
                    dayName = dayName,
                    dateNumber = dateNumber,
                    monthFormatted = monthFormatted,
                    fullDateFormatted = fullDate,
                    displayHeader = displayHeader,
                    startOfDaySec = startSec,
                    endOfDaySec = endSec,
                    isToday = isToday
                )
            )
        }
        list
    }

    val todayIndex = remember(days) {
        val idx = days.indexOfFirst { it.isToday }
        if (idx >= 0) idx else 0
    }
    var selectedDayIndex by remember(todayIndex) { mutableIntStateOf(todayIndex) }
    val selectedDay = days.getOrNull(selectedDayIndex) ?: days.getOrNull(todayIndex)

    val rowListState = rememberLazyListState()
    LaunchedEffect(todayIndex) {
        if (todayIndex > 0) {
            // Scroll so today is visible, with previous dates to the left and upcoming dates to the right
            rowListState.scrollToItem((todayIndex - 1).coerceAtLeast(0))
        }
    }

    // Filter airing anime for selected day - never fall back to other days
    val dayAiring = remember(vm.airing, selectedDay) {
        if (selectedDay == null) emptyList()
        else {
            vm.airing.filter { it.at in selectedDay.startOfDaySec..selectedDay.endOfDaySec }
                .sortedBy { it.at }
        }
    }

    // Group items by hour marker
    val groupedByHour = remember(dayAiring) {
        val hourFormat = SimpleDateFormat("HH:00", Locale.getDefault())
        dayAiring.groupBy { airing ->
            hourFormat.format(Date(airing.at * 1000))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Airing Schedule",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = selectedDay?.let { "${it.displayHeader} • ${dayAiring.size} anime" } ?: "Broadcasting Anime",
                    fontSize = 12.sp,
                    color = OttTextSecondary
                )
            }
            IconButton(
                onClick = { vm.loadAiring(forceRefresh = true) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Horizontal Day Selector Tabs
        LazyRow(
            state = rowListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(days) { index, day ->
                val isSelected = index == selectedDayIndex
                Box(
                    modifier = Modifier
                        .width(66.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isSelected -> Color.White
                                day.isToday -> Color(0xFF23252E)
                                else -> Color(0xFF16171B)
                            }
                        )
                        .border(
                            width = if (isSelected) 0.dp else if (day.isToday) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> Color.Transparent
                                day.isToday -> OttPrimary
                                else -> Color(0x18FFFFFF)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedDayIndex = index }
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = day.dayName.uppercase(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF1E1E1E) else if (day.isToday) OttPrimary else Color(0xFF888B96)
                        )
                        Text(
                            text = day.dateNumber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                        if (day.isToday) {
                            Text(
                                text = "TODAY",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color(0xFF0066FF) else OttAccentGold,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Text(
                                text = day.monthFormatted.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFF555555) else Color(0xFF6B6E7B)
                            )
                        }
                    }
                }
            }
        }

        if (dayAiring.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Scheduled Broadcasts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "No anime scheduled to air on ${selectedDay?.displayHeader ?: "this date"}.",
                        color = OttTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Timeline Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedByHour.forEach { (hourKey, itemsForHour) ->
                    item(key = "hour_header_$hourKey") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "> $hourKey",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    items(itemsForHour, key = { "airing_${it.anime.id}_${it.episode}_${it.at}" }) { airing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(Color(0x30FFFFFF))
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            TimelineAiringCard(
                                airing = airing,
                                onClick = { onAnimeClick(airing.anime.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineAiringCard(
    airing: Airing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anime = airing.anime
    val now = System.currentTimeMillis() / 1000
    val diffSeconds = airing.at - now
    val timeFormatted = remember(airing.at) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(airing.at * 1000))
    }

    val yearText = anime.seasonYear?.toString() ?: remember(airing.at) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(airing.at * 1000))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF16171B))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.title,
                modifier = Modifier
                    .size(width = 68.dp, height = 86.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = anime.displayTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "Episode ${airing.episode}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9E9E9E)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (anime.isMovie) "Movie" else anime.displayFormat,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    if (yearText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF26272C))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = yearText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(Modifier.height(18.dp))

                val statusLabel = when {
                    diffSeconds <= 0 -> "AIRED"
                    diffSeconds in 1..3600 -> "AIRING"
                    else -> "UPCOMING"
                }

                val statusColor = when (statusLabel) {
                    "AIRED" -> Color(0xFF757575)
                    "AIRING" -> OttAccentGold
                    else -> Color(0xFF888B96)
                }

                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
