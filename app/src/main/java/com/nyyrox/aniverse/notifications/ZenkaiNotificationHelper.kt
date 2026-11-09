package com.nyyrox.aniverse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.nyyrox.aniverse.MainActivity
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.data.supabase.AppNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ZenkaiNotificationHelper {
    const val CHANNEL_ID = "zenkai_notifications_v2"
    const val CHANNEL_NAME = "ZENKAI Alerts & Mentions"
    const val CHANNEL_DESC = "Real-time alerts for community mentions, replies, announcements and direct messages"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    suspend fun showNotification(context: Context, notification: AppNotification) {
        try {
            // Strictly enforce show-once per notification ID across APK restarts
            if (notification.id.isNotBlank() && !notification.id.startsWith("test_")) {
                val prefs = context.getSharedPreferences("zenkai_notifs_cache", Context.MODE_PRIVATE)
                if (prefs.getBoolean("notified_${notification.id}", false)) {
                    return
                }
                prefs.edit().putBoolean("notified_${notification.id}", true).commit()
            }

            createNotificationChannel(context)

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (!notificationManagerCompat.areNotificationsEnabled()) {
                return
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                val routeName = when (notification.type) {
                    "dm" -> "direct_chat"
                    "mention", "reply" -> "community"
                    else -> "notifications"
                }
                putExtra("route", routeName)
                putExtra("notification_id", notification.id)
                if (!notification.channel.isNullOrBlank()) {
                    putExtra("channel", notification.channel)
                }
                val partnerId = notification.senderId ?: notification.targetId
                if (!partnerId.isNullOrBlank()) {
                    putExtra("partner_user_id", partnerId)
                    putExtra("target_id", partnerId)
                }
                if (!notification.actionUrl.isNullOrBlank()) {
                    putExtra("action_url", notification.actionUrl)
                }
            }

            val notifId = if (notification.id.isNotBlank()) {
                notification.id.hashCode()
            } else {
                (System.currentTimeMillis().toInt() and 0x7FFFFFFF)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val avatarBitmap: Bitmap? = if (!notification.senderAvatar.isNullOrBlank() || notification.id.startsWith("welcome_")) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val model: Any = when {
                            !notification.senderAvatar.isNullOrBlank() && !notification.senderAvatar.contains("welcome_nano_banana") -> notification.senderAvatar
                            notification.senderAvatar?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> R.drawable.welcome_nano_banana
                            else -> notification.senderAvatar ?: ""
                        }
                        val request = ImageRequest.Builder(context)
                            .data(model)
                            .build()
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            result.image.toBitmap()
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            } else null

            val imageBitmap: Bitmap? = if (!notification.imageUrl.isNullOrBlank() || notification.id.startsWith("welcome_")) {
                withContext(Dispatchers.IO) {
                    try {
                        val loader = ImageLoader(context)
                        val model: Any = when {
                            !notification.imageUrl.isNullOrBlank() && !notification.imageUrl.contains("welcome_nano_banana") -> notification.imageUrl
                            notification.imageUrl?.contains("welcome_nano_banana") == true || notification.id.startsWith("welcome_") -> R.drawable.welcome_nano_banana
                            else -> notification.imageUrl ?: ""
                        }
                        val request = ImageRequest.Builder(context)
                            .data(model)
                            .build()
                        val result = loader.execute(request)
                        if (result is SuccessResult) {
                            result.image.toBitmap()
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            } else null

            val title = when {
                notification.title.isNotBlank() -> notification.title
                notification.type == "dm" -> "💬 ${notification.senderName ?: "Otaku"} (Direct Message)"
                notification.type == "mention" -> "💬 ${notification.senderName ?: "Someone"} mentioned you"
                notification.type == "reply" -> "↩️ ${notification.senderName ?: "Someone"} replied to you"
                notification.type == "reward" -> "🎁 Special Reward Available!"
                notification.type == "announcement" -> "📢 ZENKAI Announcement"
                notification.type == "admin" -> "🛡️ Message from ZENKAI Staff"
                notification.type == "update" -> "⚡ New Update Available"
                else -> "🔔 ZENKAI Notification"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_zenkai_notification)
                .setColor(0xFFE11D48.toInt())
                .setContentTitle(title)
                .setContentText(notification.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Direct Reply action from notification shade / lockscreen
            if (notification.type == "dm" || notification.type == "mention" || notification.type == "reply") {
                val targetUserId = notification.senderId ?: notification.targetId
                if (!targetUserId.isNullOrBlank()) {
                    val remoteInput = androidx.core.app.RemoteInput.Builder(DirectReplyReceiver.KEY_TEXT_REPLY)
                        .setLabel("Reply to ${notification.senderName ?: "Otaku"}...")
                        .build()

                    val replyIntent = Intent(context, DirectReplyReceiver::class.java).apply {
                        putExtra(DirectReplyReceiver.EXTRA_NOTIFICATION_ID, notifId)
                        putExtra(DirectReplyReceiver.EXTRA_TARGET_USER_ID, targetUserId)
                        putExtra(DirectReplyReceiver.EXTRA_TARGET_NAME, notification.senderName ?: "Otaku")
                        putExtra(DirectReplyReceiver.EXTRA_CHANNEL, notification.channel ?: "")
                        putExtra(DirectReplyReceiver.EXTRA_REPLY_TO_ID, notification.targetId ?: "")
                        putExtra(DirectReplyReceiver.EXTRA_NOTIF_TYPE, notification.type)
                    }

                    val replyPendingIntent = PendingIntent.getBroadcast(
                        context,
                        notifId,
                        replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )

                    val replyAction = NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_send,
                        "Reply",
                        replyPendingIntent
                    )
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build()

                    builder.addAction(replyAction)
                }
            }

            if (imageBitmap != null) {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(imageBitmap)
                        .setBigContentTitle(title)
                        .setSummaryText(notification.message)
                )
            } else {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notification.message)
                        .setSummaryText(notification.senderName ?: "ZENKAI")
                )
            }

            if (avatarBitmap != null) {
                builder.setLargeIcon(avatarBitmap)
            }

            notificationManagerCompat.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ permission not yet granted by user
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
