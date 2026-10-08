package com.nyyrox.aniverse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AniListRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val anibridgeCache = ConcurrentHashMap<Int, Int>() // tmdbId → anilistId (-1 = none)

    companion object {
        private const val API_URL = "https://graphql.anilist.co"
        private const val TMDB_VERCEL = "https://aniversee.vercel.app/api/tmdb"
        private const val ANIMAP_BASE = "https://animap.id/api/map/tmdb"
        private const val TMDB_IMG = "https://image.tmdb.org/t/p"

        private val defaultInstance by lazy { AniListRepository() }

        suspend fun fetchMangaDetails(title: String? = null, anilistId: Int? = null): JSONObject? =
            defaultInstance.fetchMangaDetails(title, anilistId)
    }

    private suspend fun post(query: String, variables: JSONObject = JSONObject()): JSONObject =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("query", query)
                .put("variables", variables)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .addHeader("Origin", "https://anilist.co")
                .addHeader("Referer", "https://anilist.co/")
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body.string()
                if (!response.isSuccessful) {
                    throw IllegalStateException("AniList request failed (${response.code}).")
                }
                val json = JSONObject(raw)
                val errors = json.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val msg = errors.optJSONObject(0)?.optString("message").orEmpty()
                    throw IllegalStateException(msg.ifBlank { "AniList returned an error." })
                }
                json
            }
        }

    private fun optCleanString(obj: JSONObject?, key: String): String? {
        if (obj == null || obj.isNull(key)) return null
        val str = obj.optString(key).trim()
        return if (str.isBlank() || str.equals("null", ignoreCase = true)) null else str
    }

    private fun parseMedia(o: JSONObject): Anime {
        val titleObj = o.optJSONObject("title")
        val englishTitle = optCleanString(titleObj, "english")
        val romajiTitle = optCleanString(titleObj, "romaji")
        val userPreferred = optCleanString(titleObj, "userPreferred")
        val nativeTitle = optCleanString(titleObj, "native")
        val title = englishTitle ?: romajiTitle ?: userPreferred ?: nativeTitle ?: "Unknown Anime"

        val coverObj = o.optJSONObject("coverImage")
        val cover = optCleanString(coverObj, "extraLarge")
            ?: optCleanString(coverObj, "large")
            ?: ""

        val banner = optCleanString(o, "bannerImage")
        val next = o.optJSONObject("nextAiringEpisode")

        val genres = buildList {
            o.optJSONArray("genres")?.let { a ->
                for (i in 0 until a.length()) {
                    val g = a.optString(i).trim()
                    if (g.isNotBlank() && !g.equals("null", ignoreCase = true)) add(g)
                }
            }
        }

        val studios = buildList {
            o.optJSONObject("studios")?.optJSONArray("nodes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val name = optCleanString(arr.optJSONObject(i), "name")
                    if (name != null) add(name)
                }
            }
        }

        val relations = buildList {
            val relObj = o.optJSONObject("relations")
            val edges = relObj?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val relType = optCleanString(edge, "relationType") ?: "SIDE_STORY"
                    val node = edge.optJSONObject("node") ?: continue
                    val relId = node.optInt("id")
                    val nodeType = optCleanString(node, "type") ?: "ANIME"
                    val edgeTitleObj = node.optJSONObject("title")
                    val english = optCleanString(edgeTitleObj, "english")
                    val romaji = optCleanString(edgeTitleObj, "romaji")
                    val edgeUserPreferred = optCleanString(edgeTitleObj, "userPreferred")
                    val edgeNative = optCleanString(edgeTitleObj, "native")
                    val relTitle = english ?: romaji ?: edgeUserPreferred ?: edgeNative ?: "Unknown Anime"
                    val edgeCoverObj = node.optJSONObject("coverImage")
                    val relCover = optCleanString(edgeCoverObj, "extraLarge")
                        ?: optCleanString(edgeCoverObj, "large")
                        ?: ""
                    val relFormat = optCleanString(node, "format")
                    val relEpisodes = if (node.isNull("episodes")) null else node.optInt("episodes")
                    val relStatus = optCleanString(node, "status")
                    val relScore = if (node.isNull("averageScore")) null else node.optInt("averageScore")
                    if (relId > 0 && relTitle.isNotBlank()) {
                        add(
                            MediaRelation(
                                id = relId,
                                type = nodeType,
                                relationType = relType,
                                title = relTitle,
                                romajiTitle = romaji ?: edgeUserPreferred,
                                cover = relCover,
                                format = relFormat,
                                episodes = relEpisodes,
                                status = relStatus,
                                score = relScore
                            )
                        )
                    }
                }
            }
        }

        val characters = buildList {
            val charObj = o.optJSONObject("characters")
            val edges = charObj?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val role = optCleanString(edge, "role") ?: "SUPPORTING"
                    val node = edge.optJSONObject("node") ?: continue
                    val charId = node.optInt("id")
                    val nameObj = node.optJSONObject("name")
                    val full = optCleanString(nameObj, "full") ?: optCleanString(nameObj, "userPreferred") ?: "Unknown"
                    val nativeName = optCleanString(nameObj, "native")
                    val imageObj = node.optJSONObject("image")
                    val image = optCleanString(imageObj, "large") ?: optCleanString(imageObj, "medium")

                    var vaName: String? = null
                    var vaNative: String? = null
                    var vaImage: String? = null
                    var vaLang: String? = null
                    val vaArr = edge.optJSONArray("voiceActors")
                    if (vaArr != null && vaArr.length() > 0) {
                        var selectedVa: JSONObject? = null
                        for (v in 0 until vaArr.length()) {
                            val va = vaArr.optJSONObject(v) ?: continue
                            val lang = optCleanString(va, "languageV2")
                            if (lang.equals("Japanese", ignoreCase = true)) {
                                selectedVa = va
                                break
                            }
                        }
                        if (selectedVa == null) selectedVa = vaArr.optJSONObject(0)
                        if (selectedVa != null) {
                            val vaNameObj = selectedVa.optJSONObject("name")
                            vaName = optCleanString(vaNameObj, "full") ?: optCleanString(vaNameObj, "userPreferred")
                            vaNative = optCleanString(vaNameObj, "native")
                            val vaImgObj = selectedVa.optJSONObject("image")
                            vaImage = optCleanString(vaImgObj, "large") ?: optCleanString(vaImgObj, "medium")
                            vaLang = optCleanString(selectedVa, "languageV2")
                        }
                    }

                    if (charId > 0) {
                        add(
                            AnimeCharacter(
                                id = charId,
                                name = full,
                                nativeName = nativeName,
                                image = image,
                                role = role,
                                voiceActorName = vaName,
                                voiceActorNative = vaNative,
                                voiceActorImage = vaImage,
                                voiceActorLanguage = vaLang
                            )
                        )
                    }
                }
            }
        }

        val staff = buildList {
            val staffObj = o.optJSONObject("staff")
            val edges = staffObj?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val role = optCleanString(edge, "role") ?: "Staff"
                    val node = edge.optJSONObject("node") ?: continue
                    val staffId = node.optInt("id")
                    val nameObj = node.optJSONObject("name")
                    val full = optCleanString(nameObj, "full") ?: optCleanString(nameObj, "userPreferred") ?: "Unknown"
                    val imgObj = node.optJSONObject("image")
                    val image = optCleanString(imgObj, "large") ?: optCleanString(imgObj, "medium")
                    if (staffId > 0) {
                        add(AnimeStaff(id = staffId, name = full, role = role, image = image))
                    }
                }
            }
        }

        val recommendations = buildList {
            val recObj = o.optJSONObject("recommendations")
            val nodes = recObj?.optJSONArray("nodes")
            if (nodes != null) {
                for (i in 0 until nodes.length()) {
                    val item = nodes.optJSONObject(i) ?: continue
                    val mediaRec = item.optJSONObject("mediaRecommendation") ?: continue
                    val recId = mediaRec.optInt("id")
                    val recTitleObj = mediaRec.optJSONObject("title")
                    val recTitle = optCleanString(recTitleObj, "english")
                        ?: optCleanString(recTitleObj, "romaji")
                        ?: optCleanString(recTitleObj, "userPreferred") ?: "Anime"
                    val recRomaji = optCleanString(recTitleObj, "romaji")
                    val recCoverObj = mediaRec.optJSONObject("coverImage")
                    val recCover = optCleanString(recCoverObj, "extraLarge")
                        ?: optCleanString(recCoverObj, "large") ?: ""
                    val recBanner = optCleanString(mediaRec, "bannerImage")
                    val recScore = if (mediaRec.isNull("averageScore")) null else mediaRec.optInt("averageScore")
                    val recFormat = optCleanString(mediaRec, "format")
                    val recEpisodes = if (mediaRec.isNull("episodes")) null else mediaRec.optInt("episodes")
                    if (recId > 0 && recCover.isNotBlank()) {
                        add(
                            AnimeRecommendation(
                                id = recId,
                                title = recTitle,
                                romajiTitle = recRomaji,
                                cover = recCover,
                                banner = recBanner,
                                score = recScore,
                                format = recFormat,
                                episodes = recEpisodes
                            )
                        )
                    }
                }
            }
        }

        return Anime(
            id = o.optInt("id"),
            idMal = if (o.isNull("idMal")) null else o.optInt("idMal"),
            title = title,
            romajiTitle = romajiTitle ?: userPreferred,
            cover = cover,
            banner = banner,
            episodes = if (o.isNull("episodes")) null else o.optInt("episodes"),
            status = optCleanString(o, "status") ?: "UNKNOWN",
            score = if (o.isNull("averageScore")) null else o.optInt("averageScore"),
            description = optCleanString(o, "description") ?: "",
            nextEpisode = next?.optInt("episode"),
            nextAt = next?.optLong("airingAt"),
            genres = genres,
            seasonYear = if (o.isNull("seasonYear")) null else o.optInt("seasonYear"),
            season = optCleanString(o, "season"),
            format = optCleanString(o, "format"),
            duration = if (o.isNull("duration")) null else o.optInt("duration"),
            studios = studios,
            source = optCleanString(o, "source"),
            trailerId = o.optJSONObject("trailer")?.takeIf { it.optString("site") == "youtube" }?.let { optCleanString(it, "id") },
            relations = relations,
            characters = characters,
            staff = staff,
            recommendations = recommendations
        )
    }

    suspend fun list(
        sort: String = "TRENDING_DESC",
        page: Int = 1,
        perPage: Int = 24,
        search: String = "",
        genre: String? = null,
        status: String? = null,
        seasonYear: Int? = null,
        format: String? = null
    ): List<Anime> {
        val query = """
            query(${'$'}page:Int!, ${'$'}perPage:Int!, ${'$'}search:String, ${'$'}genre:String, ${'$'}status:MediaStatus, ${'$'}seasonYear:Int, ${'$'}format:MediaFormat) {
              Page(page:${'$'}page, perPage:${'$'}perPage) {
                media(type:ANIME, sort:$sort, search:${'$'}search, genre:${'$'}genre, status:${'$'}status, seasonYear:${'$'}seasonYear, format:${'$'}format, isAdult:false) {
                  id
                  idMal
                  title { romaji english userPreferred native }
                  coverImage { extraLarge large }
                  bannerImage
                  episodes
                  status
                  averageScore
                  description
                  genres
                  seasonYear
                  season
                  format
                  duration
                  source
                  trailer { id site }
                  studios(isMain:true) { nodes { name } }
                  nextAiringEpisode { episode airingAt }
                }
              }
            }
        """.trimIndent()

        val vars = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
            if (search.isNotBlank()) {
                put("search", search.trim())
            }
            if (!genre.isNullOrBlank() && genre != "All") {
                put("genre", genre)
            }
            if (!status.isNullOrBlank() && status != "All") {
                put("status", status.uppercase(Locale.US).replace(" ", "_"))
            }
            if (seasonYear != null && seasonYear > 0) {
                put("seasonYear", seasonYear)
            }
            if (!format.isNullOrBlank() && format != "ALL") {
                put("format", format.uppercase(Locale.US))
            }
        }

        val data = post(query, vars).optJSONObject("data")
        val pageObj = data?.optJSONObject("Page")
        val array = pageObj?.optJSONArray("media") ?: return emptyList()

        return buildList {
            for (i in 0 until array.length()) {
                val mediaObj = array.optJSONObject(i) ?: continue
                try {
                    add(parseMedia(mediaObj))
                } catch (_: Throwable) {}
            }
        }
    }

    suspend fun detail(id: Int): Anime {
        return try {
            val query = """
                query(${'$'}id:Int!) {
                  Media(id:${'$'}id, type:ANIME) {
                    id
                    idMal
                    title { romaji english userPreferred native }
                    coverImage { extraLarge large }
                    bannerImage
                    episodes
                    status
                    averageScore
                    description
                    genres
                    seasonYear
                    season
                    format
                    duration
                    source
                    trailer { id site }
                    studios(isMain:true) { nodes { name } }
                    nextAiringEpisode { episode airingAt timeUntilAiring }
                    relations {
                      edges {
                        relationType
                        node {
                          id
                          type
                          title { romaji english userPreferred native }
                          coverImage { extraLarge large }
                          format
                          episodes
                          status
                          averageScore
                        }
                      }
                    }
                    characters(sort: [ROLE, RELEVANCE], perPage: 25) {
                      edges {
                        role
                        node {
                          id
                          name { full native userPreferred }
                          image { large medium }
                        }
                        voiceActors(language: JAPANESE, sort: [RELEVANCE]) {
                          id
                          name { full native userPreferred }
                          languageV2
                          image { large medium }
                        }
                      }
                    }
                    staff(perPage: 15, sort: [RELEVANCE]) {
                      edges {
                        role
                        node {
                          id
                          name { full native userPreferred }
                          image { large medium }
                        }
                      }
                    }
                    recommendations(sort: [RATING_DESC], perPage: 14) {
                      nodes {
                        mediaRecommendation {
                          id
                          title { romaji english userPreferred }
                          coverImage { extraLarge large }
                          bannerImage
                          averageScore
                          format
                          episodes
                          status
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val json = post(query, JSONObject().put("id", id))
            val mediaObj = json.optJSONObject("data")?.optJSONObject("Media")
                ?: throw IllegalStateException("Anime with ID $id not found on AniList.")
            parseMedia(mediaObj)
        } catch (e: Exception) {
            detailFromTmdb(id) ?: throw e
        }
    }

    suspend fun fetchMalId(anilistId: Int): Int? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (anilistId <= 0) return@withContext null
        val knownMap = mapOf(
            918 to 918,
            9969 to 9969,
            15417 to 15417,
            20996 to 28977,
            97889 to 34096,
            99712 to 35843,
            100784 to 36838,
            101925 to 37491
        )
        knownMap[anilistId]?.let { return@withContext it }
        try {
            val query = """
                query(${'$'}id:Int!) {
                  Media(id:${'$'}id, type:ANIME) {
                    idMal
                  }
                }
            """.trimIndent()
            val json = post(query, JSONObject().put("id", anilistId))
            val mediaObj = json.optJSONObject("data")?.optJSONObject("Media")
            if (mediaObj != null && !mediaObj.isNull("idMal")) {
                val malId = mediaObj.optInt("idMal")
                if (malId > 0) malId else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun airing(): List<Airing> {
        val start = (System.currentTimeMillis() / 1000) - 86400
        val query = """
            query {
              Page(page:1, perPage:50) {
                airingSchedules(airingAt_greater:$start, sort:TIME) {
                  episode
                  airingAt
                  media {
                    id
                    idMal
                    title { romaji english userPreferred native }
                    coverImage { extraLarge large }
                    bannerImage
                    episodes
                    status
                    averageScore
                    description
                    genres
                    seasonYear
                    format
                    duration
                    studios(isMain:true) { nodes { name } }
                    nextAiringEpisode { episode airingAt }
                  }
                }
              }
            }
        """.trimIndent()

        val data = post(query).optJSONObject("data")
        val pageObj = data?.optJSONObject("Page")
        val array = pageObj?.optJSONArray("airingSchedules") ?: return emptyList()

        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val mediaObj = item.optJSONObject("media") ?: continue
                try {
                    add(
                        Airing(
                            anime = parseMedia(mediaObj),
                            episode = item.optInt("episode"),
                            at = item.optLong("airingAt")
                        )
                    )
                } catch (_: Throwable) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TMDB FALLBACK — used when AniList GraphQL is unavailable
    //  Fetches Japanese animation from TMDB Discover, then reverse-maps
    //  each TMDB ID to a real AniList ID via AniBridge (animap.id).
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun listFromTmdb(
        sort: String = "TRENDING_DESC",
        page: Int = 1,
        perPage: Int = 15,
        search: String = "",
        genre: String? = null,
        year: Int? = null,
        format: String? = null
    ): List<Anime> = withContext(Dispatchers.IO) {
        val isMovie = format?.equals("MOVIE", ignoreCase = true) == true
        val mediaType = if (isMovie) "movie" else "tv"
        val tmdbSort = when (sort) {
            "SCORE_DESC"       -> "vote_average.desc&vote_count.gte=300"
            "POPULARITY_DESC"  -> "popularity.desc"
            else               -> "popularity.desc"
        }

        val yearParam = if (year != null && year > 0) {
            if (isMovie) "&primary_release_year=$year" else "&first_air_date_year=$year"
        } else ""

        val discoverUrl = if (search.isNotBlank()) {
            val encodedQuery = URLEncoder.encode(search.trim(), "UTF-8")
            "$TMDB_VERCEL/search/$mediaType?query=$encodedQuery&include_adult=false&page=$page$yearParam"
        } else {
            val genreParam = when (genre?.lowercase(Locale.US)) {
                "action" -> "&with_genres=16,10759"
                "comedy" -> "&with_genres=16,35"
                "drama" -> "&with_genres=16,18"
                "fantasy" -> "&with_genres=16,10765"
                "romance" -> "&with_genres=16,10749"
                "mystery" -> "&with_genres=16,9648"
                "sci-fi" -> "&with_genres=16,878"
                "horror" -> "&with_genres=16,27"
                else -> "&with_genres=16"
            }
            "$TMDB_VERCEL/discover/$mediaType?with_original_language=ja$genreParam$yearParam&sort_by=$tmdbSort&page=$page&include_adult=false"
        }

        val rawItems: List<JSONObject> = try {
            val req = Request.Builder()
                .url(discoverUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val json = JSONObject(resp.body.string())
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<JSONObject>()
                for (i in 0 until minOf(results.length(), perPage)) {
                    val obj: JSONObject? = results.optJSONObject(i)
                    if (obj != null) list.add(obj)
                }
                list
            }
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        if (rawItems.isEmpty()) return@withContext emptyList<Anime>()

        // Parallel AniBridge reverse lookups – each in its own async inside coroutineScope
        val deferreds: List<kotlinx.coroutines.Deferred<Anime?>> = coroutineScope {
            rawItems.map { item: JSONObject ->
                async(Dispatchers.IO) {
                    val tmdbId: Int = item.optInt("id")
                    if (tmdbId <= 0) return@async null
                    val anilistId: Int = resolveAnilistIdFromTmdb(tmdbId) ?: tmdbId
                    buildAnimeFromTmdb(item, anilistId)
                }
            }
        }
        deferreds.mapNotNull { deferred: kotlinx.coroutines.Deferred<Anime?> -> deferred.await() }
    }

    suspend fun detailFromTmdb(anilistId: Int): Anime? = withContext(Dispatchers.IO) {
        try {
            val aniBridge = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(anilistId)
            val tmdbId = aniBridge?.tmdbId ?: anilistId
            val isMovie = aniBridge?.tmdbType?.equals("movie", ignoreCase = true) == true
            var endpoint = if (isMovie) "movie" else "tv"
            var req = Request.Builder()
                .url("$TMDB_VERCEL/$endpoint/$tmdbId")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .build()

            var resp = client.newCall(req).execute()
            if (!resp.isSuccessful && !isMovie) {
                resp.close()
                endpoint = "movie"
                req = Request.Builder()
                    .url("$TMDB_VERCEL/$endpoint/$tmdbId")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                    .build()
                resp = client.newCall(req).execute()
            }

            resp.use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body.string())
                val title = json.optString("name").ifBlank {
                    json.optString("title").ifBlank {
                        json.optString("original_name").ifBlank { json.optString("original_title") }
                    }
                }
                val posterPath = json.optString("poster_path").takeIf { it.isNotBlank() }
                val backdropPath = json.optString("backdrop_path").takeIf { it.isNotBlank() }
                val cover = posterPath?.let { "$TMDB_IMG/w500$it" } ?: ""
                val banner = backdropPath?.let { "$TMDB_IMG/w1280$it" }
                val epCount = json.optInt("number_of_episodes").takeIf { it > 0 } ?: 12
                val voteAvg = json.optDouble("vote_average", 0.0)
                val score = if (voteAvg > 0.0) (voteAvg * 10.0).toInt().coerceIn(1, 100) else null
                val dateStr = json.optString("first_air_date").ifBlank { json.optString("release_date") }
                val year = dateStr.take(4).toIntOrNull()
                val overview = json.optString("overview").ifBlank { "No description available." }
                val genresArr = json.optJSONArray("genres")
                val genres = buildList {
                    if (genresArr != null) {
                        for (i in 0 until genresArr.length()) {
                            genresArr.optJSONObject(i)?.optString("name")?.let { add(it) }
                        }
                    }
                }

                Anime(
                    id = anilistId,
                    title = title,
                    romajiTitle = json.optString("original_name").takeIf { it.isNotBlank() && it != title },
                    cover = cover,
                    banner = banner,
                    episodes = epCount,
                    status = json.optString("status", "RELEASING").uppercase(),
                    score = score,
                    description = overview,
                    genres = genres,
                    seasonYear = year,
                    format = if (isMovie) "MOVIE" else "TV"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveAnilistIdFromTmdb(tmdbId: Int): Int? {
        val cached = anibridgeCache[tmdbId]
        if (cached != null) return if (cached == -1) null else cached
        return try {
            val req = Request.Builder()
                .url("$ANIMAP_BASE/$tmdbId")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Aniverse/3.0")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    anibridgeCache[tmdbId] = -1
                    return null
                }
                val json = JSONObject(resp.body.string())
                val arr = json.optJSONArray("anilist_id")
                val id = if (arr != null && arr.length() > 0) arr.optInt(0).takeIf { it > 0 } else null
                anibridgeCache[tmdbId] = id ?: -1
                id
            }
        } catch (_: Exception) {
            anibridgeCache[tmdbId] = -1
            null
        }
    }

    private fun buildAnimeFromTmdb(item: JSONObject, anilistId: Int): Anime? {
        val title = item.optString("name").ifBlank {
            item.optString("original_name").ifBlank { return null }
        }
        val posterPath  = item.optString("poster_path").takeIf { it.isNotBlank() } ?: return null
        val backdropPath = item.optString("backdrop_path").takeIf { it.isNotBlank() }
        val cover  = "$TMDB_IMG/w500$posterPath"
        val banner = backdropPath?.let { "$TMDB_IMG/w1280$it" }
        val year   = item.optString("first_air_date").take(4).toIntOrNull()
        val voteAvg = item.optDouble("vote_average", 0.0)
        val score  = if (voteAvg > 0.0) (voteAvg * 10.0).toInt().coerceIn(1, 100) else null
        val overview = item.optString("overview").takeIf { it.isNotBlank() } ?: ""

        val tmdbGenreMap = mapOf(
            10759 to "Action", 35 to "Comedy", 18 to "Drama",
            10749 to "Romance", 9648 to "Mystery", 10765 to "Fantasy",
            27 to "Horror", 878 to "Sci-Fi", 10762 to "Kids",
            10751 to "Family", 10764 to "Reality", 10768 to "War"
        )
        val genreIds = item.optJSONArray("genre_ids")
        val genres = buildList {
            if (genreIds != null) {
                for (i in 0 until genreIds.length()) {
                    tmdbGenreMap[genreIds.optInt(i)]?.let { add(it) }
                }
            }
        }

        return Anime(
            id           = anilistId,
            title        = title,
            romajiTitle  = item.optString("original_name").takeIf { it.isNotBlank() && it != title },
            cover        = cover,
            banner       = banner,
            description  = overview,
            score        = score,
            seasonYear   = year,
            genres       = genres,
            format       = "TV",
            status       = "RELEASING"
        )
    }

    suspend fun fetchMangaDetails(title: String? = null, anilistId: Int? = null): JSONObject? = withContext(Dispatchers.IO) {
        if (title.isNullOrBlank() && (anilistId == null || anilistId <= 0)) return@withContext null
        try {
            val query = """
                query(${'$'}id: Int, ${'$'}search: String) {
                  Media(id: ${'$'}id, search: ${'$'}search, type: MANGA) {
                    id
                    idMal
                    title { romaji english userPreferred native }
                    coverImage { extraLarge large }
                    bannerImage
                    description
                    genres
                    status
                    averageScore
                    chapters
                    volumes
                    format
                    source
                    staff(perPage: 6, sort: [RELEVANCE]) {
                      edges {
                        role
                        node { name { full } }
                      }
                    }
                  }
                }
            """.trimIndent()
            val variables = JSONObject().apply {
                if (anilistId != null && anilistId > 0) put("id", anilistId)
                else if (!title.isNullOrBlank()) put("search", title.trim())
            }
            val response = post(query, variables)
            response.optJSONObject("data")?.optJSONObject("Media")
        } catch (_: Exception) {
            null
        }
    }
}
