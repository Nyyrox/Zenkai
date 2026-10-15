package com.nyyrox.aniverse.data.tracker

import android.util.Log
import com.nyyrox.aniverse.data.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object MalTrackerClient {
    private const val TAG = "MalTrackerClient"
    const val CLIENT_ID = "197858020eec08ff1adf81c6db61f419"
    const val CODE_CHALLENGE = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567"
    private const val BASE_API = "https://api.myanimelist.net/v2"
    private const val TOKEN_URL = "https://myanimelist.net/v1/oauth2/token"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getOAuthUrl(): String {
        return "https://myanimelist.net/v1/oauth2/authorize?response_type=code&client_id=$CLIENT_ID&code_challenge=$CODE_CHALLENGE&code_challenge_method=plain"
    }

    suspend fun exchangeCodeForToken(code: String): MalAccount = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("code", code)
            .add("code_verifier", CODE_CHALLENGE)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string().orEmpty()
        response.close()

        if (!response.isSuccessful) {
            Log.e(TAG, "Token exchange failed: HTTP ${response.code} - $bodyStr")
            throw Exception("Failed to authorize with MyAnimeList: HTTP ${response.code}")
        }

        val json = JSONObject(bodyStr)
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val expiresIn = json.optLong("expires_in", 2592000L) // Default ~30 days
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        fetchViewer(accessToken, refreshToken, expiresAt)
    }

    suspend fun fetchViewer(
        accessToken: String,
        refreshToken: String? = null,
        expiresAt: Long = 0L
    ): MalAccount = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_API/users/@me")
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string().orEmpty()
        response.close()

        if (!response.isSuccessful) {
            Log.e(TAG, "Fetch viewer failed: HTTP ${response.code} - $bodyStr")
            throw Exception("Failed to fetch user details from MyAnimeList")
        }

        val json = JSONObject(bodyStr)
        val name = json.getString("name")
        val picture = json.optString("picture").takeIf { it.isNotBlank() }

        MalAccount(
            username = name,
            avatarUrl = picture,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    suspend fun fetchUserByUsername(username: String): MalAccount = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        val request = Request.Builder()
            .url("https://api.jikan.moe/v4/users/$cleanName")
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string().orEmpty()
        response.close()

        var avatarUrl: String? = null
        if (response.isSuccessful) {
            try {
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data")
                val images = data?.optJSONObject("images")
                val jpg = images?.optJSONObject("jpg")
                avatarUrl = jpg?.optString("image_url")?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {}
        }

        MalAccount(
            username = cleanName,
            avatarUrl = avatarUrl,
            accessToken = "",
            refreshToken = null,
            expiresAt = 0L,
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    suspend fun fetchUserAnimeListByUsername(username: String): List<WatchlistItem> = withContext(Dispatchers.IO) {
        val cleanName = username.trim()
        // Try Jikan API for public user list first
        val request = Request.Builder()
            .url("https://api.jikan.moe/v4/users/$cleanName/animelist/all")
            .header("Accept", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string().orEmpty()
            response.close()

            if (response.isSuccessful) {
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                val items = mutableListOf<WatchlistItem>()

                for (i in 0 until data.length()) {
                    val entry = data.optJSONObject(i) ?: continue
                    val entryData = entry.optJSONObject("entry") ?: continue
                    val malId = entryData.getInt("mal_id")
                    val title = entryData.optString("title", "MAL Anime $malId")
                    val images = entryData.optJSONObject("images")
                    val jpg = images?.optJSONObject("jpg")
                    val cover = jpg?.optString("large_image_url")?.takeIf { it.isNotBlank() }
                        ?: jpg?.optString("image_url")?.takeIf { it.isNotBlank() }
                        ?: ""

                    val scoreTen = entry.optInt("score", 0)
                    val score = if (scoreTen > 0) scoreTen * 10 else null
                    val statusRaw = entry.optString("status")
                    val zenkaiStatus = TrackerStatusMapper.malToZenkai(statusRaw)
                    val progress = entry.optInt("episodes_watched", 0)
                    val now = System.currentTimeMillis()

                    items.add(
                        WatchlistItem(
                            animeId = malId,
                            title = title,
                            cover = cover,
                            banner = null,
                            score = score,
                            genres = emptyList(),
                            episodes = null,
                            status = zenkaiStatus,
                            progressEpisode = progress,
                            releaseYear = null,
                            customTags = emptyList(),
                            addedAt = now,
                            updatedAt = now
                        )
                    )
                }
                if (items.isNotEmpty()) return@withContext items
            }
        } catch (e: Exception) {
            Log.w(TAG, "Jikan public list fetch failed, falling back to MAL API", e)
        }

        // Fallback: MAL official user animelist
        val malUrl = "$BASE_API/users/$cleanName/animelist?fields=list_status{status,score,num_episodes_watched,updated_at},num_episodes,main_picture,genres,start_season,title&limit=500"
        val malReq = Request.Builder()
            .url(malUrl)
            .header("X-MAL-CLIENT-ID", CLIENT_ID)
            .build()

        try {
            val response = client.newCall(malReq).execute()
            val bodyStr = response.body?.string().orEmpty()
            response.close()

            if (response.isSuccessful) {
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                return@withContext parseMalDataArray(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MAL API public list fetch error", e)
        }

        emptyList()
    }

    suspend fun fetchUserAnimeList(accessToken: String): List<WatchlistItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_API/users/@me/animelist?fields=list_status{status,score,num_episodes_watched,updated_at},num_episodes,main_picture,genres,start_season,title&limit=500"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string().orEmpty()
        response.close()

        if (!response.isSuccessful) {
            Log.e(TAG, "Fetch anime list failed: HTTP ${response.code} - $bodyStr")
            return@withContext emptyList()
        }

        val json = JSONObject(bodyStr)
        val data = json.optJSONArray("data") ?: return@withContext emptyList()
        parseMalDataArray(data)
    }

    private fun parseMalDataArray(data: org.json.JSONArray): List<WatchlistItem> {
        val items = mutableListOf<WatchlistItem>()
        for (i in 0 until data.length()) {
            val nodeWrapper = data.optJSONObject(i) ?: continue
            val node = nodeWrapper.optJSONObject("node") ?: continue
            val listStatus = nodeWrapper.optJSONObject("list_status")

            val malId = node.getInt("id")
            val title = node.optString("title", "MAL Anime $malId")

            val mainPic = node.optJSONObject("main_picture")
            val cover = mainPic?.optString("large")?.takeIf { it.isNotBlank() }
                ?: mainPic?.optString("medium")?.takeIf { it.isNotBlank() }
                ?: ""

            val episodes = if (node.isNull("num_episodes")) null else node.getInt("num_episodes")
            val statusRaw = listStatus?.optString("status")
            val zenkaiStatus = TrackerStatusMapper.malToZenkai(statusRaw)
            val progress = listStatus?.optInt("num_watched_episodes", 0) ?: 0
            val scoreTen = listStatus?.optInt("score", 0) ?: 0
            val score = if (scoreTen > 0) scoreTen * 10 else null

            val genresList = mutableListOf<String>()
            node.optJSONArray("genres")?.let { gArr ->
                for (k in 0 until gArr.length()) {
                    val gObj = gArr.optJSONObject(k)
                    val gName = gObj?.optString("name")
                    if (!gName.isNullOrBlank()) genresList.add(gName)
                }
            }

            val startSeason = node.optJSONObject("start_season")
            val releaseYear = startSeason?.optString("year")?.takeIf { it.isNotBlank() }
            val now = System.currentTimeMillis()

            items.add(
                WatchlistItem(
                    animeId = malId,
                    title = title,
                    cover = cover,
                    banner = null,
                    score = score,
                    genres = genresList,
                    episodes = episodes,
                    status = zenkaiStatus,
                    progressEpisode = progress,
                    releaseYear = releaseYear,
                    customTags = emptyList(),
                    addedAt = now,
                    updatedAt = now
                )
            )
        }
        return items
    }

    suspend fun updateAnimeStatus(
        malId: Int,
        status: String,
        progress: Int,
        scoreTenScale: Int? = null,
        accessToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        val formBuilder = FormBody.Builder()
            .add("status", TrackerStatusMapper.zenkaiToMal(status))
            .add("num_watched_episodes", progress.toString())

        if (scoreTenScale != null && scoreTenScale in 0..10) {
            formBuilder.add("score", scoreTenScale.toString())
        }

        val request = Request.Builder()
            .url("$BASE_API/anime/$malId/my_list_status")
            .patch(formBuilder.build())
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed updateAnimeStatus on MAL for $malId", e)
            false
        }
    }

    suspend fun deleteAnimeStatus(malId: Int, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_API/anime/$malId/my_list_status")
            .delete()
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleteAnimeStatus on MAL for $malId", e)
            false
        }
    }
}
