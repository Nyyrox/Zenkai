package com.styropyr0.prismal.depth

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.styropyr0.prismal.internal.PrismalShapeLayout
import com.styropyr0.prismal.internal.blur
import kotlin.math.ceil

internal class PrismalDepthShadowElement(
    val shapeProvider: PrismalShapeLayout,
    val shadow: () -> PrismalDepthShadow?
) : ModifierNodeElement<PrismalDepthShadowNode>() {

    override fun create(): PrismalDepthShadowNode {
        return PrismalDepthShadowNode(shapeProvider, shadow)
    }

    override fun update(node: PrismalDepthShadowNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shadow"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrismalDepthShadowElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (shadow != other.shadow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

internal class PrismalDepthShadowNode(
    var shapeProvider: PrismalShapeLayout,
    var shadow: () -> PrismalDepthShadow?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private var blurRadius = Float.NaN

    private var recordedShadow: PrismalDepthShadow? = null
    private var recordedOutline: Outline? = null
    private var recordedSize = Size.Unspecified
    private var recordedDensity = Float.NaN

    override fun ContentDrawScope.draw() {
        val shadow = shadow() ?: return drawContent()

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()
            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)

            shadowLayer.alpha = shadow.alpha
            shadowLayer.blendMode = shadow.blendMode

            val recordKey = shadow.copy(alpha = 1f, blendMode = DrawScope.DefaultBlendMode)
            if (
                recordKey != recordedShadow ||
                outline !== recordedOutline ||
                size != recordedSize ||
                density.density != recordedDensity
            ) {
                recordedShadow = recordKey
                recordedOutline = outline
                recordedSize = size
                recordedDensity = density.density

                val offsetX = shadow.offset.x.toPx()
                val offsetY = shadow.offset.y.toPx()
                val shadowSize = IntSize(
                    ceil(size.width + radius * 4f + offsetX).toInt(),
                    ceil(size.height + radius * 4f + offsetY).toInt()
                )

                configurePaint(shadow)

                shadowLayer.record(shadowSize) {
                    translate(radius * 2f + offsetX, radius * 2f + offsetY) {
                        val canvas = drawContext.canvas
                        canvas.drawOutline(outline, paint)
                        canvas.translate(-offsetX, -offsetY)
                        canvas.drawOutline(outline, PrismalDepthShadowMaskPaint)
                        canvas.translate(offsetX, offsetY)
                    }
                }
            }

            translate(-radius * 2f, -radius * 2f) {
                drawLayer(shadowLayer)
            }
        }

        drawContent()
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        shadowLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            shadowLayer = null
        }
        blurRadius = Float.NaN
        recordedShadow = null
        recordedOutline = null
        recordedSize = Size.Unspecified
        recordedDensity = Float.NaN
    }

    private fun DrawScope.configurePaint(shadow: PrismalDepthShadow) {
        paint.color = shadow.color
        val radius = shadow.radius.toPx()
        if (radius != blurRadius) {
            paint.blur(radius)
            blurRadius = radius
        }
    }
}

private val PrismalDepthShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
