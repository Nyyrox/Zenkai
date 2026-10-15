package com.styropyr0.prismal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified

/** Default overlay alpha for [drawPrismalGlassTint] — keeps colored glass see-through. */
const val PrismalDefaultTintAlpha = 0.28f

/**
 * Draws a hue-preserving glass tint that stays translucent.
 *
 * Applies a [BlendMode.Hue] pass, then a low-alpha solid overlay. Prefer passing
 * [alpha] explicitly for “blue glass” / branded tints instead of baking opacity into
 * [tint] (the overlay alpha is what controls how opaque the color looks).
 */
fun DrawScope.drawPrismalGlassTint(
    tint: Color,
    alpha: Float = PrismalDefaultTintAlpha,
) {
    if (!tint.isSpecified) return
    drawRect(tint, blendMode = BlendMode.Hue)
    drawRect(tint.copy(alpha = alpha.coerceIn(0f, 1f)))
}
