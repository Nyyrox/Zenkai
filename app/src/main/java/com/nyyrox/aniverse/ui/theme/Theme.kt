package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

val OttCardShape = RoundedCornerShape(16.dp)
val OttDialogShape = RoundedCornerShape(22.dp)
val OttPillShape = RoundedCornerShape(28.dp)
val OttButtonShape = RoundedCornerShape(14.dp)

// ---- Legacy dark-only constants: migrate screens to LocalAppColors, then delete. ----
@Deprecated("Use LocalAppColors.current.bg") val OttBg = Color(0xFF040406)
@Deprecated("Use LocalAppColors.current.glass.tint") val OttSurface = Color(0x12FFFFFF)
@Deprecated("Use LocalAppColors.current.surfaceElevated") val OttSurfaceElevated = Color(0x1EFFFFFF)
@Deprecated("Use LocalAppColors.current.glass.border") val OttSurfaceBorder = Color(0x24FFFFFF)
@Deprecated("Use Modifier.aniGlass()") val OttCardGlass = Color(0x14FFFFFF)
@Deprecated("Use LocalAppColors.current.accent") val OttPrimary = Color(0xFFFFFFFF)
@Deprecated("Use LocalAppColors.current.accent") val OttAccentRed = Color(0xFFFFFFFF) // red is brand-only now
@Deprecated("Use StarGold") val OttAccentGold = StarGold
@Deprecated("Use LocalAppColors.current.textPrimary") val OttTextPrimary = Color(0xFFFFFFFF)
@Deprecated("Use LocalAppColors.current.textSecondary") val OttTextSecondary = Color(0xFFA0A4B2)
@Deprecated("Use LocalAppColors.current.textMuted") val OttTextMuted = Color(0xFF6B6E7E)
@Deprecated("Use LocalAppColors.current.skeleton") val OttSkeletonDark = Color(0x10FFFFFF)
@Deprecated("Use LocalAppColors.current.skeletonHighlight") val OttSkeletonHighlight = Color(0x26FFFFFF)
@Deprecated("Use LocalAppColors.current.textSecondary") val OttSecondary = Color(0xFFD0D4E0)
@Deprecated("Use LocalAppColors.current.accent") val OttPrimaryGlow = Color(0xFFE2E4EB)
@Deprecated("Use LocalAppColors.current.surface") val OttDarkPill = Color(0xFF101116)
@Deprecated("Use Modifier.aniGlass()") val OttGlassIconBg = Color(0x1EFFFFFF)
@Deprecated("Use LocalAppColors.current.glass.border") val OttGlassIconBorder = Color(0x33FFFFFF)
@Deprecated("Use LocalAppColors.current.accent") val OttWhiteGradient = Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD4D8E2)))
@Deprecated("Use LocalAppColors.current.accent") val OttLiquidPillGradient = Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE8EAF0)))
@Deprecated("Use Modifier.aniGlass()") val OttGlassGradient = Brush.verticalGradient(listOf(Color(0x20FFFFFF), Color(0x0AFFFFFF)))
@Deprecated("Use LocalAppColors.current.heroGradient")
val OttHeroGradient = Brush.verticalGradient(
    0.0f to Color.Transparent, 0.4f to Color(0x40040406), 0.8f to Color(0xDD040406), 1.0f to Color(0xFF040406)
)
@Deprecated("Use LocalAppColors.current.cardBottomGradient")
val OttCardBottomGradient = Brush.verticalGradient(
    0.0f to Color.Transparent, 0.5f to Color(0x80040406), 1.0f to Color(0xF5040406)
)

// ---- Old "blood" palette, restored for unmigrated screens (BrowseScreen etc.). Real brand red = LocalAppColors.current.brand. ----
@Deprecated("Use LocalAppColors.current.brand") val OttBloodPrimary = Color(0xFFFF1E27) // Arterial vivid blood red
@Deprecated("Use LocalAppColors.current.brand") val OttBloodDark = Color(0xFF8A0303)    // Deep coagulated crimson
@Deprecated("Use LocalAppColors.current.bg") val OttBloodBg = Color(0xFF080204)         // Dark obsidian blood
@Deprecated("Use LocalAppColors.current.surface") val OttBloodSurface = Color(0xFF160508)
@Deprecated("Use LocalAppColors.current.glass.border") val OttBloodBorder = Color(0x50FF1E27)
@Deprecated("Use LocalAppColors.current.brand")
val OttBloodGradient = Brush.horizontalGradient(listOf(Color(0xFF8A0303), Color(0xFFFF1E27), Color(0xFFC4121A)))
@Deprecated("Use LocalAppColors.current.brand")
val OttBloodFlowGradient = Brush.linearGradient(
    listOf(
        Color(0xFF8A0303), Color(0xFFFF1E27), Color(0xFFC4121A),
        Color(0xFF5A0207), Color(0xFFFF2A32), Color(0xFF8A0303)
    )
)

@Composable
private fun materialScheme(): ColorScheme {
    val c = LocalAppColors.current
    return if (LocalIsDark.current) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent,
        secondary = c.textSecondary, onSecondary = c.onAccent,
        background = c.bg, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceElevated, onSurfaceVariant = c.textSecondary,
        outline = c.glass.border, error = c.error
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent,
        secondary = c.textSecondary, onSecondary = c.onAccent,
        background = c.bg, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceElevated, onSurfaceVariant = c.textSecondary,
        outline = c.glass.border, error = c.error
    )
}

@Composable
fun AniverseTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val currentDensity = LocalDensity.current

    // Baseline 390dp: scale text/icons/components identically across DPI and screen sizes.
    val screenWidth = configuration.screenWidthDp.toFloat()
    val densityMultiplier = if (screenWidth > 0f) (screenWidth / 390f).coerceIn(0.85f, 1.15f) else 1.0f
    val normalizedDensity = Density(density = currentDensity.density * densityMultiplier, fontScale = 1.0f)

    CompositionLocalProvider(LocalDensity provides normalizedDensity) {
        ThemeEngineProvider(settings) {
            MaterialTheme(colorScheme = materialScheme(), typography = ZenkaiAppTypography, content = content)
        }
    }
}
