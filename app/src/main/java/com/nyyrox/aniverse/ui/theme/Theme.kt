package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// Modern Dark + White Glassmorphic Design System
val OttBg = Color(0xFF040406) // Pure deep OLED dark
val OttSurface = Color(0x12FFFFFF) // Subtle frosted glass surface
val OttSurfaceElevated = Color(0x1EFFFFFF) // Elevated glass highlight
val OttSurfaceBorder = Color(0x24FFFFFF) // Crisp thin frosted white outline
val OttCardGlass = Color(0x14FFFFFF)

// Monochrome High-Contrast Accents
val OttPrimary = Color(0xFFFFFFFF) // Crisp, luxury pure white
val OttPrimaryGlow = Color(0xFFE2E4EB)
val OttSecondary = Color(0xFFD0D4E0) // Silver/White accent
val OttAccentGold = Color(0xFFFFD700) // AniList Score Star
val OttDarkPill = Color(0xFF101116)

val OttTextPrimary = Color(0xFFFFFFFF)
val OttTextSecondary = Color(0xFFA0A4B2)
val OttTextMuted = Color(0xFF6B6E7E)

val OttSkeletonDark = Color(0x10FFFFFF)
val OttSkeletonHighlight = Color(0x26FFFFFF)

// Blood-Red Liquid Accents
val OttBloodPrimary = Color(0xFFFF1E27) // Arterial vivid blood red
val OttBloodDark = Color(0xFF8A0303)    // Deep coagulated crimson
val OttBloodBg = Color(0xFF080204)      // Dark obsidian blood
val OttBloodSurface = Color(0xFF160508)
val OttBloodBorder = Color(0x50FF1E27)

// Unified design token aliases
val OttAccentRed = OttBloodPrimary          // Single accent red for all active/CTA states
val OttGlassIconBg = Color(0x1EFFFFFF)      // 12% white — glass icon button background  
val OttGlassIconBorder = Color(0x33FFFFFF)  // 20% white — glass icon button border

// Glass & Liquid Gradients
val OttWhiteGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFD4D8E2))
)

val OttLiquidPillGradient = Brush.horizontalGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFE8EAF0))
)

val OttBloodGradient = Brush.horizontalGradient(
    listOf(Color(0xFF8A0303), Color(0xFFFF1E27), Color(0xFFC4121A))
)

val OttBloodFlowGradient = Brush.linearGradient(
    listOf(
        Color(0xFF8A0303),
        Color(0xFFFF1E27),
        Color(0xFFC4121A),
        Color(0xFF5A0207),
        Color(0xFFFF2A32),
        Color(0xFF8A0303)
    )
)

val OttGlassGradient = Brush.verticalGradient(
    listOf(Color(0x20FFFFFF), Color(0x0AFFFFFF))
)

val OttHeroGradient = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.4f to Color(0x40040406),
    0.8f to Color(0xDD040406),
    1.0f to OttBg
)

val OttCardBottomGradient = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.5f to Color(0x80040406),
    1.0f to Color(0xF5040406)
)

val OttCardShape = RoundedCornerShape(16.dp)
val OttDialogShape = RoundedCornerShape(22.dp)
val OttPillShape = RoundedCornerShape(28.dp)
val OttButtonShape = RoundedCornerShape(14.dp)

private val OttDarkColorScheme = darkColorScheme(
    primary = OttPrimary,
    onPrimary = Color.Black,
    secondary = OttSecondary,
    onSecondary = Color.Black,
    background = OttBg,
    onBackground = OttTextPrimary,
    surface = Color(0xFF0C0D12),
    onSurface = OttTextPrimary,
    surfaceVariant = OttSurfaceElevated,
    onSurfaceVariant = OttTextSecondary,
    outline = OttSurfaceBorder
)

@Composable
fun AniverseTheme(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val currentDensity = LocalDensity.current

    // Baseline design screen width is 390dp (standard modern phone display).
    // Normalize density scale so text, icons, and components scale proportionally identical on low DPI, high DPI, small screens (e.g. 320dp) and large screens (e.g. 440dp).
    val screenWidth = configuration.screenWidthDp.toFloat()
    val densityMultiplier = if (screenWidth > 0f) {
        (screenWidth / 390f).coerceIn(0.85f, 1.15f)
    } else {
        1.0f
    }

    val normalizedDensity = Density(
        density = currentDensity.density * densityMultiplier,
        fontScale = 1.0f
    )

    CompositionLocalProvider(
        LocalDensity provides normalizedDensity
    ) {
        MaterialTheme(
            colorScheme = OttDarkColorScheme,
            typography = ZenkaiAppTypography,
            content = content
        )
    }
}
