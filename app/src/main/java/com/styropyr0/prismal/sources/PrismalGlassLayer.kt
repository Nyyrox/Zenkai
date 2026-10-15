package com.styropyr0.prismal.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.internal.PrismalLayerTransformScope
import com.styropyr0.prismal.isAGSLShaderSupported

private val DefaultOnDraw: ContentDrawScope.() -> Unit = { drawContent() }

/**
 * Remembers a layer-backed [PrismalBackdrop] that records composable content via
 * [prismalGlassLayer].
 *
 * Attach the returned layer to background content, then pass it as the [backdrop] argument
 * to glass surfaces that should sample that content.
 *
 * @param graphicsLayer Layer used to capture content; defaults to a remembered layer.
 * @param onDraw Optional draw hook; defaults to [androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawContent].
 */
@Composable
fun rememberPrismalGlassLayer(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw
): PrismalGlassLayer {
    return remember(graphicsLayer, onDraw) {
        PrismalGlassLayer(graphicsLayer, onDraw)
    }
}

/**
 * Layer-backed [PrismalBackdrop] created by [rememberPrismalGlassLayer].
 *
 * Content is recorded into [graphicsLayer] by [prismalGlassLayer]. Glass surfaces sample
 * this layer with coordinate-aware offset correction.
 */
@Stable
class PrismalGlassLayer internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit
) : PrismalBackdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    private var contentVersion by mutableIntStateOf(0)

    private var inverseLayerScope: PrismalLayerTransformScope? = null

    /**
     * Signals glass surfaces sampling this layer that its content was re-recorded.
     *
     * Below API 33 the renderer can keep showing a stale copy of this layer inside a glass
     * surface's cached layer, so samplers re-record themselves when this changes.
     */
    internal fun notifyContentRecorded() {
        if (isAGSLShaderSupported()) return
        Snapshot.withoutReadObservation { contentVersion++ }
    }

    override fun readSamplingState() {
        layerCoordinates
    }

    override fun DrawScope.drawPrismalGlass(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        if (!isAGSLShaderSupported()) contentVersion
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates
        withTransform({
            if (layerBlock != null) {
                with(obtainPrismalLayerTransformScope()) { inverseTransform(density, layerBlock) }
            }
            if (layerCoordinates != null) {
                val offset =
                    try {
                        layerCoordinates.localPositionOf(coordinates)
                    } catch (_: Exception) {
                        // TODO: outer transformations lead to wrong position calculation
                        coordinates.positionInWindow() - layerCoordinates.positionInWindow()
                    }
                translate(-offset.x, -offset.y)
            }
        }) {
            drawLayer(graphicsLayer)
        }
    }

    private fun obtainPrismalLayerTransformScope(): PrismalLayerTransformScope {
        return inverseLayerScope?.apply { reset() }
            ?: PrismalLayerTransformScope().also { inverseLayerScope = it }
    }
}
