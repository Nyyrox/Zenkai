package com.styropyr0.prismal.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.styropyr0.prismal.sources.PrismalGlassLayer

/**
 * Glass layer exported by an ancestor surface (via [com.styropyr0.prismal.drawPrismalGlass]
 * `nestedGlassSource`).
 *
 * Nested controls such as [PrismalGlassSlider] and [PrismalGlassToggle] sample this instead of
 * a screen-level backdrop so their thumbs refract the parent glass panel they sit on.
 */
val LocalPrismalParentGlassLayer = staticCompositionLocalOf<PrismalGlassLayer?> { null }
