package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.nyyrox.aniverse.ui.animation.heroScrollParallax
import com.nyyrox.aniverse.ui.animation.staggeredEntrance
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.ui.components.ContinueWatchingSkeleton
import com.nyyrox.aniverse.ui.components.HeroBannerSkeleton
import com.nyyrox.aniverse.ui.components.OttHeroBanner
import com.nyyrox.aniverse.ui.components.OttContinueWatchingRow
import com.nyyrox.aniverse.ui.components.OttUnifiedContinueWatchingRow
import com.nyyrox.aniverse.ui.components.OttPosterCard
import com.nyyrox.aniverse.ui.components.OttSectionHeader
import com.nyyrox.aniverse.ui.components.OttTop10Row
import com.nyyrox.aniverse.ui.components.PosterRowSkeleton
import com.nyyrox.aniverse.ui.components.Top10RowSkeleton
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttHeroGradient
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttSurfaceElevated
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.components.ZenkaiCommunityRow
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    onMangaReadClick: (mangaId: String, chapterId: String) -> Unit = { _, _ -> },
    onNovelClick: (String) -> Unit = {},
    onNovelReadClick: (novelSlug: String, chapterSlug: String) -> Unit = { _, _ -> }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBg)
    ) {

        if (vm.homeLoading && vm.trending.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item { HeroBannerSkeleton() }
                if (vm.history.isNotEmpty()) {
                    item { ContinueWatchingSkeleton() }
                }
                item { PosterRowSkeleton(titleWidth = 140) }
                item { Top10RowSkeleton() }
                item { PosterRowSkeleton(titleWidth = 120) }
            }
            return@Box
        }

        if (vm.homeError != null && vm.trending.isEmpty() && vm.popular.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Connection Issue",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = vm.homeError ?: "Unable to fetch trending anime.",
                        fontSize = 13.sp,
                        color = OttTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.loadHome() },
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
            return@Box
        }

        val lazyListState = vm.homeLazyListState
        val exploreList = vm.homeExplore.ifEmpty { vm.popular }

        val shouldLoadMore by remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val total = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                total > 0 && lastVisible >= total - 3
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore && !vm.homeExploreLoading && vm.homeExploreHasMore) {
                vm.loadMoreHomeExplore()
            }
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Inline warning banner when using TMDB fallback data
        if (vm.homeError != null && vm.trending.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FFC107))
                        .border(1.dp, Color(0x44FFC107), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⚠  ${vm.homeError}",
                        fontSize = 11.5.sp,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Featured 3D Stacked Hero Banner (10-15 different shows)
        item(key = "home_hero_banner") {
            val heroShows = remember(vm.trending, vm.popular) {
                (vm.trending + vm.popular).distinctBy { it.id }.take(15)
            }
            if (heroShows.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .heroScrollParallax(lazyListState)
                        .staggeredEntrance(index = 0)
                ) {
                    OttHeroBanner(
                        featuredList = heroShows,
                        tmdbArtworks = vm.tmdbArtworks,
                        onLoadArtwork = { vm.loadTmdbArtwork(it) },
                        isInWatchlist = { vm.isInWatchlist(it) },
                        onToggleWatchlist = { vm.toggleWatchlist(it) },
                        onAnimeClick = onAnimeClick,
                        onPlayClick = { onPlayClick(it, 1) },
                        isVerticalScrollInProgress = lazyListState.isScrollInProgress
                    )
                }
            }
        }

        // Continue Watching & Reading Row (Unified Anime, Manga & Novel History)
        val hasAnyHistory = vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()
        if (hasAnyHistory) {
            item(key = "home_continue_watching") {
                Box(modifier = Modifier.staggeredEntrance(index = 1)) {
                    OttUnifiedContinueWatchingRow(
                        animeHistory = vm.history,
                        mangaHistory = vm.mangaHistory,
                        novelHistory = vm.novelHistoryList,
                        onPlayAnime = onPlayClick,
                        onReadManga = onMangaReadClick,
                        onReadNovel = onNovelReadClick,
                        onRemoveAnime = { vm.removeHistory(it) },
                        onRemoveManga = { vm.removeMangaHistory(it) },
                        onRemoveNovel = { vm.removeNovelHistory(it) }
                    )
                }
            }
        }

        // Featured Trending & Top Watched YouTube-style Episode Feed
        val trendingTopEpisodes = remember(vm.trending, vm.popular) {
            (vm.trending.ifEmpty { vm.popular }).take(2)
        }
        if (trendingTopEpisodes.isNotEmpty()) {
            item(key = "home_trending_episodes_header") {
                Column(modifier = Modifier.staggeredEntrance(index = 2)) {
                    OttSectionHeader(
                        title = "Trending Top Watched",
                        subtitle = "Popular episodes streaming right now"
                    )
                }
            }
            itemsIndexed(trendingTopEpisodes, key = { idx, a -> "trending_ep_${a.id}_$idx" }) { idx, anime ->
                YouTubeStyleEpisodeCard(
                    anime = anime,
                    episodeNumber = 1,
                    badgeText = if (idx == 0) "🔥 #1 TOP WATCHED" else "TRENDING NOW",
                    onAnimeClick = onAnimeClick,
                    onPlayClick = onPlayClick,
                    modifier = Modifier.staggeredEntrance(index = 2 + idx)
                )
            }
        }

        // Netflix-Style Top 10 in Anime Row
        if (vm.trending.isNotEmpty()) {
            item(key = "home_top_10") {
                Column(modifier = Modifier.staggeredEntrance(index = 2)) {
                    OttSectionHeader(
                        title = "Top 10 Today",
                        subtitle = "Most streamed titles right now"
                    )
                    OttTop10Row(
                        list = vm.trending,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Popular Series This Season
        if (vm.popular.isNotEmpty()) {
            item(key = "home_popular_series") {
                Column(modifier = Modifier.staggeredEntrance(index = 3)) {
                    OttSectionHeader(
                        title = "Popular Series",
                        subtitle = "Fan favorites and trending hits"
                    )
                    OttHorizontalMediaRow(
                        list = vm.popular,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Top Rated Masterpieces
        if (vm.topRated.isNotEmpty()) {
            item(key = "home_top_rated") {
                Column(modifier = Modifier.staggeredEntrance(index = 4)) {
                    OttSectionHeader(
                        title = "Critically Acclaimed",
                        subtitle = "Highest rated by global community"
                    )
                    OttHorizontalMediaRow(
                        list = vm.topRated,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Action & Thrills
        if (vm.actionAnime.isNotEmpty()) {
            item(key = "home_action_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 5)) {
                    OttSectionHeader(
                        title = "Action & High Adrenaline",
                        subtitle = "Epic fights and supernatural showdowns"
                    )
                    OttHorizontalMediaRow(
                        list = vm.actionAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Fantasy & Supernatural
        if (vm.fantasyAnime.isNotEmpty()) {
            item(key = "home_fantasy_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 6)) {
                    OttSectionHeader(
                        title = "Fantasy & Magic",
                        subtitle = "Immersive worlds and magical adventures"
                    )
                    OttHorizontalMediaRow(
                        list = vm.fantasyAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // New Releases
        if (vm.newReleases.isNotEmpty()) {
            item(key = "home_new_releases") {
                Column(modifier = Modifier.staggeredEntrance(index = 7)) {
                    OttSectionHeader(
                        title = "New Releases",
                        subtitle = "Freshly airing broadcast episodes"
                    )
                    OttHorizontalMediaRow(
                        list = vm.newReleases,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Upcoming Anime
        if (vm.upcomingAnime.isNotEmpty()) {
            item(key = "home_upcoming_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 8)) {
                    OttSectionHeader(
                        title = "Upcoming Shows",
                        subtitle = "Most anticipated upcoming titles"
                    )
                    OttHorizontalMediaRow(
                        list = vm.upcomingAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Sci-Fi & Cyberpunk
        if (vm.scifiAnime.isNotEmpty()) {
            item(key = "home_scifi_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 9)) {
                    OttSectionHeader(
                        title = "Sci-Fi & Cyberpunk",
                        subtitle = "Futuristic realms and tech thrillers"
                    )
                    OttHorizontalMediaRow(
                        list = vm.scifiAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Romance & Drama
        if (vm.romanceAnime.isNotEmpty()) {
            item(key = "home_romance_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 10)) {
                    OttSectionHeader(
                        title = "Romance & Slice of Life",
                        subtitle = "Heartwarming stories and emotional journeys"
                    )
                    OttHorizontalMediaRow(
                        list = vm.romanceAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // ── 7. Dynamic YouTube-Style Feed: Top Episodes + Random Genre Shelves with Media Cards ───
        if (exploreList.isNotEmpty()) {
            item(key = "home_explore_header") {
                Column(modifier = Modifier.padding(top = 10.dp).staggeredEntrance(index = 11)) {
                    OttSectionHeader(
                        title = "Top Episodes & Discover",
                        subtitle = "Stream top episodes and explore genres"
                    )
                }
            }

            val genreShelves = remember(
                vm.actionAnime,
                vm.fantasyAnime,
                vm.topRated,
                vm.scifiAnime,
                vm.romanceAnime,
                vm.newReleases,
                vm.upcomingAnime,
                vm.popular
            ) {
                listOfNotNull(
                    if (vm.actionAnime.isNotEmpty()) ("💥 Action & Adrenaline" to vm.actionAnime) else null,
                    if (vm.fantasyAnime.isNotEmpty()) ("✨ Fantasy & Magic" to vm.fantasyAnime) else null,
                    if (vm.topRated.isNotEmpty()) ("🌟 Masterpieces & Critically Acclaimed" to vm.topRated) else null,
                    if (vm.scifiAnime.isNotEmpty()) ("🚀 Sci-Fi & Cyberpunk" to vm.scifiAnime) else null,
                    if (vm.romanceAnime.isNotEmpty()) ("💖 Romance & Slice of Life" to vm.romanceAnime) else null,
                    if (vm.newReleases.isNotEmpty()) ("⚡ Fresh Broadcast Hits" to vm.newReleases) else null,
                    if (vm.upcomingAnime.isNotEmpty()) ("🔮 Highly Anticipated" to vm.upcomingAnime) else null,
                    if (vm.popular.isNotEmpty()) ("🔥 Fan Favorite Hits" to vm.popular) else null
                ).shuffled()
            }

            itemsIndexed(
                items = exploreList,
                key = { idx, anime -> "explore_yt_feed_${anime.id}_$idx" }
            ) { index, anime ->
                val epNum = remember(anime.id) { ((anime.id % 12) + 1).coerceAtLeast(1) }
                val badge = remember(index) {
                    when (index % 5) {
                        0 -> "🔥 TOP EPISODE"
                        1 -> "TOP WATCHED"
                        2 -> "TRENDING NOW"
                        3 -> "POPULAR PICK"
                        else -> "MUST WATCH"
                    }
                }

                // YouTube-style episode card
                YouTubeStyleEpisodeCard(
                    anime = anime,
                    episodeNumber = epNum,
                    badgeText = badge,
                    onAnimeClick = onAnimeClick,
                    onPlayClick = onPlayClick
                )

                // Interleave random genre shelf with media cards every 2 episode cards
                if (genreShelves.isNotEmpty() && (index % 2 == 1)) {
                    val shelfIdx = (index / 2) % genreShelves.size
                    val (genreTitle, genreList) = genreShelves[shelfIdx]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        OttSectionHeader(
                            title = genreTitle,
                            subtitle = "Top recommended titles in this genre"
                        )
                        OttHorizontalMediaRow(
                            list = genreList,
                            onAnimeClick = onAnimeClick
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            if (vm.homeExploreLoading) {
                item(key = "home_explore_spinner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = OttPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun OttHorizontalMediaRow(
    list: List<Anime>,
    onAnimeClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { it.id }) { anime ->
            OttPosterCard(
                anime = anime,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

/**
 * YouTube-style episode feed card with 16:9 thumbnail, duration and episode badge,
 * circular channel avatar, title, genres, and quick play action.
 */
@Composable
fun YouTubeStyleEpisodeCard(
    anime: Anime,
    episodeNumber: Int = 1,
    badgeText: String? = null,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationText = remember(anime.duration) {
        val mins = anime.duration ?: 24
        "${mins}:00"
    }

    val displayTitle = anime.displayTitle
    val episodeTitle = "Episode $episodeNumber"

    val thumbnailImage = remember(anime.banner, anime.cover) {
        anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAnimeClick(anime.id) }
            .padding(bottom = 18.dp)
    ) {
        // 1. YouTube 16:9 Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141822))
                .clickable { onPlayClick(anime.id, episodeNumber) }
        ) {
            AsyncImage(
                model = thumbnailImage,
                contentDescription = displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient darkening
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x35000000),
                                Color.Transparent,
                                Color(0xA6000000)
                            )
                        )
                    )
            )

            // Top-Left Badge (e.g. "🔥 TOP WATCHED" or "TRENDING")
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD90F141E))
                        .border(0.8.dp, Color(0x40FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Center Circular Play Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .border(1.dp, Color(0x80FFFFFF), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Bottom-Left Episode Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EP $episodeNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttAccentGold
                )
            }

            // Bottom-Right Duration Badge (YouTube style)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Video Info Row (Avatar on Left, Title + Subtitle in Middle, Play button on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Anime Avatar (YouTube Channel style)
            AsyncImage(
                model = anime.cover,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onAnimeClick(anime.id) },
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Title + Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAnimeClick(anime.id) }
            ) {
                Text(
                    text = "$displayTitle • $episodeTitle",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                val genreText = anime.genres.take(2).joinToString(" • ")
                val scoreText = anime.score?.let { "★ $it%" } ?: "Trending"
                val formatText = anime.displayFormat ?: "TV"

                Text(
                    text = listOfNotNull(genreText.ifBlank { null }, formatText, scoreText).joinToString(" • "),
                    fontSize = 12.sp,
                    color = OttTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play Icon Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable { onPlayClick(anime.id, episodeNumber) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

