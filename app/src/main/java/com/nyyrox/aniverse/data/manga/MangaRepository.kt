package com.nyyrox.aniverse.data.manga

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object MangaRepository {
    private const val TAG = "MangaRepository"

    val mangaDex = MangaDexRepository()
    val comicK = ComicKRepository()
    val manhua = ManhuaRepository(comicK)
    val asura = AsuraScansRepository()
    val weebCentral = WeebCentralRepository()

    private val repositories = mapOf<MangaSource, IMangaSourceRepository>(
        MangaSource.MANGADEX to mangaDex,
        MangaSource.COMICK to comicK,
        MangaSource.MANHUA to manhua,
        MangaSource.ASURA to asura,
        MangaSource.WEEBCENTRAL to weebCentral
    )

    private val _activeSource = MutableStateFlow(MangaSource.MANGADEX)
    val activeSource: StateFlow<MangaSource> = _activeSource.asStateFlow()

    private val aniListRepo by lazy { com.nyyrox.aniverse.data.AniListRepository() }

    fun setSource(source: MangaSource) {
        _activeSource.value = source
    }

    fun getRepoForSource(source: MangaSource): IMangaSourceRepository =
        repositories[source] ?: mangaDex

    val currentRepo: IMangaSourceRepository get() = getRepoForSource(_activeSource.value)

    fun getSourceForId(id: String): MangaSource {
        return when {
            id.startsWith("md_") -> MangaSource.MANGADEX
            id.startsWith("ck_") -> MangaSource.COMICK
            id.startsWith("mh_") -> MangaSource.MANHUA
            id.startsWith("as_") -> MangaSource.ASURA
            id.startsWith("wc_") -> MangaSource.WEEBCENTRAL
            else -> _activeSource.value
        }
    }

    fun cleanWeebId(id: String): String {
        val s = id.trim().substringBefore("?").substringBefore("#")
        if (s.startsWith("md_") || s.startsWith("ck_") || s.startsWith("mh_") || s.startsWith("as_") || s.startsWith("wc_")) {
            return s
        }
        if (s.contains("/series/")) {
            val after = s.substringAfter("/series/")
            val seriesId = after.substringBefore("/").substringBefore("_")
            if (seriesId.isNotBlank()) return "wc_$seriesId"
        }
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) return "wc_${ulidMatch.groupValues[1]}"
        return s
    }

    fun extractSlugTitle(id: String): String? {
        val s = id.trim().substringBefore("?").substringBefore("#")
        if (s.contains("/series/")) {
            val after = s.substringAfter("/series/")
            val parts = after.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return parts[1].replace("-", " ").replace("_", " ").trim()
            }
        }
        if (s.startsWith("ck_")) {
            return s.removePrefix("ck_").replace("-", " ").replace("_", " ").trim()
        }
        if (s.startsWith("mh_")) {
            return s.removePrefix("mh_").replace("-", " ").replace("_", " ").trim()
        }
        if (s.startsWith("as_")) {
            return s.removePrefix("as_").replace("-", " ").replace("_", " ").trim()
        }
        if (s.startsWith("wc_") && s.contains("_")) {
            val parts = s.removePrefix("wc_").split("_")
            if (parts.size >= 2) {
                return parts.drop(1).joinToString(" ").replace("-", " ").replace("_", " ").trim()
            }
        }
        if (s.startsWith("md_")) {
            return s.removePrefix("md_").replace("-", " ").replace("_", " ").trim()
        }
        return null
    }

    fun cleanChapterId(chapterId: String): String {
        val s = chapterId.trim().substringBefore("?").substringBefore("#")
        if (s.startsWith("md_") || s.startsWith("ck_") || s.startsWith("mh_") || s.startsWith("as_") || s.startsWith("wc_")) {
            return s
        }
        if (s.contains("/chapters/")) {
            val after = s.substringAfter("/chapters/").substringBefore("/")
            val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(after)
            if (ulidMatch != null) return "wc_${ulidMatch.groupValues[1]}"
            return "wc_$after"
        }
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) return "wc_${ulidMatch.groupValues[1]}"
        return s
    }

    fun proxyImageUrl(url: String, source: MangaSource? = null): String {
        if (url.isBlank() || !url.startsWith("http")) return url
        val effSource = source ?: when {
            url.contains("comick") || url.contains("meo") -> MangaSource.COMICK
            url.contains("asura") -> MangaSource.ASURA
            url.contains("mangadex") -> MangaSource.MANGADEX
            else -> MangaSource.WEEBCENTRAL
        }
        val referer = effSource.baseUrl + "/"
        return com.nyyrox.aniverse.player.StreamProxyServer.buildProxyUrl(
            url,
            mapOf(
                "Referer" to referer,
                "Origin" to effSource.baseUrl,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
        )
    }

    // ── 1. HOME ─────────────────────────────────────────────────────────────
    suspend fun getHome(): MangaHome = currentRepo.getHome()

    // ── 2. SEARCH ───────────────────────────────────────────────────────────
    suspend fun search(query: String): List<MangaCard> = currentRepo.search(query)

    // ── 2.5 RESOLUTION ─────────────────────────────────────────────────────
    suspend fun resolveMangaDetails(
        mangaId: String,
        titleFallback: String? = null
    ): Triple<String, MangaInfo?, List<MangaChapter>> = withContext(Dispatchers.IO) {
        val src = getSourceForId(mangaId)
        val isNumericAniListId = mangaId.isNotBlank() && mangaId.all { it.isDigit() } && mangaId.length <= 8
        val aniId = if (isNumericAniListId) mangaId.toIntOrNull() else null

        // 1. If it's a numeric AniList ID or we have titleFallback, query AniList for metadata first
        val aniData: JSONObject? = try {
            aniListRepo.fetchMangaDetails(
                title = titleFallback?.takeIf { !isNumericAniListId },
                anilistId = aniId
            )
        } catch (_: Exception) {
            null
        }

        val aniTitle: String? = aniData?.optJSONObject("title")?.let { t ->
            val eng = t.optString("english").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val rom = t.optString("romaji").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val usr = t.optString("userPreferred").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            eng ?: rom ?: usr
        }
        val aniCover: String? = aniData?.optJSONObject("coverImage")?.let { c ->
            val xl = c.optString("extraLarge").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val l = c.optString("large").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            xl ?: l
        }
        val aniBanner: String? = aniData?.optString("bannerImage")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val aniDesc: String? = aniData?.optString("description")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?.replace(Regex("<[^>]*>"), "")?.trim()
        val aniGenres: List<String> = buildList {
            val arr = aniData?.optJSONArray("genres")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val g = arr.optString(i).trim()
                    if (g.isNotBlank() && !g.equals("null", ignoreCase = true)) add(g)
                }
            }
        }
        val aniScore: String? = aniData?.optInt("averageScore")?.takeIf { it > 0 }?.let { "$it%" }
        val aniStatus: String? = aniData?.optString("status")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val aniFormat: String? = aniData?.optString("format")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val aniChapters: Int? = aniData?.optInt("chapters")?.takeIf { it > 0 }
        val aniAuthors: List<String> = buildList {
            val edges: JSONArray? = aniData?.optJSONObject("staff")?.optJSONArray("edges")
            if (edges != null) {
                for (i in 0 until edges.length()) {
                    val edgeObj = edges.optJSONObject(i)
                    val node = edgeObj?.optJSONObject("node")
                    val name = node?.optJSONObject("name")?.optString("full")
                    if (!name.isNullOrBlank()) add(name)
                }
            }
        }

        // 2. Direct fetch if NOT purely numeric AniList ID and has recognized prefix
        if (!isNumericAniListId && (mangaId.startsWith("md_") || mangaId.startsWith("ck_") || mangaId.startsWith("mh_") || mangaId.startsWith("as_") || mangaId.startsWith("wc_"))) {
            val repo = getRepoForSource(src)
            val infoDeferred = async { repo.getInfo(mangaId) }
            val chaptersDeferred = async { repo.getChapters(mangaId) }
            val info = infoDeferred.await()
            val chapters = chaptersDeferred.await()

            if (info != null) {
                // Enrich with AniList using info.title if not yet queried
                val effectiveAni = aniData ?: try {
                    if (info.title.isNotBlank() && info.title != "Unknown") {
                        aniListRepo.fetchMangaDetails(title = info.title)
                    } else null
                } catch (_: Exception) { null }

                val curAniTitle = effectiveAni?.optJSONObject("title")?.let { t ->
                    val eng = t.optString("english").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val rom = t.optString("romaji").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val usr = t.optString("userPreferred").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    eng ?: rom ?: usr
                } ?: aniTitle

                val curAniCover = effectiveAni?.optJSONObject("coverImage")?.let { c ->
                    val xl = c.optString("extraLarge").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    val l = c.optString("large").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    xl ?: l
                } ?: aniCover

                val curAniBanner = effectiveAni?.optString("bannerImage")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: aniBanner

                val curAniDesc = effectiveAni?.optString("description")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?.replace(Regex("<[^>]*>"), "")?.trim() ?: aniDesc

                val curAniGenres = buildList {
                    val arr = effectiveAni?.optJSONArray("genres")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val g = arr.optString(i).trim()
                            if (g.isNotBlank() && !g.equals("null", ignoreCase = true)) add(g)
                        }
                    }
                }.ifEmpty { aniGenres }

                val curAniScore = effectiveAni?.optInt("averageScore")?.takeIf { it > 0 }?.let { "$it%" } ?: aniScore
                val curAniStatus = effectiveAni?.optString("status")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: aniStatus
                val curAniFormat = effectiveAni?.optString("format")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: aniFormat
                val curAniChapters = effectiveAni?.optInt("chapters")?.takeIf { it > 0 } ?: aniChapters
                val curAniAuthors = buildList {
                    val edges = effectiveAni?.optJSONObject("staff")?.optJSONArray("edges")
                    if (edges != null) {
                        for (i in 0 until edges.length()) {
                            val name = edges.optJSONObject(i)?.optJSONObject("node")?.optJSONObject("name")?.optString("full")
                            if (!name.isNullOrBlank()) add(name)
                        }
                    }
                }.ifEmpty { aniAuthors }

                var resolvedChapters = chapters
                var resolvedId = mangaId

                // If chapters are empty or severely incomplete compared to expected chapters (e.g. MangaDex licensed titles with only external links), search other sources for readable chapters!
                val expectedChapters = curAniChapters ?: 10
                if (resolvedChapters.size <= 1 || (resolvedChapters.size < 5 && expectedChapters >= 15)) {
                    val searchQuery = curAniTitle ?: info.title
                    val otherSources = repositories.keys.filter { it != src }
                    for (otherSrc in otherSources) {
                        val otherRepo = repositories[otherSrc] ?: continue
                        try {
                            val searchResults = otherRepo.search(searchQuery)
                            if (searchResults.isNotEmpty()) {
                                val match = searchResults.find { it.title.equals(searchQuery, ignoreCase = true) } ?: searchResults.first()
                                val altChs = otherRepo.getChapters(match.id)
                                if (altChs.size > resolvedChapters.size) {
                                    resolvedChapters = altChs
                                    resolvedId = match.id
                                    break
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val enrichedInfo = info.copy(
                    title = curAniTitle ?: info.title,
                    poster = curAniCover ?: info.poster,
                    banner = curAniBanner ?: info.banner ?: curAniCover ?: info.poster,
                    description = if (info.description.isBlank()) (curAniDesc ?: "") else info.description,
                    genres = if (info.genres.isEmpty()) curAniGenres else info.genres,
                    rating = info.rating ?: curAniScore,
                    status = info.status ?: curAniStatus,
                    type = info.type ?: curAniFormat ?: "Manga",
                    anilistId = aniId ?: effectiveAni?.optInt("id")?.takeIf { it > 0 },
                    totalChapters = curAniChapters ?: info.totalChapters,
                    authors = if (info.authors.isEmpty()) curAniAuthors else info.authors
                )
                return@withContext Triple(resolvedId, enrichedInfo, resolvedChapters)
            }
        }

        // 3. Fallback resolution via query candidate: search active source first, then others
        val queryCandidate = aniTitle?.trim()
            ?.ifBlank { null }
            ?: titleFallback?.trim()?.ifBlank { null }
            ?: extractSlugTitle(mangaId)
            ?: mangaId.takeIf { !isNumericAniListId && it.length < 30 }

        if (!queryCandidate.isNullOrBlank() && queryCandidate.length >= 2) {
            val orderedSources = listOf(_activeSource.value) + repositories.keys.filter { it != _activeSource.value }
            for (s in orderedSources) {
                val repo = repositories[s] ?: continue
                try {
                    val searchResults = repo.search(queryCandidate)
                    if (searchResults.isNotEmpty()) {
                        val bestMatch = searchResults.find { it.title.equals(queryCandidate, ignoreCase = true) }
                            ?: searchResults.first()
                        val rawInfo = repo.getInfo(bestMatch.id)
                        val chapters = repo.getChapters(bestMatch.id)
                        if (chapters.isNotEmpty() || rawInfo != null) {
                            val resolvedInfo = MangaInfo(
                                id = bestMatch.id,
                                title = aniTitle ?: rawInfo?.title ?: bestMatch.title,
                                poster = aniCover ?: rawInfo?.poster ?: bestMatch.poster,
                                banner = aniBanner ?: rawInfo?.banner ?: aniCover ?: rawInfo?.poster,
                                description = rawInfo?.description?.takeIf { it.isNotBlank() } ?: (aniDesc ?: ""),
                                genres = rawInfo?.genres?.takeIf { it.isNotEmpty() } ?: aniGenres,
                                status = rawInfo?.status ?: aniStatus,
                                rating = rawInfo?.rating ?: aniScore,
                                type = rawInfo?.type ?: aniFormat ?: bestMatch.type ?: "Manga",
                                source = s.id,
                                anilistId = aniId ?: aniData?.optInt("id")?.takeIf { it > 0 },
                                totalChapters = aniChapters ?: rawInfo?.totalChapters,
                                authors = rawInfo?.authors?.takeIf { it.isNotEmpty() } ?: aniAuthors
                            )
                            return@withContext Triple(bestMatch.id, resolvedInfo, chapters)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Search resolution failed for source ${s.displayName}: ${e.message}")
                }
            }
        }

        val fallbackInfo = if (aniData != null && !aniTitle.isNullOrBlank()) {
            val fallbackAniId: Int? = aniId ?: aniData.optInt("id").takeIf { it > 0 }
            MangaInfo(
                id = mangaId,
                title = aniTitle,
                poster = aniCover,
                banner = aniBanner,
                description = aniDesc ?: "",
                genres = aniGenres,
                status = aniStatus,
                rating = aniScore,
                type = aniFormat ?: "Manga",
                source = "anilist",
                anilistId = fallbackAniId,
                totalChapters = aniChapters,
                authors = aniAuthors
            )
        } else null

        Triple(mangaId, fallbackInfo, emptyList())
    }

    // ── 3. INFO ─────────────────────────────────────────────────────────────
    suspend fun getInfo(mangaId: String): MangaInfo? {
        val src = getSourceForId(mangaId)
        val repo = getRepoForSource(src)
        return repo.getInfo(mangaId)
    }

    // ── 4. CHAPTERS ─────────────────────────────────────────────────────────
    suspend fun getChapters(mangaId: String): List<MangaChapter> {
        val src = getSourceForId(mangaId)
        val repo = getRepoForSource(src)
        return repo.getChapters(mangaId)
    }

    // ── 5. PAGES ────────────────────────────────────────────────────────────
    suspend fun getPages(chapterId: String): List<String> {
        val src = getSourceForId(chapterId)
        val repo = getRepoForSource(src)
        return repo.getPages(chapterId)
    }
}
