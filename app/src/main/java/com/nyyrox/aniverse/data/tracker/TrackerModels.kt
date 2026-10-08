package com.nyyrox.aniverse.data.tracker

data class AniListAccount(
    val userId: Int,
    val username: String,
    val avatarUrl: String? = null,
    val accessToken: String,
    val lastSyncedAt: Long = 0L
)

data class MalAccount(
    val username: String,
    val avatarUrl: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long = 0L,
    val lastSyncedAt: Long = 0L
)

data class TrackerSettings(
    val isAutoSyncEnabled: Boolean = true,
    val syncOnEpisodeComplete: Boolean = true,
    val lastFullSyncTimestamp: Long = 0L
)

data class TrackerSyncResult(
    val totalPulled: Int = 0,
    val updatedLocal: Int = 0,
    val errors: List<String> = emptyList()
)

object TrackerStatusMapper {
    fun zenkaiToAniList(status: String): String {
        return when (status.lowercase()) {
            "watching" -> "CURRENT"
            "completed" -> "COMPLETED"
            "on_hold", "paused" -> "PAUSED"
            "dropped" -> "DROPPED"
            else -> "PLANNING"
        }
    }

    fun aniListToZenkai(status: String?): String {
        return when (status?.uppercase()) {
            "CURRENT" -> "watching"
            "COMPLETED" -> "completed"
            "PAUSED" -> "on_hold"
            "DROPPED" -> "dropped"
            "PLANNING" -> "plan_to_watch"
            else -> "plan_to_watch"
        }
    }

    fun zenkaiToMal(status: String): String {
        return when (status.lowercase()) {
            "watching" -> "watching"
            "completed" -> "completed"
            "on_hold", "paused" -> "on_hold"
            "dropped" -> "dropped"
            else -> "plan_to_watch"
        }
    }

    fun malToZenkai(status: String?): String {
        return when (status?.lowercase()) {
            "watching" -> "watching"
            "completed" -> "completed"
            "on_hold" -> "on_hold"
            "dropped" -> "dropped"
            "plan_to_watch" -> "plan_to_watch"
            else -> "plan_to_watch"
        }
    }
}