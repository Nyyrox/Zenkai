package com.styropyr0.prismal.sources

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.styropyr0.prismal.internal.capturePrismalLayer

/**
 * Records this composable's drawn content into [backdrop] so glass surfaces can sample it.
 *
 * Apply to background layers (images, scroll content, etc.) before drawing glass on top.
 */
fun Modifier.prismalGlassLayer(backdrop: PrismalGlassLayer): Modifier = this then PrismalGlassLayerElement(backdrop)

private class PrismalGlassLayerElement(
    val backdrop: PrismalGlassLayer
) : ModifierNodeElement<PrismalGlassLayerNode>() {

    override fun create(): PrismalGlassLayerNode {
        return PrismalGlassLayerNode(backdrop)
    }

    override fun update(node: PrismalGlassLayerNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "prismalGlassLayer"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrismalGlassLayerElement) return false

        if (backdrop != other.backdrop) return false

        return true
    }

    override fun hashCode(): Int {
        return backdrop.hashCode()
    }
}

private class PrismalGlassLayerNode(
    var backdrop: PrismalGlassLayer
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw() {
        drawContent()
        capturePrismalLayer(node = this, layer = backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
        backdrop.notifyContentRecorded()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val wasUnset = backdrop.layerCoordinates == null
            backdrop.layerCoordinates = coordinates
            if (wasUnset) {
                invalidateDraw()
            }
        }
    }

    override fun onDetach() {
        backdrop.layerCoordinates = null
    }
}
