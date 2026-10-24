package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * HYBRID theme engine.
 *  - NEW API: AppColors / LocalAppColors / LocalIsDark / ThemeMode (light + dark, glass tokens)
 *  - OLD API: AnimeTheme / ThemeColors / getThemeColors / LocalThemeColors / LocalAnimeTheme / ThemeSettings.theme
 * Old screens keep compiling; migrate them to LocalAppColors at your own pace.
 */

// ---------------- OLD: anime presets ----------------
enum class AnimeTheme(
    val title: String,
    val anime: String,
    val description: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewBg: Color
) {
    ZENKAI_CRIMSON(
        title = "Zenkai Crimson",
        anime = "Zenkai Blood",
        description = "Blood Red Katana & Obsidian Flow",
        previewPrimary = Color(0xFFFF1E27),
        previewSecondary = Color(0xFF8A0303),
        previewBg = Color(0xFF080204)
    ),
    GOJO_LIMITLESS(
        title = "Gojo Limitless",
        anime = "Jujutsu Kaisen",
        description = "Infinity Cyan & Hollow Purple Cursed Energy",
        previewPrimary = Color(0xFF38BDF8),
        previewSecondary = Color(0xFFA855F7),
        previewBg = Color(0xFF040711)
    ),
    SUKUNA_MALEVOLENT(
        title = "Sukuna Malevolent",
        anime = "Jujutsu Kaisen",
        description = "Malevolent Shrine Crimson & Cleave Fire",
        previewPrimary = Color(0xFFEF4444),
        previewSecondary = Color(0xFFF97316),
        previewBg = Color(0xFF0A0406)
    ),
    SHADOW_MONARCH(
        title = "Shadow Monarch",
        anime = "Solo Leveling",
        description = "Arise Abyssal Obsidian & Electric Violet",
        previewPrimary = Color(0xFF8B5CF6),
        previewSecondary = Color(0xFF3B82F6),
        previewBg = Color(0xFF06050D)
    ),
    SUN_BREATHING(
        title = "Sun Breathing",
        anime = "Demon Slayer",
        description = "Hinokami Kagura Solar Gold & Nichirin Flame",
        previewPrimary = Color(0xFFF59E0B),
        previewSecondary = Color(0xFFFB923C),
        previewBg = Color(0xFF0D0604)
    ),
    CYBERPUNK_EDGERUNNERS(
        title = "Edgerunners",
        anime = "Cyberpunk 2077",
        description = "Sandevistan Neon Yellow & Chrome Cyan",
        previewPrimary = Color(0xFFFACC15),
        previewSecondary = Color(0xFF06B6D4),
        previewBg = Color(0xFF05080E)
    ),
    SURVEY_CORPS(
        title = "Survey Corps",
        anime = "Attack on Titan",
        description = "Scout Wings Emerald & Titan ODM Bronze",
        previewPrimary = Color(0xFF10B981),
        previewSecondary = Color(0xFFD97706),
        previewBg = Color(0xFF050B08)
    ),
    OLED_STEALTH(
        title = "OLED Stealth",
        anime = "Minimalist",
        description = "Absolute Pitch Black & Frosted Silver Quartz",
        previewPrimary = Color(0xFFE2E8F0),
        previewSecondary = Color(0xFF94A3B8),
        previewBg = Color(0xFF000000)
    )
}

// ---------------- OLD: legacy colour bag (still used by unmigrated screens) ----------------
data class ThemeColors(
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val secondary: Color,
    val accentGlow: Color,
    val borderGlow: Color,
    val gradient: List<Color>
)

// ---------------- NEW: light/dark mode ----------------
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// theme first + mode last keeps old positional/named callers working.
// mode defaults to DARK because unmigrated screens use dark-only Ott* constants; set SYSTEM once they're migrated.
data class ThemeSettings(
    val theme: AnimeTheme = AnimeTheme.ZENKAI_CRIMSON,
    val isReducedMotionEnabled: Boolean = false,
    val isPowerSavingEnabled: Boolean = false,
    val preferredRefreshRate: Int = 90,
    val isGlassmorphismEnabled: Boolean = true,
    val mode: ThemeMode = ThemeMode.DARK
)

