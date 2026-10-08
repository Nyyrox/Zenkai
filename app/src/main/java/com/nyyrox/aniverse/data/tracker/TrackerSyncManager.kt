package com.nyyrox.aniverse.data.tracker

import android.util.Log
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.data.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TrackerSyncManager {
    private const val TAG = "TrackerSyncManager"

    suspend fun pullFromTrackers(storage: StorageManager): TrackerSyncResult = withContext(Dispatchers.IO) {
        val aniListAcc = storage.getAniListAccount()
        val malAcc = storage.getMalAccount()

        var totalPulled = 0
        var updatedCount = 0
        val errors = mutableListOf<String>()

        val remoteItems = mutableListOf<WatchlistItem>()

        // 1. Pull AniList
        if (aniListAcc != null && (aniListAcc.accessToken.isNotBlank() || aniListAcc.userId > 0)) {
            try {
                val token = aniListAcc.accessToken.takeIf { it.isNotBlank() }
                val aniListItems = AniListTrackerClient.fetchUserAnimeList(aniListAcc.userId, token)
                remoteItems.addAll(aniListItems)
                totalPulled += aniListItems.size
                storage.saveAniListAccount(aniListAcc.copy(lastSyncedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error pulling from AniList", e)
                errors.add("AniList: ${e.message ?: "Sync error"}")
            }
        }

        // 2. Pull MyAnimeList
        if (malAcc != null && (malAcc.accessToken.isNotBlank() || malAcc.username.isNotBlank())) {
            try {
                val malItems = if (malAcc.accessToken.isNotBlank()) {
                    MalTrackerClient.fetchUserAnimeList(malAcc.accessToken)
                } else {
                    MalTrackerClient.fetchUserAnimeListByUsername(malAcc.username)
                }
                remoteItems.addAll(malItems)
                totalPulled += malItems.size
                storage.saveMalAccount(malAcc.copy(lastSyncedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error pulling from MAL", e)
                errors.add("MAL: ${e.message ?: "Sync error"}")
            }
        }

        if (remoteItems.isEmpty()) {
            return@withContext TrackerSyncResult(totalPulled, updatedCount, errors)
        }

        // 3. Merge into local watchlist
        val localList = storage.getWatchlist().toMutableList()
        val localMap = localList.associateBy { it.animeId }.toMutableMap()

        for (remote in remoteItems) {
            val existing = localMap[remote.animeId]
            if (existing == null) {
                localMap[remote.animeId] = remote
                updatedCount++
            } else {
                val mergedProgress = maxOf(existing.progressEpisode, remote.progressEpisode)
                val mergedStatus = if (remote.updatedAt >= existing.updatedAt) remote.status else existing.status
                val mergedScore = remote.score ?: existing.score
                val mergedItem = existing.copy(
                    status = mergedStatus,
                    progressEpisode = mergedProgress,
                    score = mergedScore,
                    updatedAt = maxOf(existing.updatedAt, remote.updatedAt)
                )
                localMap[remote.animeId] = mergedItem
                if (mergedItem != existing) {
                    updatedCount++
                }
            }
        }

        val finalWatchlist = localMap.values.sortedByDescending { it.updatedAt }
        storage.saveWatchlist(finalWatchlist)
        storage.saveTrackerSettings(storage.getTrackerSettings().copy(lastFullSyncTimestamp = System.currentTimeMillis()))

        TrackerSyncResult(totalPulled, updatedCount, errors)
    }

    suspend fun pushWatchlistUpdate(
        storage: StorageManager,
        animeId: Int,
        malId: Int? = null,
        status: String,
        progress: Int,
        score: Double? = null
    ) = withContext(Dispatchers.IO) {
        val settings = storage.getTrackerSettings()
        if (!settings.isAutoSyncEnabled) return@withContext

        val aniListAcc = storage.getAniListAccount()
        if (aniListAcc != null && aniListAcc.accessToken.isNotBlank()) {
            try {
                AniListTrackerClient.saveMediaEntry(
                    mediaId = animeId,
                    status = status,
                    progress = progress,
                    score = score,
                    token = aniListAcc.accessToken
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed auto-push to AniList for anime $animeId", e)
            }
        }

        val malAcc = storage.getMalAccount()
        val targetMalId = malId ?: animeId
        if (malAcc != null && malAcc.accessToken.isNotBlank() && targetMalId > 0) {
            try {
                val scoreTen = score?.let { (it / 10.0).toInt().coerceIn(0, 10) }
                MalTrackerClient.updateAnimeStatus(
                    malId = targetMalId,
                    status = status,
                    progress = progress,
                    scoreTenScale = scoreTen,
                    accessToken = malAcc.accessToken
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed auto-push to MAL for anime $targetMalId", e)
            }
        }
    }

    suspend fun pushWatchlistDelete(
        storage: StorageManager,
        animeId: Int,
        malId: Int? = null
    ) = withContext(Dispatchers.IO) {
        val settings = storage.getTrackerSettings()
        if (!settings.isAutoSyncEnabled) return@withContext

        val aniListAcc = storage.getAniListAccount()
        if (aniListAcc != null && aniListAcc.accessToken.isNotBlank()) {
            try {
                AniListTrackerClient.deleteMediaEntry(animeId, aniListAcc.accessToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed delete from AniList for anime $animeId", e)
            }
        }

        val malAcc = storage.getMalAccount()
        val targetMalId = malId ?: animeId
        if (malAcc != null && malAcc.accessToken.isNotBlank() && targetMalId > 0) {
            try {
                MalTrackerClient.deleteAnimeStatus(targetMalId, malAcc.accessToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed delete from MAL for anime $targetMalId", e)
            }
        }
    }
}
