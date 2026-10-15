package com.styropyr0.prismal.shapes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn

/**
 * Rounded rectangle shape with a uniform [cornerRadius].
 *
 * @param cornerRadius Corner radius applied to all four corners.
 * @param style Corner interpolation style ([PrismalRoundedCornerStyle.Continuous] by default).
 */
@Immutable
class PrismalRoundedRectangle(
    val cornerRadius: Dp,
    override val style: PrismalRoundedCornerStyle = PrismalRoundedCornerStyle.Continuous
) : PrismalRoundedRectangularShape {

    override fun corners(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): PrismalRoundedRectangularShape.Corners {
        val radius = with(density) { cornerRadius.toPx() }.fastCoerceIn(0f, size.minDimension * 0.5f)
        return PrismalRoundedRectangularShape.Corners(
            topLeft = radius,
            topRight = radius,
            bottomRight = radius,
            bottomLeft = radius
        )
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { cornerRadius.toPx() }.fastCoerceIn(0f, size.minDimension * 0.5f)
        return prismalRoundedRectangleOutline(
            size = size,
            radius = radius,
            style = style
        )
    }

    override fun copy(style: PrismalRoundedCornerStyle) =
        PrismalRoundedRectangle(
            cornerRadius = cornerRadius,
            style = style
        )

    fun copy(
        cornerRadius: Dp = this.cornerRadius,
        style: PrismalRoundedCornerStyle = this.style
    ) =
        PrismalRoundedRectangle(
            cornerRadius = cornerRadius,
            style = style
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrismalRoundedRectangle) return false

        if (cornerRadius != other.cornerRadius) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "PrismalRoundedRectangle(cornerRadius=$cornerRadius, style=$style)"
    }
}
