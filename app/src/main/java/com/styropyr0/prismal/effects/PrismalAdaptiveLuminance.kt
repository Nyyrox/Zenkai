package com.styropyr0.prismal.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import com.styropyr0.prismal.isRenderEffectSupported
import com.styropyr0.prismal.sources.PrismalGlassLayer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

private const val LUMINANCE_PROBE_SIZE = 8
private const val LUMINANCE_SAMPLE_INTERVAL_MS = 600L
private const val LEGACY_LUMINANCE_SAMPLE_INTERVAL_MS = 1500L
private const val LUMINANCE_UPDATE_THRESHOLD = 0.04f
private const val LUMINANCE_QUANTUM = 0.05f

@Stable
class PrismalAdaptiveLuminanceState internal constructor(initialLuminance: Float) {
    var luminance by mutableFloatStateOf(initialLuminance)
        internal set
    var contentColor by mutableStateOf(
        if (initialLuminance > 0.5f) Color.Black else Color.White
    )
        internal set
}

/**
 * Monitors [source] backdrop layer brightness and exposes adaptive glass tuning values.
 *
 * Starts at a neutral [initialLuminance] (`0.5` by default) so glass does not wash out
 * before the first probe sample. Pass [enabled] = false to freeze at that midpoint.
 *
 * [isLightTheme] only seeds [PrismalAdaptiveLuminanceState.contentColor] before the
 * first sample; it no longer forces initial luminance to `1` / `0`.
 */
@Composable
fun rememberPrismalAdaptiveLuminance(
    enabled: Boolean,
    source: PrismalGlassLayer,
    isLightTheme: Boolean,
    initialLuminance: Float = 0.5f,
): PrismalAdaptiveLuminanceState {
    val initial = initialLuminance.coerceIn(0f, 1f)
    val state = remember {
        PrismalAdaptiveLuminanceState(initial).also {
            it.contentColor = if (isLightTheme) Color.Black else Color.White
        }
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val probeLayer = rememberGraphicsLayer()

    LaunchedEffect(enabled, source, density, layoutDirection) {
        if (!enabled) {
            state.luminance = initial
            state.contentColor = if (isLightTheme) Color.Black else Color.White
            return@LaunchedEffect
        }

        val pixelBuffer = IntArray(LUMINANCE_PROBE_SIZE * LUMINANCE_PROBE_SIZE)
        val probeSize = IntSize(LUMINANCE_PROBE_SIZE, LUMINANCE_PROBE_SIZE)
        val sampleIntervalMs = if (isRenderEffectSupported()) LUMINANCE_SAMPLE_INTERVAL_MS else LEGACY_LUMINANCE_SAMPLE_INTERVAL_MS

        while (isActive) {
            try {
                val sourceLayer = source.graphicsLayer
                val sourceSize = sourceLayer.size
                if (sourceSize.width > 0 && sourceSize.height > 0) {
                    probeLayer.record(density, layoutDirection, probeSize) {
                        scale(
                            LUMINANCE_PROBE_SIZE.toFloat() / sourceSize.width,
                            LUMINANCE_PROBE_SIZE.toFloat() / sourceSize.height
                        ) {
                            drawLayer(sourceLayer)
                        }
                    }
                    val probeBitmap = probeLayer.toImageBitmap()
                    probeBitmap.readPixels(
                        buffer = pixelBuffer,
                        startX = 0,
                        startY = 0,
                        width = LUMINANCE_PROBE_SIZE,
                        height = LUMINANCE_PROBE_SIZE,
                    )

                    val averageLuminance = averageLuminanceFromArgb(pixelBuffer)
                    val quantized =
                        (averageLuminance / LUMINANCE_QUANTUM).roundToInt() * LUMINANCE_QUANTUM

                    if (abs(quantized - state.luminance) >= LUMINANCE_UPDATE_THRESHOLD) {
                        state.luminance = quantized
                        state.contentColor = if (quantized > 0.5f) Color.Black else Color.White
                    }
                }
            } catch (_: Exception) {
            }
            delay(sampleIntervalMs.milliseconds)
        }
    }

    return state
}

private fun averageLuminanceFromArgb(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0.5f
    val sum =
        pixels.sumOf { argb ->
            val r = (argb shr 16 and 0xFF) / 255.0
            val g = (argb shr 8 and 0xFF) / 255.0
            val b = (argb and 0xFF) / 255.0
            0.2126 * r + 0.7152 * g + 0.0722 * b
        }
    return (sum / pixels.size).toFloat().coerceIn(0f, 1f)
}
