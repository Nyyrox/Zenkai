package com.nyyrox.aniverse.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import kotlin.math.roundToInt

// =========================================================================
// Models & Presets
// =========================================================================

enum class CaptionSettingsTab(val title: String) {
    PRESETS("Presets"),
    FONT("Font"),
    TEXT("Text"),
    BOX("Box"),
    SYNC("Sync")
}

data class CaptionStylePreset(
    val name: String,
    val previewLabel: String,
    val description: String = "",
    val sampleText: String = "Subtitle Style",
    val config: CaptionCustomization
)

val CaptionPresetsList: List<CaptionStylePreset> = listOf(
    CaptionStylePreset(
        name = "Crunchyroll",
        previewLabel = "Crunchyroll",
        description = "Official anime style · Bold black outline",
        sampleText = "I'll become the greatest hero!",
        config = CaptionCustomization(
            fontFamilyName = "Roboto",
            textColor = Color.White,
            bgAlpha = 0f,
            hasBgBox = false,
            isBold = true,
            weightPreset = "Bold",
            hasStroke = true,
            strokeColor = Color.Black,
            strokeWidth = 3.8f,
            hasShadow = true,
            activePreset = "Crunchyroll"
        )
    ),
    CaptionStylePreset(
        name = "Netflix",
        previewLabel = "Netflix",
        description = "Proportional sans · Clean drop shadow",
        sampleText = "Never give up on your dreams!",
        config = CaptionCustomization(
            fontFamilyName = "Inter",
            textColor = Color.White,
            bgAlpha = 0f,
            hasBgBox = false,
            isBold = true,
            weightPreset = "Bold",
            hasStroke = true,
            strokeColor = Color(0xB3000000),
            strokeWidth = 2.0f,
            hasShadow = true,
            activePreset = "Netflix"
        )
    ),
    CaptionStylePreset(
        name = "Fansub Best",
        previewLabel = "Fansub Best",
        description = "Golden anime fansub · Heavy dark stroke",
        sampleText = "Omae wa mou shindeiru.",
        config = CaptionCustomization(
            fontFamilyName = "Outfit",
            textColor = Color(0xFFFFDD00),
            bgAlpha = 0f,
            hasBgBox = false,
            isBold = true,
            weightPreset = "Heavy",
            hasStroke = true,
            strokeColor = Color.Black,
            strokeWidth = 4.2f,
            hasShadow = true,
            activePreset = "Fansub Best"
        )
    ),
    CaptionStylePreset(
        name = "Crunchy Amber",
        previewLabel = "Crunchy Amber",
        description = "Warm canary yellow · Solid black stroke",
        sampleText = "Believe it! Dattebayo!",
        config = CaptionCustomization(
            fontFamilyName = "Roboto",
            textColor = Color(0xFFFFEE55),
            bgAlpha = 0f,
            hasBgBox = false,
            isBold = true,
            weightPreset = "Bold",
            hasStroke = true,
            strokeColor = Color.Black,
            strokeWidth = 3.8f,
            hasShadow = true,
            activePreset = "Crunchy Amber"
        )
    ),
    CaptionStylePreset(
        name = "Cinema",
        previewLabel = "Cinema",
        description = "Warm serif italic · Translucent backing box",
        sampleText = "In a world of silence...",
        config = CaptionCustomization(
            fontFamilyName = "Merriweather",
            textColor = Color(0xFFF3EFE0),
            bgAlpha = 0.50f,
            bgColor = Color(0xFF0F1117),
            hasBgBox = true,
            boxCornerRadiusDp = 6f,
            isItalic = true,
            isBold = false,
            letterSpacingSp = 0.8f,
            hasStroke = false,
            strokeWidth = 0f,
            hasShadow = true,
            activePreset = "Cinema"
        )
    ),
    CaptionStylePreset(
        name = "Cyber Neon",
        previewLabel = "Cyber Neon",
        description = "Electric cyan · Futuristic anime glow",
        sampleText = "System override initialized.",
        config = CaptionCustomization(
            fontFamilyName = "Outfit",
            textColor = Color(0xFF38BDF8),
            bgAlpha = 0.45f,
            bgColor = Color(0xFF030712),
            hasBgBox = true,
            boxCornerRadiusDp = 8f,
            isBold = true,
            hasStroke = true,
            strokeColor = Color(0xFF0C4A6E),
            strokeWidth = 2.4f,
            hasShadow = true,
            activePreset = "Cyber Neon"
        )
    ),
    CaptionStylePreset(
        name = "Modern Glass",
        previewLabel = "Modern Glass",
        description = "Frosted glass capsule · High legibility",
        sampleText = "Clear, crisp and centered.",
        config = CaptionCustomization(
            fontFamilyName = "Inter",
            textColor = Color.White,
            bgAlpha = 0.70f,
            bgColor = Color(0xFF1E2430),
            hasBgBox = true,
            boxCornerRadiusDp = 10f,
            isBold = true,
            hasStroke = false,
            strokeWidth = 0f,
            hasShadow = false,
            activePreset = "Modern Glass"
        )
    ),
    CaptionStylePreset(
        name = "High Contrast",
        previewLabel = "High Contrast",
        description = "Accessible bright yellow on solid black",
        sampleText = "MAXIMUM VISIBILITY",
        config = CaptionCustomization(
            fontFamilyName = "Roboto",
            textColor = Color(0xFFFFFF00),
            bgAlpha = 0.95f,
            bgColor = Color.Black,
            hasBgBox = true,
            boxCornerRadiusDp = 4f,
            isBold = true,
            weightPreset = "Heavy",
            hasStroke = false,
            strokeWidth = 0f,
            hasShadow = false,
            activePreset = "High Contrast"
        )
    ),
    CaptionStylePreset(
        name = "Retro VHS",
        previewLabel = "Retro VHS",
        description = "Monospace anime tape · Retro arcade feel",
        sampleText = "EPISODE 01 // PLAY [SP]",
        config = CaptionCustomization(
            fontFamilyName = "JetBrains Mono",
            textColor = Color(0xFFFACC15),
            bgAlpha = 0f,
            hasBgBox = false,
            isBold = true,
            isUppercase = true,
            letterSpacingSp = 1.0f,
            hasStroke = true,
            strokeColor = Color.Black,
            strokeWidth = 3.0f,
            hasShadow = true,
            activePreset = "Retro VHS"
        )
    ),
    CaptionStylePreset(
        name = "Default",
        previewLabel = "Default",
        description = "Classic anime subtitles with subtle box",
        sampleText = "I will protect my friends!",
        config = CaptionCustomization(
            fontFamilyName = "Roboto",
            textColor = Color.White,
            bgAlpha = 0.55f,
            bgColor = Color.Black,
            hasBgBox = true,
            boxCornerRadiusDp = 6f,
            isBold = false,
            hasStroke = true,
            strokeColor = Color.Black,
            strokeWidth = 2.0f,
            activePreset = "Default"
        )
    )
)

