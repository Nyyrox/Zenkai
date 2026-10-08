package com.nyyrox.aniverse.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Display
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

object DisplayRefreshRateManager {

    /**
     * Returns the distinct supported refresh rates on this device (e.g. [60, 90], [60, 120], [60, 90, 120]).
     * Always contains at least 60Hz.
     */
    fun getSupportedRefreshRates(context: Context): List<Int> {
        val rates = mutableSetOf(60)
        try {
            val modes = getDisplayModes(context)
            for (mode in modes) {
                val hz = mode.refreshRate.roundToInt()
                if (hz in 58..62) rates.add(60)
                else if (hz in 88..93) rates.add(90)
                else if (hz in 118..124) rates.add(120)
                else if (hz in 140..148) rates.add(144)
                else if (hz > 60) rates.add(hz)
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

        val supported = getSupportedRefreshRates(context)
        val maxHz = supported.maxOrNull() ?: 60

        // If targetGz <= 0, default to 90Hz if supported, else max capability
        val requested = if (preferredHz <= 0) {
            if (supported.contains(90)) 90 else if (maxHz >= 90) 90 else maxHz
        } else {
            preferredHz
        }

        // Return closest supported rate <= requested, or closest overall
        return if (supported.contains(requested)) {
            requested
        } else {
            supported.filter { it <= requested }.maxOrNull() ?: maxHz
        }
    }

    /**
     * Applies the target refresh rate to the Activity window.
     * Sets preferredRefreshRate, preferredDisplayModeId, and decorView frame rate.
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

            // Find matching display mode ID
            val modes = getDisplayModes(activity)
            val matchingMode = modes.filter { abs(it.refreshRate - effectiveHz.toFloat()) < 2.5f }
                .maxByOrNull { it.physicalWidth * it.physicalHeight }
                ?: modes.minByOrNull { abs(it.refreshRate - effectiveHz.toFloat()) }

            if (matchingMode != null) {
                params.preferredDisplayModeId = matchingMode.modeId
            }

            window.attributes = params
        } catch (_: Throwable) {}
        return effectiveHz
    }

    private fun getDisplayModes(context: Context): Array<Display.Mode> {
        return try {
            if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.supportedModes ?: emptyArray()
            } else {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                wm?.defaultDisplay?.supportedModes ?: emptyArray()
            }
        } catch (_: Throwable) {
            emptyArray()
        }
    }
}
