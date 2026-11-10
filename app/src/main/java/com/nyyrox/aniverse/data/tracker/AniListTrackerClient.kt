package com.nyyrox.aniverse.data.tracker

import android.util.Log
import com.nyyrox.aniverse.data.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AniListTrackerClient {
    private const val TAG = "AniListTrackerClient"
    const val CLIENT_ID = "53784"
    const val CLIENT_SECRET = "xoPBUv82QMZooAv1k5jR1PjigVplPG0ZJWt9Fmgf"
    const val REDIRECT_URI = "https://zenkai.cfd/auth/anilist"
    // Clean AniList authorization URL with pin/token redirect
    const val AUTH_URL = "https://anilist.co/api/v2/oauth/authorize?client_id=$CLIENT_ID&response_type=token"
    private const val GRAPHQL_URL = "https://graphql.anilist.co"

    fun getOAuthUrl(): String = AUTH_URL

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun executeGraphQL(query: String, variables: JSONObject = JSONObject(), token: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val requestBuilder = Request.Builder()
            .url(GRAPHQL_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val bodyStr = response.body?.string().orEmpty()
        response.close()

        if (!response.isSuccessful) {
            Log.e(TAG, "GraphQL request failed: HTTP ${response.code} - $bodyStr")
            throw Exception("AniList API error (HTTP ${response.code})")
        }

        JSONObject(bodyStr)
    }

    suspend fun fetchViewer(token: String): AniListAccount = withContext(Dispatchers.IO) {
        val query = """
            query {
                Viewer {
                    id
                    name
                    avatar {
                        large
                        medium
                    }
                }
            }
        """.trimIndent()

        val json = executeGraphQL(query, JSONObject(), token)
        val data = json.optJSONObject("data") ?: throw Exception("Invalid response from AniList")
        val viewer = data.getJSONObject("Viewer")

        val avatar = viewer.optJSONObject("avatar")?.let {
            it.optString("large").takeIf { s -> s.isNotBlank() } ?: it.optString("medium").takeIf { s -> s.isNotBlank() }
        }

        AniListAccount(
            userId = viewer.getInt("id"),
            username = viewer.getString("name"),
            avatarUrl = avatar,
            accessToken = token,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    suspend fun fetchUserByUsername(username: String): AniListAccount = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}name: String) {
                User(name: ${'$'}name) {
                    id
                    name
                    avatar {
                        large
                        medium
                    }
                }
            }
        """.trimIndent()

        val vars = JSONObject().apply {
            put("name", username.trim())
        }

        val json = executeGraphQL(query, vars)
        val data = json.optJSONObject("data") ?: throw Exception("User '$username' not found on AniList")
        val user = data.getJSONObject("User")

        val avatar = user.optJSONObject("avatar")?.let {
            it.optString("large").takeIf { s -> s.isNotBlank() } ?: it.optString("medium").takeIf { s -> s.isNotBlank() }
        }

        AniListAccount(
            userId = user.getInt("id"),
            username = user.getString("name"),
            avatarUrl = avatar,
            accessToken = "",
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    suspend fun fetchUserAnimeList(userId: Int, token: String? = null): List<WatchlistItem> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: ANIME) {
                    lists {
                        name
                        status
                        entries {
                            id
                            status
                            score(format: POINT_100)
                            progress
                            updatedAt
                            media {
                                id
                                idMal
                                title {
                                    userPreferred
                                    english
                                    romaji
                                }
                                coverImage {
                                    extraLarge
                                    large
                                }
                                bannerImage
                                genres
                                episodes
                                startDate {
                                    year
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val vars = JSONObject().apply {
            put("userId", userId)
        }

        val json = executeGraphQL(query, vars, token)
        val data = json.optJSONObject("data") ?: return@withContext emptyList()
        val collection = data.optJSONObject("MediaListCollection") ?: return@withContext emptyList()
        val lists = collection.optJSONArray("lists") ?: return@withContext emptyList()

        val items = mutableListOf<WatchlistItem>()
        val seenIds = mutableSetOf<Int>()

        for (i in 0 until lists.length()) {
            val listObj = lists.optJSONObject(i) ?: continue
            val entries = listObj.optJSONArray("entries") ?: continue

            for (j in 0 until entries.length()) {
                val entry = entries.optJSONObject(j) ?: continue
                val media = entry.optJSONObject("media") ?: continue
                val animeId = media.getInt("id")

                if (seenIds.contains(animeId)) continue
                seenIds.add(animeId)

                val titleObj = media.optJSONObject("title")
                val title = titleObj?.optString("userPreferred")?.takeIf { it.isNotBlank() }
                    ?: titleObj?.optString("english")?.takeIf { it.isNotBlank() }
                    ?: titleObj?.optString("romaji")?.takeIf { it.isNotBlank() }
                    ?: "Anime $animeId"

                val coverObj = media.optJSONObject("coverImage")
                val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                    ?: coverObj?.optString("large")?.takeIf { it.isNotBlank() }
                    ?: ""

                val banner = media.optString("bannerImage").takeIf { it.isNotBlank() }
                val score = if (entry.isNull("score") || entry.optDouble("score", 0.0) <= 0.0) null else entry.optInt("score")
                val statusStr = entry.optString("status")
                val zenkaiStatus = TrackerStatusMapper.aniListToZenkai(statusStr)
                val progress = entry.optInt("progress", 0)
                val episodes = if (media.isNull("episodes")) null else media.getInt("episodes")

                val genresList = mutableListOf<String>()
                media.optJSONArray("genres")?.let { gArr ->
                    for (k in 0 until gArr.length()) {
                        genresList.add(gArr.optString(k))
                    }
                }

                val releaseYear = media.optJSONObject("startDate")?.optString("year")?.takeIf { it.isNotBlank() }
                val updatedAtSec = entry.optLong("updatedAt", 0L)
                val updatedAtMs = if (updatedAtSec > 0) updatedAtSec * 1000L else System.currentTimeMillis()

                items.add(
                    WatchlistItem(
                        animeId = animeId,
                        title = title,
                        cover = cover,
                        banner = banner,
                        score = score,
                        genres = genresList,
                        episodes = episodes,
                        status = zenkaiStatus,
                        progressEpisode = progress,
                        releaseYear = releaseYear,
                        customTags = emptyList(),
                        addedAt = updatedAtMs,
                        updatedAt = updatedAtMs
                    )
                )
            }
        }
        items
    }

    suspend fun saveMediaEntry(
        mediaId: Int,
        status: String,
        progress: Int,
        score: Double? = null,
        token: String
    ): Boolean = withContext(Dispatchers.IO) {
        val query = """
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}score: Float) {
                SaveMediaListEntry (mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress, score: ${'$'}score) {
                    id
                    status
                    progress
                }
            }
        """.trimIndent()

        val aniListStatus = TrackerStatusMapper.zenkaiToAniList(status)
        val vars = JSONObject().apply {
            put("mediaId", mediaId)
            put("status", aniListStatus)
            put("progress", progress)
            if (score != null && score > 0) {
                put("score", score)
            }
        }

        try {
            executeGraphQL(query, vars, token)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save media entry to AniList", e)
            false
        }
    }

    suspend fun deleteMediaEntry(mediaId: Int, token: String): Boolean = withContext(Dispatchers.IO) {
        val query = """
            mutation (${'$'}id: Int) {
                DeleteMediaListEntry (id: ${'$'}id) {
                    deleted
                }
            }
        """.trimIndent()
        val vars = JSONObject().apply {
            put("id", mediaId)
        }
        try {
            executeGraphQL(query, vars, token)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media entry from AniList", e)
            false
        }
    }
}