data class SubtitleFontInfo(
    val name: String,
    val category: String,
    val fontFamily: FontFamily
)

val SubtitleFontsList: List<SubtitleFontInfo> = listOf(
    SubtitleFontInfo("Inter", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Roboto", "Sans", FontFamily.Default),
    SubtitleFontInfo("Outfit", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Nunito", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Poppins", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Montserrat", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Open Sans", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Lato", "Sans", FontFamily.SansSerif),
    SubtitleFontInfo("Merriweather", "Serif", FontFamily.Serif),
    SubtitleFontInfo("Playfair", "Serif", FontFamily.Serif),
    SubtitleFontInfo("JetBrains Mono", "Mono", FontFamily.Monospace),
    SubtitleFontInfo("Fira Code", "Mono", FontFamily.Monospace),
    SubtitleFontInfo("Bebas Neue", "Display", FontFamily.SansSerif),
    SubtitleFontInfo("Oswald", "Display", FontFamily.SansSerif),
    SubtitleFontInfo("Caveat", "Handwriting", FontFamily.Cursive),
    SubtitleFontInfo("Pacifico", "Handwriting", FontFamily.Cursive)
)

fun resolveFontFamily(name: String): FontFamily {
    return SubtitleFontsList.find { it.name.equals(name, ignoreCase = true) }?.fontFamily ?: FontFamily.Default
}

// =========================================================================
// 1. Subtitles Selection Modal (Image 1)
// =========================================================================

@Composable
fun SubtitlesModal(
    availableSubtitles: List<SubtitleTrack>,
    isCaptionsEnabled: Boolean,
    activeCaptionTitle: String,
    activeSubtitleUrl: String?,
    downloadingSubtitleId: String?,
    onSelectSubtitle: (SubtitleTrack) -> Unit,
    onTurnOffCaptions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDialoguePanel: () -> Unit,
    onUploadClick: () -> Unit,
    onDismiss: () -> Unit,
    anchor: Rect? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedLanguages = remember { mutableStateMapOf<String, Boolean>() }

    // Group subtitles by language
    val groupedByLang = remember(availableSubtitles, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            availableSubtitles
        } else {
            availableSubtitles.filter {
                it.language.contains(searchQuery, ignoreCase = true) ||
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.source.contains(searchQuery, ignoreCase = true)
            }
        }
        val map = linkedMapOf<String, MutableList<SubtitleTrack>>()
        filtered.forEach { sub ->
            val lang = sub.language.ifBlank { "English" }
            map.getOrPut(lang) { mutableListOf() }.add(sub)
        }
        map
    }

    // Default expand the first active language or English
    LaunchedEffect(availableSubtitles) {
        if (expandedLanguages.isEmpty() && groupedByLang.isNotEmpty()) {
            val activeLang = availableSubtitles.find {
                it.url == activeSubtitleUrl || it.label == activeCaptionTitle
            }?.language?.ifBlank { "English" } ?: "English"
            expandedLanguages[activeLang] = true
            if (groupedByLang.keys.firstOrNull() != null) {
                expandedLanguages[groupedByLang.keys.first()] = true
            }
        }
    }

    GlassDialog(
        anchor = anchor,
        centered = true,
        onDismiss = onDismiss
    ) {
        Box(
            modifier = modifier
                .widthIn(min = 310.dp, max = 360.dp)
                .heightIn(max = 540.dp)
                .glassPanel(24.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header (Image 1): 文A SUBTITLES on left, ⚙ Settings on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "SUBTITLES",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenSettings() }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Style & Presets",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Filter search bar (Image 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1E26))
                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF717684),
                            modifier = Modifier.size(17.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Filter languages...",
                                    color = Color(0xFF717684),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // Subtitle list: "Off" option + Language accordions
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // "Off" option row
                    item {
                        val isOff = !isCaptionsEnabled || activeCaptionTitle == "Off"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isOff) Color(0x18FFFFFF) else Color.Transparent)
                                .clickable { onTurnOffCaptions() }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Off",
                                color = if (isOff) Color.White else Color(0xD9FFFFFF),
                                fontSize = 13.5.sp,
                                fontWeight = if (isOff) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isOff) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Language groups
                    groupedByLang.forEach { (language, tracks) ->
                        val isExpanded = expandedLanguages[language] ?: false
                        val hasSelectedTrack = tracks.any {
                            isCaptionsEnabled && (it.url == activeSubtitleUrl || (activeSubtitleUrl == null && it.label == activeCaptionTitle))
                        }

                        item(key = "header_$language") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isExpanded) Color(0x18FFFFFF) else Color.Transparent)
                                    .clickable {
                                        expandedLanguages[language] = !isExpanded
                                    }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$language (${tracks.size})",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (hasSelectedTrack) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFFA0A5B5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isExpanded) {
                            items(tracks, key = { it.id }) { track ->
                                val isSelected = isCaptionsEnabled &&
                                        (track.url == activeSubtitleUrl || (activeSubtitleUrl == null && track.label == activeCaptionTitle))
                                val isDownloading = downloadingSubtitleId == track.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0x18FFFFFF) else Color.Transparent)
                                        .clickable(enabled = !isDownloading) {
                                            onSelectSubtitle(track)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.label.ifBlank { "Default" },
                                            color = if (isSelected) Color.White else Color(0xD9FFFFFF),
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        if (track.source.isNotBlank() && track.source != "Stream") {
                                            Text(
                                                text = track.source,
                                                color = Color(0xFF868C9E),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom auxiliary actions (Upload & Dialogue Transcript)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(10.dp))
                            .clickable { onUploadClick() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Upload .SRT", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(10.dp))
                            .clickable { onOpenDialoguePanel() }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Transcript", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. Caption Settings Modal (Images 2, 3, 4)
// =========================================================================

@Composable
private fun CaptionLivePreviewBox(
    captionConfig: CaptionCustomization,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0D14), Color(0xFF141924))
                )
            )
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "LIVE PREVIEW",
                color = Color(0xCCFFFFFF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        val fontFam = resolveFontFamily(captionConfig.fontFamilyName)
        val baseSize = (13.5f * captionConfig.sizeScale).coerceAtLeast(11f).sp

        Box(
            modifier = Modifier
                .then(
                    if (captionConfig.hasBgBox && captionConfig.bgAlpha > 0f) {
                        Modifier
                            .clip(RoundedCornerShape(captionConfig.boxCornerRadiusDp.dp))
                            .background(captionConfig.bgColor.copy(alpha = captionConfig.bgAlpha))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            val displayText = if (captionConfig.isUppercase) {
                "I'M GONNA BE KING OF THE PIRATES!"
            } else {
                "I'm gonna be King of the Pirates!"
            }

            // Optional text stroke
            if (captionConfig.hasStroke && captionConfig.strokeWidth > 0f) {
                Text(
                    text = displayText,
                    fontFamily = fontFam,
                    fontSize = baseSize,
                    fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (captionConfig.isItalic) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = captionConfig.letterSpacingSp.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle.Default.copy(
                        color = captionConfig.strokeColor,
                        drawStyle = Stroke(
                            width = captionConfig.strokeWidth * 0.85f,
                            join = StrokeJoin.Round
                        )
                    )
                )
            }

            // Main text
            Text(
                text = displayText,
                fontFamily = fontFam,
                fontSize = baseSize,
                fontWeight = if (captionConfig.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (captionConfig.isItalic) FontStyle.Italic else FontStyle.Normal,
                letterSpacing = captionConfig.letterSpacingSp.sp,
                color = captionConfig.textColor.copy(alpha = captionConfig.textAlpha),
                textAlign = TextAlign.Center,
                style = if (captionConfig.hasShadow) {
                    TextStyle.Default.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            blurRadius = 4f
                        )
                    )
                } else TextStyle.Default
            )
        }
    }
}

