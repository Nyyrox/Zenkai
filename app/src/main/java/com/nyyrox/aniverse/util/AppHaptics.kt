package com.nyyrox.aniverse.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Global low-intensity tactile haptics for Zenkai.
 * Provides subtle, refined feedback (tick) for player controls, poster clicks,
 * mini-player handoff, volume limits, and setting changes.
 */
object AppHaptics {

    /**
     * Subtle, very low intensity tick vibration.
     */
    fun tick(context: Context?) {
        if (context == null) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10L, 25))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(8L)
            }
        } catch (_: Throwable) {}
    }

    /**
     * View-based haptic tick fallback.
     */
    fun viewTick(view: View?) {
        if (view == null) return
        try {
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (_: Throwable) {
            tick(view.context)
        }
    }
}
