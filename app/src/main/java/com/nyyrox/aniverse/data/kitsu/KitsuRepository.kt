package com.nyyrox.aniverse.data.kitsu

import com.nyyrox.aniverse.data.Anime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object KitsuRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://kitsu.io/api/edge"
    private val kitsuToAnilistCache = ConcurrentHashMap<Int, Int>()

    suspend fun resolveAnilistId(kitsuId: Int): Int? = withContext(Dispatchers.IO) {
        kitsuToAnilistCache[kitsuId]?.let { return@withContext it }
        try {
            val req = Request.Builder()
                .url("$BASE_URL/anime/$kitsuId/mappings")
                .header("Accept", "application/vnd.api+json")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body.string())
                val data = json.optJSONArray("data") ?: return@withContext null
                for (i in 0 until data.length()) {
                    val m = data.optJSONObject(i) ?: continue
                    val attr = m.optJSONObject("attributes") ?: continue
                    if (attr.optString("externalSite") == "anilist/anime") {
                        val alId = attr.optString("externalId").toIntOrNull()
                        if (alId != null && alId > 0) {
                            kitsuToAnilistCache[kitsuId] = alId
                            return@withContext alId
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun getTrending(limit: Int = 15): List<Anime> = fetchList("$BASE_URL/trending/anime?limit=$limit")

    suspend fun getPopular(limit: Int = 15): List<Anime> = fetchList("$BASE_URL/anime?sort=-userCount&page[limit]=$limit")

    suspend fun getTopRated(limit: Int = 15): List<Anime> = fetchList("$BASE_URL/anime?sort=-averageRating&page[limit]=$limit")

    suspend fun getByGenre(genre: String, limit: Int = 15): List<Anime> {
        val cat = genre.lowercase().trim()
        return fetchList("$BASE_URL/anime?filter[categories]=$cat&sort=-userCount&page[limit]=$limit")
    }

    suspend fun getNewReleases(limit: Int = 15): List<Anime> = fetchList("$BASE_URL/anime?filter[status]=current&sort=-userCount&page[limit]=$limit")

    suspend fun getUpcoming(limit: Int = 15): List<Anime> = fetchList("$BASE_URL/anime?filter[status]=upcoming&sort=-userCount&page[limit]=$limit")

    private suspend fun fetchList(url: String): List<Anime> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.api+json")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val rawItems = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val json = JSONObject(resp.body.string())
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                val list = mutableListOf<JSONObject>()
                for (i in 0 until data.length()) {
                    data.optJSONObject(i)?.let { list.add(it) }
                }
                list
            }

            coroutineScope {
                rawItems.map { item ->
                    async(Dispatchers.IO) {
                        parseAnime(item)
                    }
                }.mapNotNull { it.await() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun parseAnime(item: JSONObject): Anime? {
        val kitsuId = item.optInt("id")
        if (kitsuId <= 0) return null
        val attr = item.optJSONObject("attributes") ?: return null
        val titles = attr.optJSONObject("titles")
        val englishTitle = titles?.optString("en")?.takeIf { it.isNotBlank() }
            ?: attr.optString("canonicalTitle").takeIf { it.isNotBlank() }
            ?: "Unknown Anime"
        val romajiTitle = titles?.optString("en_jp")?.takeIf { it.isNotBlank() }
            ?: attr.optString("canonicalTitle").takeIf { it.isNotBlank() }

        val posterObj = attr.optJSONObject("posterImage")
        val cover = posterObj?.optString("large")
            ?: posterObj?.optString("original")
            ?: posterObj?.optString("medium")
            ?: ""

        val coverObj = attr.optJSONObject("coverImage")
        val banner = coverObj?.optString("large")
            ?: coverObj?.optString("original")
            ?: cover

        val avgRating = attr.optString("averageRating").toFloatOrNull()
        val score = avgRating?.let { it.toInt() }
        val synopsis = attr.optString("synopsis")
        val episodeCount = attr.optInt("episodeCount").takeIf { it > 0 }
        val statusStr = attr.optString("status").uppercase()
        val subType = attr.optString("subtype").uppercase()

        val startDate = attr.optString("startDate")
        val year = startDate.split("-").firstOrNull()?.toIntOrNull()

        val anilistId = resolveAnilistId(kitsuId) ?: kitsuId

        return Anime(
            id = anilistId,
            idMal = null,
            title = englishTitle,
            romajiTitle = romajiTitle,
            cover = cover,
            banner = banner,
            episodes = episodeCount,
            status = if (statusStr == "CURRENT") "RELEASING" else statusStr,
            score = score,
            description = synopsis,
            format = subType.ifBlank { "TV" },
            seasonYear = year,
            genres = emptyList()
        )
    }
}