@Composable
private fun RenderCaptionTabContent(
    activeTab: CaptionSettingsTab,
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit,
    isCaptionsEnabled: Boolean,
    onToggleCaptions: (Boolean) -> Unit,
    selectedFontCategory: String,
    onSelectFontCategory: (String) -> Unit,
    subSyncMs: Long,
    onAdjustSync: (Long) -> Unit,
    onResetSync: () -> Unit
) {
    when (activeTab) {
        CaptionSettingsTab.PRESETS -> {
            PresetsTabContent(
                captionConfig = captionConfig,
                onConfigChange = onConfigChange,
                isCaptionsEnabled = isCaptionsEnabled,
                onToggleCaptions = onToggleCaptions
            )
        }
        CaptionSettingsTab.FONT -> {
            FontTabContent(
                captionConfig = captionConfig,
                onConfigChange = onConfigChange,
                selectedCategory = selectedFontCategory,
                onSelectCategory = onSelectFontCategory
            )
        }
        CaptionSettingsTab.TEXT -> {
            TextTabContent(
                captionConfig = captionConfig,
                onConfigChange = onConfigChange
            )
        }
        CaptionSettingsTab.BOX -> {
            BoxTabContent(
                captionConfig = captionConfig,
                onConfigChange = onConfigChange
            )
        }
        CaptionSettingsTab.SYNC -> {
            SyncTabContent(
                subSyncMs = subSyncMs,
                onAdjustSync = onAdjustSync,
                onResetSync = onResetSync
            )
        }
    }
}

