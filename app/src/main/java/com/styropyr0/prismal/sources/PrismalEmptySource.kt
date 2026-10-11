package com.styropyr0.prismal.sources

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.styropyr0.prismal.PrismalBackdrop

/** Returns a backdrop that draws nothing — a transparent sample. */
@Stable
fun emptyGlassSource(): PrismalBackdrop = PrismalEmptySource

@Immutable
private object PrismalEmptySource : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
    }
}
