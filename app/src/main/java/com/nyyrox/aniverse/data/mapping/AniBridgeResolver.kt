package com.nyyrox.aniverse.data.mapping

import android.content.Context
import android.util.JsonReader
import android.util.Log
import com.nyyrox.aniverse.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class AniBridgeTargetRange(
    val sourceStart: Int,
    val sourceEnd: Int,
    val targetStart: Int,
    val targetEnd: Int
)

data class AniBridgeShowTarget(
    val id: Int,
    val season: Int,
    val ranges: List<AniBridgeTargetRange> = emptyList()
)

data class AniBridgeEntry(
    val anilistId: Int,
    val tmdbShowTargets: List<AniBridgeShowTarget> = emptyList(),
    val tmdbMovieId: Int? = null,
    val tvdbId: Int? = null,
    val malId: Int? = null,
    val imdbId: String? = null
)

data class AniBridgeResult(
    val anilistId: Int,
    val tmdbId: Int? = null,
    val tmdbType: String? = null, // "tv" or "movie"
    val tvdbId: Int? = null,
    val malId: Int? = null,
    val imdbId: String? = null,
    val title: String? = null,
    val defaultSeason: Int = 1,
    val source: String = "bundled",
    val tmdbShowTargets: List<AniBridgeShowTarget> = emptyList()
)

data class ResolvedEpisodeMapping(
    val anilistId: Int,
    val anilistEpisode: Int,
    val tmdbId: Int,
    val tmdbType: String = "tv", // "tv" or "movie"
    val tmdbSeason: Int = 1,
    val tmdbEpisode: Int,
    val mappingSource: String // "anibridge_remote", "anibridge_cache", "anibridge_bundled", "fallback"
)

/**
 * Official AniBridge v3 Mapping Engine for ZENKAI.
 *
 * Runtime priority:
 * 1. Latest valid mapping from Nyyrox/anibridge-mappings fork
 * 2. Previously downloaded valid local disk cache
 * 3. Bundled APK mapping snapshot (assets/anibridge/mappings.min.json)
 */
object AniBridgeResolver {
    const val ANIBRIDGE_REMOTE_URL = "https://github.com/Nyyrox/anibridge-mappings/releases/download/v3/mappings.min.json"
    const val ANIBRIDGE_BACKUP_URL = "https://raw.githubusercontent.com/Nyyrox/anibridge-mappings/main/data/out/mappings.min.json"

    private const val TAG = "ZENKAI_MAPPING"
    private const val ASSET_PATH = "anibridge/mappings.min.json"
    private const val CACHE_FILE_NAME = "mappings.min.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private var appContext: Context? = null
    private val isInitializing = AtomicBoolean(false)
    private var isLoaded = false
    private var currentSource = "bundled"

