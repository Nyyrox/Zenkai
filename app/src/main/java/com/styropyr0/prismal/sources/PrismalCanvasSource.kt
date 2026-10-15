package com.styropyr0.prismal.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.styropyr0.prismal.PrismalBackdrop

/**
 * Remembers a backdrop that draws procedurally via [onDraw] each time it is sampled.
 *
 * Useful for solid fills, gradients, or custom canvas content without a layer capture.
 */
@Composable
fun rememberPrismalCanvasSource(
    onDraw: DrawScope.() -> Unit
): PrismalBackdrop {
    return remember(onDraw) {
        PrismalCanvasSource(onDraw)
    }
}

@Immutable
private class PrismalCanvasSource(
    val onDraw: DrawScope.() -> Unit
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw()
    }
}
