package com.styropyr0.prismal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Receiver scope for building a chained [androidx.compose.ui.graphics.RenderEffect] pipeline
 * inside [drawPrismalGlass] and [drawPlainPrismalGlass].
 *
 * Use helpers such as [com.styropyr0.prismal.effects.prismalBlur],
 * [com.styropyr0.prismal.effects.prismalLens], and
 * [com.styropyr0.prismal.effects.applyPrismalGlassEffects] to compose effects.
 * The accumulated [renderEffect] is applied to the sampled backdrop layer.
 */
sealed interface PrismalGlassEffectProvider : Density, PrismalShaderCache {

    /** Size of the glass surface being drawn. */
    val size: Size

    /** Layout direction of the glass surface. */
    val layoutDirection: LayoutDirection

    /** Clip shape of the glass surface (used by refraction shaders). */
    val shape: Shape

    /**
     * Extra padding around the sampled backdrop, expanded automatically by blur radius
     * and refraction height so edge effects are not clipped.
     */
    var padding: Float

    /** Chained render effect applied to the backdrop sample. */
    var renderEffect: RenderEffect?

    /**
     * Frosted overlay strength in `[0, 1]` used on [PrismalGlassPipeline.Legacy] devices when
     * backdrop blur is unavailable.
     */
    var legacyFrostStrength: Float
}

internal abstract class PrismalGlassEffectScopeImpl : PrismalGlassEffectProvider, PrismalShaderCache {

    override var density: Float = 1f
    override var fontScale: Float = 1f
    override var size: Size = Size.Unspecified
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var padding: Float = 0f
    override var renderEffect: RenderEffect? = null
    override var legacyFrostStrength: Float = 0f

    private val runtimeShaderCache = PrismalShaderCacheImpl()

    override fun obtainAGSLShader(key: String, string: String): PrismalShader {
        return runtimeShaderCache.obtainAGSLShader(key, string)
    }

    fun update(scope: DrawScope): Boolean {
        val newDensity = scope.density
        val newFontScale = scope.fontScale
        val newSize = scope.size
        val newLayoutDirection = scope.layoutDirection

        val changed = newDensity != density ||
                newFontScale != fontScale ||
                newSize != size ||
                newLayoutDirection != layoutDirection

        if (changed) {
            density = newDensity
            fontScale = newFontScale
            size = newSize
            layoutDirection = newLayoutDirection
        }

        return changed
    }

    fun apply(effects: PrismalGlassEffectProvider.() -> Unit) {
        padding = 0f
        renderEffect = null
        legacyFrostStrength = 0f
        effects()
    }

    fun reset() {
        density = 1f
        fontScale = 1f
        size = Size.Unspecified
        layoutDirection = LayoutDirection.Ltr
        padding = 0f
        renderEffect = null
        legacyFrostStrength = 0f
        runtimeShaderCache.clear()
    }
}
