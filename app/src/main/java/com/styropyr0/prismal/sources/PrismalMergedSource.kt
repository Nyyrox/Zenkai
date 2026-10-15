package com.styropyr0.prismal.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.styropyr0.prismal.PrismalBackdrop

/** Combines two backdrop sources into one draw call. */
@Composable
fun rememberPrismalMergedSource(
    backdrop1: PrismalBackdrop,
    backdrop2: PrismalBackdrop
): PrismalBackdrop {
    return remember(backdrop1, backdrop2) {
        Combined2Backdrops(backdrop1, backdrop2)
    }
}

/** Combines three backdrop sources into one draw call. */
@Composable
fun rememberPrismalMergedSource(
    backdrop1: PrismalBackdrop,
    backdrop2: PrismalBackdrop,
    backdrop3: PrismalBackdrop
): PrismalBackdrop {
    return remember(backdrop1, backdrop2, backdrop3) {
        Combined3Backdrops(backdrop1, backdrop2, backdrop3)
    }
}

/** Combines any number of backdrop sources into one draw call. */
@Composable
fun rememberPrismalMergedSource(vararg sources: PrismalBackdrop): PrismalBackdrop {
    return remember(*sources) {
        PrismalMergedSources(*sources)
    }
}

@Immutable
private class Combined2Backdrops(
    val backdrop1: PrismalBackdrop,
    val backdrop2: PrismalBackdrop
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean =
        backdrop1.isCoordinatesDependent || backdrop2.isCoordinatesDependent

    override fun readSamplingState() {
        backdrop1.readSamplingState()
        backdrop2.readSamplingState()
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        with(backdrop1) { drawPrismalGlass(density, coordinates, layerBlock) }
        with(backdrop2) { drawPrismalGlass(density, coordinates, layerBlock) }
    }
}

@Immutable
private class Combined3Backdrops(
    val backdrop1: PrismalBackdrop,
    val backdrop2: PrismalBackdrop,
    val backdrop3: PrismalBackdrop
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean =
        backdrop1.isCoordinatesDependent ||
                backdrop2.isCoordinatesDependent ||
                backdrop3.isCoordinatesDependent

    override fun readSamplingState() {
        backdrop1.readSamplingState()
        backdrop2.readSamplingState()
        backdrop3.readSamplingState()
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        with(backdrop1) { drawPrismalGlass(density, coordinates, layerBlock) }
        with(backdrop2) { drawPrismalGlass(density, coordinates, layerBlock) }
        with(backdrop3) { drawPrismalGlass(density, coordinates, layerBlock) }
    }
}

@Immutable
private class PrismalMergedSources(
    vararg val sources: PrismalBackdrop
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean =
        sources.any { it.isCoordinatesDependent }

    override fun readSamplingState() {
        sources.forEach { it.readSamplingState() }
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        sources.forEach { source ->
            with(source) { drawPrismalGlass(density, coordinates, layerBlock) }
        }
    }
}
