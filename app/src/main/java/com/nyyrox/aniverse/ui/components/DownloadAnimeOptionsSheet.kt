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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary

data class QualityOption(
    val id: String,
    val label: String,
    val resolution: String,
    val approxSize: String
)

val QUALITY_OPTIONS = listOf(
    QualityOption("1080p", "1080p", "1920x1080", "~350 MB"),
    QualityOption("720p", "720p", "1280x720", "~200 MB"),
    QualityOption("480p", "480p", "854x480", "~110 MB"),
    QualityOption("360p", "360p", "640x360", "~65 MB"),
    QualityOption("Auto", "Auto", "Best Available", "Optimal")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadAnimeOptionsSheet(
    anime: Anime,
    episode: Int = 1,
    episodeTitle: String? = null,
    episodesRange: IntRange? = null,
    initialProvider: Provider? = null,
    availableDirectServers: List<Provider>? = null,
    onDismiss: () -> Unit,
    onStartDownload: (provider: Provider, quality: String, audio: String) -> Unit = { _, _, _ -> },
    onStartBatchDownload: ((episodes: List<Int>, provider: Provider, quality: String, audio: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val storage = remember { StorageManager(context) }

    val savedAudio = remember { storage.getLastDownloadAudio() }
    val savedQuality = remember { storage.getLastDownloadQuality() }
    val savedServerId = remember { storage.getLastDownloadServerId() }

    val initialAudioMode = if (initialProvider?.isDub == true) "DUB"
        else if (initialProvider?.isMulti == true) "MULTI"
        else if (initialProvider != null) "SUB"
        else null

    var selectedAudio by remember(initialProvider) {
        mutableStateOf(
            initialAudioMode ?: savedAudio ?: "SUB"
        )
    }
    var selectedQuality by remember {
        mutableStateOf(savedQuality ?: "720p")
    }

    val availableProviders = remember(selectedAudio, availableDirectServers) {
        val isDubSelected = selectedAudio.equals("dub", ignoreCase = true)
        val isMultiSelected = selectedAudio.equals("multi", ignoreCase = true)

        val directList = if (!availableDirectServers.isNullOrEmpty()) {
            val matching = when {
                isDubSelected -> availableDirectServers.filter { it.isDub || it.isMulti }
                isMultiSelected -> availableDirectServers.filter { it.isMulti }
                else -> availableDirectServers.filter { !it.isDub && !it.isMulti }
            }
            if (matching.isNotEmpty()) matching else {
                com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudio)
            }
        } else {
            com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudio)
        }
        // Strictly filter to ONLY available direct servers - zero embed servers
        directList.filter { it.isDirectStream && !it.isEmbedOnly }.distinctBy { it.id }
    }

    var selectedProvider by remember(selectedAudio, initialProvider, availableProviders) {
        // Priority 1: Match the current active provider by exact ID
        val matchingInit = initialProvider?.let { init ->
            availableProviders.firstOrNull { it.id == init.id }
        }
        // Priority 2: Match current active provider by name (e.g. if audio was switched from SUB to DUB)
        val matchingInitByName = initialProvider?.let { init ->
            availableProviders.firstOrNull { it.name.equals(init.name, ignoreCase = true) }
        }
        // Priority 3: Previously saved server from storage, if it exists in available direct servers
        val matchingSaved = savedServerId?.let { sId ->
            availableProviders.firstOrNull { it.id == sId }
        }
        // Priority 4: First available direct server
        val firstDirect = availableProviders.firstOrNull { it.isDirectStream }
        mutableStateOf(
            matchingInit
                ?: matchingInitByName
                ?: matchingSaved
                ?: firstDirect
                ?: availableProviders.firstOrNull()
                ?: Provider("flixcloud-sub", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OttBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OttPrimary.copy(alpha = 0.15f))
                            .border(1.dp, OttPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = OttPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        val titleText = if (episodesRange != null) {
                            "Download Episodes ${episodesRange.first}–${episodesRange.last}"
                        } else {
                            "Download Episode $episode"
                        }
                        Text(
                            text = titleText,
                            color = OttTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val subText = if (episodesRange != null) {
                            "${episodesRange.count()} episodes • ${anime.title}"
                        } else if (!episodeTitle.isNullOrBlank()) {
                            episodeTitle
                        } else {
                            anime.title
                        }
                        Text(
                            text = subText,
                            color = OttTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 1. Audio Track Section
            SectionHeader(title = "Audio Track", icon = Icons.Default.Audiotrack)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val audioOptions = listOf(
                    Triple("SUB", "Japanese / Sub", "Original Japanese with subtitles"),
                    Triple("DUB", "English / Dub", "English voice audio"),
                    Triple("MULTI", "Multi-Audio", "Multiple language tracks")
                )
                audioOptions.forEach { (key, title, _) ->
                    val isSelected = selectedAudio == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) OttPrimary.copy(alpha = 0.2f) else OttSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) OttPrimary else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedAudio = key }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = key,
                                color = if (isSelected) Color.White else OttTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = if (key == "SUB") "Subbed" else if (key == "DUB") "Dubbed" else "Multi",
                                color = if (isSelected) OttPrimary else Color(0x66FFFFFF),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. Quality Selection Section
            SectionHeader(title = "Video Quality", icon = Icons.Default.HighQuality)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUALITY_OPTIONS) { opt ->
                    val isSelected = selectedQuality == opt.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) OttPrimary.copy(alpha = 0.2f) else OttSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) OttPrimary else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedQuality = opt.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = opt.label,
                                color = if (isSelected) Color.White else OttTextSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = if (episodesRange != null && opt.id != "Auto") "${opt.approxSize}/ep" else opt.approxSize,
                                color = if (isSelected) OttPrimary else Color(0x66FFFFFF),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 3. Server Selection Section
            SectionHeader(title = "Download Server (${availableProviders.size} available)", icon = Icons.Default.Dns)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.ui.unit.Dp(240f))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableProviders.forEach { prov ->
                    val isSelected = selectedProvider.id == prov.id
                    val isMulti = prov.isMultiAudio || prov.isMulti

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) OttPrimary.copy(alpha = 0.15f) else OttSurfaceElevated)
                            .border(
                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                color = if (isSelected) OttPrimary else Color(0x18FFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedProvider = prov }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = prov.name,
                                color = if (isSelected) Color.White else OttTextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val badgeText = when {
                                isMulti -> "MULTI-AUDIO"
                                prov.serverBadge.isNotBlank() -> prov.serverBadge.uppercase()
                                else -> "DIRECT HD"
                            }
                            val badgeColor = if (isMulti) Color(0xFF38BDF8) else Color(0xFF10B981)
                            Box(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.8f), OttPillShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OttPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Captions / Subtitles Info Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1438BDF8))
                    .border(0.8.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Offline Captions Included",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Matching subtitles will download automatically for offline playback",
                            color = OttTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = {
                    storage.saveDownloadSettings(selectedAudio, selectedQuality, selectedProvider.id)
                    if (episodesRange != null && onStartBatchDownload != null) {
                        onStartBatchDownload(episodesRange.toList(), selectedProvider, selectedQuality, selectedAudio)
                    } else {
                        onStartDownload(selectedProvider, selectedQuality, selectedAudio)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OttPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                val buttonText = if (episodesRange != null) {
                    "Download ${episodesRange.count()} Episodes ($selectedQuality • $selectedAudio)"
                } else {
                    "Download ($selectedQuality • $selectedAudio)"
                }
                Text(
                    text = buttonText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OttPrimary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            color = OttTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
