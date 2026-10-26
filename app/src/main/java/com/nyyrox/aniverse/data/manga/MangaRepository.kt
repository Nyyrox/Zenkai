package com.nyyrox.aniverse.data.manga

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.nyyrox.aniverse.data.MediaRelation
import org.json.JSONArray
import org.json.JSONObject

object MangaRepository {
    private const val TAG = "MangaRepository"

    val mangaDex = MangaDexRepository()
    val comicK = ComicKRepository()
    val manhua = ManhuaRepository(comicK)
    val asura = AsuraScansRepository()
    val demonicScans = DemonicScansRepository()
    val weebCentral = WeebCentralRepository()

    private val repositories = mapOf<MangaSource, IMangaSourceRepository>(
        MangaSource.WEEBCENTRAL to weebCentral,
        MangaSource.DEMONIC_SCANS to demonicScans,
        MangaSource.COMICK to comicK,
        MangaSource.MANHUA to manhua,
        MangaSource.ASURA to asura,
        MangaSource.MANGADEX to mangaDex
    )

    private val _activeSource = MutableStateFlow(MangaSource.WEEBCENTRAL)
    val activeSource: StateFlow<MangaSource> = _activeSource.asStateFlow()

    private val aniListRepo by lazy { com.nyyrox.aniverse.data.AniListRepository() }

    fun parseRelations(data: JSONObject?): List<MediaRelation> = buildList {
        val relEdges = data?.optJSONObject("relations")?.optJSONArray("edges") ?: return@buildList
        for (i in 0 until relEdges.length()) {
            val edge = relEdges.optJSONObject(i) ?: continue
            val relType = edge.optString("relationType")
            val node = edge.optJSONObject("node") ?: continue
            val relId = node.optInt("id")
            val nodeType = node.optString("type")
            val titleObj = node.optJSONObject("title")
            val relTitle = titleObj?.optString("english")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: titleObj?.optString("userPreferred")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: titleObj?.optString("romaji").orEmpty()
            val romaji = titleObj?.optString("romaji")
            val relCover = node.optJSONObject("coverImage")?.optString("large").orEmpty()
            val relFormat = node.optString("format")
            val relStatus = node.optString("status")
            val nextAiringObj = node.optJSONObject("nextAiringEpisode")
            val nextAiringEp = nextAiringObj?.optInt("episode")?.takeIf { it > 0 }
            val nextAiringAt = nextAiringObj?.optLong("airingAt")?.takeIf { it > 0 }
            if (relId > 0 && relTitle.isNotBlank()) {
                add(
                    MediaRelation(
                        id = relId,
                        type = nodeType,
                        relationType = relType,
                        title = relTitle,
                        romajiTitle = romaji,
                        cover = relCover,
                        format = relFormat,
                        status = relStatus,
                        nextAiringEpisode = nextAiringEp,
                        nextAiringTime = nextAiringAt
                    )
                )
            }
        }
    }

    fun setSource(source: MangaSource) {
        _activeSource.value = source
    }

    fun getRepoForSource(source: MangaSource): IMangaSourceRepository =
        repositories[source] ?: weebCentral

    val currentRepo: IMangaSourceRepository get() = getRepoForSource(_activeSource.value)

    fun getSourceForId(id: String): MangaSource {
        return when {
            id.startsWith("md_") -> MangaSource.MANGADEX
            id.startsWith("ck_") -> MangaSource.COMICK
            id.startsWith("mh_") -> MangaSource.MANHUA
            id.startsWith("as_") -> MangaSource.ASURA
            id.startsWith("ds_") -> MangaSource.DEMONIC_SCANS
            id.startsWith("wc_") -> MangaSource.WEEBCENTRAL
            else -> _activeSource.value
        }
    }

