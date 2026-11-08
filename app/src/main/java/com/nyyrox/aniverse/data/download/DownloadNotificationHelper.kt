package com.nyyrox.aniverse.data.download

import android.app.Notification
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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DownloadNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.nyyrox.aniverse.ACTION_CANCEL_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD  = "com.nyyrox.aniverse.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.nyyrox.aniverse.ACTION_RESUME_DOWNLOAD"
        const val ACTION_RETRY_DOWNLOAD  = "com.nyyrox.aniverse.ACTION_RETRY_DOWNLOAD"
        const val ACTION_CANCEL_ALL      = "com.nyyrox.aniverse.ACTION_CANCEL_ALL"

        const val EXTRA_TYPE        = "extra_download_type"
        const val EXTRA_ITEM_ID     = "extra_item_id"
        const val EXTRA_CHAPTER_ID  = "extra_chapter_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CANCEL_DOWNLOAD -> {
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.ANIME.name
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: return
                val type = try { DownloadType.valueOf(typeStr) } catch (_: Exception) { DownloadType.ANIME }
                val taskKey = "$itemId:$chapterId"
                DownloadManager.instance?.cancelTask(type, itemId, chapterId)
                DownloadNotificationHelper.cancelNotification(context, taskKey)
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.ANIME.name
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: return
                val type = try { DownloadType.valueOf(typeStr) } catch (_: Exception) { DownloadType.ANIME }
                DownloadManager.instance?.pauseTask(type, itemId, chapterId)
            }
            ACTION_RESUME_DOWNLOAD -> {
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.ANIME.name
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: return
                val type = try { DownloadType.valueOf(typeStr) } catch (_: Exception) { DownloadType.ANIME }
                DownloadManager.instance?.resumeTask(type, itemId, chapterId)
            }
            ACTION_RETRY_DOWNLOAD -> {
                val typeStr = intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.ANIME.name
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                val chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: return
                val type = try { DownloadType.valueOf(typeStr) } catch (_: Exception) { DownloadType.ANIME }
                val taskKey = "$itemId:$chapterId"
                DownloadNotificationHelper.cancelNotification(context, taskKey)
                DownloadManager.instance?.retryTask(type, itemId, chapterId)
            }
            ACTION_CANCEL_ALL -> {
                DownloadManager.instance?.cancelAll()
                ZenkaiDownloadService.stop(context)
            }
        }
    }
}

object DownloadNotificationHelper {
    const val CHANNEL_PROGRESS_ID = "zenkai_anime_downloads"
    const val CHANNEL_PROGRESS_NAME = "Active Downloads"
    const val CHANNEL_PROGRESS_DESC = "Shows ongoing download progress, speeds, and pause/resume controls"

    const val CHANNEL_COMPLETE_ID = "zenkai_anime_downloads_complete"
    const val CHANNEL_COMPLETE_NAME = "Completed Downloads"
    const val CHANNEL_COMPLETE_DESC = "Notifies when downloads finish successfully, fail, or complete cleanups"

    const val NOTIFICATION_ID_DELETION = 2002

    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    private var lastUnifiedProgressUpdate = 0L

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

            // Channel 2: Completed / Failed / Cleanup (default importance)
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

