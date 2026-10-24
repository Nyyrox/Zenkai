package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.WatchHistoryItem
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.aniGlass
import com.nyyrox.aniverse.viewmodel.MainViewModel
import com.styropyr0.prismal.PrismalGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// ═════════════════════════════════════════════════════════════════════════════
//  Self-contained Home. No Ott* / Ani* components, no cards.
//  Public API is unchanged: HomeScreen, YouTubeStyleEpisodeCard, OfflineHomeScreenContent.
// ═════════════════════════════════════════════════════════════════════════════

// ── Palette: every colour is chosen separately for dark and light ────────────

@Immutable
private class HomePalette(
    val dark: Boolean,
    val glassOn: Boolean,
    val bg: Color,
    val text: Color,
    val textSub: Color,
    val textMuted: Color,
    val accentA: Color,
    val accentB: Color,
    val accentText: Color,
    val onAccent: Color,
    val flatFill: Color,
    val glassBorder: Color,
    val skeleton: Color,
    val skeletonHi: Color,
    val glow: Color,
    val rating: Color
) {
    val accentBrush: Brush = Brush.horizontalGradient(listOf(accentA, accentB))
}

@Composable
private fun rememberHomePalette(): HomePalette {
    val c = LocalAppColors.current
    val ts = LocalThemeSettings.current
    val dark = c.bg.luminance() < 0.5f
    val glassOn = ts.isGlassmorphismEnabled && PrismalGlass.supportsRefraction
    return remember(c.bg, glassOn) {
        if (dark) HomePalette(
            dark = true, glassOn = glassOn, bg = c.bg,
            text = Color(0xFFFFFFFF),
            textSub = Color(0xFFDCE0EE),
            textMuted = Color(0xFFA6ADC4),
            accentA = Color(0xFF9B8CFF),
            accentB = Color(0xFFFF7AB8),
            accentText = Color(0xFFB9AEFF),
            onAccent = Color(0xFF120E2A),
            flatFill = Color(0xE61C2030),
            glassBorder = Color(0x40FFFFFF),
            skeleton = Color(0xFF1E2333),
            skeletonHi = Color(0xFF2C3248),
            glow = Color(0xFF9B8CFF).copy(alpha = 0.30f),
            rating = Color(0xFFFF4B5C)
        ) else HomePalette(
            dark = false, glassOn = glassOn, bg = c.bg,
            text = Color(0xFF0A0D18),
            textSub = Color(0xFF272D42),
            textMuted = Color(0xFF50586F),
            accentA = Color(0xFF4A3AE6),
            accentB = Color(0xFFC2185B),
            accentText = Color(0xFF3B2BD1),
            onAccent = Color(0xFFFFFFFF),
            flatFill = Color(0xEBFFFFFF),
            glassBorder = Color(0x26000000),
            skeleton = Color(0xFFDDE1EE),
            skeletonHi = Color(0xFFF0F2FA),
            glow = Color(0xFF4A3AE6).copy(alpha = 0.16f),
            rating = Color(0xFFD0102A)
        )
    }
}

/** Liquid glass when the profile toggle is on, a flat tinted pill when it is off. */
@Composable
private fun Modifier.homeGlass(p: HomePalette, radius: Dp): Modifier =
    if (p.glassOn) {
        this.aniGlass(radius = radius)
    } else {
        val s = RoundedCornerShape(radius)
        this.clip(s).background(p.flatFill).border(0.8.dp, p.glassBorder, s)
    }

// Text / icons sitting on artwork always get a soft shadow instead of a background.
private val ImgShadow = Shadow(color = Color(0xAA000000), offset = Offset(0f, 2f), blurRadius = 8f)

/** Icon with no background: a soft dark copy underneath keeps it readable on any image. */
@Composable
private fun GlyphIcon(icon: ImageVector, desc: String?, size: Dp, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color(0x73000000), modifier = Modifier.size(size).offset(0.dp, 1.5.dp))
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
private fun Modifier.shimmer(p: HomePalette): Modifier {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "x"
    )
    return this.background(
        Brush.horizontalGradient(
            listOf(p.skeleton, p.skeletonHi, p.skeleton),
            startX = -300f + x * 900f,
            endX = 300f + x * 900f
        )
    )
}

// ── Small shared pieces ──────────────────────────────────────────────────────

