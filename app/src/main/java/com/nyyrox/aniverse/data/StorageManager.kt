package com.nyyrox.aniverse.data

import android.content.Context
import android.content.SharedPreferences
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.novel.NovelHistoryItem
import com.nyyrox.aniverse.data.discord.DiscordAccount
import com.nyyrox.aniverse.data.tracker.AniListAccount
import com.nyyrox.aniverse.data.tracker.MalAccount
import com.nyyrox.aniverse.data.tracker.TrackerSettings
import com.nyyrox.aniverse.ui.theme.AnimeTheme
import com.nyyrox.aniverse.ui.theme.ThemeSettings
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aniverse_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WATCHLIST = "key_watchlist"
        private const val KEY_HISTORY = "key_history"
        private const val KEY_MANGA_BOOKMARKS = "key_manga_bookmarks"
        private const val KEY_MANGA_HISTORY = "key_manga_history"
        private const val KEY_NOVEL_HISTORY = "key_novel_history"
        private const val KEY_USER_PROFILE = "key_user_profile"
        private const val KEY_THEME_SETTINGS = "key_theme_settings"
        private const val KEY_PREFERRED_SERVER = "key_preferred_server"
        private const val KEY_AUTO_PLAY_NEXT = "key_auto_play_next"
        private const val KEY_AUTO_SKIP_INTRO_OUTRO = "key_auto_skip_intro_outro"
        private const val KEY_ANILIST_ACCOUNT = "key_anilist_account"
        private const val KEY_MAL_ACCOUNT = "key_mal_account"
        private const val KEY_TRACKER_SETTINGS = "key_tracker_settings"
        private const val KEY_TIME_SPENT = "key_time_spent"
        private const val KEY_CINEPLAYER_CUSTOMIZATION = "key_cineplayer_customization"
        private const val KEY_DISCORD_ACCOUNT = "key_discord_account"
        private const val KEY_DISCORD_CLIENT_ID = "key_discord_client_id"
        private const val KEY_DISCORD_CLIENT_SECRET = "key_discord_client_secret"
        private const val KEY_LAST_DOWNLOAD_AUDIO = "key_last_download_audio"
        private const val KEY_LAST_DOWNLOAD_QUALITY = "key_last_download_quality"
        private const val KEY_LAST_DOWNLOAD_SERVER = "key_last_download_server"
        private const val KEY_DELETED_HISTORY_IDS = "key_deleted_history_ids"
        private const val KEY_DELETED_MANGA_IDS = "key_deleted_manga_ids"
        private const val KEY_DELETED_NOVEL_SLUGS = "key_deleted_novel_slugs"
        private const val KEY_LAST_DAILY_CLOUD_SYNC = "key_last_daily_cloud_sync"
    }

    // --- Daily Cloud Sync Tracking ---
    fun getLastCloudSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_DAILY_CLOUD_SYNC, 0L)
    }

    fun setLastCloudSyncTimestamp(ts: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_DAILY_CLOUD_SYNC, ts).apply()
    }

    // --- Watchlist ---
    fun getWatchlist(): List<WatchlistItem> {
        val raw = prefs.getString(KEY_WATCHLIST, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val genres = buildList {
                        obj.optJSONArray("genres")?.let { arr ->
                            for (j in 0 until arr.length()) add(arr.optString(j))
                        }
                    }
                    val customTags = buildList {
                        obj.optJSONArray("customTags")?.let { arr ->
                            for (j in 0 until arr.length()) add(arr.optString(j))
                        }
                    }
                    val addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    add(
                        WatchlistItem(
                            animeId = obj.getInt("animeId"),
                            title = obj.getString("title"),
                            cover = obj.getString("cover"),
                            banner = obj.optString("banner").takeIf { it.isNotBlank() },
                            score = if (obj.isNull("score")) null else obj.getInt("score"),
                            genres = genres,
                            episodes = if (obj.isNull("episodes")) null else obj.getInt("episodes"),
                            status = obj.optString("status").takeIf { it.isNotBlank() } ?: "plan_to_watch",
                            progressEpisode = obj.optInt("progressEpisode", 0),
                            releaseYear = obj.optString("releaseYear").takeIf { it.isNotBlank() },
                            customTags = customTags,
                            addedAt = addedAt,
                            updatedAt = obj.optLong("updatedAt", addedAt)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isInWatchlist(animeId: Int): Boolean {
        return getWatchlist().any { it.animeId == animeId }
    }

    fun getWatchlistItem(animeId: Int): WatchlistItem? {
        return getWatchlist().firstOrNull { it.animeId == animeId }
    }

    fun toggleWatchlist(anime: Anime, defaultStatus: String = "plan_to_watch"): Boolean {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == anime.id }
        val added = if (index >= 0) {
            list.removeAt(index)
            false
        } else {
            val now = System.currentTimeMillis()
            list.add(
                0,
                WatchlistItem(
                    animeId = anime.id,
                    title = anime.title,
                    cover = anime.cover,
                    banner = anime.banner,
                    score = anime.score,
                    genres = anime.genres,
                    episodes = anime.episodes ?: anime.nextEpisode,
                    status = defaultStatus,
                    progressEpisode = 0,
                    releaseYear = anime.displayYear.takeIf { it.isNotBlank() },
                    addedAt = now,
                    updatedAt = now
                )
            )
            true
        }
        saveWatchlist(list)
        return added
    }

    fun setWatchlistStatus(animeId: Int, status: String, anime: Anime? = null) {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        val now = System.currentTimeMillis()
        if (index >= 0) {
            val existing = list[index]
            list[index] = existing.copy(
                status = status,
                updatedAt = now
            )
        } else if (anime != null) {
            list.add(
                0,
                WatchlistItem(
                    animeId = anime.id,
                    title = anime.title,
                    cover = anime.cover,
                    banner = anime.banner,
                    score = anime.score,
                    genres = anime.genres,
                    episodes = anime.episodes ?: anime.nextEpisode,
                    status = status,
                    progressEpisode = 0,
                    releaseYear = anime.displayYear.takeIf { it.isNotBlank() },
                    addedAt = now,
                    updatedAt = now
                )
            )
        }
        saveWatchlist(list)
    }

    fun updateWatchlistProgress(animeId: Int, episode: Int, totalEpisodes: Int?) {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        if (index >= 0) {
            val item = list[index]
            val currentProg = maxOf(item.progressEpisode, episode)
            var newStatus = item.status
            if (newStatus == "plan_to_watch") {
                newStatus = "watching"
            }
            if (totalEpisodes != null && currentProg >= totalEpisodes && totalEpisodes > 0) {
                newStatus = "completed"
            } else if (newStatus == "completed" && episode == 1 && totalEpisodes != null && totalEpisodes > 1) {
                newStatus = "rewatching"
            }
            list[index] = item.copy(
                progressEpisode = currentProg,
                episodes = item.episodes ?: totalEpisodes,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            saveWatchlist(list)
        }
    }

    fun setWatchlistDetails(
        animeId: Int,
        status: String? = null,
        progressEpisode: Int? = null,
        customTags: List<String>? = null,
        anime: Anime? = null
    ) {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        val now = System.currentTimeMillis()
        if (index >= 0) {
            val existing = list[index]
            val newProgress = progressEpisode ?: existing.progressEpisode
            var newStatus = status ?: existing.status
            if (existing.episodes != null && existing.episodes > 0 && newProgress >= existing.episodes && newStatus == "watching") {
                newStatus = "completed"
            }
            list[index] = existing.copy(
                status = newStatus,
                progressEpisode = newProgress,
                customTags = customTags ?: existing.customTags,
                updatedAt = now
            )
        } else if (anime != null) {
            list.add(
                0,
                WatchlistItem(
                    animeId = anime.id,
                    title = anime.title,
                    cover = anime.cover,
                    banner = anime.banner,
                    score = anime.score,
                    genres = anime.genres,
                    episodes = anime.episodes ?: anime.nextEpisode,
                    status = status ?: "plan_to_watch",
                    progressEpisode = progressEpisode ?: 0,
                    releaseYear = anime.displayYear.takeIf { it.isNotBlank() },
                    customTags = customTags ?: emptyList(),
                    addedAt = now,
                    updatedAt = now
                )
            )
        }
        saveWatchlist(list)
    }

    fun incrementWatchlistProgress(animeId: Int): Int {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        if (index >= 0) {
            val item = list[index]
            val newProgress = item.progressEpisode + 1
            var newStatus = item.status
            if (newStatus == "plan_to_watch") {
                newStatus = "watching"
            }
            if (item.episodes != null && item.episodes > 0 && newProgress >= item.episodes) {
                newStatus = "completed"
            }
            list[index] = item.copy(
                progressEpisode = newProgress,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            saveWatchlist(list)
            return newProgress
        }
        return 0
    }

    fun removeFromWatchlist(animeId: Int) {
        val list = getWatchlist().filterNot { it.animeId == animeId }
        saveWatchlist(list)
    }

    fun addToWatchlistFromCloud(
        animeId: Int,
        title: String,
        cover: String,
        banner: String?,
        score: Int?,
        genres: List<String>,
        episodes: Int?,
        status: String,
        progressEpisode: Int,
        releaseYear: String?,
        customTags: List<String>
    ) {
        val list = getWatchlist().toMutableList()
        val index = list.indexOfFirst { it.animeId == animeId }
        val now = System.currentTimeMillis()
        val item = WatchlistItem(
            animeId = animeId,
            title = title,
            cover = cover,
            banner = banner,
            score = score,
            genres = genres,
            episodes = episodes,
            status = status,
            progressEpisode = progressEpisode,
            releaseYear = releaseYear,
            customTags = customTags,
            addedAt = now,
            updatedAt = now
        )
        if (index >= 0) {
            list[index] = item
        } else {
            list.add(0, item)
        }
        saveWatchlist(list)
    }

    fun saveWatchlist(list: List<WatchlistItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("animeId", item.animeId)
                put("title", item.title)
                put("cover", item.cover)
                put("banner", item.banner ?: JSONObject.NULL)
                put("score", item.score ?: JSONObject.NULL)
                put("genres", JSONArray(item.genres))
                put("episodes", item.episodes ?: JSONObject.NULL)
                put("status", item.status)
                put("progressEpisode", item.progressEpisode)
                put("releaseYear", item.releaseYear ?: JSONObject.NULL)
                put("customTags", JSONArray(item.customTags))
                put("addedAt", item.addedAt)
                put("updatedAt", item.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_WATCHLIST, array.toString()).apply()
    }

    // --- Continue Watching / History ---
    fun getHistory(): List<WatchHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        WatchHistoryItem(
                            animeId = obj.getInt("animeId"),
                            title = obj.getString("title"),
                            cover = obj.getString("cover"),
                            banner = obj.optString("banner").takeIf { it.isNotBlank() },
                            episodeStill = obj.optString("episodeStill").takeIf { it.isNotBlank() },
                            lastEpisode = obj.getInt("lastEpisode"),
                            totalEpisodes = if (obj.isNull("totalEpisodes")) null else obj.getInt("totalEpisodes"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getHistoryItem(animeId: Int): WatchHistoryItem? {
        return getHistory().firstOrNull { it.animeId == animeId }
    }

    fun getEpisodePosition(animeId: Int, episode: Int): Long {
        val pos = prefs.getLong("pos_${animeId}_$episode", 0L)
        val dur = prefs.getLong("dur_${animeId}_$episode", 0L)
        if (pos <= 0L) return 0L
        if (dur > 0L && (pos >= dur - 25_000L || pos >= (dur * 0.90f).toLong())) {
            return 0L
        }
        return pos
    }

    fun saveEpisodePosition(animeId: Int, episode: Int, positionMs: Long, durationMs: Long) {
        if (positionMs <= 0L) return
        val posToSave = if (durationMs > 0L && (positionMs >= durationMs - 25_000L || positionMs >= (durationMs * 0.90f).toLong())) {
            0L
        } else {
            positionMs
        }
        prefs.edit()
            .putLong("pos_${animeId}_$episode", posToSave)
            .putLong("dur_${animeId}_$episode", durationMs)
            .apply()
    }

    fun recordProgress(
        anime: Anime,
        episode: Int,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        episodeStill: String? = null
    ) {
        val history = getHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.animeId == anime.id }
        val existingItem = if (existingIndex >= 0) history[existingIndex] else null
        val savedPos = if (positionMs > 0L) positionMs else getEpisodePosition(anime.id, episode)

        // Resolve final episode still: explicit parameter > existing if same episode > null
        val finalStill = episodeStill?.takeIf { it.isNotBlank() }
            ?: if (existingItem != null && existingItem.lastEpisode == episode) existingItem.episodeStill else null

        if (existingIndex >= 0) {
            history.removeAt(existingIndex)
        }
        history.add(
            0,
            WatchHistoryItem(
                animeId = anime.id,
                title = anime.title,
                cover = anime.cover,
                banner = anime.banner,
                episodeStill = finalStill,
                lastEpisode = episode,
                totalEpisodes = anime.episodes ?: anime.nextEpisode,
                timestamp = System.currentTimeMillis(),
                positionMs = savedPos,
                durationMs = durationMs
            )
        )
        // Keep up to 30 recent items
        val capped = history.take(30)
        saveHistoryList(capped)
        if (positionMs > 0L) {
            saveEpisodePosition(anime.id, episode, positionMs, durationMs)
        }
        updateWatchlistProgress(anime.id, episode, anime.episodes ?: anime.nextEpisode)
        unmarkHistoryDeleted(anime.id)
    }

    fun updateHistoryEpisodeStill(animeId: Int, episode: Int, stillUrl: String) {
        if (stillUrl.isBlank()) return
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.animeId == animeId }
        if (index >= 0) {
            val item = history[index]
            if (item.lastEpisode == episode && item.episodeStill != stillUrl) {
                history[index] = item.copy(episodeStill = stillUrl)
                saveHistoryList(history)
            }
        }
    }

    fun saveHistoryList(history: List<WatchHistoryItem>) {
        val array = JSONArray()
        for (item in history) {
            val obj = JSONObject().apply {
                put("animeId", item.animeId)
                put("title", item.title)
                put("cover", item.cover)
                put("banner", item.banner ?: JSONObject.NULL)
                put("episodeStill", item.episodeStill ?: JSONObject.NULL)
                put("lastEpisode", item.lastEpisode)
                put("totalEpisodes", item.totalEpisodes ?: JSONObject.NULL)
                put("timestamp", item.timestamp)
                put("positionMs", item.positionMs)
                put("durationMs", item.durationMs)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun isHistoryDeleted(animeId: Int): Boolean {
        val raw = prefs.getStringSet(KEY_DELETED_HISTORY_IDS, null) ?: return false
        return raw.contains(animeId.toString())
    }

    fun markHistoryDeleted(animeId: Int) {
        val current = prefs.getStringSet(KEY_DELETED_HISTORY_IDS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(animeId.toString())
        prefs.edit().putStringSet(KEY_DELETED_HISTORY_IDS, current).apply()
    }

    fun unmarkHistoryDeleted(animeId: Int) {
        val current = prefs.getStringSet(KEY_DELETED_HISTORY_IDS, null)?.toMutableSet() ?: return
        if (current.remove(animeId.toString())) {
            prefs.edit().putStringSet(KEY_DELETED_HISTORY_IDS, current).apply()
        }
    }

    fun getDeletedHistoryIds(): Set<Int> {
        val raw = prefs.getStringSet(KEY_DELETED_HISTORY_IDS, null) ?: return emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun isMangaHistoryDeleted(mangaId: String): Boolean {
        val raw = prefs.getStringSet(KEY_DELETED_MANGA_IDS, null) ?: return false
        return raw.contains(mangaId)
    }

    fun markMangaHistoryDeleted(mangaId: String) {
        val current = prefs.getStringSet(KEY_DELETED_MANGA_IDS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(mangaId)
        prefs.edit().putStringSet(KEY_DELETED_MANGA_IDS, current).apply()
    }

    fun unmarkMangaHistoryDeleted(mangaId: String) {
        val current = prefs.getStringSet(KEY_DELETED_MANGA_IDS, null)?.toMutableSet() ?: return
        if (current.remove(mangaId)) {
            prefs.edit().putStringSet(KEY_DELETED_MANGA_IDS, current).apply()
        }
    }

    fun isNovelHistoryDeleted(slug: String): Boolean {
        val raw = prefs.getStringSet(KEY_DELETED_NOVEL_SLUGS, null) ?: return false
        return raw.contains(slug)
    }

    fun markNovelHistoryDeleted(slug: String) {
        val current = prefs.getStringSet(KEY_DELETED_NOVEL_SLUGS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(slug)
        prefs.edit().putStringSet(KEY_DELETED_NOVEL_SLUGS, current).apply()
    }

    fun unmarkNovelHistoryDeleted(slug: String) {
        val current = prefs.getStringSet(KEY_DELETED_NOVEL_SLUGS, null)?.toMutableSet() ?: return
        if (current.remove(slug)) {
            prefs.edit().putStringSet(KEY_DELETED_NOVEL_SLUGS, current).apply()
        }
    }

    fun removeHistory(animeId: Int) {
        markHistoryDeleted(animeId)
        val history = getHistory().filterNot { it.animeId == animeId }
        val array = JSONArray()
        for (item in history) {
            val obj = JSONObject().apply {
                put("animeId", item.animeId)
                put("title", item.title)
                put("cover", item.cover)
                put("banner", item.banner ?: JSONObject.NULL)
                put("lastEpisode", item.lastEpisode)
                put("totalEpisodes", item.totalEpisodes ?: JSONObject.NULL)
                put("timestamp", item.timestamp)
                put("positionMs", item.positionMs)
                put("durationMs", item.durationMs)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun addToHistoryFromCloud(
        animeId: Int,
        title: String,
        cover: String,
        banner: String?,
        lastEpisode: Int,
        totalEpisodes: Int?,
        positionMs: Long,
        durationMs: Long,
        episodeStill: String? = null
    ) {
        if (isHistoryDeleted(animeId)) return
        val history = getHistory().toMutableList()
        val index = history.indexOfFirst { it.animeId == animeId }
        val existingItem = if (index >= 0) history[index] else null
        val finalStill = episodeStill?.takeIf { it.isNotBlank() }
            ?: if (existingItem != null && existingItem.lastEpisode == lastEpisode) existingItem.episodeStill else null
        val item = WatchHistoryItem(
            animeId = animeId,
            title = title,
            cover = cover,
            banner = banner,
            episodeStill = finalStill,
            lastEpisode = lastEpisode,
            totalEpisodes = totalEpisodes,
            timestamp = System.currentTimeMillis(),
            positionMs = positionMs,
            durationMs = durationMs
        )
        if (index >= 0) {
            history[index] = item
        } else {
            history.add(0, item)
        }
        saveHistoryList(history.take(30))
        if (positionMs > 0L) {
            saveEpisodePosition(animeId, lastEpisode, positionMs, durationMs)
        }
    }

    // --- Manga Bookmarks (Series, Chapter, Page) ---
    fun getMangaBookmarks(): List<MangaBookmarkItem> {
        val raw = prefs.getString(KEY_MANGA_BOOKMARKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MangaBookmarkItem>()
            val seenKeys = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mId = obj.getString("mangaId")
                val bTypeStr = obj.optString("bookmarkType", "SERIES")
                val bType = try {
                    com.nyyrox.aniverse.data.manga.MangaBookmarkType.valueOf(bTypeStr)
                } catch (_: Exception) {
                    com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES
                }
                val chId = obj.optString("chapterId").takeIf { it.isNotBlank() }
                val chNum = obj.optString("chapterNumber").takeIf { it.isNotBlank() }
                val pIdx = obj.optInt("pageIndex", 0)
                val pCount = obj.optInt("pageCount", 0)

                val uniqueKey = "$bType:$mId:${chId ?: chNum ?: ""}:$pIdx"
                if (mId.isNotBlank() && !seenKeys.contains(uniqueKey)) {
                    seenKeys.add(uniqueKey)
                    list.add(
                        MangaBookmarkItem(
                            mangaId = mId,
                            title = obj.getString("title"),
                            poster = obj.optString("poster").takeIf { it.isNotBlank() },
                            type = obj.optString("type", "Manga"),
                            bookmarkType = bType,
                            chapterId = chId,
                            chapterNumber = chNum,
                            pageIndex = pIdx,
                            pageCount = pCount,
                            latestChapter = obj.optString("latestChapter").takeIf { it.isNotBlank() },
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isMangaBookmarked(mangaId: String): Boolean {
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        return getMangaBookmarks().any {
            it.bookmarkType == com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES &&
            (it.mangaId == mangaId || com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean)
        }
    }

    fun isChapterBookmarked(mangaId: String, chapterId: String?, chapterNumber: String? = null): Boolean {
        val cleanM = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        return getMangaBookmarks().any {
            it.bookmarkType == com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER &&
            (it.mangaId == mangaId || com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == cleanM) &&
            ((!chapterId.isNullOrBlank() && it.chapterId == chapterId) || (!chapterNumber.isNullOrBlank() && it.chapterNumber == chapterNumber))
        }
    }

    fun isPageBookmarked(mangaId: String, chapterId: String?, pageIndex: Int): Boolean {
        val cleanM = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        return getMangaBookmarks().any {
            it.bookmarkType == com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE &&
            (it.mangaId == mangaId || com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == cleanM) &&
            (!chapterId.isNullOrBlank() && it.chapterId == chapterId) &&
            it.pageIndex == pageIndex
        }
    }

    fun toggleMangaBookmark(
        mangaId: String,
        title: String,
        poster: String? = null,
        type: String? = "Manga",
        latestChapter: String? = null,
        bookmarkType: com.nyyrox.aniverse.data.manga.MangaBookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES,
        chapterId: String? = null,
        chapterNumber: String? = null,
        pageIndex: Int = 0,
        pageCount: Int = 0
    ): Boolean {
        val list = getMangaBookmarks().toMutableList()
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)

        val index = list.indexOfFirst {
            val sameManga = it.mangaId == mangaId ||
                    (it.title.isNotBlank() && it.title.equals(title, ignoreCase = true)) ||
                    (clean.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean)

            if (!sameManga || it.bookmarkType != bookmarkType) return@indexOfFirst false

            when (bookmarkType) {
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES -> true
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER -> {
                    (!chapterId.isNullOrBlank() && it.chapterId == chapterId) ||
                    (!chapterNumber.isNullOrBlank() && it.chapterNumber == chapterNumber)
                }
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE -> {
                    it.chapterId == chapterId && it.pageIndex == pageIndex
                }
            }
        }

        val added = if (index >= 0) {
            list.removeAt(index)
            false
        } else {
            list.add(
                0,
                MangaBookmarkItem(
                    mangaId = mangaId,
                    title = title,
                    poster = poster,
                    type = type ?: "Manga",
                    bookmarkType = bookmarkType,
                    chapterId = chapterId,
                    chapterNumber = chapterNumber,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    latestChapter = latestChapter,
                    updatedAt = System.currentTimeMillis()
                )
            )
            true
        }
        saveMangaBookmarks(list)
        return added
    }

    fun removeMangaBookmark(
        mangaId: String,
        bookmarkType: com.nyyrox.aniverse.data.manga.MangaBookmarkType? = null,
        chapterId: String? = null,
        pageIndex: Int? = null
    ) {
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        val list = getMangaBookmarks().filterNot {
            val sameManga = it.mangaId == mangaId || com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean
            if (!sameManga) return@filterNot false
            if (bookmarkType == null) return@filterNot true
            if (it.bookmarkType != bookmarkType) return@filterNot false
            when (bookmarkType) {
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES -> true
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.CHAPTER -> chapterId == null || it.chapterId == chapterId
                com.nyyrox.aniverse.data.manga.MangaBookmarkType.PAGE -> (chapterId == null || it.chapterId == chapterId) && (pageIndex == null || it.pageIndex == pageIndex)
            }
        }
        saveMangaBookmarks(list)
    }

    private fun saveMangaBookmarks(list: List<MangaBookmarkItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("mangaId", item.mangaId)
                put("title", item.title)
                put("poster", item.poster ?: JSONObject.NULL)
                put("type", item.type ?: "Manga")
                put("bookmarkType", item.bookmarkType.name)
                put("chapterId", item.chapterId ?: JSONObject.NULL)
                put("chapterNumber", item.chapterNumber ?: JSONObject.NULL)
                put("pageIndex", item.pageIndex)
                put("pageCount", item.pageCount)
                put("latestChapter", item.latestChapter ?: JSONObject.NULL)
                put("updatedAt", item.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_MANGA_BOOKMARKS, array.toString()).apply()
    }

    // --- Manga History / Continue Reading ---
    fun getMangaHistory(): List<MangaHistoryItem> {
        val raw = prefs.getString(KEY_MANGA_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MangaHistoryItem>()
            val seenIds = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mId = obj.getString("mangaId")
                val cleanId = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mId)
                if (mId.isNotBlank() && !seenIds.contains(mId) && !seenIds.contains(cleanId)) {
                    seenIds.add(mId)
                    seenIds.add(cleanId)
                    list.add(
                        MangaHistoryItem(
                            mangaId = mId,
                            title = obj.getString("title"),
                            poster = obj.optString("poster").takeIf { it.isNotBlank() },
                            type = obj.optString("type", "Manga"),
                            chapterId = obj.optString("chapterId", ""),
                            chapterNumber = obj.optString("chapterNumber", ""),
                            chapterTitle = obj.optString("chapterTitle").takeIf { it.isNotBlank() },
                            pageIndex = obj.optInt("pageIndex", 0),
                            pageCount = obj.optInt("pageCount", 0),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getMangaHistoryItem(mangaId: String): MangaHistoryItem? {
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        return getMangaHistory().firstOrNull {
            it.mangaId == mangaId || (clean.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean)
        }
    }

    fun getMangaChapterPageProgress(mangaId: String, chapterId: String): Int {
        val cleanM = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        val cleanCh = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chapterId)
        val v1 = prefs.getInt("manga_page_${cleanM}_${cleanCh}", -1)
        if (v1 >= 0) return v1
        val v2 = prefs.getInt("manga_page_${mangaId}_${chapterId}", -1)
        if (v2 >= 0) return v2
        val v3 = prefs.getInt("manga_page_${cleanM}_${chapterId}", -1)
        if (v3 >= 0) return v3
        // Also check history item
        val hist = getMangaHistoryItem(mangaId)
        if (hist != null && (hist.chapterId == chapterId || com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(hist.chapterId) == cleanCh)) {
            return hist.pageIndex
        }
        return 0
    }

    fun recordMangaProgress(
        mangaId: String,
        title: String,
        poster: String? = null,
        type: String? = "Manga",
        chapterId: String,
        chapterNumber: String,
        chapterTitle: String? = null,
        pageIndex: Int = 0,
        pageCount: Int = 0
    ) {
        val history = getMangaHistory().toMutableList()
        val cleanCurrentId = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        val effectiveMangaId = cleanCurrentId.ifBlank { mangaId }
        val cleanInputCh = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chapterId)

        unmarkMangaHistoryDeleted(mangaId)
        if (effectiveMangaId != mangaId) {
            unmarkMangaHistoryDeleted(effectiveMangaId)
        }

        val existingIndex = history.indexOfFirst {
            it.mangaId == mangaId ||
            it.mangaId == effectiveMangaId ||
            (it.title.isNotBlank() && it.title.equals(title, ignoreCase = true)) ||
            (cleanCurrentId.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == cleanCurrentId)
        }

        val existing = if (existingIndex >= 0) history.removeAt(existingIndex) else null

        val finalPoster = poster ?: existing?.poster
        val finalType = type ?: existing?.type ?: "Manga"
        val finalChapterId = if (chapterId.isNotBlank()) chapterId else (existing?.chapterId ?: "")
        val finalCleanCh = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(finalChapterId)
        val finalChapterNumber = if (chapterNumber.isNotBlank()) chapterNumber else (existing?.chapterNumber ?: "1")
        val finalChapterTitle = chapterTitle ?: existing?.chapterTitle
        val isSameChapter = existing != null && (
            existing.chapterId == finalChapterId ||
            com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(existing.chapterId) == finalCleanCh ||
            existing.chapterNumber == finalChapterNumber
        )
        val finalPageIndex = if (pageIndex >= 0) {
            pageIndex
        } else if (isSameChapter) {
            existing?.pageIndex ?: 0
        } else {
            0
        }
        val finalPageCount = if (pageCount > 0) {
            pageCount
        } else if (isSameChapter) {
            existing?.pageCount ?: 0
        } else {
            0
        }

        history.add(
            0,
            MangaHistoryItem(
                mangaId = effectiveMangaId,
                title = title.ifBlank { existing?.title ?: "Manga" },
                poster = finalPoster,
                type = finalType,
                chapterId = finalChapterId,
                chapterNumber = finalChapterNumber,
                chapterTitle = finalChapterTitle,
                pageIndex = finalPageIndex,
                pageCount = finalPageCount,
                updatedAt = System.currentTimeMillis()
            )
        )
        val capped = history.take(30)
        val array = JSONArray()
        for (item in capped) {
            val obj = JSONObject().apply {
                put("mangaId", item.mangaId)
                put("title", item.title)
                put("poster", item.poster ?: JSONObject.NULL)
                put("type", item.type ?: "Manga")
                put("chapterId", item.chapterId)
                put("chapterNumber", item.chapterNumber)
                put("chapterTitle", item.chapterTitle ?: JSONObject.NULL)
                put("pageIndex", item.pageIndex)
                put("pageCount", item.pageCount)
                put("updatedAt", item.updatedAt)
            }
            array.put(obj)
        }
        val cleanM = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(effectiveMangaId)
        prefs.edit()
            .putString(KEY_MANGA_HISTORY, array.toString())
            .putInt("manga_page_${effectiveMangaId}_${finalChapterId}", finalPageIndex)
            .putInt("manga_page_${effectiveMangaId}_${finalCleanCh}", finalPageIndex)
            .putInt("manga_page_${mangaId}_${chapterId}", finalPageIndex)
            .putInt("manga_page_${cleanM}_${finalCleanCh}", finalPageIndex)
            .apply()
    }

    fun removeMangaHistory(mangaId: String) {
        markMangaHistoryDeleted(mangaId)
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        if (clean.isNotBlank()) {
            markMangaHistoryDeleted(clean)
        }
        val history = getMangaHistory().filterNot {
            it.mangaId == mangaId || (clean.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean)
        }
        val array = JSONArray()
        for (item in history) {
            val obj = JSONObject().apply {
                put("mangaId", item.mangaId)
                put("title", item.title)
                put("poster", item.poster ?: JSONObject.NULL)
                put("type", item.type ?: "Manga")
                put("chapterId", item.chapterId)
                put("chapterNumber", item.chapterNumber)
                put("chapterTitle", item.chapterTitle ?: JSONObject.NULL)
                put("pageIndex", item.pageIndex)
                put("pageCount", item.pageCount)
                put("updatedAt", item.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_MANGA_HISTORY, array.toString()).apply()
    }

    // --- Novel History / Continue Reading ---
    fun getNovelHistory(): List<NovelHistoryItem> {
        val raw = prefs.getString(KEY_NOVEL_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<NovelHistoryItem>()
            val seenSlugs = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawSlug = obj.optString("slug", "").trim().removePrefix("/book/").removePrefix("/").removeSuffix("/")
                val lastCh = obj.optString("lastChapterSlug", "").trim().removePrefix("/").removeSuffix("/")
                val title = obj.optString("title", "").trim()
                if (rawSlug.isNotBlank() && lastCh.isNotBlank() && !seenSlugs.contains(rawSlug)) {
                    seenSlugs.add(rawSlug)
                    list.add(
                        NovelHistoryItem(
                            slug = rawSlug,
                            title = title.ifBlank { rawSlug.replace("-", " ") },
                            cover = obj.optString("cover", ""),
                            lastChapterSlug = lastCh,
                            lastChapterTitle = obj.optString("lastChapterTitle", "").ifBlank { lastCh },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getNovelHistoryItem(slug: String): NovelHistoryItem? {
        return getNovelHistory().firstOrNull { it.slug == slug }
    }

    fun recordNovelProgress(
        slug: String,
        title: String,
        cover: String = "",
        lastChapterSlug: String,
        lastChapterTitle: String
    ) {
        unmarkNovelHistoryDeleted(slug)
        val history = getNovelHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.slug == slug }
        if (existingIndex >= 0) {
            history.removeAt(existingIndex)
        }
        history.add(
            0,
            NovelHistoryItem(
                slug = slug,
                title = title,
                cover = cover,
                lastChapterSlug = lastChapterSlug,
                lastChapterTitle = lastChapterTitle,
                timestamp = System.currentTimeMillis()
            )
        )
        val capped = history.take(30)
        val array = JSONArray()
        for (item in capped) {
            val obj = JSONObject().apply {
                put("slug", item.slug)
                put("title", item.title)
                put("cover", item.cover)
                put("lastChapterSlug", item.lastChapterSlug)
                put("lastChapterTitle", item.lastChapterTitle)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOVEL_HISTORY, array.toString()).apply()
    }

    fun removeNovelHistory(slug: String) {
        markNovelHistoryDeleted(slug)
        val history = getNovelHistory().filterNot { it.slug == slug }
        val array = JSONArray()
        for (item in history) {
            val obj = JSONObject().apply {
                put("slug", item.slug)
                put("title", item.title)
                put("cover", item.cover)
                put("lastChapterSlug", item.lastChapterSlug)
                put("lastChapterTitle", item.lastChapterTitle)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOVEL_HISTORY, array.toString()).apply()
    }

    // --- User Profile ---
    fun getUserProfile(): UserProfile {
        val raw = prefs.getString(KEY_USER_PROFILE, null) ?: return UserProfile()
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = obj.optString("id").takeIf { it.isNotBlank() },
                email = obj.optString("email").takeIf { it.isNotBlank() },
                avatarUrl = obj.optString("avatarUrl", ""),
                bannerUrl = obj.optString("bannerUrl", ""),
                displayName = obj.optString("displayName", "Guest"),
                username = obj.optString("username", ""),
                bio = obj.optString("bio", ""),
                bioFont = obj.optString("bioFont", "default"),
                favoriteAnime = obj.optString("favoriteAnime", ""),
                otakuRank = obj.optString("otakuRank", "Genin"),
                isAdmin = obj.optBoolean("isAdmin", false),
                role = obj.optString("role", "USER").takeIf { it.isNotBlank() },
                roles = obj.optJSONArray("roles")?.let { arr ->
                    (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
                } ?: (if (obj.optBoolean("isAdmin", false)) listOf("ADMIN") else emptyList()),
                customTag = obj.optString("customTag", "").takeIf { it.isNotBlank() },
                avatarDecorationId = obj.optString("avatarDecorationId", "").takeIf { it.isNotBlank() },
                avatarFrameId = obj.optString("avatarFrameId", "").takeIf { it.isNotBlank() },
                textEffectId = obj.optString("textEffectId", "").takeIf { it.isNotBlank() },
                profileEffectId = obj.optString("profileEffectId", "").takeIf { it.isNotBlank() },
                bannerThemeId = obj.optString("bannerThemeId", "").takeIf { it.isNotBlank() },
                nameplateId = obj.optString("nameplateId", "").takeIf { it.isNotBlank() },
                nameplateEmoji = obj.optString("nameplateEmoji", "").takeIf { it.isNotBlank() },
                exp = obj.optLong("exp", 0L),
                level = obj.optInt("level", 1).coerceAtLeast(1),
                streakCount = obj.optInt("streakCount", 1).coerceAtLeast(1),
                longestStreak = obj.optInt("longestStreak", 1).coerceAtLeast(1),
                lastActiveDate = obj.optString("lastActiveDate", "").takeIf { it.isNotBlank() },
                bannerEffectId = obj.optString("bannerEffectId", "none"),
                isWatchlistPrivate = obj.optBoolean("isWatchlistPrivate", false),
                isWatchHistoryPrivate = obj.optBoolean("isWatchHistoryPrivate", false),
                isMangaPrivate = obj.optBoolean("isMangaPrivate", false),
                isNovelPrivate = obj.optBoolean("isNovelPrivate", false)
            )
        } catch (_: Exception) {
            UserProfile()
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        val obj = JSONObject().apply {
            put("id", profile.id ?: JSONObject.NULL)
            put("email", profile.email ?: JSONObject.NULL)
            put("avatarUrl", profile.avatarUrl)
            put("bannerUrl", profile.bannerUrl)
            put("displayName", profile.displayName)
            put("username", profile.username)
            put("bio", profile.bio)
            put("bioFont", profile.bioFont)
            put("favoriteAnime", profile.favoriteAnime)
            put("otakuRank", profile.otakuRank)
            put("isAdmin", profile.isAdmin)
            put("role", profile.role ?: JSONObject.NULL)
            put("roles", org.json.JSONArray(profile.roles))
            put("customTag", profile.customTag ?: JSONObject.NULL)
            put("avatarDecorationId", profile.avatarDecorationId ?: JSONObject.NULL)
            put("avatarFrameId", profile.avatarFrameId ?: JSONObject.NULL)
            put("textEffectId", profile.textEffectId ?: JSONObject.NULL)
            put("profileEffectId", profile.profileEffectId ?: JSONObject.NULL)
            put("bannerThemeId", profile.bannerThemeId ?: JSONObject.NULL)
            put("nameplateId", profile.nameplateId ?: JSONObject.NULL)
            put("nameplateEmoji", profile.nameplateEmoji ?: JSONObject.NULL)
            put("exp", profile.exp)
            put("level", profile.level)
            put("streakCount", profile.streakCount)
            put("longestStreak", profile.longestStreak)
            put("lastActiveDate", profile.lastActiveDate ?: JSONObject.NULL)
            put("bannerEffectId", profile.bannerEffectId)
            put("isWatchlistPrivate", profile.isWatchlistPrivate)
            put("isWatchHistoryPrivate", profile.isWatchHistoryPrivate)
            put("isMangaPrivate", profile.isMangaPrivate)
            put("isNovelPrivate", profile.isNovelPrivate)
        }
        prefs.edit().putString(KEY_USER_PROFILE, obj.toString()).apply()
    }

    // --- Theme & Performance Settings ---
    fun getThemeSettings(): ThemeSettings {
        val raw = prefs.getString(KEY_THEME_SETTINGS, null) ?: return ThemeSettings()
        return try {
            val obj = JSONObject(raw)
            val themeName = obj.optString("theme", AnimeTheme.GOJO_LIMITLESS.name)
            val theme = try {
                AnimeTheme.valueOf(themeName)
            } catch (_: Exception) {
                AnimeTheme.GOJO_LIMITLESS
            }
            ThemeSettings(
                theme = theme,
                isReducedMotionEnabled = obj.optBoolean("reducedMotion", false),
                isPowerSavingEnabled = obj.optBoolean("powerSaving", false),
                preferredRefreshRate = obj.optInt("preferredRefreshRate", 90),
                isGlassmorphismEnabled = obj.optBoolean("glassmorphism", true),
                mode = try {
                    com.nyyrox.aniverse.ui.theme.ThemeMode.valueOf(obj.optString("mode", "DARK"))
                } catch (_: Exception) {
                    com.nyyrox.aniverse.ui.theme.ThemeMode.DARK
                },
                useCards = obj.optBoolean("useCards", true)
            )
        } catch (_: Exception) {
            ThemeSettings()
        }
    }

    fun saveThemeSettings(settings: ThemeSettings) {
        val obj = JSONObject().apply {
            put("theme", settings.theme.name)
            put("reducedMotion", settings.isReducedMotionEnabled)
            put("powerSaving", settings.isPowerSavingEnabled)
            put("preferredRefreshRate", settings.preferredRefreshRate)
            put("glassmorphism", settings.isGlassmorphismEnabled)
            put("mode", settings.mode.name)
            put("useCards", settings.useCards)
        }
        prefs.edit().putString(KEY_THEME_SETTINGS, obj.toString()).apply()
    }

    // --- Server Preference ---
    fun getPreferredServerId(): String? {
        return prefs.getString(KEY_PREFERRED_SERVER, null)
    }

    fun savePreferredServerId(serverId: String) {
        prefs.edit().putString(KEY_PREFERRED_SERVER, serverId).apply()
    }

    // --- Download Preferences ---
    fun getLastDownloadAudio(): String? {
        return prefs.getString(KEY_LAST_DOWNLOAD_AUDIO, null)
    }

    fun getLastDownloadQuality(): String? {
        return prefs.getString(KEY_LAST_DOWNLOAD_QUALITY, null)
    }

    fun getLastDownloadServerId(): String? {
        return prefs.getString(KEY_LAST_DOWNLOAD_SERVER, null)
    }

    fun saveDownloadSettings(audio: String, quality: String, serverId: String) {
        prefs.edit()
            .putString(KEY_LAST_DOWNLOAD_AUDIO, audio)
            .putString(KEY_LAST_DOWNLOAD_QUALITY, quality)
            .putString(KEY_LAST_DOWNLOAD_SERVER, serverId)
            .apply()
    }

    // --- Playback Preferences ---
    fun isAutoPlayNext(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, enabled).apply()
    }

    fun isAutoSkipIntroOutro(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SKIP_INTRO_OUTRO, true)
    }

    fun setAutoSkipIntroOutro(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SKIP_INTRO_OUTRO, enabled).apply()
    }

    // --- Tracking Accounts (AniList & MAL) ---
    fun getAniListAccount(): AniListAccount? {
        val raw = prefs.getString(KEY_ANILIST_ACCOUNT, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            AniListAccount(
                userId = obj.getInt("userId"),
                username = obj.getString("username"),
                avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
                accessToken = obj.getString("accessToken"),
                lastSyncedAt = obj.optLong("lastSyncedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveAniListAccount(account: AniListAccount?) {
        if (account == null) {
            prefs.edit().remove(KEY_ANILIST_ACCOUNT).apply()
            return
        }
        val obj = JSONObject().apply {
            put("userId", account.userId)
            put("username", account.username)
            put("avatarUrl", account.avatarUrl.orEmpty())
            put("accessToken", account.accessToken)
            put("lastSyncedAt", account.lastSyncedAt)
        }
        prefs.edit().putString(KEY_ANILIST_ACCOUNT, obj.toString()).apply()
    }

    fun getMalAccount(): MalAccount? {
        val raw = prefs.getString(KEY_MAL_ACCOUNT, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            MalAccount(
                username = obj.getString("username"),
                avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
                accessToken = obj.getString("accessToken"),
                refreshToken = obj.optString("refreshToken").takeIf { it.isNotBlank() },
                expiresAt = obj.optLong("expiresAt", 0L),
                lastSyncedAt = obj.optLong("lastSyncedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveMalAccount(account: MalAccount?) {
        if (account == null) {
            prefs.edit().remove(KEY_MAL_ACCOUNT).apply()
            return
        }
        val obj = JSONObject().apply {
            put("username", account.username)
            put("avatarUrl", account.avatarUrl.orEmpty())
            put("accessToken", account.accessToken)
            put("refreshToken", account.refreshToken.orEmpty())
            put("expiresAt", account.expiresAt)
            put("lastSyncedAt", account.lastSyncedAt)
        }
        prefs.edit().putString(KEY_MAL_ACCOUNT, obj.toString()).apply()
    }

    // --- Discord Integration ---
    fun getDiscordAccount(): DiscordAccount? {
        val raw = prefs.getString(KEY_DISCORD_ACCOUNT, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            DiscordAccount(
                userId = obj.getString("userId"),
                username = obj.getString("username"),
                globalName = obj.optString("globalName").takeIf { it.isNotBlank() },
                avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
                accessToken = obj.getString("accessToken"),
                refreshToken = obj.optString("refreshToken").takeIf { it.isNotBlank() },
                userToken = obj.optString("userToken").takeIf { it.isNotBlank() && it != obj.optString("accessToken") },
                isRpcEnabled = obj.optBoolean("isRpcEnabled", true)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveDiscordAccount(account: DiscordAccount?) {
        if (account == null) {
            prefs.edit().remove(KEY_DISCORD_ACCOUNT).apply()
            return
        }
        val obj = JSONObject().apply {
            put("userId", account.userId)
            put("username", account.username)
            put("globalName", account.globalName.orEmpty())
            put("avatarUrl", account.avatarUrl.orEmpty())
            put("accessToken", account.accessToken)
            put("refreshToken", account.refreshToken.orEmpty())
            put("userToken", account.userToken.orEmpty())
            put("isRpcEnabled", account.isRpcEnabled)
        }
        prefs.edit().putString(KEY_DISCORD_ACCOUNT, obj.toString()).apply()
    }

    fun getDiscordClientId(): String {
        return prefs.getString(KEY_DISCORD_CLIENT_ID, null) ?: com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_ID
    }

    fun saveDiscordClientId(clientId: String) {
        prefs.edit().putString(KEY_DISCORD_CLIENT_ID, clientId).apply()
        com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_ID = clientId
    }

    fun getDiscordClientSecret(): String {
        return prefs.getString(KEY_DISCORD_CLIENT_SECRET, null) ?: com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_SECRET
    }

    fun saveDiscordClientSecret(secret: String) {
        prefs.edit().putString(KEY_DISCORD_CLIENT_SECRET, secret).apply()
        com.nyyrox.aniverse.data.discord.DiscordConfig.CLIENT_SECRET = secret
    }

    fun getTrackerSettings(): TrackerSettings {
        val raw = prefs.getString(KEY_TRACKER_SETTINGS, null) ?: return TrackerSettings()
        return try {
            val obj = JSONObject(raw)
            TrackerSettings(
                isAutoSyncEnabled = obj.optBoolean("isAutoSyncEnabled", true),
                syncOnEpisodeComplete = obj.optBoolean("syncOnEpisodeComplete", true),
                lastFullSyncTimestamp = obj.optLong("lastFullSyncTimestamp", 0L)
            )
        } catch (_: Exception) {
            TrackerSettings()
        }
    }

    fun saveTrackerSettings(settings: TrackerSettings) {
        val obj = JSONObject().apply {
            put("isAutoSyncEnabled", settings.isAutoSyncEnabled)
            put("syncOnEpisodeComplete", settings.syncOnEpisodeComplete)
            put("lastFullSyncTimestamp", settings.lastFullSyncTimestamp)
        }
        prefs.edit().putString(KEY_TRACKER_SETTINGS, obj.toString()).apply()
    }

    // --- Time Spent Activity Tracking ---
    private fun getTimeSpentKey(userId: String?): String {
        val clean = userId?.trim()
        return if (!clean.isNullOrBlank() && clean != "null" && clean != "guest") {
            "key_time_spent_$clean"
        } else {
            "key_time_spent_guest"
        }
    }

    fun getTimeSpentRecord(userId: String? = null): TimeSpentRecord {
        // Clear contaminated legacy unkeyed preference if it still exists
        if (prefs.contains(KEY_TIME_SPENT)) {
            prefs.edit().remove(KEY_TIME_SPENT).apply()
        }
        val key = getTimeSpentKey(userId)
        val raw = prefs.getString(key, null) ?: return TimeSpentRecord().checkAndReset()
        return try {
            val obj = JSONObject(raw)
            val todayDate = obj.optString("todayDate", TimeSpentRecord.getTodayDateString())
            val currentWeek = obj.optString("currentWeek", TimeSpentRecord.getCurrentWeekString())

            fun parseStats(statKey: String): ActivityTimeStats {
                val sObj = obj.optJSONObject(statKey) ?: return ActivityTimeStats()
                return ActivityTimeStats(
                    animeSeconds = sObj.optLong("animeSeconds", 0L),
                    mangaSeconds = sObj.optLong("mangaSeconds", 0L),
                    novelSeconds = sObj.optLong("novelSeconds", 0L),
                    appSeconds = sObj.optLong("appSeconds", 0L)
                )
            }

            TimeSpentRecord(
                todayDate = todayDate,
                daily = parseStats("daily"),
                currentWeek = currentWeek,
                weekly = parseStats("weekly"),
                allTime = parseStats("allTime")
            ).checkAndReset()
        } catch (_: Exception) {
            TimeSpentRecord().checkAndReset()
        }
    }

    fun saveTimeSpentRecord(record: TimeSpentRecord, userId: String? = null) {
        fun statsToObj(stats: ActivityTimeStats): JSONObject = JSONObject().apply {
            put("animeSeconds", stats.animeSeconds)
            put("mangaSeconds", stats.mangaSeconds)
            put("novelSeconds", stats.novelSeconds)
            put("appSeconds", stats.appSeconds)
        }

        val obj = JSONObject().apply {
            put("todayDate", record.todayDate)
            put("currentWeek", record.currentWeek)
            put("daily", statsToObj(record.daily))
            put("weekly", statsToObj(record.weekly))
            put("allTime", statsToObj(record.allTime))
        }
        val key = getTimeSpentKey(userId)
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun clearTimeSpentRecord(userId: String? = null) {
        val key = getTimeSpentKey(userId)
        prefs.edit().remove(key).apply()
    }

    // --- CinePlayer Customization Studio Settings ---
    fun getCinePlayerCustomization(): com.nyyrox.aniverse.player.CinePlayerCustomizationSettings {
        val raw = prefs.getString(KEY_CINEPLAYER_CUSTOMIZATION, null)
        return com.nyyrox.aniverse.player.CinePlayerCustomizationSettings.fromJson(raw)
    }

    fun saveCinePlayerCustomization(settings: com.nyyrox.aniverse.player.CinePlayerCustomizationSettings) {
        prefs.edit().putString(KEY_CINEPLAYER_CUSTOMIZATION, settings.toJson()).apply()
    }

    // --- Welcome Notification Template Settings ---
    fun getWelcomeNotificationConfig(): WelcomeNotificationConfig {
        val raw = prefs.getString("key_welcome_notification_config", null) ?: return WelcomeNotificationConfig()
        return try {
            val obj = JSONObject(raw)
            WelcomeNotificationConfig(
                title = obj.optString("title", "Welcome to Zenkai"),
                message = obj.optString("message", "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey"),
                imageUrl = obj.optString("imageUrl", ""),
                actionLabel = obj.optString("actionLabel", "Claim +100 Starter EXP"),
                actionUrl = obj.optString("actionUrl", "zenkai://rewards/welcome")
            )
        } catch (_: Exception) {
            WelcomeNotificationConfig()
        }
    }

    fun saveWelcomeNotificationConfig(config: WelcomeNotificationConfig) {
        try {
            val obj = JSONObject().apply {
                put("title", config.title)
                put("message", config.message)
                put("imageUrl", config.imageUrl)
                put("actionLabel", config.actionLabel)
                put("actionUrl", config.actionUrl)
            }
            prefs.edit().putString("key_welcome_notification_config", obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Notification Local Preferences ---
    fun getReadNotificationIds(): Set<String> {
        return prefs.getStringSet("key_read_notification_ids", emptySet()) ?: emptySet()
    }

    fun markNotificationReadLocal(id: String) {
        val current = getReadNotificationIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("key_read_notification_ids", current).apply()
    }

    fun markAllNotificationsReadLocal(ids: List<String>) {
        val current = getReadNotificationIds().toMutableSet()
        current.addAll(ids)
        prefs.edit().putStringSet("key_read_notification_ids", current).apply()
    }

    fun getDeletedNotificationIds(): Set<String> {
        return prefs.getStringSet("key_deleted_notification_ids", emptySet()) ?: emptySet()
    }

    fun deleteNotificationLocal(id: String) {
        val current = getDeletedNotificationIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("key_deleted_notification_ids", current).apply()
    }

    // --- Share Deep-Linking Pending Route ---
    fun getPendingShare(): String? = prefs.getString("pendingShare", null)
    fun setPendingShare(route: String?) {
        if (route == null) prefs.edit().remove("pendingShare").apply()
        else prefs.edit().putString("pendingShare", route).apply()
    }
}
