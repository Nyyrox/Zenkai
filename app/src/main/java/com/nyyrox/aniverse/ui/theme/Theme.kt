package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Theme modes (Light / Dark / AMOLED) + card style.
// All Ott* colour tokens are getters that read Compose snapshot state, so every
// existing call site recomposes automatically when the mode changes.
// ─────────────────────────────────────────────────────────────────────────────
enum class ThemeMode(val title: String, val subtitle: String) {
    DARK("Dark", "Deep charcoal glass"),
    LIGHT("Light", "Bright frosted glass"),
    AMOLED("AMOLED", "Pure black, battery friendly")
}

object ThemeRuntime {
    /** Effective mode used by the Ott* tokens (immersive screens may force DARK). */
    var mode by mutableStateOf(ThemeMode.DARK)
    /** The mode the user picked in Settings. */
    var userMode: ThemeMode = ThemeMode.DARK
    /** true = boxed cards (surface + border); false = clean, card-less poster layout. */
    var cardStyle by mutableStateOf(false)
    val isLight: Boolean get() = mode == ThemeMode.LIGHT
}

private fun pick(dark: Color, light: Color, amoled: Color = dark): Color = when (ThemeRuntime.mode) {
    ThemeMode.DARK -> dark
    ThemeMode.LIGHT -> light
    ThemeMode.AMOLED -> amoled
}

/** "Ink" = the foreground tint: white on dark/AMOLED, near-black on light. Use for glass fills, dividers, borders. */
fun OttInk(alpha: Float): Color =
    (if (ThemeRuntime.isLight) Color(0xFF0F1220) else Color.White).copy(alpha = alpha)

val OttInkSolid: Color get() = if (ThemeRuntime.isLight) Color(0xFF0F1220) else Color.White

val OttBg: Color get() = pick(Color(0xFF0B0C11), Color(0xFFF1F2F7), Color(0xFF000000))
val OttSurface: Color get() = pick(Color(0x14FFFFFF), Color(0xCCFFFFFF), Color(0x0FFFFFFF))
val OttSurfaceElevated: Color get() = pick(Color(0x1FFFFFFF), Color(0xF2FFFFFF), Color(0x1AFFFFFF))
val OttSurfaceBorder: Color get() = pick(Color(0x26FFFFFF), Color(0x1F0F1220), Color(0x22FFFFFF))
val OttCardGlass: Color get() = pick(Color(0x14FFFFFF), Color(0xB3FFFFFF), Color(0x10FFFFFF))

// High-contrast accent: white on dark themes, ink on light
/** Solid panels (sheets, dialogs, settings groups): replaces hard-coded near-black fills. */
val OttPanel: Color get() = pick(Color(0xFF0D1017), Color(0xFFFFFFFF), Color(0xFF07080A))
val OttPanelHigh: Color get() = pick(Color(0xFF171B26), Color(0xFFE9EBF2), Color(0xFF111318))

val OttPrimary: Color get() = if (ThemeRuntime.isLight) Color(0xFF12141C) else Color(0xFFFFFFFF)
val OttOnPrimary: Color get() = if (ThemeRuntime.isLight) Color.White else Color.Black
val OttPrimaryGlow: Color get() = if (ThemeRuntime.isLight) Color(0xFF2A2E3D) else Color(0xFFE2E4EB)
val OttSecondary: Color get() = pick(Color(0xFFD0D4E0), Color(0xFF3B4052))
/** Text that sits ON artwork (posters, banners) - fixed colours that ignore the Light/Dark mode. */
val OttOnMedia = Color.White
val OttOnMediaDim = Color(0xFFC3C7D4)
val OttAccentGold = Color(0xFFFFD700) // AniList Score Star
val OttDarkPill: Color get() = pick(Color(0xFF101116), Color(0xFFE4E6EE), Color(0xFF08080A))

val OttTextPrimary: Color get() = pick(Color(0xFFFFFFFF), Color(0xFF0F1220))
val OttTextSecondary: Color get() = pick(Color(0xFFA0A4B2), Color(0xFF4A4F63))
val OttTextMuted: Color get() = pick(Color(0xFF6B6E7E), Color(0xFF7C8196))

val OttSkeletonDark: Color get() = pick(Color(0x10FFFFFF), Color(0x120F1220))
val OttSkeletonHighlight: Color get() = pick(Color(0x26FFFFFF), Color(0x240F1220))