@Composable
private fun GradientPill(label: String, icon: ImageVector?, p: HomePalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .pressable(onClick)
            .clip(CircleShape)
            .background(p.accentBrush)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.onAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = p.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun GlassPill(label: String, icon: ImageVector?, p: HomePalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .pressable(onClick)
            .homeGlass(p, 999.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = p.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(title: String, p: HomePalette, subtitle: String? = null, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = p.textMuted, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (onSeeAll != null) {
            Text(
                "See all", color = p.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onSeeAll).padding(6.dp)
            )
        }
    }
}

private fun ratingText(a: Anime): String? = a.score?.let { String.format("%.1f", it.toDouble() / 10.0) }

@Composable
private fun RatingText(a: Anime, p: HomePalette, iconSize: Dp = 13.dp, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp) {
    val r = ratingText(a) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = p.rating, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(3.dp))
        Text(r, color = p.rating, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun pickEpisode(anime: Anime): Int =
    if (anime.isMovie || anime.episodes == 1) 1
    else {
        val maxEp = (anime.episodes ?: 12).coerceAtLeast(1)
        ((anime.id % maxEp) + 1).coerceIn(1, maxEp)
    }

// ── Poster tile + rows ───────────────────────────────────────────────────────

@Composable
private fun PosterTile(
    title: String, image: String?, meta: String?, p: HomePalette,
    anime: Anime? = null, onClick: () -> Unit
) {
    Column(Modifier.width(140.dp).pressable(onClick)) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).shimmer(p)) {
            AsyncImage(
                model = image, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (anime != null && ratingText(anime) != null) {
                RatingText(anime, p)
                if (!meta.isNullOrBlank()) Text("  ·  ", color = p.textMuted, fontSize = 12.sp)
            }
            if (!meta.isNullOrBlank()) {
                Text(meta, color = p.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AnimeRow(list: List<Anime>, p: HomePalette, onAnimeClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(list, key = { i, a -> "${a.id}_$i" }) { _, anime ->
            PosterTile(anime.displayTitle, anime.cover, anime.displayFormat, p, anime) { onAnimeClick(anime.id) }
        }
    }
}

@Composable
private fun Top10Row(list: List<Anime>, p: HomePalette, onAnimeClick: (Int) -> Unit) {
    val top = remember(list) { list.take(10) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(top, key = { i, a -> "top_${a.id}_$i" }) { idx, anime ->
            Box(Modifier.width(if (idx >= 9) 196.dp else 164.dp).height(184.dp).pressable { onAnimeClick(anime.id) }) {
                Text(
                    text = "${idx + 1}",
                    color = p.accentA,
                    fontSize = 104.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-6).sp,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = 4.dp, y = 10.dp)
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .width(118.dp).height(176.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmer(p)
                ) {
                    AsyncImage(
                        model = anime.cover, contentDescription = anime.displayTitle,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaRow(list: List<com.nyyrox.aniverse.data.manga.MangaCard>, p: HomePalette, onMangaClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(list, key = { i, m -> "manga_${m.id}_$i" }) { _, manga ->
            PosterTile(
                title = manga.title,
                image = manga.poster,
                meta = manga.chapter?.takeIf { it.isNotBlank() } ?: (manga.type ?: "Manga"),
                p = p,
                onClick = { onMangaClick(manga.id) }
            )
        }
    }
}

@Composable
private fun NovelRow(list: List<com.nyyrox.aniverse.data.novel.NovelItem>, p: HomePalette, onNovelClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(list, key = { i, n -> "novel_${n.slug}_$i" }) { _, novel ->
            PosterTile(
                title = novel.title,
                image = novel.cover,
                meta = novel.rating?.takeIf { it.isNotBlank() } ?: (novel.stats?.takeIf { it.isNotBlank() } ?: novel.source),
                p = p,
                onClick = { onNovelClick(novel.slug) }
            )
        }
    }
}

// ── Hero: swipeable 3D card stack ────────────────────────────────────────────

private val HeroCardW = 232.dp
private val HeroCardH = 340.dp

@Composable
private fun HomeHero(
    shows: List<Anime>,
    pager: PagerState,
    vm: MainViewModel,
    p: HomePalette,
    listScrolling: Boolean,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit
) {
    val reduced = LocalThemeSettings.current.isReducedMotionEnabled
    val dragging = pager.isScrollInProgress
    LaunchedEffect(shows.size, listScrolling, dragging, reduced) {
        if (shows.size < 2 || listScrolling || dragging || reduced) return@LaunchedEffect
        while (true) {
            delay(6000)
            pager.animateScrollToPage((pager.currentPage + 1) % shows.size)
        }
    }

    val scope = rememberCoroutineScope()
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val sidePad = ((screenW - HeroCardW) / 2).coerceAtLeast(0.dp)
    val current = pager.currentPage.coerceIn(0, shows.lastIndex)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // clears the floating top bar (separate file)
        Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(68.dp))
        Text(
            "Watch today", color = p.textSub, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start).padding(start = 22.dp)
        )
        Spacer(Modifier.height(10.dp))

        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = sidePad),
            pageSpacing = 0.dp,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(HeroCardH + 28.dp)
        ) { page ->
            val anime = shows[page]
            LaunchedEffect(anime.id) { vm.loadTmdbArtwork(anime) }
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .zIndex(-abs(page - pager.currentPage).toFloat())
                    .width(HeroCardW)
                    .height(HeroCardH)
                    .graphicsLayer {
                        val off = (pager.currentPage - page) + pager.currentPageOffsetFraction
                        val a = abs(off).coerceAtMost(2f)
                        val s = 1f - 0.14f * a.coerceAtMost(1f) - 0.05f * (a - 1f).coerceAtLeast(0f)
                        scaleX = s
                        scaleY = s
                        rotationY = (off * 30f).coerceIn(-42f, 42f)
                        translationX = off * size.width * 0.22f
                        alpha = (1f - 0.32f * a).coerceIn(0.3f, 1f)
                        cameraDistance = 14f * density
                    }
                    .shadow(20.dp, shape, ambientColor = p.accentA, spotColor = p.accentA)
                    .clip(shape)
                    .shimmer(p)
                    .border(0.8.dp, p.glassBorder, shape)
                    .clickable {
                        if (pager.currentPage == page) onAnimeClick(anime.id)
                        else scope.launch { pager.animateScrollToPage(page) }
                    }
            ) {
                AsyncImage(
                    model = anime.cover, contentDescription = anime.displayTitle,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Crossfade(targetState = current, label = "heroInfo") { idx ->
            val anime = shows[idx.coerceIn(0, shows.lastIndex)]
            val artwork = vm.tmdbArtworks[anime.id]
            val title = artwork?.title?.takeIf { it.isNotBlank() } ?: anime.displayTitle
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "#${idx + 1} TRENDING", color = p.accentText, fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(6.dp))
                if (!artwork?.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artwork?.logoUrl, contentDescription = title,
                        modifier = Modifier.heightIn(max = 52.dp).fillMaxWidth(0.6f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        title, color = p.text, fontSize = 24.sp, lineHeight = 28.sp,
                        fontWeight = FontWeight.Black, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        listOfNotNull(anime.genres.take(2).joinToString(" • ").ifBlank { null }, anime.displayFormat)
                            .joinToString(" · "),
                        color = p.textSub, fontSize = 13.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (ratingText(anime) != null) {
                        Text("  ·  ", color = p.textMuted, fontSize = 13.sp)
                        RatingText(anime, p, 14.dp, 14.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GradientPill("Play", Icons.Default.PlayArrow, p, { onPlayClick(anime.id, 1) })
                    GlassPill("Details", Icons.Default.Info, p, { onAnimeClick(anime.id) })
                }
            }
        }

        if (shows.size > 1) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(shows.size) { i ->
                    val active = i == current
                    Box(
                        Modifier
                            .height(4.dp)
                            .width(if (active) 20.dp else 5.dp)
                            .clip(CircleShape)
                            .then(
                                if (active) Modifier.background(p.accentBrush)
                                else Modifier.background(p.textMuted.copy(alpha = 0.5f))
                            )
                    )
                }
            }
        }
    }
}

/** Soft ambient glow behind the whole page (no images, no hard edges). It is what the glass refracts. */
@Composable
private fun HomeBackdrop(state: LazyListState, p: HomePalette) {
    Box(Modifier.fillMaxSize().background(p.bg)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "home_hero_banner" }
                    alpha = if (info == null) 0f else (1f + info.offset / (info.size * 1.2f)).coerceIn(0f, 1f)
                }
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(p.glow, Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.30f),
                            radius = size.width * 1.15f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(p.accentB.copy(alpha = if (p.dark) 0.16f else 0.09f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.55f),
                            radius = size.width * 0.9f
                        )
                    )
                }
        )
    }
}

