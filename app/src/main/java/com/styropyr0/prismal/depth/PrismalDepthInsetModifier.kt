package com.styropyr0.prismal.depth

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import com.styropyr0.prismal.internal.PrismalShapeLayout
import com.styropyr0.prismal.internal.clipOutline
import com.styropyr0.prismal.isRenderEffectSupported

internal class PrismalDepthInsetElement(
    val shapeProvider: PrismalShapeLayout,
    val shadow: () -> PrismalDepthInset?
) : ModifierNodeElement<PrismalDepthInsetNode>() {

    override fun create(): PrismalDepthInsetNode {
        return PrismalDepthInsetNode(shapeProvider, shadow)
    }

    override fun update(node: PrismalDepthInsetNode) {
        node.shapeProvider = shapeProvider
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "prismalDepthInset"
        properties["shapeProvider"] = shapeProvider
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrismalDepthInsetElement) return false

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

internal class PrismalDepthInsetNode(
    var shapeProvider: PrismalShapeLayout,
    var shadow: () -> PrismalDepthInset?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null

    private val paint = Paint()
    private var clipPath: Path? = null

    private var prevRadius = Float.NaN

    private var recordedShadow: PrismalDepthInset? = null
    private var recordedOutline: Outline? = null
    private var recordedSize = Size.Unspecified
    private var recordedDensity = Float.NaN

    override fun ContentDrawScope.draw() {
        drawContent()

        if (!isRenderEffectSupported()) return

        val shadow = shadow() ?: return

        val shadowLayer = shadowLayer
        if (shadowLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val radius = shadow.radius.toPx()

            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)
            val clipPath =
                if (outline is Outline.Rounded) {
                    clipPath ?: Path().also { clipPath = it }
                } else {
                    null
                }

            shadowLayer.alpha = shadow.alpha
            shadowLayer.blendMode = shadow.blendMode
            if (prevRadius != radius) {
                shadowLayer.renderEffect =
                    if (radius > 0f) {
                        BlurEffect(radius, radius, TileMode.Decal)
                    } else {
                        null
                    }
                prevRadius = radius
            }

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

                configurePaint(shadow)

                shadowLayer.record {
                    val canvas = drawContext.canvas
                    canvas.save()
                    canvas.clipOutline(outline, clipPath)
                    canvas.drawOutline(outline, paint)
                    canvas.translate(offsetX, offsetY)
                    canvas.drawOutline(outline, PrismalDepthShadowMaskPaint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.restore()
                }
            }

            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipOutline(outline, clipPath)
            drawLayer(shadowLayer)
            canvas.restore()
        }
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
        prevRadius = Float.NaN
        clipPath = null
        recordedShadow = null
        recordedOutline = null
        recordedSize = Size.Unspecified
        recordedDensity = Float.NaN
    }

    private fun DrawScope.configurePaint(shadow: PrismalDepthInset) {
        paint.color = shadow.color
    }
}

private val PrismalDepthShadowMaskPaint = Paint().apply {
    blendMode = BlendMode.Clear
}
