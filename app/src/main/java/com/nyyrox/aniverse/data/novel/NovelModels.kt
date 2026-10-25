package com.nyyrox.aniverse.data.novel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.nyyrox.aniverse.data.MediaRelation

data class NovelItem(
    val slug: String,
    val title: String,
    val cover: String,
    val stats: String? = null,
    val rating: String? = null,
    val source: String = "novelfire"
)

data class NovelDetail(
    val slug: String,
    val title: String,
    val cover: String,
    val author: String? = null,
    val rating: String? = null,
    val status: String? = null,
    val chaptersCount: String? = null,
    val views: String? = null,
    val genres: List<String> = emptyList(),
    val synopsis: String = "",
    val firstChapterSlug: String? = null,
    val banner: String? = null,
    val anilistId: Int? = null,
    val relations: List<MediaRelation> = emptyList(),
    val source: String = "novelfire"
)

data class NovelChapter(
    val slug: String,
    val title: String,
    val number: String? = null,
    val releaseTime: String? = null,
    val cover: String? = null
)

data class NovelChapterContent(
    val novelSlug: String,
    val chapterSlug: String,
    val title: String,
    val paragraphs: List<String>,
    val prevChapterSlug: String? = null,
    val nextChapterSlug: String? = null
)

data class NovelHistoryItem(
    val slug: String,
    val title: String,
    val cover: String,
    val lastChapterSlug: String,
    val lastChapterTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NovelFontFamily(val displayName: String, val fontFamily: FontFamily) {
    SERIF("Serif", FontFamily.Serif),
    SANS("Sans-Serif", FontFamily.SansSerif),
    MONO("Monospace", FontFamily.Monospace),
    DEFAULT("Default", FontFamily.Default)
}

enum class NovelPageStyle(val displayName: String) {
    PLAIN("Plain Paper"),
    LINED("Lined Notebook"),
    CRACKED("Antique Parchment"),
    DECKLED("Vintage Book")
}

enum class NovelTextColor(val displayName: String, val color: Color) {
    DEFAULT("Theme Default", Color.Unspecified),
    DEEP_CHARCOAL("Charcoal Black", Color(0xFF181818)),
    ANTIQUE_ESPRESSO("Espresso Brown", Color(0xFF2B1D14)),
    FOUNTAIN_NAVY("Fountain Navy", Color(0xFF14243B)),
    VINTAGE_SEPIA("Sepia Ink", Color(0xFF423223)),
    CREAM_WHITE("Cream White", Color(0xFFF2ECE1)),
    GOLDEN_AMBER("Golden Amber", Color(0xFFE6BA72))
}

enum class NovelReaderTheme(
    val displayName: String,
    val backgroundColor: Color,
    val textColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val lineOrCrackColor: Color = Color(0x20888888)
) {
    PARCHMENT_CREAM(
        displayName = "Cream Parchment",
        backgroundColor = Color(0xFFF7F3E8), // Authentic book paper ivory
        textColor = Color(0xFF231F20), // Deep antique ink
        secondaryColor = Color(0xFF7D7565),
        accentColor = Color(0xFFB8281E),
        lineOrCrackColor = Color(0x358D7B68)
    ),
    ANTIQUE_PAPYRUS(
        displayName = "Aged Papyrus",
        backgroundColor = Color(0xFFEFE2C6), // Aged weathered papyrus with cracks
        textColor = Color(0xFF281E15), // Dark espresso ink
        secondaryColor = Color(0xFF806B52),
        accentColor = Color(0xFF9E2A2B),
        lineOrCrackColor = Color(0x556B5338)
    ),
    LINED_NOTEBOOK(
        displayName = "Lined Journal",
        backgroundColor = Color(0xFFFBFBF9), // Clean lined notebook paper
        textColor = Color(0xFF162544), // Fountain pen navy
        secondaryColor = Color(0xFF6B7280),
        accentColor = Color(0xFFDC2626),
        lineOrCrackColor = Color(0x353B82F6) // Soft notebook blue rule
    ),
    WARM_SEPIA(
        displayName = "Warm Sepia",
        backgroundColor = Color(0xFF1E1A16),
        textColor = Color(0xFFEADBC8),
        secondaryColor = Color(0xFF9E8E7D),
        accentColor = Color(0xFFE5A95D),
        lineOrCrackColor = Color(0x25FFFFFF)
    ),
    SLATE_CHARCOAL(
        displayName = "Slate Charcoal",
        backgroundColor = Color(0xFF141721),
        textColor = Color(0xFFE0E6F0),
        secondaryColor = Color(0xFF7E899D),
        accentColor = Color(0xFF6366F1),
        lineOrCrackColor = Color(0x20FFFFFF)
    ),
    CYBER_DARK(
        displayName = "Cyber Dark",
        backgroundColor = Color(0xFF0A0D14),
        textColor = Color(0xFFEDEFF7),
        secondaryColor = Color(0xFF828A9E),
        accentColor = Color(0xFFFF3B49),
        lineOrCrackColor = Color(0x1AFFFFFF)
    ),
    OLED_BLACK(
        displayName = "OLED Black",
        backgroundColor = Color(0xFF000000),
        textColor = Color(0xFFE2E4EB),
        secondaryColor = Color(0xFF8B92A4),
        accentColor = Color(0xFFFF1E27),
        lineOrCrackColor = Color(0x15FFFFFF)
    )
}

data class NovelReaderSettings(
    val fontSize: Float = 17f,
    val fontFamily: NovelFontFamily = NovelFontFamily.SERIF,
    val lineHeight: Float = 1.75f,
    val theme: NovelReaderTheme = NovelReaderTheme.OLED_BLACK,
    val pageStyle: NovelPageStyle = NovelPageStyle.PLAIN,
    val customTextColor: NovelTextColor = NovelTextColor.DEFAULT,
    val brightness: Float = 1.0f,
    val isContinuousScroll: Boolean = true,
    val indentParagraphs: Boolean = true
)