// ── Episode feed item ────────────────────────────────────────────────────────

/** 16:9 thumbnail (tap = play) with label / title / meta underneath (tap = details). No card, no icon backgrounds. */
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

    val p = rememberHomePalette()
    val durationText = remember(anime.duration) { "${anime.duration ?: 24}:00" }
    val title = tmdbTitle?.takeIf { it.isNotBlank() } ?: anime.displayTitle
    val headerTitle = if (anime.isMovie) title else "$title • Episode $episodeNumber"
    val thumb = remember(episodeStill, anime.banner, anime.cover) {
        episodeStill?.takeIf { it.isNotBlank() }
            ?: anime.banner?.takeIf { it.isNotBlank() }
            ?: anime.cover
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .shimmer(p)
                .clickable { onPlayClick(anime.id, episodeNumber) }
        ) {
            SubcomposeAsyncImage(
                model = thumb, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                error = {
                    AsyncImage(
                        model = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                }
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xB3000000))
                )
            )
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = logoUrl, contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = 12.dp)
                        .heightIn(max = 34.dp)
                        .fillMaxWidth(0.5f),
                    contentScale = ContentScale.Fit, alignment = Alignment.BottomStart
                )
            }
            GlyphIcon(Icons.Default.PlayArrow, "Play", 38.dp, Modifier.align(Alignment.Center))
            Text(
                durationText, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = ImgShadow),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            )
        }

        Column(Modifier.fillMaxWidth().clickable { onAnimeClick(anime.id) }.padding(top = 10.dp)) {
            if (!badgeText.isNullOrBlank()) {
                Text(badgeText, color = p.accentText, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
            }
            Text(
                headerTitle, color = p.text, fontSize = 16.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(anime.genres.take(2).joinToString(" • ").ifBlank { null }, anime.displayFormat ?: "TV")
                        .joinToString(" • "),
                    color = p.textSub, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (ratingText(anime) != null) {
                    Text("  •  ", color = p.textMuted, fontSize = 12.5.sp)
                    RatingText(anime, p, 13.dp, 12.5.sp)
                }
            }
        }
    }
}

