package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.movie.MovieGenre
import com.nyyrox.aniverse.data.movie.MovieItem
import com.nyyrox.aniverse.data.movie.MovieRepository
import com.nyyrox.aniverse.data.movie.toAnime
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MovieHomeScreen(
    vm: MainViewModel,
    onMovieClick: (id: Int, type: String, title: String?, poster: String?) -> Unit,
    onPlayMovie: (id: Int, type: String, season: Int, episode: Int) -> Unit,
    onSearchClick: () -> Unit
) {
    val p = rememberHomePalette()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var mediaType by remember { mutableStateOf("all") } // "all", "movie", "tv"
    var selectedGenre by remember { mutableStateOf<MovieGenre?>(null) }

    // Shelves raw items & mapped to Anime
    var heroItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var popularTv by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var nowPlayingItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var topRatedItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var genreDiscoverItems by remember { mutableStateOf<List<MovieItem>>(emptyList()) }

    val idTypeMap = remember { mutableMapOf<Int, String>() }

    var isLoadingHome by remember { mutableStateOf(true) }
    var isLoadingGenre by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadMovieHistory()
    }

    fun loadData() {
        scope.launch {
            isLoadingHome = true
            hasError = false
            try {
                val tType = if (mediaType == "all") "all" else mediaType
                val trending = MovieRepository.getTrending(tType, 1)
                heroItems = trending.filter { !it.backdrop.isNullOrBlank() }.take(6)
                heroItems.forEach { item ->
                    vm.loadMovieArtwork(item.id, item.type)
                }
                trending.forEach { idTypeMap[it.id] = it.type }

                popularMovies = MovieRepository.getPopular("movie", 1)
                popularMovies.forEach { idTypeMap[it.id] = "movie" }

                popularTv = MovieRepository.getPopular("tv", 1)
                popularTv.forEach { idTypeMap[it.id] = "tv" }

                val currentType = if (mediaType == "tv") "tv" else "movie"
                nowPlayingItems = MovieRepository.getNowPlaying(currentType, 1)
                nowPlayingItems.forEach { idTypeMap[it.id] = it.type }

                topRatedItems = MovieRepository.getTopRated(currentType, 1)
                topRatedItems.forEach { idTypeMap[it.id] = it.type }
            } catch (_: Exception) {
                hasError = true
            } finally {
                isLoadingHome = false
            }
        }
    }

    LaunchedEffect(mediaType) {
        loadData()
    }

    fun selectGenre(genre: MovieGenre?) {
        selectedGenre = genre
        if (genre == null) {
            genreDiscoverItems = emptyList()
            return
        }
        scope.launch {
            isLoadingGenre = true
            try {
                val dType = if (mediaType == "tv") "tv" else "movie"
                val items = MovieRepository.discover(type = dType, genreId = genre.id, page = 1)
                items.forEach { idTypeMap[it.id] = it.type }
                genreDiscoverItems = items
            } catch (_: Exception) {}
            isLoadingGenre = false
        }
    }

    val genresList = remember(mediaType) {
        if (mediaType == "tv") MovieRepository.TV_GENRES else MovieRepository.MOVIE_GENRES
    }

    // Convert lists to Anime data models for complete parity with anime components
    val heroShows = remember(heroItems) { heroItems.map { it.toAnime() } }
    val top10Shows = remember(heroItems, popularMovies, popularTv) {
        (heroItems + popularMovies + popularTv).distinctBy { it.id }.take(10).map { it.toAnime() }
    }
    val popularMoviesShows = remember(popularMovies) { popularMovies.map { it.toAnime() } }
    val popularTvShows = remember(popularTv) { popularTv.map { it.toAnime() } }
    val nowPlayingShows = remember(nowPlayingItems) { nowPlayingItems.map { it.toAnime() } }
    val topRatedShows = remember(topRatedItems) { topRatedItems.map { it.toAnime() } }
    val genreShows = remember(genreDiscoverItems) { genreDiscoverItems.map { it.toAnime() } }

    fun resolveType(animeId: Int): String {
        return idTypeMap[animeId] ?: if (mediaType == "tv") "tv" else "movie"
    }

    fun findItem(animeId: Int): MovieItem? {
        return heroItems.find { it.id == animeId }
            ?: popularMovies.find { it.id == animeId }
            ?: popularTv.find { it.id == animeId }
            ?: nowPlayingItems.find { it.id == animeId }
            ?: topRatedItems.find { it.id == animeId }
            ?: genreDiscoverItems.find { it.id == animeId }
    }

    GlassBackdropHost(
        background = { HomeBackdrop(p) }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(p.pageBrush)) {
            if (isLoadingHome && heroShows.isEmpty()) {
                HomeSkeleton(p)
                return@Box
            }

            if (hasError && heroShows.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Could not load movies and series", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.accentBrush)
                            .clickable { loadData() }
                            .padding(horizontal = 20.dp, vertical = 9.dp)
                    ) {
                        Text(text = "Try Again", color = p.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                return@Box
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. EXACT ANIME HERO BANNER (3D coverflow, Ken Burns zoom, backdrop parallax, action pill buttons)
                if (heroShows.isNotEmpty()) {
                        item(key = "movie_hero") {
                            HomeHero(
                                shows = heroShows,
                                vm = vm,
                                p = p,
                                listScrolling = listState.isScrollInProgress,
                                onAnimeClick = { animeId ->
                                    val item = findItem(animeId)
                                    onMovieClick(animeId, resolveType(animeId), item?.title, item?.posterUrl ?: item?.backdropUrl)
                                },
                                onPlayClick = { animeId, _ ->
                                    val t = resolveType(animeId)
                                    onPlayMovie(animeId, t, 1, 1)
                                }
                            )
                        }
                    }

                    // 2. CONTINUE WATCHING (Unified ContinueTile design)
                    if (vm.movieHistory.isNotEmpty()) {
                        val historyList = vm.movieHistory
                        item(key = "movie_continue_section") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle("Continue Watching", p, "Pick up where you left off")
                                val continueRowState = rememberLazyListState()
                                LazyRow(
                                    state = continueRowState,
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(historyList, key = { _, h -> "mh_${h.id}_${h.season}_${h.episode}" }) { i, h ->
                                        val pos = h.positionMs
                                        val dur = h.durationMs
                                        val left = if (dur > pos && dur > 0L) ((dur - pos) / 60000L).coerceAtLeast(1L) else 0L
                                        val epLabel = if (h.type == "tv") "S${h.season} · EP ${h.episode}" else "Movie"
                                        val sub = epLabel + if (dur > 0L) " · ${left}m left" else ""
                                        val entry = ContinueEntry(
                                            key = "movie_${h.id}_${h.season}_${h.episode}",
                                            kind = ContinueKind.MOVIE,
                                            title = h.title,
                                            subtitle = sub,
                                            progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f,
                                            image = { h.backdropUrl ?: h.posterUrl },
                                            onOpen = { onPlayMovie(h.id, h.type, h.season, h.episode) },
                                            onRemove = { vm.removeMovieHistory(h.id, h.type) }
                                        )
                                        ContinueTile(entry, p, Modifier.rowTilt(continueRowState, i))
                                    }
                                }
                            }
                        }
                    }

                    // 3. TOP 10 TODAY (Numbered giant 1-10 anime card row with rowTilt)
                    if (top10Shows.isNotEmpty()) {
                        item(key = "movie_top10") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle("Top 10 Today", p, "Most popular movies & shows")
                                Top10Row(
                                    list = top10Shows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = findItem(animeId)
                                        onMovieClick(animeId, resolveType(animeId), item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }

                    // 4. POPULAR MOVIES (AnimeRow media cards with rowTilt & ratings)
                    if (mediaType != "tv" && popularMoviesShows.isNotEmpty()) {
                        item(key = "movie_popular_movies") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle("Popular Movies", p, "Blockbusters & trending films")
                                AnimeRow(
                                    list = popularMoviesShows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = popularMovies.find { it.id == animeId } ?: findItem(animeId)
                                        onMovieClick(animeId, "movie", item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }

                    // 5. POPULAR TV SERIES (AnimeRow media cards with rowTilt & ratings)
                    if (mediaType != "movie" && popularTvShows.isNotEmpty()) {
                        item(key = "movie_popular_tv") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle("Popular TV Series", p, "Binge-worthy shows & serials")
                                AnimeRow(
                                    list = popularTvShows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = popularTv.find { it.id == animeId } ?: findItem(animeId)
                                        onMovieClick(animeId, "tv", item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }

                    // 6. NOW PLAYING & IN THEATERS
                    if (nowPlayingShows.isNotEmpty()) {
                        item(key = "movie_now_playing") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle(
                                    title = if (mediaType == "tv") "Airing Today" else "Now In Theaters",
                                    p = p,
                                    subtitle = "Latest releases on screens"
                                )
                                AnimeRow(
                                    list = nowPlayingShows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = findItem(animeId)
                                        onMovieClick(animeId, resolveType(animeId), item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }

                    // 7. TOP RATED ALL TIME
                    if (topRatedShows.isNotEmpty()) {
                        item(key = "movie_top_rated") {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                SectionTitle("Critically Acclaimed & Top Rated", p, "Highest rated of all time")
                                AnimeRow(
                                    list = topRatedShows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = findItem(animeId)
                                        onMovieClick(animeId, resolveType(animeId), item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }

                    // 8. GENRE DISCOVERY SHELF
                    item(key = "movie_genres_section") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            SectionTitle("Explore by Genre", p, "Filter by mood & category")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                genresList.forEach { genre ->
                                    val isSel = selectedGenre?.id == genre.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .then(if (isSel) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                            .clickable {
                                                if (isSel) selectGenre(null) else selectGenre(genre)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = genre.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) p.onAccent else p.text
                                        )
                                    }
                                }
                            }

                            if (isLoadingGenre) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = p.accentA, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                                }
                            } else if (genreShows.isNotEmpty() && selectedGenre != null) {
                                AnimeRow(
                                    list = genreShows,
                                    p = p,
                                    onAnimeClick = { animeId ->
                                        val item = findItem(animeId)
                                        onMovieClick(animeId, resolveType(animeId), item?.title, item?.posterUrl ?: item?.backdropUrl)
                                    }
                                )
                            }
                        }
                    }
            }
        }
    }
}
