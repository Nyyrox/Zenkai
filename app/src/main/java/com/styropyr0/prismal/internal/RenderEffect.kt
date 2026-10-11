package com.styropyr0.prismal.internal

import android.graphics.RenderEffect.createChainEffect
import android.graphics.RenderEffect.createColorFilterEffect
import android.graphics.RenderEffect.createRuntimeShaderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.styropyr0.prismal.PrismalShader
import com.styropyr0.prismal.asPrismalShaderHost

@RequiresApi(Build.VERSION_CODES.S)
internal fun RenderEffect?.chain(other: RenderEffect): RenderEffect {
    return if (this != null) {
        createChainEffect(
            other.asAndroidRenderEffect(),
            this.asAndroidRenderEffect()
        ).asComposeRenderEffect()
    } else {
        other
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun PrismalShaderEffect(
    runtimeShader: PrismalShader,
    uniformShaderName: String
): RenderEffect {
    return createRuntimeShaderEffect(
        runtimeShader.asPrismalShaderHost(),
        uniformShaderName
    ).asComposeRenderEffect()
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun ColorFilterEffect(
    renderEffect: RenderEffect?,
    colorFilter: ColorFilter
): RenderEffect {
    return if (renderEffect != null) {
        createColorFilterEffect(
            colorFilter.asAndroidColorFilter(),
            renderEffect.asAndroidRenderEffect()
        ).asComposeRenderEffect()
    } else {
        createColorFilterEffect(
            colorFilter.asAndroidColorFilter(),
        ).asComposeRenderEffect()
    }
}
