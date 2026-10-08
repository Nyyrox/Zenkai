package com.nyyrox.aniverse.data.manga

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MangaRepository {
    private const val TAG = "MangaRepository"
    private val WORKER_ORIGIN: String
        get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.getWorkerUrl(
            "manga_worker_origin",
            "https://brhgxkmnoken-mud-a7f7nyroxx.ammhfoo.workers.dev"
        )
    private val PROXY_ORIGIN: String
        get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.getWorkerUrl(
            "manga_proxy_origin",
            "https://holy-union-7b91.mahfoooozzzzalm.workers.dev"
        )
    private const val JINA_ORIGIN = "https://r.jina.ai/http://weebcentral.com"
    private const val WEEBCENTRAL_BASE = "https://weebcentral.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val aniListRepo by lazy { com.nyyrox.aniverse.data.AniListRepository() }

    fun cleanWeebId(id: String): String {
        var s = id.trim()
        if (s.contains("?")) s = s.substringBefore("?")
        if (s.contains("#")) s = s.substringBefore("#")
        if (s.contains("/series/")) {
            val after = s.substringAfter("/series/")
            val seriesId = after.substringBefore("/").substringBefore("_")
            if (seriesId.isNotBlank()) return seriesId
        }
        s = s.removePrefix("wc_").removePrefix("md_")
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) {
            return ulidMatch.groupValues[1]
        }
        return s.split("_").firstOrNull()?.split("/")?.firstOrNull() ?: s
    }

    fun extractSlugTitle(id: String): String? {
        var s = id.trim()
        if (s.contains("?")) s = s.substringBefore("?")
        if (s.contains("#")) s = s.substringBefore("#")
        if (s.contains("/series/")) {
            val after = s.substringAfter("/series/")
            val parts = after.split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return parts[1].replace("-", " ").replace("_", " ").trim()
            }
        }
        if (s.startsWith("wc_") && s.contains("_")) {
            val parts = s.removePrefix("wc_").split("_")
            if (parts.size >= 2) {
                return parts.drop(1).joinToString(" ").replace("-", " ").trim()
            }
        }
        if (s.startsWith("md_")) {
            return s.removePrefix("md_").replace("-", " ").replace("_", " ").trim()
        }
        return null
    }

    fun cleanChapterId(chapterId: String): String {
        var s = chapterId.trim()
        if (s.contains("?")) s = s.substringBefore("?")
        if (s.contains("#")) s = s.substringBefore("#")
        if (s.contains("/chapters/")) {
            s = s.substringAfter("/chapters/").substringBefore("/")
        }
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) {
            return ulidMatch.groupValues[1]
        }
        return s.removePrefix("wc_chapter_").removePrefix("wc_").removePrefix("md_ch_").removePrefix("md_")
    }

    private fun proxyImageUrl(url: String): String {
        return url
    }

    private fun extractVolume(rawText: String, chapterNumber: String): String? {
        val volMatch = Regex("\\b(?:Volume|Vol\\.?)\\s*([\\d.]+)", RegexOption.IGNORE_CASE).find(rawText)
        val explicitVol = volMatch?.groupValues?.get(1)?.let { "Vol. $it" }
        return explicitVol ?: run {
            val n = chapterNumber.toFloatOrNull()?.toInt()
            if (n != null && n > 0) {
                val bucket = ((n - 1) / 25) + 1
                val start = (bucket - 1) * 25 + 1
                val end = bucket * 25
                "Vol. $bucket (Ch. $start-$end)"
            } else null
        }
    }

    // ── 1. HOME ─────────────────────────────────────────────────────────────
    suspend fun getHome(): MangaHome = withContext(Dispatchers.IO) {
        // Strategy A: Cloudflare Worker
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=home")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 Aniverse/1.0")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val trending = parseCardsArray(json.optJSONArray("trending"))
                        val latest = parseCardsArray(json.optJSONArray("latest"))
                        val newRelease = parseCardsArray(json.optJSONArray("newRelease"))

                        if (trending.isNotEmpty() || latest.isNotEmpty()) {
                            Log.d(TAG, "Loaded home from Worker successfully: ${trending.size} trending, ${latest.size} latest")
                            return@withContext MangaHome(
                                trending = trending,
                                latest = latest,
                                newRelease = if (newRelease.isNotEmpty()) newRelease else latest.take(20)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worker getHome failed, falling back to client-side: ${e.message}")
        }

        // Strategy B: Client-side WeebCentral HTML Scraper Fallback
        try {
            val html = fetchWeebHtml("/")
            val cards = parseSeriesCardsFromHtml(html)
            if (cards.isNotEmpty()) {
                val half = (cards.size / 2).coerceAtLeast(1)
                Log.d(TAG, "Loaded home from client-side scraping: ${cards.size} cards")
                return@withContext MangaHome(
                    trending = cards.take(half),
                    latest = cards.drop(half),
                    newRelease = cards.take(16)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getHome failed: ${e.message}")
        }

        // Strategy C: Jina Markdown Fallback
        try {
            val md = fetchWeebMarkdown("/")
            val cards = parseSeriesCardsFromMarkdown(md)
            if (cards.isNotEmpty()) {
                val half = (cards.size / 2).coerceAtLeast(1)
                return@withContext MangaHome(
                    trending = cards.take(half),
                    latest = cards.drop(half),
                    newRelease = cards.take(16)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Jina getHome failed: ${e.message}")
        }

        MangaHome()
    }

    // ── 2. SEARCH ───────────────────────────────────────────────────────────
    suspend fun search(query: String): List<MangaCard> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(q, "UTF-8")

        // Strategy A: Worker
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=search&q=$encodedQuery")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 Aniverse/1.0")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val results = parseCardsArray(json.optJSONArray("results"))
                        if (results.isNotEmpty()) {
                            return@withContext results
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worker search failed: ${e.message}")
        }

        // Strategy B: Client-side scrape
        try {
            val url = "/search/data?text=$encodedQuery&sort=Best+Match&order=Descending&official=Any&anime=Any&adult=Any&display_mode=Full+Display"
            val html = fetchWeebHtml(url)
            val cards = parseSeriesCardsFromHtml(html)
            if (cards.isNotEmpty()) return@withContext cards
        } catch (e: Exception) {
            Log.e(TAG, "Client-side search failed: ${e.message}")
        }

        // Strategy C: Jina markdown search
        try {
            val url = "/search/data?text=$encodedQuery&sort=Best+Match&order=Descending&official=Any&anime=Any&adult=Any&display_mode=Full+Display"
            val md = fetchWeebMarkdown(url)
            val cards = parseSeriesCardsFromMarkdown(md)
            if (cards.isNotEmpty()) return@withContext cards
        } catch (e: Exception) {
            Log.e(TAG, "Jina search failed: ${e.message}")
        }

        emptyList()
    }

    // ── 2.5 RESOLUTION ─────────────────────────────────────────────────────
    suspend fun resolveMangaDetails(
        mangaId: String,
        titleFallback: String? = null
    ): Triple<String, MangaInfo?, List<MangaChapter>> = withContext(Dispatchers.IO) {
        val directId = cleanWeebId(mangaId)
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

        // 2. Try direct WeebCentral fetch if NOT purely numeric AniList ID
        if (!isNumericAniListId) {
            val infoDeferred = async { getInfo(directId) }
            val chaptersDeferred = async { getChapters(directId) }
            val info = infoDeferred.await()
            val chapters = chaptersDeferred.await()

            if (info != null && chapters.isNotEmpty()) {
                val enrichedInfo = info.copy(
                    poster = info.poster ?: aniCover,
                    banner = aniBanner ?: info.banner,
                    description = if (info.description.isBlank()) (aniDesc ?: "") else info.description,
                    genres = if (info.genres.isEmpty()) aniGenres else info.genres,
                    rating = info.rating ?: aniScore,
                    status = info.status ?: aniStatus,
                    type = info.type ?: aniFormat ?: "Manga",
                    anilistId = aniId ?: aniData?.optInt("id")?.takeIf { it > 0 },
                    totalChapters = aniChapters ?: info.totalChapters,
                    authors = if (info.authors.isEmpty()) aniAuthors else info.authors
                )
                return@withContext Triple(directId, enrichedInfo, chapters)
            }
        }

        // 3. Fallback resolution: search WeebCentral using aniTitle, fallback title, slug title, or clean query
        val queryCandidate = aniTitle?.trim()
            ?.ifBlank { null }
            ?: titleFallback?.trim()?.ifBlank { null }
            ?: extractSlugTitle(mangaId)
            ?: directId.takeIf { !isNumericAniListId && it.length < 30 }

        if (!queryCandidate.isNullOrBlank() && queryCandidate.length >= 2) {
            Log.d(TAG, "Resolving manga '$mangaId' via search with query '$queryCandidate'...")
            val searchResults = try { search(queryCandidate) } catch (e: Exception) {
                Log.w(TAG, "Search resolution failed: ${e.message}")
                emptyList()
            }

            if (searchResults.isNotEmpty()) {
                val bestMatch = searchResults.find { it.title.equals(queryCandidate, ignoreCase = true) }
                    ?: searchResults.first()
                val newDirectId = cleanWeebId(bestMatch.id)

                val fallbackInfoDeferred = async {
                    val rawInfo = getInfo(newDirectId)
                    MangaInfo(
                        id = newDirectId,
                        title = aniTitle ?: rawInfo?.title ?: bestMatch.title,
                        poster = rawInfo?.poster ?: bestMatch.poster ?: aniCover,
                        banner = aniBanner ?: rawInfo?.banner,
                        description = rawInfo?.description?.takeIf { it.isNotBlank() } ?: (aniDesc ?: ""),
                        genres = rawInfo?.genres?.takeIf { it.isNotEmpty() } ?: aniGenres,
                        status = rawInfo?.status ?: aniStatus,
                        rating = rawInfo?.rating ?: aniScore,
                        type = rawInfo?.type ?: aniFormat ?: bestMatch.type ?: "Manga",
                        source = "weebcentral",
                        anilistId = aniId ?: aniData?.optInt("id")?.takeIf { it > 0 },
                        totalChapters = aniChapters ?: rawInfo?.totalChapters,
                        authors = rawInfo?.authors?.takeIf { it.isNotEmpty() } ?: aniAuthors
                    )
                }
                val fallbackChaptersDeferred = async { getChapters(newDirectId) }

                return@withContext Triple(newDirectId, fallbackInfoDeferred.await(), fallbackChaptersDeferred.await())
            }
        }

        val fallbackInfo = if (aniData != null && !aniTitle.isNullOrBlank()) {
            val fallbackAniId: Int? = aniId ?: aniData.optInt("id").takeIf { it > 0 }
            MangaInfo(
                id = directId,
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

        Triple(directId, fallbackInfo, emptyList())
    }

    // ── 3. INFO ─────────────────────────────────────────────────────────────
    suspend fun getInfo(mangaId: String): MangaInfo? = withContext(Dispatchers.IO) {
        val seriesId = cleanWeebId(mangaId)
        if (seriesId.isBlank()) return@withContext null

        // Strategy A: Worker
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=info&id=$seriesId")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val infoObj = json.optJSONObject("info")
                        if (infoObj != null) {
                            val genres = buildList {
                                infoObj.optJSONArray("genres")?.let { arr ->
                                    for (i in 0 until arr.length()) add(arr.optString(i))
                                }
                            }
                            return@withContext MangaInfo(
                                id = seriesId,
                                title = infoObj.optString("title", "Unknown"),
                                altTitle = infoObj.optString("altTitle").takeIf { it.isNotBlank() },
                                poster = infoObj.optString("poster").takeIf { it.isNotBlank() },
                                description = infoObj.optString("description", ""),
                                type = infoObj.optString("type", "Manga"),
                                status = infoObj.optString("status").takeIf { it.isNotBlank() },
                                rating = infoObj.optString("rating").takeIf { it.isNotBlank() },
                                genres = genres,
                                source = "weebcentral"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worker getInfo failed: ${e.message}")
        }

        // Strategy B: Client-side scrape series HTML
        try {
            val html = fetchWeebHtml("/series/$seriesId")
            val titleRegex = Regex("<h1[^>]*>([\\s\\S]*?)</h1>", RegexOption.IGNORE_CASE)
            val title = titleRegex.find(html)?.groupValues?.get(1)?.let { cleanHtmlText(it) } ?: "Unknown"

            val posterRegex = Regex("<img[^>]+src=[\"'](https://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"'][^>]*>", RegexOption.IGNORE_CASE)
            val poster = posterRegex.find(html)?.groupValues?.get(1)

            val descRegex = Regex("<(?:p|div)[^>]*class=[\"'][^\"']*(?:description|synopsis)[^\"']*[\"'][^>]*>([\\s\\S]*?)</(?:p|div)>", RegexOption.IGNORE_CASE)
            val description = descRegex.find(html)?.groupValues?.get(1)?.let { cleanHtmlText(it) }.orEmpty()

            val status = extractBetween(html, "<strong>Status:</strong>", "</")
            val type = extractBetween(html, "<strong>Type:</strong>", "</").ifBlank { "Manga" }

            val genres = mutableListOf<String>()
            val genreRegex = Regex("<div[^>]+class=[\"'][^\"']*opacity-70[^\"']*[\"'][^>]*>([\\s\\S]*?)</div>", RegexOption.IGNORE_CASE)
            genreRegex.findAll(html).forEach { match ->
                val g = cleanHtmlText(match.groupValues[1])
                if (g.isNotBlank() && g.length < 30 && !g.contains('\n')) genres.add(g)
            }

            return@withContext MangaInfo(
                id = seriesId,
                title = title,
                poster = poster,
                description = description,
                type = type,
                status = status.ifBlank { null },
                genres = genres.distinct(),
                source = "weebcentral"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getInfo failed: ${e.message}")
        }

        null
    }

    // ── 4. CHAPTERS ─────────────────────────────────────────────────────────
    suspend fun getChapters(mangaId: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val seriesId = cleanWeebId(mangaId)
        if (seriesId.isBlank()) return@withContext emptyList()

        // Strategy A: Worker
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=chapters&id=$seriesId")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val arr = JSONArray(body)
                        val chapters = mutableListOf<MangaChapter>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val rawChapId = obj.optString("chapterId")
                            val cleanChId = cleanChapterId(rawChapId)
                            val numStr = obj.optString("number", "${i + 1}")
                            val titleStr = obj.optString("title").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            val rawName = obj.optString("name").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            val rawForVol = listOfNotNull(titleStr, numStr, rawName).joinToString(" ")
                            val volume = extractVolume(rawForVol, numStr)?.takeIf { !it.equals("null", ignoreCase = true) }
                            chapters.add(
                                MangaChapter(
                                    number = numStr,
                                    title = titleStr,
                                    volume = volume,
                                    chapterId = cleanChId,
                                    releaseDate = obj.optString("releaseDate").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                                    url = "$WEEBCENTRAL_BASE/chapters/$cleanChId",
                                    source = "weebcentral"
                                )
                            )
                        }
                        if (chapters.isNotEmpty()) {
                            return@withContext chapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worker getChapters failed: ${e.message}")
        }

        // Strategy B: Client-side scrape /series/{id}/full-chapter-list
        try {
            val html = fetchWeebHtml("/series/$seriesId/full-chapter-list")
            val chapters = parseChaptersFromHtml(html)
            if (chapters.isNotEmpty()) {
                return@withContext chapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getChapters full-chapter-list failed: ${e.message}")
        }

        // Strategy C: Series page chapter list
        try {
            val html = fetchWeebHtml("/series/$seriesId")
            val chapters = parseChaptersFromHtml(html)
            if (chapters.isNotEmpty()) {
                return@withContext chapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getChapters series page failed: ${e.message}")
        }

        emptyList()
    }

    // ── 5. PAGES ────────────────────────────────────────────────────────────
    suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val cleanId = cleanChapterId(chapterId)
        if (cleanId.isBlank()) return@withContext emptyList()

        // Strategy A: Worker (Primary, fast)
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=pages&chapterId=$cleanId")
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val pages = parsePagesFromBody(body)
                        if (pages.isNotEmpty()) {
                            Log.d(TAG, "Loaded ${pages.size} pages from Worker for $cleanId")
                            return@withContext pages
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Worker getPages failed: ${e.message}")
        }

        // Strategy B: Client-side WeebCentral HTMX endpoint
        try {
            val target = "/chapters/$cleanId/images?is_prev=False&current_page=0&reading_style=long_strip"
            val html = fetchWeebHtml(target, headers = mapOf("HX-Request" to "true", "X-Requested-With" to "XMLHttpRequest"))
            val pages = parsePageImagesFromHtml(html)
            if (pages.isNotEmpty()) {
                Log.d(TAG, "Loaded ${pages.size} pages from client-side HTMX for $cleanId")
                return@withContext pages
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getPages HTMX failed: ${e.message}")
        }

        // Strategy C: Chapter main page scrape
        try {
            val html = fetchWeebHtml("/chapters/$cleanId")
            val pages = parsePageImagesFromHtml(html)
            if (pages.isNotEmpty()) {
                Log.d(TAG, "Loaded ${pages.size} pages from chapter page scrape for $cleanId")
                return@withContext pages
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client-side getPages chapter page failed: ${e.message}")
        }

        // Strategy D: Jina markdown fallback
        try {
            val md = fetchWeebMarkdown("/chapters/$cleanId")
            val pages = parsePageImagesFromHtml(md)
            if (pages.isNotEmpty()) {
                return@withContext pages
            }
        } catch (e: Exception) {
            Log.e(TAG, "Jina getPages failed: ${e.message}")
        }

        emptyList()
    }

    private fun parsePagesFromBody(body: String): List<String> {
        val trimmed = body.trim()
        val pages = mutableListOf<String>()

        // 1. JSON Array of string URLs or Objects with url/src/image
        if (trimmed.startsWith("[")) {
            try {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    if (item is String && item.startsWith("http")) {
                        pages.add(item)
                    } else if (item is JSONObject) {
                        val url = item.optString("url", item.optString("src", item.optString("image", "")))
                        if (url.startsWith("http")) pages.add(url)
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. JSON Object with key pages / images / data / urls / result / items
        if (trimmed.startsWith("{")) {
            try {
                val obj = JSONObject(trimmed)
                val listKeys = listOf("pages", "images", "data", "urls", "result", "items")
                for (k in listKeys) {
                    val arr = obj.optJSONArray(k)
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.opt(i)
                            if (item is String && item.startsWith("http")) {
                                pages.add(item)
                            } else if (item is JSONObject) {
                                val url = item.optString("url", item.optString("src", item.optString("image", "")))
                                if (url.startsWith("http")) pages.add(url)
                            }
                        }
                        if (pages.isNotEmpty()) break
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Regex extraction of image URLs from raw text
        if (pages.isEmpty()) {
            val regex = Regex("https?://[^\"'\\s<>]+\\.(?:jpg|jpeg|png|webp|avif)(?:\\?[^\"'\\s<>]*)?", RegexOption.IGNORE_CASE)
            regex.findAll(body).forEach { match ->
                val u = match.value
                if (!u.contains("/static/images/") && !u.contains("logo", ignoreCase = true) && !u.contains("avatar", ignoreCase = true)) {
                    pages.add(u)
                }
            }
        }

        return pages.filter { it.startsWith("http") }.distinct()
    }

    // ── HELPER NETWORK CALLS ────────────────────────────────────────────────
    private fun fetchWeebHtml(path: String, headers: Map<String, String> = emptyMap()): String {
        val target = "$WEEBCENTRAL_BASE$path"
        val proxyUrl = "$PROXY_ORIGIN/?url=${URLEncoder.encode(target, "UTF-8")}"

        val reqBuilder = Request.Builder()
            .url(proxyUrl)
            .header("Accept", "text/html, */*")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) Aniverse/1.0")

        headers.forEach { (k, v) -> reqBuilder.header(k, v) }

        try {
            client.newCall(reqBuilder.build()).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) return body
                }
            }
        } catch (_: Exception) {}

        // Fallback: Direct Request with WeebCentral headers
        try {
            val directReqBuilder = Request.Builder()
                .url(target)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://weebcentral.com/")
                .header("Accept", "text/html, */*")
            headers.forEach { (k, v) -> directReqBuilder.header(k, v) }
            client.newCall(directReqBuilder.build()).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) return body
                }
            }
        } catch (_: Exception) {}

        // Fallback: Local APK StreamProxyServer
        try {
            val localProxyUrl = com.nyyrox.aniverse.player.StreamProxyServer.buildProxyUrl(
                target,
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
                    "Referer" to "https://weebcentral.com/"
                )
            )
            val localReq = Request.Builder()
                .url(localProxyUrl)
                .header("Accept", "text/html, */*")
                .build()
            client.newCall(localReq).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) return body
                }
            }
        } catch (_: Exception) {}

        // Fallback directly or through Jina
        val jinaUrl = "$JINA_ORIGIN$path"
        val jinaReq = Request.Builder()
            .url(jinaUrl)
            .header("Accept", "text/plain, */*")
            .build()

        client.newCall(jinaReq).execute().use { res ->
            if (res.isSuccessful) return res.body?.string().orEmpty()
        }

        return ""
    }

    private fun fetchWeebMarkdown(path: String): String {
        val jinaUrl = "$JINA_ORIGIN$path"
        val jinaReq = Request.Builder()
            .url(jinaUrl)
            .header("Accept", "text/plain, text/markdown, */*")
            .build()

        client.newCall(jinaReq).execute().use { res ->
            if (res.isSuccessful) return res.body?.string().orEmpty()
        }
        return ""
    }

    // ── PARSERS ─────────────────────────────────────────────────────────────
    private fun parseCardsArray(arr: JSONArray?): List<MangaCard> {
        if (arr == null) return emptyList()
        val cards = mutableListOf<MangaCard>()
        val seen = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val rawId = obj.optString("id")
            val title = cleanHtmlText(obj.optString("title"))
            if (rawId.isBlank() || title.isBlank() || seen.contains(rawId)) continue
            seen.add(rawId)

            val id = if (rawId.startsWith("wc_")) rawId else "wc_$rawId"
            cards.add(
                MangaCard(
                    id = id,
                    title = title,
                    poster = obj.optString("poster").takeIf { it.isNotBlank() },
                    type = obj.optString("type", "Manga"),
                    chapter = obj.optString("chapter").takeIf { it.isNotBlank() },
                    updated = obj.optString("updated").takeIf { it.isNotBlank() },
                    source = "weebcentral"
                )
            )
        }
        return cards
    }

    private fun parseSeriesCardsFromHtml(html: String): List<MangaCard> {
        val cards = mutableListOf<MangaCard>()
        val seen = mutableSetOf<String>()

        val anchorRegex = Regex("<a\\b[^>]*href=[\"'](?:https://weebcentral\\.com)?(/series/([A-Z0-9]+)/([^\"']+))[\"'][^>]*>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE)
        anchorRegex.findAll(html).forEach { match ->
            val seriesId = match.groupValues[2]
            val slug = match.groupValues[3]
            val inner = match.groupValues[4]

            if (seriesId.isNotBlank() && !seen.contains(seriesId)) {
                val posterRegex = Regex("<img[^>]+src=[\"'](https://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"']", RegexOption.IGNORE_CASE)
                val poster = posterRegex.find(inner)?.groupValues?.get(1)

                val altRegex = Regex("<img[^>]+alt=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                val rawTitle = altRegex.find(inner)?.groupValues?.get(1)?.ifBlank { null } ?: cleanHtmlText(inner)
                val title = rawTitle.replace(Regex("\\s+cover$", RegexOption.IGNORE_CASE), "").trim()

                if (title.isNotBlank()) {
                    seen.add(seriesId)
                    cards.add(
                        MangaCard(
                            id = "wc_${seriesId}_$slug",
                            title = title,
                            poster = poster,
                            type = "Manga",
                            chapter = Regex("\\b(?:Chapter|Ch\\.?|Episode)\\s*([\\d.]+)", RegexOption.IGNORE_CASE).find(inner)?.groupValues?.get(1),
                            updated = Regex("\\b(\\d+\\s*(?:m|h|d|w|mo|y)\\s*ago)\\b", RegexOption.IGNORE_CASE).find(inner)?.groupValues?.get(1),
                            source = "weebcentral"
                        )
                    )
                }
            }
        }
        return cards
    }

    private fun parseSeriesCardsFromMarkdown(md: String): List<MangaCard> {
        val cards = mutableListOf<MangaCard>()
        val seen = mutableSetOf<String>()

        val regex = Regex("\\[!\\[Image\\s+\\d+:\\s*([^\\]]+?)\\s+cover\\]\\((https?://[^)]+)\\)([\\s\\S]*?)\\]\\(https?://weebcentral\\.com/series/([^/\\s)]+)/([^)#?\\s]+)[^)]*\\)", RegexOption.IGNORE_CASE)
        regex.findAll(md).forEach { match ->
            val title = cleanHtmlText(match.groupValues[1])
            val poster = match.groupValues[2]
            val seriesId = match.groupValues[4]
            val slug = match.groupValues[5]

            if (seriesId.isNotBlank() && title.isNotBlank() && !seen.contains(seriesId)) {
                seen.add(seriesId)
                cards.add(
                    MangaCard(
                        id = "wc_${seriesId}_$slug",
                        title = title,
                        poster = poster,
                        type = "Manga",
                        source = "weebcentral"
                    )
                )
            }
        }
        return cards
    }

    private fun parseChaptersFromHtml(html: String): List<MangaChapter> {
        val chapters = mutableListOf<MangaChapter>()
        val seen = mutableSetOf<String>()

        val regex = Regex("<a[^>]+href=[\"'](?:https://weebcentral\\.com)?/chapters/([A-Z0-9]+)[\"'][^>]*>([\\s\\S]*?)</a>", RegexOption.IGNORE_CASE)
        regex.findAll(html).forEach { match ->
            val chapterId = match.groupValues[1]
            val inner = match.groupValues[2]
            if (chapterId.isNotBlank() && !seen.contains(chapterId)) {
                seen.add(chapterId)
                val text = cleanHtmlText(inner)
                if (text.isNotBlank() && !text.contains("bookmark", ignoreCase = true) && !text.contains("login", ignoreCase = true)) {
                    val numMatch = Regex("\\b(?:Chapter|Ch\\.?|Episode|Ep\\.?)\\s*([\\d.]+)", RegexOption.IGNORE_CASE).find(text)
                    val number = numMatch?.groupValues?.get(1) ?: Regex("\\b(\\d+(?:\\.\\d+)?)\\b").find(text)?.groupValues?.get(1) ?: "${chapters.size + 1}"

                    val title = text
                        .replace(Regex("\\b(?:Chapter|Ch\\.?|Episode|Ep\\.?)\\s*[\\d.]+", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("^[-:.\\s]+"), "")
                        .trim()
                        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

                    val dateMatch = Regex("(\\d{4}-\\d{2}-\\d{2})|(\\w+ \\d{1,2}, \\d{4})").find(inner)
                    val volume = extractVolume(text, number)

                    chapters.add(
                        MangaChapter(
                            number = number,
                            title = title,
                            volume = volume,
                            chapterId = cleanChapterId(chapterId),
                            releaseDate = dateMatch?.value,
                            url = "$WEEBCENTRAL_BASE/chapters/${cleanChapterId(chapterId)}",
                            source = "weebcentral"
                        )
                    )
                }
            }
        }
        return chapters
    }

    private fun parsePageImagesFromHtml(html: String): List<String> {
        val pages = mutableListOf<String>()
        val imgRegex = Regex("<img[^>]+(?:src|data-src)=[\"'](https?://[^\"']+)[\"']", RegexOption.IGNORE_CASE)
        imgRegex.findAll(html).forEach { match ->
            val url = match.groupValues[1]
            if (!url.contains("/static/images/") && !url.contains("logo", ignoreCase = true)) {
                pages.add(url)
            }
        }
        val directRegex = Regex("https://[^\"'\\s<>]+\\.(?:jpg|jpeg|png|webp)(?:\\?[^\"'\\s<>]*)?", RegexOption.IGNORE_CASE)
        directRegex.findAll(html).forEach { match ->
            val url = match.value
            if (!url.contains("/static/images/") && !url.contains("logo", ignoreCase = true)) {
                pages.add(url)
            }
        }
        return pages.distinct()
    }

    private fun cleanHtmlText(text: String): String {
        return text
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractBetween(source: String, start: String, end: String): String {
        val s = source.indexOf(start)
        if (s == -1) return ""
        val e = source.indexOf(end, s + start.length)
        return if (e == -1) "" else cleanHtmlText(source.substring(s + start.length, e))
    }
}