@Composable
fun CaptionSettingsModal(
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit,
    isCaptionsEnabled: Boolean,
    onToggleCaptions: (Boolean) -> Unit,
    subSyncMs: Long,
    onAdjustSync: (Long) -> Unit,
    onResetSync: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(CaptionSettingsTab.PRESETS) }
    var selectedFontCategory by remember { mutableStateOf("All") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 580.dp

            Box(
                modifier = modifier
                    .fillMaxWidth(if (isWide) 0.88f else 0.96f)
                    .widthIn(max = 760.dp)
                    .fillMaxHeight(if (isWide) 0.88f else 0.94f)
                    .heightIn(max = 580.dp)
                    .glassPanel(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent dismiss on modal click */ }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: "Subtitle Style & Presets" + Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtitle Style & Presets",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isWide) {
                        // Two-Pane Content for Landscape / Tablets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Pane: Live Preview + Tab Menu
                            Column(
                                modifier = Modifier
                                    .width(200.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CaptionLivePreviewBox(
                                    captionConfig = captionConfig,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CaptionSettingsTab.values().forEach { tab ->
                                        val isSelected = activeTab == tab
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0x22FFFFFF) else Color.Transparent)
                                                .clickable { activeTab = tab }
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.5.dp)
                                                        .height(18.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Color.White)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = tab.title,
                                                color = if (isSelected) Color.White else Color(0xFF9EA3B2),
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Pane: Active Tab Controls
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                RenderCaptionTabContent(
                                    activeTab = activeTab,
                                    captionConfig = captionConfig,
                                    onConfigChange = onConfigChange,
                                    isCaptionsEnabled = isCaptionsEnabled,
                                    onToggleCaptions = onToggleCaptions,
                                    selectedFontCategory = selectedFontCategory,
                                    onSelectFontCategory = { selectedFontCategory = it },
                                    subSyncMs = subSyncMs,
                                    onAdjustSync = onAdjustSync,
                                    onResetSync = onResetSync
                                )
                            }
                        }
                    } else {
                        // Portrait / Compact Phone Layout (Full-Width Responsive!)
                        CaptionLivePreviewBox(
                            captionConfig = captionConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Horizontal Scrollable Tab Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CaptionSettingsTab.values().forEach { tab ->
                                val isSelected = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0x26FFFFFF) else Color(0x0CFFFFFF))
                                        .clickable { activeTab = tab }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) Color.White else Color(0xFF9EA3B2),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Full-Width Tab Content Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            RenderCaptionTabContent(
                                activeTab = activeTab,
                                captionConfig = captionConfig,
                                onConfigChange = onConfigChange,
                                isCaptionsEnabled = isCaptionsEnabled,
                                onToggleCaptions = onToggleCaptions,
                                selectedFontCategory = selectedFontCategory,
                                onSelectFontCategory = { selectedFontCategory = it },
                                subSyncMs = subSyncMs,
                                onAdjustSync = onAdjustSync,
                                onResetSync = onResetSync
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Tab 1: PRESETS Content (Image 2)
// =========================================================================

@Composable
private fun PresetsTabContent(
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit,
    isCaptionsEnabled: Boolean,
    onToggleCaptions: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Show subtitles" toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF))
                .clickable { onToggleCaptions(!isCaptionsEnabled) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Show subtitles",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isCaptionsEnabled) "Captions currently visible" else "Captions turned off",
                    color = Color(0xFF888E9E),
                    fontSize = 11.5.sp
                )
            }
            Switch(
                checked = isCaptionsEnabled,
                onCheckedChange = onToggleCaptions,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color(0xFFAAAAAA),
                    uncheckedTrackColor = Color(0x33FFFFFF)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STYLE PRESETS",
                color = Color(0xFF888E9E),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Tap any style to apply",
                color = Color(0xFF6B7280),
                fontSize = 11.sp
            )
        }

        // 2-Column Grid of Style Presets
        val chunked = remember { CaptionPresetsList.chunked(2) }
        chunked.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { preset ->
                    val isSelected = captionConfig.activePreset == preset.name
                    PresetCardItem(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = {
                            onConfigChange(preset.config)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PresetCardItem(
    preset: CaptionStylePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = preset.config
    val fontFam = resolveFontFamily(cfg.fontFamilyName)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isSelected) Color(0xFF263348) else Color(0xFF1D222E),
                        if (isSelected) Color(0xFF17202F) else Color(0xFF13161F)
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Mini Simulated Video Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF090B10), Color(0xFF141822))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val sample = if (cfg.isUppercase) preset.sampleText.uppercase() else preset.sampleText

            Box(
                modifier = Modifier
                    .then(
                        if (cfg.hasBgBox && cfg.bgAlpha > 0f) {
                            Modifier
                                .clip(RoundedCornerShape(cfg.boxCornerRadiusDp.dp))
                                .background(cfg.bgColor.copy(alpha = cfg.bgAlpha))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Stroke outline layer
                if (cfg.hasStroke && cfg.strokeWidth > 0f) {
                    Text(
                        text = sample,
                        fontFamily = fontFam,
                        fontSize = 11.5.sp,
                        fontWeight = if (cfg.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (cfg.isItalic) FontStyle.Italic else FontStyle.Normal,
                        letterSpacing = cfg.letterSpacingSp.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle.Default.copy(
                            color = cfg.strokeColor,
                            drawStyle = Stroke(
                                width = cfg.strokeWidth * 0.75f,
                                join = StrokeJoin.Round
                            )
                        )
                    )
                }

                // Fill text layer
                Text(
                    text = sample,
                    fontFamily = fontFam,
                    fontSize = 11.5.sp,
                    fontWeight = if (cfg.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (cfg.isItalic) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = cfg.letterSpacingSp.sp,
                    color = cfg.textColor.copy(alpha = cfg.textAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = if (cfg.hasShadow) {
                        TextStyle.Default.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                blurRadius = 3f
                            )
                        )
                    } else TextStyle.Default
                )
            }
        }

        // Preset Details Label
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    color = if (isSelected) Color.White else Color(0xFFD6DBE7),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )

                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.22f))
                            .border(0.5.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "ACTIVE",
                            color = Color(0xFF34D399),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (preset.description.isNotBlank()) {
                Text(
                    text = preset.description,
                    color = if (isSelected) Color(0xFF93C5FD) else Color(0xFF7F8697),
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// Tab 2: FONT Content (Image 3)
// =========================================================================

@Composable
private fun FontTabContent(
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val categories = listOf("All", "Sans", "Serif", "Mono", "Display", "Handwriting")
    val filteredFonts = remember(selectedCategory) {
        if (selectedCategory == "All") SubtitleFontsList
        else SubtitleFontsList.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category Pills (Smooth horizontal scrollable row with single-line chips - never wraps Serif!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0x28FFFFFF) else Color(0x0EFFFFFF))
                        .clickable { onSelectCategory(cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else Color(0xB3FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Font Rows (Clean list items: NO cards, NO heavy borders, selected with subtle highlight only)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            filteredFonts.forEach { font ->
                val isSelected = captionConfig.fontFamilyName.equals(font.name, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0x18FFFFFF) else Color.Transparent)
                        .clickable {
                            onConfigChange(captionConfig.copy(fontFamilyName = font.name))
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = font.name,
                        fontFamily = font.fontFamily,
                        color = if (isSelected) Color.White else Color(0xD9FFFFFF),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "The quick brown fox",
                            fontFamily = font.fontFamily,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF888E9E),
                            fontSize = 12.5.sp,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Tab 3: TEXT Content (Image 4)
// =========================================================================

@Composable
private fun TextTabContent(
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. SIZE buttons (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("SIZE", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("Tiny", 14f),
                    Pair("Small", 16f),
                    Pair("Medium", 18f),
                    Pair("Large", 22f),
                    Pair("Xlarge", 26f)
                ).forEach { (label, spVal) ->
                    val isSelected = captionConfig.sizePreset.equals(label, ignoreCase = true) ||
                            (captionConfig.fontSizeSp == spVal && captionConfig.sizePreset.isEmpty())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF1B1E26))
                            .border(1.dp, if (isSelected) Color(0x40FFFFFF) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                onConfigChange(captionConfig.copy(fontSizeSp = spVal, sizePreset = label))
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0F1117) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. SIZE FINE-TUNE (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val pct = (captionConfig.sizeScale * 100).roundToInt()
            Text(
                text = "SIZE FINE-TUNE ($pct%)",
                color = Color(0xFF888E9E),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Slider(
                value = captionConfig.sizeScale,
                onValueChange = { onConfigChange(captionConfig.copy(sizeScale = it)) },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. WEIGHT (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("WEIGHT", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Light", "Normal", "Bold", "Heavy").forEach { w ->
                    val isSelected = captionConfig.weightPreset.equals(w, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF1B1E26))
                            .border(1.dp, if (isSelected) Color(0x40FFFFFF) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                onConfigChange(
                                    captionConfig.copy(
                                        weightPreset = w,
                                        isBold = w == "Bold" || w == "Heavy"
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = w,
                            color = if (isSelected) Color(0xFF0F1117) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. STYLE (Italic / UPPERCASE) (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("STYLE", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (captionConfig.isItalic) Color.White else Color(0xFF1B1E26))
                        .border(1.dp, if (captionConfig.isItalic) Color.White else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onConfigChange(captionConfig.copy(isItalic = !captionConfig.isItalic)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Italic",
                        fontStyle = FontStyle.Italic,
                        color = if (captionConfig.isItalic) Color(0xFF0F1117) else Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (captionConfig.isUppercase) Color.White else Color(0xFF1B1E26))
                        .border(1.dp, if (captionConfig.isUppercase) Color.White else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onConfigChange(captionConfig.copy(isUppercase = !captionConfig.isUppercase)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "UPPERCASE",
                        color = if (captionConfig.isUppercase) Color(0xFF0F1117) else Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. LETTER SPACING (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("LETTER SPACING", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("Tight", -0.8f),
                    Pair("Normal", 0f),
                    Pair("Wide", 1.8f)
                ).forEach { (label, spVal) ->
                    val isSelected = captionConfig.letterSpacingSp == spVal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF1B1E26))
                            .border(1.dp, if (isSelected) Color(0x40FFFFFF) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onConfigChange(captionConfig.copy(letterSpacingSp = spVal)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0F1117) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 6. COLOR Palette (Image 4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("COLOR", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFFEB3B),
                    Color(0xFF00E5FF),
                    Color(0xFF69F0AE),
                    Color(0xFFFF9100),
                    Color(0xFFFF4081)
                ).forEach { col ->
                    val isSelected = captionConfig.textColor == col
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(col)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x40FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onConfigChange(captionConfig.copy(textColor = col)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (col == Color.White || col == Color(0xFFFFEB3B)) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Rainbow / custom option swatch
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { onConfigChange(captionConfig.copy(textColor = Color(0xFFFFEE55))) }
                )
            }
        }
    }
}

// =========================================================================
// Tab 4: BOX Content
// =========================================================================

@Composable
private fun BoxTabContent(
    captionConfig: CaptionCustomization,
    onConfigChange: (CaptionCustomization) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toggle Background Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onConfigChange(captionConfig.copy(hasBgBox = !captionConfig.hasBgBox)) }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Background Box", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Dark backdrop behind subtitle text", color = Color(0xFF888E9E), fontSize = 11.5.sp)
            }
            Switch(
                checked = captionConfig.hasBgBox,
                onCheckedChange = { onConfigChange(captionConfig.copy(hasBgBox = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color(0xFFAAAAAA),
                    uncheckedTrackColor = Color(0x33FFFFFF)
                )
            )
        }

        // Opacity Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val pct = (captionConfig.bgAlpha * 100).roundToInt()
            Text("BOX OPACITY ($pct%)", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Slider(
                value = captionConfig.bgAlpha,
                onValueChange = { onConfigChange(captionConfig.copy(bgAlpha = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Corner Radius
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CORNER RADIUS", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("None", 0f),
                    Pair("Small", 4f),
                    Pair("Medium", 8f),
                    Pair("Pill", 16f)
                ).forEach { (label, r) ->
                    val isSelected = captionConfig.boxCornerRadiusDp == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF1B1E26))
                            .border(1.dp, if (isSelected) Color(0x40FFFFFF) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onConfigChange(captionConfig.copy(boxCornerRadiusDp = r)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0F1117) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Height From Bottom
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val pos = captionConfig.bottomPaddingDp.roundToInt()
            Text("HEIGHT FROM BOTTOM (${pos}dp)", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Slider(
                value = captionConfig.bottomPaddingDp,
                onValueChange = { onConfigChange(captionConfig.copy(bottomPaddingDp = it)) },
                valueRange = 4f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// Tab 5: SYNC Content
// =========================================================================

@Composable
private fun SyncTabContent(
    subSyncMs: Long,
    onAdjustSync: (Long) -> Unit,
    onResetSync: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SUBTITLE TIMING SYNC", color = Color(0xFF888E9E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

        // Current Offset Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1E26))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${if (subSyncMs > 0) "+" else ""}${subSyncMs} ms",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${if (subSyncMs > 0) "+" else ""}${"%.2f".format(subSyncMs / 1000f)}s offset",
                    color = Color(0xFF888E9E),
                    fontSize = 12.sp
                )
            }
        }

        // Stepper Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Pair("-500ms", -500L),
                Pair("-100ms", -100L),
                Pair("Reset", 0L),
                Pair("+100ms", 100L),
                Pair("+500ms", 500L)
            ).forEach { (label, delta) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (label == "Reset") Color.White else Color(0xFF1B1E26))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .clickable {
                            if (label == "Reset") onResetSync() else onAdjustSync(delta)
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (label == "Reset") Color(0xFF0F1117) else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "• If dialogues appear before characters speak, tap +100ms or +500ms.\n• If subtitles are lagging behind voices, tap -100ms or -500ms.",
            color = Color(0xFF888E9E),
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}
