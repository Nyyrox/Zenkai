package com.nyyrox.aniverse.data.movie

import com.nyyrox.aniverse.data.tmdb.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object MovieRepository {

    private const val STREAM_WORKER_BASE = "https://zenkaii.pages.dev"
    private const val BARRIER_TOKEN = "zenkai-barrier-apk-2026-v1"

    val MOVIE_GENRES = listOf(
        MovieGenre(28, "Action"),
        MovieGenre(12, "Adventure"),
        MovieGenre(16, "Animation"),
        MovieGenre(35, "Comedy"),
        MovieGenre(80, "Crime"),
        MovieGenre(99, "Documentary"),
        MovieGenre(18, "Drama"),
        MovieGenre(10751, "Family"),
        MovieGenre(14, "Fantasy"),
        MovieGenre(36, "History"),
        MovieGenre(27, "Horror"),
        MovieGenre(10402, "Music"),
        MovieGenre(9648, "Mystery"),
        MovieGenre(10749, "Romance"),
        MovieGenre(878, "Sci-Fi"),
        MovieGenre(53, "Thriller"),
        MovieGenre(10752, "War"),
        MovieGenre(37, "Western")
    )

    val allGenresMap: Map<Int, String> by lazy { (MOVIE_GENRES + TV_GENRES).associate { it.id to it.name } }

    val TV_GENRES = listOf(
        MovieGenre(10759, "Action & Adventure"),
        MovieGenre(16, "Animation"),
        MovieGenre(35, "Comedy"),
        MovieGenre(80, "Crime"),
        MovieGenre(99, "Documentary"),
        MovieGenre(18, "Drama"),
        MovieGenre(10751, "Family"),
        MovieGenre(10762, "Kids"),
        MovieGenre(9648, "Mystery"),
        MovieGenre(10764, "Reality"),
        MovieGenre(10765, "Sci-Fi & Fantasy"),
        MovieGenre(10768, "War & Politics"),
        MovieGenre(37, "Western")
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, Any>()

    private fun parseMovieItem(json: JSONObject, fallbackType: String = "movie"): MovieItem? {
        val id = json.optInt("id", 0)
        if (id <= 0) return null

        val mediaType = json.optString("media_type").ifBlank { fallbackType }
        if (mediaType != "movie" && mediaType != "tv") return null

        val title = json.optString("title").ifBlank { json.optString("name") }
        if (title.isBlank()) return null

        val poster = json.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
        val backdrop = json.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" }
        val date = json.optString("release_date").ifBlank { json.optString("first_air_date") }
        val year = if (date.length >= 4) date.substring(0, 4) else null
        val vote = json.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
        val rating = vote?.let { Math.round(it * 10.0) / 10.0 }
        val overview = json.optString("overview")

        val genreIds = mutableListOf<Int>()
        val arr = json.optJSONArray("genre_ids")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                genreIds.add(arr.getInt(i))
            }
        }

        return MovieItem(
            id = id,
            type = mediaType,
            title = title,
            poster = poster,
            backdrop = backdrop,
            year = year,
            rating = rating,
            overview = overview,
            genreIds = genreIds
        )
    }

    private fun parseList(jsonStr: String?, fallbackType: String = "movie"): List<MovieItem> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<MovieItem>()
        try {
            val root = JSONObject(jsonStr)
            val results = root.optJSONArray("results") ?: return emptyList()
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val item = parseMovieItem(obj, fallbackType)
                if (item != null && item.poster != null) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {}
        return list
    }

    suspend fun getTrending(type: String = "all", page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val cacheKey = "trending:$type:$page"
        @Suppress("UNCHECKED_CAST")
        (cache[cacheKey] as? List<MovieItem>)?.let { return@withContext it }

        val res = TmdbRepository.executeTmdbGet("/trending/$type/week", "page=$page")
        val items = parseList(res, if (type == "tv") "tv" else "movie")
        if (items.isNotEmpty()) cache[cacheKey] = items
        items
    }

    suspend fun getPopular(type: String = "movie", page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val cacheKey = "popular:$type:$page"
        @Suppress("UNCHECKED_CAST")
        (cache[cacheKey] as? List<MovieItem>)?.let { return@withContext it }

        val res = TmdbRepository.executeTmdbGet("/$type/popular", "page=$page")
        val items = parseList(res, type)
        if (items.isNotEmpty()) cache[cacheKey] = items
        items
    }

    suspend fun getTopRated(type: String = "movie", page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val cacheKey = "top_rated:$type:$page"
        @Suppress("UNCHECKED_CAST")
        (cache[cacheKey] as? List<MovieItem>)?.let { return@withContext it }

        val res = TmdbRepository.executeTmdbGet("/$type/top_rated", "page=$page")
        val items = parseList(res, type)
        if (items.isNotEmpty()) cache[cacheKey] = items
        items
    }

    suspend fun getNowPlaying(type: String = "movie", page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        val cacheKey = "now_playing:$type:$page"
        @Suppress("UNCHECKED_CAST")
        (cache[cacheKey] as? List<MovieItem>)?.let { return@withContext it }

        val endpoint = if (type == "tv") "/tv/on_the_air" else "/movie/now_playing"
        val res = TmdbRepository.executeTmdbGet(endpoint, "page=$page")
        val items = parseList(res, type)
        if (items.isNotEmpty()) cache[cacheKey] = items
        items
    }

    suspend fun search(query: String, type: String = "multi", page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val res = TmdbRepository.executeTmdbGet("/search/$type", "query=$encoded&page=$page&include_adult=false")
        parseList(res, if (type == "tv") "tv" else "movie")
    }

    suspend fun discover(
        type: String = "movie",
        genreId: Int? = null,
        year: String? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1
    ): List<MovieItem> = withContext(Dispatchers.IO) {
        val qb = StringBuilder("sort_by=$sortBy&page=$page&include_adult=false")
        if (genreId != null) qb.append("&with_genres=$genreId")
        if (!year.isNullOrBlank()) {
            val yrParam = if (type == "tv") "first_air_date_year" else "primary_release_year"
            qb.append("&$yrParam=$year")
        }
        if (sortBy.startsWith("vote_average")) {
            qb.append("&vote_count.gte=300")
        }
        val res = TmdbRepository.executeTmdbGet("/discover/$type", qb.toString())
        parseList(res, type)
    }

    suspend fun getDetails(id: Int, type: String): MovieDetails? = withContext(Dispatchers.IO) {
        val cacheKey = "details:$type:$id"
        (cache[cacheKey] as? MovieDetails)?.let { return@withContext it }

        val query = "append_to_response=credits,videos,similar,external_ids,images&include_image_language=en,null"
        val res = TmdbRepository.executeTmdbGet("/$type/$id", query) ?: return@withContext null

        try {
            val root = JSONObject(res)
            val title = root.optString("title").ifBlank { root.optString("name") }
            val poster = root.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
            val backdrop = root.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" }
            val date = root.optString("release_date").ifBlank { root.optString("first_air_date") }
            val year = if (date.length >= 4) date.substring(0, 4) else null
            val vote = root.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
            val rating = vote?.let { Math.round(it * 10.0) / 10.0 }
            val overview = root.optString("overview")
            val tagline = root.optString("tagline").takeIf { it.isNotBlank() }
            val status = root.optString("status").takeIf { it.isNotBlank() }

            val runtime = root.optInt("runtime", 0).takeIf { it > 0 }
                ?: root.optJSONArray("episode_run_time")?.let { if (it.length() > 0) it.getInt(0) else null }

            // Genres
            val genres = mutableListOf<MovieGenre>()
            val gArr = root.optJSONArray("genres")
            if (gArr != null) {
                for (i in 0 until gArr.length()) {
                    val gObj = gArr.optJSONObject(i) ?: continue
                    genres.add(MovieGenre(gObj.getInt("id"), gObj.getString("name")))
                }
            }

            // Logo
            var logoPath: String? = null
            val imagesObj = root.optJSONObject("images")
            val logosArr = imagesObj?.optJSONArray("logos")
            if (logosArr != null && logosArr.length() > 0) {
                for (i in 0 until logosArr.length()) {
                    val lObj = logosArr.optJSONObject(i) ?: continue
                    val fp = lObj.optString("file_path")
                    if (fp.isNotBlank() && !fp.endsWith(".svg", ignoreCase = true)) {
                        logoPath = fp
                        break
                    }
                }
            }

            // Trailer
            var trailerKey: String? = null
            val vidsObj = root.optJSONObject("videos")
            val vidsArr = vidsObj?.optJSONArray("results")
            if (vidsArr != null) {
                for (i in 0 until vidsArr.length()) {
                    val vObj = vidsArr.optJSONObject(i) ?: continue
                    if (vObj.optString("site").equals("YouTube", ignoreCase = true) &&
                        vObj.optString("type").equals("Trailer", ignoreCase = true)
                    ) {
                        trailerKey = vObj.optString("key")
                        break
                    }
                }
            }

            // Seasons (for TV)
            val seasons = mutableListOf<MovieSeason>()
            val sArr = root.optJSONArray("seasons")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val sObj = sArr.optJSONObject(i) ?: continue
                    val sNum = sObj.optInt("season_number", 0)
                    val epCount = sObj.optInt("episode_count", 0)
                    if (sNum > 0 && epCount > 0) {
                        seasons.add(MovieSeason(sNum, sObj.optString("name", "Season $sNum"), epCount))
                    }
                }
            }

            // Cast
            val cast = mutableListOf<MovieCast>()
            val creditsObj = root.optJSONObject("credits")
            val castArr = creditsObj?.optJSONArray("cast")
            if (castArr != null) {
                for (i in 0 until minOf(castArr.length(), 18)) {
                    val cObj = castArr.optJSONObject(i) ?: continue
                    val cId = cObj.optInt("id", 0)
                    val cName = cObj.optString("name")
                    val cChar = cObj.optString("character")
                    val cPhoto = cObj.optString("profile_path").takeIf { it.isNotBlank() && it != "null" }
                    if (cId > 0 && cName.isNotBlank()) {
                        cast.add(MovieCast(cId, cName, cChar, cPhoto))
                    }
                }
            }

            // Similar
            val similar = mutableListOf<MovieItem>()
            val simObj = root.optJSONObject("similar")
            val simArr = simObj?.optJSONArray("results")
            if (simArr != null) {
                for (i in 0 until minOf(simArr.length(), 15)) {
                    val sm = simArr.optJSONObject(i) ?: continue
                    val item = parseMovieItem(sm, type)
                    if (item != null && item.poster != null) similar.add(item)
                }
            }

            // External IDs (IMDb)
            val extObj = root.optJSONObject("external_ids")
            val imdbId = extObj?.optString("imdb_id")?.takeIf { it.isNotBlank() && it != "null" }
                ?: root.optString("imdb_id").takeIf { it.isNotBlank() && it != "null" }

            // Collection / Franchise (for movies)
            val collectionParts = mutableListOf<MovieItem>()
            val collObj = root.optJSONObject("belongs_to_collection")
            val collId = collObj?.optInt("id", 0) ?: 0
            if (collId > 0) {
                try {
                    val collRes = TmdbRepository.executeTmdbGet("/collection/$collId", "language=en-US")
                    if (!collRes.isNullOrBlank()) {
                        val cJson = JSONObject(collRes)
                        val pArr = cJson.optJSONArray("parts")
                        if (pArr != null) {
                            for (pIdx in 0 until pArr.length()) {
                                val pObj = pArr.optJSONObject(pIdx) ?: continue
                                val pItem = parseMovieItem(pObj, "movie")
                                if (pItem != null && pItem.poster != null) collectionParts.add(pItem)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val details = MovieDetails(
                id = id,
                type = type,
                title = title,
                poster = poster,
                backdrop = backdrop,
                year = year,
                rating = rating,
                overview = overview,
                tagline = tagline,
                runtime = runtime,
                status = status,
                genres = genres,
                logo = logoPath,
                trailerKey = trailerKey,
                seasons = seasons,
                cast = cast,
                similar = similar,
                collection = collectionParts,
                imdbId = imdbId
            )
            cache[cacheKey] = details
            details
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getEpisodes(id: Int, season: Int): List<MovieEpisode> = withContext(Dispatchers.IO) {
        val cacheKey = "episodes:$id:$season"
        @Suppress("UNCHECKED_CAST")
        (cache[cacheKey] as? List<MovieEpisode>)?.let { return@withContext it }

        val res = TmdbRepository.executeTmdbGet("/tv/$id/season/$season") ?: return@withContext emptyList()
        val eps = mutableListOf<MovieEpisode>()
        try {
            val root = JSONObject(res)
            val arr = root.optJSONArray("episodes") ?: return@withContext emptyList()
            for (i in 0 until arr.length()) {
                val eObj = arr.optJSONObject(i) ?: continue
                val epNum = eObj.optInt("episode_number", i + 1)
                val epName = eObj.optString("name", "Episode $epNum")
                val epOverview = eObj.optString("overview")
                val still = eObj.optString("still_path").takeIf { it.isNotBlank() && it != "null" }
                val runtime = eObj.optInt("runtime", 0).takeIf { it > 0 }
                val airDate = eObj.optString("air_date").takeIf { it.isNotBlank() }
                eps.add(MovieEpisode(epNum, epName, epOverview, still, runtime, airDate))
            }
            if (eps.isNotEmpty()) cache[cacheKey] = eps
        } catch (_: Exception) {}
        eps
    }

    fun getMovieServers(id: Int, type: String, season: Int = 1, episode: Int = 1): List<MovieServerOption> {
        val isTv = type.equals("tv", ignoreCase = true)
        return listOf(
            MovieServerOption(
                id = "moviebox",
                label = "MovieBox",
                tags = listOf("Direct", "Multi audio"),
                isDirect = true
            ),
            MovieServerOption(
                id = "cinemaos",
                label = "Scarlet Witch",
                tags = listOf("Multi audio"),
                isDirect = false,
                embedUrl = if (isTv) "https://cinemaos.tech/player/$id/$season/$episode?lang=en" else "https://cinemaos.tech/player/$id?lang=en"
            ),
            MovieServerOption(
                id = "vidzee",
                label = "Captain America",
                tags = listOf("Multi audio"),
                isDirect = false,
                embedUrl = if (isTv) "https://player.vidzee.wtf/embed/tv/$id/$season/$episode?lang=en" else "https://player.vidzee.wtf/embed/movie/$id?lang=en"
            ),
            MovieServerOption(
                id = "videasy",
                label = "Iron Man (4K)",
                tags = listOf("Multi audio", "4K"),
                isDirect = false,
                embedUrl = if (isTv) "https://player.videasy.to/tv/$id/$season/$episode?color=E50914&overlay=true&nextEpisode=true&autoplayNextEpisode=true&episodeSelector=true" else "https://player.videasy.to/movie/$id?color=E50914&overlay=true&nextEpisode=true"
            ),
            MovieServerOption(
                id = "vidup",
                label = "Thor",
                tags = listOf("1080p"),
                isDirect = false,
                embedUrl = if (isTv) "https://vidup.to/tv/$id/$season/$episode?server=zenith" else "https://vidup.to/movie/$id?server=zenith"
            ),
            MovieServerOption(
                id = "vidcore",
                label = "Hulk",
                tags = listOf("HD"),
                isDirect = false,
                embedUrl = if (isTv) "https://vidcore.net/tv/$id/$season/$episode?server=Crystal&autoPlay=true" else "https://vidcore.net/movie/$id?server=Crystal&autoPlay=true"
            ),
            MovieServerOption(
                id = "vidsrc-ru",
                label = "Black Widow",
                tags = listOf("Multi"),
                isDirect = false,
                embedUrl = if (isTv) "https://vidsrcme.ru/embed/tv/$id/$season/$episode" else "https://vidsrcme.ru/embed/movie/$id"
            ),
            MovieServerOption(
                id = "vidlove",
                label = "Hawkeye",
                tags = listOf("Fast"),
                isDirect = false,
                embedUrl = if (isTv) "https://player.vidlove.cc/embed/tv/$id/$season/$episode" else "https://player.vidlove.cc/embed/movie/$id"
            ),
            MovieServerOption(
                id = "1embed",
                label = "Spider-Man",
                tags = listOf("HD"),
                isDirect = false,
                embedUrl = if (isTv) "https://1embed.cc/embed/tv/$id/$season/$episode" else "https://1embed.cc/embed/movie/$id"
            ),
            MovieServerOption(
                id = "cinesrc",
                label = "Doctor Strange",
                tags = listOf("HD"),
                isDirect = false,
                embedUrl = if (isTv) "https://cinesrc.st/embed/tv/$id/$season/$episode" else "https://cinesrc.st/embed/movie/$id"
            ),
            MovieServerOption(
                id = "primesrc",
                label = "Black Panther",
                tags = listOf("HD"),
                isDirect = false,
                embedUrl = if (isTv) "https://primesrc.me/embed/tv?tmdb=$id&season=$season&episode=$episode&fallback=false&serverOrder=PrimeVid,Voe,Dood" else "https://primesrc.me/embed/movie?tmdb=$id"
            )
        )
    }

    /**
     * Resolves MovieBox direct stream with multi-audio dubs, qualities, and subtitles.
     * Uses native on-device MovieBoxResolver first for instant playback without external proxies.
     */
    suspend fun fetchMovieStream(
        id: Int,
        type: String, // "movie" or "tv"
        season: Int = 1,
        episode: Int = 1,
        title: String? = null,
        year: String? = null,
        dubId: String? = null,
        imdbId: String? = null
    ): MovieStreamResult = withContext(Dispatchers.IO) {
        // 1. Native on-device MovieBox resolver
        try {
            val nativeRes = MovieBoxResolver.resolveStream(
                tmdbId = id,
                type = type,
                season = season,
                episode = episode,
                title = title,
                year = year,
                dubId = dubId
            )
            if (nativeRes.success && nativeRes.streamUrl.isNotBlank()) {
                android.util.Log.d("MovieRepository", "Native MovieBox stream resolved successfully: ${nativeRes.quality}")
                return@withContext nativeRes
            }
        } catch (e: Exception) {
            android.util.Log.w("MovieRepository", "Native MovieBox resolve exception: ${e.message}")
        }

        // 2. Fallback to stream proxy worker if needed
        val qb = StringBuilder("id=$id&type=$type&s=$season&e=$episode")
        if (!title.isNullOrBlank()) qb.append("&title=").append(URLEncoder.encode(title, "UTF-8"))
        if (!year.isNullOrBlank()) qb.append("&year=").append(URLEncoder.encode(year, "UTF-8"))
        if (!dubId.isNullOrBlank()) qb.append("&dub=").append(URLEncoder.encode(dubId, "UTF-8"))
        if (!imdbId.isNullOrBlank()) qb.append("&imdb=").append(URLEncoder.encode(imdbId, "UTF-8"))

        val url = "$STREAM_WORKER_BASE/api/movies/stream?$qb"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Zenkai/1.0.5")
                .addHeader("X-Zenkai-Barrier", BARRIER_TOKEN)
                .build()

            val quickClient = httpClient.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()

            quickClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body.string()
                if (!resp.isSuccessful) {
                    return@withContext MovieStreamResult(
                        success = false,
                        server = "moviebox",
                        title = title ?: "",
                        streamUrl = "",
                        quality = "Auto",
                        error = "MovieBox stream not available for this title. Try Iron Man, Captain America, or another server."
                    )
                }

                val json = JSONObject(bodyStr)
                if (!json.optBoolean("success", false)) {
                    return@withContext MovieStreamResult(
                        success = false,
                        server = json.optString("server", "moviebox"),
                        title = title ?: "",
                        streamUrl = "",
                        quality = "Auto",
                        error = json.optString("error", "Stream not available")
                    )
                }

                fun fixUrl(u: String?): String {
                    if (u.isNullOrBlank()) return ""
                    return if (u.startsWith("http://") || u.startsWith("https://")) u else "$STREAM_WORKER_BASE$u"
                }

                val server = json.optString("server", "moviebox")
                val streamTitle = json.optString("title", title ?: "")
                val streamUrl = fixUrl(json.optString("streamUrl"))
                val downloadUrl = fixUrl(json.optString("downloadUrl")).takeIf { it.isNotBlank() }
                val quality = json.optString("quality", "1080p")
                val format = json.optString("format", "mp4")
                val duration = json.optDouble("duration", 0.0).takeIf { it > 0.0 }

                // Dubs
                val dubs = mutableListOf<MovieDub>()
                val dubsArr = json.optJSONArray("dubs")
                if (dubsArr != null) {
                    for (i in 0 until dubsArr.length()) {
                        val dObj = dubsArr.optJSONObject(i) ?: continue
                        val dId = dObj.optString("id")
                        val dLabel = dObj.optString("label")
                        val dSel = dObj.optBoolean("selected", false)
                        if (dId.isNotBlank() && dLabel.isNotBlank()) {
                            dubs.add(MovieDub(dId, dLabel, dSel))
                        }
                    }
                }

                // Streams
                val streams = mutableListOf<MovieStream>()
                val streamsArr = json.optJSONArray("streams")
                if (streamsArr != null) {
                    for (i in 0 until streamsArr.length()) {
                        val sObj = streamsArr.optJSONObject(i) ?: continue
                        val sQual = sObj.optString("quality", "1080p")
                        val sRes = sObj.optInt("resolutionNum", 0).takeIf { it > 0 }
                        val sFmt = sObj.optString("format", "mp4")
                        val sSize = sObj.optLong("size", 0L).takeIf { it > 0L }
                        val sSizeLabel = sObj.optString("sizeLabel").takeIf { it.isNotBlank() }
                        val sDur = sObj.optDouble("duration", 0.0).takeIf { it > 0.0 }
                        val sUrl = fixUrl(sObj.optString("streamUrl"))
                        val sDlUrl = fixUrl(sObj.optString("downloadUrl"))
                        val sDlName = sObj.optString("downloadFileName").takeIf { it.isNotBlank() }

                        if (sUrl.isNotBlank()) {
                            streams.add(
                                MovieStream(
                                    quality = sQual,
                                    resolutionNum = sRes,
                                    format = sFmt,
                                    size = sSize,
                                    sizeLabel = sSizeLabel,
                                    duration = sDur,
                                    streamUrl = sUrl,
                                    downloadUrl = sDlUrl,
                                    downloadFileName = sDlName
                                )
                            )
                        }
                    }
                }

                // Subtitles
                val subtitles = mutableListOf<MovieSubtitle>()
                val subsArr = json.optJSONArray("subtitles")
                if (subsArr != null) {
                    for (i in 0 until subsArr.length()) {
                        val subObj = subsArr.optJSONObject(i) ?: continue
                        val subId = subObj.optString("id", "sub-$i")
                        val subLabel = subObj.optString("label", "English")
                        val subLang = subObj.optString("language").ifBlank { subObj.optString("lang", "en") }
                        val subUrl = fixUrl(subObj.optString("url"))
                        if (subUrl.isNotBlank()) {
                            subtitles.add(MovieSubtitle(subId, subLabel, subLang, subUrl))
                        }
                    }
                }

                MovieStreamResult(
                    success = true,
                    server = server,
                    title = streamTitle,
                    streamUrl = streamUrl,
                    downloadUrl = downloadUrl,
                    quality = quality,
                    format = format,
                    duration = duration,
                    dubs = dubs,
                    streams = streams,
                    subtitles = subtitles
                )
            }
        } catch (e: Exception) {
            MovieStreamResult(
                success = false,
                server = "moviebox",
                title = title ?: "",
                streamUrl = "",
                quality = "Auto",
                error = "MovieBox stream not available for this title. Try Iron Man, Captain America, or another server."
            )
        }
    }
}
