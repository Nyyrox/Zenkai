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
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import kotlinx.coroutines.launch
import kotlin.math.abs
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

        // Home-wide glass state (Profile → Liquid Glassmorphism). Ambient art takes its colour from the active hero card.
        val glassOn = rememberHomeGlass()
        val glassLayer = rememberPrismalGlassLayer()
        val backdrop = if (glassOn) glassLayer else null
        var heroAmbient by remember { mutableStateOf<String?>(null) }

        PlayerAmbientArt(layer = backdrop, model = heroAmbient, modifier = Modifier.fillMaxSize())

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            // Inline warning banner when using TMDB fallback data
            if (vm.homeError != null && vm.trending.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HomeMargin)
                            .frosted(glassOn, 14.dp, tint = Color(0xFF3A2F00))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
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

            // Spotlight hero (up to 15 shows)
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
                        HomeSpotlightHero(
                            shows = heroShows,
                            glassOn = glassOn,
                            backdrop = backdrop,
                            logoUrlOf = { vm.tmdbArtworks[it]?.logoUrl },
                            onLoadArtwork = { vm.loadTmdbArtwork(it) },
                            isInWatchlist = { vm.isInWatchlist(it) },
                            onToggleWatchlist = { vm.toggleWatchlist(it) },
                            onAnimeClick = onAnimeClick,
                            onPlayClick = { onPlayClick(it, 1) },
                            isVerticalScrollInProgress = lazyListState.isScrollInProgress,
                            onActiveChanged = { heroAmbient = it.banner?.takeIf { b -> b.isNotBlank() } ?: it.cover }
                        )
                    }
                }
            }

            // Continue Watching & Reading (unified anime / manga / novel history)
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

            // Trending episodes
            if (trendingTopEpisodes.isNotEmpty()) {
                item(key = "home_trending_episodes") {
                    HomeSection(
                        title = "Trending Top Watched",
                        subtitle = "Popular episodes streaming right now",
                        modifier = Modifier.staggeredEntrance(index = 2)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            trendingTopEpisodes.forEachIndexed { idx, anime ->
                                key(anime.id) {
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
                                        backdrop = backdrop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top 10
            if (vm.trending.isNotEmpty()) {
                item(key = "home_top_10") {
                    HomeSection(
                        title = "Top 10 Today",
                        subtitle = "Most streamed titles right now",
                        modifier = Modifier.staggeredEntrance(index = 2)
                    ) {
                        HomeTop10Row(list = vm.trending, glassOn = glassOn, onAnimeClick = onAnimeClick)
                    }
                }
            }

            // Popular
            if (vm.popular.isNotEmpty()) {
                item(key = "home_popular_series") {
                    HomeSection("Popular Series", "Fan favorites and trending hits", Modifier.staggeredEntrance(index = 3)) {
                        HomePosterRow(vm.popular, glassOn, onAnimeClick)
                    }
                }
            }

            // Must-watch spotlight (between Popular and Critically Acclaimed)
            val spotlight1 = vm.popular.getOrNull(1) ?: vm.trending.getOrNull(2)
            if (spotlight1 != null) {
                item(key = "spotlight_popular_${spotlight1.id}") {
                    val artwork = vm.tmdbArtworks[spotlight1.id]
                    val epStill = vm.tmdbEpisodeStills["${spotlight1.id}_1"]
                    HomeSection("Must Watch Spotlight", "Trending worldwide on Zenkai", Modifier.staggeredEntrance(index = 4)) {
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
                            onPlayClick = onPlayClick,
                            backdrop = backdrop
                        )
                    }
                }
            }

            // Top rated
            if (vm.topRated.isNotEmpty()) {
                item(key = "home_top_rated") {
                    HomeSection("Critically Acclaimed", "Highest rated by global community", Modifier.staggeredEntrance(index = 4)) {
                        HomePosterRow(vm.topRated, glassOn, onAnimeClick)
                    }
                }
            }

            // Action
            if (vm.actionAnime.isNotEmpty()) {
                item(key = "home_action_anime") {
                    HomeSection("Action & High Adrenaline", "Epic fights and supernatural showdowns", Modifier.staggeredEntrance(index = 5)) {
                        HomePosterRow(vm.actionAnime, glassOn, onAnimeClick)
                    }
                }
            }

            // Action spotlight
            val actionSpotlight = vm.actionAnime.firstOrNull()
            if (actionSpotlight != null && actionSpotlight.id != spotlight1?.id) {
                item(key = "spotlight_action_${actionSpotlight.id}") {
                    val artwork = vm.tmdbArtworks[actionSpotlight.id]
                    val epStill = vm.tmdbEpisodeStills["${actionSpotlight.id}_1"]
                    HomeSection("Action Spotlight", "High octane showdowns and epic fights", Modifier.staggeredEntrance(index = 6)) {
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
                            onPlayClick = onPlayClick,
                            backdrop = backdrop
                        )
                    }
                }
            }

            // Fantasy
            if (vm.fantasyAnime.isNotEmpty()) {
                item(key = "home_fantasy_anime") {
                    HomeSection("Fantasy & Magic", "Immersive worlds and magical adventures", Modifier.staggeredEntrance(index = 6)) {
                        HomePosterRow(vm.fantasyAnime, glassOn, onAnimeClick)
                    }
                }
            }

            // New releases
            if (vm.newReleases.isNotEmpty()) {
                item(key = "home_new_releases") {
                    HomeSection("New Releases", "Freshly airing broadcast episodes", Modifier.staggeredEntrance(index = 7)) {
                        HomePosterRow(vm.newReleases, glassOn, onAnimeClick)
                    }
                }
            }

            // Upcoming
            if (vm.upcomingAnime.isNotEmpty()) {
                item(key = "home_upcoming_anime") {
                    HomeSection("Upcoming Shows", "Most anticipated upcoming titles", Modifier.staggeredEntrance(index = 8)) {
                        HomePosterRow(vm.upcomingAnime, glassOn, onAnimeClick)
                    }
                }
            }

            // Sci-Fi
            if (vm.scifiAnime.isNotEmpty()) {
                item(key = "home_scifi_anime") {
                    HomeSection("Sci-Fi & Cyberpunk", "Futuristic realms and tech thrillers", Modifier.staggeredEntrance(index = 9)) {
                        HomePosterRow(vm.scifiAnime, glassOn, onAnimeClick)
                    }
                }
            }

            // Romance
            if (vm.romanceAnime.isNotEmpty()) {
                item(key = "home_romance_anime") {
                    HomeSection("Romance & Slice of Life", "Heartwarming stories and emotional journeys", Modifier.staggeredEntrance(index = 10)) {
                        HomePosterRow(vm.romanceAnime, glassOn, onAnimeClick)
                    }
                }
            }

            // Manga
            val mangaList = vm.mangaHome.trending.ifEmpty { vm.mangaHome.latest }
            if (mangaList.isNotEmpty()) {
                item(key = "home_popular_manga") {
                    HomeSection("Popular Manga", "Trending manga, manhwa & comics", Modifier.staggeredEntrance(index = 10)) {
                        HomeMangaRow(list = mangaList, glassOn = glassOn, onMangaClick = onMangaClick)
                    }
                }
            }

            // Light novels
            val novelList = vm.novelPopular.ifEmpty { vm.novelLightNovels }
            if (novelList.isNotEmpty()) {
                item(key = "home_popular_novels") {
                    HomeSection("Top Light Novels", "Most read novels & web serials", Modifier.staggeredEntrance(index = 11)) {
                        HomeNovelRow(list = novelList, glassOn = glassOn, onNovelClick = onNovelClick)
                    }
                }
            }

            // Featured spotlight (between novels and the explore feed)
            val featureSpotlight = vm.scifiAnime.firstOrNull() ?: vm.fantasyAnime.firstOrNull()
            if (featureSpotlight != null && featureSpotlight.id != spotlight1?.id && featureSpotlight.id != actionSpotlight?.id) {
                item(key = "spotlight_feature_${featureSpotlight.id}") {
                    val artwork = vm.tmdbArtworks[featureSpotlight.id]
                    val epStill = vm.tmdbEpisodeStills["${featureSpotlight.id}_1"]
                    HomeSection("Featured Spotlight", "Handpicked masterpiece with full episodes", Modifier.staggeredEntrance(index = 11)) {
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
                            onPlayClick = onPlayClick,
                            backdrop = backdrop
                        )
                    }
                }
            }

            // Explore feed: episode cards with a genre shelf woven in every second card
            if (exploreList.isNotEmpty()) {
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

                    Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (index == 0) {
                                HomeSectionHeader(
                                    title = "Top Episodes & Discover",
                                    subtitle = "Stream top episodes and explore genres"
                                )
                            }
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
                                onPlayClick = onPlayClick,
                                backdrop = backdrop
                            )
                        }

                        if (genreShelves.isNotEmpty() && (index % 2 == 1)) {
                            val shelfIdx = (index / 2) % genreShelves.size
                            val (genreTitle, genreList) = genreShelves[shelfIdx]
                            HomeSection(genreTitle, "Top recommended titles in this genre") {
                                HomePosterRow(genreList, glassOn, onAnimeClick)
                            }
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

// ─────────────────────────────────────────────────────────────────────────────────────────
//  HOME DESIGN SYSTEM
//  One margin, one rhythm, one surface language. Glass on  = rim-lit frosted surfaces + Prismal
//  shader buttons over a drifting ambient backdrop. Glass off = flat solid surfaces, same layout.
// ─────────────────────────────────────────────────────────────────────────────────────────

private val HomeMargin = 20.dp

/** Same switch as the details/player pages (Profile → Liquid Glassmorphism), gated to Android 13+. */
@Composable
private fun rememberHomeGlass(): Boolean =
    LocalThemeSettings.current.isGlassmorphismEnabled && Build.VERSION.SDK_INT >= 33

/**
 * Playful press: springy squish, a radial light that blooms from the exact touch point, haptic tick on tap,
 * optional long-press with a stronger haptic. Clips to [shape] so the glow follows rounded corners.
 * Place it BEFORE background/border modifiers so everything scales together.
 */
private fun Modifier.liquidPress(
    shape: Shape,
    pressedScale: Float = 0.96f,
    onLongTap: (() -> Unit)? = null,
    onTap: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var glowCenter by remember { mutableStateOf(Offset.Unspecified) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lpScale"
    )
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 90 else 420),
        label = "lpGlow"
    )
    val currentTap by rememberUpdatedState(onTap)
    val currentLong by rememberUpdatedState(onLongTap)
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (glow > 0.01f && glowCenter.isSpecified) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f * glow), Color.Transparent),
                        center = glowCenter,
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    glowCenter = offset
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = {
                    currentLong?.let { action ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                },
                onTap = {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    currentTap()
                }
            )
        }
        .semantics(mergeDescendants = true) {
            role = Role.Button
            onClick { currentTap(); true }
        }
}