// Blood-Red Liquid Accents
val OttBloodPrimary = Color(0xFFFF1E27) // Arterial vivid blood red
val OttBloodDark = Color(0xFF8A0303)    // Deep coagulated crimson
val OttBloodBg: Color get() = pick(Color(0xFF080204), Color(0xFFFFF1F2), Color(0xFF000000))
val OttBloodSurface: Color get() = pick(Color(0xFF160508), Color(0xFFFFE4E6), Color(0xFF0A0204))
val OttBloodBorder = Color(0x50FF1E27)

// Unified design token aliases
val OttAccentRed = OttBloodPrimary          // Single accent red for all active/CTA states
val OttGlassIconBg: Color get() = OttInk(0.12f)
val OttGlassIconBorder: Color get() = OttInk(0.20f)

// Glass & Liquid Gradients
val OttWhiteGradient: Brush get() = Brush.horizontalGradient(
    if (ThemeRuntime.isLight) listOf(Color(0xFF12141C), Color(0xFF2A2E3D)) else listOf(Color(0xFFFFFFFF), Color(0xFFD4D8E2))
)

val OttLiquidPillGradient: Brush get() = Brush.horizontalGradient(
    if (ThemeRuntime.isLight) listOf(Color(0xFF12141C), Color(0xFF2A2E3D)) else listOf(Color(0xFFFFFFFF), Color(0xFFE8EAF0))
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

val OttGlassGradient: Brush get() = Brush.verticalGradient(
    listOf(OttInk(if (ThemeRuntime.isLight) 0.55f else 0.125f), OttInk(if (ThemeRuntime.isLight) 0.25f else 0.04f))
)

val OttHeroGradient: Brush get() = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.4f to OttBg.copy(alpha = 0.25f),
    0.8f to OttBg.copy(alpha = 0.87f),
    1.0f to OttBg
)

/** Scrim that sits ON TOP of poster artwork. Stays dark in every mode so white text on posters stays readable. */
val OttCardBottomGradient = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.5f to Color(0x80040406),
    1.0f to Color(0xF5040406)
)

val OttCardShape = RoundedCornerShape(16.dp)
val OttDialogShape = RoundedCornerShape(22.dp)
val OttPillShape = RoundedCornerShape(28.dp)
val OttButtonShape = RoundedCornerShape(14.dp)

@Composable
private fun ottColorScheme(mode: ThemeMode) = when (mode) {
    ThemeMode.LIGHT -> lightColorScheme(
        primary = Color(0xFF12141C),
        onPrimary = Color.White,
        secondary = Color(0xFF3B4052),
        onSecondary = Color.White,
        background = Color(0xFFF1F2F7),
        onBackground = Color(0xFF0F1220),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F1220),
        surfaceVariant = Color(0xFFE6E8F0),
        onSurfaceVariant = Color(0xFF4A4F63),
        outline = Color(0x330F1220)
    )
    else -> darkColorScheme(
        primary = OttPrimary,
        onPrimary = Color.Black,
        secondary = OttSecondary,
        onSecondary = Color.Black,
        background = OttBg,
        onBackground = OttTextPrimary,
        surface = if (mode == ThemeMode.AMOLED) Color(0xFF050506) else Color(0xFF12131A),
        onSurface = OttTextPrimary,
        surfaceVariant = OttSurfaceElevated,
        onSurfaceVariant = OttTextSecondary,
        outline = OttSurfaceBorder
    )
}

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

    val view = androidx.compose.ui.platform.LocalView.current
    val lightBars = ThemeRuntime.isLight
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = lightBars
                controller.isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides normalizedDensity
    ) {
        MaterialTheme(
            colorScheme = ottColorScheme(ThemeRuntime.mode),
            typography = ZenkaiAppTypography,
            content = content
        )
    }
}

/**
 * Immersive screens (video player, readers, watch-together) are always dark so controls over
 * video / pages stay legible. Restores the user's chosen mode when they leave.
 */
@Composable
fun ImmersiveDark(content: @Composable () -> Unit) {
    if (ThemeRuntime.mode != ThemeMode.DARK && ThemeRuntime.mode != ThemeMode.AMOLED) ThemeRuntime.mode = ThemeMode.DARK
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { ThemeRuntime.mode = ThemeRuntime.userMode }
    }
    content()
}
