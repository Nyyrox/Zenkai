package com.nyyrox.aniverse.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.nyyrox.aniverse.R
import com.nyyrox.aniverse.data.UserProfile
import com.nyyrox.aniverse.data.supabase.SupabaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TARGET_USER_ID = "extra_target_user_id"
        const val EXTRA_TARGET_NAME = "extra_target_name"
        const val EXTRA_CHANNEL = "extra_channel"
        const val EXTRA_REPLY_TO_ID = "extra_reply_to_id"
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val targetUserId = intent.getStringExtra(EXTRA_TARGET_USER_ID) ?: ""
        val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: "Otaku"
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: ""
        val replyToId = intent.getStringExtra(EXTRA_REPLY_TO_ID) ?: ""
        val notifType = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: if (channel.startsWith("dm_")) "dm" else "mention"

        if (replyText.isNullOrBlank() || targetUserId.isBlank()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabaseRepo = SupabaseRepository()
                val storageManager = com.nyyrox.aniverse.data.StorageManager(context)
                val savedProfile = storageManager.getUserProfile()
                val currentUserId = supabaseRepo.getCurrentUserId() ?: savedProfile.id?.takeIf { it.isNotBlank() }
                if (currentUserId != null) {
                    val myProfile = try { supabaseRepo.getProfile(currentUserId) } catch (_: Exception) { null }
                    val senderProfile = UserProfile(
                        id = currentUserId,
                        displayName = myProfile?.displayName?.ifBlank { null } ?: savedProfile.displayName.ifBlank { "Otaku" },
                        username = myProfile?.username ?: savedProfile.username,
                        avatarUrl = myProfile?.avatarUrl ?: savedProfile.avatarUrl,
                        avatarDecorationId = myProfile?.avatarDecoration ?: savedProfile.avatarDecorationId,
                        avatarFrameId = myProfile?.avatarFrame ?: savedProfile.avatarFrameId,
                        textEffectId = myProfile?.textEffect ?: savedProfile.textEffectId,
                        streakCount = myProfile?.streakCount ?: savedProfile.streakCount
                    )

                    if (notifType == "dm" || channel.startsWith("dm_")) {
                        supabaseRepo.sendDirectMessage(
                            targetUserId = targetUserId,
                            text = replyText,
                            senderProfile = senderProfile
                        )
                    } else {
                        // Community mention / reply: Post directly into the respective channel!
                        val targetChannel = if (channel.isNotBlank() && !channel.startsWith("dm_")) channel else "general"
                        supabaseRepo.sendCommunityMessage(
                            channel = targetChannel,
                            messageText = replyText,
                            profile = senderProfile,
                            replyToId = replyToId.ifBlank { null },
                            replyToUsername = targetName.ifBlank { null },
                            replyToUserId = targetUserId.ifBlank { null },
                            isMention = true
                        )
                    }
                }

                // Update notification in shade to show confirmation
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && notifId != 0) {
                    val confirmTitle = if (notifType == "dm" || channel.startsWith("dm_")) {
                        "💬 Replied to $targetName (DM)"
                    } else {
                        "💬 Replied to $targetName in #${if (channel.isNotBlank()) channel else "general"}"
                    }
                    val repliedNotification = NotificationCompat.Builder(context, ZenkaiNotificationHelper.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_zenkai_notification)
                        .setColor(0xFFE11D48.toInt())
                        .setContentTitle(confirmTitle)
                        .setContentText("Sent: \"$replyText\"")
                        .setTimeoutAfter(3000)
                        .setAutoCancel(true)
                        .build()
                    nm.notify(notifId, repliedNotification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
