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
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.nyyrox.aniverse.ui.components.iosGlass
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
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.ui.components.ShimmerBox
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    onMangaClick: (String) -> Unit = { vm.openMangaDetail(it) },
    onMangaReadClick: (mangaId: String, chapterId: String) -> Unit = { _, _ -> },
    onNovelClick: (String) -> Unit = {},
    onNovelReadClick: (novelSlug: String, chapterSlug: String) -> Unit = { _, _ -> },
    onDownloadsClick: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        if (vm.mangaHome.trending.isEmpty() && vm.mangaHome.latest.isEmpty()) {
            vm.loadMangaHome()
        }
        if (vm.novelPopular.isEmpty() && vm.novelRankings.isEmpty()) {
            vm.loadNovelHome()
        }
    }

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

        if (vm.trending.isEmpty() && vm.popular.isEmpty() && (vm.homeError != null || !vm.homeLoading)) {
            OfflineHomeScreenContent(
                vm = vm,
                onAnimeClick = onAnimeClick,
                onPlayClick = onPlayClick,
                onMangaClick = onMangaClick,
                onMangaReadClick = onMangaReadClick,
                onNovelClick = onNovelClick,
                onNovelReadClick = onNovelReadClick,
                onDownloadsClick = onDownloadsClick
            )
            return@Box
        }

        val lazyListState = vm.homeLazyListState
        val exploreList = vm.homeExplore.ifEmpty { vm.popular }

        val trendingTopEpisodes = remember(vm.trending, vm.popular) {
            (vm.trending.ifEmpty { vm.popular }).take(2)
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
                if (vm.actionAnime.isNotEmpty()) ("Action & Adrenaline" to vm.actionAnime) else null,
                if (vm.fantasyAnime.isNotEmpty()) ("Fantasy & Magic" to vm.fantasyAnime) else null,
                if (vm.topRated.isNotEmpty()) ("Masterpieces & Critically Acclaimed" to vm.topRated) else null,
                if (vm.scifiAnime.isNotEmpty()) ("Sci-Fi & Cyberpunk" to vm.scifiAnime) else null,
                if (vm.romanceAnime.isNotEmpty()) ("Romance & Slice of Life" to vm.romanceAnime) else null,
                if (vm.newReleases.isNotEmpty()) ("Fresh Broadcast Hits" to vm.newReleases) else null,
                if (vm.upcomingAnime.isNotEmpty()) ("Highly Anticipated" to vm.upcomingAnime) else null,
                if (vm.popular.isNotEmpty()) ("Fan Favorite Hits" to vm.popular) else null
            ).shuffled()
        }

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
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFD54F))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = vm.homeError ?: "",
                        fontSize = 11.5.sp,
                        color = Color(0xFFFFE082),
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
                        tmdbEpisodeStills = vm.tmdbEpisodeStills,
                        onLoadEpisodeStill = { id, ep -> vm.loadHistoryEpisodeStill(id, ep) },
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
                val epStill = vm.tmdbEpisodeStills["${anime.id}_1"]
                val artwork = vm.tmdbArtworks[anime.id]
                YouTubeStyleEpisodeCard(
                    anime = anime,
                    episodeNumber = 1,
                    badgeText = if (idx == 0) "MUST WATCH" else "TRENDING NOW",
                    episodeStill = epStill,
                    logoUrl = artwork?.logoUrl,
                    tmdbTitle = artwork?.title,
                    onLoadStill = { vm.loadTmdbEpisodeStill(anime, 1) },
                    onLoadArtwork = { vm.loadTmdbArtwork(anime) },
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

        // ── Must Watch Spotlight 1 (In Between Popular Series and Critically Acclaimed) ──
        val spotlight1 = vm.popular.getOrNull(1) ?: vm.trending.getOrNull(2)
        if (spotlight1 != null) {
            item(key = "spotlight_popular_${spotlight1.id}") {
                val artwork = vm.tmdbArtworks[spotlight1.id]
                val epStill = vm.tmdbEpisodeStills["${spotlight1.id}_1"]
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).staggeredEntrance(index = 4)) {
                    OttSectionHeader(
                        title = "Must Watch Spotlight",
                        subtitle = "Trending worldwide on Zenkai"
                    )
                    YouTubeStyleEpisodeCard(
                        anime = spotlight1,
                        episodeNumber = 1,
                        badgeText = "MUST WATCH",
                        episodeStill = epStill,
                        logoUrl = artwork?.logoUrl,
                        tmdbTitle = artwork?.title,
                        onLoadStill = { vm.loadTmdbEpisodeStill(spotlight1, 1) },
                        onLoadArtwork = { vm.loadTmdbArtwork(spotlight1) },
                        onAnimeClick = onAnimeClick,
                        onPlayClick = onPlayClick
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

        // ── Action Spotlight 2 (In Between Action & Fantasy) ──
        val actionSpotlight = vm.actionAnime.firstOrNull()
        if (actionSpotlight != null && actionSpotlight.id != spotlight1?.id) {
            item(key = "spotlight_action_${actionSpotlight.id}") {
                val artwork = vm.tmdbArtworks[actionSpotlight.id]
                val epStill = vm.tmdbEpisodeStills["${actionSpotlight.id}_1"]
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).staggeredEntrance(index = 6)) {
                    OttSectionHeader(
                        title = "Action Spotlight",
                        subtitle = "High octane showdowns and epic fights"
                    )
                    YouTubeStyleEpisodeCard(
                        anime = actionSpotlight,
                        episodeNumber = 1,
                        badgeText = "MUST WATCH • ACTION",
                        episodeStill = epStill,
                        logoUrl = artwork?.logoUrl,
                        tmdbTitle = artwork?.title,
                        onLoadStill = { vm.loadTmdbEpisodeStill(actionSpotlight, 1) },
                        onLoadArtwork = { vm.loadTmdbArtwork(actionSpotlight) },
                        onAnimeClick = onAnimeClick,
                        onPlayClick = onPlayClick
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

        // ── Popular Manga Shelf ──
        val mangaList = vm.mangaHome.trending.ifEmpty { vm.mangaHome.latest }
        if (mangaList.isNotEmpty()) {
            item(key = "home_popular_manga") {
                Column(modifier = Modifier.staggeredEntrance(index = 10)) {
                    OttSectionHeader(
                        title = "Popular Manga",
                        subtitle = "Trending manga, manhwa & comics"
                    )
                    HomeMangaRow(
                        list = mangaList,
                        onMangaClick = onMangaClick
                    )
                }
            }
        }

        // ── Top Light Novels Shelf ──
        val novelList = vm.novelPopular.ifEmpty { vm.novelLightNovels }
        if (novelList.isNotEmpty()) {
            item(key = "home_popular_novels") {
                Column(modifier = Modifier.staggeredEntrance(index = 11)) {
                    OttSectionHeader(
                        title = "Top Light Novels",
                        subtitle = "Most read novels & web serials"
                    )
                    HomeNovelRow(
                        list = novelList,
                        onNovelClick = onNovelClick
                    )
                }
            }
        }

        // ── Featured Spotlight 3 (In Between Novels and Explore Feed) ──
        val featureSpotlight = vm.scifiAnime.firstOrNull() ?: vm.fantasyAnime.firstOrNull()
        if (featureSpotlight != null && featureSpotlight.id != spotlight1?.id && featureSpotlight.id != actionSpotlight?.id) {
            item(key = "spotlight_feature_${featureSpotlight.id}") {
                val artwork = vm.tmdbArtworks[featureSpotlight.id]
                val epStill = vm.tmdbEpisodeStills["${featureSpotlight.id}_1"]
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).staggeredEntrance(index = 11)) {
                    OttSectionHeader(
                        title = "Featured Spotlight",
                        subtitle = "Handpicked masterpiece with full episodes"
                    )
                    YouTubeStyleEpisodeCard(
                        anime = featureSpotlight,
                        episodeNumber = 1,
                        badgeText = "MUST WATCH",
                        episodeStill = epStill,
                        logoUrl = artwork?.logoUrl,
                        tmdbTitle = artwork?.title,
                        onLoadStill = { vm.loadTmdbEpisodeStill(featureSpotlight, 1) },
                        onLoadArtwork = { vm.loadTmdbArtwork(featureSpotlight) },
                        onAnimeClick = onAnimeClick,
                        onPlayClick = onPlayClick
                    )
                }
            }
        }

        // ── 7. Dynamic YouTube-Style Feed: Top Episodes + Random Genre Shelves with Media Cards ───
        if (exploreList.isNotEmpty()) {
            item(key = "home_explore_header") {
                Column(modifier = Modifier.padding(top = 10.dp).staggeredEntrance(index = 12)) {
                    OttSectionHeader(
                        title = "Top Episodes & Discover",
                        subtitle = "Stream top episodes and explore genres"
                    )
                }
            }

            itemsIndexed(
                items = exploreList,
                key = { idx, anime -> "explore_yt_feed_${anime.id}_$idx" }
            ) { index, anime ->
                val epNum = remember(anime.id, anime.episodes, anime.isMovie) {
                    if (anime.isMovie || anime.episodes == 1) 1
                    else {
                        val maxEp = (anime.episodes ?: 12).coerceAtLeast(1)
                        ((anime.id % maxEp) + 1).coerceIn(1, maxEp)
                    }
                }
                val badge = remember(index) {
                    when (index % 5) {
                        0 -> "MUST WATCH"
                        1 -> "TOP EPISODE"
                        2 -> "TOP WATCHED"
                        3 -> "TRENDING NOW"
                        else -> "POPULAR PICK"
                    }
                }
                val epStill = vm.tmdbEpisodeStills["${anime.id}_$epNum"]
                val artwork = vm.tmdbArtworks[anime.id]

                // YouTube-style episode card
                YouTubeStyleEpisodeCard(
                    anime = anime,
                    episodeNumber = epNum,
                    badgeText = badge,
                    episodeStill = epStill,
                    logoUrl = artwork?.logoUrl,
                    tmdbTitle = artwork?.title,
                    onLoadStill = { vm.loadTmdbEpisodeStill(anime, epNum) },
                    onLoadArtwork = { vm.loadTmdbArtwork(anime) },
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

@Composable
private fun HomeMangaRow(
    list: List<com.nyyrox.aniverse.data.manga.MangaCard>,
    onMangaClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { "manga_${it.id}" }) { manga ->
            Column(
                modifier = Modifier
                    .width(118.dp)
                    .clickable { onMangaClick(manga.id) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141822))
                ) {
                    AsyncImage(
                        model = manga.poster,
                        contentDescription = manga.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xB3000000))
                                )
                            )
                    )
                    val chText: String = manga.chapter?.takeIf { it.isNotBlank() } ?: (manga.type ?: "Manga")
                    if (chText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x8A000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = chText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OttAccentGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = manga.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeNovelRow(
    list: List<com.nyyrox.aniverse.data.novel.NovelItem>,
    onNovelClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { "novel_${it.slug}" }) { novel ->
            Column(
                modifier = Modifier
                    .width(118.dp)
                    .clickable { onNovelClick(novel.slug) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141822))
                ) {
                    AsyncImage(
                        model = novel.cover,
                        contentDescription = novel.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xB3000000))
                                )
                            )
                    )
                    val badge: String = novel.rating?.takeIf { it.isNotBlank() } ?: (novel.stats?.takeIf { it.isNotBlank() } ?: novel.source)
                    if (badge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x8A000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = novel.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * YouTube-style episode feed card with 16:9 thumbnail, duration and episode badge,
 * circular channel avatar, title, genres, and quick play action.
 * Supports lazy loading TMDB episode stills with fallback to AniList banner/cover.
 * Subtle frosted glass styling, borderless text boxes, and emoji-free.
 */
@Composable
fun YouTubeStyleEpisodeCard(
    anime: Anime,
    episodeNumber: Int = 1,
    badgeText: String? = null,
    episodeStill: String? = null,
    logoUrl: String? = null,
    tmdbTitle: String? = null,
    onLoadStill: (() -> Unit)? = null,
    onLoadArtwork: (() -> Unit)? = null,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(anime.id, episodeNumber) {
        onLoadStill?.invoke()
        onLoadArtwork?.invoke()
    }

    val durationText = remember(anime.duration) {
        val mins = anime.duration ?: 24
        "${mins}:00"
    }

    val displayTitle = anime.displayTitle
    val preferredTitle = tmdbTitle?.takeIf { it.isNotBlank() } ?: displayTitle
    val episodeTitle = if (anime.isMovie) "Full Movie" else "Episode $episodeNumber"

    val thumbnailImage = remember(episodeStill, anime.banner, anime.cover) {
        episodeStill?.takeIf { it.isNotBlank() }
            ?: anime.banner?.takeIf { it.isNotBlank() }
            ?: anime.cover
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAnimeClick(anime.id) }
            .padding(bottom = 16.dp)
    ) {
        // 1. YouTube 16:9 Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131722))
                .clickable { onPlayClick(anime.id, episodeNumber) }
        ) {
            SubcomposeAsyncImage(
                model = thumbnailImage,
                contentDescription = preferredTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerBox(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp)
                    )
                },
                error = {
                    AsyncImage(
                        model = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                        contentDescription = preferredTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )

            // Gradient darkening (top, center, and deep bottom for text and logo readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x60000000),
                                Color(0x10000000),
                                Color(0x9A000000)
                            )
                        )
                    )
            )

            // Top-Left Badge (e.g. "MUST WATCH", "TOP WATCHED", "TRENDING") - Subtle frosted glass (no border)
            if (!badgeText.isNullOrBlank()) {
                val isMustWatch = badgeText.contains("MUST WATCH", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMustWatch) Color(0x95E50914) else Color(0x850E131E))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
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

            // TMDB Title Logo PNG Overlay (Overlaid inside thumbnail above bottom badges)
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = preferredTitle,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 36.dp)
                        .heightIn(max = 42.dp)
                        .fillMaxWidth(0.55f),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart
                )
            }

            // Center Circular Play Button - Frosted Glass Circle (no border)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x60000000))
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

            // Bottom-Left Episode Badge (no border)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x8A000000))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (anime.isMovie) "MOVIE" else "EP $episodeNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OttAccentGold
                )
            }

            // Bottom-Right Duration Badge (no border)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x8A000000))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Anime Avatar (YouTube Channel style) - Clean Circle (no border)
            AsyncImage(
                model = anime.cover,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
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
                val headerTitle = if (anime.isMovie) preferredTitle else "$preferredTitle • $episodeTitle"
                Text(
                    text = headerTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                val genreText = anime.genres.take(2).joinToString(" • ")
                val scoreText = anime.score?.let { "$it% Match" } ?: "Trending"
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

            // Play Icon Button (Frosted glass button, no border)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1FFFFFFF))
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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    if (mb < 1024.0) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}

private val OfflineMargin = 18.dp

/** Offline home: status card with library stats, then the downloaded shelves. Clears the floating top bar and nav bar. */
@Composable
fun OfflineHomeScreenContent(
    vm: MainViewModel,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    onMangaClick: (String) -> Unit,
    onMangaReadClick: (mangaId: String, chapterId: String) -> Unit,
    onNovelClick: (String) -> Unit,
    onNovelReadClick: (novelSlug: String, chapterSlug: String) -> Unit,
    onDownloadsClick: () -> Unit
) {
    val downloadedAnime by vm.downloadManager.downloadedAnime.collectAsState()
    val downloadedManga by vm.downloadManager.downloadedManga.collectAsState()
    val downloadedNovels by vm.downloadManager.downloadedNovels.collectAsState()

    val animeBytes = remember(downloadedAnime) { downloadedAnime.sumOf { it.totalSizeBytes } }
    val mangaBytes = remember(downloadedManga) { downloadedManga.sumOf { it.totalSizeBytes } }
    val novelBytes = remember(downloadedNovels) { downloadedNovels.sumOf { it.totalSizeBytes } }
    val totalDownloadsCount = downloadedAnime.size + downloadedManga.size + downloadedNovels.size
    val totalDownloadedBytes = animeBytes + mangaBytes + novelBytes
    val hasAnyHistory = vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()

    val animeColor = Color(0xFF60A5FA)
    val mangaColor = Color(0xFF34D399)
    val novelColor = Color(0xFFF472B6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Top: clear the status bar + floating app bar (menu / logo / search). Bottom: clear the floating nav bar.
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "offline_top_spacer") {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(40.dp))
        }

        // ── Offline status card ──
        item(key = "offline_hero_banner") {
            val shape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OfflineMargin)
                    .iosGlass(
                        shape = shape,
                        backgroundColor = Color(0xD81C1D24),
                        borderWidth = 1.dp
                    )
            ) {
                // Warm amber glow in the corner
                Box(Modifier.matchParentSize().clip(shape)) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .offset(x = (-70).dp, y = (-90).dp)
                            .background(
                                Brush.radialGradient(listOf(Color(0x33F59E0B), Color.Transparent)),
                                CircleShape
                            )
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OfflinePulseIcon()
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0x28F59E0B))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "OFFLINE MODE",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "You're Offline",
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (totalDownloadsCount > 0) {
                            "No connection, but your library is ready. Everything below plays and reads without internet."
                        } else {
                            "No internet connection detected. Connect to Wi-Fi or mobile data to explore trending shows, or browse your downloaded library."
                        },
                        color = Color(0xB3FFFFFF),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )

                    if (totalDownloadsCount > 0) {
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OfflineStatTile(animeColor, downloadedAnime.size.toString(), "Anime")
                            OfflineStatTile(mangaColor, downloadedManga.size.toString(), "Manga")
                            OfflineStatTile(novelColor, downloadedNovels.size.toString(), "Novels")
                        }

                        if (totalDownloadedBytes > 0L) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Storage used", color = Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(formatBytes(totalDownloadedBytes), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1FFFFFFF)),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (animeBytes > 0L) Box(Modifier.weight(animeBytes.toFloat()).fillMaxHeight().background(animeColor))
                                if (mangaBytes > 0L) Box(Modifier.weight(mangaBytes.toFloat()).fillMaxHeight().background(mangaColor))
                                if (novelBytes > 0L) Box(Modifier.weight(novelBytes.toFloat()).fillMaxHeight().background(novelColor))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    val scope = rememberCoroutineScope()
                    val spin = remember { Animatable(0f) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDownloadsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = OttPillShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "Open Downloads",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    spin.snapTo(0f)
                                    spin.animateTo(360f, tween(650, easing = FastOutSlowInEasing))
                                }
                                vm.loadHome()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x28FFFFFF),
                                contentColor = Color.White
                            ),
                            shape = OttPillShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = spin.value }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Continue Watching & Reading (if history exists)
        if (hasAnyHistory) {
            item(key = "offline_continue_watching") {
                OttUnifiedContinueWatchingRow(
                    animeHistory = vm.history,
                    mangaHistory = vm.mangaHistory,
                    novelHistory = vm.novelHistoryList,
                    tmdbEpisodeStills = vm.tmdbEpisodeStills,
                    onLoadEpisodeStill = { id, ep -> vm.loadHistoryEpisodeStill(id, ep) },
                    onPlayAnime = onPlayClick,
                    onReadManga = onMangaReadClick,
                    onReadNovel = onNovelReadClick,
                    onRemoveAnime = { vm.removeHistory(it) },
                    onRemoveManga = { vm.removeMangaHistory(it) },
                    onRemoveNovel = { vm.removeNovelHistory(it) }
                )
            }
        }

        // Downloaded Anime
        if (downloadedAnime.isNotEmpty()) {
            item(key = "offline_downloaded_anime") {
                Column {
                    OttSectionHeader(
                        title = "Downloaded Anime",
                        subtitle = "${downloadedAnime.size} title(s) • ${formatBytes(animeBytes)}",
                        onSeeAllClick = onDownloadsClick
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(downloadedAnime, key = { "offline_anime_${it.animeId}" }) { anime ->
                            OfflineMediaCard(
                                title = anime.title,
                                coverUrl = anime.cover,
                                countBadge = "${anime.episodesCount} EPS",
                                sizeBytes = anime.totalSizeBytes,
                                badgeColor = animeColor,
                                onClick = { onAnimeClick(anime.animeId) },
                                onDirectAction = { onPlayClick(anime.animeId, 1) },
                                directActionIcon = Icons.Default.PlayArrow
                            )
                        }
                    }
                }
            }
        }

        // Downloaded Manga
        if (downloadedManga.isNotEmpty()) {
            item(key = "offline_downloaded_manga") {
                Column {
                    OttSectionHeader(
                        title = "Downloaded Manga",
                        subtitle = "${downloadedManga.size} series • ${formatBytes(mangaBytes)}",
                        onSeeAllClick = onDownloadsClick
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(downloadedManga, key = { "offline_manga_${it.mangaId}" }) { manga ->
                            OfflineMediaCard(
                                title = manga.title,
                                coverUrl = manga.poster,
                                countBadge = "${manga.chaptersCount} CH",
                                sizeBytes = manga.totalSizeBytes,
                                badgeColor = mangaColor,
                                onClick = { onMangaClick(manga.mangaId) }
                            )
                        }
                    }
                }
            }
        }

        // Downloaded Novels
        if (downloadedNovels.isNotEmpty()) {
            item(key = "offline_downloaded_novels") {
                Column {
                    OttSectionHeader(
                        title = "Downloaded Novels",
                        subtitle = "${downloadedNovels.size} light novel(s) • ${formatBytes(novelBytes)}",
                        onSeeAllClick = onDownloadsClick
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(downloadedNovels, key = { "offline_novel_${it.novelSlug}" }) { novel ->
                            OfflineMediaCard(
                                title = novel.title,
                                coverUrl = novel.cover,
                                countBadge = "${novel.chaptersCount} CH",
                                sizeBytes = novel.totalSizeBytes,
                                badgeColor = novelColor,
                                onClick = { onNovelClick(novel.novelSlug) }
                            )
                        }
                    }
                }
            }
        }

        // Empty state when there are 0 downloads and 0 history
        if (totalDownloadsCount == 0 && !hasAnyHistory) {
            item(key = "offline_empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OfflineMargin)
                        .iosGlass(
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = Color(0xD81C1D24),
                            borderWidth = 1.dp
                        )
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0x1FFFFFFF))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xAAFFFFFF),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Downloaded Titles Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Download anime episodes, manga chapters, and novel chapters while connected so you can enjoy them anywhere without internet.",
                            fontSize = 13.sp,
                            color = Color(0x99FFFFFF),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = onDownloadsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = OttPillShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Open Downloads Hub",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Amber download icon with a slow radiating ring. */
