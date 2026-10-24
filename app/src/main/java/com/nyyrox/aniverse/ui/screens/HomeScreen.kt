package com.nyyrox.aniverse.ui.screens

import androidx.compose.foundation.background
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.WatchHistoryItem
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.ui.components.ZenkaiCommunityRow
import com.nyyrox.aniverse.ui.theme.AniButton
import com.nyyrox.aniverse.ui.theme.LocalAppColors
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

    val c = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
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

        // Random episode cards dropped between shelves (new picks every launch)
        val breakRnd = remember { kotlin.random.Random(System.nanoTime()) }
        val breakSlots = remember {
            listOf("top_rated", "fantasy", "new", "upcoming", "scifi", "romance").shuffled(breakRnd).take(3).toSet()
        }
        val breakPool = remember(vm.trending, vm.popular, vm.topRated, vm.actionAnime) {
            val used = setOfNotNull(
                (vm.popular.getOrNull(1) ?: vm.trending.getOrNull(2))?.id,
                vm.actionAnime.firstOrNull()?.id,
                (vm.scifiAnime.firstOrNull() ?: vm.fantasyAnime.firstOrNull())?.id
            )
            (vm.trending + vm.popular + vm.topRated + vm.actionAnime)
                .distinctBy { it.id }
                .filter { it.id !in used }
                .shuffled(breakRnd)
        }

        // Full-screen tint behind everything: strongest at the top, melts into the page,
        // and fades out as the hero scrolls away.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "home_hero_banner" }
                    alpha = if (info == null) 0f else (1f + info.offset / (info.size * 1.4f)).coerceIn(0f, 1f)
                }
                .background(
                    Brush.verticalGradient(
                        0f to c.accent.copy(alpha = 0.34f),
                        0.35f to c.accent.copy(alpha = 0.16f),
                        0.75f to c.accent.copy(alpha = 0.05f),
                        1f to Color.Transparent
                    )
                )
        )

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
                        .background(c.surfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = vm.homeError ?: "",
                        fontSize = 11.5.sp,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Full-bleed hero (up to 15 shows)
        item(key = "home_hero_banner") {
            val heroShows = remember(vm.trending, vm.popular) {
                (vm.trending + vm.popular).distinctBy { it.id }.take(15)
            }
            if (heroShows.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .heroSlideAway(lazyListState)
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
                    HomeContinueRow(
                        animeHistory = vm.history,
                        mangaHistory = vm.mangaHistory,
                        novelHistory = vm.novelHistoryList,
                        stillFor = { id, ep -> vm.tmdbEpisodeStills["${id}_$ep"] },
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
                        title = "Trending Top Watched"
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
                        title = "Top 10 Today"
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
                        title = "Popular Series"
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
                        title = "Must Watch Spotlight"
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
                        title = "Critically Acclaimed"
                    )
                    OttHorizontalMediaRow(
                        list = vm.topRated,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("top_rated" in breakSlots) episodeBreak("top_rated", breakPool, 0, vm, onAnimeClick, onPlayClick)

        // Action & Thrills
        if (vm.actionAnime.isNotEmpty()) {
            item(key = "home_action_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 5)) {
                    OttSectionHeader(
                        title = "Action & High Adrenaline"
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
                        title = "Action Spotlight"
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
                        title = "Fantasy & Magic"
                    )
                    OttHorizontalMediaRow(
                        list = vm.fantasyAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("fantasy" in breakSlots) episodeBreak("fantasy", breakPool, 1, vm, onAnimeClick, onPlayClick)

        // New Releases
        if (vm.newReleases.isNotEmpty()) {
            item(key = "home_new_releases") {
                Column(modifier = Modifier.staggeredEntrance(index = 7)) {
                    OttSectionHeader(
                        title = "New Releases"
                    )
                    OttHorizontalMediaRow(
                        list = vm.newReleases,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("new" in breakSlots) episodeBreak("new", breakPool, 2, vm, onAnimeClick, onPlayClick)

        // Upcoming Anime
        if (vm.upcomingAnime.isNotEmpty()) {
            item(key = "home_upcoming_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 8)) {
                    OttSectionHeader(
                        title = "Upcoming Shows"
                    )
                    OttHorizontalMediaRow(
                        list = vm.upcomingAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("upcoming" in breakSlots) episodeBreak("upcoming", breakPool, 3, vm, onAnimeClick, onPlayClick)

        // Sci-Fi & Cyberpunk
        if (vm.scifiAnime.isNotEmpty()) {
            item(key = "home_scifi_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 9)) {
                    OttSectionHeader(
                        title = "Sci-Fi & Cyberpunk"
                    )
                    OttHorizontalMediaRow(
                        list = vm.scifiAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("scifi" in breakSlots) episodeBreak("scifi", breakPool, 4, vm, onAnimeClick, onPlayClick)

        // Romance & Drama
        if (vm.romanceAnime.isNotEmpty()) {
            item(key = "home_romance_anime") {
                Column(modifier = Modifier.staggeredEntrance(index = 10)) {
                    OttSectionHeader(
                        title = "Romance & Slice of Life"
                    )
                    OttHorizontalMediaRow(
                        list = vm.romanceAnime,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        if ("romance" in breakSlots) episodeBreak("romance", breakPool, 5, vm, onAnimeClick, onPlayClick)

        // ── Popular Manga Shelf ──
        val mangaList = vm.mangaHome.trending.ifEmpty { vm.mangaHome.latest }
        if (mangaList.isNotEmpty()) {
            item(key = "home_popular_manga") {
                Column(modifier = Modifier.staggeredEntrance(index = 10)) {
                    OttSectionHeader(
                        title = "Popular Manga"
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
                        title = "Top Light Novels"
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
                        title = "Featured Spotlight"
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
                        title = "Top Episodes & Discover"
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
                            title = genreTitle
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
                            color = c.accent,
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
) = OttOverlapMediaRow(list = list, onAnimeClick = onAnimeClick)

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
            HomePosterTile(
                title = manga.title,
                image = manga.poster,
                meta = manga.chapter?.takeIf { it.isNotBlank() } ?: (manga.type ?: "Manga"),
                onClick = { onMangaClick(manga.id) }
            )
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
            HomePosterTile(
                title = novel.title,
                image = novel.cover,
                meta = novel.rating?.takeIf { it.isNotBlank() } ?: (novel.stats?.takeIf { it.isNotBlank() } ?: novel.source),
                onClick = { onNovelClick(novel.slug) }
            )
        }
    }
}

/** Same look as OttPosterCard: plain poster, title, one muted meta line. */
@Composable
private fun HomePosterTile(title: String, image: String?, meta: String?, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.width(118.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = image,
            contentDescription = null, // title below labels the tile
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.skeleton),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!meta.isNullOrBlank()) {
            Text(text = meta, fontSize = 11.sp, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Episode feed item: 16:9 thumbnail (tap = play), then label / title / meta on the page background (tap = details).
 * No card, no badges. Scrims sit on the image only, so they stay dark in both themes.
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

    val c = LocalAppColors.current
    val durationText = remember(anime.duration) { "${anime.duration ?: 24}:00" }
    val title = tmdbTitle?.takeIf { it.isNotBlank() } ?: anime.displayTitle
    val headerTitle = if (anime.isMovie) title else "$title • Episode $episodeNumber"
    val thumbnailImage = remember(episodeStill, anime.banner, anime.cover) {
        episodeStill?.takeIf { it.isNotBlank() }
            ?: anime.banner?.takeIf { it.isNotBlank() }
            ?: anime.cover
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(c.skeleton)
                .clickable { onPlayClick(anime.id, episodeNumber) }
        ) {
            SubcomposeAsyncImage(
                model = thumbnailImage,
                contentDescription = null, // title below labels the item
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp)) },
                error = {
                    AsyncImage(
                        model = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )

            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x99000000))
                )
            )

            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                        .heightIn(max = 36.dp)
                        .fillMaxWidth(0.5f),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomStart
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(26.dp))
            }

            Text(
                text = durationText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAnimeClick(anime.id) }
                .padding(top = 10.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textMuted,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = headerTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                maxLines = 2,
                lineHeight = 19.sp,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    anime.genres.take(2).joinToString(" • ").ifBlank { null },
                    anime.displayFormat ?: "TV",
                    anime.score?.let { "$it% Match" } ?: "Trending"
                ).joinToString(" • "),
                fontSize = 12.sp,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

/** Offline home: plain status block, then the downloaded shelves. Clears the floating top bar and nav bar. */
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
    val c = LocalAppColors.current
    val downloadedAnime by vm.downloadManager.downloadedAnime.collectAsState()
    val downloadedManga by vm.downloadManager.downloadedManga.collectAsState()
    val downloadedNovels by vm.downloadManager.downloadedNovels.collectAsState()

    val animeBytes = remember(downloadedAnime) { downloadedAnime.sumOf { it.totalSizeBytes } }
    val mangaBytes = remember(downloadedManga) { downloadedManga.sumOf { it.totalSizeBytes } }
    val novelBytes = remember(downloadedNovels) { downloadedNovels.sumOf { it.totalSizeBytes } }
    val totalDownloadsCount = downloadedAnime.size + downloadedManga.size + downloadedNovels.size
    val totalDownloadedBytes = animeBytes + mangaBytes + novelBytes
    val hasAnyHistory = vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "offline_top_spacer") {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(56.dp))
        }

        item(key = "offline_status") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OfflineMargin)) {
                Text("OFFLINE", color = c.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text("You're offline", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (totalDownloadsCount > 0) {
                        "No connection, but your library is ready. Everything below plays and reads without internet."
                    } else {
                        "No internet connection detected. Connect to Wi-Fi or mobile data to explore trending shows, or browse your downloaded library."
                    },
                    color = c.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (totalDownloadsCount > 0) {
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OfflineStat(downloadedAnime.size.toString(), "Anime")
                        OfflineStat(downloadedManga.size.toString(), "Manga")
                        OfflineStat(downloadedNovels.size.toString(), "Novels")
                    }

                    if (totalDownloadedBytes > 0L) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Storage used", color = c.textMuted, fontSize = 12.sp)
                            Text(formatBytes(totalDownloadedBytes), color = c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        // One neutral hue in three strengths: anime / manga / novels
                        Row(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.skeleton),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (animeBytes > 0L) Box(Modifier.weight(animeBytes.toFloat()).fillMaxHeight().background(c.accent))
                            if (mangaBytes > 0L) Box(Modifier.weight(mangaBytes.toFloat()).fillMaxHeight().background(c.accent.copy(alpha = 0.55f)))
                            if (novelBytes > 0L) Box(Modifier.weight(novelBytes.toFloat()).fillMaxHeight().background(c.accent.copy(alpha = 0.25f)))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                // ponytail: retry spin animation dropped
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AniButton(onClick = onDownloadsClick, primary = true, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Open Downloads", color = c.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    AniButton(onClick = { vm.loadHome() }, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }

        if (hasAnyHistory) {
            item(key = "offline_continue_watching") {
                HomeContinueRow(
                    animeHistory = vm.history,
                    mangaHistory = vm.mangaHistory,
                    novelHistory = vm.novelHistoryList,
                    stillFor = { id, ep -> vm.tmdbEpisodeStills["${id}_$ep"] },
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
                                onClick = { onAnimeClick(anime.animeId) },
                                onDirectAction = { onPlayClick(anime.animeId, 1) },
                                directActionIcon = Icons.Default.PlayArrow
                            )
                        }
                    }
                }
            }
        }

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
                                onClick = { onMangaClick(manga.mangaId) }
                            )
                        }
                    }
                }
            }
        }

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
                                onClick = { onNovelClick(novel.novelSlug) }
                            )
                        }
                    }
                }
            }
        }

        if (totalDownloadsCount == 0 && !hasAnyHistory) {
            item(key = "offline_empty_state") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = OfflineMargin * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(32.dp))
                    Text("No downloads yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary, textAlign = TextAlign.Center)
                    Text(
                        text = "Download anime episodes, manga chapters, and novel chapters while connected so you can enjoy them anywhere without internet.",
                        fontSize = 13.sp,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    AniButton(onClick = onDownloadsClick, primary = true, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)) {
                        Text("Open Downloads Hub", color = c.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.OfflineStat(value: String, label: String) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.weight(1f)) {
        Text(value, color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = c.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun OfflineMediaCard(
    title: String,
    coverUrl: String?,
    countBadge: String,
    sizeBytes: Long,
    onClick: () -> Unit,
    onDirectAction: (() -> Unit)? = null,
    directActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val c = LocalAppColors.current
    Column(modifier = Modifier.width(130.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(c.skeleton)
        ) {
            SubcomposeAsyncImage(
                model = coverUrl,
                contentDescription = null, // title below labels the card
                contentScale = ContentScale.Crop,
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(32.dp))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (onDirectAction != null && directActionIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                        .clickable(onClick = onDirectAction),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(directActionIcon, contentDescription = "Play", tint = c.onAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(text = title, color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = listOfNotNull(countBadge, if (sizeBytes > 0L) formatBytes(sizeBytes) else null).joinToString(" · "),
            color = c.textMuted,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Home redesign helpers: 3D press tilt, overlapped poster rows, hero slide-away,
//  random episode breaks, and the new Continue row.
// ═════════════════════════════════════════════════════════════════════════════

/** Press = card tilts toward the finger and sinks in, release = springs back with a little bounce. */
@Composable
private fun Modifier.tiltClickable(maxTilt: Float = 12f, onClick: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val sc = remember { Animatable(1f) }
    val density = LocalDensity.current.density
    val latest by rememberUpdatedState(onClick)
    return this
        .graphicsLayer {
            rotationX = rx.value
            rotationY = ry.value
            scaleX = sc.value
            scaleY = sc.value
            cameraDistance = 14f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { p ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val down = spring<Float>(stiffness = Spring.StiffnessMedium)
                    scope.launch { ry.animateTo(((p.x / w) - 0.5f) * 2f * maxTilt, down) }
                    scope.launch { rx.animateTo(-((p.y / h) - 0.5f) * 2f * maxTilt, down) }
                    scope.launch { sc.animateTo(0.95f, down) }
                    tryAwaitRelease()
                    val back = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    scope.launch { ry.animateTo(0f, back) }
                    scope.launch { rx.animateTo(0f, back) }
                    scope.launch { sc.animateTo(1f, back) }
                },
                onTap = { latest() }
            )
        }
}

/**
 * Hero leaves like a layer under the page: it drifts up slower than the content (parallax),
 * zooms in a touch and fades out, instead of shrinking back. Content slides over it.
 */
private fun Modifier.heroSlideAway(state: LazyListState): Modifier = graphicsLayer {
    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "home_hero_banner" }
    if (info == null) {
        alpha = 0f
        return@graphicsLayer
    }
    val scrolled = (-info.offset).toFloat().coerceAtLeast(0f)
    val frac = (scrolled / info.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    transformOrigin = TransformOrigin(0.5f, 0f)
    translationY = scrolled * 0.55f
    val s = 1f + 0.06f * frac
    scaleX = s
    scaleY = s
    alpha = (1f - frac * 1.25f).coerceIn(0f, 1f)
}

// ── Overlapped poster row (front card big, the rest tucked behind, 3D on swipe) ──

private val OverlapCardW = 150.dp
private val OverlapCardH = 214.dp
private val OverlapAmount = 38.dp

@Composable
private fun OttOverlapMediaRow(list: List<Anime>, onAnimeClick: (Int) -> Unit) {
    val listState = rememberLazyListState()
    val stepPx = with(LocalDensity.current) { (OverlapCardW - OverlapAmount).toPx() }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 56.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(-OverlapAmount)
    ) {
        itemsIndexed(list, key = { _, a -> a.id }) { idx, anime ->
            // 0 = card in the front slot, 1 = next one behind it, negative = leaving to the left
            val rel: () -> Float = {
                val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == idx }
                if (info == null) 4f else info.offset / stepPx
            }
            OverlapPosterCard(
                anime = anime,
                depth = (list.size - idx).toFloat(),
                rel = rel,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

@Composable
private fun OverlapPosterCard(anime: Anime, depth: Float, rel: () -> Float, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(20.dp)
    val meta = remember(anime.id) {
        listOfNotNull(anime.displayFormat, anime.score?.let { "$it%" }).joinToString(" · ")
    }
    Box(
        Modifier
            .zIndex(depth) // earlier cards sit on top of later ones
            .graphicsLayer {
                val r = rel()
                val behind = r.coerceIn(0f, 3f)
                val leaving = (-r).coerceIn(0f, 1f)
                val s = 1f - 0.075f * behind - 0.10f * leaving
                scaleX = s
                scaleY = s
                rotationY = -9f * behind.coerceAtMost(2f) + 26f * leaving
                alpha = 1f - 0.55f * leaving
                cameraDistance = 14f * density
            }
            .width(OverlapCardW)
            .height(OverlapCardH)
            .tiltClickable(onClick = onClick)
            .clip(shape)
            .background(c.skeleton)
    ) {
        AsyncImage(
            model = anime.cover,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xCC000000))
            )
        )
        // cards further back get darker, so the front one reads as the focus
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val r = rel()
                    alpha = (0.16f * r.coerceIn(0f, 3f) + 0.30f * (-r).coerceIn(0f, 1f)).coerceAtMost(0.5f)
                }
                .background(Color.Black)
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = anime.displayTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = meta, fontSize = 11.sp, color = Color(0xB3FFFFFF), maxLines = 1)
            }
        }
    }
}

// ── Random episode cards dropped between shelves ──

private fun LazyListScope.episodeBreak(
    slot: String,
    pool: List<Anime>,
    order: Int,
    vm: MainViewModel,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit
) {
    val anime = pool.getOrNull(order) ?: return
    item(key = "ep_break_${slot}_${anime.id}") {
        val epNum = remember(anime.id, anime.episodes, anime.isMovie) {
            if (anime.isMovie || anime.episodes == 1) 1
            else {
                val maxEp = (anime.episodes ?: 12).coerceAtLeast(1)
                ((anime.id % maxEp) + 1).coerceIn(1, maxEp)
            }
        }
        val titles = listOf("Jump Into an Episode", "Pick Up Here", "Top Episode Right Now", "Don't Miss This")
        val artwork = vm.tmdbArtworks[anime.id]
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            OttSectionHeader(title = titles[order % titles.size])
            YouTubeStyleEpisodeCard(
                anime = anime,
                episodeNumber = epNum,
                badgeText = "TOP EPISODE",
                episodeStill = vm.tmdbEpisodeStills["${anime.id}_$epNum"],
                logoUrl = artwork?.logoUrl,
                tmdbTitle = artwork?.title,
                onLoadStill = { vm.loadTmdbEpisodeStill(anime, epNum) },
                onLoadArtwork = { vm.loadTmdbArtwork(anime) },
                onAnimeClick = onAnimeClick,
                onPlayClick = onPlayClick
            )
        }
    }
}

// ── Continue row: wide cards, progress bar, type chip, quick remove ──

private enum class ContinueKind(val label: String) { ANIME("ANIME"), MANGA("MANGA"), NOVEL("NOVEL") }

private class ContinueEntry(
    val key: String,
    val kind: ContinueKind,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val image: () -> Any?,
    val onOpen: () -> Unit,
    val onRemove: () -> Unit,
    val loadStill: (() -> Unit)? = null
)

private fun firstNonBlank(vararg s: String?): String? = s.firstOrNull { !it.isNullOrBlank() }

private fun Number?.asLong(): Long = this?.toLong() ?: 0L

@Composable
private fun HomeContinueRow(
    animeHistory: List<WatchHistoryItem>,
    mangaHistory: List<MangaHistoryItem>,
    novelHistory: List<NovelHistoryItem>,
    stillFor: (Int, Int) -> String?,
    onLoadEpisodeStill: (Int, Int) -> Unit,
    onPlayAnime: (Int, Int) -> Unit,
    onReadManga: (String, String) -> Unit,
    onReadNovel: (String, String) -> Unit,
    onRemoveAnime: (Int) -> Unit,
    onRemoveManga: (String) -> Unit,
    onRemoveNovel: (String) -> Unit
) {
    val entries = remember(animeHistory, mangaHistory, novelHistory) {
        val out = ArrayList<ContinueEntry>()
        animeHistory.forEach { h ->
            val ep = h.lastEpisode
            val pos = h.positionMs.asLong()
            val dur = h.durationMs.asLong()
            val left = ((dur - pos) / 60_000L).coerceAtLeast(1L)
            out += ContinueEntry(
                key = "a_${h.animeId}",
                kind = ContinueKind.ANIME,
                title = h.title,
                subtitle = "EP $ep" + if (dur > 0L) " · ${left}m left" else "",
                progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f,
                image = { firstNonBlank(stillFor(h.animeId, ep), h.banner, h.cover) },
                onOpen = { onPlayAnime(h.animeId, ep) },
                onRemove = { onRemoveAnime(h.animeId) },
                loadStill = { onLoadEpisodeStill(h.animeId, ep) }
            )
        }
        mangaHistory.forEach { m ->
            val total = m.pageCount
            out += ContinueEntry(
                key = "m_${m.mangaId}",
                kind = ContinueKind.MANGA,
                title = m.title,
                subtitle = "Ch ${m.chapterNumber}" + if (total > 0) " · p${m.pageIndex + 1}/$total" else "",
                progress = if (total > 0) ((m.pageIndex + 1).toFloat() / total).coerceIn(0f, 1f) else 0f,
                image = { m.poster },
                onOpen = { onReadManga(m.mangaId, m.chapterId) },
                onRemove = { onRemoveManga(m.mangaId) }
            )
        }
        novelHistory.forEach { n ->
            val chapter = (n.lastChapterTitle as String?)?.takeIf { it.isNotBlank() } ?: "Continue reading"
            out += ContinueEntry(
                key = "n_${n.slug}",
                kind = ContinueKind.NOVEL,
                title = n.title,
                subtitle = chapter,
                progress = 0f,
                image = { n.cover },
                onOpen = { onReadNovel(n.slug, n.lastChapterSlug ?: "") },
                onRemove = { onRemoveNovel(n.slug) }
            )
        }
        out.take(20)
    }
    if (entries.isEmpty()) return

    Column(Modifier.fillMaxWidth()) {
        OttSectionHeader(title = "Continue")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.key }) { e -> ContinueCard(e) }
        }
    }
}

@Composable
private fun ContinueCard(e: ContinueEntry) {
    val c = LocalAppColors.current
    LaunchedEffect(e.key) { e.loadStill?.invoke() }
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .width(272.dp)
            .aspectRatio(16f / 9f)
            .tiltClickable(maxTilt = 8f, onClick = e.onOpen)
            .clip(shape)
            .background(c.skeleton)
    ) {
        AsyncImage(
            model = e.image(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = if (e.kind == ContinueKind.ANIME) Alignment.Center else Alignment.TopCenter
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0x55000000), 0.4f to Color.Transparent, 1f to Color(0xE6000000))
            )
        )
        // type chip
        Text(
            text = e.kind.label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
        // quick remove
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .clickable(onClick = e.onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        // title + subtitle + play
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(e.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(e.subtitle, fontSize = 11.5.sp, color = Color(0xB3FFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(c.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(22.dp))
            }
        }
        // progress
        if (e.progress > 0f) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x40FFFFFF)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(e.progress).height(3.dp).background(c.accent))
        }
    }
}
