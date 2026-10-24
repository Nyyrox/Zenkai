package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val isReducedMotionEnabled: Boolean = false,
    val isPowerSavingEnabled: Boolean = false,
    val preferredRefreshRate: Int = 90,
    val isGlassmorphismEnabled: Boolean = true
)

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

val LocalAppColors = staticCompositionLocalOf { DarkColors }
val LocalIsDark = staticCompositionLocalOf { true }
val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }


// ---- Legacy shim: old screens read LocalThemeColors. Migrate to LocalAppColors, then delete. ----
@Deprecated("Use AppColors")
data class ThemeColors(
    val bg: Color, val surface: Color, val surfaceElevated: Color,
    val primary: Color, val secondary: Color,
    val accentGlow: Color, val borderGlow: Color, val gradient: List<Color>
)

@Suppress("DEPRECATION")
val LocalThemeColors = staticCompositionLocalOf {
    ThemeColors(DarkColors.bg, DarkColors.surface, DarkColors.surfaceElevated, DarkColors.accent,
        DarkColors.textSecondary, Color(0x40FFFFFF), DarkColors.glass.border, listOf(DarkColors.bg, DarkColors.bg))
}

@Suppress("DEPRECATION")
private fun AppColors.toLegacy() = ThemeColors(
    bg, surface, surfaceElevated, accent, textSecondary,
    accent.copy(alpha = 0.25f), glass.border, listOf(bg, bg, bg)
)

@Composable
fun ThemeEngineProvider(settings: ThemeSettings, content: @Composable () -> Unit) {
    val dark = when (settings.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme() // auto-switches on system change
    }
    val colors = if (dark) DarkColors else LightColors
    @Suppress("DEPRECATION")
    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalAppColors provides colors,
        LocalThemeColors provides colors.toLegacy(),
        LocalThemeSettings provides settings,
        content = content
    )
}
