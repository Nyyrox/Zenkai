package com.styropyr0.prismal.effects

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import com.styropyr0.prismal.shapes.PrismalRoundedRectangularShape
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.internal.PrismalShaderEffect
import com.styropyr0.prismal.internal.PrismalRectRefractionShader
import com.styropyr0.prismal.internal.PrismalRectRefractionDispersionShader
import com.styropyr0.prismal.isAGSLShaderSupported

/**
 * Rounded-rectangle lens refraction applied to the backdrop sample.
 *
 * Requires a [com.styropyr0.prismal.shapes.PrismalRoundedRectangularShape] or
 * [androidx.compose.foundation.shape.CornerBasedShape] clip shape.
 *
 * @param refractionHeight Height of the curved refraction zone at the shape edge.
 * @param refractionAmount Horizontal displacement strength at the edge.
 * @param depthEffect When true, refraction follows surface normals for a deeper lens look.
 * @param chromaticAberration Dispersion strength in `[0, 1]`; splits RGB channels when above zero.
 */
fun PrismalGlassEffectProvider.prismalLens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0) chromaticAberration: Float = 0f
) {
    if (!isAGSLShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (!size.isSpecified) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).fastCoerceAtLeast(0f)
    }

    val cornerRadii = cornerRadii
    val effect =
        if (cornerRadii != null) {
            val useDispersion = chromaticAberration > 0f
            val shader =
                if (!useDispersion) {
                    obtainAGSLShader(
                        "Refraction",
                        PrismalRectRefractionShader
                    )
                } else {
                    obtainAGSLShader(
                        "RefractionWithDispersion",
                        PrismalRectRefractionDispersionShader
                    )
                }
            shader.apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("offset", -padding, -padding)
                setFloatUniform("cornerRadii", cornerRadii)
                setFloatUniform("refractionHeight", refractionHeight)
                setFloatUniform("refractionAmount", -refractionAmount)
                setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
                if (useDispersion) {
                    setFloatUniform("chromaticAberration", chromaticAberration)
                }
            }
            PrismalShaderEffect(shader, "content")
        } else {
            throwUnsupportedSDFException()
        }
    effect(effect)
}

private val PrismalGlassEffectProvider.cornerRadii: FloatArray?
    get() {
        if (!size.isSpecified) return null
        return when (val shape = shape) {
        is PrismalRoundedRectangularShape -> {
            val corners = shape.corners(size, layoutDirection, this)
            floatArrayOf(
                corners.topLeft,
                corners.topRight,
                corners.bottomRight,
                corners.bottomLeft
            )
        }

        is AbsoluteRoundedCornerShape -> {
            val size = size
            val maxRadius = size.minDimension / 2f
            val topLeft = shape.topStart.toPx(size, this)
            val topRight = shape.topEnd.toPx(size, this)
            val bottomRight = shape.bottomEnd.toPx(size, this)
            val bottomLeft = shape.bottomStart.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }

        is CornerBasedShape -> {
            val size = size
            val maxRadius = size.minDimension / 2f
            val isLtr = layoutDirection == LayoutDirection.Ltr
            val topLeft =
                if (isLtr) shape.topStart.toPx(size, this)
                else shape.topEnd.toPx(size, this)
            val topRight =
                if (isLtr) shape.topEnd.toPx(size, this)
                else shape.topStart.toPx(size, this)
            val bottomRight =
                if (isLtr) shape.bottomEnd.toPx(size, this)
                else shape.bottomStart.toPx(size, this)
            val bottomLeft =
                if (isLtr) shape.bottomStart.toPx(size, this)
                else shape.bottomEnd.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }

        else -> null
        }
    }

private fun throwUnsupportedSDFException(): Nothing {
    throw UnsupportedOperationException(
        "Only PrismalRoundedRectangularShape or CornerBasedShape is supported in prismalLens effects."
    )
}
