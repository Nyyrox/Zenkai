package com.styropyr0.prismal

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/** Active rendering tier selected automatically from the device API level. */
enum class PrismalGlassPipeline {
    /** API 25–30: sampled backdrop with a frosted surface treatment. */
    Legacy,
    /** API 31–32: backdrop blur and color filters via [androidx.compose.ui.graphics.RenderEffect]. */
    Standard,
    /** API 33+: full liquid glass with AGSL refraction and shader effects. */
    Liquid
}

/** Returns `true` when [androidx.compose.ui.graphics.RenderEffect] is available (API 31+). */
@ChecksSdkIntAtLeast(Build.VERSION_CODES.S)
fun isRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Returns `true` when AGSL runtime shaders are available (API 33+). */
@ChecksSdkIntAtLeast(Build.VERSION_CODES.TIRAMISU)
fun isAGSLShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Platform capability checks and pipeline selection for the Prismal glass stack. */
object PrismalGlass {

    /** Minimum supported SDK for the library (Android 7.0). */
    const val MIN_SDK = 25

    /** Master toggle to optimize render transparency / enable or disable glassmorphism. */
    var isGlassmorphismEnabled: Boolean = true

    /**
     * Returns the pipeline tier in use on this device.
     *
     * Callers do not need to branch on this for normal glass usage; effect helpers skip
     * unsupported features automatically.
     */
    val pipeline: PrismalGlassPipeline
        get() = when {
            !isGlassmorphismEnabled -> PrismalGlassPipeline.Legacy
            isAGSLShaderSupported() -> PrismalGlassPipeline.Liquid
            isRenderEffectSupported() -> PrismalGlassPipeline.Standard
            else -> PrismalGlassPipeline.Legacy
        }

    /** Glass rendering is available on API [MIN_SDK]+ with automatic tier degradation. */
    @ChecksSdkIntAtLeast(api = MIN_SDK)
    @SuppressLint("ObsoleteSdkInt")
    fun isSupported(): Boolean = isGlassmorphismEnabled && Build.VERSION.SDK_INT >= MIN_SDK

    /** Backdrop blur via RenderEffect (API 31+). */
    val supportsBackdropBlur: Boolean
        get() = isGlassmorphismEnabled && pipeline != PrismalGlassPipeline.Legacy

    /** Edge lens refraction and dispersion shaders (API 33+). */
    val supportsRefraction: Boolean
        get() = isGlassmorphismEnabled && pipeline == PrismalGlassPipeline.Liquid

    /** Custom AGSL shader effects such as gradient glass (API 33+). */
    val supportsAgslEffects: Boolean
        get() = isGlassmorphismEnabled && pipeline == PrismalGlassPipeline.Liquid
}
