package com.nyyrox.aniverse.data.giphy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class GifItem(
    val id: String,
    val title: String,
    val url: String,
    val previewUrl: String = url,
    val source: String = "giphy"
)

object GiphyRepository {
    private const val GIPHY_API_KEY = "jyDapujj4daGMpIwAey9opl7mWfYBtiw"
    private const val GIPHY_SEARCH_URL = "https://api.giphy.com/v1/gifs/search"
    private const val GIPHY_TRENDING_URL = "https://api.giphy.com/v1/gifs/trending"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, List<GifItem>>()

    val TOP_ANIME_CATEGORIES = listOf(
        "Trending",
        "Dragon Ball",
        "Naruto",
        "One Piece",
        "Jujutsu Kaisen",
        "Demon Slayer",
        "Bleach",
        "Solo Leveling",
        "Attack on Titan",
        "Reactions",
        "Memes"
    )

    private val CURATED_ANIME_GIFS = mapOf(
        "Trending" to listOf(
            GifItem("cur_tr_1", "Gojo Satoru Domain", "https://media.giphy.com/media/v1.Y2lkPTQxYzFiNDgxeWp2enY2bDJ0enlvaHpoeGZ4bzJwY3FranhmemoydWQ1aTE2c3N2cSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/OU6tgBi0YJ4HK/200.gif"),
            GifItem("cur_tr_2", "Luffy Gear 5 Laugh", "https://media.giphy.com/media/WmkqburJqXziM/giphy.gif"),
            GifItem("cur_tr_3", "Tanjiro Hinokami Kagura", "https://media.giphy.com/media/dyjrpqaUVqCELGuQVr/giphy.gif"),
            GifItem("cur_tr_4", "Sukuna Malevolent Shrine", "https://media.giphy.com/media/4lu5FuhtrbaOQgKN57/giphy.gif")
        ),
        "Dragon Ball" to listOf(
            GifItem("cur_db_1", "Goku Ultra Instinct", "https://media.giphy.com/media/19JSJ5ucu91R5D7a3w/giphy.gif"),
            GifItem("cur_db_2", "Vegeta Super Saiyan Blue", "https://media.giphy.com/media/thZQWKUyDWayA/giphy.gif"),
            GifItem("cur_db_3", "Goku Kamehameha", "https://media.giphy.com/media/oTjoawKEq3wYD5fKEh/giphy.gif"),
            GifItem("cur_db_4", "Gogeta Blue", "https://media.giphy.com/media/HeDLTI576bBgA/giphy.gif")
        ),
        "Naruto" to listOf(
            GifItem("cur_nar_1", "Naruto Rasengan", "https://media.giphy.com/media/doX7ytfiD5ONT09ldZ/giphy.gif"),
            GifItem("cur_nar_2", "Sasuke Chidori", "https://media.giphy.com/media/JRlqKEzTDKcibTOEGgc/giphy.gif"),
            GifItem("cur_nar_3", "Kakashi Sharingan", "https://media.giphy.com/media/kHYWTCigdEKKk/giphy.gif"),
            GifItem("cur_nar_4", "Madara Uchiha Awaken", "https://media.giphy.com/media/sFSkNGLHOCEB2/giphy.gif")
        ),
        "One Piece" to listOf(
            GifItem("cur_op_1", "Luffy Gear 5", "https://media.giphy.com/media/C3brYLms1bhv2/giphy.gif"),
            GifItem("cur_op_2", "Zoro Three Sword Style", "https://media.giphy.com/media/9az09tlYyYNfq/giphy.gif"),
            GifItem("cur_op_3", "Sanji Ifrit Jambe", "https://media.giphy.com/media/DMvXFFMFH252w/giphy.gif"),
            GifItem("cur_op_4", "Luffy Red Roc", "https://media.giphy.com/media/SEaKN8JCUjf2vLWSwg/giphy.gif")
        ),
        "Jujutsu Kaisen" to listOf(
            GifItem("cur_jjk_1", "Gojo Satoru Hollow Purple", "https://media.giphy.com/media/UgV8Y7bDoxzdg4bamY/giphy.gif"),
            GifItem("cur_jjk_2", "Sukuna Fire Arrow", "https://media.giphy.com/media/4lu5FuhtrbaOQgKN57/giphy.gif"),
            GifItem("cur_jjk_3", "Yuji Itadori Black Flash", "https://media.giphy.com/media/fB2IRTXd07IkcZfwVv/giphy.gif"),
            GifItem("cur_jjk_4", "Toji Fushiguro", "https://media.giphy.com/media/108ck8LdAUmJKM/giphy.gif")
        ),
        "Demon Slayer" to listOf(
            GifItem("cur_ds_1", "Tanjiro Sun Breathing", "https://media.giphy.com/media/dyjrpqaUVqCELGuQVr/giphy.gif"),
            GifItem("cur_ds_2", "Zenitsu God Speed", "https://media.giphy.com/media/fTN0kvVN78ysZFgg8a/giphy.gif"),
            GifItem("cur_ds_3", "Rengoku Set Your Heart Ablaze", "https://media.giphy.com/media/jh7F7XwHTywg85WBxL/giphy.gif"),
            GifItem("cur_ds_4", "Nezuko Demon Form", "https://media.giphy.com/media/X83F080ZLLJ5vWkcvV/giphy.gif")
        ),
        "Bleach" to listOf(
            GifItem("cur_bl_1", "Ichigo Kurosaki Bankai", "https://media.giphy.com/media/8uaOiZk0xg2Na/giphy.gif"),
            GifItem("cur_bl_2", "Ichigo Hollow Mask", "https://media.giphy.com/media/M13G8I9Bt2ofsE56Fi/giphy.gif"),
            GifItem("cur_bl_3", "Aizen Sosuke", "https://media.giphy.com/media/13fTar4VWAu85W/giphy.gif")
        ),
        "Solo Leveling" to listOf(
            GifItem("cur_sl_1", "Sung Jin-woo Arise", "https://media.giphy.com/media/B2pYWvuAH2cWA/giphy.gif"),
            GifItem("cur_sl_2", "Shadow Monarch Glow", "https://media.giphy.com/media/11ISwbg5jUhSLS/giphy.gif")
        ),
        "Attack on Titan" to listOf(
            GifItem("cur_aot_1", "Levi Ackerman Spin Attack", "https://media.giphy.com/media/3o7btQ8jDTPGDpgc6I/giphy.gif"),
            GifItem("cur_aot_2", "Eren Yeager Attack Titan Roar", "https://media.giphy.com/media/tliTHiCU40HgCT2HAq/giphy.gif"),
            GifItem("cur_aot_3", "Mikasa Ackerman", "https://media.giphy.com/media/Ku1FyPdoBXVg4/giphy.gif")
        ),
        "Reactions" to listOf(
            GifItem("cur_re_1", "Anime Shock", "https://media.giphy.com/media/13eshB55qyxLQA/giphy.gif"),
            GifItem("cur_re_2", "Anime Cry", "https://media.giphy.com/media/d2lcHJTG5Tscg/giphy.gif"),
            GifItem("cur_re_3", "Anime Mind Blown", "https://media.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif"),
            GifItem("cur_re_4", "Anime Hype / Cheers", "https://media.giphy.com/media/l41JRsph73VokN6ik/giphy.gif")
        ),
        "Memes" to listOf(
            GifItem("cur_me_1", "Anime Nani", "https://media.giphy.com/media/Nszyj17J4fUK0fz5M0/giphy.gif"),
            GifItem("cur_me_2", "Anime Smug Face", "https://media.giphy.com/media/7SF5scGB2AFrgsXP63/giphy.gif"),
            GifItem("cur_me_3", "Anime Popcorn", "https://media.giphy.com/media/gl0mkIZOW6Nwc/giphy.gif")
        )
    )