/**
 * Surface used for every info panel/chip. Glass on: tinted translucent body, top sheen and a rim-light
 * border (bright top-left, soft bottom-right). Glass off: solid card with a hairline.
 */
private fun Modifier.frosted(glassOn: Boolean, radius: Dp, tint: Color = Color(0xFF0B0C10)): Modifier {
    val shape = RoundedCornerShape(radius)
    return if (glassOn) {
        this
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.52f), tint.copy(alpha = 0.80f))))
            .background(Brush.linearGradient(listOf(Color(0x22FFFFFF), Color.Transparent)))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color(0x75FFFFFF), Color(0x0FFFFFFF), Color(0x3AFFFFFF))),
                shape
            )
    } else {
        this
            .clip(shape)
            .background(Color(0xF00B0B10))
            .border(1.dp, Color(0x1FFFFFFF), shape)
    }
}

@Composable
private fun GlassChip(
    glassOn: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .frosted(glassOn, 10.dp, tint)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

/** Accent bar + title + optional subtitle. Every section on Home starts with this. */
@Composable
private fun HomeSectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HomeMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (subtitle != null) 36.dp else 22.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0x22FFFFFF))))
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = OttTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(title, subtitle)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/** Pops its content and fires an expanding gold ring when [trigger] flips (skips first composition). */
@Composable
private fun HomePop(trigger: Boolean, content: @Composable () -> Unit) {
    val pop = remember { Animatable(1f) }
    val ring = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        launch {
            pop.snapTo(0.55f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
        }
        ring.snapTo(0f)
        ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        ring.snapTo(0f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .drawBehind {
                val r = ring.value
                if (r > 0f) {
                    drawCircle(
                        color = Color(0xFFFFD166).copy(alpha = (1f - r) * 0.85f),
                        radius = size.minDimension / 2f * (1f + r * 1.2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
    ) { content() }
}

/** Big bookmark that pops in the middle of a card (long-press watchlist). */
@Composable
private fun BookmarkBurst(trigger: Int, added: Boolean, modifier: Modifier = Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            p.snapTo(0f)
            p.animateTo(1f, tween(800, easing = LinearEasing))
            p.snapTo(0f)
        }
    }
    val v = p.value
    if (v > 0f) {
        val pop = if (v < 0.3f) v / 0.3f * 1.3f else 1.3f - (v - 0.3f) / 0.7f * 0.3f
        val a = if (v < 0.65f) 1f else ((1f - v) / 0.35f)
        Box(
            modifier = modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                    alpha = a.coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0x70000000))
            )
            Icon(
                imageVector = if (added) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null,
                tint = if (added) OttAccentGold else Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
//  SPOTLIGHT HERO
//  A peeking deck of tall cards: the centre card is lit, neighbours tilt and dim, artwork parallaxes
//  inside each card. Tap = open, long-press = watchlist (with a bookmark burst). Auto-advances with a
//  progress-filled indicator that pauses while you touch or scroll.
// ─────────────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSpotlightHero(
    shows: List<Anime>,
    glassOn: Boolean,
    backdrop: PrismalGlassLayer?,
    logoUrlOf: (Int) -> String?,
    onLoadArtwork: (Anime) -> Unit,
    isInWatchlist: (Int) -> Boolean,
    onToggleWatchlist: (Anime) -> Unit,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit,
    isVerticalScrollInProgress: Boolean,
    onActiveChanged: (Anime) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { shows.size })
    val scope = rememberCoroutineScope()
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    val progress = remember { Animatable(0f) }
    val paused = dragged || isVerticalScrollInProgress

    LaunchedEffect(pagerState.settledPage) {
        shows.getOrNull(pagerState.settledPage)?.let(onActiveChanged)
    }
    // Auto-advance: fill the active dot over 6.5s, then slide to the next card. Restarts after touch.
    LaunchedEffect(pagerState.settledPage, paused, shows.size) {
        progress.snapTo(0f)
        if (!paused && shows.size > 1) {
            progress.animateTo(1f, tween(6500, easing = LinearEasing))
            pagerState.animateScrollToPage((pagerState.settledPage + 1) % shows.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeMargin),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SPOTLIGHT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = OttTextMuted
            )
            Text(
                text = "%02d / %02d".format(pagerState.currentPage + 1, shows.size),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OttTextMuted
            )
        }
        Spacer(Modifier.height(14.dp))

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 34.dp),
            pageSpacing = 12.dp,
            key = { shows[it].id },
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val anime = shows[page]
            LaunchedEffect(anime.id) { onLoadArtwork(anime) }
            SpotlightCard(
                anime = anime,
                page = page,
                pagerState = pagerState,
                glassOn = glassOn,
                backdrop = backdrop,
                logoUrl = logoUrlOf(anime.id),
                inWatchlist = isInWatchlist(anime.id),
                onToggleWatchlist = { onToggleWatchlist(anime) },
                onOpen = { onAnimeClick(anime.id) },
                onPlay = { onPlayClick(anime.id) }
            )
        }

        Spacer(Modifier.height(16.dp))
        SpotlightIndicator(
            count = shows.size,
            current = pagerState.currentPage,
            progressProvider = { progress.value },
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpotlightCard(
    anime: Anime,
    page: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    glassOn: Boolean,
    backdrop: PrismalGlassLayer?,
    logoUrl: String?,
    inWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val shape = RoundedCornerShape(30.dp)
    var burst by remember { mutableIntStateOf(0) }
    // Read inside graphicsLayer lambdas only, so swiping never recomposes the card.
    val offsetOf: () -> Float = { (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .graphicsLayer {
                val off = offsetOf()
                val a = abs(off).coerceIn(0f, 1f)
                val s = 1f - 0.07f * a
                scaleX = s
                scaleY = s
                alpha = 1f - 0.5f * a
                rotationY = off * -10f
                cameraDistance = 14f * density
                translationY = a * 10.dp.toPx()
            }
            .liquidPress(
                shape = shape,
                pressedScale = 0.97f,
                onLongTap = {
                    onToggleWatchlist()
                    burst++
                },
                onTap = onOpen
            )
            .background(Color(0xFF12141C))
    ) {
        // Artwork, drifting against the swipe for depth
        AsyncImage(
            model = anime.cover,
            contentDescription = anime.displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.2f
                    scaleY = 1.2f
                    translationX = offsetOf() * size.width * 0.09f
                }
        )
        // Light falloff: dark at the top for chips, deep at the bottom for the info panel
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66000000),
                        0.28f to Color.Transparent,
                        0.55f to Color(0x22000000),
                        1f to Color(0xE6000000)
                    )
                )
        )
        // Rim light around the whole card
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (glassOn) Brush.linearGradient(listOf(Color(0x80FFFFFF), Color(0x08FFFFFF), Color(0x40FFFFFF)))
                    else SolidColor(Color(0x18FFFFFF)),
                    shape
                )
        )

        // Top row
        GlassChip(
            glassOn = glassOn,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(13.dp))
            Text("FEATURED", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            PlayerGlassButton(
                backdrop = backdrop,
                onClick = {
                    onToggleWatchlist()
                    burst++
                },
                circle = true,
                height = 40.dp
            ) {
                HomePop(inWatchlist) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (inWatchlist) OttAccentGold else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Info panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .frosted(glassOn, 24.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = anime.displayTitle,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .heightIn(max = 46.dp)
                )
            } else {
                Text(
                    text = anime.displayTitle,
                    fontSize = 23.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                anime.score?.let { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(13.dp))
                        Text("$s%", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OttAccentGold)
                    }
                }
                val meta = listOfNotNull(
                    if (anime.isMovie) "Movie" else (anime.displayFormat ?: "TV"),
                    anime.episodes?.takeIf { !anime.isMovie && it > 0 }?.let { "$it eps" },
                    anime.displayYear?.takeIf { it.isNotBlank() }
                ).joinToString("  •  ")
                Text(meta, fontSize = 12.5.sp, color = OttTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (anime.genres.isNotEmpty()) {
                Text(
                    text = anime.genres.take(3).joinToString("  •  "),
                    fontSize = 12.sp,
                    color = OttTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomePlayCta(
                    label = if (anime.isMovie) "Watch Movie" else "Watch Now",
                    glassOn = glassOn,
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    onClick = onPlay
                )
                PlayerGlassButton(backdrop = backdrop, onClick = onOpen, circle = true, height = 46.dp) {
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        BookmarkBurst(trigger = burst, added = inWatchlist, modifier = Modifier.align(Alignment.Center))
    }
}

/** Primary call-to-action: Prismal glass pill when glass is on, bold white pill when off. */
@Composable
private fun HomePlayCta(
    label: String,
    glassOn: Boolean,
    backdrop: PrismalGlassLayer?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (glassOn && backdrop != null) {
        val context = LocalContext.current
        PlayerGlassButton(
            backdrop = backdrop,
            onClick = {
                com.nyyrox.aniverse.util.AppHaptics.tick(context)
                onClick()
            },
            modifier = modifier,
            height = 46.dp
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
        }
    } else {
        Row(
            modifier = modifier
                .height(46.dp)
                .liquidPress(CircleShape, 0.94f, onTap = onClick)
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(23.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, softWrap = false)
        }
    }
}

/** Dot rail: the active dot stretches into a capsule that fills as the auto-advance timer runs. Tap a dot to jump. */
@Composable
private fun SpotlightIndicator(
    count: Int,
    current: Int,
    progressProvider: () -> Float,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val active = i == current
            val w by animateDpAsState(
                targetValue = if (active) 28.dp else 6.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "dotW"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(i) }
                    .drawBehind {
                        if (active) {
                            drawRect(
                                color = Color.White,
                                size = androidx.compose.ui.geometry.Size(size.width * progressProvider().coerceIn(0f, 1f), size.height)
                            )
                        }
                    }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
//  POSTER TILES + SHELVES
// ─────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomePosterTile(
    image: Any?,
    title: String,
    subtitle: String?,
    glassOn: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 128.dp,
    score: Int? = null,
    badge: String? = null,
    badgeColor: Color = Color.White,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.width(width)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .liquidPress(shape, 0.94f, onTap = onClick)
                .background(Color(0xFF141822))
        ) {
            AsyncImage(
                model = image,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        1.dp,
                        if (glassOn) Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x08FFFFFF), Color(0x33FFFFFF)))
                        else SolidColor(Color(0x14FFFFFF)),
                        shape
                    )
            )
            if (score != null) {
                GlassChip(
                    glassOn = glassOn,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = OttAccentGold, modifier = Modifier.size(11.dp))
                    Text("$score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (!badge.isNullOrBlank()) {
                GlassChip(
                    glassOn = glassOn,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = OttTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun HomePosterRow(
    list: List<Anime>,
    glassOn: Boolean,
    onAnimeClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeMargin),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { it.id }) { anime ->
            HomePosterTile(
                image = anime.cover,
                title = anime.displayTitle,
                subtitle = listOfNotNull(
                    anime.genres.firstOrNull(),
                    if (anime.isMovie) "Movie" else anime.displayFormat
                ).joinToString("  •  "),
                glassOn = glassOn,
                score = anime.score,
                onClick = { onAnimeClick(anime.id) }
            )
        }
    }
}