    private val anilistIndex = ConcurrentHashMap<Int, AniBridgeEntry>()
    private val tmdbToAnilistIndex = ConcurrentHashMap<String, Int>()
    private val resolvedSeriesCache = ConcurrentHashMap<Int, AniBridgeResult?>()
    private val resolvedEpisodeCache = ConcurrentHashMap<String, ResolvedEpisodeMapping>()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (isInitializing.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                loadPipeline(appContext!!)
            }
        }
    }

    /**
     * Triggers dynamic download and synchronization of the latest AniBridge JSON mappings on page open.
     */
    fun syncLatestMappings(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cacheDir = File(context.filesDir, "anibridge").apply { if (!exists()) mkdirs() }
            val cacheFile = File(cacheDir, CACHE_FILE_NAME)
            fetchRemoteUpdate(cacheFile)
        }
    }

    /**
     * Reverse lookup: finds the AniList anime ID corresponding to a TMDB Show ID and season number.
     */
    fun findAnilistIdForTmdb(tmdbId: Int, season: Int = 1): Int? {
        return tmdbToAnilistIndex["$tmdbId:$season"] ?: tmdbToAnilistIndex["$tmdbId:1"] ?: tmdbToAnilistIndex["$tmdbId:movie"]
    }

    private suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (isLoaded && anilistIndex.isNotEmpty()) return@withContext
        val ctx = appContext ?: return@withContext
        loadPipeline(ctx)
    }

    private fun parseRange(rangeStr: String): Pair<Int, Int> {
        val clean = rangeStr.split("|")[0].split(",")[0].trim()
        return if (clean.contains("-")) {
            val parts = clean.split("-")
            val start = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val end = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].toIntOrNull() ?: Int.MAX_VALUE else Int.MAX_VALUE
            Pair(start, end)
        } else {
            val single = clean.toIntOrNull() ?: 1
            Pair(single, single)
        }
    }

    private fun parseStream(inputStream: InputStream): Map<Int, AniBridgeEntry> {
        val map = HashMap<Int, AniBridgeEntry>(20000)
        JsonReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                if (key.startsWith("anilist:")) {
                    val anilistId = key.removePrefix("anilist:").toIntOrNull()
                    if (anilistId == null) {
                        reader.skipValue()
                        continue
                    }
                    var tmdbMovieId: Int? = null
                    var tvdbId: Int? = null
                    var malId: Int? = null
                    var imdbId: String? = null
                    val showTargets = mutableListOf<AniBridgeShowTarget>()

                    reader.beginObject()
                    while (reader.hasNext()) {
                        val targetKey = reader.nextName()
                        val rangesList = mutableListOf<AniBridgeTargetRange>()
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val srcRangeStr = reader.nextName()
                            val tgtRangeStr = reader.nextString()
                            val src = parseRange(srcRangeStr)
                            val tgt = parseRange(tgtRangeStr)
                            rangesList.add(AniBridgeTargetRange(src.first, src.second, tgt.first, tgt.second))
                        }
                        reader.endObject()

                        when {
                            targetKey.startsWith("tmdb_show:") -> {
                                val parts = targetKey.split(":")
                                val id = parts.getOrNull(1)?.toIntOrNull()
                                val sNum = parts.getOrNull(2)?.removePrefix("s")?.toIntOrNull() ?: 1
                                if (id != null) {
                                    showTargets.add(AniBridgeShowTarget(id, sNum, rangesList))
                                }
                            }
                            targetKey.startsWith("tmdb_movie:") -> {
                                tmdbMovieId = targetKey.removePrefix("tmdb_movie:").toIntOrNull()
                            }
                            targetKey.startsWith("tvdb_show:") -> {
                                tvdbId = targetKey.split(":").getOrNull(1)?.toIntOrNull()
                            }
                            targetKey.startsWith("mal:") -> {
                                malId = targetKey.removePrefix("mal:").toIntOrNull()
                            }
                            targetKey.startsWith("imdb_movie:") || targetKey.startsWith("imdb_show:") -> {
                                imdbId = targetKey.split(":").getOrNull(1)
                            }
                        }
                    }
                    reader.endObject()

                    map[anilistId] = AniBridgeEntry(
                        anilistId = anilistId,
                        tmdbShowTargets = showTargets,
                        tmdbMovieId = tmdbMovieId,
                        tvdbId = tvdbId,
                        malId = malId,
                        imdbId = imdbId
                    )

                    for (target in showTargets) {
                        tmdbToAnilistIndex["${target.id}:${target.season}"] = anilistId
                        if (target.season == 1) {
                            tmdbToAnilistIndex["${target.id}"] = anilistId
                        }
                    }
                    if (tmdbMovieId != null) {
                        tmdbToAnilistIndex["$tmdbMovieId:movie"] = anilistId
                        tmdbToAnilistIndex["$tmdbMovieId"] = anilistId
                    }
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
        }
        return map
    }

    private suspend fun loadPipeline(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, "anibridge").apply { if (!exists()) mkdirs() }
        val cacheFile = File(cacheDir, CACHE_FILE_NAME)

        // 1. Initial Load from Local Cache (if valid) or Bundled Asset
        var loaded = false
        if (cacheFile.exists() && cacheFile.length() > 500_000) {
            try {
                cacheFile.inputStream().use { stream ->
                    val parsed = parseStream(stream)
                    if (parsed.isNotEmpty()) {
                        anilistIndex.clear()
                        anilistIndex.putAll(parsed)
                        currentSource = "anibridge_cache"
                        isLoaded = true
                        loaded = true
                        Log.d(TAG, "[ANIBRIDGE] Source: cache (Entries: ${parsed.size})")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[ANIBRIDGE] Failed to read disk cache, falling back to bundled asset: ${e.message}")
            }
        }

        if (!loaded) {
            try {
                context.assets.open(ASSET_PATH).use { stream ->
                    val parsed = parseStream(stream)
                    if (parsed.isNotEmpty()) {
                        anilistIndex.clear()
                        anilistIndex.putAll(parsed)
                        currentSource = "anibridge_bundled"
                        isLoaded = true
                        Log.d(TAG, "[ANIBRIDGE] Source: bundled asset (Entries: ${parsed.size})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[ANIBRIDGE] Failed to load bundled asset: ${e.message}")
            }
        }

        // 2. Fetch Latest Update from Nyyrox Fork in Background
        fetchRemoteUpdate(cacheFile)
    }

    private suspend fun fetchRemoteUpdate(cacheFile: File) = withContext(Dispatchers.IO) {
        val urls = listOf(ANIBRIDGE_REMOTE_URL, ANIBRIDGE_BACKUP_URL)
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) ZENKAI/v3")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val bodyStream = resp.body.byteStream()
                    val tempFile = File(cacheFile.parentFile, "mappings.tmp")
                    tempFile.outputStream().use { out ->
                        bodyStream.copyTo(out)
                    }

                    // Validate downloaded JSON completely before replacing cache
                    if (tempFile.exists() && tempFile.length() > 500_000) {
                        try {
                            val parsed = tempFile.inputStream().use { parseStream(it) }
                            if (parsed.size > 1000) {
                                if (tempFile.renameTo(cacheFile) || (cacheFile.delete() && tempFile.renameTo(cacheFile))) {
                                    anilistIndex.clear()
                                    anilistIndex.putAll(parsed)
                                    resolvedSeriesCache.clear()
                                    resolvedEpisodeCache.clear()
                                    currentSource = "anibridge_remote"
                                    isLoaded = true
                                    Log.d(TAG, "[ANIBRIDGE] Source: remote fork update (Entries: ${parsed.size})")
                                    return@withContext
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "[ANIBRIDGE] Downloaded JSON invalid, preserving cache: ${e.message}")
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[ANIBRIDGE] Remote fetch failed for $url: ${e.message}")
            }
        }
    }

    /**
     * Resolves AniList series metadata to cross-database IDs (TMDB, TVDB, MAL, IMDb).
     * Retries mapping bridge once before concluding no data or reporting an issue.
     */
    suspend fun resolve(anilistId: Int): AniBridgeResult? = withContext(Dispatchers.IO) {
        if (anilistId <= 0) return@withContext null
        if (resolvedSeriesCache.containsKey(anilistId)) return@withContext resolvedSeriesCache[anilistId]

        ensureLoaded()

        var entry = anilistIndex[anilistId]

        // Retry mapping bridge once before declaring no data
        if (entry == null) {
            Log.w(TAG, "[ANIBRIDGE] AniList ID $anilistId not found in initial index. Retrying mapping bridge once...")
            val ctx = appContext
            if (ctx != null) {
                try {
                    reloadSync(ctx)
                    entry = anilistIndex[anilistId]
                } catch (e: Exception) {
                    Log.w(TAG, "[ANIBRIDGE] Mapping retry failed: ${e.message}")
                }
            }
        }

        if (entry != null) {
            val isMovie = entry.tmdbMovieId != null
            val tmdbId = entry.tmdbMovieId ?: entry.tmdbShowTargets.firstOrNull()?.id
            val tmdbType = if (isMovie) "movie" else if (tmdbId != null) "tv" else null
            val defaultSeason = entry.tmdbShowTargets.firstOrNull()?.season ?: 1

            val res = AniBridgeResult(
                anilistId = anilistId,
                tmdbId = tmdbId,
                tmdbType = tmdbType,
                tvdbId = entry.tvdbId,
                malId = entry.malId,
                imdbId = entry.imdbId,
                defaultSeason = defaultSeason,
                source = currentSource,
                tmdbShowTargets = entry.tmdbShowTargets
            )
            resolvedSeriesCache[anilistId] = res
            logSeries(res)
            return@withContext res
        }

        resolvedSeriesCache[anilistId] = null
        return@withContext null
    }

    /**
     * Resolves an exact AniList episode to its corresponding TMDB show/season/episode.
     * Central authority for episode lists, stills, titles, overviews, and playback.
     */
    suspend fun resolveAniListEpisode(anilistId: Int, episodeNumber: Int): ResolvedEpisodeMapping = withContext(Dispatchers.IO) {
        val cacheKey = "$anilistId:$episodeNumber"
        if (resolvedEpisodeCache.containsKey(cacheKey)) {
            return@withContext resolvedEpisodeCache[cacheKey]!!
        }

        ensureLoaded()

        var entry = anilistIndex[anilistId]

        // Retry mapping bridge once before falling back
        if (entry == null) {
            val ctx = appContext
            if (ctx != null) {
                try {
                    reloadSync(ctx)
                    entry = anilistIndex[anilistId]
                } catch (_: Exception) {}
            }
        }

        if (entry != null) {
            // 1. Movie handling
            if (entry.tmdbMovieId != null) {
                val res = ResolvedEpisodeMapping(
                    anilistId = anilistId,
                    anilistEpisode = episodeNumber,
                    tmdbId = entry.tmdbMovieId,
                    tmdbType = "movie",
                    tmdbSeason = 1,
                    tmdbEpisode = 1,
                    mappingSource = currentSource
                )
                resolvedEpisodeCache[cacheKey] = res
                logEpisode(res)
                return@withContext res
            }

            // 2. Exact episode range matching
            for (target in entry.tmdbShowTargets) {
                for (range in target.ranges) {
                    if (episodeNumber >= range.sourceStart && episodeNumber <= range.sourceEnd) {
                        val offset = episodeNumber - range.sourceStart
                        val mappedEp = range.targetStart + offset
                        val res = ResolvedEpisodeMapping(
                            anilistId = anilistId,
                            anilistEpisode = episodeNumber,
                            tmdbId = target.id,
                            tmdbType = "tv",
                            tmdbSeason = target.season,
                            tmdbEpisode = mappedEp,
                            mappingSource = currentSource
                        )
                        resolvedEpisodeCache[cacheKey] = res
                        logEpisode(res)
                        return@withContext res
                    }
                }
            }

            // 3. Fallback within AniBridge entry (e.g. single target show without ranges)
            val firstTarget = entry.tmdbShowTargets.firstOrNull()
            if (firstTarget != null) {
                val res = ResolvedEpisodeMapping(
                    anilistId = anilistId,
                    anilistEpisode = episodeNumber,
                    tmdbId = firstTarget.id,
                    tmdbType = "tv",
                    tmdbSeason = firstTarget.season,
                    tmdbEpisode = episodeNumber,
                    mappingSource = currentSource
                )
                resolvedEpisodeCache[cacheKey] = res
                logEpisode(res)
                return@withContext res
            }
        }

        // 4. Default Fallback
        val fallback = ResolvedEpisodeMapping(
            anilistId = anilistId,
            anilistEpisode = episodeNumber,
            tmdbId = anilistId,
            tmdbType = "tv",
            tmdbSeason = 1,
            tmdbEpisode = episodeNumber,
            mappingSource = "fallback"
        )
        resolvedEpisodeCache[cacheKey] = fallback
        logEpisode(fallback)
        return@withContext fallback
    }

    private fun logSeries(res: AniBridgeResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, """
                [ANIBRIDGE] Source: ${res.source}
                [ANILIST → TMDB] AniList ID: ${res.anilistId} -> TMDB ID: ${res.tmdbId} (${res.tmdbType})
            """.trimIndent())
        }
    }

    private fun logEpisode(res: ResolvedEpisodeMapping) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, """
                [EPISODE MAP] AniList ID: ${res.anilistId}, Ep: ${res.anilistEpisode} -> TMDB ID: ${res.tmdbId}, Season: ${res.tmdbSeason}, Episode: ${res.tmdbEpisode} (Source: ${res.mappingSource})
            """.trimIndent())
        }
    }

    private fun reloadSync(context: Context) {
        val cacheDir = File(context.filesDir, "anibridge")
        val cacheFile = File(cacheDir, CACHE_FILE_NAME)

        // 1. Check disk cache
        if (cacheFile.exists() && cacheFile.length() > 500_000) {
            try {
                cacheFile.inputStream().use { stream ->
                    val parsed = parseStream(stream)
                    if (parsed.isNotEmpty()) {
                        anilistIndex.putAll(parsed)
                        currentSource = "anibridge_cache"
                        isLoaded = true
                        Log.d(TAG, "[ANIBRIDGE] Mapping retry reloaded ${parsed.size} entries from cache")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[ANIBRIDGE] Disk cache retry failed: ${e.message}")
            }
        }

        // 2. Bundled asset
        try {
            context.assets.open(ASSET_PATH).use { stream ->
                val parsed = parseStream(stream)
                if (parsed.isNotEmpty()) {
                    anilistIndex.putAll(parsed)
                    currentSource = "anibridge_bundled"
                    isLoaded = true
                    Log.d(TAG, "[ANIBRIDGE] Mapping retry reloaded ${parsed.size} entries from bundled asset")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ANIBRIDGE] Bundled asset retry failed: ${e.message}")
        }
    }
}