@Composable
private fun OfflinePulseIcon() {
    val t = rememberInfiniteTransition(label = "offlinePulse")
    val ring by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing)),
        label = "offlineRing"
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .drawBehind {
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = (1f - ring) * 0.45f),
                    radius = size.minDimension / 2f * (1f + ring * 0.55f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0x26F59E0B))
                .border(1.dp, Color(0x4DF59E0B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Downloading,
                contentDescription = "Offline",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun RowScope.OfflineStatTile(color: Color, value: String, label: String) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x14FFFFFF), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(label, color = Color(0x99FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun OfflineMediaCard(
    title: String,
    coverUrl: String?,
    countBadge: String,
    sizeBytes: Long,
    badgeColor: Color,
    onClick: () -> Unit,
    onDirectAction: (() -> Unit)? = null,
    directActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "offlineCardPress"
    )
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .border(1.dp, Color(0x22FFFFFF), shape)
                .background(Color(0x18FFFFFF))
        ) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x28FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color(0x60FFFFFF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x44000000),
                            0.4f to Color.Transparent,
                            1f to Color(0xDD000000)
                        )
                    )
            )

            // Count badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xD90F1015))
                    .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(text = countBadge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Quick action (e.g. play)
            if (onDirectAction != null && directActionIcon != null) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6FFFFFF))
                        .clickable(onClick = onDirectAction),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = directActionIcon,
                        contentDescription = "Action",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Size pill
            if (sizeBytes > 0L) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xCCFFFFFF), modifier = Modifier.size(10.dp))
                    Text(
                        text = formatBytes(sizeBytes),
                        color = Color(0xE6FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = OttTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