    suspend fun searchGifs(query: String, limit: Int = 25): List<GifItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext getCategoryGifs("Trending", limit)

        val cacheKey = "search:" + cleanQuery.lowercase() + ":" + limit
        cache[cacheKey]?.let { return@withContext it }

        val apiResults = fetchFromGiphy(cleanQuery, limit)
        if (apiResults.isNotEmpty()) {
            cache[cacheKey] = apiResults
            return@withContext apiResults
        }

        val curatedMatches = CURATED_ANIME_GIFS.values.flatten().filter {
            it.title.contains(cleanQuery, ignoreCase = true)
        }
        if (curatedMatches.isNotEmpty()) {
            cache[cacheKey] = curatedMatches
            return@withContext curatedMatches
        }

        emptyList()
    }

    suspend fun getCategoryGifs(category: String, limit: Int = 25): List<GifItem> = withContext(Dispatchers.IO) {
        val cacheKey = "cat:" + category + ":" + limit
        cache[cacheKey]?.let { return@withContext it }

        val curated = CURATED_ANIME_GIFS[category] ?: emptyList()
        val searchQuery = when (category) {
            "Trending" -> "anime trending"
            "Dragon Ball" -> "dragon ball super anime"
            "Naruto" -> "naruto shippuden anime"
            "One Piece" -> "one piece anime"
            "Jujutsu Kaisen" -> "jujutsu kaisen anime"
            "Demon Slayer" -> "demon slayer kimetsu no yaiba"
            "Bleach" -> "bleach thousand year blood war"
            "Solo Leveling" -> "solo leveling anime"
            "Attack on Titan" -> "attack on titan anime"
            "Reactions" -> "anime reaction"
            "Memes" -> "anime meme"
            else -> category + " anime"
        }

        val apiResults = fetchFromGiphy(searchQuery, limit)
        val combined = (curated + apiResults).distinctBy { if (it.id.isNotBlank()) it.id else it.url }
        val result = if (combined.isNotEmpty()) combined else curated
        cache[cacheKey] = result
        result
    }

    private fun fetchFromGiphy(query: String, limit: Int): List<GifItem> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = GIPHY_SEARCH_URL + "?api_key=" + GIPHY_API_KEY + "&q=" + encodedQuery + "&limit=" + limit + "&rating=g"

            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return emptyList()
                val body = res.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val dataArray = json.optJSONArray("data") ?: return emptyList()

                val list = mutableListOf<GifItem>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    val title = item.optString("title", query)
                    val images = item.optJSONObject("images") ?: continue

                    val fixedHeight = images.optJSONObject("fixed_height")
                    val fixedSmall = images.optJSONObject("fixed_height_small")
                    val original = images.optJSONObject("original")

                    val gifUrl = fixedHeight?.optString("url")
                        ?: original?.optString("url")
                        ?: continue

                    val preview = fixedSmall?.optString("url") ?: gifUrl

                    list.add(
                        GifItem(
                            id = id,
                            title = title,
                            url = gifUrl,
                            previewUrl = preview,
                            source = "giphy"
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
