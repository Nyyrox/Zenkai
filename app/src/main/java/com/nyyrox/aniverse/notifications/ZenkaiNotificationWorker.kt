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
            val currentUserId = client.auth.currentUserOrNull()?.id ?: return Result.success()

            val notifications = repository.fetchNotifications(currentUserId)
            if (notifications.isEmpty()) {
                return Result.success()
            }

            val prefs = applicationContext.getSharedPreferences("zenkai_notifs_cache", Context.MODE_PRIVATE)
            var newlyShown = 0
            for (notif in notifications) {
                val forMe = notif.userId == null || notif.userId == currentUserId
                val isAlreadyNotified = prefs.getBoolean("notified_${notif.id}", false)
                if (forMe && !notif.isRead && !isAlreadyNotified) {
                    if (currentUserId.isNullOrBlank() || notif.senderId != currentUserId) {
                        prefs.edit().putBoolean("notified_${notif.id}", true).commit()
                        ZenkaiNotificationHelper.showNotification(applicationContext, notif)
                        newlyShown++
                        if (newlyShown >= 5) break // Cap per background cycle
                    }
                }
            }

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