// ── Continue row ─────────────────────────────────────────────────────────────

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
private fun ContinueRow(
    animeHistory: List<WatchHistoryItem>,
    mangaHistory: List<MangaHistoryItem>,
    novelHistory: List<NovelHistoryItem>,
    p: HomePalette,
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
        SectionTitle("Continue", p)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(entries, key = { it.key }) { e -> ContinueTile(e, p) }
        }
    }
}

@Composable
private fun ContinueTile(e: ContinueEntry, p: HomePalette) {
    LaunchedEffect(e.key) { e.loadStill?.invoke() }
    Box(
        Modifier
            .width(272.dp)
            .aspectRatio(16f / 9f)
            .pressable(e.onOpen)
            .clip(RoundedCornerShape(16.dp))
            .shimmer(p)
    ) {
        AsyncImage(
            model = e.image(), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            alignment = if (e.kind == ContinueKind.ANIME) Alignment.Center else Alignment.TopCenter
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color(0x66000000), 0.35f to Color.Transparent, 1f to Color(0xE6000000))
            )
        )
        Text(
            e.kind.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
            style = TextStyle(shadow = ImgShadow),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp)
        )
        Box(
            Modifier.align(Alignment.TopEnd).size(40.dp).clickable(onClick = e.onRemove),
            contentAlignment = Alignment.Center
        ) { GlyphIcon(Icons.Default.Close, "Remove", 16.dp) }

        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    e.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = ImgShadow)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    e.subtitle, color = Color(0xE6FFFFFF), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(shadow = ImgShadow)
                )
            }
            GlyphIcon(Icons.Default.PlayArrow, null, 30.dp)
        }
        if (e.progress > 0f) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x59FFFFFF)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(e.progress).height(3.dp).background(p.accentBrush))
        }
    }
}

// ── Random episode breaks between shelves ────────────────────────────────────

