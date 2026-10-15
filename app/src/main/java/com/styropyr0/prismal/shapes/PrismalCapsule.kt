package com.styropyr0.prismal.shapes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Stadium / capsule shape — a rounded rectangle whose corner radius equals half the
 * shorter side.
 */
@Immutable
class PrismalCapsule(
    override val style: PrismalRoundedCornerStyle = PrismalRoundedCornerStyle.Continuous
) : PrismalRoundedRectangularShape {

    override fun corners(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): PrismalRoundedRectangularShape.Corners {
        val radius = size.minDimension * 0.5f
        return PrismalRoundedRectangularShape.Corners(
            topLeft = radius,
            topRight = radius,
            bottomRight = radius,
            bottomLeft = radius
        )
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = size.minDimension * 0.5f
        return prismalRoundedRectangleOutline(
            size = size,
            radius = radius,
            style = style
        )
    }

    override fun copy(style: PrismalRoundedCornerStyle) =
        PrismalCapsule(style = style)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrismalCapsule) return false

        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        return style.hashCode()
    }

    override fun toString(): String {
        return "PrismalCapsule(style=$style)"
    }
}
