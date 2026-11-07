package com.nyyrox.aniverse.data.tmdb

import com.nyyrox.aniverse.data.mapping.AniBridgeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class TmdbArtwork(
    val tmdbId: Int,
    val tmdbType: String,
    val backdropUrl: String?,
    val logoUrl: String?,
    val title: String? = null
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profileUrl: String?
)

data class TmdbVideo(
    val key: String,
    val name: String,
    val type: String,
    val site: String = "YouTube"
)

data class TmdbDetails(
    val tmdbId: Int,
    val tmdbType: String,
    val imdbId: String?,
    val title: String,
    val overview: String?,
    val tagline: String?,
    val voteAverage: Double?,
    val releaseDate: String?,
    val episodeRunTime: Int?,
    val genres: List<String>,
    val backdropUrl: String?,
    val logoUrl: String?,
    val cast: List<TmdbCastMember>,
    val seasons: List<TmdbSeasonInfo> = emptyList(),
    val backdrops: List<String> = emptyList(),
    val posters: List<String> = emptyList(),
    val trailerUrl: String? = null,
    val videos: List<TmdbVideo> = emptyList()
)

data class TmdbSearchResult(
    val id: Int,
    val type: String
)

data class TmdbEpisodeStill(
    val episodeNumber: Int,
    val name: String?,
    val stillUrl: String?,
    val overview: String? = null
)

data class TmdbSeasonInfo(
    val seasonNumber: Int,
    val episodeCount: Int,
    val name: String
)

object TmdbRepository {
    private const val TMDB_VERCEL_BASE = "https://aniversee.vercel.app/api/tmdb"
    private const val TMDB_DIRECT_API_BASE = "https://api.themoviedb.org/3"
    private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"
    private const val TMDB_BEARER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlYjdkMWM0ZTgwMGUzM2FiMmE3Y2I3NDA5YmM4NjQ2YSIsIm5iZiI6MTc3OTUzMDcxOS40MzIsInN1YiI6IjZhMTE3YmRmYTlhNjNlYmFiOWUzYjc4YyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.Z9pa96oJEyicf6wAoaKGKJd9ldapeiOdktoJd4xcgLo"
    private const val TMDB_API_KEY = "eb7d1c4e800e33ab2a7cb7409bc8646a"

    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val artworkCache = ConcurrentHashMap<String, TmdbArtwork?>()
    private val detailsCache = ConcurrentHashMap<String, TmdbDetails?>()
    private val idCache = ConcurrentHashMap<String, TmdbSearchResult?>()

