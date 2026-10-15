package com.nyyrox.aniverse.ui.components

import androidx.compose.foundation.background
import com.nyyrox.aniverse.ui.theme.OttPanelHigh
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.nyyrox.aniverse.ui.theme.BangersFontFamily
import com.nyyrox.aniverse.ui.theme.CaveatFontFamily
import com.nyyrox.aniverse.ui.theme.CinzelFontFamily
import com.nyyrox.aniverse.ui.theme.KaushanFontFamily
import com.nyyrox.aniverse.ui.theme.OrbitronFontFamily
import com.nyyrox.aniverse.ui.theme.OutfitFontFamily
import com.nyyrox.aniverse.ui.theme.PlayfairFontFamily
import com.nyyrox.aniverse.ui.theme.PressStartFontFamily
import com.nyyrox.aniverse.ui.theme.RighteousFontFamily
import com.nyyrox.aniverse.ui.theme.RussoOneFontFamily

val BIO_FONT_OPTIONS = listOf(
    "default" to "Modern",
    "cyber" to "Cyberpunk",
    "gothic" to "Dark Gothic",
    "manga" to "Manga Action",
    "brush" to "Samurai Brush",
    "neon" to "Synth Neon",
    "aesthetic" to "Aesthetic",
    "retro" to "80s Retro",
    "monospace" to "Cyber Mono",
    "serif" to "Elegance Serif",
    "cursive" to "Script",
    "shonen" to "Shonen Bold"
)

fun getBioTextStyle(fontKey: String?): TextStyle {
    return when (fontKey?.lowercase()) {
        "cyber" -> TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 1.2.sp,
            color = Color(0xFF38BDF8)
        )
        "gothic" -> TextStyle(
            fontFamily = CinzelFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.8.sp,
            color = Color(0xFFF1F5F9)
        )
        "manga" -> TextStyle(
            fontFamily = BangersFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 1.0.sp,
            color = Color(0xFFFFF1F2)
        )
        "brush" -> TextStyle(
            fontFamily = KaushanFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.4.sp,
            color = Color(0xFFFEF3C7)
        )
        "neon" -> TextStyle(
            fontFamily = RighteousFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 1.1.sp,
            color = Color(0xFFE0E7FF)
        )
        "aesthetic" -> TextStyle(
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.6.sp,
            color = Color(0xFFF8FAFC)
        )
        "retro" -> TextStyle(
            fontFamily = PressStartFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 9.5.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.8.sp,
            color = Color(0xFFBBF7D0)
        )
        "serif" -> TextStyle(
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color(0xFFCBD5E1)
        )
        "monospace" -> TextStyle(
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.8.sp,
            color = Color(0xFFE2E8F0)
        )
        "cursive" -> TextStyle(
            fontFamily = CaveatFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.3.sp,
            color = Color(0xFFF1F5F9)
        )
        "shonen" -> TextStyle(
            fontFamily = RussoOneFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp,
            color = OttTextPrimary
        )
        else -> TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color(0xFFCBD5E1)
        )
    }
}

@Composable
fun BioFontSelectorPills(
    selectedFont: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "BIO TYPOGRAPHY STYLE",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = OttTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BIO_FONT_OPTIONS) { (key, label) ->
                val isSelected = selectedFont == key
                Box(
                    modifier = Modifier
                        .clip(OttPillShape)
                        .background(if (isSelected) OttPrimary.copy(alpha = 0.25f) else OttInk(0.08f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) OttPrimary else OttInk(0.16f),
                            shape = OttPillShape
                        )
                        .clickable { onFontSelected(key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) OttTextPrimary else OttTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class ParsedFavoriteAnime(
    val id: Int,
    val title: String,
    val coverImage: String,
    val score: String,
    val format: String
)

fun parseFavoriteAnimeString(raw: String): ParsedFavoriteAnime {
    if (raw.isBlank()) return ParsedFavoriteAnime(0, "", "", "", "")
    if (raw.contains("|")) {
        val parts = raw.split("|")
        return ParsedFavoriteAnime(
            id = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            title = parts.getOrNull(1)?.ifBlank { raw } ?: raw,
            coverImage = parts.getOrNull(2).orEmpty(),
            score = parts.getOrNull(3).orEmpty(),
            format = parts.getOrNull(4).orEmpty()
        )
    }
    return ParsedFavoriteAnime(0, raw, "", "", "")
}

@Composable
fun FavoriteAnimeShowcaseCard(
    favoriteAnime: String,
    onAnimeClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fav = remember(favoriteAnime) { parseFavoriteAnimeString(favoriteAnime) }
    if (fav.title.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        OttPanelHigh,
                        OttPanelHigh
                    )
                )
            )
            .border(1.dp, OttInk(0.16f), RoundedCornerShape(16.dp))
            .clickable(enabled = fav.id > 0 && onAnimeClick != null) {
                if (fav.id > 0) onAnimeClick?.invoke(fav.id)
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (fav.coverImage.isNotBlank()) {
                AsyncImage(
                    model = fav.coverImage,
                    contentDescription = fav.title,
                    modifier = Modifier
                        .size(width = 54.dp, height = 76.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⭐ ALL-TIME FAVORITE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    if (fav.score.isNotBlank() && fav.score != "0") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${fav.score}%",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = fav.title,
                    color = OttTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (fav.format.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fav.format,
                        color = OttTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListAnimePickerSheet(
    onDismiss: () -> Unit,
    onAnimeSelected: (Anime) -> Unit,
    onSearch: suspend (String) -> List<Anime>
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Trigger initial search or search on query change with debounce
    LaunchedEffect(query) {
        isLoading = true
        delay(350)
        results = onSearch(query)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OttPanelHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select Favorite Anime",
                    color = OttTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OttTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search title on AniList...", color = OttTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OttPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OttPanelHigh,
                    unfocusedContainerColor = OttPanelHigh,
                    focusedBorderColor = OttPrimary,
                    unfocusedBorderColor = OttInk(0.20f),
                    focusedTextColor = OttTextPrimary,
                    unfocusedTextColor = OttTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OttPrimary, modifier = Modifier.size(32.dp))
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "Type to search any anime on AniList" else "No anime found for \"$query\"",
                        color = OttTextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(results, key = { it.id }) { anime ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OttPanelHigh)
                                .border(1.dp, OttInk(0.10f), RoundedCornerShape(12.dp))
                                .clickable {
                                    onAnimeSelected(anime)
                                    onDismiss()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = anime.cover,
                                contentDescription = anime.title,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anime.title,
                                    color = OttTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = anime.displayFormat,
                                        color = OttTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    anime.score?.let { score ->
                                        Text(
                                            text = "⭐ $score%",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
