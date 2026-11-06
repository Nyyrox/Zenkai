package com.nyyrox.aniverse.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ==========================================
// DESIGN TOKENS (Strict Black-and-White)
// ==========================================
val PlayerBg = Color(0xFF000000)
val PlayerTextActive = Color(0xFFFFFFFF)
val PlayerMuted = Color(0xFF8A8A8A)
val PlayerFaint = Color(0xFF4A4A4A)
val PlayerHairline = Color(0xFF1F1F1F)
val PlayerSheet = Color(0xFF0A0A0A)
val PlayerToggleTrack = Color(0xFF151515)
val PlayerScrim = Color(0x99000000) // 60% Black
val PlayerActiveRowGradientStart = Color(0x0AFFFFFF)

fun isAnimationScaleZero(context: Context): Boolean {
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }
}

@Immutable
data class EpisodeItem(
    val n: Int,
    val title: String,
    val durationText: String = "24m",
    val thumbnailUrl: String? = null,
    val isWatched: Boolean = false,
    val progressFraction: Float = 0f
)

@Immutable
data class ServerItem(
    val id: String,
    val name: String,
    val isAvailable: Boolean = true,
    val isHd: Boolean = true
)

enum class AudioType {
    SUB, DUB
}

@Immutable
data class EpisodeRange(
    val start: Int,
    val end: Int
) {
    val label: String get() = "$start–$end"
}

@Immutable
data class PlayerUiState(
    val title: String = "",
    val ep: Int = 1,
    val currentEpisodeTitle: String = "",
    val metaScore: String = "★ 87%",
    val metaFormat: String = "TV",
    val metaDuration: String = "24m",
    val synopsis: String = "",
    val eps: List<EpisodeItem> = emptyList(),
    val servers: List<ServerItem> = emptyList(),
    val serverIdx: Int = 0,
    val audio: AudioType = AudioType.SUB,
    val isDubAvailable: Boolean = true,
    val availableRanges: List<EpisodeRange> = emptyList(),
    val selectedRange: EpisodeRange = EpisodeRange(1, 25),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isServerLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val controlsVisible: Boolean = true,
    val locked: Boolean = false,
    val playbackSpeed: Float = 1f,
    val isPipSupported: Boolean = false,
    val isFullscreen: Boolean = false,
    val areAllServersFailed: Boolean = false
) {
    val currentServer: ServerItem?
        get() = servers.getOrNull(serverIdx)
}

interface PlayerActions {
    fun onTogglePlayPause()
    fun onSeekTo(fraction: Float)
    fun onSeekRelative(seconds: Int)
    fun onSelectEpisode(episodeNumber: Int)
    fun onSelectServer(index: Int)
    fun onSelectAudio(audio: AudioType)
    fun onSelectRange(range: EpisodeRange)
    fun onToggleControls()
    fun onSetControlsVisible(visible: Boolean)
    fun onToggleLock()
    fun onSetSpeed(speed: Float)
    fun onToggleFullscreen()
    fun onEnterPip()
    fun onOpenQualitySettings()
    fun onOpenSettings()
    fun onDownloadAll()
    fun onDownloadEpisode(episodeNumber: Int)
    fun onMarkWatched(episodeNumber: Int)
    fun onShareEpisode(episodeNumber: Int)
    fun onRetryServer()
    fun onMinimize()
    fun onBack()
}
