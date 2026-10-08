package com.nyyrox.aniverse.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

object DisplayRefreshRateManager {

    private fun unwrapActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun getDisplayModes(context: Context): Array<Display.Mode> {
        return try {
            val act = unwrapActivity(context)
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                act?.display ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
            } else {
                @Suppress("DEPRECATION")
                act?.windowManager?.defaultDisplay ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }
            display?.supportedModes ?: emptyArray()
        } catch (_: Throwable) {
            emptyArray()
        }
    }

    /**
     * Returns the distinct supported refresh rates on this device (e.g. [60, 90], [60, 120], [60, 144]).
     * Always contains at least 60Hz.
     */
    fun getSupportedRefreshRates(context: Context): List<Int> {
        val rates = mutableSetOf(60)
        try {
            val modes = getDisplayModes(context)
            for (mode in modes) {
                val hz = mode.refreshRate.roundToInt()
                when {
                    hz in 58..63 -> rates.add(60)
                    hz in 88..94 -> rates.add(90)
                    hz in 118..125 -> rates.add(120)
                    hz in 140..149 -> rates.add(144)
                    hz in 160..168 -> rates.add(165)
                    hz > 60 -> rates.add(hz)
                }
            }
        } catch (_: Throwable) {}
        return rates.sorted()
    }

    /**
     * Highest refresh rate available on hardware.
     */
    fun getMaxSupportedRefreshRate(context: Context): Int {
        val supported = getSupportedRefreshRates(context)
        return supported.maxOrNull() ?: 60
    }

    /**
     * Returns the currently active physical display refresh rate in Hz.
     */
    fun getCurrentDisplayRefreshRate(context: Context): Float {
        return try {
            val act = unwrapActivity(context)
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                act?.display ?: (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
            } else {
                @Suppress("DEPRECATION")
                act?.windowManager?.defaultDisplay ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }
            display?.mode?.refreshRate ?: @Suppress("DEPRECATION") display?.refreshRate ?: 60f
        } catch (_: Throwable) {
            60f
        }
    }

    /**
     * Checks whether the device is in OS system battery saver mode.
     */
    fun isSystemPowerSaveMode(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Computes the effective target refresh rate:
     * - If Battery Saver (either system or in-app) is ON -> locked to 60Hz.
     * - Otherwise -> returns targetHz (or default 90Hz if targetHz <= 0 or not set, capped to max supported).
     */
    fun calculateEffectiveRefreshRate(
        context: Context,
        preferredHz: Int,
        isPowerSavingEnabled: Boolean
    ): Int {
        val isSystemPowerSave = isSystemPowerSaveMode(context)
        if (isPowerSavingEnabled || isSystemPowerSave) {
            return 60
        }

        if (preferredHz == 60) {
            return 60
        }

        val supported = getSupportedRefreshRates(context)
        val maxHz = supported.maxOrNull() ?: 60

        return when {
            preferredHz == 144 && (supported.contains(144) || maxHz >= 140) -> 144
            preferredHz > 0 && supported.contains(preferredHz) -> preferredHz
            preferredHz > 0 && preferredHz <= maxHz -> preferredHz
            preferredHz > 0 -> maxHz
            supported.contains(90) -> 90
            maxHz >= 90 -> 90
            else -> maxHz
        }
    }

    /**
     * Applies the target refresh rate to the Activity window.
     * Sets preferredRefreshRate, preferredMinDisplayRefreshRate, preferredMaxDisplayRefreshRate,
     * preferredDisplayModeId, and decorView frame rate on API 31+.
     */
    fun applyRefreshRate(
        activity: Activity,
        preferredHz: Int,
        isPowerSavingEnabled: Boolean
    ): Int {
        val effectiveHz = calculateEffectiveRefreshRate(activity, preferredHz, isPowerSavingEnabled)
        try {
            val window = activity.window ?: return effectiveHz
            val params = window.attributes ?: return effectiveHz

            // Explicitly set preferredRefreshRate (API 23+)
            params.preferredRefreshRate = effectiveHz.toFloat()

            // In Android 11+ (API 30+), clamp the variable refresh rate range
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (effectiveHz == 60) {
                    params.preferredMinDisplayRefreshRate = 60f
                    params.preferredMaxDisplayRefreshRate = 60f
                } else {
                    params.preferredMinDisplayRefreshRate = effectiveHz.toFloat()
                    params.preferredMaxDisplayRefreshRate = effectiveHz.toFloat()
                }
            }

            // Find matching hardware display mode ID
            val modes = getDisplayModes(activity)
            val matchingMode = modes.filter { abs(it.refreshRate - effectiveHz.toFloat()) < 3.5f }
                .maxByOrNull { it.physicalWidth * it.physicalHeight }
                ?: if (effectiveHz == 60) {
                    modes.find { abs(it.refreshRate - 60f) < 2f }
                } else if (effectiveHz == 144) {
                    modes.find { it.refreshRate >= 140f }
                } else {
                    modes.minByOrNull { abs(it.refreshRate - effectiveHz.toFloat()) }
                }

            if (matchingMode != null) {
                params.preferredDisplayModeId = matchingMode.modeId
            }

            window.attributes = params

            // On Android 12+ (API 31+), set the decorView frame rate for Choreographer / SurfaceFlinger
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    window.decorView.setFrameRate(
                        effectiveHz.toFloat(),
                        android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                    )
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        return effectiveHz
    }
}
