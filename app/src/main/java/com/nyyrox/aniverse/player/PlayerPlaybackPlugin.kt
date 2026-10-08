package com.nyyrox.aniverse.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Playback controller plugin that decouples UI play/pause state from ExoPlayer's
 * transient STATE_BUFFERING, preserving playback intent across seeks, scrubbing,
 * and buffering transitions.
 */
object PlayerPlaybackPlugin {

    /**
     * Safely toggles play/pause state.
     * If the video has ended, restarts from 0.
     * Toggles playWhenReady directly so UI updates reliably even during buffering.
     */
    fun togglePlayPause(player: ExoPlayer?) {
        try {
            val p = player ?: return
            if (p.playbackState == Player.STATE_ENDED) {
                p.seekTo(0L)
                p.playWhenReady = true
            } else {
                p.playWhenReady = !p.playWhenReady
            }
        } catch (_: Exception) {}
    }

    /**
     * Safely seeks to an absolute position in milliseconds,
     * maintaining the user's playback intent (playWhenReady).
     */
    fun seekTo(player: ExoPlayer?, targetMs: Long) {
        try {
            val p = player ?: return
            val dur = p.duration.coerceAtLeast(0L)
            val safeTarget = if (dur > 0L) targetMs.coerceIn(0L, dur) else targetMs.coerceAtLeast(0L)
            val wasPlaying = p.playWhenReady
            p.seekTo(safeTarget)
            p.playWhenReady = wasPlaying
        } catch (_: Exception) {}
    }

    /**
     * Relative seek by deltaMs (e.g. +10000L or -10000L).
     */
    fun seekBy(player: ExoPlayer?, deltaMs: Long) {
        try {
            val p = player ?: return
            seekTo(p, p.currentPosition + deltaMs)
        } catch (_: Exception) {}
    }

    /**
     * Determines whether the player is playing or intends to play.
     * Avoids returning false during STATE_BUFFERING when user pressed play.
     */
    fun isPlayingOrIntended(player: ExoPlayer?): Boolean {
        try {
            val p = player ?: return false
            return p.playWhenReady && p.playbackState != Player.STATE_ENDED
        } catch (_: Exception) {
            return false
        }
    }
}