// ---------------- NEW: AppColors ----------------
data class GlassTokens(val tint: Color, val border: Color)

data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,      // neutral: white (dark) / near-black (light)
    val onAccent: Color,
    val brand: Color,       // the ~1% red: logo, splash, LIVE dot only
    val error: Color,
    val skeleton: Color,
    val skeletonHighlight: Color,
    val glass: GlassTokens
) {
    val heroGradient: Brush
        get() = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.4f to bg.copy(alpha = 0.25f),
            0.8f to bg.copy(alpha = 0.87f),
            1.0f to bg
        )
    val cardBottomGradient: Brush
        get() = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.5f to bg.copy(alpha = 0.5f),
            1.0f to bg.copy(alpha = 0.96f)
        )
}

val Brand = Color(0xFFFF1E27)
val StarGold = Color(0xFFFFD700) // rating star only

val DarkColors = AppColors(
    bg = Color(0xFF040406),
    surface = Color(0xFF0C0D12),
    surfaceElevated = Color(0xFF14151B),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0A4B2),
    textMuted = Color(0xFF6B6E7E),
    accent = Color(0xFFFFFFFF),
    onAccent = Color(0xFF000000),
    brand = Brand,
    error = Color(0xFFFF6B70),
    skeleton = Color(0x10FFFFFF),
    skeletonHighlight = Color(0x26FFFFFF),
    glass = GlassTokens(tint = Color(0x12FFFFFF), border = Color(0x24FFFFFF))
)

val LightColors = AppColors(
    bg = Color(0xFFF4F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEBECF1),
    textPrimary = Color(0xFF0B0C10),
    textSecondary = Color(0xFF50546A),
    textMuted = Color(0xFF8A8EA0),
    accent = Color(0xFF0B0C10),
    onAccent = Color(0xFFFFFFFF),
    brand = Brand,
    error = Color(0xFFE5484D),
    skeleton = Color(0x0F000000),
    skeletonHighlight = Color(0x1F000000),
    glass = GlassTokens(tint = Color(0x66FFFFFF), border = Color(0x1A000000))
)

/** Hybrid rule: neutral accent + base surfaces stay as designed; the chosen AnimeTheme tints `brand`. */
fun appColorsFor(theme: AnimeTheme, dark: Boolean): AppColors =
    (if (dark) DarkColors else LightColors).copy(brand = theme.previewPrimary)

// ---------------- OLD: getThemeColors (now mode-aware, default = old dark behaviour) ----------------
fun getThemeColors(theme: AnimeTheme, dark: Boolean = true): ThemeColors =
    if (dark) darkThemeColors(theme) else lightThemeColors(theme)

private fun lightThemeColors(theme: AnimeTheme) = ThemeColors(
    bg = LightColors.bg,
    surface = LightColors.surface,
    surfaceElevated = LightColors.surfaceElevated,
    primary = theme.previewPrimary,
    secondary = theme.previewSecondary,
    accentGlow = theme.previewPrimary.copy(alpha = 0.25f),
    borderGlow = LightColors.glass.border,
    gradient = listOf(LightColors.bg, LightColors.bg, LightColors.bg)
)

