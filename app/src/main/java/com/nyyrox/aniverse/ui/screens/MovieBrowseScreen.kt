package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.movie.MovieDetails
import com.nyyrox.aniverse.data.movie.MovieGenre
import com.nyyrox.aniverse.data.movie.MovieItem
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.ui.animation.staggeredEntrance
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieBrowseScreen(
    vm: MainViewModel,
    onPlayMovie: (id: Int, type: String) -> Unit
) {
    val p = rememberHomePalette()
    val scope = rememberCoroutineScope()

    var mediaType by remember { mutableStateOf("all") } // "all", "movie", "tv"
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenreId by remember { mutableStateOf<Int?>(null) }

    // Data states
    var trendingItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var popularTv by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var topRatedItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var genreDiscoverItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

    var isLoadingHome by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingGenre by remember { mutableStateOf(false) }

    // Detail Bottom Sheet
    var selectedItemForDetail by remember { mutableStateOf<MovieItem?>(null) }
    var selectedDetails by remember { mutableStateOf<MovieDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load initial shelves
    LaunchedEffect(mediaType) {
        isLoadingHome = true
        try {
            val tType = if (mediaType == "all") "all" else mediaType
            trendingItems = MovieRepository.getTrending(tType, 1)
            popularMovies = MovieRepository.getPopular("movie", 1)
            popularTv = MovieRepository.getPopular("tv", 1)
            topRatedItems = MovieRepository.getTopRated(if (mediaType == "tv") "tv" else "movie", 1)
        } catch (_: Exception) {}
        isLoadingHome = false
    }

    // Trigger Search
    fun doSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        scope.launch {
            isSearching = true
            try {
                val sType = if (mediaType == "all") "multi" else mediaType
                searchResults = MovieRepository.search(query, sType, 1)
            } catch (_: Exception) {}
            isSearching = false
        }
    }

    // Trigger Genre Filter
    fun doSelectGenre(genreId: Int?) {
        selectedGenreId = genreId
        if (genreId == null) {
            genreDiscoverItems = emptyList()
            return
        }
        scope.launch {
            isLoadingGenre = true
            try {
                val dType = if (mediaType == "tv") "tv" else "movie"
                genreDiscoverItems = MovieRepository.discover(type = dType, genreId = genreId, page = 1)
            } catch (_: Exception) {}
            isLoadingGenre = false
        }
    }

    // Open Detail Modal
    fun openDetail(item: MovieItem) {
        selectedItemForDetail = item
        selectedDetails = null
        isLoadingDetails = true
        scope.launch {
            selectedDetails = MovieRepository.getDetails(item.id, item.type)
            isLoadingDetails = false
        }
    }

    val allGenres = remember(mediaType) {
        if (mediaType == "tv") MovieRepository.TV_GENRES else MovieRepository.MOVIE_GENRES
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
            // ── TOP HEADER (Brand, Type Selector Pills, Search) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZENKAI ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = p.text
                    )
                    Text(
                        text = "MOVIES & TV",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = p.rating
                    )
                }

                // Media Type Selector: [All] [Movies] [TV]
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(p.chipBg)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("all" to "All", "movie" to "Movies", "tv" to "Series").forEach { (typeKey, label) ->
                        val isSel = mediaType == typeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (isSel) p.rating else Color.Transparent)
                                .clickable {
                                    if (mediaType != typeKey) {
                                        mediaType = typeKey
                                        selectedGenreId = null
                                        genreDiscoverItems = emptyList()
                                        if (searchQuery.isNotBlank()) doSearch(searchQuery)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else p.textMuted
                            )
                        }
                    }
                }
            }

            // ── SEARCH BAR ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.chipBg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
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
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search movies, series, or blockbusters…",
                                fontSize = 13.sp,
                                color = p.textMuted
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isBlank()) searchResults = emptyList()
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
                            keyboardActions = KeyboardActions(onSearch = { doSearch(searchQuery) })
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = p.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ── GENRE PILLS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSel = selectedGenreId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAllSel) p.rating else p.chipBg)
                        .clickable { doSelectGenre(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All",
                        fontSize = 11.5.sp,
                        fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSel) Color.White else p.text
                    )
                }

                allGenres.forEach { genre ->
                    val isSel = selectedGenreId == genre.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) p.rating else p.chipBg)
                            .clickable { doSelectGenre(if (isSel) null else genre.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre.name,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else p.text
                        )
                    }
                }
            }

            // ── MAIN CONTENT ──
            if (searchQuery.isNotBlank()) {
                // SEARCH RESULTS VIEW
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.rating, modifier = Modifier.size(36.dp))
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No movies or TV shows found for \"$searchQuery\"",
                            fontSize = 13.sp,
                            color = p.textMuted
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults) { item ->
                            MovieGridCard(item = item, p = p, onClick = { openDetail(item) })
                        }
                    }
                }
            } else if (selectedGenreId != null) {
                // GENRE FILTER VIEW
                if (isLoadingGenre) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.rating, modifier = Modifier.size(36.dp))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(genreDiscoverItems) { item ->
                            MovieGridCard(item = item, p = p, onClick = { openDetail(item) })
                        }
                    }
                }
            } else {
                // HOME SHELVES & HERO CAROUSEL VIEW
                if (isLoadingHome) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.rating, modifier = Modifier.size(36.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // 1. Featured Hero Carousel (Top 5 Trending)
                        if (trendingItems.isNotEmpty()) {
                            item {
                                val heroItems = trendingItems.take(5)
                                val pagerState = rememberPagerState { heroItems.size }
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                    ) { page ->
                                        val hero = heroItems[page]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable { openDetail(hero) }
                                        ) {
                                            if (hero.backdropUrl != null) {
                                                AsyncImage(
                                                    model = hero.backdropUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            // Cinematic Dark Gradient
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.5f),
                                                                Color.Black.copy(alpha = 0.95f)
                                                            )
                                                        )
                                                    )
                                            )

                                            // Hero Overlay Content
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Type pill (MOVIE / TV)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(p.rating)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (hero.type == "tv") "SERIES" else "MOVIE",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                    }

                                                    if (hero.rating != null) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0x55000000))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = Color(0xFFFFB300),
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(Modifier.width(3.dp))
                                                            Text(
                                                                text = "${hero.rating}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }

                                                    hero.year?.let {
                                                        Text(
                                                            text = it,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFFDDDDDD),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(4.dp))

                                                Text(
                                                    text = hero.title,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                if (hero.overview.isNotBlank()) {
                                                    Text(
                                                        text = hero.overview,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFAAAAAA),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                // "Watch Now" action pill
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(Color.White)
                                                        .clickable { onPlayMovie(hero.id, hero.type) }
                                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Watch Now",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Shelf: Trending This Week
                        if (trendingItems.isNotEmpty()) {
                            item {
                                MovieShelf(
                                    title = "Trending This Week",
                                    items = trendingItems,
                                    p = p,
                                    onItemClick = { openDetail(it) }
                                )
                            }
                        }

                        // 3. Shelf: Popular Movies
                        if (popularMovies.isNotEmpty()) {
                            item {
                                MovieShelf(
                                    title = "Popular Blockbusters",
                                    items = popularMovies,
                                    p = p,
                                    onItemClick = { openDetail(it) }
                                )
                            }
                        }

                        // 4. Shelf: Popular Series
                        if (popularTv.isNotEmpty()) {
                            item {
                                MovieShelf(
                                    title = "Binge-Worthy TV Series",
                                    items = popularTv,
                                    p = p,
                                    onItemClick = { openDetail(it) }
                                )
                            }
                        }

                        // 5. Shelf: Top Rated Masterpieces
                        if (topRatedItems.isNotEmpty()) {
                            item {
                                MovieShelf(
                                    title = "Top Rated Masterpieces",
                                    items = topRatedItems,
                                    p = p,
                                    onItemClick = { openDetail(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── MOVIE DETAIL MODAL BOTTOM SHEET ──
        if (selectedItemForDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetail = null },
                sheetState = sheetState,
                containerColor = if (p.dark) Color(0xFF14171E) else Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                val item = selectedItemForDetail!!
                val d = selectedDetails

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 40.dp)
                ) {
                    // Header: Backdrop / Poster & Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                        ) {
                            if (item.posterUrl != null) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = p.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (d?.tagline != null) {
                                Text(
                                    text = "\"${d.tagline}\"",
                                    fontSize = 11.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = p.rating,
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(p.chipBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.type == "tv") "TV SERIES" else "MOVIE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.rating
                                    )
                                }

                                item.year?.let {
                                    Text(
                                        text = it,
                                        fontSize = 12.sp,
                                        color = p.textMuted
                                    )
                                }

                                d?.runtime?.let {
                                    Text(
                                        text = "· ${it}m",
                                        fontSize = 12.sp,
                                        color = p.textMuted
                                    )
                                }

                                item.rating?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "$it",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = p.text
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // "Watch Now" Big Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(p.rating)
                                    .clickable {
                                        val targetId = item.id
                                        val targetType = item.type
                                        selectedItemForDetail = null
                                        onPlayMovie(targetId, targetType)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Watch Stream",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Synopsis
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "SYNOPSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.overview.ifBlank { "No synopsis available." },
                        fontSize = 12.5.sp,
                        color = p.textSub,
                        lineHeight = 18.sp
                    )

                    // Genres
                    if (d != null && d.genres.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            d.genres.forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(p.chipBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = g.name,
                                        fontSize = 11.sp,
                                        color = p.textMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Cast Row
                    if (d != null && d.cast.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "TOP CAST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(d.cast) { c ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(p.chipBg)
                                    ) {
                                        if (c.photoUrl != null) {
                                            AsyncImage(
                                                model = c.photoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = c.name,
                                        fontSize = 10.sp,
                                        color = p.text,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun MovieShelf(
    title: String,
    items: List<MovieItem>,
    p: HomePalette,
    onItemClick: (MovieItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = p.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                MoviePosterCard(item = item, p = p, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun MoviePosterCard(
    item: MovieItem,
    p: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(p.chipBg)
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Top score badge
            if (item.rating != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
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
                        text = "${item.rating}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom format badge (MOVIE / TV)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.type == "tv") Color(0xCC6366F1) else Color(0xCCEF4444))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = if (item.type == "tv") "TV" else "FILM",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.year?.let {
            Text(
                text = it,
                fontSize = 10.5.sp,
                color = p.textMuted
            )
        }
    }
}

@Composable
private fun MovieGridCard(
    item: MovieItem,
    p: HomePalette,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(p.chipBg)
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Top score badge
            if (item.rating != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
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
                        text = "${item.rating}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom format badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.type == "tv") Color(0xCC6366F1) else Color(0xCCEF4444))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = if (item.type == "tv") "TV" else "FILM",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = p.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        item.year?.let {
            Text(
                text = it,
                fontSize = 10.5.sp,
                color = p.textMuted
            )
        }
    }
}
