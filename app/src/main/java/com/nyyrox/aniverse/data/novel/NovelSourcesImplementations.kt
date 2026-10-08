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
// 3. WEBNOVEL REPOSITORY (WebNovel & Mirror Integration)
// ============================================================================
class WebNovelRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.WEB_NOVEL
    private val baseUrl = "https://novelfire.net"
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
        val items = doc.select(".novel-item, .item, .col-truyen-main .row, .row:has(a[href*='/book/']), li:has(a[href*='/book/'])")
        for (item in items) {
            val a = item.selectFirst("a[href*='/book/']") ?: continue
            val href = a.attr("href")
            val slug = href.substringAfter("/book/").substringBefore("?").substringBefore("#").trim('/')
            if (slug.isBlank() || slug.contains("chapters") || list.any { it.slug == slug }) continue

            val titleEl = item.selectFirst(".novel-title, .title, h3, h4") ?: a
            val title = titleEl.text().trim()
            if (title.isBlank()) continue

            val img = item.selectFirst("img")
            var cover = img?.attr("src")?.ifBlank { img.attr("data-src") }.orEmpty()
            if (cover.isBlank() || cover.contains("base64") || cover.endsWith(".svg")) {
                cover = "$baseUrl/server-1/$slug.jpg"
            } else if (!cover.startsWith("http")) {
                if (cover.startsWith("//")) cover = "https:$cover"
                else cover = "$baseUrl/$cover"
            }

            val author = item.selectFirst(".author, .g_author, .book-author")?.text()?.trim()
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
        val url = if (page <= 1) "$baseUrl/sort/top-rated-novel" else "$baseUrl/sort/top-rated-novel?page=$page"
        val doc = fetchHtml(url)
        val parsed = parseNovelList(doc)
        return if (parsed.isNotEmpty()) parsed else getPopular(page)
    }

    override suspend fun getPopular(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/sort/popular-novel" else "$baseUrl/sort/popular-novel?page=$page"
        val doc = fetchHtml(url)
        val parsed = parseNovelList(doc)
        return if (parsed.isNotEmpty()) parsed else parseNovelList(fetchHtml("$baseUrl/sort/most-viewed-novel"))
    }

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/sort/latest-novel" else "$baseUrl/sort/latest-novel?page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/genre/light-novel-16" else "$baseUrl/genre/light-novel-16?page=$page"
        val doc = fetchHtml(url)
        val parsed = parseNovelList(doc)
        return if (parsed.isNotEmpty()) parsed else getPopular(page)
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val enc = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?keyword=$enc"
        val doc = fetchHtml(url)
        val parsed = parseNovelList(doc)
        if (parsed.isNotEmpty()) return parsed

        // Fallback: parse raw links if container class was different
        val list = mutableListOf<NovelItem>()
        val aLinks = doc.select("a[href*='/book/']")
        for (a in aLinks) {
            val href = a.attr("href")
            val slug = href.substringAfter("/book/").substringBefore("?").trim('/')
            if (slug.isBlank() || slug.contains("chapters") || list.any { it.slug == slug }) continue
            val title = a.text().trim()
            if (title.isBlank() || title.length < 3) continue
            val cover = "$baseUrl/server-1/$slug.jpg"
            list.add(NovelItem(slug = slug, title = title, cover = cover, source = source.id))
        }
        return list
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val cleanSlug = slug.substringAfter("/book/").substringBefore("?").trim('/')
        val url = "$baseUrl/book/$cleanSlug"
        val doc = fetchHtml(url)

        val title = doc.selectFirst(".novel-title, h1")?.text()?.trim()
            ?: cleanSlug.replace("-", " ").replaceFirstChar { it.uppercase() }

        var cover = doc.selectFirst("figure.cover img, img[src*='cover'], img[src*='uploads']")?.attr("src")?.ifBlank {
            doc.selectFirst("img[data-src]")?.attr("data-src")
        }.orEmpty()
        if (cover.isBlank() || cover.contains("base64") || cover.endsWith(".svg")) {
            cover = "$baseUrl/server-1/$cleanSlug.jpg"
        } else if (!cover.startsWith("http")) {
            if (cover.startsWith("//")) cover = "https:$cover"
            else cover = "$baseUrl/$cover"
        }

        val author = doc.selectFirst("a[href*='/author/'], .author")?.text()?.trim()
        val genres = doc.select("a[href*='/genre/'], .genre a").map { it.text().trim() }.filter { it.isNotBlank() }
        val status = doc.selectFirst(".status, .novel-status")?.text()?.trim() ?: "Ongoing"
        val desc = doc.select(".description, .summary, .content").joinToString("\n\n") { it.text().trim() }.ifBlank {
            doc.select("p").take(3).joinToString("\n\n") { it.text().trim() }
        }

        return NovelDetail(
            slug = cleanSlug,
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
        val cleanSlug = slug.substringAfter("/book/").substringBefore("?").trim('/')
        val url = "$baseUrl/book/$cleanSlug/chapters"
        val doc = fetchHtml(url)

        val list = mutableListOf<NovelChapter>()
        val chLinks = doc.select("a[href*='/chapter-'], a[href*='/book/']")
        for ((idx, a) in chLinks.withIndex()) {
            val href = a.attr("href")
            val chSlug = if (href.contains("/book/")) {
                href.substringAfter("/book/").substringBefore("?").trim('/')
            } else {
                href.substringAfter("/chapter-").substringBefore("?").trim('/')
            }
            if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue

            var name = a.text().trim()
            name = Jsoup.parse(name).text().trim()
            if (name.isBlank()) name = "Chapter ${idx + 1}"

            list.add(
                NovelChapter(
                    slug = chSlug,
                    title = name,
                    number = "${idx + 1}"
                )
            )
        }

        if (list.isEmpty()) {
            // Fallback: fetch main detail page for chapter links
            val dDoc = fetchHtml("$baseUrl/book/$cleanSlug")
            val dLinks = dDoc.select("a[href*='/chapter-']")
            for ((idx, a) in dLinks.withIndex()) {
                val href = a.attr("href")
                val chSlug = href.substringAfter("/book/").substringBefore("?").trim('/')
                if (chSlug.isBlank() || list.any { it.slug == chSlug }) continue
                list.add(NovelChapter(slug = chSlug, title = a.text().trim().ifBlank { "Chapter ${idx + 1}" }, number = "${idx + 1}"))
            }
        }

        return Pair(list, 1)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val cleanNovel = novelSlug.substringAfter("/book/").substringBefore("?").trim('/')
        val cleanChapter = chapterSlug.substringAfter("/book/").substringBefore("?").trim('/')
        val url = if (cleanChapter.contains("/")) "$baseUrl/book/$cleanChapter" else "$baseUrl/book/$cleanNovel/$cleanChapter"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h2, h1, .chapter-title")?.text()?.trim() ?: cleanChapter.replace("-", " ")
        val contentEl = doc.selectFirst(".content, #chapter-content, .chapter-content, .desc") ?: doc.body()
        contentEl.select("script, style, iframe, .ads, .pirate-wrapper, .icon-megaphone").remove()

        val paragraphs = contentEl.select("p").map { it.text().trim() }
            .filter { it.isNotBlank() && !it.startsWith("To comply with copyright", ignoreCase = true) }

        val finalParas = if (paragraphs.isNotEmpty()) {
            paragraphs
        } else {
            contentEl.wholeText().lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("To comply with copyright", ignoreCase = true) }
        }

        val prevA = doc.selectFirst("a[href*='/chapter-']:has(.icon-left-open), a:contains(Prev), a.prev-btn")
        val nextA = doc.selectFirst("a[href*='/chapter-']:has(.icon-right-open), a:contains(Next), a.next-btn")

        val prevSlug = prevA?.attr("href")?.substringAfter("/book/")?.substringBefore("?")?.trim('/')
        val nextSlug = nextA?.attr("href")?.substringAfter("/book/")?.substringBefore("?")?.trim('/')

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
