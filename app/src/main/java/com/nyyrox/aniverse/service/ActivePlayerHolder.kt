package com.nyyrox.aniverse.service

import androidx.media3.exoplayer.ExoPlayer

/**
 * Singleton bridge between CinePlayerView (which owns the real ExoPlayer)
 * and PlayerNotificationService (which hosts the MediaSession).
 *
 * CinePlayerView calls [attach] when the player is ready and [detach] on dispose.
 * PlayerNotificationService reads [player] to build/update the MediaSession.
 */
object ActivePlayerHolder {
    @Volatile var player: ExoPlayer? = null
    @Volatile var onSkipNext: (() -> Unit)? = null
    @Volatile var onSkipPrev: (() -> Unit)? = null
    @Volatile var hasNext: Boolean = false
    @Volatile var hasPrev: Boolean = false
    @Volatile var onSessionReady: ((androidx.media3.session.MediaSession) -> Unit)? = null

    fun attach(
        exoPlayer: ExoPlayer,
        skipNext: (() -> Unit)? = null,
        skipPrev: (() -> Unit)? = null,
        hasNextEp: Boolean = false,
        hasPrevEp: Boolean = false
    ) {
        player = exoPlayer
        onSkipNext = skipNext
        onSkipPrev = skipPrev
        hasNext = hasNextEp
        hasPrev = hasPrevEp
    }

    fun updateNavigation(
        hasNextEp: Boolean,
        hasPrevEp: Boolean,
        skipNext: (() -> Unit)?,
        skipPrev: (() -> Unit)?
    ) {
        hasNext = hasNextEp
        hasPrev = hasPrevEp
        onSkipNext = skipNext
        onSkipPrev = skipPrev
    }

    fun detach() {
        player = null
        onSkipNext = null
        onSkipPrev = null
        hasNext = false
        hasPrev = false
    }
}
