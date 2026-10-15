package com.nyyrox.aniverse.ui.components

import androidx.compose.animation.core.Animatable
import com.nyyrox.aniverse.ui.theme.OttPanel
import com.nyyrox.aniverse.ui.theme.OttInk
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.StorageManager

private val Amoled = OttPanel
private val Muted = Color(0xFF8E94A3)
private val Soft = Color(0xFFCBD5E1)

data class QualityOption(
    val id: String,
    val label: String,
    val resolution: String,
    val approxSize: String
)

val QUALITY_OPTIONS = listOf(
    QualityOption("1080p", "1080p", "1920x1080", "~350 MB"),
    QualityOption("720p", "720p", "1280x720", "~200 MB"),
    QualityOption("480p", "480p", "854x480", "~110 MB"),
    QualityOption("360p", "360p", "640x360", "~65 MB"),
    QualityOption("Auto", "Auto", "Best Available", "Optimal")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadAnimeOptionsSheet(
    anime: Anime,
    episode: Int = 1,
    episodeTitle: String? = null,
    episodesRange: IntRange? = null,
    initialProvider: Provider? = null,
    availableDirectServers: List<Provider>? = null,
    onDismiss: () -> Unit,
    onStartDownload: (provider: Provider, quality: String, audio: String) -> Unit = { _, _, _ -> },
    onStartBatchDownload: ((episodes: List<Int>, provider: Provider, quality: String, audio: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val storage = remember { StorageManager(context) }

    val savedAudio = remember { storage.getLastDownloadAudio() }
    val savedQuality = remember { storage.getLastDownloadQuality() }
    val savedServerId = remember { storage.getLastDownloadServerId() }

    val initialAudioMode = if (initialProvider?.isDub == true) "DUB"
        else if (initialProvider?.isMulti == true) "MULTI"
        else if (initialProvider != null) "SUB"
        else null

    var selectedAudio by remember(initialProvider) {
        mutableStateOf(
            initialAudioMode ?: savedAudio ?: "SUB"
        )
    }
    var selectedQuality by remember {
        mutableStateOf(savedQuality ?: "720p")
    }

    val availableProviders = remember(selectedAudio, availableDirectServers) {
        val isDubSelected = selectedAudio.equals("dub", ignoreCase = true)
        val isMultiSelected = selectedAudio.equals("multi", ignoreCase = true)

        val directList = if (!availableDirectServers.isNullOrEmpty()) {
            val matching = when {
                isDubSelected -> availableDirectServers.filter { it.isDub || it.isMulti }
                isMultiSelected -> availableDirectServers.filter { it.isMulti }
                else -> availableDirectServers.filter { !it.isDub && !it.isMulti }
            }
            if (matching.isNotEmpty()) matching else {
                com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudio)
            }
        } else {
            com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(selectedAudio)
        }

        // Direct servers
        val directServers = directList.filter { it.isDirectStream && !it.isEmbedOnly }

        // Embed mode servers supported via StreamSniffer (ACDN, MegaPlay, Vidstream)
        val embedServers = ALL_PROVIDERS.filter { prov ->
            prov.isEmbedOnly && (
                prov.id.contains("animesalt", ignoreCase = true) ||
                prov.id.contains("megaplay", ignoreCase = true) ||
                prov.id.contains("vidstream", ignoreCase = true)
            ) && when {
                isDubSelected -> prov.isDub || prov.isMulti
                isMultiSelected -> prov.isMulti
                else -> !prov.isDub && !prov.isMulti
            }
        }

        (directServers + embedServers).distinctBy { it.id }
    }

    var selectedProvider by remember(selectedAudio, initialProvider, availableProviders) {
        // Priority 1: Match the current active provider by exact ID
        val matchingInit = initialProvider?.let { init ->
            availableProviders.firstOrNull { it.id == init.id }
        }
        // Priority 2: Match current active provider by name (e.g. if audio was switched from SUB to DUB)
        val matchingInitByName = initialProvider?.let { init ->
            availableProviders.firstOrNull { it.name.equals(init.name, ignoreCase = true) }
        }
        // Priority 3: Previously saved server from storage, if it exists in available direct servers
        val matchingSaved = savedServerId?.let { sId ->
            availableProviders.firstOrNull { it.id == sId }
        }
        // Priority 4: First available direct server
        val firstDirect = availableProviders.firstOrNull { it.isDirectStream }
        mutableStateOf(
            matchingInit
                ?: matchingInitByName
                ?: matchingSaved
                ?: firstDirect
                ?: availableProviders.firstOrNull()
                ?: Provider("flixcloud-sub", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true)
        )
    }

    // Sheet drag distance in px (0 when fully open). Read only inside graphicsLayer lambdas, so no recomposition per frame.
    val density = LocalDensity.current
    val base = remember { floatArrayOf(Float.NaN) }
    val dragPx: () -> Float = {
        val off = runCatching { sheetState.requireOffset() }.getOrNull()
        if (off == null) 0f else {
            if (base[0].isNaN() || off < base[0]) base[0] = off
            off - base[0]
        }
    }
    val dragRange = with(density) { 420.dp.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Amoled,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 2.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(OttInk(0.25f))
            )
        }
    ) {
        // Stretch overscroll fights the sheet's own drag (the down-up-down-up jitter), so it is off inside the sheet.
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val p = (dragPx() / dragRange).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleX = 1f - 0.05f * p
                    scaleY = 1f - 0.05f * p
                    alpha = 1f - 0.45f * p
                }
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            val bobT = rememberInfiniteTransition(label = "bob")
            val bob by bobT.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bobY"
            )
            Row(
                modifier = Modifier.fillMaxWidth().floatIn(0, dragPx),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationY = (bob - 0.5f) * 5.dp.toPx() }
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.08f))
                        .border(1.dp, OttInk(0.19f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Download, null, tint = OttTextPrimary, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (episodesRange != null) "Download Episodes ${episodesRange.first}–${episodesRange.last}"
                        else "Download Episode $episode",
                        color = OttTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            episodesRange != null -> "${episodesRange.count()} episodes • ${anime.title}"
                            !episodeTitle.isNullOrBlank() -> episodeTitle
                            else -> anime.title
                        },
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(OttInk(0.08f))
                        .border(1.dp, OttInk(0.15f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = OttTextPrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Audio
            Column(Modifier.floatIn(1, dragPx), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("AUDIO TRACK")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SUB" to "Subbed", "DUB" to "Dubbed", "MULTI" to "Multi").forEach { (key, sub) ->
                        val sel = selectedAudio == key
                        GlassOption(
                            selected = sel,
                            onClick = { selectedAudio = key },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(key, color = if (sel) OttTextPrimary else Soft, fontSize = 13.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                                Text(sub, color = if (sel) Soft else Muted, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }

            // Quality
            Column(Modifier.floatIn(2, dragPx), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("VIDEO QUALITY")
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUALITY_OPTIONS) { opt ->
                        val sel = selectedQuality == opt.id
                        GlassOption(
                            selected = sel,
                            onClick = { selectedQuality = opt.id },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(opt.label, color = if (sel) OttTextPrimary else Soft, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                                Text(
                                    text = if (episodesRange != null && opt.id != "Auto") "${opt.approxSize}/ep" else opt.approxSize,
                                    color = if (sel) Soft else Muted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Servers (outer column already scrolls, so no nested fixed-height scroller)
            Column(Modifier.floatIn(3, dragPx), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("DOWNLOAD SERVER · ${availableProviders.size} AVAILABLE")
                availableProviders.forEach { prov ->
                    val sel = selectedProvider.id == prov.id
                    val isMulti = prov.isMultiAudio || prov.isMulti
                    val badgeText = when {
                        isMulti -> "MULTI-AUDIO"
                        prov.isEmbedOnly -> "SNIFFER"
                        prov.serverBadge.isNotBlank() -> prov.serverBadge.uppercase()
                        else -> "DIRECT HD"
                    }
                    val badgeColor = when {
                        isMulti -> Color(0xFF38BDF8)
                        prov.isEmbedOnly -> Color(0xFFA78BFA)
                        else -> Color(0xFF34D399)
                    }
                    GlassOption(
                        selected = sel,
                        onClick = { selectedProvider = prov },
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = prov.name,
                                modifier = Modifier.weight(1f, fill = false),
                                color = if (sel) OttTextPrimary else Soft,
                                fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.14f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (sel) OttTextPrimary else Color.Transparent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Captions banner
            Row(
                modifier = Modifier
                    .floatIn(4, dragPx)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OttInk(0.05f))
                    .border(1.dp, OttInk(0.10f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Subtitles, null, tint = Soft, modifier = Modifier.size(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Offline captions included", color = OttTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Matching subtitles download automatically for offline playback", color = Muted, fontSize = 10.5.sp, lineHeight = 14.sp)
                }
            }

            // Action
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
                label = "dlPress"
            )
            Row(
                modifier = Modifier
                    .floatIn(5, dragPx)
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(26.dp))
                    .background(OttTextPrimary)
                    .clickable(interactionSource = source, indication = null) {
                        storage.saveDownloadSettings(selectedAudio, selectedQuality, selectedProvider.id)
                        if (episodesRange != null && onStartBatchDownload != null) {
                            onStartBatchDownload(episodesRange.toList(), selectedProvider, selectedQuality, selectedAudio)
                        } else {
                            onStartDownload(selectedProvider, selectedQuality, selectedAudio)
                        }
                        onDismiss()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(19.dp))
                Text(
                    text = if (episodesRange != null) "Download ${episodesRange.count()} Episodes ($selectedQuality • $selectedAudio)"
                    else "Download ($selectedQuality • $selectedAudio)",
                    color = Color.Black,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp),
        color = Muted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

/** Selectable glass tile: lit gradient + bright edge when selected, spring press depth. */
@Composable
private fun GlassOption(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    edgeWidth: Dp = if (selected) 1.2.dp else 1.dp,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "optPress"
    )
    val pop by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "optPop"
    )
    val fill = if (selected) Brush.linearGradient(listOf(OttInk(0.22f), OttInk(0.07f)))
    else Brush.linearGradient(listOf(OttInk(0.08f), OttInk(0.02f)))
    val edge = if (selected) Brush.linearGradient(listOf(OttInk(0.85f), OttInk(0.25f)))
    else Brush.linearGradient(listOf(OttInk(0.19f), OttInk(0.04f)))
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale * pop; scaleY = scale * pop }
            .clip(shape)
            .background(fill)
            .border(edgeWidth, edge, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

/** Staggered spring entrance; lower sections also trail slightly behind while the sheet is dragged down. */
@Composable
private fun Modifier.floatIn(index: Int, dragPx: () -> Float): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 55L)
        p.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 240f))
    }
    return this.graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * 56.dp.toPx() - dragPx() * 0.035f * index
    }
}
