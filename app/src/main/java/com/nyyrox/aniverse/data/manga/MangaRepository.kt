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

    val PREFERRED_SOURCE_ORDER = listOf(
        MangaSource.WEEBCENTRAL,
        MangaSource.DEMONIC_SCANS,
        MangaSource.COMICK,
        MangaSource.MANHUA,
        MangaSource.ASURA,
        MangaSource.MANGADEX
    )

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
                            val matching = results.filter {
                                it.title.equals(cand, ignoreCase = true) ||
                                it.title.contains(cand, ignoreCase = true) ||
                                cand.contains(it.title, ignoreCase = true)
                            }.ifEmpty { listOf(results.first()) }

                            val best = matching.find { it.title.equals(cand, ignoreCase = true) } ?: matching.first()
                            val chapters = try { repo.getChapters(best.id) } catch (_: Exception) { emptyList() }

                            val editions = matching.take(4).map { r ->
                                MatchedEdition(
                                    id = r.id,
                                    title = r.title,
                                    poster = r.poster,
                                    chapterCount = if (r.id == best.id && chapters.isNotEmpty()) chapters.size else null,
                                    latestChapter = if (r.id == best.id) (chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number) else null
                                )
                            }

                            return@async AvailableSourceInfo(
                                source = source,
                                matchedId = best.id,
                                matchedTitle = best.title,
                                chapterCount = if (chapters.isNotEmpty()) chapters.size else null,
                                latestChapter = chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number,
                                editions = editions
                            )
                        }
                    } catch (_: Exception) {}
                }
                null
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun checkSourcesProgressive(
        title: String,
        altTitles: List<String> = emptyList(),
        onSourceFound: (AvailableSourceInfo) -> Unit,
        onProgress: (checked: Int, total: Int) -> Unit
    ): List<AvailableSourceInfo> = withContext(Dispatchers.IO) {
        val candidates = (listOf(title) + altTitles)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
            .distinct()

        val total = repositories.size
        val checkedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val foundList = java.util.Collections.synchronizedList(mutableListOf<AvailableSourceInfo>())

        repositories.entries.map { (source, repo) ->
            async {
                var info: AvailableSourceInfo? = null
                for (cand in candidates) {
                    try {
                        val results = repo.search(cand)
                        if (results.isNotEmpty()) {
                            val matching = results.filter {
                                it.title.equals(cand, ignoreCase = true) ||
                                it.title.contains(cand, ignoreCase = true) ||
                                cand.contains(it.title, ignoreCase = true)
                            }.ifEmpty { listOf(results.first()) }

                            val best = matching.find { it.title.equals(cand, ignoreCase = true) } ?: matching.first()
                            val chapters = try { repo.getChapters(best.id) } catch (_: Exception) { emptyList() }

                            val editions = matching.take(4).map { r ->
                                MatchedEdition(
                                    id = r.id,
                                    title = r.title,
                                    poster = r.poster,
                                    chapterCount = if (r.id == best.id && chapters.isNotEmpty()) chapters.size else null,
                                    latestChapter = if (r.id == best.id) (chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number) else null
                                )
                            }

                            info = AvailableSourceInfo(
                                source = source,
                                matchedId = best.id,
                                matchedTitle = best.title,
                                chapterCount = if (chapters.isNotEmpty()) chapters.size else null,
                                latestChapter = chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number,
                                editions = editions
                            )
                            break
                        }
                    } catch (_: Exception) {}
                }

                val cur = checkedCount.incrementAndGet()
                if (info != null) {
                    foundList.add(info)
                    onSourceFound(info)
                }
                onProgress(cur, total)
                info
            }
        }.awaitAll()

        foundList.toList()
    }

    suspend fun findServerShowsProgressive(
        title: String,
        altTitles: List<String> = emptyList(),
        onShowFound: (ServerShowItem) -> Unit,
        onProgress: (checked: Int, total: Int) -> Unit
    ): List<ServerShowItem> = withContext(Dispatchers.IO) {
        val names = (listOf(title) + altTitles)
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length >= 2 }
            .distinct()

        val variants = MangaTitleMatcher.searchVariants(names)
        val orderedSources = repositories.keys.toList()
        val total = orderedSources.size
        val checkedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val foundShows = java.util.Collections.synchronizedList(mutableListOf<ServerShowItem>())

        orderedSources.map { source ->
            async {
                val repo = repositories[source]
                if (repo != null) {
                    val seenIds = mutableSetOf<String>()
                    val serverMatches = mutableListOf<Pair<ServerShowItem, Float>>()
                    for (v in variants) {
                        try {
                            val results = repo.search(v)
                            for (item in results) {
                                if (seenIds.add(item.id)) {
                                    val score = MangaTitleMatcher.bestSimilarity(item.title, names)
                                    if (score >= MangaTitleMatcher.MATCH_AT) {
                                        val itemRating = if (!item.rating.isNullOrBlank()) {
                                            val cln = item.rating.trim().removeSuffix("%").trim()
                                            val num = cln.toDoubleOrNull()
                                            if (num != null && num > 0) {
                                                val s = if (num > 10) num / 10.0 else num
                                                String.format(java.util.Locale.US, "%.1f", s)
                                            } else cln
                                        } else if (item.score != null && item.score > 0) {
                                            val s = if (item.score > 10) item.score / 10.0 else item.score
                                            String.format(java.util.Locale.US, "%.1f", s)
                                        } else null

                                        val show = ServerShowItem(
                                            id = item.id,
                                            title = item.title,
                                            poster = item.poster,
                                            source = source,
                                            score = score,
                                            chapterCount = item.chapter?.replace(Regex("[^0-9]"), "")?.toIntOrNull(),
                                            latestChapter = item.chapter,
                                            caption = listOfNotNull(source.displayName, item.chapter).joinToString(" · "),
                                            rating = itemRating
                                        )
                                        serverMatches.add(show to score)
                                    }
                                }
                            }
                            if (serverMatches.any { it.second >= 0.85f }) break
                        } catch (_: Exception) {}
                    }
                    val topForServer = serverMatches
                        .sortedByDescending { it.second }
                        .take(3)
                        .map { it.first }

                    for (show in topForServer) {
                        foundShows.add(show)
                        onShowFound(show)
                    }
                }
                val cur = checkedCount.incrementAndGet()
                onProgress(cur, total)
            }
        }.awaitAll()

        foundShows.sortedWith(
            compareByDescending<ServerShowItem> { it.score }
                .thenBy { orderedSources.indexOf(it.source).let { idx -> if (idx < 0) 999 else idx } }
        )
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
        val aniAvg = aniData?.optInt("averageScore")?.takeIf { it > 0 }
        val aniScore: String? = aniAvg?.let { String.format(java.util.Locale.US, "%.1f", it / 10.0) }
        val aniScoreDouble: Double? = aniAvg?.let { it / 10.0 }
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

                val curAniAvg = effectiveAni?.optInt("averageScore")?.takeIf { it > 0 }
                val curAniScore = curAniAvg?.let { String.format(java.util.Locale.US, "%.1f", it / 10.0) } ?: aniScore
                val curAniScoreDouble = curAniAvg?.let { it / 10.0 } ?: aniScoreDouble
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

                // Fast initial source info; full cross-server checking happens progressively in UI
                val initialAvail = listOf(
                    AvailableSourceInfo(
                        source = src,
                        matchedId = mangaId,
                        matchedTitle = curAniTitle ?: info.title,
                        chapterCount = chapters.size.takeIf { it > 0 },
                        latestChapter = chapters.lastOrNull()?.number ?: chapters.firstOrNull()?.number
                    )
                )

                val resolvedChapters = chapters
                val resolvedId = mangaId

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
                    score = info.score ?: curAniScoreDouble,
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
                    availableSources = initialAvail,
                    relations = curAniRelations
                )
                return@withContext Triple(resolvedId, enrichedInfo, resolvedChapters)
            }
        }

        // 3. Resolution via title or numeric AniList ID: quickly check default primary server first
        val searchTarget = aniTitle?.trim()
            ?.ifBlank { null }
            ?: titleFallback?.trim()?.ifBlank { null }
            ?: extractSlugTitle(mangaId)
            ?: mangaId.takeIf { !isNumericAniListId && it.length < 30 }

        if (!searchTarget.isNullOrBlank() && searchTarget.length >= 2) {
            val primarySource = _activeSource.value
            val primaryRepo = getRepoForSource(primarySource)
            val primaryResults = try { primaryRepo.search(searchTarget) } catch (_: Exception) { emptyList() }
            var hit = primaryResults.find { it.title.equals(searchTarget, ignoreCase = true) }
                ?: primaryResults.find { it.title.contains(searchTarget, ignoreCase = true) }
                ?: primaryResults.firstOrNull()
            var usedSource = primarySource
            var usedRepo = primaryRepo

            if (hit == null) {
                val fallbackSource = if (primarySource == MangaSource.WEEBCENTRAL) MangaSource.DEMONIC_SCANS else MangaSource.WEEBCENTRAL
                val fallbackRepo = getRepoForSource(fallbackSource)
                val fallbackResults = try { fallbackRepo.search(searchTarget) } catch (_: Exception) { emptyList() }
                val altHit = fallbackResults.find { it.title.equals(searchTarget, ignoreCase = true) }
                    ?: fallbackResults.find { it.title.contains(searchTarget, ignoreCase = true) }
                    ?: fallbackResults.firstOrNull()
                if (altHit != null) {
                    hit = altHit
                    usedSource = fallbackSource
                    usedRepo = fallbackRepo
                }
            }

            if (hit != null) {
                val primaryInfo = try { usedRepo.getInfo(hit.id) } catch (_: Exception) { null }
                val primaryChapters = try { usedRepo.getChapters(hit.id) } catch (_: Exception) { emptyList() }

                val initialAvail = listOf(
                    AvailableSourceInfo(
                        source = usedSource,
                        matchedId = hit.id,
                        matchedTitle = hit.title,
                        chapterCount = primaryChapters.size.takeIf { it > 0 },
                        latestChapter = primaryChapters.lastOrNull()?.number ?: primaryChapters.firstOrNull()?.number
                    )
                )

                val resolvedInfo = MangaInfo(
                    id = hit.id,
                    title = aniTitle ?: primaryInfo?.title ?: hit.title,
                    altTitle = aniRomajiTitle ?: primaryInfo?.altTitle,
                    poster = aniCover ?: primaryInfo?.poster ?: hit.poster,
                    banner = aniBanner ?: primaryInfo?.banner ?: aniCover ?: primaryInfo?.poster ?: hit.poster,
                    description = primaryInfo?.description?.takeIf { it.isNotBlank() } ?: (aniDesc ?: ""),
                    genres = primaryInfo?.genres?.takeIf { it.isNotEmpty() } ?: aniGenres,
                    status = primaryInfo?.status ?: aniStatus,
                    rating = primaryInfo?.rating ?: aniScore,
                    score = primaryInfo?.score ?: aniScoreDouble,
                    type = primaryInfo?.type ?: aniFormat ?: "Manga",
                    source = usedSource.id,
                    anilistId = aniId ?: aniData?.optInt("id")?.takeIf { it > 0 },
                    totalChapters = aniChapters ?: primaryInfo?.totalChapters,
                    authors = primaryInfo?.authors?.takeIf { it.isNotEmpty() } ?: aniAuthors,
                    characters = aniCharacters,
                    relatedAnimeId = relAnimeId,
                    relatedAnimeTitle = relAnimeTitle,
                    relatedAnimeNextAiringEpisode = relAnime?.nextAiringEpisode,
                    relatedAnimeNextAiringTime = relAnime?.nextAiringTime,
                    availableSources = initialAvail,
                    relations = aniRelations
                )
                return@withContext Triple(hit.id, resolvedInfo, primaryChapters)
            }
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
                score = aniScoreDouble,
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

object MangaTitleMatcher {
    fun compact(s: String): String {
        return s.lowercase()
            .replace(Regex("""\(.*?\)|\{.*?\}|\[.*?\]"""), " ")
            .replace("&", " and ")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
    }

    fun similarity(a: String, b: String): Float {
        val x = compact(a)
        val y = compact(b)
        if (x.isBlank() || y.isBlank()) return 0f
        if (x == y || x.replace(" ", "") == y.replace(" ", "")) return 1f
        val (short, long) = if (x.length <= y.length) x to y else y to x
        if (short.length >= 4 && " $long ".contains(" $short ")) {
            return (0.6f + 0.4f * (short.length.toFloat() / long.length.toFloat())).coerceIn(0f, 1f)
        }
        val ta = x.split(" ").filter { it.isNotBlank() }.toSet()
        val tb = y.split(" ").filter { it.isNotBlank() }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0f
        var shared = 0
        for (t in ta) {
            if (tb.contains(t)) shared++
        }
        val total = ta.size + tb.size - shared
        return if (total > 0) shared.toFloat() / total.toFloat() else 0f
    }

    fun bestSimilarity(candidate: String, names: List<String>): Float {
        if (names.isEmpty()) return 0f
        return names.maxOfOrNull { similarity(candidate, it) } ?: 0f
    }

    fun searchVariants(names: List<String>): List<String> {
        val out = mutableListOf<String>()
        val add = { s: String? ->
            val t = (s ?: "").trim()
            if (t.length >= 2 && out.none { it.equals(t, ignoreCase = true) }) {
                out.add(t)
            }
        }
        for (n in names) add(n)
        for (n in names) {
            val part = n.split(Regex("""\s*[:：\-–—]\s+""")).firstOrNull()
            add(part)
        }
        return out.take(6)
    }

    const val MATCH_AT = 0.70f
}
