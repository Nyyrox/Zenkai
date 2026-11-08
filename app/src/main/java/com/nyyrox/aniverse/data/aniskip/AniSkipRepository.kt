package com.nyyrox.aniverse.data.aniskip

import com.nyyrox.aniverse.data.SkipInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
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
     * Sanitizes and validates timestamps against realistic anime opening/ending bounds to
     * eliminate fake/bogus community submissions.
     */
    suspend fun getSkipTimes(
        malId: Int,
        episode: Int,
        episodeLengthSec: Long = 0L
    ): List<SkipInterval> = withContext(Dispatchers.IO) {
        if (malId <= 0 || episode <= 0) return@withContext emptyList()

        val cacheKey = "$malId:$episode"
        // If we already have a validated non-empty cached result, return it
        val cached = cache[cacheKey]
        if (!cached.isNullOrEmpty()) {
            return@withContext cached
        }
        // If cached is empty list, only return it if we already queried with known episode length
        if (cached != null && episodeLengthSec <= 0L) {
            return@withContext cached
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
                    if (episodeLengthSec > 0L) cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val body = res.body.string()
                val json = JSONObject(body)
                val found = json.optBoolean("found", false)
                if (!found) {
                    if (episodeLengthSec > 0L) cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val results = json.optJSONArray("results") ?: run {
                    if (episodeLengthSec > 0L) cache[cacheKey] = emptyList()
                    return@withContext emptyList()
                }

                val parsed = mutableListOf<SkipInterval>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val interval = item.optJSONObject("interval") ?: continue
                    val startSec = interval.optDouble("startTime", -1.0)
                    val endSec = interval.optDouble("endTime", -1.0)
                    val rawType = item.optString("skipType", "").lowercase(Locale.US)
                    val duration = endSec - startSec

                    // Basic sanity checks
                    if (startSec < 0.0 || endSec <= startSec) continue

                    // Validate anime timing boundaries:
                    // 1. Duration of an opening / ending is typically 90 seconds (never < 25s or > 150s)
                    // 2. An opening (op, mixed-op) must start in the first half / first 10 minutes of an episode
                    // 3. An ending (ed, mixed-ed) must start in the latter portion of the episode, NEVER in the first 7 minutes
                    val isValid = when (rawType) {
                        "op", "mixed-op" -> {
                            val durationValid = duration in 25.0..150.0
                            val startValid = if (episodeLengthSec > 0L) {
                                startSec < (episodeLengthSec * 0.50)
                            } else {
                                startSec < 600.0 // Within first 10 minutes
                            }
                            durationValid && startValid
                        }
                        "ed", "mixed-ed" -> {
                            val durationValid = duration in 25.0..150.0
                            val startValid = if (episodeLengthSec > 0L) {
                                startSec >= (episodeLengthSec * 0.50)
                            } else {
                                startSec >= 480.0 // Must not be earlier than 8 minutes (eliminates fake 3m43s EDs)
                            }
                            durationValid && startValid
                        }
                        "recap" -> {
                            duration in 10.0..240.0 && startSec < 360.0
                        }
                        else -> false
                    }

                    if (isValid) {
                        parsed.add(
                            SkipInterval(
                                startTimeMs = (startSec * 1000).toLong(),
                                endTimeMs = (endSec * 1000).toLong(),
                                skipType = rawType
                            )
                        )
                    }
                }

                // Deduplicate & resolve conflicts:
                // If we have pure "op", ignore "mixed-op".
                // If we have pure "ed", ignore "mixed-ed".
                val hasOp = parsed.any { it.skipType == "op" }
                val hasEd = parsed.any { it.skipType == "ed" }

                val sanitized = parsed.filterNot {
                    (hasOp && it.skipType == "mixed-op") || (hasEd && it.skipType == "mixed-ed")
                }.sortedBy { it.startTimeMs }

                if (sanitized.isNotEmpty() || episodeLengthSec > 0L) {
                    cache[cacheKey] = sanitized
                }
                sanitized
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