    fun cleanWeebId(id: String): String {
        val s = id.trim().substringBefore("?").substringBefore("#")
        if (s.startsWith("md_") || s.startsWith("ck_") || s.startsWith("mh_") || s.startsWith("as_") || s.startsWith("ds_") || s.startsWith("wc_")) {
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
        if (s.startsWith("ds_")) {
            return s.removePrefix("ds_").replace("-", " ").replace("_", " ").trim()
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
        if (s.startsWith("md_") || s.startsWith("ck_") || s.startsWith("mh_") || s.startsWith("as_") || s.startsWith("ds_") || s.startsWith("wc_")) {
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
            url.contains("demoniclibs") || url.contains("demonicscans") || url.contains("mangareadon") || url.contains("readermc") -> MangaSource.DEMONIC_SCANS
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
    suspend fun getHome(): MangaHome = withContext(Dispatchers.IO) {
        try {
            val aniHome = aniListRepo.fetchMangaHome()
            if (aniHome.trending.isNotEmpty() || aniHome.popular.isNotEmpty()) {
                return@withContext aniHome
            }
        } catch (e: Exception) {
            Log.w(TAG, "AniList getHome failed, falling back to ${activeSource.value}: ${e.message}")
        }
        currentRepo.getHome()
    }

    // ── 2. SEARCH ───────────────────────────────────────────────────────────
    suspend fun search(query: String): List<MangaCard> = withContext(Dispatchers.IO) {
        try {
            val ani = aniListRepo.searchManga(query)
            if (ani.isNotEmpty()) {
                return@withContext ani
            }
        } catch (e: Exception) {
            Log.w(TAG, "AniList search failed, falling back to ${activeSource.value}: ${e.message}")
        }
        currentRepo.search(query)
    }

    // ── 2.2 SOURCE AVAILABILITY & DIRECT CHAPTER FETCH ───────────────────────
    suspend fun findAvailableSources(
        title: String,
        altTitles: List<String> = emptyList()
    ): List<AvailableSourceInfo> = withContext(Dispatchers.IO) {
        val candidates = (listOf(title) + altTitles)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
            .distinct()

        repositories.entries.map { (source, repo) ->
            async {
                for (cand in candidates) {
                    try {
                        val results = repo.search(cand)
                        if (results.isNotEmpty()) {
                            val best = results.find { it.title.equals(cand, ignoreCase = true) }
                                ?: results.find { it.title.contains(cand, ignoreCase = true) }
                                ?: results.first()
                            val chapters = try { repo.getChapters(best.id) } catch (_: Exception) { emptyList() }
                            return@async AvailableSourceInfo(
                                source = source,
                                matchedId = best.id,
                                matchedTitle = best.title,
                                chapterCount = if (chapters.isNotEmpty()) chapters.size else null,
                                latestChapter = chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number
                            )
                        }
                    } catch (_: Exception) {}
                }
                null
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getChaptersForSource(source: MangaSource, mangaId: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val repo = getRepoForSource(source)
        repo.getChapters(mangaId)
    }

    // ── 2.5 RESOLUTION ─────────────────────────────────────────────────────
    suspend fun resolveMangaDetails(
        mangaId: String,
        titleFallback: String? = null
    ): Triple<String, MangaInfo?, List<MangaChapter>> = withContext(Dispatchers.IO) {
        val src = getSourceForId(mangaId)
        val isNumericAniListId = mangaId.isNotBlank() && mangaId.all { it.isDigit() } && mangaId.length <= 8
        val aniId = if (isNumericAniListId) mangaId.toIntOrNull() else null

        // 1. Fetch AniList metadata
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
        val aniRomajiTitle: String? = aniData?.optJSONObject("title")?.optString("romaji")
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
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

        // Parse characters from AniList
        val aniCharacters: List<MangaCharacter> = buildList {
            val charNodes = aniData?.optJSONObject("characters")?.optJSONArray("nodes")
            if (charNodes != null) {
                for (i in 0 until charNodes.length()) {
                    val cn = charNodes.optJSONObject(i) ?: continue
                    val name = cn.optJSONObject("name")?.let {
                        it.optString("full").takeIf { s -> s.isNotBlank() } ?: it.optString("userPreferred")
                    }
                    val img = cn.optJSONObject("image")?.let {
                        it.optString("large").takeIf { s -> s.isNotBlank() } ?: it.optString("medium")
                    }
                    if (!name.isNullOrBlank()) {
                        add(MangaCharacter(name = name, image = img))
                    }
                }
            }
        }

        // Parse media relations from AniList
        val aniRelations = parseRelations(aniData)
        val relAnime = aniRelations.firstOrNull { it.type.equals("ANIME", ignoreCase = true) }
        var relAnimeId: Int? = relAnime?.id
        var relAnimeTitle: String? = relAnime?.title

        // 2. Direct fetch if NOT purely numeric AniList ID and has recognized prefix
        if (!isNumericAniListId && (mangaId.startsWith("md_") || mangaId.startsWith("ck_") || mangaId.startsWith("mh_") || mangaId.startsWith("as_") || mangaId.startsWith("ds_") || mangaId.startsWith("wc_"))) {
            val repo = getRepoForSource(src)
            val infoDeferred = async { repo.getInfo(mangaId) }
            val chaptersDeferred = async { repo.getChapters(mangaId) }
            val info = infoDeferred.await()
            val chapters = chaptersDeferred.await()

            if (info != null) {
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

                // Find available sources for cross-source switcher on details page
                val availSources = findAvailableSources(curAniTitle ?: info.title, listOfNotNull(aniRomajiTitle, info.altTitle))

                var resolvedChapters = chapters
                var resolvedId = mangaId

                if (resolvedChapters.isEmpty() && availSources.isNotEmpty()) {
                    val firstAvail = availSources.first()
                    try {
                        val altChs = getChaptersForSource(firstAvail.source, firstAvail.matchedId)
                        if (altChs.isNotEmpty()) {
                            resolvedChapters = altChs
                            resolvedId = firstAvail.matchedId
                        }
                    } catch (_: Exception) {}
                }

                val curAniRelations = if (effectiveAni != null && effectiveAni != aniData) {
                    parseRelations(effectiveAni).ifEmpty { aniRelations }
                } else aniRelations
                val curRelAnime = curAniRelations.firstOrNull { it.type.equals("ANIME", ignoreCase = true) }
                val curRelAnimeId = curRelAnime?.id ?: relAnimeId
                val curRelAnimeTitle = curRelAnime?.title ?: relAnimeTitle

                val enrichedInfo = info.copy(
                    title = curAniTitle ?: info.title,
                    altTitle = aniRomajiTitle ?: info.altTitle,
                    poster = curAniCover ?: info.poster,
                    banner = curAniBanner ?: info.banner ?: curAniCover ?: info.poster,
                    description = if (info.description.isBlank()) (curAniDesc ?: "") else info.description,
                    genres = if (info.genres.isEmpty()) curAniGenres else info.genres,
                    rating = info.rating ?: curAniScore,
                    status = info.status ?: curAniStatus,
                    type = info.type ?: curAniFormat ?: "Manga",
                    anilistId = aniId ?: effectiveAni?.optInt("id")?.takeIf { it > 0 },
                    totalChapters = curAniChapters ?: info.totalChapters,
                    authors = if (info.authors.isEmpty()) curAniAuthors else info.authors,
                    characters = aniCharacters,
                    relatedAnimeId = curRelAnimeId,
                    relatedAnimeTitle = curRelAnimeTitle,
                    relatedAnimeNextAiringEpisode = curRelAnime?.nextAiringEpisode ?: relAnime?.nextAiringEpisode,
                    relatedAnimeNextAiringTime = curRelAnime?.nextAiringTime ?: relAnime?.nextAiringTime,
                    availableSources = availSources,
                    relations = curAniRelations
                )
                return@withContext Triple(resolvedId, enrichedInfo, resolvedChapters)
            }
        }

        // 3. Resolution via title or numeric AniList ID: search all sources for availability
        val searchTarget = aniTitle?.trim()
            ?.ifBlank { null }
            ?: titleFallback?.trim()?.ifBlank { null }
            ?: extractSlugTitle(mangaId)
            ?: mangaId.takeIf { !isNumericAniListId && it.length < 30 }

        val availSources = if (!searchTarget.isNullOrBlank() && searchTarget.length >= 2) {
            findAvailableSources(searchTarget, listOfNotNull(aniRomajiTitle))
        } else emptyList()

        if (availSources.isNotEmpty()) {
            val primary = availSources.find { it.source == _activeSource.value } ?: availSources.first()
            val repo = getRepoForSource(primary.source)
            val primaryInfo = try { repo.getInfo(primary.matchedId) } catch (_: Exception) { null }
            val primaryChapters = try { repo.getChapters(primary.matchedId) } catch (_: Exception) { emptyList() }

            val resolvedInfo = MangaInfo(
                id = primary.matchedId,
                title = aniTitle ?: primaryInfo?.title ?: primary.matchedTitle,
                altTitle = aniRomajiTitle ?: primaryInfo?.altTitle,
                poster = aniCover ?: primaryInfo?.poster,
                banner = aniBanner ?: primaryInfo?.banner ?: aniCover ?: primaryInfo?.poster,
                description = primaryInfo?.description?.takeIf { it.isNotBlank() } ?: (aniDesc ?: ""),
                genres = primaryInfo?.genres?.takeIf { it.isNotEmpty() } ?: aniGenres,
                status = primaryInfo?.status ?: aniStatus,
                rating = primaryInfo?.rating ?: aniScore,
                type = primaryInfo?.type ?: aniFormat ?: "Manga",
                source = primary.source.id,
                anilistId = aniId ?: aniData?.optInt("id")?.takeIf { it > 0 },
                totalChapters = aniChapters ?: primaryInfo?.totalChapters,
                authors = primaryInfo?.authors?.takeIf { it.isNotEmpty() } ?: aniAuthors,
                characters = aniCharacters,
                relatedAnimeId = relAnimeId,
                relatedAnimeTitle = relAnimeTitle,
                relatedAnimeNextAiringEpisode = relAnime?.nextAiringEpisode,
                relatedAnimeNextAiringTime = relAnime?.nextAiringTime,
                availableSources = availSources,
                relations = aniRelations
            )
            return@withContext Triple(primary.matchedId, resolvedInfo, primaryChapters)
        }

        val fallbackInfo = if (aniData != null && !aniTitle.isNullOrBlank()) {
            val fallbackAniId: Int? = aniId ?: aniData.optInt("id").takeIf { it > 0 }
            MangaInfo(
                id = mangaId,
                title = aniTitle,
                altTitle = aniRomajiTitle,
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
                authors = aniAuthors,
                characters = aniCharacters,
                relatedAnimeId = relAnimeId,
                relatedAnimeTitle = relAnimeTitle,
                relatedAnimeNextAiringEpisode = relAnime?.nextAiringEpisode,
                relatedAnimeNextAiringTime = relAnime?.nextAiringTime,
                availableSources = emptyList(),
                relations = aniRelations
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