private fun darkThemeColors(theme: AnimeTheme): ThemeColors {
    return when (theme) {
        AnimeTheme.ZENKAI_CRIMSON -> ThemeColors(
            bg = Color(0xFF080204),
            surface = Color(0xFF160508),
            surfaceElevated = Color(0xFF26080E),
            primary = Color(0xFFFF1E27),
            secondary = Color(0xFF8A0303),
            accentGlow = Color(0x66FF1E27),
            borderGlow = Color(0x40FF1E27),
            gradient = listOf(Color(0xFF080204), Color(0xFF2A060B), Color(0xFF080204))
        )
        AnimeTheme.GOJO_LIMITLESS -> ThemeColors(
            bg = Color(0xFF040711),
            surface = Color(0xFF0A0F1D),
            surfaceElevated = Color(0xFF111827),
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFFA855F7),
            accentGlow = Color(0x6638BDF8),
            borderGlow = Color(0x3338BDF8),
            gradient = listOf(Color(0xFF040711), Color(0xFF0B1226), Color(0xFF040711))
        )
        AnimeTheme.SUKUNA_MALEVOLENT -> ThemeColors(
            bg = Color(0xFF0A0406),
            surface = Color(0xFF17090D),
            surfaceElevated = Color(0xFF240E14),
            primary = Color(0xFFEF4444),
            secondary = Color(0xFFF97316),
            accentGlow = Color(0x66EF4444),
            borderGlow = Color(0x33EF4444),
            gradient = listOf(Color(0xFF0A0406), Color(0xFF1F090E), Color(0xFF0A0406))
        )
        AnimeTheme.SHADOW_MONARCH -> ThemeColors(
            bg = Color(0xFF06050D),
            surface = Color(0xFF100D22),
            surfaceElevated = Color(0xFF191433),
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF3B82F6),
            accentGlow = Color(0x668B5CF6),
            borderGlow = Color(0x338B5CF6),
            gradient = listOf(Color(0xFF06050D), Color(0xFF15102D), Color(0xFF06050D))
        )
        AnimeTheme.SUN_BREATHING -> ThemeColors(
            bg = Color(0xFF0D0604),
            surface = Color(0xFF1C0D09),
            surfaceElevated = Color(0xFF2B140D),
            primary = Color(0xFFF59E0B),
            secondary = Color(0xFFFB923C),
            accentGlow = Color(0x66F59E0B),
            borderGlow = Color(0x33F59E0B),
            gradient = listOf(Color(0xFF0D0604), Color(0xFF261109), Color(0xFF0D0604))
        )
        AnimeTheme.CYBERPUNK_EDGERUNNERS -> ThemeColors(
            bg = Color(0xFF05080E),
            surface = Color(0xFF0C131F),
            surfaceElevated = Color(0xFF141F33),
            primary = Color(0xFFFACC15),
            secondary = Color(0xFF06B6D4),
            accentGlow = Color(0x66FACC15),
            borderGlow = Color(0x33FACC15),
            gradient = listOf(Color(0xFF05080E), Color(0xFF121D2F), Color(0xFF05080E))
        )
        AnimeTheme.SURVEY_CORPS -> ThemeColors(
            bg = Color(0xFF050B08),
            surface = Color(0xFF0C1712),
            surfaceElevated = Color(0xFF14261E),
            primary = Color(0xFF10B981),
            secondary = Color(0xFFD97706),
            accentGlow = Color(0x6610B981),
            borderGlow = Color(0x3310B981),
            gradient = listOf(Color(0xFF050B08), Color(0xFF102118), Color(0xFF050B08))
        )
        AnimeTheme.OLED_STEALTH -> ThemeColors(
            bg = Color(0xFF000000),
            surface = Color(0xFF0B0B0C),
            surfaceElevated = Color(0xFF171719),
            primary = Color(0xFFE2E8F0),
            secondary = Color(0xFF94A3B8),
            accentGlow = Color(0x40FFFFFF),
            borderGlow = Color(0x22FFFFFF),
            gradient = listOf(Color(0xFF000000), Color(0xFF08080A), Color(0xFF000000))
        )
    }
}

// ---------------- Locals ----------------
val LocalAppColors = staticCompositionLocalOf { DarkColors }
val LocalIsDark = staticCompositionLocalOf { true }
val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }
val LocalAnimeTheme = compositionLocalOf { AnimeTheme.GOJO_LIMITLESS }
val LocalThemeColors = compositionLocalOf { getThemeColors(AnimeTheme.GOJO_LIMITLESS) }

@Composable
fun ThemeEngineProvider(settings: ThemeSettings, content: @Composable () -> Unit) {
    val dark = when (settings.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme() // auto-switches on system change
    }
    val colors = remember(settings.theme, dark) { appColorsFor(settings.theme, dark) }
    val legacy = remember(settings.theme, dark) { getThemeColors(settings.theme, dark) }
    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalAppColors provides colors,
        LocalAnimeTheme provides settings.theme,
        LocalThemeColors provides legacy,
        LocalThemeSettings provides settings,
        content = content
    )
}
