package com.nyyrox.aniverse.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import kotlin.math.roundToInt

/**
 * Controller plugin for player Volume and Brightness gestures,
 * hardware volume key notifications, and system window brightness adjustment.
 */
object PlayerGesturePlugin {

    object VolumeNotifier {
        var isPlayerActive: Boolean = false
        var onVolumeChanged: ((Float) -> Unit)? = null
    }

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getVolumeFraction(context: Context): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) cur.toFloat() / max.toFloat() else 0f
    }

    fun setVolumeFraction(context: Context, fraction: Float) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun adjustVolumeByStep(context: Context, stepDelta: Int): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (cur + stepDelta).coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return if (max > 0) target.toFloat() / max.toFloat() else 0f
    }

    fun getBrightnessFraction(activity: Activity?): Float {
        val lp = activity?.window?.attributes ?: return 0.5f
        return if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness.coerceIn(0.01f, 1.0f)
    }

    fun setBrightnessFraction(activity: Activity?, fraction: Float) {
        val act = activity ?: return
        val safeFrac = fraction.coerceIn(0.01f, 1.0f)
        val lp = act.window.attributes
        lp.screenBrightness = safeFrac
        act.window.attributes = lp
    }
}
