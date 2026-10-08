package com.nyyrox.aniverse.player

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.media3.exoplayer.ExoPlayer
import com.nyyrox.aniverse.service.ActivePlayerHolder
import com.nyyrox.aniverse.service.PlayerNotificationService

/**
 * Owns the ExoPlayer after the user leaves PlayerScreen so playback continues in the mini player.
 * PlayerScreen sets [parkRequest] on back; CinePlayerView parks its player instead of releasing it.
 * Re-opening the same episode adopts the parked player (no reload).
 */
object MiniPlayerManager {

    class ParkRequest(val routeAnimeId: Int, val episode: Int, val providerId: String?) {
        val at: Long = SystemClock.elapsedRealtime()
    }

    class Parked(
        val player: ExoPlayer,
        val routeAnimeId: Int,
        val anilistId: Int,
        val episode: Int,
        val providerId: String?,
        val streamUrl: String,
        val sniffed: SniffedStream,
        val title: String,
        val subtitle: String,
        val onProgress: (Long, Long) -> Unit
    )

    var parked by mutableStateOf<Parked?>(null)
        private set

    /** True while PlayerScreen is on screen; mini player hides. */
    var fullScreenOpen by mutableStateOf(false)

    /** Video box of the full player page (root coords); the mini player flies to/from it. */
    var videoSlot by mutableStateOf<Rect?>(null)

    /** True while the mini player is flying into the player page; the page mounts its player after. */
    var expanding by mutableStateOf(false)

    /** True while handoff crossfade (mini -> full) is animating. */
    var isHandingOff by mutableStateOf(false)

    /** Until this time a stream-URL mismatch is tolerated when adopting (mini -> full expand). */
    var handoffUntil = 0L

    var parkRequest: ParkRequest? = null

    /** Registered by App(): navigate to the player route. */
    var onExpand: ((routeAnimeId: Int, episode: Int, providerId: String?) -> Unit)? = null

    fun freshRequest(): ParkRequest? =
        parkRequest?.takeIf { SystemClock.elapsedRealtime() - it.at < 2000L }

    fun park(p: Parked) {
        parked?.let { if (it.player !== p.player) destroy(it) }
        parkRequest = null
        isHandingOff = false
        parked = p
    }

    private fun matches(p: Parked, anilistId: Int, episode: Int, streamUrl: String) =
        p.anilistId == anilistId && p.episode == episode &&
            (p.streamUrl == streamUrl || SystemClock.elapsedRealtime() < handoffUntil)

    fun peek(anilistId: Int, episode: Int, streamUrl: String): Parked? =
        parked?.takeIf { matches(it, anilistId, episode, streamUrl) }

    /** Takes the parked player if it matches, marking handoff active so mini player fades seamlessly. */
    fun claim(anilistId: Int, episode: Int, streamUrl: String): Parked? {
        val p = parked ?: return null
        if (matches(p, anilistId, episode, streamUrl)) {
            isHandingOff = true
            return p
        }
        parked = null
        isHandingOff = false
        destroy(p)
        return null
    }

    fun finishHandoff() {
        isHandingOff = false
        parked = null
    }

    fun release(context: Context? = null) {
        val p = parked ?: return
        parked = null
        isHandingOff = false
        destroy(p)
        context?.let {
            try {
                it.startService(
                    Intent(it, PlayerNotificationService::class.java)
                        .apply { action = PlayerNotificationService.ACTION_DETACH }
                )
            } catch (_: Exception) {}
        }
    }

    private fun destroy(p: Parked) {
        if (ActivePlayerHolder.player === p.player) ActivePlayerHolder.detach()
        try {
            p.player.stop()
            p.player.release()
        } catch (_: Exception) {}
    }
}
