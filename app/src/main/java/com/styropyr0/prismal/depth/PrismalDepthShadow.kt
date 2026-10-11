package com.styropyr0.prismal.depth

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Drop shadow rendered beneath a glass shape.
 *
 * @param radius Blur radius of the shadow.
 * @param offset Shadow offset from the shape origin.
 * @param color Shadow fill color before [alpha] is applied.
 * @param alpha Opacity of the shadow layer.
 * @param blendMode Compositing mode when drawing the shadow.
 */
@Immutable
data class PrismalDepthShadow(
    val radius: Dp = 24f.dp,
    val offset: DpOffset = DpOffset(0f.dp, radius / 6f),
    val color: Color = Color.Black.copy(alpha = 0.1f),
    @param:FloatRange(from = 0.0, to = 1.0) val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @Stable
        val Default: PrismalDepthShadow = PrismalDepthShadow()
    }
}
