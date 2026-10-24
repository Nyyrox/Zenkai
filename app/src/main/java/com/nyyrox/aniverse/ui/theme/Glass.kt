package com.nyyrox.aniverse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.styropyr0.prismal.components.PrismalGlassButton
import com.styropyr0.prismal.components.PrismalGlassToggle
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.PrismalGlass
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer

val LocalGlassBackdrop = staticCompositionLocalOf<PrismalBackdrop?> { null }

/** One per screen. `background` is what the glass samples (poster/hero/gradient). */
@Composable
fun GlassBackdropHost(
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val layer = rememberPrismalGlassLayer()
    val on = rememberGlassEnabled()
    Box(Modifier.fillMaxSize().background(LocalAppColors.current.bg)) {
        // Old devices: plain background, no layer recording, no glass cost.
        Box(Modifier.fillMaxSize().then(if (on) Modifier.prismalGlassLayer(layer) else Modifier)) { background() }
        CompositionLocalProvider(LocalGlassBackdrop provides if (on) layer else null) { content() }
    }
}

/** Liquid glass only on API 33+ (full AGSL tier) and when the user hasn't turned it off. */
@Composable
fun rememberGlassEnabled(): Boolean {
    val s = LocalThemeSettings.current
    return PrismalGlass.supportsRefraction && s.isGlassmorphismEnabled && !s.isPowerSavingEnabled
}

/** The only glass entry point. Falls back to a flat bordered surface when glass is off/unavailable. */
@Composable
fun Modifier.aniGlass(radius: Dp = 20.dp): Modifier {
    val c = LocalAppColors.current
    val on = rememberGlassEnabled()
    val backdrop = LocalGlassBackdrop.current
    val density = LocalDensity.current
    val flat = RoundedCornerShape(radius)
    val border = Modifier.border(0.5.dp, c.glass.border, flat)
    return if (backdrop == null || !on) {
        this.clip(flat).background(c.surface).then(border)
    } else {
        this.drawPrismalGlass(
            backdrop = backdrop,
            shape = { PrismalRoundedRectangle(radius) },
            effects = prismalGlassEffects(density),
            onDrawSurface = { drawRect(c.glass.tint) }
        ).then(border)
    }
}

/** Glass capsule button on API 33+, flat pill otherwise. `primary` = solid accent CTA (never glass). */
@Composable
fun AniButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
    content: @Composable () -> Unit
) {
    val c = LocalAppColors.current
    val backdrop = LocalGlassBackdrop.current
    if (!primary && backdrop != null && rememberGlassEnabled()) {
        PrismalGlassButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = modifier,
            contentPadding = contentPadding
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { content() }
        }
    } else {
        Row(
            modifier = modifier
                .clip(CircleShape)
                .background(if (primary) c.accent else c.surfaceElevated)
                .clickable(onClick = onClick)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }
}

@Composable
fun AniIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AniButton(onClick, modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
        Icon(icon, contentDescription, tint = LocalAppColors.current.textPrimary, modifier = Modifier.size(18.dp))
    }
}

/** Glass toggle on API 33+, neutral Material switch otherwise. */
@Composable
fun AniSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    val backdrop = LocalGlassBackdrop.current
    if (backdrop != null && rememberGlassEnabled()) {
        PrismalGlassToggle(selected = { checked }, onSelect = onCheckedChange, backdrop = backdrop)
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, checkedBorderColor = c.accent,
                uncheckedThumbColor = c.textMuted, uncheckedTrackColor = c.skeleton, uncheckedBorderColor = c.glass.border
            )
        )
    }
}