/** Big gradient rank numerals tucked behind the posters. */
@Composable
private fun HomeTop10Row(
    list: List<Anime>,
    glassOn: Boolean,
    onAnimeClick: (Int) -> Unit
) {
    val top = remember(list) { list.take(10) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeMargin),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(top, key = { _, a -> "top10_${a.id}" }) { i, anime ->
            Box(Modifier.width(if (i == 9) 196.dp else 170.dp)) {
                Text(
                    text = "${i + 1}",
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(Color.White, Color(0x1FFFFFFF))),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-10).sp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-2).dp, y = (-26).dp)
                )
                HomePosterTile(
                    image = anime.cover,
                    title = anime.displayTitle,
                    subtitle = anime.genres.firstOrNull(),
                    glassOn = glassOn,
                    width = 122.dp,
                    score = anime.score,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { onAnimeClick(anime.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeMangaRow(
    list: List<com.nyyrox.aniverse.data.manga.MangaCard>,
    glassOn: Boolean,
    onMangaClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeMargin),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { "manga_${it.id}" }) { manga ->
            val chText: String = manga.chapter?.takeIf { it.isNotBlank() } ?: (manga.type ?: "Manga")
            HomePosterTile(
                image = manga.poster,
                title = manga.title,
                subtitle = manga.type?.takeIf { it.isNotBlank() },
                glassOn = glassOn,
                badge = chText,
                badgeColor = OttAccentGold,
                onClick = { onMangaClick(manga.id) }
            )
        }
    }
}

