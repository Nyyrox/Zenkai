package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.ui.animation.staggeredEntrance
import com.nyyrox.aniverse.ui.components.BrowseGridSkeleton
import com.nyyrox.aniverse.ui.components.BrowseListSkeleton
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.viewmodel.MainViewModel

private val BROWSE_ALL_GENRES = listOf(
    "All genres", "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy",
    "Horror", "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological",
    "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
)

private val BROWSE_ALL_YEARS = listOf("Any year") + (2026 downTo 1990).map { it.toString() }

private val BROWSE_ALL_SORTS = listOf(
    "Trending" to "🔥 Trending",
    "Popular" to "⭐ Popular",
    "Top Rated" to "🏆 Top Rated",
    "Recently Updated" to "🕒 Updated",
    "Newest" to "✨ Newest"
)

private val BROWSE_FORMATS = listOf(
    "ALL" to ("All" to null),
    "TV" to ("Series" to Icons.Default.Tv),
    "MOVIE" to ("Movies" to Icons.Default.Movie),
    "OVA" to ("OVA" to Icons.Default.VideoLibrary)
)

@Composable
fun BrowseScreen(
    vm: MainViewModel,
    onBack: (() -> Unit)? = null,
    onAnimeClick: (Int) -> Unit
) {
    val p = rememberHomePalette()
    var query by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All genres") }
    var selectedYear by remember { mutableStateOf("Any year") }
    var selectedSort by remember { mutableStateOf("Trending") }
    var selectedFormat by remember { mutableStateOf("ALL") }
    var selectedSource by remember { mutableStateOf("AniList") }
    var isGridView by remember { mutableStateOf(true) }

    var yearDropdownExpanded by remember { mutableStateOf(false) }

    fun triggerSearch(page: Int = 1) {
        val parsedYear = selectedYear.takeIf { it != "Any year" }?.toIntOrNull()
        val genreParam = if (selectedGenre == "All genres" || selectedGenre == "All") null else selectedGenre
        val formatParam = when (selectedFormat) {
            "TV" -> "TV"
            "MOVIE" -> "MOVIE"
            "OVA" -> "OVA"
            else -> null
        }
        val isTmdb = selectedSource.equals("TMDB", ignoreCase = true)

        vm.loadBrowse(
            page = page,
            search = query.trim(),
            genre = genreParam,
            status = null,
            sort = selectedSort,
            year = parsedYear,
            format = formatParam,
            isTmdbSource = isTmdb
        )
    }

    LaunchedEffect(Unit) {
        if (vm.browse.isEmpty()) {
            triggerSearch(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.pageBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── TOP NAVIGATION BAR (Liquid Glass Header) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(p.chipBg)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = p.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ZENKAI ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = p.text
                        )
                        Text(
                            text = "BROWSE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            color = p.rating
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source Toggle Pill: [AniList | TMDB]
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(p.chipBg)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAniList = selectedSource == "AniList"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (isAniList) p.rating else Color.Transparent)
                                .clickable {
                                    if (!isAniList) {
                                        selectedSource = "AniList"
                                        triggerSearch(1)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AniList",
                                fontSize = 11.sp,
                                fontWeight = if (isAniList) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAniList) Color.White else p.textMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (!isAniList) p.rating else Color.Transparent)
                                .clickable {
                                    if (isAniList) {
                                        selectedSource = "TMDB"
                                        triggerSearch(1)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TMDB",
                                fontSize = 11.sp,
                                fontWeight = if (!isAniList) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isAniList) Color.White else p.textMuted
                            )
                        }
                    }

                    // Grid / List Toggle
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(p.chipBg)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = p.text,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // ── MODERN SEARCH BAR (Glass Glow Border) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.chipBg)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = p.textMuted,
                        modifier = Modifier.size(18.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search anime by title, studio, or character…",
                                fontSize = 13.sp,
                                color = p.textMuted
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                if (it.isBlank()) triggerSearch(1)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = p.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(p.rating),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { triggerSearch(1) })
                        )
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                triggerSearch(1)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = p.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Compact trigger icon
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(p.rating)
                            .clickable { triggerSearch(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // ── MODERN LIQUID GLASS FILTER STRIPS ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Strip 1: Formats & Year Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BROWSE_FORMATS.forEach { (key, pair) ->
                        val (label, icon) = pair
                        val isSel = selectedFormat == key
                        ModernFilterPill(
                            label = label,
                            icon = icon,
                            isSelected = isSel,
                            p = p,
                            onClick = {
                                if (selectedFormat != key) {
                                    selectedFormat = key
                                    triggerSearch(1)
                                }
                            }
                        )
                    }

                    // Year Picker Pill
                    Box {
                        ModernFilterPill(
                            label = if (selectedYear == "Any year") "Year" else selectedYear,
                            icon = Icons.Default.ArrowDropDown,
                            isSelected = selectedYear != "Any year",
                            p = p,
                            onClick = { yearDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = yearDropdownExpanded,
                            onDismissRequest = { yearDropdownExpanded = false },
                            modifier = Modifier.background(if (p.dark) Color(0xFF161A24) else Color.White)
                        ) {
                            BROWSE_ALL_YEARS.forEach { yr ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = yr,
                                            fontSize = 12.sp,
                                            color = if (selectedYear == yr) p.rating else p.text,
                                            fontWeight = if (selectedYear == yr) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedYear = yr
                                        yearDropdownExpanded = false
                                        triggerSearch(1)
                                    }
                                )
                            }
                        }
                    }
                }

                // Strip 2: SORTS (Trending, Popular, Top Rated, Updated, Newest)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BROWSE_ALL_SORTS.forEach { (sortKey, sortLabel) ->
                        val isSel = selectedSort == sortKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) p.rating else p.chipBg)
                                .border(1.dp, if (isSel) p.rating else Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .clickable {
                                    if (selectedSort != sortKey) {
                                        selectedSort = sortKey
                                        triggerSearch(1)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sortLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else p.text
                            )
                        }
                    }
                }

                // Strip 3: GENRES (Horizontal scrolling glass chips with glow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BROWSE_ALL_GENRES.forEach { genre ->
                        val isSel = selectedGenre == genre || (genre == "All genres" && (selectedGenre == "All" || selectedGenre == "All genres"))
                        val displayLabel = if (genre == "All genres") "All Genres" else genre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) p.rating else p.chipBg)
                                .border(1.dp, if (isSel) p.rating else Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .clickable {
                                    if (selectedGenre != genre) {
                                        selectedGenre = genre
                                        triggerSearch(1)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = displayLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else p.text
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── MAIN CONTENT (Glass Poster Grid or Sleek List) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    vm.browseLoading && vm.browse.isEmpty() -> {
                        if (isGridView) {
                            BrowseGridSkeleton()
                        } else {
                            BrowseListSkeleton()
                        }
                    }

                    vm.browseError != null && vm.browse.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Unable to load anime catalog",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.text
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = vm.browseError ?: "Unknown error",
                                    fontSize = 12.sp,
                                    color = p.textMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = { triggerSearch(1) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = p.rating,
                                        contentColor = Color.White
                                    ),
                                    shape = OttPillShape
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    vm.browse.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = p.textMuted,
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No anime matches found",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.text
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Try adjusting search terms or resetting filters.",
                                    fontSize = 12.5.sp,
                                    color = p.textMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        query = ""
                                        selectedGenre = "All genres"
                                        selectedYear = "Any year"
                                        selectedSort = "Trending"
                                        selectedFormat = "ALL"
                                        triggerSearch(1)
                                    },
                                    shape = OttPillShape
                                ) {
                                    Text("Reset Filters", color = p.text)
                                }
                            }
                        }
                    }

                    isGridView -> {
                        // Premium Glass Poster Grid
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(vm.browse, key = { index, it -> "${it.id}_$index" }) { _, anime ->
                                BrowseModernPosterTile(
                                    anime = anime,
                                    p = p,
                                    onClick = { onAnimeClick(anime.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                LaunchedEffect(Unit) {
                                    if (!vm.browseLoading && vm.browse.isNotEmpty()) {
                                        triggerSearch(vm.browsePage + 1)
                                    }
                                }
                                if (vm.browseLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = p.rating,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Modern Glass List View
                        LazyColumn(
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(vm.browse, key = { index, it -> "${it.id}_$index" }) { index, anime ->
                                BrowseModernListItem(
                                    rank = index + 1,
                                    anime = anime,
                                    p = p,
                                    onClick = { onAnimeClick(anime.id) }
                                )
                            }

                            item {
                                LaunchedEffect(Unit) {
                                    if (!vm.browseLoading && vm.browse.isNotEmpty()) {
                                        triggerSearch(vm.browsePage + 1)
                                    }
                                }
                                if (vm.browseLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = p.rating,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
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

/**
 * Modern Glass Filter Pill with subtle border and smooth active state
 */
@Composable
private fun ModernFilterPill(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    p: HomePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) p.rating else p.chipBg)
            .border(1.dp, if (isSelected) p.rating else Color(0x18FFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else p.textSub,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else p.textSub
            )
        }
    }
}

/**
 * Modern Anime Poster Tile (Glass accents, floating score & format badges)
 */
@Composable
internal fun BrowseModernPosterTile(
    anime: Anime,
    p: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(14.dp))
                .background(p.chipBg)
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.6f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )

            // Format Badge (Top Left Glass Capsule)
            if (anime.displayFormat.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = anime.displayFormat,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Rating Star Badge (Top Right Glass Capsule)
            anime.score?.let { score ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", score / 10f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom metadata tag inside poster (Episodes)
            anime.episodes?.let { epCount ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x88000000))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "$epCount eps",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDDDDDD)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Title
        Text(
            text = anime.displayTitle,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle metadata (Year • Genres)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            val metaParts = mutableListOf<String>()
            if (anime.displayYear.isNotBlank()) metaParts.add(anime.displayYear)
            if (anime.genres.isNotEmpty()) metaParts.add(anime.genres.first())

            Text(
                text = metaParts.joinToString(" • "),
                fontSize = 10.5.sp,
                color = p.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern Anime List Item (Glass card with rank accent)
 */
@Composable
internal fun BrowseModernListItem(
    rank: Int,
    anime: Anime,
    p: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.chipBg)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 108.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E2026))
        ) {
            AsyncImage(
                model = anime.cover,
                contentDescription = anime.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Crimson Rank Capsule
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(bottomEnd = 6.dp))
                    .background(p.rating)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = anime.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                anime.score?.let { score ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", score / 10f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.text
                        )
                    }
                    Text("•", fontSize = 10.sp, color = p.textMuted)
                }

                Text(
                    text = anime.displayFormat,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.rating
                )

                if (anime.displayYear.isNotBlank()) {
                    Text("•", fontSize = 10.sp, color = p.textMuted)
                    Text(
                        text = anime.displayYear,
                        fontSize = 11.sp,
                        color = p.textSub
                    )
                }

                anime.episodes?.let { epCount ->
                    Text("•", fontSize = 10.sp, color = p.textMuted)
                    Text(
                        text = "$epCount eps",
                        fontSize = 11.sp,
                        color = p.textSub
                    )
                }
            }

            if (anime.cleanDescription.isNotBlank()) {
                Text(
                    text = anime.cleanDescription,
                    fontSize = 11.sp,
                    color = p.textMuted,
                    maxLines = 2,
                    lineHeight = 15.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (anime.genres.isNotEmpty()) {
                Text(
                    text = anime.genres.take(3).joinToString(" • "),
                    fontSize = 10.sp,
                    color = p.rating,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Backward compatibility composables
@Composable
internal fun Browse2DPosterTile(
    anime: Anime,
    p: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrowseModernPosterTile(
        anime = anime,
        p = p,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun Browse2DListItem(
    rank: Int,
    anime: Anime,
    p: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrowseModernListItem(
        rank = rank,
        anime = anime,
        p = p,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun Browse3DMediaCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    BrowseModernPosterTile(
        anime = anime,
        p = p,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun Browse3DListItem(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    BrowseModernListItem(
        rank = 1,
        anime = anime,
        p = p,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun OttBrowseBackdropCard(
    rank: Int,
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    BrowseModernListItem(
        rank = rank,
        anime = anime,
        p = p,
        onClick = onClick,
        modifier = modifier
    )
}
