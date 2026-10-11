package com.styropyr0.prismal.effects

import androidx.compose.ui.graphics.RenderEffect
import com.styropyr0.prismal.PrismalGlassEffectProvider
import com.styropyr0.prismal.PrismalShader
import com.styropyr0.prismal.internal.PrismalShaderEffect
import com.styropyr0.prismal.internal.chain
import com.styropyr0.prismal.isRenderEffectSupported
import com.styropyr0.prismal.isAGSLShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

/** Chains a pre-built [RenderEffect] onto the current backdrop pipeline. */
fun PrismalGlassEffectProvider.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

/**
 * Applies a custom AGSL [shaderString] as a runtime render effect.
 *
 * @param key Cache key for the compiled shader.
 * @param shaderString AGSL source.
 * @param uniformShaderName Name of the `uniform shader` input in the AGSL (usually `"content"`).
 * @param block Uniform setup applied after the shader is obtained from cache.
 */
@OptIn(ExperimentalContracts::class)
fun PrismalGlassEffectProvider.prismalAGSLShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: PrismalShader.() -> Unit
) {
    if (!isAGSLShaderSupported()) return

    val effect =
        PrismalShaderEffect(
            runtimeShader = obtainAGSLShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
