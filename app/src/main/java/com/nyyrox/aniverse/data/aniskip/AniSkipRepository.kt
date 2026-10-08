package com.nyyrox.aniverse.data.aniskip

import com.nyyrox.aniverse.data.SkipInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object AniSkipRepository {
    private const val BASE_URL = "https://api.aniskip.com/v2/skip-times"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, List<SkipInterval>>()

    /**
     * Retrieves skip intervals (OP, ED, Recap) for a given MyAnimeList ID and episode number.
     */
    suspend fun getSkipTimes(
        malId: Int,
        episode: Int,
        episodeLengthSec: Long = 0L
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        if (malId <= 0 || episode <= 0) return@withContext emptyList()

        val cacheKey = "$malId:$episode"
        if (cache.containsKey(cacheKey)) {
            return@withContext cache[cacheKey] ?: emptyList()
        }

        try {
            val lengthParam = episodeLengthSec.coerceAtLeast(0L)
            val url = "$BASE_URL/$malId/$episode?types[]=op&types[]=ed&types[]=mixed-op&types[]=mixed-ed&types[]=recap&episodeLength=$lengthParam"

            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val body = res.body.string()
                val json = JSONObject(body)
                val found = json.optBoolean("found", false)
                if (!found) {
                    cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val results = json.optJSONArray("results") ?: run {
                    cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val list = mutableListOf<SkipInterval>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val interval = item.optJSONObject("interval") ?: continue
                    val startSec = interval.optDouble("startTime", 0.0)
                    val endSec = interval.optDouble("endTime", 0.0)
                    val skipType = item.optString("skipType", "op")

                    if (endSec > startSec) {
                        list.add(
                            SkipInterval(
                                startTimeMs = (startSec * 1000).toLong(),
                                endTimeMs = (endSec * 1000).toLong(),
                                skipType = skipType
                            )
                        )
                    }
                }

                cache[cacheKey] = list
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
