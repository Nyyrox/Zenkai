package com.nyyrox.aniverse.data.novel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NovelFireRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : INovelSourceRepository {
    override val source: NovelSource = NovelSource.NOVEL_FIRE
    private val baseUrl = "https://novelfire.net"
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

    private fun parseNovelList(doc: Document): List<NovelItem> {
        val list = mutableListOf<NovelItem>()
        val cards = doc.select(".novel-item, li.item, .list-novel .row, .book-item")
        val items = if (cards.isNotEmpty()) cards else doc.select("li:has(a[href*=/book/])")

        for (c in items) {
            val a = c.selectFirst("a[href*=/book/]") ?: continue
            val href = a.attr("href")
            val slug = href.substringAfter("/book/").substringBefore("/").substringBefore("?")
            if (slug.isBlank()) continue

            val titleEl = c.selectFirst(".novel-title, .title, h3, h4, h5") ?: a
            val title = titleEl.text().trim().ifBlank { a.attr("title").trim() }
            if (title.isBlank()) continue

            val img = c.selectFirst("img")
            var cover = img?.attr("data-src")?.ifBlank { img.attr("src") }.orEmpty()
            if (cover.isNotBlank() && !cover.startsWith("http")) {
                cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
            }

            val stats = c.select(".novel-stats, .stats, .chapter-count, .item-body span")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString(" • ")

            val rating = c.selectFirst(".rating, .nub, .rate, [data-rating]")?.text()?.trim()

            list.add(
                NovelItem(
                    slug = slug,
                    title = title,
                    cover = cover,
                    stats = stats.ifBlank { null },
                    rating = rating?.ifBlank { null },
                    source = source.id
                )
            )
        }
        return list
    }

    override suspend fun getHome(page: Int): NovelHomeData = withContext(Dispatchers.IO) {
        val rankings = try { getRankings(page) } catch (_: Exception) { emptyList() }
        val popular = try { getPopular(page) } catch (_: Exception) { emptyList() }
        val latest = try { getLatest(page) } catch (_: Exception) { emptyList() }
        val lightNovels = try { getLightNovels(page) } catch (_: Exception) { emptyList() }
        NovelHomeData(rankings, popular, latest, lightNovels)
    }

    override suspend fun getRankings(page: Int): List<NovelItem> {
        val url = if (page <= 1) "$baseUrl/ranking" else "$baseUrl/ranking?page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getPopular(page: Int): List<NovelItem> {
        val url = "$baseUrl/genre-all/sort-popular/status-all/all-novel?page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getLatest(page: Int): List<NovelItem> {
        val url = "$baseUrl/genre-all/sort-new/status-all/all-novel?page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getLightNovels(page: Int): List<NovelItem> {
        val url = "$baseUrl/genre-light-novel/sort-popular/status-all/all-novel?page=$page"
        val list = parseNovelList(fetchHtml(url))
        return if (list.isNotEmpty()) list else getByGenre("japanese", page)
    }

    suspend fun getByGenre(genre: String, page: Int = 1): List<NovelItem> {
        val slug = genre.lowercase().replace(" ", "-")
        val url = "$baseUrl/genre-$slug/sort-popular/status-all/all-novel?page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun search(query: String, page: Int): List<NovelItem> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?keyword=$encoded&page=$page"
        return parseNovelList(fetchHtml(url))
    }

    override suspend fun getDetail(slug: String): NovelDetail? {
        val url = "$baseUrl/book/$slug"
        val doc = fetchHtml(url)

        val title = doc.selectFirst("h1.novel-title, h1, .main-head h1")?.text()?.trim().orEmpty()
        
        val coverImg = doc.selectFirst(".novel-cover img, .cover img, .novel-info img, img[itemprop=image]")
        var cover = coverImg?.attr("data-src")?.ifBlank { coverImg.attr("src") }.orEmpty()
        if (cover.isNotBlank() && !cover.startsWith("http")) {
            cover = baseUrl + (if (cover.startsWith("/")) "" else "/") + cover
        }

        val author = doc.selectFirst(".author [itemprop=author], .author a, .author span")?.text()?.trim()
        val rating = doc.selectFirst(".rating .nub, .rating-star, [data-rating]")?.text()?.trim()

        var status: String? = null
        var chaptersCount: String? = null
        var views: String? = null

        doc.select(".header-stats span, .novel-stats span").forEach { stat ->
            val text = stat.text().trim()
            when {
                text.contains("Chapter", ignoreCase = true) -> chaptersCount = text
                text.contains("View", ignoreCase = true) -> views = text
                text.contains("Status", ignoreCase = true) || text.contains("Ongoing", ignoreCase = true) || text.contains("Completed", ignoreCase = true) -> status = text.replace("Status", "").trim()
            }
        }

        val genres = doc.select(".categories a, .genres a").map { it.text().trim() }.filter { it.isNotBlank() }

        val synopsisEl = doc.selectFirst(".summary .content, .summary, .description, .novel-summary, .novel-desc")
        val synopsis = synopsisEl?.text()?.trim().orEmpty().removePrefix("Summary").trim()

        val firstChLink = doc.selectFirst("a.button[href*=/chapter-], nav.links a[href*=/chapter-], a[href*=/book/$slug/chapter-1]")?.attr("href")
        val firstChapterSlug = firstChLink?.substringAfter("/book/$slug/")?.substringBefore("?")

        return NovelDetail(
            slug = slug,
            title = title.ifBlank { slug.replace("-", " ").capitalizeWords() },
            cover = cover,
            author = author,
            rating = rating,
            status = status,
            chaptersCount = chaptersCount,
            views = views,
            genres = genres,
            synopsis = synopsis,
            firstChapterSlug = firstChapterSlug
        )
    }

    override suspend fun getChapters(slug: String, page: Int): Pair<List<NovelChapter>, Int> {
        val url = "$baseUrl/book/$slug/chapters?page=$page"
        val doc = fetchHtml(url)

        val list = mutableListOf<NovelChapter>()
        doc.select("a[href*=/book/$slug/chapter-]").forEach { a ->
            val href = a.attr("href")
            val chSlug = href.substringAfter("/book/$slug/").substringBefore("?").trim()
            val text = a.text().trim()
            if (chSlug.isNotBlank()) {
                list.add(
                    NovelChapter(
                        slug = chSlug,
                        title = text,
                        number = chSlug.removePrefix("chapter-")
                    )
                )
            }
        }

        // Calculate total pages from pagination
        var maxPage = page
        doc.select(".pagination a, ul.pagination a").forEach { pagLink ->
            val pageParam = pagLink.attr("href").substringAfter("page=").toIntOrNull()
            if (pageParam != null && pageParam > maxPage) {
                maxPage = pageParam
            }
        }

        return Pair(list, maxPage)
    }

    override suspend fun getChapterContent(novelSlug: String, chapterSlug: String): NovelChapterContent? {
        val url = "$baseUrl/book/$novelSlug/$chapterSlug"
        val doc = fetchHtml(url)

        val contentDiv = doc.selectFirst("#content, #chapter-content, .chapter-content, .content-inner")
        contentDiv?.select("script, style, iframe, .ads, .adsbygoogle, .ad-box, .novel-info")?.remove()

        val title = doc.selectFirst("h1.chapter-title, h2.chapter-title, .titles h2, h1")?.text()?.trim()
            ?: chapterSlug.replace("-", " ").capitalizeWords()

        val paragraphs = mutableListOf<String>()
        if (contentDiv != null) {
            val pTags = contentDiv.select("p")
            if (pTags.isNotEmpty()) {
                pTags.forEach { p ->
                    val text = p.text().trim()
                    if (text.isNotBlank() && !text.startsWith("NovelFire") && !text.contains("window.adsbygoogle")) {
                        paragraphs.add(text)
                    }
                }
            } else {
                // Split by line breaks if no <p> tags
                contentDiv.wholeText().lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank()) {
                        paragraphs.add(trimmed)
                    }
                }
            }
        }

        var prevSlug: String? = null
        var nextSlug: String? = null

        doc.select("a[href*=/book/$novelSlug/chapter-]").forEach { a ->
            val txt = a.text().lowercase()
            val href = a.attr("href")
            val target = href.substringAfter("/book/$novelSlug/").substringBefore("?").trim()
            if (target.isNotBlank() && target != chapterSlug) {
                if (txt.contains("prev") || a.attr("class").contains("prev")) {
                    prevSlug = target
                } else if (txt.contains("next") || a.attr("class").contains("next")) {
                    nextSlug = target
                }
            }
        }

        // Infer next/prev by number if not explicitly in HTML
        val currentNum = chapterSlug.removePrefix("chapter-").toIntOrNull()
        if (currentNum != null) {
            if (prevSlug == null && currentNum > 1) {
                prevSlug = "chapter-${currentNum - 1}"
            }
            if (nextSlug == null) {
                nextSlug = "chapter-${currentNum + 1}"
            }
        }

        return NovelChapterContent(
            novelSlug = novelSlug,
            chapterSlug = chapterSlug,
            title = title,
            paragraphs = paragraphs,
            prevChapterSlug = prevSlug,
            nextChapterSlug = nextSlug
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
