package com.styropyr0.prismal.specular

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specular edge highlight applied around a glass shape.
 *
 * @param width Stroke width of the highlight band.
 * @param blurRadius Gaussian blur applied to the highlight stroke.
 * @param alpha Opacity multiplier for the highlight layer.
 * @param style Shader or paint style used to render the highlight.
 */
@Immutable
data class PrismalSpecular(
    val width: Dp = 0.5f.dp,
    val blurRadius: Dp = width / 2f,
    @param:FloatRange(from = 0.0, to = 1.0) val alpha: Float = 1f,
    val style: PrismalSpecularStyle = PrismalSpecularStyle.Default
) {

    companion object {

        /** Directional AGSL specular with a 45° light angle. */
        @Stable
        val Default: PrismalSpecular = PrismalSpecular()

        /** Soft ambient rim derived from surface normals. */
        @Stable
        val Ambient: PrismalSpecular = PrismalSpecular(style = PrismalSpecularStyle.Ambient)

        /** Simple white stroke without a runtime shader. */
        @Stable
        val Plain: PrismalSpecular = PrismalSpecular(style = PrismalSpecularStyle.Plain)
    }
}
