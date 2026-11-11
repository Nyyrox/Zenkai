package com.nyyrox.aniverse.data.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Robust Foreground Service for Zenkai Downloads.
 * Keeps CPU and Wi-Fi active in the background, preventing Android from freezing
 * or killing downloading episodes when the screen turns off or Battery Saver is enabled.
 */
class ZenkaiDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val ACTION_START = "com.nyyrox.aniverse.action.START_DOWNLOAD_SERVICE"
        const val ACTION_STOP = "com.nyyrox.aniverse.action.STOP_DOWNLOAD_SERVICE"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            try {
                val intent = Intent(context, ZenkaiDownloadService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ZenkaiDownloadService", "Failed to start download foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, ZenkaiDownloadService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("ZenkaiDownloadService", "Failed to stop download foreground service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireLocks()
        startInForeground()
        observeActiveTasks()
    }

    private fun acquireLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null && pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zenkai:DownloadServiceWakeLock").apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L) // 12 hours safety timeout
                }
            }
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiLock == null && wm != null) {
                @Suppress("DEPRECATION")
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "Zenkai:DownloadServiceWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.w("ZenkaiDownloadService", "Could not acquire wake/wifi locks: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            wifiLock?.let { if (it.isHeld) it.release() }
            wifiLock = null
        } catch (e: Exception) {
            Log.w("ZenkaiDownloadService", "Error releasing locks: ${e.message}")
        }
    }

    private fun startInForeground() {
        try {
            DownloadNotificationHelper.createNotificationChannels(this)
            val initialNotif = DownloadNotificationHelper.buildServiceNotification(
                context = this,
                title = "Zenkai Downloads",
                content = "Downloading in background..."
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotif)
            }
        } catch (e: Exception) {
            Log.e("ZenkaiDownloadService", "Could not start in foreground: ${e.message}", e)
        }
    }

    private fun observeActiveTasks() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            val dm = DownloadManager.instance ?: return@launch
            var hadActiveTasks = false
            dm.tasks.collectLatest { taskList ->
                val activeTasks = taskList.filter {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
                }
                if (activeTasks.isEmpty()) {
                    if (hadActiveTasks) {
                        kotlinx.coroutines.delay(3000L)
                        val recheck = dm.tasks.value.any {
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.PAUSED
                        }
                        if (!recheck) {
                            releaseLocks()
                            try {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } catch (_: Exception) {}
                            stopSelf()
                        }
                    }
                } else {
                    hadActiveTasks = true
                    val anyActive = activeTasks.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                    if (anyActive) {
                        acquireLocks()
                    } else {
                        releaseLocks()
                    }
                    DownloadNotificationHelper.updateUnifiedForegroundNotification(this@ZenkaiDownloadService, activeTasks)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseLocks()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                acquireLocks()
                observeActiveTasks()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        releaseLocks()
        super.onDestroy()
    }
}
