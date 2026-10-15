package com.nyyrox.aniverse.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nyyrox.aniverse.data.supabase.SupabaseManager
import com.nyyrox.aniverse.data.supabase.SupabaseRepository
import io.github.jan.supabase.auth.auth
import java.util.concurrent.TimeUnit

import androidx.work.OneTimeWorkRequestBuilder

class ZenkaiNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = SupabaseRepository()

    override suspend fun doWork(): Result {
        return try {
            val client = SupabaseManager.client
            val currentUserId = client.auth.currentUserOrNull()?.id
                ?: com.nyyrox.aniverse.data.StorageManager(applicationContext).getUserProfile().id

            val notifications = repository.fetchNotifications(currentUserId)
            if (notifications.isEmpty()) {
                return Result.success()
            }

            val prefs = applicationContext.getSharedPreferences("zenkai_notifs_cache", Context.MODE_PRIVATE)
            val notifiedIds = prefs.getStringSet("notified_ids", emptySet()) ?: emptySet()
            val newNotifiedIds = notifiedIds.toMutableSet()

            var newlyShown = 0
            for (notif in notifications) {
                val forMe = notif.userId == null || notif.userId == currentUserId
                // Only notify for unread notifications that have not yet been posted to status bar
                if (forMe && !notif.isRead && !notifiedIds.contains(notif.id)) {
                    // Don't notify if the notification is from the user themselves
                    if (currentUserId.isNullOrBlank() || notif.senderId != currentUserId) {
                        ZenkaiNotificationHelper.showNotification(applicationContext, notif)
                        newNotifiedIds.add(notif.id)
                        newlyShown++
                        if (newlyShown >= 5) break // Cap per background cycle
                    }
                }
            }

            // Save updated set
            prefs.edit().putStringSet("notified_ids", newNotifiedIds).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "zenkai_background_notifications_work"

        fun enqueuePeriodicWork(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<ZenkaiNotificationWorker>(
                    15, TimeUnit.MINUTES,
                    5, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun enqueueOneTimeWork(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ZenkaiNotificationWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
