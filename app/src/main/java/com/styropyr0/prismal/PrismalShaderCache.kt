package com.styropyr0.prismal

import org.intellij.lang.annotations.Language

/** Cache for reusing compiled [PrismalShader] instances within a single glass draw pass. */
sealed interface PrismalShaderCache {

    /**
     * Returns a cached shader for [key], compiling [string] on first use.
     *
     * @param key Stable identifier for the shader variant (e.g. `"Refraction"`).
     * @param string AGSL source code.
     */
    fun obtainAGSLShader(key: String, @Language("AGSL") string: String): PrismalShader
}

internal class PrismalShaderCacheImpl : PrismalShaderCache {
    private val agslShaders = mutableMapOf<String, PrismalShader>()

    override fun obtainAGSLShader(key: String, string: String): PrismalShader {
        return agslShaders.getOrPut(key) { PrismalShader(string) }
    }

    fun clear() { agslShaders.clear() }
}