@Composable
private fun HomeNovelRow(
    list: List<com.nyyrox.aniverse.data.novel.NovelItem>,
    glassOn: Boolean,
    onNovelClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeMargin),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(list, key = { "novel_${it.slug}" }) { novel ->
            val badge: String = novel.rating?.takeIf { it.isNotBlank() } ?: (novel.stats?.takeIf { it.isNotBlank() } ?: novel.source)
            HomePosterTile(
                image = novel.cover,
                title = novel.title,
                subtitle = novel.source.takeIf { it.isNotBlank() },
                glassOn = glassOn,
                badge = badge,
                onClick = { onNovelClick(novel.slug) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────
//  EPISODE SHOWDOWN CARD
// ─────────────────────────────────────────────────────────────────────────────────────────

/**
 * Cinematic episode card: full-bleed still, glass info panel docked at the bottom with a Prismal play button.
 * Tap the artwork to play, tap the title block to open details. Pass [backdrop] (null = flat, glass off).
 * TMDB stills load lazily with fallback to the AniList banner/cover.
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
    modifier: Modifier = Modifier,
    backdrop: PrismalGlassLayer? = null
) {
    LaunchedEffect(anime.id, episodeNumber) {
        onLoadStill?.invoke()
        onLoadArtwork?.invoke()
    }
    val glassOn = backdrop != null
    val shape = RoundedCornerShape(28.dp)
    val context = LocalContext.current

    val durationText = remember(anime.duration) { "${anime.duration ?: 24}m" }
    val preferredTitle = tmdbTitle?.takeIf { it.isNotBlank() } ?: anime.displayTitle
    val thumbnailImage = remember(episodeStill, anime.banner, anime.cover) {
        episodeStill?.takeIf { it.isNotBlank() }
            ?: anime.banner?.takeIf { it.isNotBlank() }
            ?: anime.cover
    }
    val isMustWatch = badgeText?.contains("MUST WATCH", ignoreCase = true) == true

    Box(
        modifier = modifier
            .padding(horizontal = HomeMargin)
            .fillMaxWidth()
            .aspectRatio(16f / 10.5f)
            .liquidPress(shape, 0.975f, onTap = { onPlayClick(anime.id, episodeNumber) })
            .background(Color(0xFF131722))
    ) {
        SubcomposeAsyncImage(
            model = thumbnailImage,
            contentDescription = preferredTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), shape = shape) },
            error = {
                AsyncImage(
                    model = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                    contentDescription = preferredTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66000000),
                        0.3f to Color.Transparent,
                        1f to Color(0xCC000000)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (glassOn) Brush.linearGradient(listOf(Color(0x80FFFFFF), Color(0x08FFFFFF), Color(0x40FFFFFF)))
                    else SolidColor(Color(0x18FFFFFF)),
                    shape
                )
        )

        if (!badgeText.isNullOrBlank()) {
            GlassChip(
                glassOn = glassOn,
                tint = if (isMustWatch) Color(0xFFB00A12) else Color.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Color.White)
            }
        }
        GlassChip(
            glassOn = glassOn,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
        ) {
            Text(durationText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Info dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .frosted(glassOn, 22.dp)
                .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        com.nyyrox.aniverse.util.AppHaptics.tick(context)
                        onAnimeClick(anime.id)
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = preferredTitle,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(max = 34.dp)
                    )
                } else {
                    Text(
                        text = preferredTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val meta = listOfNotNull(
                    if (anime.isMovie) "Full Movie" else "Episode $episodeNumber",
                    anime.genres.take(2).joinToString(" • ").ifBlank { null },
                    anime.score?.let { "$it% Match" }
                ).joinToString("  •  ")
                Text(
                    text = meta,
                    fontSize = 11.5.sp,
                    color = OttTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            PlayerGlassButton(
                backdrop = backdrop,
                onClick = {
                    com.nyyrox.aniverse.util.AppHaptics.tick(context)
                    onPlayClick(anime.id, episodeNumber)
                },
                circle = true,
                height = 46.dp
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(26.dp))
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

    val totalDownloadsCount = downloadedAnime.size + downloadedManga.size + downloadedNovels.size
    val totalDownloadedBytes = remember(downloadedAnime, downloadedManga, downloadedNovels) {
        downloadedAnime.sumOf { it.totalSizeBytes } +
            downloadedManga.sumOf { it.totalSizeBytes } +
            downloadedNovels.sumOf { it.totalSizeBytes }
    }
    val hasAnyHistory = vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Hero Offline Status Card
        item(key = "offline_hero_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .iosGlass(
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color(0xD81C1D24),
                        borderWidth = 1.dp
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x26F59E0B))
                                    .border(1.dp, Color(0x4DF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Downloading,
                                    contentDescription = "Offline",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x28F59E0B))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OFFLINE MODE",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "You're Offline",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (totalDownloadsCount > 0) {
                            "No internet connection detected. You have $totalDownloadsCount downloaded title(s) (${formatBytes(totalDownloadedBytes)}) ready to watch and read offline."
                        } else {
                            "No internet connection detected. Connect to Wi-Fi or mobile data to explore trending shows, or browse your downloaded library."
                        },
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(16.dp))

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
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Explore Downloads",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { vm.loadHome() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x28FFFFFF),
                                contentColor = Color.White
                            ),
                            shape = OttPillShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(5.dp))
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

        // Continue Watching & Reading Row (if history exists)
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

        // Downloaded Anime Shelf
        if (downloadedAnime.isNotEmpty()) {
            item(key = "offline_downloaded_anime_header") {
                OttSectionHeader(
                    title = "Downloaded Anime",
                    subtitle = "${downloadedAnime.size} title(s) • ${formatBytes(downloadedAnime.sumOf { it.totalSizeBytes })}",
                    onSeeAllClick = onDownloadsClick
                )
            }
            item(key = "offline_downloaded_anime_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloadedAnime, key = { "offline_anime_${it.animeId}" }) { anime ->
                        OfflineMediaCard(
                            title = anime.title,
                            coverUrl = anime.cover,
                            countBadge = "${anime.episodesCount} EPS",
                            sizeBytes = anime.totalSizeBytes,
                            badgeColor = Color(0xFF60A5FA),
                            onClick = { onAnimeClick(anime.animeId) },
                            onDirectAction = { onPlayClick(anime.animeId, 1) },
                            directActionIcon = Icons.Default.PlayArrow
                        )
                    }
                }
            }
        }

        // Downloaded Manga Shelf
        if (downloadedManga.isNotEmpty()) {
            item(key = "offline_downloaded_manga_header") {
                OttSectionHeader(
                    title = "Downloaded Manga",
                    subtitle = "${downloadedManga.size} series • ${formatBytes(downloadedManga.sumOf { it.totalSizeBytes })}",
                    onSeeAllClick = onDownloadsClick
                )
            }
            item(key = "offline_downloaded_manga_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloadedManga, key = { "offline_manga_${it.mangaId}" }) { manga ->
                        OfflineMediaCard(
                            title = manga.title,
                            coverUrl = manga.poster,
                            countBadge = "${manga.chaptersCount} CH",
                            sizeBytes = manga.totalSizeBytes,
                            badgeColor = Color(0xFF34D399),
                            onClick = { onMangaClick(manga.mangaId) }
                        )
                    }
                }
            }
        }

        // Downloaded Novels Shelf
        if (downloadedNovels.isNotEmpty()) {
            item(key = "offline_downloaded_novels_header") {
                OttSectionHeader(
                    title = "Downloaded Novels",
                    subtitle = "${downloadedNovels.size} light novel(s) • ${formatBytes(downloadedNovels.sumOf { it.totalSizeBytes })}",
                    onSeeAllClick = onDownloadsClick
                )
            }
            item(key = "offline_downloaded_novels_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloadedNovels, key = { "offline_novel_${it.novelSlug}" }) { novel ->
                        OfflineMediaCard(
                            title = novel.title,
                            coverUrl = novel.cover,
                            countBadge = "${novel.chaptersCount} CH",
                            sizeBytes = novel.totalSizeBytes,
                            badgeColor = Color(0xFFF472B6),
                            onClick = { onNovelClick(novel.novelSlug) }
                        )
                    }
                }
            }
        }

        // Empty state when user has 0 downloads and 0 history
        if (totalDownloadsCount == 0 && !hasAnyHistory) {
            item(key = "offline_empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .iosGlass(
                            shape = RoundedCornerShape(20.dp),
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x1FFFFFFF)),
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
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
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

            // Scrim overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to Color(0xCC000000)
                        )
                    )
            )

            // Top badge: count badge (e.g. "12 eps" / "45 ch")
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD90F1015))
                    .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = countBadge,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Direct play or action button (if provided)
            if (onDirectAction != null && directActionIcon != null) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xCCFFFFFF))
                        .clickable(onClick = onDirectAction),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = directActionIcon,
                        contentDescription = "Action",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom size badge
            if (sizeBytes > 0L) {
                Text(
                    text = formatBytes(sizeBytes),
                    color = Color(0xCCFFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = title,
            color = OttTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


