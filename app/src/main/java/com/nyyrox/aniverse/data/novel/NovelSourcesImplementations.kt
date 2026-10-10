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

            val img = item.selectFirst("figure img, img[data-type='cover'], img[src*='covers-'], img")
            var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
            if (cover.contains("nocover-new-min.png")) {
                cover = ""
            }
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
        val rows = doc.select("#chapters tbody tr, tr.chapter-row, tr:has(a[href*='/chapter/']), tr[data-url*='/chapter/']")
        for ((idx, row) in rows.withIndex()) {
            val a = row.selectFirst("td:first-child a, .chapter-row td a, a[href*='/chapter/']")
            val href = a?.attr("href")?.ifBlank { row.attr("data-url") } ?: row.attr("data-url")
            val chSlug = href.removePrefix("/").substringBefore("?").trim()
            if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue

            val name = a?.text()?.trim()?.ifBlank { "Chapter ${idx + 1}" } ?: "Chapter ${idx + 1}"
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

        val paragraphs = mutableListOf<String>()
        val elements = contentEl.children()
        if (elements.isNotEmpty()) {
            for (el in elements) {
                val img = if (el.tagName() == "img") el else el.selectFirst("img")
                if (img != null) {
                    var src = img.attr("src").ifBlank { img.attr("data-src") }.trim()
                    if (src.isNotBlank()) {
                        if (!src.startsWith("http")) src = if (src.startsWith("/")) "$baseUrl$src" else "$baseUrl/$src"
                        paragraphs.add("[img]$src[/img]")
                    }
                }
                val txt = el.text().trim()
                if (txt.isNotBlank()) {
                    paragraphs.add(txt)
                }
            }
        }
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

    private suspend fun fetchHtmlPost(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.RequestBody.create(null, ByteArray(0)))
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
        // Only select item cards, never parent tab wrappers (.c-tabs-item__content)
        val items = doc.select(".page-item-detail, .c-tabs-item__content .col-6, .c-tabs-item__content .col-12, .c-tabs-item__content .row > div, .slider__item, div.row.c-row div.col-6, div:has(> .item-thumb), div:has(> .post-title)")
        for (item in items) {
            // Find true novel title link, skipping genre / badge links like 'novel-genre', 'manga-genre', 'novel-tag'
            val candidates = item.select(".post-title h3 a, .post-title h5 a, .post-title .h5 a, h3.h5 a, h3 a, h5 a, a[href*='/novel/']")
            val a = candidates.firstOrNull { link ->
                val href = link.attr("href")
                !href.contains("genre") && !href.contains("tag") && !href.contains("/chapter") && !link.text().trim().startsWith("Web Novel (", ignoreCase = true)
            } ?: continue
            val href = a.attr("href")
            val slug = href.substringAfter("/novel/").substringBefore("/chapter").substringBefore("?").trim('/')
            if (slug.isBlank() || slug.startsWith("genre") || slug.startsWith("tag") || list.any { it.slug == slug }) continue

            val title = a.text().trim()
            if (title.isBlank() || title.equals("HOT", true) || title.equals("NEW", true)) continue

            val img = item.selectFirst(".item-thumb img, img:not([src*='logo']):not([src*='avatar'])") ?: item.selectFirst("img")
            var cover = img?.attr("data-src")?.ifBlank { img.attr("src") }.orEmpty()
            if (cover.contains("dflazy.jpg") || cover.isBlank() || cover.contains("logo") || cover.contains("banner")) {
                cover = img?.attr("srcset")?.split(",")?.lastOrNull()?.trim()?.substringBefore(" ") ?: cover
            }
            if (cover.contains("logo") || cover.contains("banner")) {
                cover = ""
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
        val cleanSlug = slug.substringAfter("/novel/").substringBefore("/chapter").trim('/')
        val list = mutableListOf<NovelChapter>()

        var chLinks = org.jsoup.select.Elements()
        try {
            val ajaxDoc = fetchHtmlPost("$baseUrl/novel/$cleanSlug/ajax/chapters/")
            chLinks = ajaxDoc.select(".wp-manga-chapter a, li.wp-manga-chapter a, a[href*='chapter']")
        } catch (_: Exception) {}

        if (chLinks.isEmpty()) {
            try {
                val ajaxDoc = fetchHtml("$baseUrl/novel/$cleanSlug/ajax/chapters/")
                chLinks = ajaxDoc.select(".wp-manga-chapter a, li.wp-manga-chapter a, a[href*='chapter']")
            } catch (_: Exception) {}
        }

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
        val cleanNovel = novelSlug.substringAfter("/novel/").substringBefore("/chapter").trim('/')
        val cleanChapter = chapterSlug.substringAfter("/novel/$cleanNovel/").trim('/')
        val url = "$baseUrl/novel/$cleanNovel/$cleanChapter/"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1#chapter-heading, h1, .breadcrumb li.active")?.text()?.trim() ?: cleanChapter.replace("-", " ")
        val contentEl = doc.selectFirst(".reading-content .text-left, .reading-content, .entry-content") ?: return null
        contentEl.select("script, style, iframe, .ads, .ad, .social-share").remove()

        val paragraphs = mutableListOf<String>()
        val elements = contentEl.children()
        if (elements.isNotEmpty()) {
            for (el in elements) {
                val img = if (el.tagName() == "img") el else el.selectFirst("img")
                if (img != null) {
                    var src = img.attr("data-src").ifBlank { img.attr("src") }.trim()
                    if (src.isNotBlank()) {
                        if (!src.startsWith("http")) src = if (src.startsWith("/")) "$baseUrl$src" else "$baseUrl/$src"
                        paragraphs.add("[img]$src[/img]")
                    }
                }
                val txt = el.text().trim()
                if (txt.isNotBlank()) {
                    paragraphs.add(txt)
                }
            }
        }
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

// ============================================================================
// 6. FREEWEBNOVEL REPOSITORY (Premier Light Novel & Web Novel Source)
// ============================================================================
class FreeWebNovelRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.FREE_WEB_NOVEL
    private val baseUrl = "https://freewebnovel.com"
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

    private suspend fun searchPost(keyword: String): Document = withContext(Dispatchers.IO) {
        val formBody = okhttp3.FormBody.Builder()
            .add("keyword", keyword)
            .build()
        val request = Request.Builder()
            .url("$baseUrl/search")
            .post(formBody)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", baseUrl)
            .build()
        val response = client.newCall(request).execute()
        val body = response.body.string()
        Jsoup.parse(body, "$baseUrl/search")
    }

    private fun parseList(doc: Document): List<NovelItem> {
        val list = mutableListOf<NovelItem>()
        val items = doc.select(".li-row, .col-xs-12:has(.tit), div:has(h3.tit), .ul-list1 li")
        for (item in items) {
            val a = item.selectFirst("h3.tit a, .tit a, a[href*='/novel/']") ?: continue
            val href = a.attr("href")
            val slug = href.substringAfter("/novel/").substringBefore("/chapter").substringBefore("?").trim('/')
            if (slug.isBlank() || list.any { it.slug == slug }) continue

            val title = a.attr("title").ifBlank { a.text() }.trim()
            if (title.isBlank()) continue

            val img = item.selectFirst("img")
            var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
            if (cover.isNotBlank() && !cover.startsWith("http")) {
                cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
            }

            val author = item.selectFirst(".author a, .item-author a")?.text()?.trim()
            val stats = author?.let { "By $it" }

            list.add(
                NovelItem(
                    slug = slug,
                    title = title,
                    cover = cover,
                    stats = stats,
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
        val url = if (page <= 1) "$baseUrl/most-popular-novel" else "$baseUrl/most-popular-novel/$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun getPopular(page: Int): List<NovelItem> = getRankings(page)

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/latest-release-novel" else "$baseUrl/latest-release-novel/$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/genre/Fantasy" else "$baseUrl/genre/Fantasy/$page"
        return parseList(fetchHtml(url))
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        return try {
            parseList(searchPost(query))
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val cleanSlug = slug.substringAfter("/novel/").substringBefore("/chapter").trim('/')
        val url = "$baseUrl/novel/$cleanSlug"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1.tit, h1, .m-desc h1")?.text()?.trim() ?: cleanSlug.replace("-", " ")
        val img = doc.selectFirst(".pic img, .m-imgwrap img, .m-desc img")
        var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
        }

        val author = doc.selectFirst(".author a, span:contains(Author) + a")?.text()?.trim()
        val genres = doc.select(".txt .item:contains(Genre) a, .m-desc .txt a[href*='/genre/']").map { it.text().trim() }
        val desc = doc.select(".inner p, .txt .m-desc p, .desc p").joinToString("\n\n") { it.text().trim() }.ifBlank {
            doc.select(".inner, .m-desc").text().trim()
        }

        val firstChLink = doc.selectFirst("a[href*='/novel/$cleanSlug/chapter-1']")?.attr("href")
            ?: doc.selectFirst("a[href*='/novel/$cleanSlug/chapter-']")?.attr("href")
        val firstChapterSlug = firstChLink?.substringAfter("/novel/$cleanSlug/")?.trim('/')

        return NovelDetail(
            slug = cleanSlug,
            title = title,
            cover = cover,
            author = author,
            rating = null,
            status = null,
            chaptersCount = null,
            views = null,
            genres = genres,
            synopsis = desc,
            firstChapterSlug = firstChapterSlug,
            source = source.id
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        val cleanSlug = slug.substringAfter("/novel/").substringBefore("/chapter").trim('/')
        val url = "$baseUrl/novel/$cleanSlug"
        val doc = fetchHtml(url)

        val list = mutableListOf<NovelChapter>()
        val links = doc.select("a[href*='/novel/$cleanSlug/chapter-']")
        for ((idx, a) in links.withIndex()) {
            val href = a.attr("href")
            val chSlug = href.substringAfter("/novel/$cleanSlug/").trim('/')
            if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue

            val name = a.attr("title").ifBlank { a.text() }.trim().ifBlank { "Chapter ${idx + 1}" }
            list.add(
                NovelChapter(
                    slug = chSlug,
                    title = name,
                    releaseTime = null
                )
            )
        }
        return Pair(list, 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val cleanNovel = novelSlug.substringAfter("/novel/").trim('/')
        val cleanChapter = chapterSlug.substringAfter("/novel/$cleanNovel/").trim('/')
        val url = "$baseUrl/novel/$cleanNovel/$cleanChapter"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1.tit, h2.tit, .m-read h1, h1")?.text()?.trim() ?: "Chapter"
        val contentEl = doc.selectFirst(".txt, .m-read .content, #article, .m-desc") ?: return null
        contentEl.select("script, style, iframe, .ads, .ad").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
        val finalParas = if (paragraphs.isNotEmpty()) paragraphs else contentEl.wholeText().lines().map { it.trim() }.filter { it.isNotBlank() }

        val prevA = doc.selectFirst("a[href*='/novel/$cleanNovel/']:contains(Prev), a.prev_page")
        val nextA = doc.selectFirst("a[href*='/novel/$cleanNovel/']:contains(Next), a.next_page")

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

