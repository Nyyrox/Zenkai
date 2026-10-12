package com.nyyrox.aniverse.data.download

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class DownloadStorageLocation {
    INTERNAL,        // App internal private storage: /data/user/0/.../files/downloads/anime
    EXTERNAL_PUBLIC  // Device public storage: /storage/emulated/0/Movies/Zenkai
}

data class DownloadSettings(
    val maxConcurrentDownloads: Int = 2,
    val storageLocation: DownloadStorageLocation = DownloadStorageLocation.INTERNAL,
    val notificationsEnabled: Boolean = true
)

object DownloadPreferences {
    private const val PREFS_NAME = "zenkai_download_prefs"
    private const val KEY_MAX_CONCURRENT = "max_concurrent_downloads"
    private const val KEY_STORAGE_LOCATION = "storage_location"
    private const val KEY_NOTIFICATIONS_ENABLED = "download_notifications_enabled"

    private val _settings = MutableStateFlow(DownloadSettings())
    val settings: StateFlow<DownloadSettings> = _settings.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val maxConcurrent = prefs.getInt(KEY_MAX_CONCURRENT, 2).coerceIn(1, 5)
            val locStr = prefs.getString(KEY_STORAGE_LOCATION, DownloadStorageLocation.INTERNAL.name)
                ?: DownloadStorageLocation.INTERNAL.name
            val location = try {
                DownloadStorageLocation.valueOf(locStr)
            } catch (_: Exception) {
                DownloadStorageLocation.INTERNAL
            }
            val notifEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

            _settings.value = DownloadSettings(
                maxConcurrentDownloads = maxConcurrent,
                storageLocation = location,
                notificationsEnabled = notifEnabled
            )
            isInitialized = true
        }
    }

    fun getMaxConcurrentDownloads(context: Context): Int {
        init(context)
        return _settings.value.maxConcurrentDownloads
    }

    fun setMaxConcurrentDownloads(context: Context, value: Int) {
        init(context)
        val clamped = value.coerceIn(1, 5)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MAX_CONCURRENT, clamped).apply()
        _settings.value = _settings.value.copy(maxConcurrentDownloads = clamped)
    }

    fun getStorageLocation(context: Context): DownloadStorageLocation {
        init(context)
        return _settings.value.storageLocation
    }

    fun setStorageLocation(context: Context, location: DownloadStorageLocation) {
        init(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STORAGE_LOCATION, location.name).apply()
        _settings.value = _settings.value.copy(storageLocation = location)
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        init(context)
        return _settings.value.notificationsEnabled
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        init(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    /**
     * Resolves the primary directory where anime downloads should be saved.
     * If EXTERNAL_PUBLIC is chosen, creates/uses Movies/Zenkai.
     * If external storage is unavailable/unwritable, safely falls back to app internal storage.
     */
    fun getAnimeDownloadDir(context: Context): File {
        init(context)
        return if (_settings.value.storageLocation == DownloadStorageLocation.EXTERNAL_PUBLIC) {
            getPublicMoviesDir(context)
        } else {
            getInternalAnimeDir(context)
        }
    }

    fun getInternalAnimeDir(context: Context): File {
        return File(context.filesDir, "downloads/anime").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getPublicMoviesDir(context: Context): File {
        return try {
            val publicMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val zenkaiDir = File(publicMovies, "Zenkai")
            if (!zenkaiDir.exists()) zenkaiDir.mkdirs()
            if (zenkaiDir.canWrite()) {
                zenkaiDir
            } else {
                // Fallback to app-specific external files dir
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(context.filesDir, "downloads/anime")
                File(extDir, "Zenkai").apply { if (!exists()) mkdirs() }
            }
        } catch (_: Exception) {
            getInternalAnimeDir(context)
        }
    }

    /**
     * Returns all storage directories where downloaded anime might exist
     * so that the offline library discovers anime downloaded in internal or external storage.
     */
    fun getAllAnimeDownloadDirs(context: Context): List<File> {
        val list = mutableListOf<File>()
        val internal = getInternalAnimeDir(context)
        list.add(internal)

        try {
            val publicMovies = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Zenkai")
            if (publicMovies.exists() && publicMovies.isDirectory && publicMovies.absolutePath != internal.absolutePath) {
                list.add(publicMovies)
            }
        } catch (_: Exception) {}

        try {
            val extFiles = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            if (extFiles != null) {
                val zenkaiExt = File(extFiles, "Zenkai")
                if (zenkaiExt.exists() && zenkaiExt.isDirectory && !list.any { it.absolutePath == zenkaiExt.absolutePath }) {
                    list.add(zenkaiExt)
                }
            }
        } catch (_: Exception) {}

        return list
    }
}
