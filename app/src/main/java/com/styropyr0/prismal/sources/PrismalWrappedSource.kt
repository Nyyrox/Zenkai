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
 * Wraps [backdrop] and intercepts sampling via [onDraw].
 *
 * The lambda receives a `drawPrismalGlass` callback to invoke the inner source — useful
 * for masking, clipping, or compositing multiple glass layers (e.g. slider track + thumb).
 */
@Composable
fun rememberPrismalWrappedSource(
    backdrop: PrismalBackdrop,
    onDraw: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit
): PrismalBackdrop {
    return remember(backdrop, onDraw) {
        PrismalWrappedSource(backdrop, onDraw)
    }
}

@Immutable
private class PrismalWrappedSource(
    val backdrop: PrismalBackdrop,
    val onDraw: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean = backdrop.isCoordinatesDependent

    override fun readSamplingState() {
        backdrop.readSamplingState()
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw { with(backdrop) { drawPrismalGlass(density, coordinates, layerBlock) } }
    }
}
