package com.nyyrox.aniverse.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nyyrox.aniverse.MainActivity

/**
 * Media3 MediaSessionService that provides the system media notification with
 * working play/pause, seek-bar, and next/previous episode controls.
 */
@OptIn(UnstableApi::class)
class PlayerNotificationService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID     = "playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_ATTACH = "com.nyyrox.aniverse.action.ATTACH_PLAYER"
        const val ACTION_DETACH = "com.nyyrox.aniverse.action.DETACH_PLAYER"
        const val ACTION_UPDATE = "com.nyyrox.aniverse.action.UPDATE_PLAYER"

        // Legacy compatibility aliases
        const val ACTION_START        = ACTION_ATTACH
        const val ACTION_STOP         = ACTION_DETACH
        const val EXTRA_TITLE         = "extra_title"
        const val EXTRA_EPISODE_INFO  = "extra_episode_info"
        const val EXTRA_IMAGE_URL     = "extra_image_url"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        rebuildSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_ATTACH, ACTION_START, ACTION_UPDATE -> rebuildSession()
            ACTION_DETACH, ACTION_STOP -> {
                mediaSession?.let { session ->
                    removeSession(session)
                    session.release()
                    mediaSession = null
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Replaces the current MediaSession with one bound to the active player. */
    private fun rebuildSession() {
        val realPlayer = ActivePlayerHolder.player
        val targetPlayer: Player = if (realPlayer != null) {
            object : ForwardingPlayer(realPlayer) {
                override fun getAvailableCommands(): Player.Commands {
                    val builder = super.getAvailableCommands().buildUpon()
                    builder.add(Player.COMMAND_PLAY_PAUSE)
                    builder.add(Player.COMMAND_STOP)
                    builder.add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    if (ActivePlayerHolder.hasNext) {
                        builder.add(Player.COMMAND_SEEK_TO_NEXT)
                        builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    } else {
                        builder.remove(Player.COMMAND_SEEK_TO_NEXT)
                        builder.remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    }
                    if (ActivePlayerHolder.hasPrev) {
                        builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    } else {
                        builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                        builder.remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    }
                    return builder.build()
                }

                override fun isCommandAvailable(command: Int): Boolean {
                    return when (command) {
                        Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP, Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                        Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> ActivePlayerHolder.hasNext
                        Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> ActivePlayerHolder.hasPrev
                        else -> super.isCommandAvailable(command)
                    }
                }

                override fun seekToNext() {
                    ActivePlayerHolder.onSkipNext?.invoke()
                }

                override fun seekToNextMediaItem() {
                    ActivePlayerHolder.onSkipNext?.invoke()
                }

                override fun seekToPrevious() {
                    ActivePlayerHolder.onSkipPrev?.invoke()
                }

                override fun seekToPreviousMediaItem() {
                    ActivePlayerHolder.onSkipPrev?.invoke()
                }
            }
        } else {
            ExoPlayer.Builder(this).build().also { it.stop() }
        }

        val oldSession = mediaSession
        if (oldSession != null) {
            removeSession(oldSession)
            oldSession.release()
        }

        val returnToAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val newSession = MediaSession.Builder(this, targetPlayer)
            .setSessionActivity(returnToAppIntent)
            .build()

        mediaSession = newSession
        addSession(newSession)
        ActivePlayerHolder.onSessionReady?.invoke(newSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            removeSession(session)
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_LOW)
                        .apply { description = "Zenkai player controls" }
                )
            }
        }
    }
}
