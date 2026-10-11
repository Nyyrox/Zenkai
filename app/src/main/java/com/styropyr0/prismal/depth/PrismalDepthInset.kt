package com.styropyr0.prismal.depth

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

/**
 * Inner shadow / inset shading rendered inside a glass shape.
 *
 * Used for pressed states, thumb wells, and tab indicator depth.
 */
@Immutable
data class PrismalDepthInset(
    val radius: Dp = 24f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    @param:FloatRange(from = 0.0, to = 1.0) val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Stable
        val Default: PrismalDepthInset = PrismalDepthInset()
    }
}

/** Linearly interpolates between two [PrismalDepthInset] values. */
@Stable
fun lerp(start: PrismalDepthInset, stop: PrismalDepthInset, fraction: Float): PrismalDepthInset {
    return PrismalDepthInset(
        radius = lerp(start.radius, stop.radius, fraction),
        offset = lerp(start.offset, stop.offset, fraction),
        color = lerp(start.color, stop.color, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction),
        blendMode = if (fraction < 0.5f) start.blendMode else stop.blendMode
    )
}
