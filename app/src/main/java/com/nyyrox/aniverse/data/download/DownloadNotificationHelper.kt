package com.nyyrox.aniverse.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nyyrox.aniverse.MainActivity
import com.nyyrox.aniverse.R
import java.util.concurrent.ConcurrentHashMap

class DownloadNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.nyyrox.aniverse.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_TYPE = "extra_download_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_CHAPTER_ID = "extra_chapter_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_CANCEL_DOWNLOAD) {
            val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.ANIME.name
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
            val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: return
            val type = try {
                DownloadType.valueOf(typeStr)
            } catch (_: Exception) {
                DownloadType.ANIME
            }

            val taskKey = "$itemId:$chapterId"
            DownloadManager.instance?.cancelTask(type, itemId, chapterId)
            DownloadNotificationHelper.cancelNotification(context, taskKey)
        }
    }
}

object DownloadNotificationHelper {
    const val CHANNEL_PROGRESS_ID = "zenkai_anime_downloads"
    const val CHANNEL_PROGRESS_NAME = "Active Downloads"
    const val CHANNEL_PROGRESS_DESC = "Shows ongoing download progress and network speed"

    const val CHANNEL_COMPLETE_ID = "zenkai_anime_downloads_complete"
    const val CHANNEL_COMPLETE_NAME = "Completed Downloads"
    const val CHANNEL_COMPLETE_DESC = "Notifies when anime downloads finish successfully or fail"

    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // Channel 1: Progress (low importance, silent to avoid constant buzzing)
            if (notificationManager.getNotificationChannel(CHANNEL_PROGRESS_ID) == null) {
                val progressChannel = NotificationChannel(
                    CHANNEL_PROGRESS_ID,
                    CHANNEL_PROGRESS_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_PROGRESS_DESC
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(progressChannel)
            }

            // Channel 2: Completed / Failed (default importance)
            if (notificationManager.getNotificationChannel(CHANNEL_COMPLETE_ID) == null) {
                val completeChannel = NotificationChannel(
                    CHANNEL_COMPLETE_ID,
                    CHANNEL_COMPLETE_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_COMPLETE_DESC
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(completeChannel)
            }
        }
    }

    private fun getNotificationId(taskKey: String): Int {
        return (taskKey.hashCode() and 0x7FFFFFFF)
    }

    fun updateProgressNotification(context: Context, task: AnimeDownloadTask) {
        if (!DownloadPreferences.isNotificationsEnabled(context)) {
            return
        }

        val taskKey = "${task.animeId}:${task.episode}"
        val now = System.currentTimeMillis()
        val lastUpdate = lastUpdateTimes[taskKey] ?: 0L

        // Throttle updates to at most once every 600ms unless progress is starting or complete
        val isFirst = lastUpdate == 0L
        val isDone = task.progress >= 1f || task.status == DownloadStatus.COMPLETED
        if (!isFirst && !isDone && (now - lastUpdate) < 600L) {
            return
        }
        lastUpdateTimes[taskKey] = now

        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val notifId = getNotificationId(taskKey)

            // Content intent opens DownloadsScreen
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "downloads")
            }
            val pendingContentIntent = PendingIntent.getActivity(
                context,
                notifId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel action
            val cancelIntent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = DownloadNotificationReceiver.ACTION_CANCEL_DOWNLOAD
                putExtra(DownloadNotificationReceiver.EXTRA_TYPE, DownloadType.ANIME.name)
                putExtra(DownloadNotificationReceiver.EXTRA_ITEM_ID, task.animeId.toString())
                putExtra(DownloadNotificationReceiver.EXTRA_CHAPTER_ID, task.episode.toString())
            }
            val pendingCancelIntent = PendingIntent.getBroadcast(
                context,
                notifId + 100000,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val percentage = (task.progress.coerceIn(0f, 1f) * 100).toInt()
            val epLabel = task.episodeTitle?.takeIf { it.isNotBlank() && it != "Episode ${task.episode}" }
                ?.let { "Ep. ${task.episode}: $it" }
                ?: "Episode ${task.episode}"

            val subtitleText = when (task.status) {
                DownloadStatus.QUEUED -> "Queued for download..."
                DownloadStatus.DOWNLOADING -> {
                    if (task.speedFormatted.isNotBlank()) {
                        "$epLabel · $percentage% (${task.speedFormatted})"
                    } else {
                        "$epLabel · $percentage%"
                    }
                }
                else -> epLabel
            }

            val isIndeterminate = task.status == DownloadStatus.QUEUED || (task.progress <= 0f && task.status == DownloadStatus.DOWNLOADING)

            val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(0xFFE11D48.toInt())
                .setContentTitle(task.animeTitle)
                .setContentText(subtitleText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, percentage, isIndeterminate)
                .setContentIntent(pendingContentIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    pendingCancelIntent
                )

            notificationManager.notify(notifId, builder.build())
        } catch (_: Exception) {}
    }

    fun showCompleteNotification(context: Context, task: AnimeDownloadTask) {
        val taskKey = "${task.animeId}:${task.episode}"
        lastUpdateTimes.remove(taskKey)

        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val notifId = getNotificationId(taskKey)
            // Cancel the ongoing progress notification
            notificationManager.cancel(notifId)

            if (!DownloadPreferences.isNotificationsEnabled(context)) {
                return
            }

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "downloads")
            }
            val pendingContentIntent = PendingIntent.getActivity(
                context,
                notifId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val epLabel = task.episodeTitle?.takeIf { it.isNotBlank() && it != "Episode ${task.episode}" }
                ?.let { "Episode ${task.episode} - $it" }
                ?: "Episode ${task.episode}"

            val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(0xFF10B981.toInt())
                .setContentTitle("Download Complete: ${task.animeTitle}")
                .setContentText("$epLabel is ready for offline playback.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)

            notificationManager.notify(notifId, builder.build())
        } catch (_: Exception) {}
    }

    fun showFailedNotification(context: Context, task: AnimeDownloadTask, reason: String) {
        val taskKey = "${task.animeId}:${task.episode}"
        lastUpdateTimes.remove(taskKey)

        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val notifId = getNotificationId(taskKey)
            // Cancel the ongoing progress notification
            notificationManager.cancel(notifId)

            if (!DownloadPreferences.isNotificationsEnabled(context)) {
                return
            }

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "downloads")
            }
            val pendingContentIntent = PendingIntent.getActivity(
                context,
                notifId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val epLabel = task.episodeTitle?.takeIf { it.isNotBlank() && it != "Episode ${task.episode}" }
                ?.let { "Episode ${task.episode} - $it" }
                ?: "Episode ${task.episode}"

            val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(0xFFEF4444.toInt())
                .setContentTitle("Download Failed: ${task.animeTitle}")
                .setContentText("$epLabel: $reason")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)

            notificationManager.notify(notifId, builder.build())
        } catch (_: Exception) {}
    }

    fun cancelNotification(context: Context, taskKey: String) {
        lastUpdateTimes.remove(taskKey)
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notifId = getNotificationId(taskKey)
            notificationManager.cancel(notifId)
        } catch (_: Exception) {}
    }
}
