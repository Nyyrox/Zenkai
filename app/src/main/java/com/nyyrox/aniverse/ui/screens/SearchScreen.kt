package com.nyyrox.aniverse.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.manga.MangaCard
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.data.novel.NovelItem
import com.nyyrox.aniverse.data.novel.NovelSource
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SearchTab(val label: String) {
    ANIME("Anime"),
    MANGA("Manga"),
    NOVEL("Novels")
}

@Composable
fun SearchScreen(
    vm: MainViewModel,
    initialTab: String = "anime",
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit,
    onMangaClick: ((String) -> Unit)? = null,
    onNovelClick: ((String) -> Unit)? = null
) {
    val p = rememberHomePalette()
    var selectedTab by remember(initialTab) {
        mutableStateOf(
            when (initialTab.lowercase()) {
                "manga" -> SearchTab.MANGA
                "novel", "novels" -> SearchTab.NOVEL
                else -> SearchTab.ANIME
            }
        )
    }

    var query by remember { mutableStateOf("") }
    var animeResults by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var mangaResults by remember { mutableStateOf<List<MangaCard>>(emptyList()) }
    var novelResults by remember { mutableStateOf<List<NovelItem>>(emptyList()) }
    var selectedNovelSource by remember { mutableStateOf<NovelSource?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedTab, selectedNovelSource) {
        val q = query.trim()
        if (q.isBlank()) {
            animeResults = emptyList()
            mangaResults = emptyList()
            novelResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(350)
        try {
            when (selectedTab) {
                SearchTab.ANIME -> {
                    animeResults = vm.searchAniListAnime(q)
                }
                SearchTab.MANGA -> {
                    mangaResults = withContext(Dispatchers.IO) {
                        MangaRepository.search(q)
                    }
                }
                SearchTab.NOVEL -> {
                    novelResults = withContext(Dispatchers.IO) {
                        vm.novelRepo.search(q, 1, selectedNovelSource)
                    }
                }
            }
        } catch (_: Exception) {
            when (selectedTab) {
                SearchTab.ANIME -> animeResults = emptyList()
                SearchTab.MANGA -> mangaResults = emptyList()
                SearchTab.NOVEL -> novelResults = emptyList()
            }
        } finally {
            isSearching = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(p.pageBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar: Back Button + Glass Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(p.chipBg)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = p.text,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(p.chipBg)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = p.textMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = p.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(p.accentA),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    val hint = when (selectedTab) {
                                        SearchTab.ANIME -> "Search anime by title, genre..."
                                        SearchTab.MANGA -> "Search manga, manhwa & comics..."
                                        SearchTab.NOVEL -> "Search web & light novels..."
                                    }
                                    Text(
                                        text = hint,
                                        color = p.textMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = p.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Liquid Glass Segmented Tab Switcher (Anime / Manga / Novels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(p.chipBg)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .then(
                                if (isSelected) Modifier.background(p.accentBrush)
                                else Modifier
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val icon = when (tab) {
                                SearchTab.ANIME -> Icons.Default.Movie
                                SearchTab.MANGA -> Icons.Default.AutoStories
                                SearchTab.NOVEL -> Icons.AutoMirrored.Filled.MenuBook
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) p.onAccent else p.textSub,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = tab.label,
                                color = if (isSelected) p.onAccent else p.textSub,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Novel Source Selector (when in Novel search tab)
            if (selectedTab == SearchTab.NOVEL) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isAll = selectedNovelSource == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .then(if (isAll) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                .clickable { selectedNovelSource = null }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All Sources",
                                color = if (isAll) p.onAccent else p.text,
                                fontSize = 12.sp,
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    val novelSources = listOf(
                        NovelSource.LNORI,
                        NovelSource.NOVEL_FULL,
                        NovelSource.NOVEL_FIRE,
                        NovelSource.FREE_WEB_NOVEL,
                        NovelSource.ROYAL_ROAD,
                        NovelSource.WUXIA_WORLD
                    )
                    items(novelSources) { src ->
                        val isSel = selectedNovelSource == src
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .then(if (isSel) Modifier.background(p.accentBrush) else Modifier.background(p.chipBg))
                                .clickable { selectedNovelSource = if (isSel) null else src }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = src.displayName,
                                color = if (isSel) p.onAccent else p.text,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Search Content View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                when {
                    query.isBlank() -> {
                        // Empty Search Landing State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(p.chipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (selectedTab) {
                                    SearchTab.ANIME -> Icons.Default.Movie
                                    SearchTab.MANGA -> Icons.Default.AutoStories
                                    SearchTab.NOVEL -> Icons.AutoMirrored.Filled.MenuBook
                                }
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = p.accentText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            val title = when (selectedTab) {
                                SearchTab.ANIME -> "Explore Anime"
                                SearchTab.MANGA -> "Discover Manga & Manhwa"
                                SearchTab.NOVEL -> "Read Web & Light Novels"
                            }
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(6.dp))

                            val subtitle = when (selectedTab) {
                                SearchTab.ANIME -> "Search thousands of series, movies and OVAs powered by AniList."
                                SearchTab.MANGA -> "Search across top manga, manhwa & comics from AniList and sources."
                                SearchTab.NOVEL -> "Search thousands of web novels, light novels and translations."
                            }
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                color = p.textSub,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    isSearching -> {
                        // Loading State
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = p.accentA,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Searching ${selectedTab.label.lowercase()}...",
                                    fontSize = 13.sp,
                                    color = p.textSub
                                )
                            }
                        }
                    }

                    selectedTab == SearchTab.ANIME && animeResults.isEmpty() ||
                    selectedTab == SearchTab.MANGA && mangaResults.isEmpty() ||
                    selectedTab == SearchTab.NOVEL && novelResults.isEmpty() -> {
                        // No Results State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No results found for \"$query\"",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Check for typos or try searching with a different title or keyword.",
                                fontSize = 13.sp,
                                color = p.textSub,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        // Results List for Active Tab
                        Crossfade(targetState = selectedTab, label = "searchResultsCrossfade") { tab ->
                            when (tab) {
                                SearchTab.ANIME -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(animeResults, key = { "anime_${it.id}" }) { anime ->
                                            SearchResultAnimeItem(
                                                anime = anime,
                                                p = p,
                                                onClick = { onAnimeClick(anime.id) }
                                            )
                                        }
                                    }
                                }
                                SearchTab.MANGA -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(mangaResults, key = { "manga_${it.id}" }) { manga ->
                                            SearchResultMangaItem(
                                                manga = manga,
                                                p = p,
                                                onClick = { onMangaClick?.invoke(manga.id) }
                                            )
                                        }
                                    }
                                }
                                SearchTab.NOVEL -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(novelResults, key = { "novel_${it.slug}" }) { novel ->
                                            SearchResultNovelItem(
                                                novel = novel,
                                                p = p,
                                                onClick = { onNovelClick?.invoke(novel.slug) }
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
}

@Composable
private fun SearchResultAnimeItem(
    anime: Anime,
    p: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = anime.cover,
            contentDescription = anime.title,
            modifier = Modifier
                .size(width = 70.dp, height = 96.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = anime.displayTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            val genresText = anime.genres.take(3).joinToString(", ")
            if (genresText.isNotBlank()) {
                Text(
                    text = genresText,
                    fontSize = 12.5.sp,
                    color = p.textSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val score = anime.score
                if (score != null && score > 0) {
                    val formattedScore = String.format(Locale.US, "%.1f", score / 10f)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = p.rating,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = formattedScore,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text
                            )
                        }
                    }
                }

                val format = if (anime.isMovie) "Movie" else anime.displayFormat
                if (format.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = format,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.textSub
                        )
                    }
                }

                val year = anime.seasonYear
                if (year != null && year > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "$year",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = p.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultMangaItem(
    manga: MangaCard,
    p: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = manga.poster,
            contentDescription = manga.title,
            modifier = Modifier
                .size(width = 70.dp, height = 96.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = manga.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            val genresText = manga.genres.take(3).joinToString(", ")
            if (genresText.isNotBlank()) {
                Text(
                    text = genresText,
                    fontSize = 12.5.sp,
                    color = p.textSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!manga.rating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = p.rating,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = manga.rating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text
                            )
                        }
                    }
                }

                val fmt = manga.format ?: manga.type ?: "MANGA"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(p.chipBg)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = fmt.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textSub
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultNovelItem(
    novel: NovelItem,
    p: HomePalette,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = rememberNovelImageRequest(novel.cover),
            contentDescription = novel.title,
            modifier = Modifier
                .size(width = 70.dp, height = 96.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = novel.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            val statsText = novel.stats ?: novel.rating ?: novel.source
            Text(
                text = statsText,
                fontSize = 12.5.sp,
                color = p.textSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!novel.rating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(p.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = p.rating,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = novel.rating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.text
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(p.chipBg)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = novel.source.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.textSub
                    )
                }
            }
        }
    }
}
