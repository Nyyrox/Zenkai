package com.styropyr0.prismal

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

/**
 * A pixel source sampled by glass surfaces through [drawPrismalGlass].
 *
 * Implementations capture content behind or around a glass panel — for example a
 * [com.styropyr0.prismal.sources.PrismalGlassLayer] that records a composable subtree,
 * or a [com.styropyr0.prismal.sources.rememberPrismalCanvasSource] that draws procedurally.
 *
 * @see com.styropyr0.prismal.drawPrismalGlass
 */
interface PrismalBackdrop {

    /**
     * Whether drawing this source requires live [androidx.compose.ui.layout.LayoutCoordinates].
     *
     * Layer-backed sources return `true`; canvas and empty sources return `false`.
     */
    val isCoordinatesDependent: Boolean

    /**
     * Reads mutable state that affects sampling output.
     *
     * Glass draw nodes call this during observation so surfaces redraw once layer-backed
     * sources become positioned.
     */
    fun readSamplingState() {}

    /**
     * Draws the source content that will be refracted through a glass surface.
     *
     * @param density Current draw density.
     * @param coordinates Target glass coordinates, or `null` when not yet positioned.
     * @param layerBlock Optional inverse transform applied before sampling (e.g. press scale).
     */
    fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
