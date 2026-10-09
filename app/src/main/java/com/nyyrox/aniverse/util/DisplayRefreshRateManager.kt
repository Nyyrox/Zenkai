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
        activity.runOnUiThread {
            try {
                val window = activity.window ?: return@runOnUiThread
                val currentAttrs = window.attributes ?: return@runOnUiThread
                val params = WindowManager.LayoutParams().apply { copyFrom(currentAttrs) }

                val rate = effectiveHz.toFloat()
                // Explicitly set preferredRefreshRate (API 23+)
                params.preferredRefreshRate = rate

                // In Android 11+ (API 30+), clamp the variable refresh rate range safely via reflection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val minField = params.javaClass.getField("preferredMinDisplayRefreshRate")
                        val maxField = params.javaClass.getField("preferredMaxDisplayRefreshRate")
                        minField.setFloat(params, rate)
                        maxField.setFloat(params, rate)
                    } catch (_: Throwable) {}
                }

                // Get current display mode resolution to ensure we don't switch resolution
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display ?: (activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.getDisplay(Display.DEFAULT_DISPLAY)
                } else {
                    @Suppress("DEPRECATION")
                    activity.windowManager?.defaultDisplay
                }
                val currentMode = display?.mode
                val curW = currentMode?.physicalWidth ?: 0
                val curH = currentMode?.physicalHeight ?: 0

                // Find matching hardware display mode ID with same resolution
                val modes = getDisplayModes(activity)
                val sameResModes = modes.filter {
                    (curW == 0 || it.physicalWidth == curW) && (curH == 0 || it.physicalHeight == curH)
                }
                val candidateModes = if (sameResModes.isNotEmpty()) sameResModes else modes.toList()

                val matchingMode = candidateModes.filter { abs(it.refreshRate - rate) < 3.5f }
                    .maxByOrNull { it.physicalWidth * it.physicalHeight }
                    ?: if (effectiveHz == 60) {
                        candidateModes.find { abs(it.refreshRate - 60f) < 2f }
                    } else if (effectiveHz == 144) {
                        candidateModes.find { it.refreshRate >= 140f }
                    } else {
                        candidateModes.minByOrNull { abs(it.refreshRate - rate) }
                    }

                if (matchingMode != null) {
                    params.preferredDisplayModeId = matchingMode.modeId
                }

                window.attributes = params

                // Request layout on decor view and set frame rate on root surface control
                window.decorView.post {
                    try {
                        window.decorView.requestLayout()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val root = window.decorView.javaClass.getMethod("getRootSurfaceControl").invoke(window.decorView)
                            if (root != null) {
                                root.javaClass.getMethod("setFrameRate", Float::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                    .invoke(root, rate, 0)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }
        return effectiveHz
    }
}
