package com.nyyrox.aniverse.data.mapping

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AnimeMappingEntry(
    val saltId: String,
    val season: Int? = 1,
    val startEpisode: Int? = 1,
    val dekhoSlug: String? = null,
    val episodeOffset: Int? = null
)

data class EpisodeCandidate(
    val saltId: String,
    val season: Int?,
    val finalEp: Int,
    val isMovie: Boolean,
    val source: String,
    val candidateSlugs: List<String> = emptyList(),
    val slug: String = if (isMovie) saltId else "$saltId-${season ?: 1}x$finalEp"
)

object AnimeMappingRepository {
    private const val TAG = "AnimeMappingRepo"

    // Primary: Zenkai Web GitHub anime-mapping.json
    private const val GITHUB_MAPPING_URL = "https://raw.githubusercontent.com/nyyrox2/Zenkai/main/public/anime-mapping.json"
    private const val GITHUB_MAPPING_FALLBACK_URL = "https://raw.githubusercontent.com/nyyrox2/Zenkai/master/public/anime-mapping.json"
    private const val VERCEL_MAPPING_URL = "https://aniversee.vercel.app/anime-mapping.json"
    private const val CACHE_FILE_NAME = "anime-mapping-cache.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mappingCache = ConcurrentHashMap<String, List<AnimeMappingEntry>>().apply {
        // Built-in mapping overrides for Gintama seasons
        put("918", listOf(
            AnimeMappingEntry("gintama", 1, 1, "gintama"),
            AnimeMappingEntry("gintama", 2, 51, "gintama"),
            AnimeMappingEntry("gintama", 3, 102, "gintama"),
            AnimeMappingEntry("gintama", 4, 115, "gintama")
        ))
        put("9969", listOf(AnimeMappingEntry("gintama", 2, 1, "gintama")))
        put("15417", listOf(AnimeMappingEntry("gintama", 2, 1, "gintama")))
        put("20996", listOf(AnimeMappingEntry("gintama", 3, 1, "gintama")))
        put("97889", listOf(AnimeMappingEntry("gintama", 4, 1, "gintama")))
        put("99712", listOf(AnimeMappingEntry("gintama", 5, 1, "gintama")))
        put("100784", listOf(AnimeMappingEntry("gintama", 6, 1, "gintama")))
        put("101925", listOf(AnimeMappingEntry("gintama", 6, 1, "gintama")))

