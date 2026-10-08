package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import kotlin.math.cos
import kotlin.math.sin
import com.nyyrox.aniverse.data.tmdb.TmdbArtwork
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Premium 3D Stacked Tilted Hero Banner Carousel.
 *
 * Features:
 * - Dynamic backdrop thumbnail with crossfade transitioning to the active card's image.
 * - Upper 3D Media Card stack: center elevated card with lateral cards tilted inward
 *   (rotationZ & rotationY 3D yaw, scale, translationX, z-index).
 * - Smooth touch-responsive swipe gestures and side-card tap navigation.
 * - Cinematic gradient blends directly into the dark theme background.
 * - Lower OTT action deck: Watch Now button, My List toggle, Details button,
 *   ranking badge, and metadata chips.
 */
@Composable
fun OttHeroBanner(
    featuredList: List<Anime>,
    tmdbArtworks: Map<Int, TmdbArtwork>,
    onLoadArtwork: (Anime) -> Unit,
    isInWatchlist: (Int) -> Boolean,
    onToggleWatchlist: (Anime) -> Boolean,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit,
    isVerticalScrollInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (featuredList.isEmpty()) return

    val virtualPageCount = if (featuredList.isEmpty()) 0 else 10_000
    val initialPage = if (featuredList.isEmpty()) 0 else (5_000 - (5_000 % featuredList.size))

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Preload active and neighbor artworks for buttery smoothness
    val activeIndex = if (featuredList.isEmpty()) 0 else (pagerState.currentPage % featuredList.size)
    val activeAnime = featuredList[activeIndex]

    LaunchedEffect(activeIndex) {
        onLoadArtwork(activeAnime)
        // Preload next and prev artworks
        val nextIdx = (activeIndex + 1) % featuredList.size
        val prevIdx = if (activeIndex - 1 >= 0) activeIndex - 1 else featuredList.lastIndex
        onLoadArtwork(featuredList[nextIdx])
        onLoadArtwork(featuredList[prevIdx])
    }

    // Relaxed, gentle auto-cycle (7 seconds) - paused during active scroll to prevent hitching
    LaunchedEffect(pagerState, isVerticalScrollInProgress) {
        while (true) {
            delay(5000)
            if (!isVerticalScrollInProgress && !pagerState.isScrollInProgress && featuredList.isNotEmpty()) {
                val next = pagerState.currentPage + 1
                pagerState.animateScrollToPage(
                    page = next,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val activeArtwork = tmdbArtworks[activeAnime.id]
    val heroBackdropUrl = activeArtwork?.backdropUrl ?: activeAnime.banner ?: activeAnime.cover

    // Continuous slow ambient Ken Burns drift (14s cycle) that stays active from card change to next swipe
    val heroBgInfinite = rememberInfiniteTransition(label = "hero_bg_infinite")
    val ambientBgScale by heroBgInfinite.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_ambient_scale"
    )
    val ambientBgTransX by heroBgInfinite.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_ambient_trans_x"
    )

    // Gentle swipe responsiveness combined with continuous slow cinematic drift
    val swipeBgScaleOffset by animateFloatAsState(
        targetValue = abs(pagerState.currentPageOffsetFraction) * 0.04f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_swipe_scale"
    )
    val swipeBgTransXOffset by animateFloatAsState(
        targetValue = -pagerState.currentPageOffsetFraction * 18f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_swipe_trans_x"
    )

    val finalBgScale = ambientBgScale + swipeBgScaleOffset
    val finalBgTransX = ambientBgTransX + swipeBgTransXOffset

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .clipToBounds()
            .background(OttBg)
    ) {
        // ── 1. Dynamic Background Backdrop with continuous slow cinematic drift & blur ───
        AsyncImage(
            model = heroBackdropUrl,
            contentDescription = activeAnime.displayTitle,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = finalBgScale * 1.12f
                    scaleY = finalBgScale * 1.12f
                    translationX = finalBgTransX
                }
                .blur(24.dp),
            contentScale = ContentScale.Crop
        )

        // ── 2. Single Unified Vertical Gradient Overlay (Eliminates overdraw & blends smoothly) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC0A0C13),
                            Color(0x500A0C13),
                            Color(0x750A0C13),
                            Color(0xD0060608),
                            Color(0xFF060608)
                        )
                    )
                )
        )

        // ── 3. Main Content Container ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 78.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // ── Upper 3D Stacked Media Cards Deck (5 cards visible: center + 2 on left + 2 on right) ──
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(285.dp),
                contentAlignment = Alignment.Center
            ) {
                val screenWidth = maxWidth
                val cardWidth = 146.dp
                val cardHeight = 232.dp

                // Symmetrical horizontal padding so active center item is precisely centered
                val horizontalPadding = ((screenWidth - cardWidth) / 2).coerceAtLeast(0.dp)

                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fixed(cardWidth),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val anime = featuredList[page % featuredList.size]
                    val artwork = tmdbArtworks[anime.id]
                    val posterUrl = anime.cover ?: artwork?.backdropUrl ?: anime.banner

                    // Real-time offset relative to center (0 = center, positive = left, negative = right)
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val absOffset = abs(pageOffset)

                    // Continuous smooth scale with steeper perspective: Center = 1.0f, ±1 = 0.84f, ±2 = 0.68f
                    val scale = (1f - (absOffset * 0.16f)).coerceIn(0.68f, 1.0f)
                    // Continuous smooth alpha: Center = 1.0f, ±1 = 0.90f, ±2 = 0.68f
                    val alpha = (1f - (absOffset * 0.16f)).coerceIn(0.65f, 1.0f)
                    // zIndex: Center card sits on top (10), ±1 sits under center (9), ±2 sits at bottom (8)
                    val zIndexVal = (10f - absOffset).coerceAtLeast(0f)

                    // Pull translation to eliminate excess gaps and create tight stacked fan
                    val pullTranslationX = pageOffset * 50f * density

                    // 3D Arc translation along Y: background cards curve down slightly along depth stage
                    val depthTranslationY = (absOffset * absOffset * 6.5f * density).coerceAtMost(32f * density)

                    // 3D Yaw and Roll Tilt: left cards tilt inward to the right, right cards tilt inward to the left
                    val tiltRotationY = (-pageOffset * 18f).coerceIn(-28f, 28f)
                    val tiltRotationZ = (-pageOffset * 3.2f).coerceIn(-7f, 7f)

                    // Depth dimming overlay for background cards
                    val depthDim = (absOffset * 0.28f).coerceIn(0f, 0.52f)

                    val isCenter = absOffset < 0.40f
                    var centerCardBounds by remember { mutableStateOf<Rect?>(null) }
                    val coordinator = LocalSharedPosterCoordinator.current
                    val isSharedActive = isCenter && coordinator?.activeAnimeId == anime.id && coordinator.isTransitioning

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(zIndexVal),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .onGloballyPositioned { coords ->
                                    if (isCenter) {
                                        centerCardBounds = coords.boundsInWindow()
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                    translationX = pullTranslationX
                                    translationY = depthTranslationY
                                    rotationY = tiltRotationY
                                    rotationZ = tiltRotationZ
                                    cameraDistance = 12f * density
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    width = if (isCenter) 1.5.dp else 1.dp,
                                    brush = if (isCenter) {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x90FFFFFF),
                                                Color(0x60FFFFFF),
                                                Color(0x15FFFFFF)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x25FFFFFF),
                                                Color(0x0EFFFFFF)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isCenter) {
                                        val bounds = centerCardBounds ?: Rect.Zero
                                        if (coordinator != null && bounds.width > 0) {
                                            coordinator.startForwardTransition(
                                                animeId = anime.id,
                                                posterUrl = posterUrl,
                                                bounds = bounds,
                                                onNavigate = { onAnimeClick(anime.id) }
                                            )
                                        } else {
                                            onAnimeClick(anime.id)
                                        }
                                    } else {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                page = page,
                                                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                }
                        ) {
                            // Poster Artwork
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = anime.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        this.alpha = if (isSharedActive) 0f else 1f
                                    },
                                contentScale = ContentScale.Crop
                            )

                            // Depth Dim Scrim for background cards to enhance focal pop
                            if (depthDim > 0.02f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = depthDim))
                                )
                            }

                            // Lightweight bottom poster gradient (only over bottom 90dp, not fillMaxSize)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xB3000000))
                                        )
                                    )
                            )

                            // Center Card Quality / Score Badge
                            if (isCenter) {
                                anime.score?.let { score ->
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xCC000000))
                                            .border(0.5.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = OttAccentGold,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1f", score / 10f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Lower Section: Title, Meta Info & Action Controls (Fast Fade, zero layout measurement shifts) ──
            AnimatedContent(
                targetState = activeAnime,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                        slideInVertically(
                            animationSpec = tween(260, delayMillis = 40),
                            initialOffsetY = { (it * 0.04f).toInt() } // 8dp-equivalent slide up
                        ))
                        .togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "HeroDetailsFade"
            ) { targetAnime ->
                val targetArtwork = tmdbArtworks[targetAnime.id]
                val isBookmarked = isInWatchlist(targetAnime.id)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title: TMDB Logo if present, otherwise bold multi-line text (Fixed container height, zero layout shift)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!tmdbArtworks.containsKey(targetAnime.id)) {
                            // Loading TMDB artwork: show skeleton loading
                            ShimmerBox(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else if (!targetArtwork?.logoUrl.isNullOrBlank()) {
                            // PNG logo available: directly load PNG
                            AsyncImage(
                                model = targetArtwork.logoUrl,
                                contentDescription = targetAnime.displayTitle,
                                modifier = Modifier
                                    .heightIn(max = 52.dp)
                                    .fillMaxWidth(0.70f),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center
                            )
                        } else {
                            // Fallback: normal bold title text
                            Text(
                                text = targetAnime.displayTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                Spacer(Modifier.height(6.dp))

                // Metadata Row: Rating, Year, Format, Primary Genre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    targetAnime.score?.let { score ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = OttAccentGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", score / 10f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    if (targetAnime.displayYear.isNotBlank()) {
                        Text(
                            text = targetAnime.displayYear,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OttTextSecondary,
                            softWrap = false,
                            maxLines = 1
                        )
                        Text(text = "•", fontSize = 10.sp, color = OttTextMuted)
                    }

                    Text(
                        text = targetAnime.displayFormat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )

                    if (targetAnime.genres.isNotEmpty()) {
                        Text(text = "•", fontSize = 10.sp, color = OttTextMuted)
                        Text(
                            text = targetAnime.genres.first(),
                            fontSize = 11.sp,
                            color = OttSecondary,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── OTT Actions: Watch Now + My List + Details ──────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Watch Now Button (Most-elevated clay CTA — primary action)
                    Button(
                        onClick = { onPlayClick(targetAnime.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = OttPillShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = OttPillShape,
                                ambientColor = Color(0x60FFFFFF),
                                spotColor = Color(0x80000000)
                            )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color.Black
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Watch Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }


                }
            }
        }

                Spacer(Modifier.height(14.dp))

                // Interactive Morphing Hero Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    repeat(featuredList.size) { idx ->
                        val isActive = idx == activeIndex
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 22.dp else 6.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "hero_dot_width_$idx"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isActive) OttPrimary else Color(0x4DFFFFFF),
                            animationSpec = tween(300),
                            label = "hero_dot_color_$idx"
                        )

                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(3.dp))
                                .background(dotColor)
                                .clickable {
                                    val currentVirtual = pagerState.currentPage
                                    val currentVirtualBase = currentVirtual - (currentVirtual % featuredList.size)
                                    val targetVirtual = currentVirtualBase + idx
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            page = targetVirtual,
                                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