private fun LazyListScope.episodeBreak(
    slot: String,
    pool: List<Anime>,
    order: Int,
    vm: MainViewModel,
    p: HomePalette,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit
) {
    val anime = pool.getOrNull(order) ?: return
    item(key = "ep_break_${slot}_${anime.id}") {
        val epNum = remember(anime.id, anime.episodes, anime.isMovie) { pickEpisode(anime) }
        val titles = listOf("Jump Into an Episode", "Pick Up Here", "Top Episode Right Now", "Don't Miss This")
        val artwork = vm.tmdbArtworks[anime.id]
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            SectionTitle(titles[order % titles.size], p)
            Spacer(Modifier.height(8.dp))
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

// ── Loading skeleton ─────────────────────────────────────────────────────────

@Composable
private fun HomeSkeleton(p: HomePalette) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(96.dp))
                Box(Modifier.width(HeroCardW).height(HeroCardH).clip(RoundedCornerShape(24.dp)).shimmer(p))
            }
        }
        repeat(3) {
            item {
                Column {
                    Box(Modifier.padding(start = 18.dp).width(140.dp).height(18.dp).clip(RoundedCornerShape(6.dp)).shimmer(p))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.padding(start = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(3) { Box(Modifier.width(124.dp).height(184.dp).clip(RoundedCornerShape(14.dp)).shimmer(p)) }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  HomeScreen
// ═════════════════════════════════════════════════════════════════════════════

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

    val p = rememberHomePalette()

    Box(Modifier.fillMaxSize().background(p.bg)) {
        if (vm.homeLoading && vm.trending.isEmpty()) {
            HomeSkeleton(p)
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
        val heroShows = remember(vm.trending, vm.popular) {
            (vm.trending + vm.popular).distinctBy { it.id }.take(10)
        }
        val pager = rememberPagerState(pageCount = { heroShows.size })

        if (p.glassOn) {
            GlassBackdropHost(
                background = { HomeBackdrop(lazyListState, p) }
            ) {
                HomeFeed(
                    vm, p, heroShows, pager, lazyListState,
                    onAnimeClick, onPlayClick, onMangaClick, onMangaReadClick, onNovelClick, onNovelReadClick
                )
            }
        } else {
            HomeBackdrop(lazyListState, p)
            HomeFeed(
                vm, p, heroShows, pager, lazyListState,
                onAnimeClick, onPlayClick, onMangaClick, onMangaReadClick, onNovelClick, onNovelReadClick
            )
        }
    }
}

@Composable
private fun HomeFeed(
    vm: MainViewModel,
    p: HomePalette,
    heroShows: List<Anime>,
    pager: PagerState,
    lazyListState: LazyListState,
    onAnimeClick: (Int) -> Unit,
    onPlayClick: (animeId: Int, episode: Int) -> Unit,
    onMangaClick: (String) -> Unit,
    onMangaReadClick: (mangaId: String, chapterId: String) -> Unit,
    onNovelClick: (String) -> Unit,
    onNovelReadClick: (novelSlug: String, chapterSlug: String) -> Unit
) {
    val exploreList = vm.homeExplore.ifEmpty { vm.popular }
    val trendingTopEpisodes = remember(vm.trending, vm.popular) { (vm.trending.ifEmpty { vm.popular }).take(2) }

    val genreShelves = remember(
        vm.actionAnime, vm.fantasyAnime, vm.topRated, vm.scifiAnime,
        vm.romanceAnime, vm.newReleases, vm.upcomingAnime, vm.popular
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
            val info = lazyListState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !vm.homeExploreLoading && vm.homeExploreHasMore) {
            vm.loadMoreHomeExplore()
        }
    }

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

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (vm.homeError != null && vm.trending.isNotEmpty()) {
            item(key = "home_error_note") {
                Box(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .homeGlass(p, 14.dp)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        vm.homeError ?: "", fontSize = 12.sp, color = p.textSub,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (heroShows.isNotEmpty()) {
            item(key = "home_hero_banner") {
                HomeHero(
                    shows = heroShows, pager = pager, vm = vm, p = p,
                    listScrolling = lazyListState.isScrollInProgress,
                    onAnimeClick = onAnimeClick, onPlayClick = onPlayClick
                )
            }
        }

        if (vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()) {
            item(key = "home_continue_watching") {
                ContinueRow(
                    animeHistory = vm.history, mangaHistory = vm.mangaHistory, novelHistory = vm.novelHistoryList,
                    p = p,
                    stillFor = { id, ep -> vm.tmdbEpisodeStills["${id}_$ep"] },
                    onLoadEpisodeStill = { id, ep -> vm.loadHistoryEpisodeStill(id, ep) },
                    onPlayAnime = onPlayClick, onReadManga = onMangaReadClick, onReadNovel = onNovelReadClick,
                    onRemoveAnime = { vm.removeHistory(it) },
                    onRemoveManga = { vm.removeMangaHistory(it) },
                    onRemoveNovel = { vm.removeNovelHistory(it) }
                )
            }
        }

        if (trendingTopEpisodes.isNotEmpty()) {
            item(key = "home_trending_episodes_header") { SectionTitle("Trending Top Watched", p) }
            itemsIndexed(trendingTopEpisodes, key = { idx, a -> "trending_ep_${a.id}_$idx" }) { idx, anime ->
                val artwork = vm.tmdbArtworks[anime.id]
                YouTubeStyleEpisodeCard(
                    anime = anime, episodeNumber = 1,
                    badgeText = if (idx == 0) "MUST WATCH" else "TRENDING NOW",
                    episodeStill = vm.tmdbEpisodeStills["${anime.id}_1"],
                    logoUrl = artwork?.logoUrl, tmdbTitle = artwork?.title,
                    onLoadStill = { vm.loadTmdbEpisodeStill(anime, 1) },
                    onLoadArtwork = { vm.loadTmdbArtwork(anime) },
                    onAnimeClick = onAnimeClick, onPlayClick = onPlayClick
                )
            }
        }

        if (vm.trending.isNotEmpty()) {
            item(key = "home_top_10") {
                Column {
                    SectionTitle("Top 10 Today", p)
                    Top10Row(vm.trending, p, onAnimeClick)
                }
            }
        }

        fun LazyListScope.shelf(key: String, title: String, list: List<Anime>) {
            if (list.isEmpty()) return
            item(key = key) {
                Column {
                    SectionTitle(title, p)
                    AnimeRow(list, p, onAnimeClick)
                }
            }
        }

        fun LazyListScope.spotlight(key: String, title: String, badge: String, anime: Anime?) {
            if (anime == null) return
            item(key = key) {
                val artwork = vm.tmdbArtworks[anime.id]
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    SectionTitle(title, p)
                    Spacer(Modifier.height(8.dp))
                    YouTubeStyleEpisodeCard(
                        anime = anime, episodeNumber = 1, badgeText = badge,
                        episodeStill = vm.tmdbEpisodeStills["${anime.id}_1"],
                        logoUrl = artwork?.logoUrl, tmdbTitle = artwork?.title,
                        onLoadStill = { vm.loadTmdbEpisodeStill(anime, 1) },
                        onLoadArtwork = { vm.loadTmdbArtwork(anime) },
                        onAnimeClick = onAnimeClick, onPlayClick = onPlayClick
                    )
                }
            }
        }

        shelf("home_popular_series", "Popular Series", vm.popular)

        val spotlight1 = vm.popular.getOrNull(1) ?: vm.trending.getOrNull(2)
        spotlight("spotlight_popular_${spotlight1?.id}", "Must Watch Spotlight", "MUST WATCH", spotlight1)

        shelf("home_top_rated", "Critically Acclaimed", vm.topRated)
        if ("top_rated" in breakSlots) episodeBreak("top_rated", breakPool, 0, vm, p, onAnimeClick, onPlayClick)

        shelf("home_action_anime", "Action & High Adrenaline", vm.actionAnime)

        val actionSpotlight = vm.actionAnime.firstOrNull()
        if (actionSpotlight != null && actionSpotlight.id != spotlight1?.id) {
            spotlight("spotlight_action_${actionSpotlight.id}", "Action Spotlight", "MUST WATCH • ACTION", actionSpotlight)
        }

        shelf("home_fantasy_anime", "Fantasy & Magic", vm.fantasyAnime)
        if ("fantasy" in breakSlots) episodeBreak("fantasy", breakPool, 1, vm, p, onAnimeClick, onPlayClick)

        shelf("home_new_releases", "New Releases", vm.newReleases)
        if ("new" in breakSlots) episodeBreak("new", breakPool, 2, vm, p, onAnimeClick, onPlayClick)

        shelf("home_upcoming_anime", "Upcoming Shows", vm.upcomingAnime)
        if ("upcoming" in breakSlots) episodeBreak("upcoming", breakPool, 3, vm, p, onAnimeClick, onPlayClick)

        shelf("home_scifi_anime", "Sci-Fi & Cyberpunk", vm.scifiAnime)
        if ("scifi" in breakSlots) episodeBreak("scifi", breakPool, 4, vm, p, onAnimeClick, onPlayClick)

        shelf("home_romance_anime", "Romance & Slice of Life", vm.romanceAnime)
        if ("romance" in breakSlots) episodeBreak("romance", breakPool, 5, vm, p, onAnimeClick, onPlayClick)

        val mangaList = vm.mangaHome.trending.ifEmpty { vm.mangaHome.latest }
        if (mangaList.isNotEmpty()) {
            item(key = "home_popular_manga") {
                Column {
                    SectionTitle("Popular Manga", p)
                    MangaRow(mangaList, p, onMangaClick)
                }
            }
        }

        val novelList = vm.novelPopular.ifEmpty { vm.novelLightNovels }
        if (novelList.isNotEmpty()) {
            item(key = "home_popular_novels") {
                Column {
                    SectionTitle("Top Light Novels", p)
                    NovelRow(novelList, p, onNovelClick)
                }
            }
        }

        val featureSpotlight = vm.scifiAnime.firstOrNull() ?: vm.fantasyAnime.firstOrNull()
        if (featureSpotlight != null && featureSpotlight.id != spotlight1?.id && featureSpotlight.id != actionSpotlight?.id) {
            spotlight("spotlight_feature_${featureSpotlight.id}", "Featured Spotlight", "MUST WATCH", featureSpotlight)
        }

        if (exploreList.isNotEmpty()) {
            item(key = "home_explore_header") {
                Column(Modifier.padding(top = 10.dp)) { SectionTitle("Top Episodes & Discover", p) }
            }

            itemsIndexed(exploreList, key = { idx, anime -> "explore_yt_feed_${anime.id}_$idx" }) { index, anime ->
                val epNum = remember(anime.id, anime.episodes, anime.isMovie) { pickEpisode(anime) }
                val badge = remember(index) {
                    when (index % 5) {
                        0 -> "MUST WATCH"
                        1 -> "TOP EPISODE"
                        2 -> "TOP WATCHED"
                        3 -> "TRENDING NOW"
                        else -> "POPULAR PICK"
                    }
                }
                val artwork = vm.tmdbArtworks[anime.id]
                Column {
                    YouTubeStyleEpisodeCard(
                        anime = anime, episodeNumber = epNum, badgeText = badge,
                        episodeStill = vm.tmdbEpisodeStills["${anime.id}_$epNum"],
                        logoUrl = artwork?.logoUrl, tmdbTitle = artwork?.title,
                        onLoadStill = { vm.loadTmdbEpisodeStill(anime, epNum) },
                        onLoadArtwork = { vm.loadTmdbArtwork(anime) },
                        onAnimeClick = onAnimeClick, onPlayClick = onPlayClick
                    )
                    if (genreShelves.isNotEmpty() && index % 2 == 1) {
                        val (genreTitle, genreList) = genreShelves[(index / 2) % genreShelves.size]
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            SectionTitle(genreTitle, p)
                            AnimeRow(genreList, p, onAnimeClick)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            if (vm.homeExploreLoading) {
                item(key = "home_explore_spinner") {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.accentA, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Offline home
// ═════════════════════════════════════════════════════════════════════════════

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    if (mb < 1024.0) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
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
    val p = rememberHomePalette()
    val downloadedAnime by vm.downloadManager.downloadedAnime.collectAsState()
    val downloadedManga by vm.downloadManager.downloadedManga.collectAsState()
    val downloadedNovels by vm.downloadManager.downloadedNovels.collectAsState()

    val animeBytes = remember(downloadedAnime) { downloadedAnime.sumOf { it.totalSizeBytes } }
    val mangaBytes = remember(downloadedManga) { downloadedManga.sumOf { it.totalSizeBytes } }
    val novelBytes = remember(downloadedNovels) { downloadedNovels.sumOf { it.totalSizeBytes } }
    val totalDownloadsCount = downloadedAnime.size + downloadedManga.size + downloadedNovels.size
    val totalDownloadedBytes = animeBytes + mangaBytes + novelBytes
    val hasAnyHistory = vm.history.isNotEmpty() || vm.mangaHistory.isNotEmpty() || vm.novelHistoryList.isNotEmpty()

    val content: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "offline_top_spacer") {
                Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(56.dp))
            }

            item(key = "offline_status") {
                Column(Modifier.fillMaxWidth().padding(horizontal = OfflineMargin)) {
                    Text("OFFLINE", color = p.accentText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("You're offline", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (totalDownloadsCount > 0) {
                            "No connection, but your library is ready. Everything below plays and reads without internet."
                        } else {
                            "No internet connection detected. Connect to Wi-Fi or mobile data to explore trending shows, or browse your downloaded library."
                        },
                        color = p.textSub, fontSize = 14.sp, lineHeight = 20.sp
                    )

                    if (totalDownloadsCount > 0) {
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth()) {
                            OfflineStat(downloadedAnime.size.toString(), "Anime", p)
                            OfflineStat(downloadedManga.size.toString(), "Manga", p)
                            OfflineStat(downloadedNovels.size.toString(), "Novels", p)
                        }
                        if (totalDownloadedBytes > 0L) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Storage used", color = p.textMuted, fontSize = 12.5.sp)
                                Text(formatBytes(totalDownloadedBytes), color = p.text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(p.skeleton),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (animeBytes > 0L) Box(Modifier.weight(animeBytes.toFloat()).fillMaxHeight().background(p.accentA))
                                if (mangaBytes > 0L) Box(Modifier.weight(mangaBytes.toFloat()).fillMaxHeight().background(p.accentB))
                                if (novelBytes > 0L) Box(Modifier.weight(novelBytes.toFloat()).fillMaxHeight().background(p.accentA.copy(alpha = 0.4f)))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        GradientPill("Open Downloads", Icons.Default.Storage, p, onDownloadsClick)
                        GlassPill("Retry", Icons.Default.Refresh, p, { vm.loadHome() })
                    }
                }
            }

            if (hasAnyHistory) {
                item(key = "offline_continue_watching") {
                    ContinueRow(
                        animeHistory = vm.history, mangaHistory = vm.mangaHistory, novelHistory = vm.novelHistoryList,
                        p = p,
                        stillFor = { id, ep -> vm.tmdbEpisodeStills["${id}_$ep"] },
                        onLoadEpisodeStill = { id, ep -> vm.loadHistoryEpisodeStill(id, ep) },
                        onPlayAnime = onPlayClick, onReadManga = onMangaReadClick, onReadNovel = onNovelReadClick,
                        onRemoveAnime = { vm.removeHistory(it) },
                        onRemoveManga = { vm.removeMangaHistory(it) },
                        onRemoveNovel = { vm.removeNovelHistory(it) }
                    )
                }
            }

            if (downloadedAnime.isNotEmpty()) {
                item(key = "offline_downloaded_anime") {
                    Column {
                        SectionTitle("Downloaded Anime", p, "${downloadedAnime.size} title(s) • ${formatBytes(animeBytes)}", onDownloadsClick)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(downloadedAnime, key = { "offline_anime_${it.animeId}" }) { anime ->
                                OfflineTile(
                                    title = anime.title, coverUrl = anime.cover,
                                    countBadge = "${anime.episodesCount} EPS", sizeBytes = anime.totalSizeBytes, p = p,
                                    onClick = { onAnimeClick(anime.animeId) },
                                    onPlay = { onPlayClick(anime.animeId, 1) }
                                )
                            }
                        }
                    }
                }
            }

            if (downloadedManga.isNotEmpty()) {
                item(key = "offline_downloaded_manga") {
                    Column {
                        SectionTitle("Downloaded Manga", p, "${downloadedManga.size} series • ${formatBytes(mangaBytes)}", onDownloadsClick)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(downloadedManga, key = { "offline_manga_${it.mangaId}" }) { manga ->
                                OfflineTile(
                                    title = manga.title, coverUrl = manga.poster,
                                    countBadge = "${manga.chaptersCount} CH", sizeBytes = manga.totalSizeBytes, p = p,
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
                        SectionTitle("Downloaded Novels", p, "${downloadedNovels.size} light novel(s) • ${formatBytes(novelBytes)}", onDownloadsClick)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = OfflineMargin, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(downloadedNovels, key = { "offline_novel_${it.novelSlug}" }) { novel ->
                                OfflineTile(
                                    title = novel.title, coverUrl = novel.cover,
                                    countBadge = "${novel.chaptersCount} CH", sizeBytes = novel.totalSizeBytes, p = p,
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
                        Modifier.fillMaxWidth().padding(horizontal = OfflineMargin * 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Storage, null, tint = p.textMuted, modifier = Modifier.size(32.dp))
                        Text("No downloads yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = p.text, textAlign = TextAlign.Center)
                        Text(
                            "Download anime episodes, manga chapters, and novel chapters while connected so you can enjoy them anywhere without internet.",
                            fontSize = 13.sp, color = p.textSub, textAlign = TextAlign.Center, lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        GradientPill("Open Downloads Hub", null, p, onDownloadsClick)
                    }
                }
            }
        }
    }

    if (p.glassOn) {
        GlassBackdropHost(
            background = {
                Box(Modifier.fillMaxSize().background(p.bg)) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to p.glow, 0.5f to Color.Transparent)))
                }
            }
        ) { content() }
    } else {
        Box(Modifier.fillMaxSize().background(p.bg)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to p.glow, 0.5f to Color.Transparent)))
            content()
        }
    }
}

@Composable
private fun RowScope.OfflineStat(value: String, label: String, p: HomePalette) {
    Column(Modifier.weight(1f)) {
        Text(value, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(label, color = p.textMuted, fontSize = 12.5.sp)
    }
}

@Composable
private fun OfflineTile(
    title: String,
    coverUrl: String?,
    countBadge: String,
    sizeBytes: Long,
    p: HomePalette,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null
) {
    Column(Modifier.width(130.dp).pressable(onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).shimmer(p)) {
            SubcomposeAsyncImage(
                model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, null, tint = p.textMuted, modifier = Modifier.size(32.dp))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (onPlay != null) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(48.dp).clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) { GlyphIcon(Icons.Default.PlayArrow, "Play", 30.dp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(countBadge, if (sizeBytes > 0L) formatBytes(sizeBytes) else null).joinToString(" · "),
            color = p.textMuted, fontSize = 11.5.sp, maxLines = 1
        )
    }
}
