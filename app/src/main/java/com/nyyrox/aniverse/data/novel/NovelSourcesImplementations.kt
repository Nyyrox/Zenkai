package com.nyyrox.aniverse.data.novel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// ============================================================================
// 1. NOVELFULL REPOSITORY (Premier Light Novel & Web Novel Source)
// ============================================================================
class NovelFullRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.NOVEL_FULL
    private val baseUrl = "https://novelfull.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private suspend fun fetchHtml(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val response = client.newCall(request).execute()
        val body = response.body.string()
        Jsoup.parse(body, url)
    }

    private fun parseList(doc: Document): List<NovelItem> {
        val list = mutableListOf<NovelItem>()
        val rows = doc.select(".list-truyen .row, .col-truyen-main .row, .row:has(.truyen-title)")
        for (r in rows) {
            val a = r.selectFirst("h3.truyen-title a, .truyen-title a") ?: continue
            val href = a.attr("href")
            val slug = href.removePrefix("/").substringBefore(".html").trim()
            if (slug.isBlank()) continue

            val title = a.text().trim()
            if (title.isBlank()) continue

            val img = r.selectFirst("img")
            var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
            if (cover.isNotBlank() && !cover.startsWith("http")) {
                cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
            }

            val chapter = r.selectFirst(".text-info a")?.text()?.trim()
            val author = r.selectFirst(".author")?.text()?.trim()
            val stats = listOfNotNull(chapter, author).joinToString(" • ")

            list.add(
                NovelItem(
                    slug = slug,
                    title = title,
                    cover = cover,
                    stats = stats.ifBlank { null },
                    rating = null,
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(page: Int): NovelHomeData = withContext(Dispatchers.IO) {
        val popular = try { getPopular(page) } catch (_: Exception) { emptyList() }
        val latest = try { getLatest(page) } catch (_: Exception) { emptyList() }
        val lightNovels = try { getLightNovels(page) } catch (_: Exception) { emptyList() }
        val rankings = popular.take(15)
        NovelHomeData(rankings, popular, latest, lightNovels)
    }

    override suspend fun getRankings(page: Int): List<NovelItem> {
        return getPopular(page)
    }

    override suspend fun getPopular(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/most-popular" else "$baseUrl/most-popular?page=$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/latest-release-novel" else "$baseUrl/latest-release-novel?page=$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/genre/Fantasy" else "$baseUrl/genre/Fantasy?page=$page"
        val list = parseList(fetchHtml(url))
        return if (list.isNotEmpty()) list else getPopular(page)
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/search?keyword=$enc" else "$baseUrl/search?keyword=$enc&page=$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val url = "$baseUrl/$slug.html"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h3.title, .title")?.text()?.trim() ?: return null
        val img = doc.selectFirst(".book img, .info-holder img")
        var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
        }

        val author = doc.select(".info div:has(h3:contains(Author)) a").text().trim().ifBlank { null }
        val genres = doc.select(".info div:has(h3:contains(Genre)) a").map { it.text().trim() }
        val status = doc.select(".info div:has(h3:contains(Status)) a").text().trim().ifBlank { null }
        val desc = doc.select(".desc-text p").joinToString("\n\n") { it.text().trim() }.ifBlank {
            doc.select(".desc-text").text().trim()
        }

        return NovelDetail(
            slug = slug,
            title = title,
            cover = cover,
            author = author,
            rating = null,
            status = status,
            chaptersCount = null,
            views = null,
            genres = genres,
            synopsis = desc,
            source = source.id
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        val detailUrl = "$baseUrl/$slug.html"
        val doc = fetchHtml(detailUrl)

        val list = mutableListOf<NovelChapter>()

        // Try ajax-chapter-option if novelId is available
        val novelId = doc.selectFirst("#rating[data-novel-id]")?.attr("data-novel-id")
        if (!novelId.isNullOrBlank()) {
            try {
                val ajaxDoc = fetchHtml("$baseUrl/ajax-chapter-option?novelId=$novelId")
                ajaxDoc.select("option").forEach { opt ->
                    val href = opt.attr("value")
                    val chSlug = href.removePrefix("/").substringBefore(".html").trim()
                    val text = opt.text().trim()
                    if (chSlug.isNotBlank()) {
                        list.add(
                            NovelChapter(
                                slug = chSlug,
                                title = text,
                                number = chSlug.substringAfterLast("-", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // fallback to page chapters
            }
        }

        if (list.isEmpty()) {
            val pagedUrl = if (page <= 1) "$baseUrl/$slug.html" else "$baseUrl/$slug.html?page=$page"
            val pageDoc = if (pagedUrl == detailUrl) doc else fetchHtml(pagedUrl)
            pageDoc.select(".list-chapter li a").forEach { a ->
                val href = a.attr("href")
                val chSlug = href.removePrefix("/").substringBefore(".html").trim()
                val text = a.text().trim()
                if (chSlug.isNotBlank()) {
                    list.add(
                        NovelChapter(
                            slug = chSlug,
                            title = text,
                            number = chSlug.substringAfterLast("-", "")
                        )
                    )
                }
            }
        }

        var maxPage = page
        doc.select(".pagination a").forEach { pLink ->
            val p = pLink.attr("href").substringAfter("page=").substringBefore("&").toIntOrNull()
            if (p != null && p > maxPage) maxPage = p
        }

        return Pair(list, maxPage)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val url = "$baseUrl/$chapterSlug.html"
        val doc = fetchHtml(url)

        val title = doc.selectFirst(".chapter-title, h2, h1")?.text()?.trim() ?: chapterSlug
        val contentEl = doc.selectFirst("#chapter-content, .chapter-c, .chapter-content") ?: return null
        contentEl.select("script, style, iframe, .ads, .adsbygoogle").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
        val finalParas = if (paragraphs.isNotEmpty()) {
            paragraphs
        } else {
            contentEl.wholeText().lines().map { it.trim() }.filter { it.isNotBlank() }
        }

        val prevA = doc.selectFirst("#prev_chap, a:contains(Prev)")
        val nextA = doc.selectFirst("#next_chap, a:contains(Next)")

        val prevSlug = prevA?.attr("href")?.removePrefix("/")?.substringBefore(".html")?.takeIf { it.isNotBlank() && !it.contains("javascript") }
        val nextSlug = nextA?.attr("href")?.removePrefix("/")?.substringBefore(".html")?.takeIf { it.isNotBlank() && !it.contains("javascript") }

        return NovelChapterContent(
            novelSlug = novelSlug,
            chapterSlug = chapterSlug,
            title = title,
            paragraphs = finalParas,
            prevChapterSlug = prevSlug,
            nextChapterSlug = nextSlug
        )
    }
}

// ============================================================================
// 3. ROYAL ROAD REPOSITORY (#1 English Web Novel, LitRPG & Progression Fantasy)
// ============================================================================
class RoyalRoadRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.ROYAL_ROAD
    private val baseUrl = "https://www.royalroad.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private suspend fun fetchHtml(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val response = client.newCall(request).execute()
        val body = response.body.string()
        Jsoup.parse(body, url)
    }

    private fun parseFictionList(doc: Document): List<NovelItem> {
        val list = mutableListOf<NovelItem>()
        val items = doc.select(".fiction-list-item, div:has(h2.fiction-title)")
        for (item in items) {
            val a = item.selectFirst("h2.fiction-title a, .fiction-title a, a[href*='/fiction/']") ?: continue
            val href = a.attr("href")
            val slug = href.removePrefix("/fiction/").trim('/')
            if (slug.isBlank() || list.any { it.slug == slug }) continue

            val title = a.text().trim()
            if (title.isBlank()) continue

            val img = item.selectFirst("img")
            var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
            if (cover.isNotBlank() && !cover.startsWith("http")) {
                cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
            }

            val author = item.selectFirst("a[href*='/profile/'], .author a, span:contains(by)")?.text()?.trim()
            val stats = author?.let { if (it.startsWith("by", ignoreCase = true)) it else "By $it" }

            val rating = item.selectFirst(".star[title], [aria-label*='stars'], .fiction-rating span")?.let {
                it.attr("title").ifBlank { it.text() }.trim()
            }

            list.add(
                NovelItem(
                    slug = slug,
                    title = title,
                    cover = cover,
                    stats = stats,
                    rating = rating?.take(4),
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(page: Int): NovelHomeData = withContext(Dispatchers.IO) {
        val popular = try { getPopular(page) } catch (_: Exception) { emptyList() }
        val latest = try { getLatest(page) } catch (_: Exception) { emptyList() }
        val lightNovels = try { getLightNovels(page) } catch (_: Exception) { emptyList() }
        val rankings = try { getRankings(page).take(15) } catch (_: Exception) { popular.take(15) }
        NovelHomeData(rankings, popular, latest, lightNovels)
    }

    override suspend fun getRankings(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/fictions/best-rated" else "$baseUrl/fictions/best-rated?page=$page"
        return parseFictionList(fetchHtml(url))
    }

    override suspend fun getPopular(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/fictions/weekly-popular" else "$baseUrl/fictions/weekly-popular?page=$page"
        val list = parseFictionList(fetchHtml(url))
        return if (list.isNotEmpty()) list else getRankings(page)
    }

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/fictions/latest-updates" else "$baseUrl/fictions/latest-updates?page=$page"
        return parseFictionList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/fictions/active-popular" else "$baseUrl/fictions/active-popular?page=$page"
        val list = parseFictionList(fetchHtml(url))
        return if (list.isNotEmpty()) list else getPopular(page)
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/fictions/search?title=$enc" else "$baseUrl/fictions/search?title=$enc&page=$page"
        return parseFictionList(fetchHtml(url))
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val cleanSlug = slug.removePrefix("/").removePrefix("fiction/").trim('/')
        val url = if (cleanSlug.startsWith("http")) cleanSlug else "$baseUrl/fiction/$cleanSlug"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1.font-white, h1, .fiction-title")?.text()?.trim() ?: return null
        val img = doc.selectFirst("img.thumbnail, img[src*='covers-full'], .fiction-info img")
        var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
        }

        val author = doc.selectFirst("h4.font-white a, .author a, a[href*='/profile/']")?.text()?.trim()
        val rating = doc.selectFirst(".star[title], [aria-label*='stars']")?.attr("title")?.take(4)
        val status = doc.select(".label-sm, .fiction-info span.badge").firstOrNull {
            it.text().contains("ONGOING", ignoreCase = true) || it.text().contains("COMPLETED", ignoreCase = true) || it.text().contains("HIATUS", ignoreCase = true)
        }?.text()?.trim() ?: "Ongoing"

        val chaptersCount = doc.select("#chapters tbody tr").size.takeIf { it > 0 }?.let { "$it Chapters" }
        val views = doc.select(".list-unstyled li:contains(Views)").firstOrNull()?.text()?.substringAfter("Views")?.trim()

        val genres = doc.select(".tags a.label, .tags span.label, a.fiction-tag").map { it.text().trim() }.filter { it.isNotBlank() }
        val desc = doc.select(".description p").joinToString("\n\n") { it.text().trim() }.ifBlank {
            doc.select(".description").text().trim()
        }

        val firstChLink = doc.selectFirst("#chapters tbody tr a[href*='/chapter/']")?.attr("href")
        val firstChapterSlug = firstChLink?.removePrefix("/")?.substringBefore("?")

        return NovelDetail(
            slug = cleanSlug,
            title = title,
            cover = cover,
            author = author,
            rating = rating,
            status = status,
            chaptersCount = chaptersCount,
            views = views,
            genres = genres,
            synopsis = desc,
            firstChapterSlug = firstChapterSlug,
            source = source.id
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        val cleanSlug = slug.removePrefix("/").removePrefix("fiction/").trim('/')
        val url = if (cleanSlug.startsWith("http")) cleanSlug else "$baseUrl/fiction/$cleanSlug"
        val doc = fetchHtml(url)

        val list = mutableListOf<NovelChapter>()
        val rows = doc.select("#chapters tbody tr, tr:has(a[href*='/chapter/'])")
        for ((idx, row) in rows.withIndex()) {
            val a = row.selectFirst("a[href*='/chapter/']") ?: continue
            val href = a.attr("href")
            val chSlug = href.removePrefix("/").substringBefore("?").trim()
            if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue

            val name = a.text().trim().ifBlank { "Chapter ${idx + 1}" }
            val time = row.selectFirst("time")?.let { it.attr("title").ifBlank { it.text() }.trim() }

            list.add(
                NovelChapter(
                    slug = chSlug,
                    title = name,
                    number = "${idx + 1}",
                    releaseTime = time
                )
            )
        }
        return Pair(list, 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val url = if (chapterSlug.startsWith("http")) chapterSlug
                  else if (chapterSlug.startsWith("/")) "$baseUrl$chapterSlug"
                  else "$baseUrl/$chapterSlug"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1.font-white, h1, .chapter-title")?.text()?.trim() ?: chapterSlug
        val contentEl = doc.selectFirst(".chapter-content, .chapter-inner, .content") ?: return null
        contentEl.select("script, style, iframe, .ads, .ad, .author-note-portlet, hr").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
        val finalParas = if (paragraphs.isNotEmpty()) paragraphs else contentEl.wholeText().lines().map { it.trim() }.filter { it.isNotBlank() }

        val prevA = doc.selectFirst("a[href*='/chapter/']:contains(Previous), .nav-buttons a:contains(Previous), a:contains(Previous Chapter)")
        val nextA = doc.selectFirst("a[href*='/chapter/']:contains(Next), .nav-buttons a:contains(Next), a:contains(Next Chapter)")

        val prevSlug = prevA?.attr("href")?.removePrefix("/")?.substringBefore("?")?.takeIf { it.isNotBlank() && !it.contains("javascript") }
        val nextSlug = nextA?.attr("href")?.removePrefix("/")?.substringBefore("?")?.takeIf { it.isNotBlank() && !it.contains("javascript") }

        return NovelChapterContent(
            novelSlug = novelSlug,
            chapterSlug = chapterSlug,
            title = title,
            paragraphs = finalParas,
            prevChapterSlug = prevSlug,
            nextChapterSlug = nextSlug
        )
    }
}

// ============================================================================
// 4. WUXIAWORLD REPOSITORY (Premier Asian Web Novel, Cultivation & Xianxia)
// ============================================================================
class WuxiaWorldRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.WUXIA_WORLD
    private val baseUrl = "https://wuxiaworld.site"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private suspend fun fetchHtml(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val response = client.newCall(request).execute()
        val body = response.body.string()
        Jsoup.parse(body, url)
    }

    private fun parseNovelList(doc: Document): List<NovelItem> {
        val list = mutableListOf<NovelItem>()
        val items = doc.select(".c-tabs-item__content, .page-item-detail, .col-6, div:has(.post-title)")
        for (item in items) {
            val a = item.selectFirst(".post-title a, h3.h5 a, a[href*='/novel/']") ?: continue
            val href = a.attr("href")
            val slug = href.substringAfter("/novel/").trim('/')
            if (slug.isBlank() || list.any { it.slug == slug }) continue

            val title = a.text().trim()
            if (title.isBlank()) continue

            val img = item.selectFirst("img")
            var cover = img?.attr("data-src")?.ifBlank { img.attr("src") }.orEmpty()
            if (cover.contains("dflazy.jpg") || cover.isBlank()) {
                cover = img?.attr("srcset")?.split(",")?.lastOrNull()?.trim()?.substringBefore(" ") ?: cover
            }
            if (cover.isNotBlank() && !cover.startsWith("http")) {
                cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
            }

            val author = item.selectFirst(".mg_author a, .author a, .post-author")?.text()?.trim()
            val stats = author?.let { "By $it" }
            val rating = item.selectFirst(".score, .post-total-rating")?.text()?.trim()

            list.add(
                NovelItem(
                    slug = slug,
                    title = title,
                    cover = cover,
                    stats = stats,
                    rating = rating,
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(page: Int): NovelHomeData = withContext(Dispatchers.IO) {
        val popular = try { getPopular(page) } catch (_: Exception) { emptyList() }
        val latest = try { getLatest(page) } catch (_: Exception) { emptyList() }
        val lightNovels = try { getLightNovels(page) } catch (_: Exception) { emptyList() }
        val rankings = try { getRankings(page).take(15) } catch (_: Exception) { popular.take(15) }
        NovelHomeData(rankings, popular, latest, lightNovels)
    }

    override suspend fun getRankings(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/novel/?m_orderby=rating" else "$baseUrl/novel/?m_orderby=rating&paged=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getPopular(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/novel/?m_orderby=views" else "$baseUrl/novel/?m_orderby=views&paged=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/novel/?m_orderby=latest" else "$baseUrl/novel/?m_orderby=latest&paged=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/novel-genre/light-novel/" else "$baseUrl/novel-genre/light-novel/?paged=$page"
        val list = parseNovelList(fetchHtml(url))
        return if (list.isNotEmpty()) list else getPopular(page)
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?s=$enc&post_type=wp-manga" else "$baseUrl/?s=$enc&post_type=wp-manga&paged=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val cleanSlug = slug.substringAfter("/novel/").trim('/')
        val url = "$baseUrl/novel/$cleanSlug/"
        val doc = fetchHtml(url)

        val title = doc.selectFirst(".post-title h1, h1")?.text()?.trim() ?: cleanSlug.replace("-", " ")
        val img = doc.selectFirst(".summary_image img")
        var cover = img?.attr("data-src")?.ifBlank { img.attr("src") }.orEmpty()
        if (cover.contains("dflazy.jpg") || cover.isBlank()) {
            cover = img?.attr("srcset")?.split(",")?.lastOrNull()?.trim()?.substringBefore(" ") ?: cover
        }
        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
        }

        val author = doc.select(".author-content a").text().trim().ifBlank { null }
        val rating = doc.selectFirst(".score, .post-total-rating")?.text()?.trim()
        val status = doc.select(".post-status .summary-content").text().trim().ifBlank { null }
        val genres = doc.select(".genres-content a").map { it.text().trim() }.filter { it.isNotBlank() }

        val desc = doc.select(".description-summary .summary__content p, .description-summary p").joinToString("\n\n") { it.text().trim() }.ifBlank {
            doc.select(".description-summary").text().trim()
        }

        val firstChLink = doc.selectFirst("a[href*='/chapter-1/'], a:contains(Read First)")?.attr("href")
        val firstChapterSlug = firstChLink?.substringAfter("/novel/$cleanSlug/")?.trim('/')

        return NovelDetail(
            slug = cleanSlug,
            title = title,
            cover = cover,
            author = author,
            rating = rating,
            status = status,
            chaptersCount = null,
            views = null,
            genres = genres,
            synopsis = desc,
            firstChapterSlug = firstChapterSlug,
            source = source.id
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        val cleanSlug = slug.substringAfter("/novel/").trim('/')
        val list = mutableListOf<NovelChapter>()

        var chLinks = org.jsoup.select.Elements()
        try {
            val ajaxDoc = fetchHtml("$baseUrl/novel/$cleanSlug/ajax/chapters/")
            chLinks = ajaxDoc.select(".wp-manga-chapter a, li.wp-manga-chapter a, a[href*='chapter']")
        } catch (_: Exception) {}

        if (chLinks.isEmpty()) {
            try {
                val detailUrl = "$baseUrl/novel/$cleanSlug/"
                val doc = fetchHtml(detailUrl)
                chLinks = doc.select(".wp-manga-chapter a, li.wp-manga-chapter a, a[href*='/novel/$cleanSlug/chapter-'], a[href*='chapter']")
            } catch (_: Exception) {}
        }

        // Madara theme lists chapters newest-first, reverse to read chronologically
        val orderedLinks = chLinks.reversed()
        for ((idx, a) in orderedLinks.withIndex()) {
            val href = a.attr("href")
            val chSlug = href.substringAfter("/novel/$cleanSlug/").trim('/')
            if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue

            val name = a.text().trim().ifBlank { "Chapter ${idx + 1}" }
            list.add(
                NovelChapter(
                    slug = chSlug,
                    title = name,
                    number = "${idx + 1}"
                )
            )
        }
        return Pair(list, 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val cleanNovel = novelSlug.substringAfter("/novel/").trim('/')
        val cleanChapter = chapterSlug.substringAfter("/novel/$cleanNovel/").trim('/')
        val url = "$baseUrl/novel/$cleanNovel/$cleanChapter/"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1#chapter-heading, h1, .breadcrumb li.active")?.text()?.trim() ?: cleanChapter.replace("-", " ")
        val contentEl = doc.selectFirst(".reading-content .text-left, .reading-content, .entry-content") ?: return null
        contentEl.select("script, style, iframe, .ads, .ad, .social-share").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
        val finalParas = if (paragraphs.isNotEmpty()) paragraphs else contentEl.wholeText().lines().map { it.trim() }.filter { it.isNotBlank() }

        val prevA = doc.selectFirst(".nav-previous a, a.prev_page")
        val nextA = doc.selectFirst(".nav-next a, a.next_page")

        val prevSlug = prevA?.attr("href")?.substringAfter("/novel/$cleanNovel/")?.trim('/')
        val nextSlug = nextA?.attr("href")?.substringAfter("/novel/$cleanNovel/")?.trim('/')

        return NovelChapterContent(
            novelSlug = cleanNovel,
            chapterSlug = cleanChapter,
            title = title,
            paragraphs = finalParas,
            prevChapterSlug = prevSlug,
            nextChapterSlug = nextSlug
        )
    }
}

// ============================================================================
// 5. WEBNOVEL REPOSITORY (Alias mapping to Royal Road for backward compatibility)
// ============================================================================
class WebNovelRepository(
    private val delegate: RoyalRoadRepository = RoyalRoadRepository()
) : INovelSourceRepository by delegate {
    override val source: NovelSource = NovelSource.WEB_NOVEL
}
