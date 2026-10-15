package com.styropyr0.prismal.specular

import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.styropyr0.prismal.PrismalShader
import com.styropyr0.prismal.PrismalShaderCache
import com.styropyr0.prismal.internal.PrismalAmbientSpecularShader
import com.styropyr0.prismal.internal.PrismalDefaultSpecularShader
import com.styropyr0.prismal.isAGSLShaderSupported
import com.styropyr0.prismal.shapes.PrismalRoundedRectangularShape
import kotlin.math.PI

/** Rendering style for [PrismalSpecular] edge highlights. */
@Immutable
interface PrismalSpecularStyle {

    val color: Color

    val blendMode: BlendMode

    fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: PrismalShaderCache
    ): PrismalShader?

    @Immutable
    data class Plain(
        override val color: Color = Color.White.copy(alpha = 0.38f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : PrismalSpecularStyle {

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: PrismalShaderCache
        ): PrismalShader? = null
    }

    @Immutable
    data class Default(
        override val color: Color = Color.White.copy(alpha = 0.5f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : PrismalSpecularStyle {

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: PrismalShaderCache
        ): PrismalShader? {
            return if (isAGSLShaderSupported()) {
                runtimeShaderCache.obtainAGSLShader(
                    "Default",
                    PrismalDefaultSpecularShader
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setColorUniform("color", color.copy(alpha = 1f))
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
            } else {
                null
            }
        }
    }

    @Immutable
    data class Ambient(
        @param:FloatRange(from = 0.0, to = 1.0) val intensity: Float = 0.38f
    ) : PrismalSpecularStyle {

        override val color: Color = Color.White.copy(alpha = intensity)

        override val blendMode: BlendMode = DrawScope.DefaultBlendMode

        override fun DrawScope.createShader(
            shape: Shape,
            runtimeShaderCache: PrismalShaderCache
        ): PrismalShader? {
            return if (isAGSLShaderSupported()) {
                runtimeShaderCache.obtainAGSLShader(
                    "Ambient",
                    PrismalAmbientSpecularShader
                ).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("cornerRadii", getCornerRadii(shape))
                    setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                    setFloatUniform("falloff", 1f)
                }
            } else {
                null
            }
        }
    }

    companion object {

        @Stable
        val Default: Default = Default()

        @Stable
        val Ambient: Ambient = Ambient()

        @Stable
        val Plain: Plain = Plain()
    }
}

private fun DrawScope.getCornerRadii(shape: Shape): FloatArray {
    val size = size
    val maxRadius = size.minDimension / 2f
    return when (shape) {
        is PrismalRoundedRectangularShape -> {
            val corners = shape.corners(size, layoutDirection, this)
            floatArrayOf(
                corners.topLeft.fastCoerceAtMost(maxRadius),
                corners.topRight.fastCoerceAtMost(maxRadius),
                corners.bottomRight.fastCoerceAtMost(maxRadius),
                corners.bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }

        is CornerBasedShape -> {
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

        else -> FloatArray(4) { maxRadius }
    }
}
