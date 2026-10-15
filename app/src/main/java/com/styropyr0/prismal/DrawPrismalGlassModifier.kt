package com.styropyr0.prismal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.styropyr0.prismal.internal.capturePrismalLayer
import com.styropyr0.prismal.sources.PrismalGlassLayer
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.specular.PrismalSpecularElement
import com.styropyr0.prismal.internal.PrismalShapeLayout
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthInsetElement
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.depth.PrismalDepthShadowElement

private val DefaultSpecular = { PrismalSpecular.Default }
private val DefaultDepthShadow = { PrismalDepthShadow.Default }
private val DefaultOnDrawGlassLayer: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

/**
 * Draws a glass surface without specular highlights, depth shadow, or depth inset.
 *
 * Samples [backdrop] through [effects], clips to [shape], and optionally layers custom
 * drawing via [onDrawBehind], [onDrawSurface], and [onDrawFront].
 *
 * @param backdrop Content sampled and refracted through the glass.
 * @param shape Clip shape; should be a [com.styropyr0.prismal.shapes.PrismalRoundedRectangularShape]
 *   when using [com.styropyr0.prismal.effects.prismalLens].
 * @param effects RenderEffect chain (blur, lens, color controls, etc.).
 * @param layerBlock Optional [graphicsLayer] transform applied before sampling.
 * @param nestedGlassSource When set, exports the processed glass layer for nested surfaces.
 * @param onDrawBehind Drawn beneath the refracted backdrop.
 * @param onDrawGlassLayer Hook to wrap or replace backdrop sampling.
 * @param onDrawSurface Drawn on top of the refracted layer (tints, fills).
 * @param onDrawFront Drawn above everything else.
 */
fun Modifier.drawPlainPrismalGlass(
    backdrop: PrismalBackdrop,
    shape: () -> Shape,
    effects: PrismalGlassEffectProvider.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    nestedGlassSource: PrismalGlassLayer? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawGlassLayer: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit = DefaultOnDrawGlassLayer,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier {
    val shapeProvider = PrismalShapeLayout(shape)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            DrawPrismalGlassElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                nestedGlassSource = nestedGlassSource,
                onDrawBehind = onDrawBehind,
                onDrawGlassLayer = onDrawGlassLayer,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront
            )
        )
}

/**
 * Draws a full glass surface with optional specular edge, depth shadow, and depth inset.
 *
 * This is the primary glass modifier. It extends [drawPlainPrismalGlass] with decorative
 * layers controlled by [specular], [depthShadow], and [depthInset].
 *
 * @param specular Edge highlight style; pass `null` to disable. Defaults to [PrismalSpecular.Default].
 * @param depthShadow Drop shadow beneath the glass shape; pass `null` to disable.
 * @param depthInset Inner shadow / inset shading; pass `null` to disable.
 * @see drawPlainPrismalGlass for remaining parameters.
 */
fun Modifier.drawPrismalGlass(
    backdrop: PrismalBackdrop,
    shape: () -> Shape,
    effects: PrismalGlassEffectProvider.() -> Unit,
    specular: (() -> PrismalSpecular?)? = DefaultSpecular,
    depthShadow: (() -> PrismalDepthShadow?)? = DefaultDepthShadow,
    depthInset: (() -> PrismalDepthInset?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    nestedGlassSource: PrismalGlassLayer? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawGlassLayer: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit = DefaultOnDrawGlassLayer,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier {
    val shapeProvider = PrismalShapeLayout(shape)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            if (depthInset != null) {
                PrismalDepthInsetElement(
                    shapeProvider = shapeProvider,
                    shadow = depthInset
                )
            } else {
                Modifier
            }
        )
        .then(
            if (depthShadow != null) {
                PrismalDepthShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = depthShadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (specular != null) {
                PrismalSpecularElement(
                    shapeProvider = shapeProvider,
                    specular = specular
                )
            } else {
                Modifier
            }
        )
        .then(
            DrawPrismalGlassElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                nestedGlassSource = nestedGlassSource,
                onDrawBehind = onDrawBehind,
                onDrawGlassLayer = onDrawGlassLayer,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront
            )
        )
}

private class DrawPrismalGlassElement(
    val backdrop: PrismalBackdrop,
    val shapeProvider: PrismalShapeLayout,
    val effects: PrismalGlassEffectProvider.() -> Unit,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val nestedGlassSource: PrismalGlassLayer?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawGlassLayer: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?
) : ModifierNodeElement<DrawPrismalGlassNode>() {

    override fun create(): DrawPrismalGlassNode {
        return DrawPrismalGlassNode(
            backdrop = backdrop,
            shapeProvider = shapeProvider,
            effects = effects,
            layerBlock = layerBlock,
            nestedGlassSource = nestedGlassSource,
            onDrawBehind = onDrawBehind,
            onDrawGlassLayer = onDrawGlassLayer,
            onDrawSurface = onDrawSurface,
            onDrawFront = onDrawFront
        )
    }

    override fun update(node: DrawPrismalGlassNode) {
        node.backdrop = backdrop
        node.shapeProvider = shapeProvider
        node.effects = effects
        node.layerBlock = layerBlock
        if (node.nestedGlassSource != nestedGlassSource) {
            node.nestedGlassSource?.layerCoordinates = null
            node.nestedGlassSource = nestedGlassSource
        }
        node.onDrawBehind = onDrawBehind
        node.onDrawGlassLayer = onDrawGlassLayer
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawPrismalGlass"
        properties["backdrop"] = backdrop
        properties["shapeProvider"] = shapeProvider
        properties["effects"] = effects
        properties["layerBlock"] = layerBlock
        properties["nestedGlassSource"] = nestedGlassSource
        properties["onDrawBehind"] = onDrawBehind
        properties["onDrawGlassLayer"] = onDrawGlassLayer
        properties["onDrawSurface"] = onDrawSurface
        properties["onDrawFront"] = onDrawFront
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawPrismalGlassElement) return false

        if (backdrop != other.backdrop) return false
        if (shapeProvider != other.shapeProvider) return false
        if (effects != other.effects) return false
        if (layerBlock != other.layerBlock) return false
        if (nestedGlassSource != other.nestedGlassSource) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawGlassLayer != other.onDrawGlassLayer) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (onDrawFront != other.onDrawFront) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shapeProvider.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (nestedGlassSource?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + onDrawGlassLayer.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + (onDrawFront?.hashCode() ?: 0)
        return result
    }
}

