package com.styropyr0.prismal.shapes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Rounded-rectangular [androidx.compose.ui.graphics.Shape] with per-corner radius support.
 *
 * Required by [com.styropyr0.prismal.effects.prismalLens] for correct edge refraction.
 */
@Immutable
sealed interface PrismalRoundedRectangularShape : Shape {

    val style: PrismalRoundedCornerStyle?
        get() = null

    fun corners(size: Size, layoutDirection: LayoutDirection, density: Density): Corners

    fun copy(style: PrismalRoundedCornerStyle): PrismalRoundedRectangularShape

    data class Corners(
        val topLeft: Float,
        val topRight: Float,
        val bottomRight: Float,
        val bottomLeft: Float
    )
}
