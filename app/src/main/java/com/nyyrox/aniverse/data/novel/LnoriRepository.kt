package com.nyyrox.aniverse.data.novel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

// ============================================================================
// LNORI REPOSITORY (Premier Light Novel Library & Series Catalog)
// https://lnori.com/library
// ============================================================================
class LnoriRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.LNORI
    private val baseUrl = "https://lnori.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    data class LnoriCatalogEntry(
        val id: String,
        val title: String,
        val author: String,
        val year: String,
        val volumes: Int,
        val tags: List<String>,
        val relevance: Int,
        val slug: String,
        val cover: String,
        val description: String
    )

    private var cachedCatalog: List<LnoriCatalogEntry>? = null
    private var lastCatalogFetch: Long = 0L
    private val catalogMutex = Mutex()

    private suspend fun fetchHtml(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw java.io.IOException("HTTP ${response.code} for $url")
        }
        val body = response.body.string()
        Jsoup.parse(body, url)
    }

    private suspend fun getCatalog(): List<LnoriCatalogEntry> = catalogMutex.withLock {
        val now = System.currentTimeMillis()
        val cached = cachedCatalog
        if (cached != null && (now - lastCatalogFetch < 3600_000L)) {
            return@withLock cached
        }

        try {
            val doc = fetchHtml("$baseUrl/library")
            val entries = mutableListOf<LnoriCatalogEntry>()
            val articles = doc.select("article.card[data-id]")
            for (art in articles) {
                val id = art.attr("data-id").trim()
                if (id.isBlank()) continue

                val title = art.attr("data-t").trim().ifBlank {
                    art.selectFirst(".card-title, h2, h3")?.text()?.trim().orEmpty()
                }
                if (title.isBlank()) continue

                val author = art.attr("data-a").trim()
                val year = art.attr("data-d").trim()
                val volumes = art.attr("data-v").toIntOrNull() ?: 1
                val tags = art.attr("data-tags").split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                val relevance = art.attr("data-rel").toIntOrNull() ?: 0

                // Link & slug: href is e.g. "/series/3343/re-zero-starting-life-in-another-world"
                val linkHref = art.selectFirst("a[href*='/series/']")?.attr("href").orEmpty()
                val slug = if (linkHref.isNotBlank()) {
                    linkHref.removePrefix("/series/").removePrefix("/").trim()
                } else {
                    id
                }

                // Cover: e.g. "https://cdn.lnori.com/cover/12040.webp"
                val img = art.selectFirst("img")
                var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
                if (cover.isNotBlank() && !cover.startsWith("http")) {
                    cover = "https://cdn.lnori.com$cover"
                }

                val desc = art.selectFirst(".popup-description, .card-description")?.text()?.trim().orEmpty()

                entries.add(
                    LnoriCatalogEntry(
                        id = id,
                        title = title,
                        author = author,
                        year = year,
                        volumes = volumes,
                        tags = tags,
                        relevance = relevance,
                        slug = slug,
                        cover = cover,
                        description = desc
                    )
                )
            }
            if (entries.isNotEmpty()) {
                cachedCatalog = entries
                lastCatalogFetch = now
                return@withLock entries
            }
        } catch (_: Exception) {}

        return@withLock cachedCatalog ?: emptyList()
    }

    private fun LnoriCatalogEntry.toNovelItem(): NovelItem {
        val statsParts = mutableListOf<String>()
        if (volumes > 0) statsParts.add("$volumes Vols")
        if (year.isNotBlank()) statsParts.add(year)
        if (author.isNotBlank()) statsParts.add(author)

        return NovelItem(
            slug = slug,
            title = title,
            cover = cover,
            stats = statsParts.joinToString(" • ").ifBlank { null },
            rating = null,
            source = source.id
        )
    }

    private suspend fun resolveSeriesUrl(slug: String): String {
        if (slug.startsWith("http")) return slug
        val clean = slug.removePrefix("/series/").removePrefix("/")
        if (clean.contains("/")) {
            return "$baseUrl/series/$clean"
        }
        val catalog = getCatalog()
        val matched = catalog.find { it.id == clean || it.slug.startsWith("$clean/") || it.slug == clean }
        if (matched != null) {
            return "$baseUrl/series/${matched.slug}"
        }
        return "$baseUrl/series/$clean"
    }

    override suspend fun getHome(page: Int): NovelHomeData = withContext(Dispatchers.IO) {
        val catalog = getCatalog()
        val popularEntries = catalog.sortedByDescending { it.relevance }
        val rankings = popularEntries.take(15).map { it.toNovelItem() }
        val popular = popularEntries.take(30).map { it.toNovelItem() }
        val latest = catalog.sortedByDescending { (it.year.toIntOrNull() ?: 0) * 10000 + (it.id.toIntOrNull() ?: 0) }
            .take(30)
            .map { it.toNovelItem() }
        val lightNovels = popularEntries.filter { it.tags.any { t -> t in listOf("isekai", "fantasy", "adventure", "action", "romance", "school") } }
            .take(30)
            .map { it.toNovelItem() }
            .ifEmpty { popular }

        NovelHomeData(
            rankings = rankings,
            popular = popular,
            latest = latest,
            lightNovels = lightNovels
        )
    }

    override suspend fun getRankings(page: Int): List<NovelItem> {
        val catalog = getCatalog()
        return catalog.sortedByDescending { it.relevance }
            .drop((page - 1) * 24)
            .take(24)
            .map { it.toNovelItem() }
    }

    override suspend fun getPopular(page: Int): List<NovelItem> = getRankings(page)

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val catalog = getCatalog()
        return catalog.sortedByDescending { (it.year.toIntOrNull() ?: 0) * 10000 + (it.id.toIntOrNull() ?: 0) }
            .drop((page - 1) * 24)
            .take(24)
            .map { it.toNovelItem() }
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> = getPopular(page)

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return getPopular(page)
        val catalog = getCatalog()

        val filtered = catalog.filter { item ->
            item.title.lowercase().contains(q) ||
            item.author.lowercase().contains(q) ||
            item.tags.any { it.contains(q) }
        }.sortedWith(
            compareByDescending<LnoriCatalogEntry> { it.title.equals(q, ignoreCase = true) }
                .thenByDescending { it.title.startsWith(q, ignoreCase = true) }
                .thenByDescending { it.title.lowercase().contains(q) }
                .thenByDescending { it.relevance }
        )

        return filtered
            .drop((page - 1) * 24)
            .take(24)
            .map { it.toNovelItem() }
    }

    override suspend fun getDetail(slug: String): NovelDetail? = withContext(Dispatchers.IO) {
        val seriesUrl = resolveSeriesUrl(slug)
        val doc = try { fetchHtml(seriesUrl) } catch (_: Exception) { return@withContext null }

        var title: String? = null
        var author: String? = null
        var desc: String = ""
        var genres = emptyList<String>()
        var cover: String = ""
        var volumeCount = 0
        var firstChapterSlug: String? = null

        val scripts = doc.select("script[type=application/ld+json]")
        for (s in scripts) {
            val jsonText = s.data().trim()
            if (jsonText.contains("\"hasPart\"") || jsonText.contains("\"@type\":\"Book\"")) {
                try {
                    val obj = JSONObject(jsonText)
                    if (obj.optString("@type") == "Book") {
                        title = obj.optString("name").ifBlank { null }
                        author = obj.optJSONObject("author")?.optString("name")
                            ?: obj.optString("author").ifBlank { null }
                        desc = obj.optString("description")
                        cover = obj.optString("image")
                        val genreArr = obj.optJSONArray("genre")
                        if (genreArr != null) {
                            val glist = mutableListOf<String>()
                            for (i in 0 until genreArr.length()) glist.add(genreArr.getString(i))
                            genres = glist
                        }
                        val parts = obj.optJSONArray("hasPart")
                        volumeCount = parts?.length() ?: 0
                        if (parts != null && parts.length() > 0) {
                            val fUrl = parts.getJSONObject(0).optString("url").trim()
                            if (fUrl.isNotBlank()) {
                                firstChapterSlug = fUrl.removePrefix("$baseUrl/").removePrefix("/")
                            }
                        }
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        if (firstChapterSlug == null) {
            val firstBook = doc.selectFirst("a[href*='/book/']")
            if (firstBook != null) {
                firstChapterSlug = firstBook.attr("href").removePrefix("$baseUrl/").removePrefix("/").trim()
            }
        }

        if (title.isNullOrBlank()) {
            title = doc.selectFirst("h1, .hero-title, .series-title")?.text()?.trim()
        }
        if (title.isNullOrBlank() || title.contains("404", ignoreCase = true) || title.contains("Not Found", ignoreCase = true)) {
            return@withContext null
        }
        if (volumeCount == 0 && doc.select("a[href*='/book/']").isEmpty() && desc.isBlank()) {
            return@withContext null
        }
        if (author.isNullOrBlank()) {
            author = doc.selectFirst(".author, .hero-author")?.text()?.trim()
        }
        if (desc.isBlank()) {
            desc = doc.selectFirst(".description, .synopsis, p")?.text()?.trim().orEmpty()
        }
        // 1. Look up series in catalog first - catalog always has the authentic vertical book cover from the homepage
        val cleanSlug = slug.removePrefix("/series/").removePrefix("series/").removePrefix("/")
        val catalog = getCatalog()
        val catEntry = catalog.find {
            it.id == cleanSlug || it.slug == cleanSlug || it.slug.startsWith("$cleanSlug/") || it.slug.endsWith("/$cleanSlug")
        }
        if (catEntry != null && catEntry.cover.isNotBlank()) {
            cover = catEntry.cover
        }

        // 2. If not found in catalog, extract hero cover image from page
        if (cover.isBlank() || cover.contains("/series/")) {
            val heroImg = doc.selectFirst(".hero-card img, .cover-wrap img, .card-cover img, .series-cover img, .cover img, .vol-grid .card img")
            val heroSrc = heroImg?.attr("src")?.ifBlank { heroImg.attr("data-src") }.orEmpty()
            if (heroSrc.isNotBlank()) {
                cover = if (heroSrc.startsWith("http")) heroSrc else "$baseUrl$heroSrc"
            }
        }

        // 3. Fallback: extract volume ID from first chapter
        if (cover.isBlank() || cover.contains("/series/")) {
            if (!firstChapterSlug.isNullOrBlank()) {
                val volId = firstChapterSlug.removePrefix("book/").substringBefore("/")
                if (volId.all { it.isDigit() }) {
                    cover = "https://cdn.lnori.com/cover/$volId.webp"
                }
            }
        }

        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = "$baseUrl$cover"
        }

        NovelDetail(
            slug = slug,
            title = title,
            cover = cover,
            author = author,
            rating = null,
            status = "Completed / Ongoing",
            chaptersCount = if (volumeCount > 0) "$volumeCount Volumes" else null,
            views = null,
            genres = genres,
            synopsis = desc,
            firstChapterSlug = firstChapterSlug,
            source = source.id
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> = withContext(Dispatchers.IO) {
        val seriesUrl = resolveSeriesUrl(slug)
        val doc = try { fetchHtml(seriesUrl) } catch (_: Exception) { return@withContext Pair(emptyList(), 1) }

        val chapters = mutableListOf<NovelChapter>()

        // 1. Parse directly from HTML volume cards (.vol-grid article.card, article.card[data-id])
        val articles = doc.select(".vol-grid article.card, article.card[data-id]")
        if (articles.isNotEmpty()) {
            val seen = mutableSetOf<String>()
            var idx = 1
            for (art in articles) {
                val a = art.selectFirst("a[href*='/book/']") ?: continue
                val href = a.attr("href").trim()
                val chSlug = href.removePrefix("$baseUrl/").removePrefix("/")
                if (chSlug.isBlank() || !seen.add(chSlug)) continue

                val dataId = art.attr("data-id").trim()
                val title = art.selectFirst(".card-title, .popup-title")?.text()?.trim()
                    ?.ifBlank { a.attr("aria-label").trim() }
                    ?: "Volume $idx"

                val img = art.selectFirst("img")
                var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
                if (cover.isNotBlank() && !cover.startsWith("http")) {
                    cover = "https://cdn.lnori.com$cover"
                }
                if (cover.isBlank() && dataId.isNotBlank()) {
                    cover = "https://cdn.lnori.com/cover/$dataId.webp"
                }

                val desc = art.selectFirst(".popup-description")?.text()?.trim().orEmpty()
                val release = if (desc.isNotBlank()) {
                    desc.take(90) + if (desc.length > 90) "..." else ""
                } else {
                    "Light Novel"
                }

                chapters.add(
                    NovelChapter(
                        slug = chSlug,
                        title = title,
                        number = "$idx",
                        releaseTime = release,
                        cover = cover
                    )
                )
                idx++
            }
        }

        // 2. If HTML cards were empty, parse JSON-LD hasPart with computed volume covers
        if (chapters.isEmpty()) {
            val scripts = doc.select("script[type=application/ld+json]")
            for (s in scripts) {
                val jsonText = s.data().trim()
                if (jsonText.contains("\"hasPart\"")) {
                    try {
                        val obj = JSONObject(jsonText)
                        val parts = obj.optJSONArray("hasPart")
                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                val name = part.optString("name").trim()
                                val url = part.optString("url").trim()
                                val pos = part.optString("position").ifBlank { "${i + 1}" }
                                if (url.isNotBlank()) {
                                    val chSlug = url.removePrefix("$baseUrl/").removePrefix("/")
                                    val volId = chSlug.removePrefix("book/").substringBefore("/")
                                    val volCover = if (volId.all { it.isDigit() }) "https://cdn.lnori.com/cover/$volId.webp" else null
                                    chapters.add(
                                        NovelChapter(
                                            slug = chSlug,
                                            title = name.ifBlank { "Volume ${i + 1}" },
                                            number = pos,
                                            releaseTime = "Light Novel",
                                            cover = volCover
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (chapters.isNotEmpty()) break
            }
        }

        // 3. Fallback to generic book links
        if (chapters.isEmpty()) {
            val bookLinks = doc.select("a[href*='/book/']")
            val seen = mutableSetOf<String>()
            var idx = 1
            for (a in bookLinks) {
                val href = a.attr("href").trim()
                val chSlug = href.removePrefix("$baseUrl/").removePrefix("/")
                if (chSlug.isBlank() || !seen.add(chSlug)) continue
                val text = a.text().trim()
                val title = if (text.isNotBlank() && !text.contains("Start Reading", ignoreCase = true)) {
                    text
                } else {
                    "Volume $idx"
                }
                val volId = chSlug.removePrefix("book/").substringBefore("/")
                val volCover = if (volId.all { it.isDigit() }) "https://cdn.lnori.com/cover/$volId.webp" else null
                chapters.add(
                    NovelChapter(
                        slug = chSlug,
                        title = title,
                        number = "$idx",
                        releaseTime = "Light Novel",
                        cover = volCover
                    )
                )
                idx++
            }
        }

        Pair(chapters, 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? = withContext(Dispatchers.IO) {
        val url = when {
            chapterSlug.startsWith("http") -> chapterSlug
            chapterSlug.startsWith("book/") -> "$baseUrl/$chapterSlug"
            chapterSlug.startsWith("/") -> "$baseUrl$chapterSlug"
            else -> "$baseUrl/book/$chapterSlug"
        }

        val doc = try { fetchHtml(url) } catch (_: Exception) { return@withContext null }

        val title = doc.selectFirst("h1.book-title, h1, .title")?.text()?.trim()
            ?: chapterSlug.substringAfterLast("/").replace("-", " ")

        val contentBody = doc.selectFirst("article.content-body, article, main") ?: return@withContext null

        val paragraphs = mutableListOf<String>()

        val volCover = doc.selectFirst("img[src*='/volume/'], meta[property=og:image]")?.let {
            if (it.tagName() == "meta") it.attr("content") else it.attr("src")
        }
        if (!volCover.isNullOrBlank()) {
            val cUrl = if (volCover.startsWith("http")) volCover else "$baseUrl$volCover"
            paragraphs.add("[img]$cUrl[/img]")
        }

        val elements = contentBody.select("h1, h2, h3, h4, h5, h6, p, img, hr")
        for (el in elements) {
            when (el.tagName()) {
                "img" -> {
                    val src = el.attr("src").ifBlank { el.attr("data-src") }
                    if (src.isNotBlank() && !src.contains("avatar") && !src.contains("ico")) {
                        val fullSrc = if (src.startsWith("http")) src else "$baseUrl$src"
                        paragraphs.add("[img]$fullSrc[/img]")
                    }
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val hText = el.text().trim()
                    if (hText.isNotBlank() && hText.length < 120) {
                        paragraphs.add("== $hText ==")
                    }
                }
                "hr" -> {
                    paragraphs.add("* * *")
                }
                "p" -> {
                    val innerImg = el.selectFirst("img")
                    if (innerImg != null) {
                        val src = innerImg.attr("src").ifBlank { innerImg.attr("data-src") }
                        if (src.isNotBlank() && !src.contains("avatar") && !src.contains("ico")) {
                            val fullSrc = if (src.startsWith("http")) src else "$baseUrl$src"
                            paragraphs.add("[img]$fullSrc[/img]")
                        }
                    } else {
                        val pText = el.text().trim()
                        if (pText.isNotBlank()) {
                            paragraphs.add(pText)
                        }
                    }
                }
            }
        }

        var prevSlug: String? = null
        var nextSlug: String? = null
        try {
            val (allChapters, _) = getChapters(novelSlug, 1)
            val currentClean = chapterSlug.removePrefix("$baseUrl/").removePrefix("/")
            val curIdx = allChapters.indexOfFirst {
                it.slug.equals(currentClean, ignoreCase = true) ||
                it.slug.contains(currentClean) ||
                currentClean.contains(it.slug)
            }
            if (curIdx >= 0) {
                if (curIdx > 0) prevSlug = allChapters[curIdx - 1].slug
                if (curIdx < allChapters.size - 1) nextSlug = allChapters[curIdx + 1].slug
            }
        } catch (_: Exception) {}

        NovelChapterContent(
            novelSlug = novelSlug,
            chapterSlug = chapterSlug,
            title = title,
            paragraphs = paragraphs,
            prevChapterSlug = prevSlug,
            nextChapterSlug = nextSlug
        )
    }
}
