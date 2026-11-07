package com.nyyrox.aniverse.data.subtitles

import android.content.Context
import com.nyyrox.aniverse.data.SubtitleCue
import com.nyyrox.aniverse.data.tmdb.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String,
    val url: String,
    val isDefault: Boolean = false,
    val source: String = "OpenSubtitles"
)

object SubtitleRepository {
    private const val TMDB_VERCEL_BASE = "https://aniversee.vercel.app/api/tmdb"
    private const val OPENSUBTITLES_BASE = "https://opensubtitles-v3.strem.io/subtitles"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val imdbCache = ConcurrentHashMap<Int, String?>()
    private val subCache = ConcurrentHashMap<String, List<SubtitleTrack>>()

    private val languageMap = mapOf(
        "eng" to "English", "en" to "English",
        "spa" to "Spanish", "es" to "Spanish",
        "fre" to "French", "fra" to "French", "fr" to "French",
        "deu" to "German", "ger" to "German", "de" to "German",
        "ita" to "Italian", "it" to "Italian",
        "por" to "Portuguese", "pt" to "Portuguese", "pob" to "Portuguese (BR)",
        "ara" to "Arabic", "ar" to "Arabic",
        "rus" to "Russian", "ru" to "Russian",
        "hin" to "Hindi", "hi" to "Hindi",
        "jpn" to "Japanese", "ja" to "Japanese",
        "chi" to "Chinese", "zho" to "Chinese", "zh" to "Chinese",
        "ind" to "Indonesian", "id" to "Indonesian",
        "tha" to "Thai", "th" to "Thai",
        "vie" to "Vietnamese", "vi" to "Vietnamese",
        "kor" to "Korean", "ko" to "Korean",
        "tur" to "Turkish", "tr" to "Turkish",
        "pol" to "Polish", "pl" to "Polish",
        "nld" to "Dutch", "dut" to "Dutch", "nl" to "Dutch"
    )