        // Built-in mapping overrides for Mushoku Tensei franchises
        put("108465", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 1, 1, "mushoku-tensei-jobless-reincarnation", 0)))
        put("127720", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 1, 1, "mushoku-tensei-jobless-reincarnation", 11)))
        put("141534", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 1, 1, "mushoku-tensei-jobless-reincarnation", 23)))
        put("146065", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 2, 1, "mushoku-tensei-jobless-reincarnation", 0)))
        put("166873", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 2, 1, "mushoku-tensei-jobless-reincarnation", 13)))
        put("178789", listOf(AnimeMappingEntry("mushoku-tensei-jobless-reincarnation", 3, 1, "mushoku-tensei-jobless-reincarnation", 0)))
    }

    private var isInitialized = false
    private var isSyncedFromRemote = false
    private var appContext: Context? = null

    /**
     * Initializes the mapping repository using the bundled assets/anime-mapping.json
     * (synced with the Zenkai Web GitHub repository) and triggers a background sync.
     */
    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext

        // 1. Try reading disk cache if previously downloaded from GitHub
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists() && cacheFile.length() > 1000) {
                val cachedText = cacheFile.readText(Charsets.UTF_8)
                parseMappingJson(cachedText)
                Log.d(TAG, "Loaded mapping from disk cache (${mappingCache.size} titles)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading disk cache: ${e.message}")
        }

        // 2. Load bundled assets/anime-mapping.json (exact copy from Zenkai Web GitHub)
        try {
            context.assets.open("anime-mapping.json").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    val text = reader.readText()
                    parseMappingJson(text)
                    Log.d(TAG, "Loaded bundled assets mapping (${mappingCache.size} titles)")
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bundled anime-mapping.json", e)
        }

        // 3. Background sync with GitHub Zenkai Web mapping
        CoroutineScope(Dispatchers.IO).launch {
            syncRemoteMappings()
        }
    }

    private fun parseMappingJson(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val list = mutableListOf<AnimeMappingEntry>()
                val optArr = root.optJSONArray(key)
                if (optArr != null) {
                    for (i in 0 until optArr.length()) {
                        val item = optArr.optJSONObject(i) ?: continue
                        val saltId = item.optString("saltId")
                        if (saltId.isNotBlank()) {
                            val season = if (item.isNull("season")) null else item.optInt("season", 1)
                            val startEp = if (item.isNull("startEpisode")) 1 else item.optInt("startEpisode", 1)
                            val dekhoSlug = item.optString("dekhoSlug").takeIf { it.isNotBlank() }
                            val episodeOffset = if (item.has("episodeOffset") && !item.isNull("episodeOffset")) {
                                item.optInt("episodeOffset")
                            } else {
                                null
                            }
                            list.add(AnimeMappingEntry(saltId, season, startEp, dekhoSlug, episodeOffset))
                        }
                    }
                } else {
                    val item = root.optJSONObject(key)
                    if (item != null) {
                        val saltId = item.optString("saltId")
                        if (saltId.isNotBlank()) {
                            val season = if (item.isNull("season")) null else item.optInt("season", 1)
                            val startEp = if (item.isNull("startEpisode")) 1 else item.optInt("startEpisode", 1)
                            val dekhoSlug = item.optString("dekhoSlug").takeIf { it.isNotBlank() }
                            val episodeOffset = if (item.has("episodeOffset") && !item.isNull("episodeOffset")) {
                                item.optInt("episodeOffset")
                            } else {
                                null
                            }
                            list.add(AnimeMappingEntry(saltId, season, startEp, dekhoSlug, episodeOffset))
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    mappingCache[key] = list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing mapping JSON", e)
        }
    }

    /**
     * Cleans an anime title by stripping season, part, cour, year, and colon subtitles.
     */
    fun cleanTitleForSlug(title: String): String {
        return title
            .replace(Regex("""\s*\(\d{4}\)\s*$"""), "")
            .replace(Regex("""\s*Season\s+\d+\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\d+(?:st|nd|rd|th)\s+season\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Final Season\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*:\s*Part\s+\d+\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Part\s+\d+\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*Cour\s+\d+\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[-—]\s*Season\s+\d+\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*:.*$"""), "")
            .trim()
    }

    /**
     * Normalizes an anime title into an SEO-friendly URL slug.
     */
    fun toSlug(title: String): String {
        val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return normalized.lowercase(Locale.US)
            .replace("&", " and ")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
    }

    /**
     * Strips season and part suffixes from an already formatted slug.
     */
    fun normalizeSlug(slug: String): String {
        return slug
            .replace(Regex("""-season-\d+$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""-saison-\d+$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""-\d+(?:st|nd|rd|th)-season$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""-part-\d+$""", RegexOption.IGNORE_CASE), "")
    }

    /**
     * Generates standard candidate suffixes for slug matching.
     */
    fun generateCandidateSlugs(baseSlug: String): List<String> {
        val clean = toSlug(baseSlug)
        if (clean.isBlank()) return emptyList()
        val normalized = normalizeSlug(clean)
        val list = linkedSetOf<String>()
        list.add(clean)
        if (normalized != clean) list.add(normalized)
        if (clean.contains("gintama")) {
            list.add("gintama")
            list.add("gintama-season-3")
            list.add("gintama-2015")
            list.add("gintama-degree")
            list.add("gintama-2011")
            list.add("gintama-enchousen")
        }
        list.add("$clean-hin")
        list.add("$clean-hindi")
        list.add("$clean-dub")
        list.add("$clean-sub")
        return list.toList()
    }

    /**
     * Builds an episode slug formatted as saltId-SxEp (e.g. naruto-1x1).
     */
    fun buildEpisodeSlug(saltId: String, season: Int?, episode: Int): String {
        val s = season ?: 1
        return "$saltId-${s}x$episode"
    }

    /**
     * Retrieves mapping entries for an AniList ID.
     */
    fun getEntries(anilistId: Int): List<AnimeMappingEntry> {
        return mappingCache[anilistId.toString()] ?: emptyList()
    }

    /**
     * Checks whether an AniList ID is present in the mapping cache.
     */
    fun isMapped(anilistId: Int): Boolean {
        if (anilistId <= 0) return false
        return mappingCache.containsKey(anilistId.toString())
    }

    /**
     * Resolves the best mapping candidate for an episode.
     * Follows the exact logic of pickMapping in Zenkai Web server/mapping.js:
     * - Sorts entries descending by startEpisode
     * - Picks entry where episode >= startEpisode
     * - If episodeOffset is defined: finalEp = episode + episodeOffset
     * - If episodeOffset is not defined: finalEp = episode (continuous, does not subtract startEpisode)
     * - isMovie = isMovie || season == null || season == 0
     * - slug = if (isMovie) saltId else "$saltId-${season}x$finalEp"
     */
    fun resolveCandidate(
        anilistId: Int,
        episode: Int,
        fallbackTitle: String? = null,
        isMovie: Boolean = false
    ): EpisodeCandidate {
        val entries = getEntries(anilistId)
        if (entries.isNotEmpty()) {
            val sorted = entries.sortedByDescending { it.startEpisode ?: 1 }
            val picked = sorted.firstOrNull { episode >= (it.startEpisode ?: 1) } ?: entries.first()

            val finalEp = if (picked.episodeOffset != null) {
                maxOf(1, episode + picked.episodeOffset)
            } else {
                episode
            }
            val isMov = isMovie || picked.season == null || picked.season == 0
            val candidates = generateCandidateSlugs(picked.saltId)
            val slug = if (isMov) picked.saltId else "${picked.saltId}-${picked.season ?: 1}x$finalEp"

            return EpisodeCandidate(
                saltId = picked.saltId,
                season = picked.season,
                finalEp = finalEp,
                isMovie = isMov,
                source = "github-zenkai-mapping",
                candidateSlugs = candidates,
                slug = slug
            )
        }

        // Fallback: Clean title slug generation removing Season, Part, Cour, (year)
        val cleanTitle = fallbackTitle?.let { cleanTitleForSlug(it) } ?: "anime"
        val baseSlug = toSlug(cleanTitle).ifBlank { "anime" }
        val normalizedSlug = normalizeSlug(baseSlug)
        val candidates = generateCandidateSlugs(baseSlug)
        val slug = if (isMovie) normalizedSlug else "$normalizedSlug-1x$episode"

        return EpisodeCandidate(
            saltId = normalizedSlug,
            season = 1,
            finalEp = episode,
            isMovie = isMovie,
            source = "auto-slug",
            candidateSlugs = candidates,
            slug = slug
        )
    }

    /**
     * Asynchronously syncs latest mappings from Zenkai Web GitHub repository in background.
     */
    suspend fun syncRemoteMappings(): Boolean = withContext(Dispatchers.IO) {
        val urls = listOf(GITHUB_MAPPING_URL, GITHUB_MAPPING_FALLBACK_URL, VERCEL_MAPPING_URL)
        for (u in urls) {
            try {
                val req = Request.Builder()
                    .url(u)
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Zenkai-Android-App")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body.string()
                        if (body.length > 500) {
                            parseMappingJson(body)
                            isSyncedFromRemote = true
                            Log.d(TAG, "Synced ${mappingCache.size} mappings from GitHub ($u)")

                            // Cache to disk so app retains updated mappings offline
                            appContext?.let { ctx ->
                                try {
                                    val cacheFile = File(ctx.filesDir, CACHE_FILE_NAME)
                                    cacheFile.writeText(body, Charsets.UTF_8)
                                } catch (_: Exception) {}
                            }
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync mapping from $u: ${e.message}")
            }
        }
        false
    }
}
