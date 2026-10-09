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
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// ============================================================================
// 1. MANGADEX REPOSITORY (Official REST API - Multilingual Manga & Manhwa)
// ============================================================================
class MangaDexRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : IMangaSourceRepository {
    override val source: MangaSource = MangaSource.MANGADEX

    private val apiBase = "https://api.mangadex.org"
    private val uploadsBase = "https://uploads.mangadex.org"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private suspend fun fetchJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) JSONObject(body) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w("MangaDexRepo", "Failed to fetch $url: ${e.message}")
            null
        }
    }

    private fun extractCover(item: JSONObject, highRes: Boolean = false): String? {
        val id = item.optString("id")
        val rels = item.optJSONArray("relationships") ?: return null
        for (i in 0 until rels.length()) {
            val r = rels.optJSONObject(i) ?: continue
            if (r.optString("type") == "cover_art") {
                val fileName = r.optJSONObject("attributes")?.optString("fileName").orEmpty()
                if (fileName.isNotBlank()) {
                    return if (highRes) "$uploadsBase/covers/$id/$fileName.512.jpg?v=2" else "$uploadsBase/covers/$id/$fileName.256.jpg?v=2"
                }
            }
        }
        return null
    }

    private fun extractTitle(attributes: JSONObject?): String {
        if (attributes == null) return "Unknown"
        val titleObj = attributes.optJSONObject("title")
        val en = titleObj?.optString("en")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        if (en != null) return en

        val altTitles = attributes.optJSONArray("altTitles")
        if (altTitles != null) {
            for (i in 0 until altTitles.length()) {
                val alt = altTitles.optJSONObject(i) ?: continue
                val altEn = alt.optString("en").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                if (altEn != null) return altEn
            }
        }

        if (titleObj != null) {
            val keys = titleObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = titleObj.optString(k)
                if (v.isNotBlank() && !v.equals("null", ignoreCase = true)) return v
            }
        }
        return "Unknown"
    }

    private fun parseCards(arr: JSONArray?): List<MangaCard> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MangaCard>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val attrs = item.optJSONObject("attributes")
            val title = extractTitle(attrs)
            val cover = extractCover(item)
            val type = attrs?.optString("originalLanguage")?.let {
                when (it) {
                    "ko" -> "Manhwa"
                    "zh" -> "Manhua"
                    else -> "Manga"
                }
            } ?: "Manga"

            list.add(
                MangaCard(
                    id = "md_$id",
                    title = title,
                    poster = cover,
                    type = type,
                    chapter = null,
                    updated = attrs?.optString("updatedAt")?.take(10),
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(): MangaHome = coroutineScope {
        val trendingDeferred = async {
            val json = fetchJson("$apiBase/manga?limit=20&includes[]=cover_art&order[followedCount]=desc&contentRating[]=safe&contentRating[]=suggestive")
            parseCards(json?.optJSONArray("data"))
        }
        val latestDeferred = async {
            val json = fetchJson("$apiBase/manga?limit=20&includes[]=cover_art&order[latestUploadedChapter]=desc&contentRating[]=safe&contentRating[]=suggestive")
            parseCards(json?.optJSONArray("data"))
        }
        val newDeferred = async {
            val json = fetchJson("$apiBase/manga?limit=20&includes[]=cover_art&order[createdAt]=desc&contentRating[]=safe&contentRating[]=suggestive")
            parseCards(json?.optJSONArray("data"))
        }

        MangaHome(
            trending = trendingDeferred.await(),
            latest = latestDeferred.await(),
            newRelease = newDeferred.await()
        )
    }

    override suspend fun search(query: String): List<MangaCard> {
        val enc = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiBase/manga?title=$enc&limit=20&includes[]=cover_art&contentRating[]=safe&contentRating[]=suggestive&order[relevance]=desc"
        val json = fetchJson(url)
        return parseCards(json?.optJSONArray("data"))
    }

    override suspend fun getInfo(mangaId: String): MangaInfo? {
        val cleanId = mangaId.removePrefix("md_")
        val url = "$apiBase/manga/$cleanId?includes[]=cover_art&includes[]=author&includes[]=artist"
        val json = fetchJson(url) ?: return null
        val data = json.optJSONObject("data") ?: return null
        val attrs = data.optJSONObject("attributes") ?: return null

        val title = extractTitle(attrs)
        val cover = extractCover(data, highRes = true)
        val descObj = attrs.optJSONObject("description")
        val desc = descObj?.optString("en") ?: descObj?.optString("ko") ?: descObj?.optString("ja") ?: ""

        val genres = mutableListOf<String>()
        val tags = attrs.optJSONArray("tags")
        if (tags != null) {
            for (i in 0 until tags.length()) {
                val t = tags.optJSONObject(i)?.optJSONObject("attributes")?.optJSONObject("name")?.optString("en")
                if (!t.isNullOrBlank()) genres.add(t)
            }
        }

        val authors = mutableListOf<String>()
        val rels = data.optJSONArray("relationships")
        if (rels != null) {
            for (i in 0 until rels.length()) {
                val r = rels.optJSONObject(i) ?: continue
                if (r.optString("type") == "author" || r.optString("type") == "artist") {
                    val name = r.optJSONObject("attributes")?.optString("name")
                    if (!name.isNullOrBlank() && !authors.contains(name)) authors.add(name)
                }
            }
        }

        return MangaInfo(
            id = "md_$cleanId",
            title = title,
            poster = cover,
            description = desc,
            type = if (attrs.optString("originalLanguage") == "ko") "Manhwa" else "Manga",
            status = attrs.optString("status"),
            genres = genres,
            authors = authors,
            source = source.id
        )
    }

    override suspend fun getChapters(mangaId: String): List<MangaChapter> = coroutineScope {
        val cleanId = mangaId.removePrefix("md_")
        val limit = 500
        val baseFeed = "$apiBase/manga/$cleanId/feed?contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&order[chapter]=asc&limit=$limit"

        // Try English first (en and en-us)
        var firstJson = fetchJson("$baseFeed&translatedLanguage[]=en&translatedLanguage[]=en-us&offset=0")
        var isEn = true
        var total = firstJson?.optInt("total", 0) ?: 0
        var allData = firstJson?.optJSONArray("data")

        if (allData == null || allData.length() == 0) {
            firstJson = fetchJson("$baseFeed&offset=0")
            isEn = false
            total = firstJson?.optInt("total", 0) ?: 0
            allData = firstJson?.optJSONArray("data")
        }

        if (allData == null || allData.length() == 0) return@coroutineScope emptyList()

        val rawItems = mutableListOf<JSONObject>()
        for (i in 0 until allData.length()) {
            allData.optJSONObject(i)?.let { rawItems.add(it) }
        }

        // Fetch remaining pages concurrently if total > limit (up to 2500 chapters)
        if (total > limit) {
            val offsets = generateSequence(limit) { it + limit }
                .takeWhile { it < total && it < 2500 }
                .toList()

            val deferredPages = offsets.map { off ->
                async {
                    val langParam = if (isEn) "&translatedLanguage[]=en&translatedLanguage[]=en-us" else ""
                    val pageJson = fetchJson("$baseFeed$langParam&offset=$off")
                    pageJson?.optJSONArray("data")
                }
            }

            for (def in deferredPages) {
                val pageData = def.await() ?: continue
                for (i in 0 until pageData.length()) {
                    pageData.optJSONObject(i)?.let { rawItems.add(it) }
                }
            }
        }

        val chapters = mutableListOf<MangaChapter>()
        for (i in 0 until rawItems.size) {
            val item = rawItems[i]
            val chId = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: continue

            // Filter out external chapters and chapters with 0 pages
            val externalUrl = attrs.optString("externalUrl").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val pages = attrs.optInt("pages", 0)
            if (externalUrl != null || (attrs.has("pages") && pages == 0)) continue

            val chNum = attrs.optString("chapter").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "${i + 1}"
            val chTitle = attrs.optString("title").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val vol = attrs.optString("volume").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { "Vol. $it" }
            val publish = attrs.optString("publishAt").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            chapters.add(
                MangaChapter(
                    number = chNum,
                    title = chTitle,
                    volume = vol,
                    chapterId = "md_$chId",
                    releaseDate = publish?.take(10),
                    url = "https://mangadex.org/chapter/$chId",
                    source = source.id
                )
            )
        }

        // Deduplicate: If multiple groups translated the same chapter number, keep 1 distinct entry
        val distinctChapters = chapters.distinctBy { it.number }

        // Sort ascending by chapter number so Chapter 1 comes first
        distinctChapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
    }

    override suspend fun getPages(chapterId: String): List<String> {
        val cleanId = chapterId.removePrefix("md_ch_").removePrefix("md_")
        val url = "$apiBase/at-home/server/$cleanId"
        val json = fetchJson(url) ?: return emptyList()
        val baseUrl = json.optString("baseUrl")
        val chObj = json.optJSONObject("chapter") ?: return emptyList()
        val hash = chObj.optString("hash")
        val dataArr = chObj.optJSONArray("data") ?: return emptyList()

        val pages = mutableListOf<String>()
        for (i in 0 until dataArr.length()) {
            val file = dataArr.optString(i)
            if (file.isNotBlank()) {
                pages.add("$baseUrl/data/$hash/$file")
            }
        }
        return pages
    }
}

// ============================================================================
// 2. COMICK REPOSITORY (ComicK.art - Manhwa, Manga, Manhua)
// ============================================================================
class ComicKRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : IMangaSourceRepository {
    override val source: MangaSource = MangaSource.COMICK

    private val baseUrl = "https://comick.art"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private suspend fun fetchJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "$baseUrl/")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) JSONObject(body) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w("ComicKRepo", "Failed to fetch $url: ${e.message}")
            null
        }
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "$baseUrl/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
        } catch (e: Exception) {
            Log.w("ComicKRepo", "Failed to fetch HTML $url: ${e.message}")
            ""
        }
    }

    private fun resolveComicKCover(obj: JSONObject): String? {
        val defThumb = obj.optString("default_thumbnail").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: obj.optJSONObject("md_comics")?.optString("default_thumbnail")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        if (defThumb != null) return defThumb

        val coverUrl = obj.optString("cover_url").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        if (coverUrl != null) return coverUrl

        val thumb = obj.optString("thumbnail").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        if (thumb != null) return thumb

        val b2key = obj.optJSONObject("md_covers")?.optString("b2key")
            ?: obj.optJSONArray("md_covers")?.optJSONObject(0)?.optString("b2key")
        if (!b2key.isNullOrBlank()) {
            return "https://meo.comick.pictures/$b2key"
        }
        val slug = obj.optString("slug").takeIf { it.isNotBlank() }
        if (slug != null) {
            return "https://cdn1.comicknew.pictures/$slug/covers/101b409e.webp"
        }
        return null
    }

    private fun parseComics(arr: JSONArray?): List<MangaCard> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MangaCard>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val comic = item.optJSONObject("md_comics") ?: item
            val slug = comic.optString("slug").ifBlank { comic.optString("hid") }
            val title = comic.optString("title")
            if (slug.isBlank() || title.isBlank()) continue

            val cover = resolveComicKCover(comic)
            val country = comic.optString("country")
            val type = when (country) {
                "ko" -> "Manhwa"
                "cn" -> "Manhua"
                else -> "Manga"
            }

            list.add(
                MangaCard(
                    id = "ck_$slug",
                    title = title,
                    poster = cover,
                    type = type,
                    chapter = comic.optString("last_chapter").takeIf { it.isNotBlank() },
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(): MangaHome = coroutineScope {
        val trendingDeferred = async {
            val json = fetchJson("$baseUrl/api/search?order_by=user_follow_count&order_direction=desc&type=comic&limit=24")
            parseComics(json?.optJSONArray("data"))
        }
        val latestDeferred = async {
            val json = fetchJson("$baseUrl/api/chapters/latest?order=new&page=1")
            parseComics(json?.optJSONArray("data"))
        }
        val newDeferred = async {
            val json = fetchJson("$baseUrl/api/search?order_by=created_at&order_direction=desc&type=comic&limit=24")
            parseComics(json?.optJSONArray("data"))
        }

        MangaHome(
            trending = trendingDeferred.await(),
            latest = latestDeferred.await(),
            newRelease = newDeferred.await()
        )
    }

    override suspend fun search(query: String): List<MangaCard> {
        val enc = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/api/search?q=$enc&type=comic&showAll=false&exclude_mylist=false"
        val json = fetchJson(url)
        return parseComics(json?.optJSONArray("data"))
    }

    override suspend fun getInfo(mangaId: String): MangaInfo? {
        val slug = mangaId.removePrefix("ck_")
        val pageUrl = "$baseUrl/comic/$slug"
        val html = fetchHtml(pageUrl)

        // Strategy A: parse comic-data script
        if (html.isNotBlank()) {
            val m = Regex("""<script[^>]*id=["']comic-data["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL).find(html)
            if (m != null) {
                try {
                    val data = JSONObject(m.groupValues[1])
                    val title = data.optString("title")
                    val desc = data.optString("desc").replace(Regex("<[^>]*>"), " ").trim()
                    val poster = resolveComicKCover(data)
                    val country = data.optString("country")
                    val type = when (country) {
                        "ko" -> "Manhwa"
                        "cn" -> "Manhua"
                        else -> "Manga"
                    }
                    val genres = mutableListOf<String>()
                    val gArr = data.optJSONArray("genres")
                    if (gArr != null) {
                        for (i in 0 until gArr.length()) {
                            val name = gArr.optJSONObject(i)?.optJSONObject("genres")?.optString("name")
                            if (!name.isNullOrBlank()) genres.add(name)
                        }
                    }
                    return MangaInfo(
                        id = "ck_$slug",
                        title = title,
                        poster = poster,
                        description = desc,
                        type = type,
                        genres = genres,
                        source = source.id
                    )
                } catch (_: Exception) {}
            }
        }

        // Strategy B: search API fallback
        val searchResults = search(slug)
        val match = searchResults.find { it.id == "ck_$slug" } ?: searchResults.firstOrNull()
        if (match != null) {
            return MangaInfo(
                id = match.id,
                title = match.title,
                poster = match.poster,
                type = match.type,
                source = source.id
            )
        }

        return null
    }

    override suspend fun getChapters(mangaId: String): List<MangaChapter> = coroutineScope {
        val slug = mangaId.removePrefix("ck_")
        val url = "$baseUrl/api/comics/$slug/chapter-list?lang=en&page=1"
        var firstJson = fetchJson(url)
        var isEn = true
        var dataArr = firstJson?.optJSONArray("data")

        if (dataArr == null || dataArr.length() == 0) {
            val allUrl = "$baseUrl/api/comics/$slug/chapter-list?page=1"
            firstJson = fetchJson(allUrl)
            isEn = false
            dataArr = firstJson?.optJSONArray("data")
        }

        if (dataArr == null || dataArr.length() == 0) return@coroutineScope emptyList()

        val allItems = mutableListOf<JSONObject>()
        for (i in 0 until dataArr.length()) {
            dataArr.optJSONObject(i)?.let { allItems.add(it) }
        }

        val pagination = firstJson?.optJSONObject("pagination")
        val lastPage = pagination?.optInt("last_page", 1) ?: 1

        // Concurrently fetch remaining pages (up to 15 pages = 900 chapters)
        if (lastPage > 1) {
            val pagesToFetch = (2..minOf(lastPage, 15)).toList()
            val deferredPages = pagesToFetch.map { p ->
                async {
                    val pUrl = if (isEn) "$baseUrl/api/comics/$slug/chapter-list?lang=en&page=$p"
                    else "$baseUrl/api/comics/$slug/chapter-list?page=$p"
                    val pageJson = fetchJson(pUrl)
                    pageJson?.optJSONArray("data")
                }
            }

            for (def in deferredPages) {
                val pageData = def.await() ?: continue
                for (i in 0 until pageData.length()) {
                    pageData.optJSONObject(i)?.let { allItems.add(it) }
                }
            }
        }

        val chapters = mutableListOf<MangaChapter>()
        for (i in 0 until allItems.size) {
            val item = allItems[i]
            val hid = item.optString("hid")
            val chap = item.optString("chap").ifBlank { "${i + 1}" }
            val lang = item.optString("lang", "en")
            val vol = item.optString("vol").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.let { "Vol. $it" }
            val title = item.optString("title").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val date = item.optString("created_at")

            chapters.add(
                MangaChapter(
                    number = chap,
                    title = title,
                    volume = vol,
                    chapterId = "ck_${slug}__${hid}__${chap}__${lang}",
                    releaseDate = date.take(10),
                    url = "$baseUrl/comic/$slug/$hid-chapter-$chap-$lang",
                    source = source.id
                )
            )
        }

        // Deduplicate: If multiple scanlation groups uploaded the same chapter number, keep 1 distinct entry
        val distinctChapters = chapters.distinctBy { it.number }

        // Sort ascending by chapter number so Chapter 1 comes first
        distinctChapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
    }

    override suspend fun getPages(chapterId: String): List<String> {
        val clean = chapterId.removePrefix("ck_ch_").removePrefix("ck_")
        val parts = clean.split("__")
        val chapterUrl = if (parts.size >= 4) {
            val slug = parts[0]
            val hid = parts[1]
            val chap = parts[2]
            val lang = parts[3]
            "$baseUrl/comic/$slug/$hid-chapter-$chap-$lang"
        } else {
            "$baseUrl/comic/$clean"
        }

        val html = fetchHtml(chapterUrl)
        if (html.isBlank()) return emptyList()

        val m = Regex("""<script[^>]*id=["']sv-data["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL).find(html)
        if (m != null) {
            try {
                val data = JSONObject(m.groupValues[1])
                val chObj = data.optJSONObject("chapter")
                val imgArr = chObj?.optJSONArray("images")
                if (imgArr != null) {
                    val pages = mutableListOf<String>()
                    for (i in 0 until imgArr.length()) {
                        val img = imgArr.optJSONObject(i)
                        val u = img?.optString("url").orEmpty()
                        if (u.startsWith("http")) pages.add(u)
                    }
                    if (pages.isNotEmpty()) return pages
                }
            } catch (e: Exception) {
                Log.w("ComicKRepo", "Error parsing sv-data: ${e.message}")
            }
        }

        // Fallback: extract image URLs with cdn1.comicknew.pictures or meo.comick.pictures
        val directImgs = Regex("""https?://(?:cdn1\.comicknew\.pictures|meo\.comick\.pictures|meo3\.comick\.pictures)/[^\s"']+\.(?:jpg|jpeg|png|webp)""", RegexOption.IGNORE_CASE)
        val matches = directImgs.findAll(html).map { it.value }.distinct().toList()
        return matches
    }
}

// ============================================================================
// 3. ASURA SCANS REPOSITORY (api.asurascans.com - Korean Action, Fantasy, Regression)
// ============================================================================
class AsuraScansRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : IMangaSourceRepository {
    override val source: MangaSource = MangaSource.ASURA

    private val apiUrl = "https://api.asurascans.com/api"
    private val siteUrl = "https://asuracomic.net"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private suspend fun fetchJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Referer", "$siteUrl/")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.isNotBlank()) JSONObject(body) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w("AsuraRepo", "Failed to fetch $url: ${e.message}")
            null
        }
    }

    private fun parseSeries(arr: JSONArray?): List<MangaCard> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MangaCard>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val slug = item.optString("slug")
            val title = item.optString("title")
            if (slug.isBlank() || title.isBlank()) continue

            val cover = item.optString("cover").ifBlank { item.optString("cover_url") }.ifBlank { "https://cdn.asurascans.com/asura-images/covers/$slug.webp" }
            val type = item.optString("type").takeIf { it.isNotBlank() } ?: "Manhwa"

            list.add(
                MangaCard(
                    id = "as_$slug",
                    title = title,
                    poster = cover,
                    type = type,
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(): MangaHome = coroutineScope {
        val trendingDeferred = async {
            val json = fetchJson("$apiUrl/series?limit=20")
            parseSeries(json?.optJSONArray("data"))
        }
        val latestDeferred = async {
            val json = fetchJson("$apiUrl/series?limit=20&offset=20")
            parseSeries(json?.optJSONArray("data"))
        }
        val newDeferred = async {
            val json = fetchJson("$apiUrl/series?limit=20&offset=40")
            parseSeries(json?.optJSONArray("data"))
        }

        MangaHome(
            trending = trendingDeferred.await(),
            latest = latestDeferred.await(),
            newRelease = newDeferred.await()
        )
    }

    override suspend fun search(query: String): List<MangaCard> {
        val enc = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$apiUrl/series?search=$enc&limit=20"
        val json = fetchJson(url)
        return parseSeries(json?.optJSONArray("data"))
    }

    override suspend fun getInfo(mangaId: String): MangaInfo? {
        val slug = mangaId.removePrefix("as_")
        val url = "$apiUrl/series/$slug"
        val json = fetchJson(url) ?: return null

        val sObj = json.optJSONObject("series") ?: json.optJSONObject("data") ?: json
        val title = sObj.optString("title")
        val desc = sObj.optString("description").replace(Regex("<[^>]*>"), " ").trim()
        val cover = sObj.optString("cover").ifBlank { sObj.optString("cover_url") }.ifBlank { "https://cdn.asurascans.com/asura-images/covers/$slug.webp" }
        val status = sObj.optString("status")
        val type = sObj.optString("type").ifBlank { "Manhwa" }
        val author = sObj.optString("author").takeIf { it.isNotBlank() }
        val rating = sObj.optDouble("rating", 0.0).takeIf { it > 0 }?.let { String.format(java.util.Locale.US, "%.1f", it) }

        val genres = mutableListOf<String>()
        val gArr = sObj.optJSONArray("genres")
        if (gArr != null) {
            for (i in 0 until gArr.length()) {
                val gName = gArr.optJSONObject(i)?.optString("name")
                if (!gName.isNullOrBlank()) genres.add(gName)
            }
        }

        return MangaInfo(
            id = "as_$slug",
            title = title,
            poster = cover,
            description = desc,
            type = type,
            status = status,
            rating = rating,
            genres = genres,
            authors = listOfNotNull(author),
            source = source.id
        )
    }

    override suspend fun getChapters(mangaId: String): List<MangaChapter> {
        val slug = mangaId.removePrefix("as_")
        val url = "$apiUrl/series/$slug/chapters"
        val json = fetchJson(url) ?: return emptyList()
        val dataArr = json.optJSONArray("data") ?: return emptyList()

        val chapters = mutableListOf<MangaChapter>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val num = item.optString("number")
            val title = item.optString("title").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val date = item.optString("published_at")

            chapters.add(
                MangaChapter(
                    number = num,
                    title = title,
                    chapterId = "as_${slug}__${num}",
                    releaseDate = date.take(10),
                    url = "$siteUrl/comics/$slug/chapter/$num",
                    source = source.id
                )
            )
        }

        val distinctChapters = chapters.distinctBy { it.number }
        return distinctChapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
    }

    override suspend fun getPages(chapterId: String): List<String> {
        val clean = chapterId.removePrefix("as_ch_").removePrefix("as_")
        val parts = clean.split("__")
        val slug = parts[0]
        val num = if (parts.size > 1) parts[1] else "1"

        val url = "$apiUrl/series/$slug/chapters/$num"
        val json = fetchJson(url) ?: return emptyList()
        val dataObj = json.optJSONObject("data") ?: return emptyList()
        val chapterObj = dataObj.optJSONObject("chapter") ?: return emptyList()
        val pagesArr = chapterObj.optJSONArray("pages") ?: return emptyList()

        val pages = mutableListOf<String>()
        for (i in 0 until pagesArr.length()) {
            val p = pagesArr.optJSONObject(i) ?: continue
            val u = p.optString("url")
            if (u.startsWith("http")) pages.add(u)
        }
        return pages
    }
}

// ============================================================================
// 4. WEEBCENTRAL REPOSITORY (Scraper & Worker proxy)
// ============================================================================
class WeebCentralRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : IMangaSourceRepository {
    override val source: MangaSource = MangaSource.WEEBCENTRAL

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
    private val JINA_ORIGIN = "https://r.jina.ai/http://weebcentral.com"
    private val WEEBCENTRAL_BASE = "https://weebcentral.com"

    private fun cleanId(id: String): String {
        var s = id.trim()
        if (s.contains("?")) s = s.substringBefore("?")
        if (s.contains("#")) s = s.substringBefore("#")
        if (s.contains("/series/")) {
            val after = s.substringAfter("/series/")
            val seriesId = after.substringBefore("/").substringBefore("_")
            if (seriesId.isNotBlank()) return seriesId
        }
        s = s.removePrefix("wc_")
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) return ulidMatch.groupValues[1]
        return s.split("_").firstOrNull()?.split("/")?.firstOrNull() ?: s
    }

    private fun cleanChapId(chapterId: String): String {
        var s = chapterId.trim()
        if (s.contains("?")) s = s.substringBefore("?")
        if (s.contains("#")) s = s.substringBefore("#")
        if (s.contains("/chapters/")) {
            s = s.substringAfter("/chapters/").substringBefore("/")
        }
        val ulidMatch = Regex("""\b([0-9A-HJ-KM-NP-TV-Z]{26})\b""", RegexOption.IGNORE_CASE).find(s)
        if (ulidMatch != null) return ulidMatch.groupValues[1]
        return s.removePrefix("wc_chapter_").removePrefix("wc_")
    }

    override suspend fun getHome(): MangaHome = withContext(Dispatchers.IO) {
        // Strategy A: Worker
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
                            return@withContext MangaHome(
                                trending = trending,
                                latest = latest,
                                newRelease = if (newRelease.isNotEmpty()) newRelease else latest.take(20)
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Strategy B: Client scrape
        try {
            val html = fetchWeebHtml("/")
            val cards = parseSeriesCardsFromHtml(html)
            if (cards.isNotEmpty()) {
                val half = (cards.size / 2).coerceAtLeast(1)
                return@withContext MangaHome(
                    trending = cards.take(half),
                    latest = cards.drop(half),
                    newRelease = cards.take(16)
                )
            }
        } catch (_: Exception) {}

        MangaHome()
    }

    override suspend fun search(query: String): List<MangaCard> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        val enc = URLEncoder.encode(q, "UTF-8")

        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=search&q=$enc")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 Aniverse/1.0")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val results = parseCardsArray(json.optJSONArray("results"))
                        if (results.isNotEmpty()) return@withContext results
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val url = "/search/data?text=$enc&sort=Best+Match&order=Descending&official=Any&anime=Any&adult=Any&display_mode=Full+Display"
            val html = fetchWeebHtml(url)
            val cards = parseSeriesCardsFromHtml(html)
            if (cards.isNotEmpty()) return@withContext cards
        } catch (_: Exception) {}

        emptyList()
    }

    override suspend fun getInfo(mangaId: String): MangaInfo? = withContext(Dispatchers.IO) {
        val seriesId = cleanId(mangaId)
        if (seriesId.isBlank()) return@withContext null

        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=info&id=$seriesId")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
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
                                id = "wc_$seriesId",
                                title = infoObj.optString("title", "Unknown"),
                                altTitle = infoObj.optString("altTitle").takeIf { it.isNotBlank() },
                                poster = infoObj.optString("poster").takeIf { it.isNotBlank() },
                                description = infoObj.optString("description", ""),
                                type = infoObj.optString("type", "Manga"),
                                status = infoObj.optString("status").takeIf { it.isNotBlank() },
                                rating = infoObj.optString("rating").takeIf { it.isNotBlank() },
                                genres = genres,
                                source = source.id
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

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
                id = "wc_$seriesId",
                title = title,
                poster = poster,
                description = description,
                type = type,
                status = status.ifBlank { null },
                genres = genres.distinct(),
                source = source.id
            )
        } catch (_: Exception) {}

        null
    }

    override suspend fun getChapters(mangaId: String): List<MangaChapter> = withContext(Dispatchers.IO) {
        val seriesId = cleanId(mangaId)
        if (seriesId.isBlank()) return@withContext emptyList()

        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=chapters&id=$seriesId")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
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
                            val cleanChId = cleanChapId(rawChapId)
                            val numStr = obj.optString("number", "${i + 1}")
                            val titleStr = obj.optString("title").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            chapters.add(
                                MangaChapter(
                                    number = numStr,
                                    title = titleStr,
                                    chapterId = "wc_$cleanChId",
                                    releaseDate = obj.optString("releaseDate").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
                                    url = "$WEEBCENTRAL_BASE/chapters/$cleanChId",
                                    source = source.id
                                )
                            )
                        }
                        if (chapters.isNotEmpty()) {
                            return@withContext chapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val html = fetchWeebHtml("/series/$seriesId/full-chapter-list")
            val chapters = parseChaptersFromHtml(html)
            if (chapters.isNotEmpty()) {
                return@withContext chapters.sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.number }))
            }
        } catch (_: Exception) {}

        emptyList()
    }

    override suspend fun getPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val cleanChId = cleanChapId(chapterId)
        if (cleanChId.isBlank()) return@withContext emptyList()

        // Worker
        try {
            val req = Request.Builder()
                .url("$WORKER_ORIGIN/?action=pages&chapterId=$cleanChId")
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val pages = parsePagesFromBody(body)
                    if (pages.isNotEmpty()) return@withContext pages
                }
            }
        } catch (_: Exception) {}

        // HTMX
        try {
            val target = "/chapters/$cleanChId/images?is_prev=False&current_page=0&reading_style=long_strip"
            val html = fetchWeebHtml(target, headers = mapOf("HX-Request" to "true", "X-Requested-With" to "XMLHttpRequest"))
            val pages = parsePageImagesFromHtml(html)
            if (pages.isNotEmpty()) return@withContext pages
        } catch (_: Exception) {}

        emptyList()
    }

    private fun parsePagesFromBody(body: String): List<String> {
        val trimmed = body.trim()
        val pages = mutableListOf<String>()
        if (trimmed.startsWith("[")) {
            try {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    if (item is String && item.startsWith("http")) pages.add(item)
                    else if (item is JSONObject) {
                        val url = item.optString("url", item.optString("src", item.optString("image", "")))
                        if (url.startsWith("http")) pages.add(url)
                    }
                }
            } catch (_: Exception) {}
        }
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

        try {
            val directReqBuilder = Request.Builder()
                .url(target)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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

        try {
            val localProxyUrl = com.nyyrox.aniverse.player.StreamProxyServer.buildProxyUrl(
                target,
                mapOf("User-Agent" to "Mozilla/5.0", "Referer" to "https://weebcentral.com/")
            )
            val localReq = Request.Builder().url(localProxyUrl).header("Accept", "text/html, */*").build()
            client.newCall(localReq).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    if (body.isNotBlank()) return body
                }
            }
        } catch (_: Exception) {}

        return ""
    }

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
                    source = source.id
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
                            source = source.id
                        )
                    )
                }
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
                    chapters.add(
                        MangaChapter(
                            number = number,
                            title = title,
                            chapterId = "wc_${cleanChapId(chapterId)}",
                            releaseDate = dateMatch?.value,
                            url = "$WEEBCENTRAL_BASE/chapters/${cleanChapId(chapterId)}",
                            source = source.id
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
            if (!url.contains("/static/images/") && !url.contains("logo", ignoreCase = true)) pages.add(url)
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

