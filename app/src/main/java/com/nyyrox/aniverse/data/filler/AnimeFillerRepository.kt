package com.nyyrox.aniverse.data.filler

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AnimeFillerInfo(
    val animeId: Int,
    val fillerEpisodes: Set<Int> = emptySet(),
    val mixedEpisodes: Set<Int> = emptySet()
) {
    fun isFiller(episode: Int): Boolean = episode in fillerEpisodes
    fun isMixed(episode: Int): Boolean = episode in mixedEpisodes
    val hasAnyFiller: Boolean get() = fillerEpisodes.isNotEmpty() || mixedEpisodes.isNotEmpty()
}

object AnimeFillerRepository {
    private const val TAG = "AnimeFillerRepo"
    private const val BASE_URL = "https://www.animefillerlist.com/shows"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // In-memory cache by animeId
    private val cache = ConcurrentHashMap<Int, AnimeFillerInfo>()

    suspend fun getFillerInfo(animeId: Int, title: String, romajiTitle: String? = null): AnimeFillerInfo =
        withContext(Dispatchers.IO) {
            if (animeId <= 0) return@withContext AnimeFillerInfo(animeId)
            cache[animeId]?.let { return@withContext it }

            val candidates = generateCandidateSlugs(title, romajiTitle)
            for (slug in candidates) {
                try {
                    val info = fetchFromAfl(slug, animeId)
                    if (info != null) {
                        cache[animeId] = info
                        return@withContext info
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to fetch filler for slug: $slug", e)
                }
            }

            val emptyInfo = AnimeFillerInfo(animeId)
            cache[animeId] = emptyInfo
            emptyInfo
        }

    fun getCachedFillerInfo(animeId: Int): AnimeFillerInfo? = cache[animeId]

    private fun generateCandidateSlugs(title: String, romajiTitle: String?): List<String> {
        val candidates = mutableListOf<String>()

        fun addVariations(raw: String) {
            val base = cleanSlug(raw)
            if (base.isNotBlank()) candidates.add(base)

            if (raw.contains(":")) {
                val noColon = cleanSlug(raw.replace(":", ""))
                if (noColon.isNotBlank()) candidates.add(noColon)

                val beforeColon = cleanSlug(raw.substringBefore(":"))
                if (beforeColon.isNotBlank()) candidates.add(beforeColon)
            }

            if (raw.contains("Season", ignoreCase = true)) {
                val noSeason = cleanSlug(raw.replace(Regex("(?i)Season\\s*\\d+"), ""))
                if (noSeason.isNotBlank()) candidates.add(noSeason)
            }

            if (raw.contains("Part", ignoreCase = true)) {
                val noPart = cleanSlug(raw.replace(Regex("(?i)Part\\s*\\d+"), ""))
                if (noPart.isNotBlank()) candidates.add(noPart)
            }
        }

        if (title.isNotBlank()) addVariations(title)
        if (!romajiTitle.isNullOrBlank() && !romajiTitle.equals(title, ignoreCase = true)) {
            addVariations(romajiTitle)
        }

        return candidates.distinct()
    }

    private fun cleanSlug(raw: String): String {
        return raw.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }

    private fun fetchFromAfl(slug: String, animeId: Int): AnimeFillerInfo? {
        val url = "$BASE_URL/$slug"
        val req = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/1.6.2")
            .addHeader("Accept", "text/html,application/xhtml+xml")
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val html = res.body?.string().orEmpty()
            if (html.isBlank() || !html.contains("EpisodeList", ignoreCase = true)) return null

            val fillerEpisodes = mutableSetOf<Int>()
            val mixedEpisodes = mutableSetOf<Int>()

            // 1. Extract from Condensed summary div
            // <div class="filler">...<span class="Episodes">(...)</span></div>
            val fillerRegex = Pattern.compile("<div class=\"filler\">.*?<span class=\"Episodes\">(.*?)</span>", Pattern.DOTALL)
            val fillerMatcher = fillerRegex.matcher(html)
            if (fillerMatcher.find()) {
                val episodesText = fillerMatcher.group(1).orEmpty()
                parseEpisodeRanges(episodesText, fillerEpisodes)
            }

            // <div class="mixed_canon/filler">...<span class="Episodes">(...)</span></div>
            val mixedRegex = Pattern.compile("<div class=\"mixed_canon/filler\">.*?<span class=\"Episodes\">(.*?)</span>", Pattern.DOTALL)
            val mixedMatcher = mixedRegex.matcher(html)
            if (mixedMatcher.find()) {
                val episodesText = mixedMatcher.group(1).orEmpty()
                parseEpisodeRanges(episodesText, mixedEpisodes)
            }

            // 2. Extract from EpisodeList table rows:
            // <tr class="filler ... id="eps-XX"><td class="Number">XX</td>
            val rowRegex = Pattern.compile("<tr class=\"([^\"]*)\"[^>]*id=\"eps-(\\d+)\"", Pattern.CASE_INSENSITIVE)
            val rowMatcher = rowRegex.matcher(html)
            while (rowMatcher.find()) {
                val classAttr = rowMatcher.group(1).orEmpty().lowercase()
                val epNum = rowMatcher.group(2)?.toIntOrNull() ?: continue
                if (classAttr.contains("filler") && !classAttr.contains("mixed")) {
                    fillerEpisodes.add(epNum)
                } else if (classAttr.contains("mixed")) {
                    mixedEpisodes.add(epNum)
                }
            }

            return AnimeFillerInfo(
                animeId = animeId,
                fillerEpisodes = fillerEpisodes,
                mixedEpisodes = mixedEpisodes
            )
        }
    }

    private fun parseEpisodeRanges(htmlSnippet: String, targetSet: MutableSet<Int>) {
        val cleanText = htmlSnippet.replace(Regex("<[^>]+>"), "")
        val tokens = cleanText.split(",")
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val parts = trimmed.split("-")
                val start = parts.getOrNull(0)?.trim()?.toIntOrNull()
                val end = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (start != null && end != null && end >= start) {
                    for (i in start..end) {
                        targetSet.add(i)
                    }
                }
            } else {
                trimmed.toIntOrNull()?.let { targetSet.add(it) }
            }
        }
    }
}
