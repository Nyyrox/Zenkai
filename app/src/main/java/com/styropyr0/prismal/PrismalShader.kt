package com.styropyr0.prismal

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

/**
 * Wrapper around [android.graphics.RuntimeShader] with Compose-friendly uniform setters.
 *
 * Create instances with [PrismalShader] on API 33+. Convert to a Compose [Shader]
 * via [asComposeShader] for custom drawing.
 */
interface PrismalShader {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float)
    fun setFloatUniform(name: String, values: FloatArray)

    fun setIntUniform(name: String, value: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int)
    fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int)
    fun setIntUniform(name: String, values: IntArray)

    fun setColorUniform(name: String, color: Color)
}

/** Compiles an AGSL shader string into a [PrismalShader]. Requires API 33+. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun PrismalShader(@Language("AGSL") shaderString: String): PrismalShader {
    val shader = RuntimeShader(shaderString)
    return PrismalShaderHost(shader)
}

/** Converts this shader to a Compose [Shader] for use in [androidx.compose.ui.graphics.Paint]. */
fun PrismalShader.asComposeShader(): Shader {
    return this.asPrismalShaderHost()
}

/** Returns the underlying platform [android.graphics.RuntimeShader]. Requires API 33+. */
fun PrismalShader.asPrismalShaderHost(): RuntimeShader {
    return (this as PrismalShaderHost).shader
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class PrismalShaderHost(val shader: RuntimeShader) : PrismalShader {
    override fun setFloatUniform(name: String, value: Float) {
        shader.setFloatUniform(name, value)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float) {
        shader.setFloatUniform(name, value1, value2)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
        shader.setFloatUniform(name, value1, value2, value3)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) {
        shader.setFloatUniform(name, value1, value2, value3, value4)
    }

    override fun setFloatUniform(name: String, values: FloatArray) {
        shader.setFloatUniform(name, values)
    }

    override fun setIntUniform(name: String, value: Int) {
        shader.setIntUniform(name, value)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int) {
        shader.setIntUniform(name, value1, value2)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {
        shader.setIntUniform(name, value1, value2, value3)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {
        shader.setIntUniform(name, value1, value2, value3, value4)
    }

    override fun setIntUniform(name: String, values: IntArray) {
        shader.setIntUniform(name, values)
    }

    override fun setColorUniform(name: String, color: Color) {
        shader.setColorUniform(name, color.toArgb())
    }
}