private class DrawPrismalGlassNode(
    var backdrop: PrismalBackdrop,
    var shapeProvider: PrismalShapeLayout,
    var effects: PrismalGlassEffectProvider.() -> Unit,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var nestedGlassSource: PrismalGlassLayer?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawGlassLayer: DrawScope.(drawPrismalGlass: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?
) : LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node() {

    private val effectScope =
        object : PrismalGlassEffectScopeImpl() {

            override val shape: Shape get() = shapeProvider.innerShape
        }

    private var graphicsLayer: GraphicsLayer? = null

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = if (isRenderEffectSupported()) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }

    private var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    private var padding by mutableFloatStateOf(0f)

    private val recordBackdropBlock: (DrawScope.() -> Unit) = {
        val canvas = drawContext.canvas
        val padding = padding

        if (padding != 0f) {
            canvas.translate(padding, padding)
        }
        onDrawGlassLayer {
            with(backdrop) {
                drawPrismalGlass(
                    density = effectScope,
                    coordinates = layoutCoordinates,
                    layerBlock = layerBlock
                )
            }
        }
        if (padding != 0f) {
            canvas.translate(-padding, -padding)
        }
    }

    private val recordPrismalGlassLayer: DrawScope.() -> Unit = {
        val layer = graphicsLayer
        if (layer != null) {
            val padding = padding

            capturePrismalLayer(
                node = this@DrawPrismalGlassNode,
                layer = layer,
                size = IntSize(
                    size.width.toInt() + padding.toInt() * 2,
                    size.height.toInt() + padding.toInt() * 2
                ),
                block = recordBackdropBlock
            )

            layer.topLeft =
                if (padding != 0f) IntOffset(-padding.toInt(), -padding.toInt())
                else IntOffset.Zero
        }
    }

    private val drawPrismalGlassLayer: DrawScope.() -> Unit = {
        graphicsLayer?.let { drawLayer(it) }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (effectScope.update(this)) {
            observeEffects()
        }

        onDrawBehind?.invoke(this)
        recordPrismalGlassLayer()
        drawPrismalGlassLayer()
        if (!isRenderEffectSupported() && effectScope.legacyFrostStrength > 0f) {
            drawRect(Color.White.copy(alpha = effectScope.legacyFrostStrength))
        }
        onDrawSurface?.invoke(this)
        drawContent()
        onDrawFront?.invoke(this)

        nestedGlassSource?.let { source ->
            capturePrismalLayer(node = this@DrawPrismalGlassNode, layer = source.graphicsLayer) {
                onDrawBehind?.invoke(this)
                drawPrismalGlassLayer()
                onDrawSurface?.invoke(this)
                onDrawFront?.invoke(this)
            }
            source.notifyContentRecorded()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (backdrop.isCoordinatesDependent) {
                layoutCoordinates = coordinates
            } else {
                if (layoutCoordinates != null) {
                    layoutCoordinates = null
                }
            }
            nestedGlassSource?.layerCoordinates = coordinates
        }
    }

    override fun onObservedReadsChanged() {
        invalidateDraw()
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        observeEffects()
    }

    private fun observeEffects() {
        observeReads {
            backdrop.readSamplingState()
            updateEffects()
        }
    }

    private fun updateEffects() {
        if (!effectScope.size.isSpecified) return

        effectScope.apply(effects)
        padding = effectScope.padding
        if (isRenderEffectSupported()) {
            graphicsLayer?.renderEffect = effectScope.renderEffect
        } else {
            graphicsLayer?.renderEffect = null
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer = graphicsContext.createGraphicsLayer()

        observeEffects()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }

        effectScope.reset()
        layoutCoordinates = null
        nestedGlassSource?.layerCoordinates = null
    }
}
