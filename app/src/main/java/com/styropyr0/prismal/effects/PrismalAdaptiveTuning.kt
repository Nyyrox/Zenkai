package com.styropyr0.prismal.effects

/**
 * Caps and scales for adaptive luminance color / blur remapping.
 *
 * When [com.styropyr0.prismal.effects.applyPrismalGlassEffects] runs with
 * `adaptiveLuminance = true`, [blurRadiusPx] is treated as the **base** blur:
 * bright scenes scale toward [maxBlurScale] × base, dark scenes toward
 * [minBlurScale] × base — the manual radius is never discarded.
 *
 * @param maxBrightness Peak brightness boost on bright backdrops (`luminance → 1`).
 * @param minBrightness Brightness floor on dark backdrops (`luminance → 0`).
 * @param midBrightness Brightness at neutral luminance (`0.5`).
 * @param minContrast Lowest contrast on bright backdrops (must stay above 0 so glass
 *   does not wash to solid white).
 * @param maxBlurScale Multiplier applied to base blur on bright backdrops.
 * @param minBlurScale Multiplier applied to base blur on dark backdrops.
 */
data class PrismalAdaptiveTuning(
    val maxBrightness: Float = 0.22f,
    val minBrightness: Float = -0.12f,
    val midBrightness: Float = 0.08f,
    val minContrast: Float = 0.75f,
    val maxBlurScale: Float = 2f,
    val minBlurScale: Float = 0.25f,
) {
    companion object {
        /** Default adaptive response — gentle enough for video / bright wallpapers. */
        val Standard = PrismalAdaptiveTuning()

        /** Smaller brightness swings and tighter blur range for subtle UI chrome. */
        val Subtle = PrismalAdaptiveTuning(
            maxBrightness = 0.12f,
            minBrightness = -0.06f,
            midBrightness = 0.05f,
            minContrast = 0.88f,
            maxBlurScale = 1.5f,
            minBlurScale = 0.5f,
        )
    }
}