    fun normalizeLanguage(code: String): String {
        val clean = code.lowercase(Locale.US).trim()
        return languageMap[clean] ?: clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    suspend fun resolveImdbId(anilistId: Int, title: String, isMovie: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (imdbCache.containsKey(anilistId)) return@withContext imdbCache[anilistId]

        // 1. Check AniBridge first for deterministic IMDb ID matching
        if (anilistId > 0) {
            val aniBridge = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(anilistId)
            if (aniBridge?.imdbId != null && aniBridge.imdbId.isNotBlank() && aniBridge.imdbId.startsWith("tt")) {
                imdbCache[anilistId] = aniBridge.imdbId
                return@withContext aniBridge.imdbId
            }
        }

        // 2. Query TMDB external_ids endpoint
        val tmdb = TmdbRepository.resolveTmdbId(
            anilistId = anilistId,
            title = title,
            format = if (isMovie) "MOVIE" else "TV"
        ) ?: TmdbRepository.resolveTmdbId(
            anilistId = anilistId,
            title = com.nyyrox.aniverse.data.mapping.AnimeSeasonMapper.cleanFranchiseTitle(title),
            format = if (isMovie) "MOVIE" else "TV"
        ) ?: run {
            imdbCache[anilistId] = null
            return@withContext null
        }

        try {
            val url = "$TMDB_VERCEL_BASE/${tmdb.type}/${tmdb.id}?append_to_response=external_ids"
            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    imdbCache[anilistId] = null
                    return@withContext null
                }
                val body = res.body.string()
                val json = JSONObject(body)
                val extIds = json.optJSONObject("external_ids")
                val imdbId = extIds?.optString("imdb_id")?.takeIf { it.isNotBlank() && it.startsWith("tt") }
                    ?: json.optString("imdb_id").takeIf { it.isNotBlank() && it.startsWith("tt") }

                imdbCache[anilistId] = imdbId
                return@withContext imdbId
            }
        } catch (_: Exception) {
            imdbCache[anilistId] = null
            return@withContext null
        }
    }

    suspend fun fetchOpenSubtitles(
        anilistId: Int,
        title: String,
        episode: Int,
        season: Int = 1,
        isMovie: Boolean = false
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        val cacheKey = "$anilistId:$season:$episode:$isMovie"
        if (subCache.containsKey(cacheKey)) return@withContext subCache[cacheKey] ?: emptyList()

        val imdbId = resolveImdbId(anilistId, title, isMovie) ?: run {
            subCache[cacheKey] = emptyList()
            return@withContext emptyList()
        }

        val tracks = mutableListOf<SubtitleTrack>()

        val resolvedMapping = if (anilistId > 0 && !isMovie) {
            com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolveAniListEpisode(anilistId, episode)
        } else null

        val targetSeason = resolvedMapping?.tmdbSeason ?: season
        val targetEpisode = resolvedMapping?.tmdbEpisode ?: episode

        val primaryUrl = if (isMovie) {
            "$OPENSUBTITLES_BASE/movie/$imdbId.json"
        } else {
            "$OPENSUBTITLES_BASE/series/$imdbId:$targetSeason:$targetEpisode.json"
        }

        val primarySubs = queryStremioSubs(primaryUrl)
        tracks.addAll(primarySubs)

        // If mapped season/episode didn't return subtitles, fallback to raw season:episode
        if (!isMovie && tracks.isEmpty() && (targetSeason != season || targetEpisode != episode)) {
            val rawUrl = "$OPENSUBTITLES_BASE/series/$imdbId:$season:$episode.json"
            val rawSubs = queryStremioSubs(rawUrl)
            tracks.addAll(rawSubs)
        }

        // Fallback for anime series to Season 0 absolute episode numbering
        if (!isMovie && tracks.isEmpty()) {
            val fallbackUrl = "$OPENSUBTITLES_BASE/series/$imdbId:0:$episode.json"
            val fallbackSubs = queryStremioSubs(fallbackUrl)
            tracks.addAll(fallbackSubs)
        }

        // Deduplicate by language
        val uniqueByLang = mutableMapOf<String, SubtitleTrack>()
        for (t in tracks) {
            val langKey = t.language.lowercase(Locale.US)
            if (!uniqueByLang.containsKey(langKey)) {
                uniqueByLang[langKey] = t
            }
        }

        val result = uniqueByLang.values.sortedBy { if (it.language.equals("English", ignoreCase = true)) 0 else 1 }
        subCache[cacheKey] = result
        result
    }

    private fun queryStremioSubs(apiUrl: String): List<SubtitleTrack> {
        try {
            val req = Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return emptyList()
                val body = res.body.string()
                val json = JSONObject(body)
                val subsArray = json.optJSONArray("subtitles") ?: return emptyList()

                val list = mutableListOf<SubtitleTrack>()
                for (i in 0 until subsArray.length()) {
                    val item = subsArray.optJSONObject(i) ?: continue
                    val subUrl = item.optString("url")
                    if (subUrl.isBlank()) continue

                    val langRaw = item.optString("lang", "eng")
                    val id = item.optString("id", "sub_$i")
                    val fullLang = normalizeLanguage(langRaw)

                    list.add(
                        SubtitleTrack(
                            id = id,
                            label = fullLang,
                            language = fullLang,
                            url = subUrl,
                            isDefault = fullLang.equals("English", ignoreCase = true)
                        )
                    )
                }
                return list
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    /**
     * Searches OpenSubtitles for any show name entered by the user
     */
    suspend fun searchSubtitlesByTitle(
        title: String,
        season: Int = 1,
        episode: Int = 1,
        isMovie: Boolean = false
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext emptyList()

        val cacheKey = "search:${cleanTitle.lowercase(Locale.US)}:$season:$episode:$isMovie"
        if (subCache.containsKey(cacheKey)) return@withContext subCache[cacheKey] ?: emptyList()

        try {
            // 1. Multi-search TMDB for title
            val encodedQuery = URLEncoder.encode(cleanTitle, "UTF-8")
            val searchUrl = "$TMDB_VERCEL_BASE/search/multi?query=$encodedQuery"
            val req = Request.Builder()
                .url(searchUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            var foundId: Int? = null
            var foundType: String = if (isMovie) "movie" else "tv"

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val json = JSONObject(res.body.string())
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        for (i in 0 until results.length()) {
                            val r = results.optJSONObject(i) ?: continue
                            val mediaType = r.optString("media_type")
                            if (mediaType == "tv" || mediaType == "movie") {
                                foundId = r.optInt("id")
                                foundType = mediaType
                                break
                            }
                        }
                    }
                }
            }

            if (foundId == null || foundId <= 0) {
                val franchiseName = com.nyyrox.aniverse.data.mapping.AnimeSeasonMapper.cleanFranchiseTitle(cleanTitle)
                if (franchiseName != cleanTitle) {
                    val enc = URLEncoder.encode(franchiseName, "UTF-8")
                    val fReq = Request.Builder()
                        .url("$TMDB_VERCEL_BASE/search/multi?query=$enc")
                        .addHeader("Accept", "application/json")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                        .build()
                    client.newCall(fReq).execute().use { res ->
                        if (res.isSuccessful) {
                            val json = JSONObject(res.body.string())
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                for (i in 0 until results.length()) {
                                    val r = results.optJSONObject(i) ?: continue
                                    val mediaType = r.optString("media_type")
                                    if (mediaType == "tv" || mediaType == "movie") {
                                        foundId = r.optInt("id")
                                        foundType = mediaType
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (foundId == null || foundId <= 0) {
                subCache[cacheKey] = emptyList()
                return@withContext emptyList()
            }

            // 2. Get IMDb ID for the resolved show
            val extUrl = "$TMDB_VERCEL_BASE/$foundType/$foundId?append_to_response=external_ids"
            val extReq = Request.Builder()
                .url(extUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            var imdbId: String? = null
            client.newCall(extReq).execute().use { res ->
                if (res.isSuccessful) {
                    val json = JSONObject(res.body.string())
                    val extIds = json.optJSONObject("external_ids")
                    imdbId = extIds?.optString("imdb_id")?.takeIf { it.isNotBlank() && it.startsWith("tt") }
                        ?: json.optString("imdb_id").takeIf { it.isNotBlank() && it.startsWith("tt") }
                }
            }

            if (imdbId == null) {
                subCache[cacheKey] = emptyList()
                return@withContext emptyList()
            }

            // 3. Query Stremio OpenSubtitles
            val tracks = mutableListOf<SubtitleTrack>()
            val isShowMovie = isMovie || foundType == "movie"

            val primaryUrl = if (isShowMovie) {
                "$OPENSUBTITLES_BASE/movie/$imdbId.json"
            } else {
                "$OPENSUBTITLES_BASE/series/$imdbId:$season:$episode.json"
            }
            tracks.addAll(queryStremioSubs(primaryUrl))

            if (!isShowMovie && tracks.isEmpty()) {
                val fallbackUrl = "$OPENSUBTITLES_BASE/series/$imdbId:0:$episode.json"
                tracks.addAll(queryStremioSubs(fallbackUrl))
            }

            // Deduplicate by language
            val uniqueByLang = mutableMapOf<String, SubtitleTrack>()
            for (t in tracks) {
                val langKey = t.language.lowercase(Locale.US)
                if (!uniqueByLang.containsKey(langKey)) {
                    uniqueByLang[langKey] = t
                }
            }

            val result = uniqueByLang.values.sortedBy { if (it.language.equals("English", ignoreCase = true)) 0 else 1 }
            subCache[cacheKey] = result
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Client-side direct caption system:
     * Directly retrieves timed WebVTT subtitle tracks for the anime and episode from MegaPlay / Vidstream servers.
     */
    suspend fun fetchDirectCaptions(
        anilistId: Int,
        episode: Int,
        audio: String = "sub"
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        if (anilistId <= 0) return@withContext emptyList()
        val cacheKey = "direct:$anilistId:$episode:${audio.lowercase(Locale.US)}"
        subCache[cacheKey]?.let { return@withContext it }

        // Try primary audio mode first, then "sub" if non-sub had no tracks
        val audioModes = if (audio.equals("sub", ignoreCase = true)) listOf("sub") else listOf(audio.lowercase(Locale.US), "sub")
        val serverList = listOf("bcdn", "vidstream", "tcdn")

        for (aud in audioModes) {
            for (server in serverList) {
                try {
                    val pageUrl = "https://megaplay.buzz/stream/ani/$anilistId/$episode/$aud?s=$server"
                    val pageReq = Request.Builder()
                        .url(pageUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://megaplay.buzz/")
                        .build()

                    val html = client.newCall(pageReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        resp.body.string()
                    } ?: continue

                    val fileIdMatch = Regex("""data-id=["']([^"']+)["']""").find(html)
                    val fileId = fileIdMatch?.groupValues?.get(1) ?: continue

                    val sourceReq = Request.Builder()
                        .url("https://megaplay.buzz/stream/getSources?id=$fileId&s=$server")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .addHeader("Referer", pageUrl)
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build()

                    val tracksJson = client.newCall(sourceReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        val json = JSONObject(resp.body.string())
                        json.optJSONArray("tracks")
                    } ?: continue

                    val directSubs = mutableListOf<SubtitleTrack>()
                    for (i in 0 until tracksJson.length()) {
                        val t = tracksJson.optJSONObject(i) ?: continue
                        val file = t.optString("file").takeIf { it.isNotBlank() } ?: t.optString("url")
                        val label = t.optString("label", "English").trim()
                        if (file.isNotBlank()) {
                            val normLang = normalizeLanguage(label)
                            val isEng = normLang.equals("English", ignoreCase = true) || label.contains("English", ignoreCase = true)
                            val isDef = t.optBoolean("default", isEng)
                            directSubs.add(
                                SubtitleTrack(
                                    id = "direct-$anilistId-$episode-$server-$i",
                                    label = label,
                                    language = normLang,
                                    url = file,
                                    isDefault = isDef,
                                    source = if (server == "vidstream") "Direct (Vidstream)" else "Direct (MegaPlay)"
                                )
                            )
                        }
                    }

                    if (directSubs.isNotEmpty()) {
                        val sorted = directSubs.sortedWith(
                            compareByDescending<SubtitleTrack> { it.isDefault }
                                .thenByDescending { it.language.equals("English", ignoreCase = true) }
                                .thenBy { it.label }
                        )
                        subCache[cacheKey] = sorted
                        return@withContext sorted
                    }
                } catch (_: Exception) {
                    // Try next server option
                }
            }
        }

        subCache[cacheKey] = emptyList()
        emptyList()
    }

    /**
     * Extracts WebVTT/SRT subtitles from ACDN / AnimeSalt / AnimeDekho players.
     */
    suspend fun fetchAcdnSubtitles(
        anilistId: Int,
        episode: Int,
        animeTitle: String
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        if (anilistId <= 0 || !com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.isMapped(anilistId)) {
            return@withContext emptyList()
        }
        val cacheKey = "acdn:$anilistId:$episode"
        subCache[cacheKey]?.let { return@withContext it }
        try {
            val candidate = com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.resolveCandidate(anilistId, episode, animeTitle)
            val saltId = candidate.saltId.ifBlank {
                com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.normalizeAnimeSaltTitle(animeTitle)
            }
            val season = candidate.season ?: 1
            val finalEp = candidate.finalEp
            val isMovie = candidate.isMovie
            val slug = if (isMovie) saltId else "$saltId-${season}x$finalEp"

            val servers = com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.extractServers(slug, isMovie)
            val candidateUrls = listOfNotNull(
                servers.myStreamUrl,
                servers.abyssUrl
            ) + servers.allServers.map { it.url }

            val subs = mutableListOf<SubtitleTrack>()
            val seenUrls = mutableSetOf<String>()

            for (playerUrl in candidateUrls) {
                try {
                    val asCdnMatch = Regex("""https?://([a-zA-Z0-9_.-]+)/video/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE).find(playerUrl)
                    val targetUrl = if (asCdnMatch != null) {
                        "https://${asCdnMatch.groupValues[1]}/video/${asCdnMatch.groupValues[2]}"
                    } else playerUrl

                    val domain = try { java.net.URI(targetUrl).host ?: "animesalt.cx" } catch (_: Exception) { "animesalt.cx" }
                    val req = Request.Builder()
                        .url(targetUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .addHeader("Referer", "https://animesalt.cx/")
                        .build()
                    val html = client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                    }
                    if (html.isBlank()) continue

                    val pjsRegex = Regex("""(?:playerjsSubtitle|Subtitle)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    val pjsMatch = pjsRegex.find(html)
                    if (pjsMatch != null) {
                        val raw = pjsMatch.groupValues[1].trim()
                        val parts = raw.split(Regex(""",(?=\[|https?://)"""))
                        for (part in parts) {
                            val p = part.trim()
                            if (p.isBlank()) continue
                            val m = Regex("""^\[([^\]]+)\](.*)$""").find(p)
                            val (label, f) = if (m != null) {
                                m.groupValues[1].trim() to m.groupValues[2].trim()
                            } else {
                                "English" to p
                            }
                            var file = if (f.startsWith("//")) "https:$f" else f
                            if (file.startsWith("http") && seenUrls.add(file)) {
                                subs.add(
                                    SubtitleTrack(
                                        id = "acdn-sub-${subs.size}",
                                        label = label,
                                        language = normalizeLanguage(label),
                                        url = file,
                                        isDefault = subs.isEmpty(),
                                        source = "ACDN"
                                    )
                                )
                            }
                        }
                    }

                    val trackRegex = Regex("""<track[^>]+(?:src|data-src)=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                    for (tm in trackRegex.findAll(html)) {
                        val tag = tm.value
                        var file = tm.groupValues[1].trim()
                        if (file.startsWith("//")) file = "https:$file"
                        else if (file.startsWith("/")) file = "https://$domain$file"
                        val labelMatch = Regex("""label=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tag)
                        val langMatch = Regex("""srclang=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tag)
                        val label = labelMatch?.groupValues?.get(1) ?: langMatch?.groupValues?.get(1) ?: "English"
                        if (file.startsWith("http") && seenUrls.add(file)) {
                            subs.add(
                                SubtitleTrack(
                                    id = "acdn-track-${subs.size}",
                                    label = label,
                                    language = normalizeLanguage(label),
                                    url = file,
                                    isDefault = subs.isEmpty(),
                                    source = "ACDN"
                                )
                            )
                        }
                    }
                    if (subs.isNotEmpty()) break
                } catch (_: Exception) {}
            }
            if (subs.isNotEmpty()) {
                subCache[cacheKey] = subs
            }
            subs
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Unified client-side subtitle retrieval:
     * Concurrently retrieves captions from FlixCloud, MegaPlay, Vidstream,
     * and Community OpenSubtitles (Stremio), prioritizing the currently playing server's subtitles.
     */
    suspend fun fetchAllSubtitles(
        anilistId: Int,
        title: String,
        episode: Int,
        season: Int = 1,
        isMovie: Boolean = false,
        audio: String = "sub",
        activeServerName: String? = null
    ): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        coroutineScope {
            val flixDeferred = async {
                try {
                    val flix = com.nyyrox.aniverse.data.extractor.ReAnimeResolver.resolveStream(
                        anilistId = anilistId,
                        episode = episode,
                        audio = audio,
                        animeTitle = title,
                        preferredServer = "FlixCloud"
                    )
                    flix?.subtitles.orEmpty().map { it.copy(source = "FlixCloud") }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val directDeferred = async {
                try {
                    fetchDirectCaptions(anilistId, episode, audio)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val acdnDeferred = async {
                try {
                    fetchAcdnSubtitles(anilistId, episode, title)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val openSubsDeferred = async {
                try {
                    fetchOpenSubtitles(anilistId, title, episode, season, isMovie)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val flixList = flixDeferred.await()
            val directList = directDeferred.await()
            val acdnList = acdnDeferred.await()
            val openList = openSubsDeferred.await()

            val combined = mutableListOf<SubtitleTrack>()
            combined.addAll(flixList)
            for (track in directList) {
                if (combined.none { it.url == track.url }) {
                    combined.add(track)
                }
            }
            for (track in acdnList) {
                if (combined.none { it.url == track.url }) {
                    combined.add(track)
                }
            }
            for (openTrack in openList) {
                if (combined.none { it.url == openTrack.url }) {
                    combined.add(openTrack)
                }
            }

            val serverKey = activeServerName?.lowercase(Locale.US) ?: ""

            combined.sortedWith(
                compareByDescending<SubtitleTrack> { track ->
                    val src = track.source.lowercase(Locale.US)
                    when {
                        serverKey.isNotBlank() && src.contains(serverKey) -> 3
                        src.contains("flixcloud") || src.contains("megaplay") || src.contains("vidstream") || src.contains("acdn") || src.startsWith("direct") -> 2
                        else -> 1
                    }
                }.thenByDescending {
                    it.isDefault
                }.thenByDescending {
                    it.language.equals("English", ignoreCase = true)
                }.thenBy {
                    it.label
                }
            )
        }
    }

    /**
     * Downloads a subtitle file to the local cache with 3-stage fallback
     * (Direct provider CDN headers, Stremio headers, and StreamProxy fallbacks).
     */
    suspend fun downloadSubtitleFile(context: Context, track: SubtitleTrack): Result<File> = withContext(Dispatchers.IO) {
        if (track.url.startsWith("/") || track.url.startsWith("file://")) {
            val cleanPath = track.url.removePrefix("file://")
            val localFile = File(cleanPath)
            if (localFile.exists() && localFile.length() > 0) {
                return@withContext Result.success(localFile)
            }
        }

        val isDirectSub = track.source.contains("MegaPlay", ignoreCase = true) ||
                track.source.contains("Vidstream", ignoreCase = true) ||
                track.source.contains("Direct", ignoreCase = true) ||
                track.source.contains("Anikoto", ignoreCase = true) ||
                track.url.contains("megaplay", ignoreCase = true) ||
                track.url.contains("vidstream", ignoreCase = true) ||
                track.url.contains("stellarfrontier", ignoreCase = true) ||
                track.url.contains("nexabloom", ignoreCase = true) ||
                track.url.contains("copperhorizon", ignoreCase = true) ||
                track.url.contains("lunarfrontier", ignoreCase = true) ||
                track.url.contains("crystaldrift", ignoreCase = true) ||
                track.url.contains(".world/") ||
                track.url.contains(".top/") ||
                track.url.contains(".website/")

        val defaultReferer = when {
            isDirectSub -> "https://megaplay.buzz/"
            track.url.contains("strem.io") -> "https://web.stremio.com/"
            else -> "https://flixcloud.cc/"
        }

        val safeId = track.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val isAss = track.url.contains(".ass", ignoreCase = true) || track.url.contains(".ssa", ignoreCase = true)
        val isSrt = track.url.contains(".srt", ignoreCase = true)
        val ext = when {
            isAss -> ".ass"
            isSrt -> ".srt"
            else -> ".vtt"
        }
        val cachedFile = File(context.cacheDir, "sub_${safeId}$ext")
        if (cachedFile.exists() && cachedFile.length() > 50) {
            try {
                val head = cachedFile.readText(Charsets.UTF_8).take(120)
                if (!head.contains("<!DOCTYPE", ignoreCase = true) && !head.contains("<html", ignoreCase = true)) {
                    return@withContext Result.success(cachedFile)
                }
            } catch (_: Exception) {}
        }

        val candidateUrls = mutableListOf<String>()

        try {
            val encoded = URLEncoder.encode(track.url, "UTF-8")
            val refEncoded = URLEncoder.encode(defaultReferer, "UTF-8")
            val cfProxy = com.nyyrox.aniverse.data.config.DynamicConfigManager.getWorkerUrl(
                "zenkai_stream_proxy",
                "https://zenkai-stream-proxy.ammhfoo.workers.dev"
            )

            if (isDirectSub) {
                // Official Zenkai Edge Subtitle Download proxy (authenticates APK via X-Zenkai-Client)
                candidateUrls.add("https://zenkai-web.pages.dev/api/subtitles/download?url=$encoded")
                candidateUrls.add("https://zenkai-web.pages.dev/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add("$cfProxy/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add(track.url)
                candidateUrls.add("https://aniversee.vercel.app/api/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add(com.nyyrox.aniverse.player.StreamProxyServer.buildApiProxyUrl(track.url, defaultReferer, defaultReferer.trimEnd('/')))
            } else {
                candidateUrls.add(track.url)
                if (track.url.contains("subs5.strem.io")) {
                    candidateUrls.add(track.url.replace("subs5.strem.io", "subs.strem.io"))
                }
                candidateUrls.add("https://zenkai-web.pages.dev/api/subtitles/download?url=$encoded")
                candidateUrls.add("https://zenkai-web.pages.dev/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add("$cfProxy/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add("https://aniversee.vercel.app/api/proxy?url=$encoded&referer=$refEncoded")
                candidateUrls.add(com.nyyrox.aniverse.player.StreamProxyServer.buildApiProxyUrl(track.url, defaultReferer, defaultReferer.trimEnd('/')))
            }
        } catch (_: Exception) {
            candidateUrls.add(track.url)
        }

        var lastException: Exception? = null

        for (targetUrl in candidateUrls) {
            try {
                val req = Request.Builder()
                    .url(targetUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                    .addHeader("X-Zenkai-Client", "true")
                    .addHeader("X-Aniverse-Client", "true")
                    .addHeader("Referer", defaultReferer)
                    .addHeader("Accept", "text/vtt,text/plain,text/x-ssa,application/x-subrip,*/*")
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body.bytes()
                        if (body.size >= 16) {
                            val peek = String(body.take(150).toByteArray(), Charsets.UTF_8).trimStart()
                            if (!peek.startsWith("<!DOCTYPE", ignoreCase = true) &&
                                !peek.startsWith("<html", ignoreCase = true) &&
                                !peek.contains("SHIELD_VERIFICATION", ignoreCase = true)
                            ) {
                                cachedFile.writeBytes(body)
                                return@withContext Result.success(cachedFile)
                            }
                        }
                    } else {
                        lastException = Exception("HTTP ${res.code} downloading subtitle from $targetUrl")
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Failed to download subtitle from all available mirrors"))
    }
}

/**
 * High-performance parser for SRT, WebVTT, and ASS subtitles into memory cues.
 */
object SubtitleParser {
    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")
    private val ASS_DIALOGUE_REGEX = Regex("""^Dialogue:\s*[^,]*,([^,]+),([^,]+),[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,[^,]*,(.*)$""", RegexOption.IGNORE_CASE)
    private val VTT_TIMESTAMP_TAG = Regex("""<\d{2}:\d{2}:\d{2}\.\d{3}>""")

    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()
        var i = 0
        val n = lines.size

        while (i < n) {
            val line = lines[i].trim()

            // 1. ASS Dialogue line
            val assMatch = ASS_DIALOGUE_REGEX.find(line)
            if (assMatch != null) {
                val startMs = parseAssTimestamp(assMatch.groupValues[1])
                val endMs = parseAssTimestamp(assMatch.groupValues[2])
                val rawText = assMatch.groupValues[3]
                val cleanText = rawText
                    .replace(Regex("""\{[^}]*\}"""), "")
                    .replace("\\N", "\n")
                    .replace("\\n", "\n")
                    .replace("\\h", " ")
                    .trim()
                if (cleanText.isNotBlank() && endMs > startMs) {
                    cues.add(SubtitleCue(startMs = startMs, endMs = endMs, text = cleanText))
                }
                i++
                continue
            }

            // 2. WebVTT / SRT timestamp line (supports hh:mm:ss.ttt, mm:ss.ttt, and cue positioning tags)
            if (line.contains("-->")) {
                val arrowParts = line.split("-->")
                if (arrowParts.size >= 2) {
                    val rawStart = arrowParts[0].trim()
                    val rawEnd = arrowParts[1].trim().split(Regex("""\s+""")).firstOrNull().orEmpty()

                    val startMs = parseTimestampString(rawStart)
                    val endMs = parseTimestampString(rawEnd)

                    i++
                    val textLines = mutableListOf<String>()
                    while (i < n && lines[i].trim().isNotBlank() && !lines[i].contains("-->")) {
                        val rawL = lines[i]
                            .replace(VTT_TIMESTAMP_TAG, "")
                            .replace(HTML_TAG_REGEX, "")
                            .replace(Regex("""\{[^}]*\}"""), "")
                            .trim()
                        if (rawL.isNotBlank() && !rawL.startsWith("WEBVTT") && !rawL.startsWith("NOTE")) {
                            textLines.add(rawL)
                        }
                        i++
                    }

                    // If the last line is solely digits (e.g. next cue index without a blank separator line), remove it
                    if (textLines.isNotEmpty() && textLines.last().all { it.isDigit() }) {
                        textLines.removeAt(textLines.lastIndex)
                    }

                    val cueText = textLines.joinToString("\n").trim()
                    if (cueText.isNotBlank() && endMs > startMs) {
                        cues.add(SubtitleCue(startMs = startMs, endMs = endMs, text = cueText))
                    }
                    continue
                }
            }

            i++
        }

        return cues.sortedBy { it.startMs }
    }

    private fun parseTimestampString(str: String): Long {
        if (str.isBlank()) return 0L
        val clean = str.trim().replace(',', '.')
        val parts = clean.split(':')
        return try {
            if (parts.size == 3) {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val secParts = parts[2].split('.')
                val s = secParts[0].toLongOrNull() ?: 0L
                val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L else 0L
                (h * 3600 + m * 60 + s) * 1000 + ms
            } else if (parts.size == 2) {
                val m = parts[0].toLongOrNull() ?: 0L
                val secParts = parts[1].split('.')
                val s = secParts[0].toLongOrNull() ?: 0L
                val ms = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L else 0L
                (m * 60 + s) * 1000 + ms
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseAssTimestamp(ts: String): Long {
        val parts = ts.split(":")
        if (parts.size < 3) return 0L
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val secParts = parts[2].split(".")
        val seconds = secParts[0].toLongOrNull() ?: 0L
        val centis = if (secParts.size > 1) secParts[1].padEnd(2, '0').take(2).toLongOrNull() ?: 0L else 0L
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + (centis * 10)
    }
}

/**
 * Binary-search lookup for all subtitle cues active at [positionMs].
 * Handles overlapping dialogue lines, signs, and multi-speaker cues.
 */
fun findActiveCuesAt(cues: List<SubtitleCue>, positionMs: Long): List<SubtitleCue> {
    if (cues.isEmpty()) return emptyList()
    var lo = 0
    var hi = cues.lastIndex
    var candidate = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (cues[mid].startMs <= positionMs) {
            candidate = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    if (candidate < 0) return emptyList()
    val active = ArrayList<SubtitleCue>(2)
    val checkRange = (candidate downTo (candidate - 10).coerceAtLeast(0))
    for (back in checkRange) {
        val c = cues[back]
        if (positionMs in c.startMs..c.endMs) {
            active.add(c)
        }
    }
    return active.reversed()
}

/**
 * Binary-search lookup for the subtitle cue active at [positionMs].
 * O(log n) — replaces the O(n) firstOrNull scan used previously.
 * The cues list MUST be sorted by startMs (SubtitleParser guarantees this).
 */
fun findCueAt(cues: List<SubtitleCue>, positionMs: Long): SubtitleCue? {
    val idx = findCueIndexAt(cues, positionMs)
    return if (idx >= 0) cues[idx] else null
}

/**
 * Binary-search lookup for the index of the subtitle cue active at [positionMs].
 * Gracefully checks preceding cues in case of overlapping lines or long-duration cues.
 * Returns -1 if no cue is active at this timestamp.
 */
fun findCueIndexAt(cues: List<SubtitleCue>, positionMs: Long): Int {
    if (cues.isEmpty()) return -1
    var lo = 0
    var hi = cues.lastIndex
    var candidate = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (cues[mid].startMs <= positionMs) {
            candidate = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    if (candidate < 0) return -1
    val cue = cues[candidate]
    if (positionMs <= cue.endMs) return candidate
    for (back in (candidate - 1) downTo (candidate - 6).coerceAtLeast(0)) {
        val prev = cues[back]
        if (positionMs in prev.startMs..prev.endMs) {
            return back
        }
    }
    return -1
}

/**
 * Real-time & Lookahead Subtitle Translation Service.
 * - Lookahead Prefetching: Pre-translates 4-5 cues ahead in single batch HTTP requests.
 * - Dialogue-level Quality: Preprocesses subtitle markup (SSA tags, WebVTT tags, HTML) and postprocesses dialogue flow.
 * - Zero-lag Cache: Expanded in-memory LRU cache (1500 items) provides instant 0ms lookups.
 * - Robust Backends: Google Translate neural engine (GTX) primary with LibreTranslate fallback.
 */
object TranslationService {

    // Language display name -> ISO 639-1 code
    val SUPPORTED_LANGUAGES = linkedMapOf(
        "English"    to "en",
        "Spanish"    to "es",
        "French"     to "fr",
        "German"     to "de",
        "Japanese"   to "ja",
        "Chinese"    to "zh",
        "Hindi"      to "hi",
        "Arabic"     to "ar",
        "Portuguese" to "pt",
        "Russian"    to "ru",
        "Korean"     to "ko",
        "Indonesian" to "id",
        "Turkish"    to "tr",
        "Vietnamese" to "vi",
        "Italian"    to "it"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // LRU cache keyed by "$targetLangCode:${cleanKey(text)}" — max 1500 entries (~3-4 full episodes)
    private val cache = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 1500
    }

    private fun buildCacheKey(text: String, targetLangCode: String): String {
        val clean = cleanDialogue(text).lowercase(Locale.US).take(140)
        return "$targetLangCode:$clean"
    }

    fun cleanDialogue(rawText: String): String {
        return rawText
            .replace(Regex("""\{[^}]*\}"""), "")       // Strip SSA formatting like {\an8}
            .replace(Regex("""<[^>]+>"""), "")          // Strip HTML tags like <i>, <b>, <font>
            .replace(Regex("""<\d{2}:\d{2}:\d{2}\.\d{3}>"""), "") // Strip WebVTT timestamp tags
            .trim()
    }

    private fun postProcessDialogue(translated: String, hadSpeakerDash: Boolean): String {
        var clean = translated.trim()
        if (hadSpeakerDash && !clean.startsWith("-") && !clean.startsWith("—")) {
            clean = "- $clean"
        }
        return clean
    }

    /**
     * Synchronous cache retrieval for instant 0ms rendering on the active cue.
     */
    fun getCached(text: String?, targetLangCode: String): String? {
        if (text.isNullOrBlank()) return null
        val key = buildCacheKey(text, targetLangCode)
        return synchronized(cache) { cache[key] }
    }

    /**
     * Translates a single text string (returns from cache immediately if present).
     */
    suspend fun translate(text: String, targetLangCode: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        val key = buildCacheKey(text, targetLangCode)
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        val clean = cleanDialogue(text)
        if (clean.isBlank()) return@withContext null
        val hadDash = text.trimStart().startsWith("-") || text.trimStart().startsWith("—")

        val rawTranslated = tryGoogleTranslate(clean, targetLangCode)
            ?: tryLibreTranslate(clean, targetLangCode)
            ?: return@withContext null

        val result = postProcessDialogue(rawTranslated, hadDash)
        synchronized(cache) { cache[key] = result }
        result
    }

    /**
     * Lookahead Batch Prefetch:
     * Pre-translates [lookaheadCount] upcoming cues ahead of [currentIndex] in a SINGLE
     * batch HTTP request so they are already cached in memory when playback reaches them.
     */
    suspend fun prefetchUpcomingCues(
        cues: List<SubtitleCue>,
        currentIndex: Int,
        lookaheadCount: Int = 5,
        targetLangCode: String
    ) = withContext(Dispatchers.IO) {
        if (cues.isEmpty() || currentIndex < 0) return@withContext

        val endIndex = (currentIndex + lookaheadCount).coerceAtMost(cues.size)
        val upcomingCues = cues.subList(currentIndex, endIndex)

        // Filter out cues that are already cached
        val uncached = upcomingCues.filter { cue ->
            val key = buildCacheKey(cue.text, targetLangCode)
            synchronized(cache) { !cache.containsKey(key) }
        }

        if (uncached.isEmpty()) return@withContext

        // If only 1 uncached, translate directly
        if (uncached.size == 1) {
            translate(uncached[0].text, targetLangCode)
            return@withContext
        }

        // Batch translate: join cleaned lines with newline
        val cleanedLines = uncached.map { cue ->
            val cleaned = cleanDialogue(cue.text)
            if (cleaned.isBlank()) "..." else cleaned
        }
        val batchPayload = cleanedLines.joinToString("\n")

        try {
            val batchResult = tryGoogleTranslate(batchPayload, targetLangCode)
            if (batchResult != null) {
                val translatedLines = batchResult.split("\n")
                if (translatedLines.size == uncached.size) {
                    for (i in uncached.indices) {
                        val origCue = uncached[i]
                        val hadDash = origCue.text.trimStart().startsWith("-") || origCue.text.trimStart().startsWith("—")
                        val finalTrans = postProcessDialogue(translatedLines[i], hadDash)
                        val key = buildCacheKey(origCue.text, targetLangCode)
                        synchronized(cache) { cache[key] = finalTrans }
                    }
                    return@withContext
                }
            }
        } catch (_: Exception) {}

        // Fallback: translate individually if batch line splitting mismatched
        for (cue in uncached) {
            translate(cue.text, targetLangCode)
        }
    }

    private fun tryGoogleTranslate(text: String, targetCode: String): String? {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetCode&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val raw = res.body?.string() ?: return null
                val arr = JSONArray(raw)
                val outer = arr.optJSONArray(0) ?: return null
                val sb = StringBuilder()
                for (i in 0 until outer.length()) {
                    val inner = outer.optJSONArray(i) ?: continue
                    sb.append(inner.optString(0))
                }
                sb.toString().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) { null }
    }

    private fun tryLibreTranslate(text: String, targetCode: String): String? {
        return try {
            val jsonBody = JSONObject().apply {
                put("q", text)
                put("source", "auto")
                put("target", targetCode)
                put("format", "text")
            }.toString()
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder()
                .url("https://libretranslate.com/translate")
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val json = JSONObject(res.body?.string() ?: return null)
                json.optString("translatedText").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) { null }
    }
}