    fun buildServiceNotification(context: Context, title: String, content: String): Notification {
        createNotificationChannels(context)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "downloads")
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            ZenkaiDownloadService.NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
            .setSmallIcon(R.drawable.ic_zenkai_notification)
            .setColor(0xFFE11D48.toInt())
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            .setContentIntent(pendingContentIntent)
            .build()
    }

    fun updateUnifiedForegroundNotification(context: Context, activeTasks: List<DownloadTaskInfo>) {
        if (!DownloadPreferences.isNotificationsEnabled(context) || activeTasks.isEmpty()) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastUnifiedProgressUpdate < 400L && activeTasks.none { it.progress >= 1f }) {
            return
        }
        lastUnifiedProgressUpdate = now

        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "downloads")
            }
            val pendingContentIntent = PendingIntent.getActivity(
                context,
                ZenkaiDownloadService.NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val primaryTask = activeTasks.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                ?: activeTasks.firstOrNull { it.status == DownloadStatus.PAUSED }
                ?: activeTasks.first()

            val isAllPaused = activeTasks.all { it.status == DownloadStatus.PAUSED }
            val totalActive = activeTasks.size
            val primaryProgressPct = (primaryTask.progress.coerceIn(0f, 1f) * 100).toInt()

            val titleText = when {
                isAllPaused && totalActive == 1 -> "Paused • ${primaryTask.itemTitle}"
                isAllPaused -> "Paused ($totalActive Items)"
                totalActive == 1 -> primaryTask.itemTitle
                else -> "Downloading $totalActive Items • ${primaryTask.itemTitle}"
            }

            val speedText = if (primaryTask.speed.isNotBlank() && primaryTask.status != DownloadStatus.PAUSED) " • ${primaryTask.speed}" else ""
            val sizeText = if (primaryTask.totalBytes > 0L) {
                val dlMb = primaryTask.downloadedBytes / (1024.0 * 1024.0)
                val totMb = primaryTask.totalBytes / (1024.0 * 1024.0)
                " • %.1f/%.1f MB".format(Locale.US, dlMb, totMb)
            } else ""

            val stateSuffix = if (primaryTask.status == DownloadStatus.PAUSED) " (Paused)" else ""
            val contentText = "${primaryTask.title}: $primaryProgressPct%$speedText$sizeText$stateSuffix"

            val bigStyle = NotificationCompat.BigTextStyle().bigText(
                buildString {
                    append(contentText)
                    if (totalActive > 1) {
                        append("\n\nQueue:")
                        activeTasks.drop(1).take(4).forEach { other ->
                            val statusDesc = when (other.status) {
                                DownloadStatus.QUEUED -> "Queued"
                                DownloadStatus.PAUSED -> "Paused"
                                DownloadStatus.DOWNLOADING -> "${(other.progress * 100).toInt()}%"
                                else -> other.status.name
                            }
                            append("\n• ${other.itemTitle} ${other.title} ($statusDesc)")
                        }
                        if (totalActive > 5) {
                            append("\n• +${totalActive - 5} more queued")
                        }
                    }
                }
            )

            // Pause / Resume action for the primary task
            val toggleActionIntent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = if (primaryTask.status == DownloadStatus.PAUSED) {
                    DownloadNotificationReceiver.ACTION_RESUME_DOWNLOAD
                } else {
                    DownloadNotificationReceiver.ACTION_PAUSE_DOWNLOAD
                }
                putExtra(DownloadNotificationReceiver.EXTRA_TYPE, primaryTask.type.name)
                putExtra(DownloadNotificationReceiver.EXTRA_ITEM_ID, primaryTask.itemId)
                putExtra(DownloadNotificationReceiver.EXTRA_CHAPTER_ID, primaryTask.chapterId)
            }
            val pendingToggleIntent = PendingIntent.getBroadcast(
                context,
                ZenkaiDownloadService.NOTIFICATION_ID + 400,
                toggleActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel action for the currently downloading item
            val cancelIntent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = DownloadNotificationReceiver.ACTION_CANCEL_DOWNLOAD
                putExtra(DownloadNotificationReceiver.EXTRA_TYPE, primaryTask.type.name)
                putExtra(DownloadNotificationReceiver.EXTRA_ITEM_ID, primaryTask.itemId)
                putExtra(DownloadNotificationReceiver.EXTRA_CHAPTER_ID, primaryTask.chapterId)
            }
            val pendingCancelIntent = PendingIntent.getBroadcast(
                context,
                ZenkaiDownloadService.NOTIFICATION_ID + 500,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isIndeterminate = primaryTask.status == DownloadStatus.QUEUED ||
                    (primaryTask.progress <= 0f && primaryTask.status == DownloadStatus.DOWNLOADING)

            val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(if (isAllPaused) 0xFFF59E0B.toInt() else 0xFFE11D48.toInt())
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setStyle(bigStyle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, primaryProgressPct, isIndeterminate)
                .setContentIntent(pendingContentIntent)
                .addAction(
                    if (primaryTask.status == DownloadStatus.PAUSED) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                    if (primaryTask.status == DownloadStatus.PAUSED) "Resume" else "Pause",
                    pendingToggleIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    if (totalActive > 1) "Cancel Item" else "Cancel",
                    pendingCancelIntent
                )

            notificationManager.notify(ZenkaiDownloadService.NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    fun updateProgressNotification(context: Context, task: AnimeDownloadTask) {
        ZenkaiDownloadService.start(context)

        val dm = DownloadManager.instance ?: return
        val activeTasks = dm.tasks.value.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
        }
        if (activeTasks.isNotEmpty()) {
            updateUnifiedForegroundNotification(context, activeTasks)
        }
    }

    fun showCompleteNotification(context: Context, task: AnimeDownloadTask) {
        val taskKey = "${task.animeId}:${task.episode}"
        lastUpdateTimes.remove(taskKey)

        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val notifId = getNotificationId(taskKey)

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
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${task.animeTitle}\n$epLabel (${task.quality} • ${task.audio})\nReady for offline playback.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)

            val playIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "downloads")
                putExtra("action", "PLAY_OFFLINE")
                putExtra("anime_id", task.animeId)
                putExtra("episode", task.episode)
                putExtra("file_path", task.localFilePath)
            }
            val pendingPlayIntent = PendingIntent.getActivity(
                context,
                notifId + 200000,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play Now",
                pendingPlayIntent
            )

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
                .setContentTitle("Download Interrupted: ${task.animeTitle}")
                .setContentText("$epLabel: $reason")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${task.animeTitle} - $epLabel\nDownload paused: $reason\nTap Retry to resume downloading.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingContentIntent)

            val retryIntent = Intent(context, DownloadNotificationReceiver::class.java).apply {
                action = DownloadNotificationReceiver.ACTION_RETRY_DOWNLOAD
                putExtra(DownloadNotificationReceiver.EXTRA_TYPE, DownloadType.ANIME.name)
                putExtra(DownloadNotificationReceiver.EXTRA_ITEM_ID, task.animeId.toString())
                putExtra(DownloadNotificationReceiver.EXTRA_CHAPTER_ID, task.episode.toString())
            }
            val pendingRetryIntent = PendingIntent.getBroadcast(
                context,
                notifId + 300000,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_rotate,
                "Retry",
                pendingRetryIntent
            )

            notificationManager.notify(notifId, builder.build())
        } catch (_: Exception) {}
    }

    fun showDeletionNotification(
        context: Context,
        title: String,
        deletedCount: Int,
        totalCount: Int,
        isFinished: Boolean
    ) {
        try {
            createNotificationChannels(context)
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            if (isFinished) {
                val finishNotif = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
                    .setSmallIcon(R.drawable.ic_zenkai_notification)
                    .setColor(0xFF10B981.toInt())
                    .setContentTitle("Deleted All: $title")
                    .setContentText("Successfully deleted $totalCount items and freed up storage.")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setTimeoutAfter(4000L)
                    .build()
                notificationManager.notify(NOTIFICATION_ID_DELETION, finishNotif)
                return
            }

            val progressPct = if (totalCount > 0) ((deletedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0
            val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(0xFFEF4444.toInt())
                .setContentTitle("Deleting $title")
                .setContentText("Deleted $deletedCount/$totalCount ($progressPct%)")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(totalCount, deletedCount, false)

            notificationManager.notify(NOTIFICATION_ID_DELETION, builder.build())
        } catch (_: Exception) {}
    }

    fun cancelDeletionNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID_DELETION)
        } catch (_: Exception) {}
    }

    fun cancelAllNotificationsForAnime(context: Context, animeId: Int, episodes: List<Int> = emptyList()) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            episodes.forEach { ep ->
                val taskKey = "$animeId:$ep"
                lastUpdateTimes.remove(taskKey)
                notificationManager.cancel(getNotificationId(taskKey))
            }
        } catch (_: Exception) {}
    }

    fun cancelAllNotificationsForManga(context: Context, mangaId: String, chapters: List<String> = emptyList()) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            chapters.forEach { ch ->
                val taskKey = "$mangaId:$ch"
                lastUpdateTimes.remove(taskKey)
                notificationManager.cancel(getNotificationId(taskKey))
            }
        } catch (_: Exception) {}
    }

    fun cancelAllNotificationsForNovel(context: Context, novelSlug: String, chapters: List<String> = emptyList()) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            chapters.forEach { ch ->
                val taskKey = "$novelSlug:$ch"
                lastUpdateTimes.remove(taskKey)
                notificationManager.cancel(getNotificationId(taskKey))
            }
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