    /**
     * Resilient fetcher: tries Vercel proxy first (which handles CDN caching and bypasses regional ISP blocks),
     * and seamlessly falls back to direct TMDB API with bearer authentication if the proxy is unavailable.
     */
    suspend fun executeTmdbGet(endpointPath: String, queryParams: String = ""): String? = withContext(Dispatchers.IO) {
        val cleanPath = if (endpointPath.startsWith("/")) endpointPath else "/$endpointPath"

        // Tier 1: Vercel Proxy
        try {
            val vercelUrl = if (queryParams.isBlank()) "$TMDB_VERCEL_BASE$cleanPath" else "$TMDB_VERCEL_BASE$cleanPath?$queryParams"
            val req = Request.Builder()
                .url(vercelUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) ZENKAI/v3")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body.string()
                    if (body.isNotBlank() && (body.startsWith("{") || body.startsWith("["))) {
                        return@withContext body
                    }
                }
            }
        } catch (_: Exception) {}

        // Tier 2: Direct TMDB API fallback
        try {
            val sep = if (queryParams.isBlank()) "?" else "?$queryParams&"
            val directUrl = "$TMDB_DIRECT_API_BASE$cleanPath${sep}api_key=$TMDB_API_KEY"
            val req = Request.Builder()
                .url(directUrl)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $TMDB_BEARER_TOKEN")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body.string()
                    if (body.isNotBlank() && (body.startsWith("{") || body.startsWith("["))) {
                        return@withContext body
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace("\\s*\\(\\d{4}\\)\\s*$".toRegex(), "")
            .replace("\\s*Final Season\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s*Season\\s+\\d+\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s*:\\s*Part\\s+\\d+\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s*Part\\s+\\d+\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s*Cour\\s+\\d+\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\\s*[-–]\\s*Season\\s+\\d+\\s*$".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Resolves an anime title to a TMDB { id, type } record, preferring AniBridge mappings
     * when available, and falling back to TMDB Vercel multi-search.
     */
    suspend fun resolveTmdbId(
        anilistId: Int,
        title: String,
        year: Int? = null,
        format: String? = null
    ): TmdbSearchResult? = withContext(Dispatchers.IO) {
        val cacheKey = "$anilistId::${title.lowercase(Locale.US)}::$year"
        if (idCache.containsKey(cacheKey)) return@withContext idCache[cacheKey]

        // 1. Try AniBridge first for deterministic ID matching (with single retry)
        if (anilistId > 0) {
            var aniBridge = AniBridgeResolver.resolve(anilistId)
            if (aniBridge == null || aniBridge.tmdbId == null || aniBridge.tmdbId <= 0) {
                aniBridge = AniBridgeResolver.resolve(anilistId)
            }
            if (aniBridge?.tmdbId != null && aniBridge.tmdbId > 0) {
                val type = aniBridge.tmdbType ?: if (format.equals("MOVIE", ignoreCase = true)) "movie" else "tv"
                val res = TmdbSearchResult(aniBridge.tmdbId, type)
                idCache[cacheKey] = res
                return@withContext res
            }
        }

        // 2. Fallback: Query TMDB Vercel multi-search
        val isMovie = format.equals("MOVIE", ignoreCase = true)
        val preferredType = if (isMovie) "movie" else "tv"
        val query = cleanTitle(title)
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        try {
            val raw = executeTmdbGet("/search/multi", "query=$encodedQuery")
            if (raw.isNullOrBlank()) {
                idCache[cacheKey] = null
                return@withContext null
            }
            val json = JSONObject(raw)
                val results = json.optJSONArray("results") ?: return@withContext null

                var bestMatch: TmdbSearchResult? = null

                for (i in 0 until results.length()) {
                    val r = results.optJSONObject(i) ?: continue
                    val mediaType = r.optString("media_type")
                    if (mediaType != "movie" && mediaType != "tv") continue

                    val id = r.optInt("id")
                    if (id <= 0) continue

                    // Check if matching preferred media type
                    if (mediaType == preferredType) {
                        bestMatch = TmdbSearchResult(id, mediaType)
                        break
                    } else if (bestMatch == null) {
                        bestMatch = TmdbSearchResult(id, mediaType)
                    }
                }

                idCache[cacheKey] = bestMatch
                return@withContext bestMatch
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * Fetches high-res TMDB artwork (backdrop & transparent title logo PNG)
     * using the Vercel TMDB proxy.
     */
    suspend fun getArtwork(
        anilistId: Int,
        title: String,
        year: Int? = null,
        format: String? = null
    ): TmdbArtwork? = withContext(Dispatchers.IO) {
        val cacheKey = "$anilistId::${title.lowercase(Locale.US)}"
        if (artworkCache.containsKey(cacheKey)) return@withContext artworkCache[cacheKey]

        val resolved = resolveTmdbId(anilistId, title, year, format) ?: run {
            artworkCache[cacheKey] = null
            return@withContext null
        }

        try {
            val raw = executeTmdbGet("/${resolved.type}/${resolved.id}", "append_to_response=images&include_image_language=en,null")
            if (raw.isNullOrBlank()) {
                artworkCache[cacheKey] = null
                return@withContext null
            }
            val json = JSONObject(raw)

                // Pick Backdrop
                val backdropPath = json.optString("backdrop_path").takeIf { it.isNotBlank() }
                val imagesObj = json.optJSONObject("images")
                var selectedBackdrop = backdropPath

                val backdropsArr = imagesObj?.optJSONArray("backdrops")
                if (backdropsArr != null && backdropsArr.length() > 0) {
                    for (i in 0 until backdropsArr.length()) {
                        val b = backdropsArr.optJSONObject(i) ?: continue
                        val path = b.optString("file_path")
                        val width = b.optInt("width", 0)
                        if (path.isNotBlank() && width >= 1280) {
                            selectedBackdrop = path
                            break
                        }
                    }
                }

                val backdropUrl = selectedBackdrop?.let { "$TMDB_IMAGE_BASE/w1280$it" }

                // Pick Transparent Title Logo PNG (Prefers English or Null ISO language, skips SVG)
                var selectedLogoPath: String? = null
                val logosArr = imagesObj?.optJSONArray("logos")
                if (logosArr != null && logosArr.length() > 0) {
                    var bestScore = -1
                    for (i in 0 until logosArr.length()) {
                        val l = logosArr.optJSONObject(i) ?: continue
                        val path = l.optString("file_path")
                        if (path.isBlank() || path.endsWith(".svg", ignoreCase = true)) continue

                        val lang = l.optString("iso_639_1")
                        var score = 0
                        if (lang == "en") score = 10
                        else if (lang.isBlank() || lang == "null") score = 5
                        else score = 1

                        val votes = l.optDouble("vote_average", 0.0)
                        score += votes.toInt()

                        if (score > bestScore) {
                            bestScore = score
                            selectedLogoPath = path
                        }
                    }
                }

                val logoUrl = selectedLogoPath?.let { "$TMDB_IMAGE_BASE/w500$it" }
                val tmdbTitle = json.optString("name").ifBlank { json.optString("title") }.takeIf { it.isNotBlank() }

                val artwork = TmdbArtwork(
                    tmdbId = resolved.id,
                    tmdbType = resolved.type,
                    backdropUrl = backdropUrl,
                    logoUrl = logoUrl,
                    title = tmdbTitle
                )

                artworkCache[cacheKey] = artwork
                return@withContext artwork
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * Fetches complete TMDB metadata (overview, tagline, cast, genres, ratings, artwork)
     */
    suspend fun getDetails(
        anilistId: Int,
        title: String,
        year: Int? = null,
        format: String? = null
    ): TmdbDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "$anilistId::${title.lowercase(Locale.US)}::$year"
        if (detailsCache.containsKey(cacheKey)) return@withContext detailsCache[cacheKey]

        val resolved = resolveTmdbId(anilistId, title, year, format) ?: run {
            detailsCache[cacheKey] = null
            return@withContext null
        }

        try {
            val raw = executeTmdbGet("/${resolved.type}/${resolved.id}", "append_to_response=credits,images,external_ids,videos&include_image_language=en,null")
            if (raw.isNullOrBlank()) {
                detailsCache[cacheKey] = null
                return@withContext null
            }
            val json = JSONObject(raw)

                val showTitle = json.optString("title").ifBlank { json.optString("name", title) }
                val overview = json.optString("overview").takeIf { it.isNotBlank() }
                val tagline = json.optString("tagline").takeIf { it.isNotBlank() }
                val voteAverage = if (json.has("vote_average")) json.optDouble("vote_average").takeIf { it > 0.0 } else null
                val releaseDate = json.optString("release_date").ifBlank { json.optString("first_air_date") }.takeIf { it.isNotBlank() }

                var runtime: Int? = null
                if (json.has("runtime")) {
                    runtime = json.optInt("runtime").takeIf { it > 0 }
                } else if (json.has("episode_run_time")) {
                    val runtimes = json.optJSONArray("episode_run_time")
                    if (runtimes != null && runtimes.length() > 0) {
                        runtime = runtimes.optInt(0).takeIf { it > 0 }
                    }
                }

                val genresList = mutableListOf<String>()
                val genresArr = json.optJSONArray("genres")
                if (genresArr != null) {
                    for (i in 0 until genresArr.length()) {
                        val g = genresArr.optJSONObject(i) ?: continue
                        val gName = g.optString("name")
                        if (gName.isNotBlank()) genresList.add(gName)
                    }
                }

                val extIds = json.optJSONObject("external_ids")
                val imdbId = extIds?.optString("imdb_id")?.takeIf { it.isNotBlank() && it.startsWith("tt") }
                    ?: json.optString("imdb_id").takeIf { it.isNotBlank() && it.startsWith("tt") }

                // Cast
                val castList = mutableListOf<TmdbCastMember>()
                val creditsObj = json.optJSONObject("credits")
                val castArr = creditsObj?.optJSONArray("cast")
                if (castArr != null) {
                    val maxCast = minOf(castArr.length(), 24)
                    for (i in 0 until maxCast) {
                        val c = castArr.optJSONObject(i) ?: continue
                        val cId = c.optInt("id")
                        val cName = c.optString("name")
                        if (cName.isBlank()) continue
                        val cChar = c.optString("character").takeIf { it.isNotBlank() }
                        val cProfile = c.optString("profile_path").takeIf { it.isNotBlank() }
                        val cProfileUrl = cProfile?.let { "$TMDB_IMAGE_BASE/w185$it" }
                        castList.add(
                            TmdbCastMember(
                                id = cId,
                                name = cName,
                                character = cChar,
                                profileUrl = cProfileUrl
                            )
                        )
                    }
                }

                // Images
                val backdropPath = json.optString("backdrop_path").takeIf { it.isNotBlank() }
                val imagesObj = json.optJSONObject("images")
                var selectedBackdrop = backdropPath

                val allBackdrops = mutableListOf<String>()
                val backdropsArr = imagesObj?.optJSONArray("backdrops")
                if (backdropsArr != null && backdropsArr.length() > 0) {
                    for (i in 0 until backdropsArr.length()) {
                        val b = backdropsArr.optJSONObject(i) ?: continue
                        val path = b.optString("file_path")
                        if (path.isNotBlank()) {
                            val url = "$TMDB_IMAGE_BASE/w1280$path"
                            if (!allBackdrops.contains(url)) {
                                allBackdrops.add(url)
                            }
                            val width = b.optInt("width", 0)
                            if (selectedBackdrop == null && width >= 1280) {
                                selectedBackdrop = path
                            }
                        }
                    }
                }
                val backdropUrl = selectedBackdrop?.let { "$TMDB_IMAGE_BASE/w1280$it" } ?: allBackdrops.firstOrNull()

                val allPosters = mutableListOf<String>()
                val postersArr = imagesObj?.optJSONArray("posters")
                if (postersArr != null && postersArr.length() > 0) {
                    for (i in 0 until postersArr.length()) {
                        val p = postersArr.optJSONObject(i) ?: continue
                        val path = p.optString("file_path")
                        if (path.isNotBlank()) {
                            val url = "$TMDB_IMAGE_BASE/w500$path"
                            if (!allPosters.contains(url)) {
                                allPosters.add(url)
                            }
                        }
                    }
                }

                var selectedLogoPath: String? = null
                val logosArr = imagesObj?.optJSONArray("logos")
                if (logosArr != null && logosArr.length() > 0) {
                    var bestScore = -1
                    for (i in 0 until logosArr.length()) {
                        val l = logosArr.optJSONObject(i) ?: continue
                        val path = l.optString("file_path")
                        if (path.isBlank() || path.endsWith(".svg", ignoreCase = true)) continue

                        val lang = l.optString("iso_639_1")
                        var score = 0
                        if (lang == "en") score = 10
                        else if (lang.isBlank() || lang == "null") score = 5
                        else score = 1

                        val votes = l.optDouble("vote_average", 0.0)
                        score += votes.toInt()

                        if (score > bestScore) {
                            bestScore = score
                            selectedLogoPath = path
                        }
                    }
                }
                val logoUrl = selectedLogoPath?.let { "$TMDB_IMAGE_BASE/w500$it" }

                // Videos parsing (Trailers, Teasers, Featurettes, Clips, etc.)
                val videosList = mutableListOf<TmdbVideo>()
                var trailerUrl: String? = null
                val videosObj = json.optJSONObject("videos")
                val videosArr = videosObj?.optJSONArray("results")
                if (videosArr != null && videosArr.length() > 0) {
                    for (i in 0 until videosArr.length()) {
                        val v = videosArr.optJSONObject(i) ?: continue
                        val site = v.optString("site")
                        val key = v.optString("key")
                        val vName = v.optString("name").ifBlank { "Video ${i + 1}" }
                        val vType = v.optString("type").ifBlank { "Clip" }
                        if (site.equals("YouTube", ignoreCase = true) && key.isNotBlank()) {
                            videosList.add(TmdbVideo(key = key, name = vName, type = vType, site = site))
                            if (vType.equals("Trailer", ignoreCase = true) && trailerUrl == null) {
                                trailerUrl = key
                            } else if (trailerUrl == null) {
                                trailerUrl = key
                            }
                        }
                    }
                }

                // Seasons parsing for TV series
                val seasonsList = mutableListOf<TmdbSeasonInfo>()
                val seasonsArr = json.optJSONArray("seasons")
                if (seasonsArr != null) {
                    for (i in 0 until seasonsArr.length()) {
                        val s = seasonsArr.optJSONObject(i) ?: continue
                        val sNum = s.optInt("season_number", -1)
                        if (sNum <= 0) continue // Exclude Season 0 (Specials)
                        val epCount = s.optInt("episode_count", 0)
                        val sName = s.optString("name", "Season $sNum")
                        seasonsList.add(TmdbSeasonInfo(sNum, epCount, sName))
                    }
                }

                val details = TmdbDetails(
                    tmdbId = resolved.id,
                    tmdbType = resolved.type,
                    imdbId = imdbId,
                    title = showTitle,
                    overview = overview,
                    tagline = tagline,
                    voteAverage = voteAverage,
                    releaseDate = releaseDate,
                    episodeRunTime = runtime,
                    genres = genresList,
                    backdropUrl = backdropUrl,
                    logoUrl = logoUrl,
                    cast = castList,
                    seasons = seasonsList,
                    backdrops = allBackdrops,
                    posters = allPosters,
                    trailerUrl = trailerUrl,
                    videos = videosList
                )

                // If AniBridge or resolved season indicates sequel season (> 1), fetch season-specific poster
                val aniBridge = if (anilistId > 0) AniBridgeResolver.resolve(anilistId) else null
                val defaultSeason = aniBridge?.defaultSeason ?: 1
                if (resolved.type == "tv" && defaultSeason > 1) {
                    try {
                        val seasonRaw = executeTmdbGet("/tv/${resolved.id}/season/$defaultSeason")
                        if (!seasonRaw.isNullOrBlank()) {
                            val sJson = JSONObject(seasonRaw)
                            val sPoster = sJson.optString("poster_path").takeIf { it.isNotBlank() }
                            if (sPoster != null) {
                                val sPosterUrl = "$TMDB_IMAGE_BASE/w500$sPoster"
                                if (!allPosters.contains(sPosterUrl)) {
                                    allPosters.add(0, sPosterUrl)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                detailsCache[cacheKey] = details
                return@withContext details
        } catch (e: Exception) {
            return@withContext null
        }
    }

    private val showSeasonsCache = ConcurrentHashMap<Int, List<TmdbSeasonInfo>>()
    private val rawSeasonCache = ConcurrentHashMap<String, List<TmdbEpisodeStill>>()
    private val mappedEpisodesCache = ConcurrentHashMap<String, Map<Int, TmdbEpisodeStill>>()

    /**
     * Fetches TV show season metadata (season numbers & episode counts).
     */
    private suspend fun fetchShowSeasons(tmdbId: Int): List<TmdbSeasonInfo> = withContext(Dispatchers.IO) {
        val cached = showSeasonsCache[tmdbId]
        if (cached != null) return@withContext cached

        try {
            val raw = executeTmdbGet("/tv/$tmdbId")
            if (raw.isNullOrBlank()) {
                showSeasonsCache[tmdbId] = emptyList()
                return@withContext emptyList()
            }
            val json = JSONObject(raw)
                val seasonsArr = json.optJSONArray("seasons") ?: return@withContext emptyList()

                val seasonsList = mutableListOf<TmdbSeasonInfo>()
                for (i in 0 until seasonsArr.length()) {
                    val sObj = seasonsArr.optJSONObject(i) ?: continue
                    val sNum = sObj.optInt("season_number", -1)
                    val epCount = sObj.optInt("episode_count", 0)
                    val sName = sObj.optString("name", "")
                    if (sNum >= 1 && epCount > 0) {
                        seasonsList.add(TmdbSeasonInfo(sNum, epCount, sName))
                    }
                }
                seasonsList.sortBy { it.seasonNumber }
                showSeasonsCache[tmdbId] = seasonsList
                return@withContext seasonsList
        } catch (_: Exception) {
            showSeasonsCache[tmdbId] = emptyList()
            return@withContext emptyList()
        }
    }

    /**
     * Fetches raw episode stills and titles for a specific TMDB season.
     */
    private suspend fun fetchSingleSeasonEpisodesRaw(
        tmdbId: Int,
        seasonNumber: Int
    ): List<TmdbEpisodeStill> = withContext(Dispatchers.IO) {
        val cacheKey = "raw_season:$tmdbId:$seasonNumber"
        val cached = rawSeasonCache[cacheKey]
        if (cached != null) return@withContext cached

        try {
            val raw = executeTmdbGet("/tv/$tmdbId/season/$seasonNumber")
            if (raw.isNullOrBlank()) {
                rawSeasonCache[cacheKey] = emptyList()
                return@withContext emptyList()
            }
            val json = JSONObject(raw)
                val episodesArr = json.optJSONArray("episodes") ?: return@withContext emptyList()

                val list = ArrayList<TmdbEpisodeStill>(episodesArr.length())
                for (i in 0 until episodesArr.length()) {
                    val epObj = episodesArr.optJSONObject(i) ?: continue
                    val epNum = epObj.optInt("episode_number")
                    if (epNum <= 0) continue

                    val name = epObj.optString("name").takeIf { it.isNotBlank() }
                    val stillPath = epObj.optString("still_path").takeIf { it.isNotBlank() }
                    val fullStillUrl = stillPath?.let { "$TMDB_IMAGE_BASE/w500$it" }
                    val overview = epObj.optString("overview").takeIf { it.isNotBlank() }

                    list.add(
                        TmdbEpisodeStill(
                            episodeNumber = epNum,
                            name = name,
                            stillUrl = fullStillUrl,
                            overview = overview
                        )
                    )
                }

                rawSeasonCache[cacheKey] = list
                return@withContext list
        } catch (_: Exception) {
            rawSeasonCache[cacheKey] = emptyList()
            return@withContext emptyList()
        }
    }

    /**
     * Comprehensive episode mapping engine for AniList + TMDB:
     * - Multi-season continuous anime (e.g. One Piece, Detective Conan, Naruto, Bleach)
     *   are fetched across all TMDB seasons and mapped cumulatively (1..1122+).
     * - Specific sequel seasons (e.g. Jujutsu Kaisen S2, Demon Slayer S4, MHA S7)
     *   are mapped to both relative (1..N) and absolute cumulative numbering.
     * - Movies / OVAs are mapped to episode 1 with movie details & backdrop.
     */
    suspend fun fetchEpisodesMapped(
        anilistId: Int,
        title: String,
        romajiTitle: String? = null,
        season: Int? = null,
        totalEpisodes: Int? = null,
        format: String? = null
    ): Map<Int, TmdbEpisodeStill> = withContext(Dispatchers.IO) {
        val cacheKey = "mapped_eps:$anilistId:${title.lowercase(Locale.US)}:$season"
        if (mappedEpisodesCache.containsKey(cacheKey)) {
            return@withContext mappedEpisodesCache[cacheKey] ?: emptyMap()
        }

        try {
            // 1. First check AniBridge mapping for authoritative TMDB ID and Season Targets (with retry)
            var aniBridge = if (anilistId > 0) AniBridgeResolver.resolve(anilistId) else null
            if (anilistId > 0 && (aniBridge == null || aniBridge.tmdbId == null || aniBridge.tmdbId <= 0)) {
                aniBridge = AniBridgeResolver.resolve(anilistId)
            }

            val resolved = if (aniBridge?.tmdbId != null && aniBridge.tmdbId > 0) {
                val type = aniBridge.tmdbType ?: if (format.equals("MOVIE", ignoreCase = true)) "movie" else "tv"
                TmdbSearchResult(aniBridge.tmdbId, type)
            } else {
                resolveTmdbId(anilistId, title, format = format)
            } ?: run {
                mappedEpisodesCache[cacheKey] = emptyMap()
                return@withContext emptyMap()
            }

            // 2. Movie handling
            if (resolved.type == "movie" || format.equals("MOVIE", ignoreCase = true)) {
                val details = getDetails(anilistId, title, format = format)
                val stillUrl = details?.backdropUrl
                val overview = details?.overview
                val movieTitle = details?.title ?: title
                val movieMap = mapOf(
                    1 to TmdbEpisodeStill(
                        episodeNumber = 1,
                        name = movieTitle,
                        stillUrl = stillUrl,
                        overview = overview
                    )
                )
                mappedEpisodesCache[cacheKey] = movieMap
                return@withContext movieMap
            }

            val resultMap = ConcurrentHashMap<Int, TmdbEpisodeStill>()

            // 3. AniBridge Target-Driven Mapping (Direct Multi-Season & Range Mapping)
            val showTargets = aniBridge?.tmdbShowTargets.orEmpty()
            if (showTargets.isNotEmpty()) {
                val seasonsToFetch = showTargets.map { it.season }.distinct()
                val seasonResults = coroutineScope {
                    seasonsToFetch.map { sNum ->
                        async(Dispatchers.IO) {
                            try {
                                sNum to fetchSingleSeasonEpisodesRaw(resolved.id, sNum)
                            } catch (_: Exception) {
                                sNum to emptyList()
                            }
                        }
                    }.awaitAll().toMap()
                }

            for (target in showTargets) {
                val epList = seasonResults[target.season].orEmpty()
                if (epList.isEmpty()) continue

                if (target.ranges.isNotEmpty()) {
                    for (range in target.ranges) {
                        for ((idx, ep) in epList.withIndex()) {
                            val seasonEpIndex = idx + 1
                            val rawEpNum = ep.episodeNumber

                            // Case A: TMDB returned continuous numbering (e.g. 62..77 for One Piece)
                            if (rawEpNum >= range.sourceStart && rawEpNum <= range.sourceEnd) {
                                val mappedStill = TmdbEpisodeStill(
                                    episodeNumber = rawEpNum,
                                    name = ep.name,
                                    stillUrl = ep.stillUrl,
                                    overview = ep.overview
                                )
                                resultMap[rawEpNum] = mappedStill
                            }
                            // Case B: TMDB returned 1-based numbering relative to season (e.g. 1..16)
                            if (seasonEpIndex >= 1) {
                                val offset = seasonEpIndex - 1
                                val anilistEp = range.sourceStart + offset
                                if (anilistEp <= range.sourceEnd) {
                                    val mappedStill = TmdbEpisodeStill(
                                        episodeNumber = anilistEp,
                                        name = ep.name,
                                        stillUrl = ep.stillUrl,
                                        overview = ep.overview
                                    )
                                    resultMap[anilistEp] = mappedStill
                                }
                            }
                            // Case C: Explicit target range offset
                            if (rawEpNum >= range.targetStart && rawEpNum <= range.targetEnd) {
                                val offset = rawEpNum - range.targetStart
                                val anilistEp = range.sourceStart + offset
                                if (anilistEp <= range.sourceEnd) {
                                    val mappedStill = TmdbEpisodeStill(
                                        episodeNumber = anilistEp,
                                        name = ep.name,
                                        stillUrl = ep.stillUrl,
                                        overview = ep.overview
                                    )
                                    resultMap[anilistEp] = mappedStill
                                }
                            }
                        }
                    }
                } else {
                    for (ep in epList) {
                        resultMap[ep.episodeNumber] = ep
                    }
                }
            }

            if (resultMap.isNotEmpty()) {
                val finalMap = resultMap.toMap()
                mappedEpisodesCache[cacheKey] = finalMap
                return@withContext finalMap
            }
        }

        // 4. Fallback: Dynamic TMDB Season Discovery & Cumulative Aggregation
        val showSeasons = fetchShowSeasons(resolved.id)
        if (showSeasons.isEmpty()) {
            val s1 = fetchSingleSeasonEpisodesRaw(resolved.id, season ?: 1)
            val fallbackMap = s1.associateBy { it.episodeNumber }
            mappedEpisodesCache[cacheKey] = fallbackMap
            return@withContext fallbackMap
        }

        val targetSeason = season ?: 1
        val isExplicitSequelSeason = targetSeason > 1 && showSeasons.any { it.seasonNumber == targetSeason }

        if (isExplicitSequelSeason) {
            var previousOffset = 0
            for (s in showSeasons) {
                if (s.seasonNumber < targetSeason) {
                    previousOffset += s.episodeCount
                }
            }
            val targetEps = fetchSingleSeasonEpisodesRaw(resolved.id, targetSeason)
            for ((idx, ep) in targetEps.withIndex()) {
                val seasonEp = idx + 1
                val relStill = TmdbEpisodeStill(
                    episodeNumber = seasonEp,
                    name = ep.name,
                    stillUrl = ep.stillUrl,
                    overview = ep.overview
                )
                resultMap[seasonEp] = relStill

                val absNum = if (ep.episodeNumber > seasonEp) ep.episodeNumber else (previousOffset + seasonEp)
                val absStill = TmdbEpisodeStill(
                    episodeNumber = absNum,
                    name = ep.name,
                    stillUrl = ep.stillUrl,
                    overview = ep.overview
                )
                resultMap[absNum] = absStill
            }
        } else {
            val seasonResults = coroutineScope {
                showSeasons.map { sInfo ->
                    async(Dispatchers.IO) {
                        try {
                            sInfo.seasonNumber to fetchSingleSeasonEpisodesRaw(resolved.id, sInfo.seasonNumber)
                        } catch (_: Exception) {
                            sInfo.seasonNumber to emptyList()
                        }
                    }
                }.awaitAll().toMap()
            }

            var cumulativeOffset = 0
            for (sInfo in showSeasons) {
                val epList = seasonResults[sInfo.seasonNumber].orEmpty()
                for ((idx, ep) in epList.withIndex()) {
                    val seasonEp = idx + 1
                    val isTmdbAlreadyAbsolute = ep.episodeNumber > seasonEp
                    val absEp = if (isTmdbAlreadyAbsolute) ep.episodeNumber else (cumulativeOffset + seasonEp)

                    val mappedStill = TmdbEpisodeStill(
                        episodeNumber = absEp,
                        name = ep.name,
                        stillUrl = ep.stillUrl,
                        overview = ep.overview
                    )
                    resultMap[absEp] = mappedStill
                    if (sInfo.seasonNumber == 1 || !resultMap.containsKey(seasonEp)) {
                        resultMap[seasonEp] = TmdbEpisodeStill(
                            episodeNumber = seasonEp,
                            name = ep.name,
                            stillUrl = ep.stillUrl,
                            overview = ep.overview
                        )
                    }
                }
                cumulativeOffset += if (epList.isNotEmpty()) epList.size else sInfo.episodeCount
            }
        }

            val finalMap = resultMap.toMap()
            mappedEpisodesCache[cacheKey] = finalMap
            return@withContext finalMap
        } catch (_: Exception) {
            mappedEpisodesCache[cacheKey] = emptyMap()
            return@withContext emptyMap()
        }
    }

    /**
     * Legacy backward-compatible alias.
     */
    suspend fun fetchSeasonEpisodes(
        anilistId: Int,
        title: String,
        season: Int
    ): Map<Int, TmdbEpisodeStill> = fetchEpisodesMapped(
        anilistId = anilistId,
        title = title,
        season = season
    )
}
