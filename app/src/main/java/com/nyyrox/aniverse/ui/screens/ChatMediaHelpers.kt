package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.giphy.GifItem
import com.nyyrox.aniverse.data.giphy.GiphyRepository
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import kotlinx.coroutines.delay

data class AnimeRecommendationData(
    val animeId: Int,
    val title: String,
    val cover: String,
    val score: String,
    val format: String,
    val comment: String
)

fun parseAnimeRecommendation(text: String): AnimeRecommendationData? {
    if (!text.startsWith("[ANIME_REC|")) return null
    val endIdx = text.indexOf("]")
    if (endIdx == -1) return null
    val payload = text.substring("[ANIME_REC|".length, endIdx)
    val comment = text.substring(endIdx + 1).trim()
    val parts = payload.split("|")
    return AnimeRecommendationData(
        animeId = parts.getOrNull(0)?.toIntOrNull() ?: 0,
        title = parts.getOrNull(1) ?: "Anime",
        cover = parts.getOrNull(2) ?: "",
        score = parts.getOrNull(3) ?: "",
        format = parts.getOrNull(4) ?: "TV",
        comment = comment
    )
}

data class GifCommentData(
    val url: String,
    val title: String,
    val comment: String
)

data class ImageCommentData(
    val url: String,
    val comment: String
)

fun parseImageComment(text: String): ImageCommentData? {
    val clean = text.replace("[SPOILER]", "").trim()
    if (clean.contains("[IMG|") || clean.contains("[IMAGE|")) {
        val startTag = if (clean.contains("[IMG|")) "[IMG|" else "[IMAGE|"
        val startIdx = clean.indexOf(startTag)
        val endIdx = clean.indexOf("]", startIdx)
        if (endIdx != -1) {
            val payload = clean.substring(startIdx + startTag.length, endIdx)
            val before = clean.substring(0, startIdx).trim()
            val after = clean.substring(endIdx + 1).trim()
            val comment = listOf(before, after).filter { it.isNotBlank() }.joinToString(" ")
            val url = payload.split("|").getOrNull(0)?.trim()
            if (!url.isNullOrBlank()) {
                return ImageCommentData(url = url, comment = comment)
            }
        }
    }
    val words = clean.split("\\s+".toRegex())
    val imgUrl = words.firstOrNull { word ->
        (word.startsWith("http://") || word.startsWith("https://")) &&
        (word.endsWith(".png", true) || word.endsWith(".jpg", true) || word.endsWith(".jpeg", true) || word.endsWith(".webp", true)) &&
        !word.contains(".gif", true)
    }
    if (imgUrl != null) {
        val comment = words.filter { it != imgUrl }.joinToString(" ").trim()
        return ImageCommentData(url = imgUrl, comment = comment)
    }
    return null
}

fun parseGifComment(text: String): GifCommentData? {
    val clean = text.replace("[SPOILER]", "").trim()
    if (clean.contains("[GIF|")) {
        val startIdx = clean.indexOf("[GIF|")
        val endIdx = clean.indexOf("]", startIdx)
        if (endIdx != -1) {
            val payload = clean.substring(startIdx + "[GIF|".length, endIdx)
            val before = clean.substring(0, startIdx).trim()
            val after = clean.substring(endIdx + 1).trim()
            val comment = listOf(before, after).filter { it.isNotBlank() }.joinToString(" ")
            val parts = payload.split("|")
            val url = parts.getOrNull(0)?.trim()
            val title = parts.getOrNull(1)?.trim().takeIf { !it.isNullOrBlank() } ?: "GIF"
            if (!url.isNullOrBlank()) {
                return GifCommentData(
                    url = url,
                    title = title,
                    comment = comment
                )
            }
        }
    }
    val words = clean.split("\\s+".toRegex())
    val gifUrl = words.firstOrNull { word ->
        (word.startsWith("http://") || word.startsWith("https://")) &&
        (word.contains(".gif", true) || word.contains("tenor.com", true) || word.contains("giphy.com", true))
    }
    if (gifUrl != null) {
        val comment = words.filter { it != gifUrl }.joinToString(" ").trim()
        val title = if (comment.isNotBlank()) comment else "GIF"
        return GifCommentData(
            url = gifUrl,
            title = title,
            comment = comment
        )
    }
    return null
}

@Composable
fun ChatDateHeader(
    dateText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(OttPillShape)
                .background(Color(0xFF131722).copy(alpha = 0.92f))
                .border(1.dp, Color(0x30FFFFFF), OttPillShape)
                .padding(horizontal = 14.dp, vertical = 4.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dateText,
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerModal(
    onDismiss: () -> Unit,
    onSelectGif: (GifItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Trending") }
    var gifs by remember { mutableStateOf<List<GifItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedCategory) {
        isLoading = true
        gifs = if (searchQuery.isNotBlank()) {
            delay(400)
            GiphyRepository.searchGifs(searchQuery)
        } else {
            GiphyRepository.getCategoryGifs(selectedCategory)
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F121B),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Anime GIFs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search anime GIFs via Giphy...", color = OttTextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF161B26),
                    unfocusedContainerColor = Color(0xFF121520),
                    focusedBorderColor = Color(0xFFE11D48),
                    unfocusedBorderColor = Color(0x28FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (searchQuery.isBlank()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GiphyRepository.TOP_ANIME_CATEGORIES) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFE11D48) else Color(0x18FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF4D6D) else Color(0x28FFFFFF),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE11D48))
                }
            } else if (gifs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No GIFs found",
                        color = OttTextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(gifs, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E2330))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { onSelectGif(item) }
                        ) {
                            AsyncImage(
                                model = item.url,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

