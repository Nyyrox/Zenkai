package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.nyyrox.aniverse.ui.components.OttPosterCard
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.viewmodel.MainViewModel

private val BROWSE_ALL_GENRES = listOf(
    "All genres", "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy",
    "Horror", "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological",
    "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
)

private val BROWSE_ALL_YEARS = listOf("Any year") + (2026 downTo 1990).map { it.toString() }

private val BROWSE_ALL_SORTS = listOf("Trending", "Popular", "Top Rated", "Recently Updated", "Newest")

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
    var selectedFormat by remember { mutableStateOf("ALL") } // "ALL", "TV", "MOVIE"
    var selectedSource by remember { mutableStateOf("AniList") } // "AniList", "TMDB"
    var isGridView by remember { mutableStateOf(true) }

    var genreDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    fun triggerSearch(page: Int = 1) {
        val parsedYear = selectedYear.takeIf { it != "Any year" }?.toIntOrNull()
        val genreParam = if (selectedGenre == "All genres" || selectedGenre == "All") null else selectedGenre
        val formatParam = when (selectedFormat) {
            "TV" -> "TV"
            "MOVIE" -> "MOVIE"
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
            // ── TOP NAVIGATION BAR (Back, Brand, Source Toggle [AniList | TMDB], Grid/List Toggle) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(index = 0)
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(p.chipBg)
                                .border(1.dp, p.glassBorder, CircleShape)
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
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = p.text
                        )
                        Text(
                            text = "BROWSE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = p.rating
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source Selector Pill [AniList | TMDB]
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(p.chipBg)
                            .border(1.dp, p.glassBorder, RoundedCornerShape(17.dp))
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

                    // Grid / List View Toggle Button
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(p.chipBg)
                            .border(1.dp, p.glassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = p.text,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // ── FILTERS (Format Pills + Search Input & Button + Genre/Year/Sort Dropdowns) ──
            // Clean seamless layout directly on the background without heavy card container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(index = 1)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Format Selector Pills: [All] [Series (Tv icon)] [Movies (Movie icon)]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatSelectorPill(
                        label = "All",
                        icon = null,
                        isSelected = selectedFormat == "ALL",
                        p = p,
                        onClick = {
                            if (selectedFormat != "ALL") {
                                selectedFormat = "ALL"
                                triggerSearch(1)
                            }
                        }
                    )
                    FormatSelectorPill(
                        label = "Series",
                        icon = Icons.Default.Tv,
                        isSelected = selectedFormat == "TV",
                        p = p,
                        onClick = {
                            if (selectedFormat != "TV") {
                                selectedFormat = "TV"
                                triggerSearch(1)
                            }
                        }
                    )
                    FormatSelectorPill(
                        label = "Movies",
                        icon = Icons.Default.Movie,
                        isSelected = selectedFormat == "MOVIE",
                        p = p,
                        onClick = {
                            if (selectedFormat != "MOVIE") {
                                selectedFormat = "MOVIE"
                                triggerSearch(1)
                            }
                        }
                    )
                }

                // Row 2: Search Input + Dedicated "Search" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(p.chipBg)
                            .border(1.dp, p.glassBorder, RoundedCornerShape(12.dp))
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
                                        text = "Search anime...",
                                        fontSize = 13.5.sp,
                                        color = p.textMuted
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        color = p.text,
                                        fontSize = 13.5.sp,
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
                        }
                    }

                    Button(
                        onClick = { triggerSearch(1) },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = p.rating,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Search",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Row 3: Filter Dropdowns: [All genres ▼] [Any year ▼] [Trending ▼]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Genre Dropdown
                    Box(modifier = Modifier.weight(1.1f)) {
                        FilterDropdownButton(
                            label = selectedGenre,
                            p = p,
                            onClick = { genreDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = genreDropdownExpanded,
                            onDismissRequest = { genreDropdownExpanded = false },
                            modifier = Modifier
                                .background(if (p.dark) Color(0xFF161A24) else p.flatFill)
                                .border(1.dp, p.glassBorder, RoundedCornerShape(8.dp))
                        ) {
                            BROWSE_ALL_GENRES.forEach { genre ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = genre,
                                            fontSize = 12.sp,
                                            color = if (selectedGenre == genre) p.rating else p.text,
                                            fontWeight = if (selectedGenre == genre) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedGenre = genre
                                        genreDropdownExpanded = false
                                        triggerSearch(1)
                                    }
                                )
                            }
                        }
                    }

                    // Year Dropdown
                    Box(modifier = Modifier.weight(0.9f)) {
                        FilterDropdownButton(
                            label = selectedYear,
                            p = p,
                            onClick = { yearDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = yearDropdownExpanded,
                            onDismissRequest = { yearDropdownExpanded = false },
                            modifier = Modifier
                                .background(if (p.dark) Color(0xFF161A24) else p.flatFill)
                                .border(1.dp, p.glassBorder, RoundedCornerShape(8.dp))
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

                    // Sort Dropdown
                    Box(modifier = Modifier.weight(1.0f)) {
                        FilterDropdownButton(
                            label = selectedSort,
                            p = p,
                            onClick = { sortDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sortDropdownExpanded,
                            onDismissRequest = { sortDropdownExpanded = false },
                            modifier = Modifier
                                .background(if (p.dark) Color(0xFF161A24) else p.flatFill)
                                .border(1.dp, p.glassBorder, RoundedCornerShape(8.dp))
                        ) {
                            BROWSE_ALL_SORTS.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort,
                                            fontSize = 12.sp,
                                            color = if (selectedSort == sort) p.rating else p.text,
                                            fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSort = sort
                                        sortDropdownExpanded = false
                                        triggerSearch(1)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── CONTENT AREA (Wide Backdrop Media Cards or Adaptive Poster Grid) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .staggeredEntrance(index = 2)
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
                                    text = "Unable to load titles",
                                    fontSize = 18.sp,
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
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No anime matches found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.text
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Try changing search terms or resetting filters.",
                                    fontSize = 13.sp,
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
                        // Adaptive poster grid layout matching Home, Manga, and Novel screens
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(145.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(vm.browse, key = { index, it -> "${it.id}_$index" }) { _, anime ->
                                Browse3DMediaCard(
                                    anime = anime,
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
                        // Media Card List
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(vm.browse, key = { index, it -> "${it.id}_$index" }) { index, anime ->
                                OttBrowseBackdropCard(
                                    rank = index + 1,
                                    anime = anime,
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
 * Format Selector Pill (e.g. All, Series, Movies with TV / Film icons)
 */
@Composable
private fun FormatSelectorPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isSelected: Boolean,
    p: HomePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isSelected) p.rating else p.chipBg)
            .border(
                1.dp,
                if (isSelected) p.rating else p.glassBorder,
                RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
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
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else p.textSub
            )
        }
    }
}

/**
 * Filter Dropdown Button with Chevron Down
 */
@Composable
private fun FilterDropdownButton(
    label: String,
    p: HomePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(p.chipBg)
            .border(1.dp, p.glassBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = p.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Wide Backdrop Media Card with unified palette theme
 */
@Composable
fun OttBrowseBackdropCard(
    rank: Int,
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = rememberHomePalette()
    val bgImage = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(14.dp))
            .background(if (p.dark) Color(0xFF0C0F17) else p.flatFill)
            .border(1.dp, p.glassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        // ── 1. Subtle Darkened Wide Backdrop in Background ──
        if (bgImage.isNotBlank()) {
            AsyncImage(
                model = bgImage,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (p.dark) 0.60f else 0.25f),
                contentScale = ContentScale.Crop
            )
        }

        // ── 2. Directional Gradient Overlay for Readability ──
        val gradOverlay = if (p.dark) Color(0xCC0A0D14) else p.flatFill.copy(alpha = 0.85f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to gradOverlay,
                        0.50f to gradOverlay.copy(alpha = 0.80f),
                        1.0f to gradOverlay.copy(alpha = 0.35f)
                    )
                )
        )

        // ── 3. Foreground Content ──
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clean 2:3 vertical poster thumbnail with Rank badge overlay
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 114.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, p.glassBorder, RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = anime.cover,
                    contentDescription = anime.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Zenkai Crimson Rank Badge Capsule (e.g. #1, #2...)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(bottomEnd = 7.dp))
                        .background(p.rating)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#$rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(11.dp))

            // Right: Content details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Title
                Text(
                    text = anime.displayTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata row: Score, Format, Year, Episodes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    anime.score?.let { score ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(p.chipBg)
                                .border(0.5.dp, p.glassBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = p.rating,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", score / 10f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(p.rating.copy(alpha = 0.15f))
                            .border(0.5.dp, p.rating.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = anime.displayFormat,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.rating
                        )
                    }

                    if (anime.displayYear.isNotBlank()) {
                        Text(
                            text = anime.displayYear,
                            fontSize = 10.5.sp,
                            color = p.textSub
                        )
                    }

                    anime.episodes?.let { epCount ->
                        Text(text = "•", fontSize = 10.sp, color = p.textMuted)
                        Text(
                            text = "$epCount eps",
                            fontSize = 10.5.sp,
                            color = p.textSub
                        )
                    }
                }

                // Middle: Description
                Text(
                    text = anime.cleanDescription,
                    fontSize = 10.5.sp,
                    color = p.textSub,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )

                // Bottom: Genre tags
                if (anime.genres.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        anime.genres.take(3).forEach { g ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(p.chipBg)
                                    .border(0.5.dp, p.glassBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = g,
                                    fontSize = 8.5.sp,
                                    color = p.textSub,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Browse3DMediaCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OttPosterCard(
        anime = anime,
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
    OttBrowseBackdropCard(
        rank = 1,
        anime = anime,
        onClick = onClick,
        modifier = modifier
    )
}
