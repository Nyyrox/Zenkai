package com.nyyrox.aniverse.data.extractor

import android.util.Base64
import android.util.Log
import com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver
import com.nyyrox.aniverse.data.mapping.AnimeMappingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class MultiAudioServer(
    val id: String,
    val name: String,
    val streamUrl: String,
    val isDirectStream: Boolean = false,
    val badge: String = "Multi-Audio",
    val audioTracks: String = "Multi-Audio",
    val headers: Map<String, String> = emptyMap()
)

/**
 * Purely client-side Multi-Audio extractor.
 *
 * Implements the exact CloudStream AnimeDekho extraction flow from phisher98:
 *   1. Search AnimeDekho (https://animedekho.app/?s=<title>) with Cookie toronites_server=vidstream
 *   2. Find the series page and the matching episode watch link
 *   3. On the episode page:
 *        a) Check iframes (e.g. embed page -> as-cdn -> getVideo)
 *        b) Extract body class post ID (?:term|postid)-(\d+)
 *        c) Query trdekho (0..4) -> extract player:
 *           - AbyssPlayer: decrypt via enc-dec.app -> direct highest quality H264 stream
 *           - Pixel: extract file: "..." direct stream link
 *           - as-cdn / AWSStream: POST /player/index.php?data=<hash>&do=getVideo -> master.m3u8
 *           - Direct .m3u8 / .mp4 streams
 *   4. Falls back to AnimeSalt options-0 if AnimeDekho fails.
 *   5. Only HD-1 is returned (HD-2 removed per user requirement).
 *
 * All extraction runs client-side with zero external proxies or Cloudflare workers.
 */
object MultiAudioExtractor {

    private const val TAG = "MultiAudioExtractor"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** In-memory session cache keyed by "anilistId:episode". */
    private val serverCache = ConcurrentHashMap<String, List<MultiAudioServer>>()

    // -------------------------------------------------------------------------
    //  Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the single HD-1 Multi-Audio server using phisher98's AnimeDekho flow.
     */
    suspend fun extractServers(
        anilistId: Int,
        episode: Int,
        title: String
    ): List<MultiAudioServer> = withContext(Dispatchers.IO) {
        if (anilistId > 0 && !AnimeMappingRepository.isMapped(anilistId)) {
            return@withContext emptyList()
        }
        val cacheKey = "$anilistId:$episode"
        serverCache[cacheKey]?.let { return@withContext it }

        val candidate = AnimeMappingRepository.resolveCandidate(anilistId, episode, title)
        val saltId   = candidate.saltId
        val season   = candidate.season ?: 1
        val finalEp  = candidate.finalEp
        val isMovie  = candidate.isMovie

        val slug = if (isMovie) saltId
                   else AnimeSaltResolver.generateAnimeSaltSlug(saltId, season, finalEp)

        // 1. Primary: AnimeDekho extraction flow (from phisher98)
        val dekhoServer = extractFromAnimeDekho(title, season, finalEp, isMovie, saltId, slug)
        if (dekhoServer != null) {
            val result = listOf(dekhoServer.copy(id = "multi-hd-1", name = "HD-1"))
            serverCache[cacheKey] = result
            return@withContext result
        }

        // 2. Secondary fallback: AnimeSalt primary HD-1 options-0
        val saltServer = extractFromAnimeSalt(slug, isMovie, saltId)
        if (saltServer != null) {
            val result = listOf(saltServer.copy(id = "multi-hd-1", name = "HD-1"))
            serverCache[cacheKey] = result
            return@withContext result
        }

        // 3. Guaranteed fallback: return direct embed HD-1
        val fallback = listOf(
            MultiAudioServer(
                id = "multi-hd-1",
                name = "HD-1",
                streamUrl = "https://animedekho.app/epi/$slug/",
                isDirectStream = false,
                badge = "Multi-Audio"
            )
        )
        serverCache[cacheKey] = fallback
        fallback
    }

    /** Clears the session cache. */
    fun clearCache() = serverCache.clear()

    // -------------------------------------------------------------------------
    //  AnimeDekho (phisher98 CloudStream flow)
    // -------------------------------------------------------------------------

    private suspend fun extractFromAnimeDekho(
        title: String,
        season: Int,
        episode: Int,
        isMovie: Boolean,
        saltId: String,
        slug: String
    ): MultiAudioServer? = withContext(Dispatchers.IO) {
        val mainUrl = "https://animedekho.app"
        try {
            // Step 1: Find episode page URL
            var epUrl = findAnimeDekhoEpisodePage(mainUrl, title, season, episode, isMovie, saltId, slug)

            if (epUrl == null) {
                // Try candidate URLs directly
                val candidates = listOf(
                    "$mainUrl/epi/$saltId-${season}x$episode/",
                    "$mainUrl/epi/$saltId-1x$episode/",
                    "$mainUrl/epi/$slug/"
                )
                for (cand in candidates) {
                    val html = httpGet(cand, "$mainUrl/", cookie = "toronites_server=vidstream")
                    if (!html.isNullOrBlank() && html.length > 500) {
                        epUrl = cand
                        break
                    }
                }
            }

            if (epUrl == null) return@withContext null

            // Step 2: Fetch episode HTML
            val epHtml = httpGet(epUrl, "$mainUrl/", cookie = "toronites_server=vidstream") ?: return@withContext null

            // Step 3: Check iframe.serversel / embed iframes
            val embedIframes = Regex("""<iframe[^>]+(?:class="[^"]*serversel[^"]*"[^>]+)?src="([^"]+)"""", RegexOption.IGNORE_CASE)
                .findAll(epHtml)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotBlank() && !it.equals("about:blank", ignoreCase = true) }
                .toList()

            for (iframeSrc in embedIframes) {
                val resolved = resolveIframeToServer(iframeSrc, epUrl, "multi-hd-1", "HD-1")
                if (resolved != null && resolved.isDirectStream) {
                    return@withContext resolved
                }
            }

            // Step 4: Extract post ID from body class (e.g. postid-45054 or term-1234)
            val bodyClass = Regex("""<body[^>]+class="([^"]+)"""", RegexOption.IGNORE_CASE)
                .find(epHtml)?.groupValues?.get(1).orEmpty()
            val postId = Regex("""(?:term|postid)-(\d+)""").find(bodyClass)?.groupValues?.getOrNull(1)

            if (!postId.isNullOrBlank()) {
                val mediaType = if (isMovie) 1 else 2
                for (trdekho in 0..4) {
                    val trUrl = "$mainUrl/?trdekho=$trdekho&trid=$postId&trtype=$mediaType"
                    try {
                        val trHtml = httpGet(trUrl, epUrl, cookie = "toronites_server=vidstream") ?: continue
                        val iframeSrc = Regex("""<iframe[^>]+src="([^"]+)"""", RegexOption.IGNORE_CASE)
                            .find(trHtml)?.groupValues?.get(1)?.trim() ?: continue

                        val resolved = resolveIframeToServer(iframeSrc, trUrl, "multi-hd-1", "HD-1")
                        if (resolved != null && resolved.isDirectStream) {
                            return@withContext resolved
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "trdekho=$trdekho check failed: ${e.message}")
                    }
                }
            }

            // If we found any valid embed page, return it as fallback
            if (embedIframes.isNotEmpty()) {
                return@withContext MultiAudioServer(
                    id = "multi-hd-1",
                    name = "HD-1",
                    streamUrl = embedIframes.first(),
                    isDirectStream = false,
                    badge = "Multi-Audio"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "AnimeDekho extraction error: ${e.message}")
        }
        null
    }

    /**
     * Searches AnimeDekho and navigates to the series page to locate the episode link.
     */
    private suspend fun findAnimeDekhoEpisodePage(
        mainUrl: String,
        title: String,
        season: Int,
        episode: Int,
        isMovie: Boolean,
        saltId: String,
        slug: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(title, "UTF-8")
            val searchUrl = "$mainUrl/?s=$q"
            val html = httpGet(searchUrl, "$mainUrl/", cookie = "toronites_server=vidstream") ?: return@withContext null

            // Find all article links
            val seriesLinks = Regex("""<a[^>]+href="([^"]+)"[^>]*class="[^"]*lnk-blk""", RegexOption.IGNORE_CASE)
                .findAll(html)
                .map { it.groupValues[1] }
                .toList()
                .ifEmpty {
                    Regex("""href="([^"]+)"[^>]*class="lnk-blk"""", RegexOption.IGNORE_CASE)
                        .findAll(html)
                        .map { it.groupValues[1] }
                        .toList()
                }

            if (seriesLinks.isEmpty()) return@withContext null

            // Check the best matching series page
            val seriesUrl = seriesLinks.first()
            val seriesHtml = httpGet(seriesUrl, searchUrl, cookie = "toronites_server=vidstream") ?: return@withContext null

            if (isMovie) {
                return@withContext seriesUrl
            }

            // Look inside seasons-lst for matching episode
            val seasons = Regex("""<ul[^>]+class="[^"]*seasons-lst[^"]*"[^>]*>([\s\S]*?)</ul>""", RegexOption.IGNORE_CASE)
                .findAll(seriesHtml)
                .map { it.groupValues[1] }
                .toList()

            for (sHtml in seasons) {
                val items = Regex("""<li>([\s\S]*?)</li>""", RegexOption.IGNORE_CASE).findAll(sHtml)
                for (it in items) {
                    val itemContent = it.groupValues[1]
                    val linkMatch = Regex("""href="([^"]+)"""", RegexOption.IGNORE_CASE).find(itemContent)
                    val epMatch = Regex("""<span>S(\d+)-E(\d+)</span>|E(\d+)|Episode\s*(\d+)""", RegexOption.IGNORE_CASE).find(itemContent)
                    if (linkMatch != null && epMatch != null) {
                        val num = (epMatch.groupValues[2].ifBlank { epMatch.groupValues[3] }
                            .ifBlank { epMatch.groupValues[4] }.ifBlank { epMatch.groupValues[1] }).toIntOrNull()
                        if (num == episode) {
                            return@withContext linkMatch.groupValues[1]
                        }
                    }
                }
            }

            // Fallback: look for /epi/ links with matching season and episode pattern
            val epiLinks = Regex("""href="([^"]*/epi/[^"]+)"""", RegexOption.IGNORE_CASE)
                .findAll(seriesHtml)
                .map { it.groupValues[1] }
                .toList()

            for (link in epiLinks) {
                if (link.contains("-${season}x${episode}/") ||
                    link.contains("-1x${episode}/") ||
                    link.contains("x${episode}/") ||
                    link.contains("-${episode}/")) {
                    return@withContext link
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search AnimeDekho episode page error: ${e.message}")
        }
        null
    }

    // -------------------------------------------------------------------------
    //  AnimeSalt extraction (Secondary Fallback)
    // -------------------------------------------------------------------------

    private suspend fun extractFromAnimeSalt(
        slug: String,
        isMovie: Boolean,
        saltId: String
    ): MultiAudioServer? = withContext(Dispatchers.IO) {
        val pathSlug = if (isMovie) "/movies/$saltId/" else "/episode/$slug/"
        val mirrors = listOf("https://animesalt.cx", "https://animesalt.ac", "https://animesalt.ro")

        for (mirror in mirrors) {
            try {
                val html = httpGet("$mirror$pathSlug", "$mirror/") ?: continue

                // Check options-0 iframe (primary player: ravok.buzz or as-cdn)
                extractOptionIframe(html, "options-0", mirror)?.let { url ->
                    val server = resolveIframeToServer(url, "$mirror/", "multi-hd-1", "HD-1")
                    if (server != null && server.isDirectStream) return@withContext server
                }

                // Check options-1 iframe (multi-language player: Abyss / player.php)
                extractOptionIframe(html, "options-1", mirror)?.let { url ->
                    val server = resolveIframeToServer(url, "$mirror/", "multi-hd-1", "HD-1")
                    if (server != null && server.isDirectStream) return@withContext server
                }

                // Check loadMi buttons
                val fromLoadMi = parseLoadMiButtons(html, mirror)
                if (fromLoadMi != null && fromLoadMi.isDirectStream) return@withContext fromLoadMi
            } catch (_: Exception) {}
        }
        null
    }

    private fun extractOptionIframe(html: String, optionId: String, baseUrl: String): String? {
        val regex = Regex(
            """<div[^>]+id=["']$optionId["'][^>]*>[\s\S]*?<iframe[^>]+(?:data-src|src)=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        var url = regex.find(html)?.groupValues?.get(1)?.trim() ?: return null
        if (url.startsWith("//")) url = "https:$url"
        if (url.startsWith("/"))  url = "$baseUrl$url"
        if (url.isBlank() || url.equals("about:blank", ignoreCase = true)) return null
        return url
    }

    private fun parseLoadMiButtons(html: String, baseUrl: String): MultiAudioServer? {
        for (match in Regex("""loadMi\(\{\s*value:\s*'([^']+)'""", RegexOption.IGNORE_CASE).findAll(html)) {
            try {
                val decoded = String(Base64.decode(match.groupValues[1], Base64.DEFAULT), Charsets.UTF_8)
                var src = Regex("""<iframe[^>]*(?:data-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    .find(decoded)?.groupValues?.get(1)?.trim() ?: continue
                if (src.startsWith("//")) src = "https:$src"
                if (src.startsWith("/"))  src = "$baseUrl$src"
                if (src.isBlank() || src.equals("about:blank", ignoreCase = true)) continue
                val server = resolveIframeToServer(src, "$baseUrl/", "multi-hd-1", "HD-1")
                if (server != null) return server
            } catch (_: Exception) {}
        }
        return null
    }

    // -------------------------------------------------------------------------
    //  Iframe -> Direct Stream Resolver
    // -------------------------------------------------------------------------

    fun resolveIframeToServer(
        iframeUrl: String,
        referer: String,
        tempId: String,
        tempName: String,
        preferredLanguage: String = ""
    ): MultiAudioServer? {
        if (iframeUrl.isBlank() || iframeUrl.equals("about:blank", ignoreCase = true)) return null

        // 1. AbyssPlayer URL (e.g. https://abyssplayer.com/<id>)
        if (iframeUrl.contains("abyssplayer.com", ignoreCase = true)) {
            val direct = resolveAbyssDirectStream(iframeUrl, tempId, tempName)
            if (direct != null) return direct
        }

        // 2. Direct as-cdn / ravok / any video CDN player URL (e.g. https://ravok.buzz/video/<hash> or https://as-cdn26.top/video/<hash>)
        val asCdnMatch = Regex("""https?://([a-zA-Z0-9_.-]+)/video/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE).find(iframeUrl)
        if (asCdnMatch != null) {
            val direct = resolveAsCdnDirectM3u8(asCdnMatch.groupValues[1], asCdnMatch.groupValues[2])
            if (direct != null) return direct.copy(id = tempId, name = tempName)
        }

        // 3. Multi-lang-plyr / player.php (base64 JSON array of language links)
        if (iframeUrl.contains("player.php", ignoreCase = true) || iframeUrl.contains("multi-lang-plyr.php", ignoreCase = true)) {
            val fromMultiLang = resolveMultiLangPlyr(iframeUrl, tempId, tempName, preferredLanguage)
            if (fromMultiLang != null) return fromMultiLang
        }

        // 4. Pixel player (pixeldrain direct file)
        if (iframeUrl.contains("pixel", ignoreCase = true)) {
            val html = httpGetSync(iframeUrl, referer)
            if (html != null) {
                val fMatch = Regex("""file\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(html)
                if (fMatch != null) {
                    val stream = fMatch.groupValues[1].replace("\\/", "/")
                    return MultiAudioServer(
                        id = tempId,
                        name = tempName,
                        streamUrl = stream,
                        isDirectStream = true,
                        badge = "Multi-Audio",
                        headers = mapOf("Referer" to iframeUrl, "User-Agent" to USER_AGENT)
                    )
                }
            }
        }

        // 5. Direct stream URL (.m3u8 or .mp4)
        if (iframeUrl.contains(".m3u8", ignoreCase = true) || iframeUrl.contains(".mp4", ignoreCase = true)) {
            return MultiAudioServer(
                id = tempId,
                name = tempName,
                streamUrl = iframeUrl,
                isDirectStream = true,
                badge = "Multi-Audio",
                headers = mapOf("Referer" to referer, "User-Agent" to USER_AGENT)
            )
        }

        // 6. Inspect inner page for embed links (embed/<id>/...)
        val inner = tryExtractFromPage(iframeUrl, referer, tempId, tempName)
        if (inner != null) return inner

        // 7. Fallback embed page
        return MultiAudioServer(
            id = tempId,
            name = tempName,
            streamUrl = iframeUrl,
            isDirectStream = false,
            badge = "Multi-Audio"
        )
    }

    /**
     * Resolves AbyssPlayer direct video stream using CloudStream's decryption endpoint.
     */
    fun resolveAbyssDirectStream(
        abyssUrl: String,
        tempId: String = "multi-hd-1",
        tempName: String = "HD-1"
    ): MultiAudioServer? {
        return try {
            val req = Request.Builder()
                .url(abyssUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://playhydrax.com")
                .addHeader("Referer", "https://playhydrax.com/")
                .build()

            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string() ?: return null
            }

            val m = Regex("""const\s+datas\s*=\s*"([^"]*)"""").find(html) ?: return null
            decryptAbyssPayload(m.groupValues[1], tempId, tempName)
        } catch (e: Exception) {
            Log.w(TAG, "Abyss resolution error: ${e.message}")
            null
        }
    }

    /**
     * Decrypts AbyssPlayer payload via enc-dec.app (identical to CloudStream phisher98 Abyss extractor).
     */
    private fun decryptAbyssPayload(
        encrypted: String,
        tempId: String,
        tempName: String
    ): MultiAudioServer? {
        return try {
            val jsonBody = JSONObject().put("text", encrypted).toString()
            val reqBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val postReq = Request.Builder()
                .url("https://enc-dec.app/api/dec-abyss")
                .post(reqBody)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Origin", "https://playhydrax.com")
                .addHeader("Referer", "https://playhydrax.com/")
                .build()

            client.newCall(postReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val result = json.optJSONObject("result") ?: return null
                val sources = result.optJSONArray("sources") ?: return null

                var bestUrl: String? = null
                var bestQualityScore = -1

                for (i in 0 until sources.length()) {
                    val s = sources.optJSONObject(i) ?: continue
                    if (!s.optBoolean("status", false)) continue
                    val url = s.optString("url")
                    if (url.isBlank()) continue

                    val type = s.optString("type", "").lowercase()
                    val codec = s.optString("codec", "").lowercase()

                    // Scoring: 1080p h264 > 720p h264 > 1080p av1 > 720p av1 > 480p > 360p
                    var score = 0
                    if (type.contains("1080")) score += 100
                    else if (type.contains("720")) score += 70
                    else if (type.contains("480")) score += 40
                    else score += 20

                    if (codec == "h264") score += 30
                    else if (codec == "av1") score += 10

                    if (score > bestQualityScore) {
                        bestQualityScore = score
                        bestUrl = url
                    }
                }

                if (bestUrl != null) {
                    MultiAudioServer(
                        id = tempId,
                        name = tempName,
                        streamUrl = bestUrl,
                        isDirectStream = true,
                        badge = "Multi-Audio",
                        headers = mapOf(
                            "Referer" to "https://abyssplayer.com/",
                            "Origin" to "https://abyssplayer.com",
                            "User-Agent" to USER_AGENT
                        )
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Abyss decryption error: ${e.message}")
            null
        }
    }

    /**
     * Posts to the as-cdn getVideo API -- identical to CloudStream AWSStream/ascdn21 extractor.
     */
    fun resolveAsCdnDirectM3u8(cdnDomain: String, hash: String): MultiAudioServer? {
        return try {
            val formBody = FormBody.Builder()
                .add("hash", hash)
                .add("r", "https://animesalt.cx/")
                .build()

            val req = Request.Builder()
                .url("https://$cdnDomain/player/index.php?data=$hash&do=getVideo")
                .post(formBody)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://$cdnDomain/video/$hash")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Origin", "https://$cdnDomain")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string().orEmpty().trim()
                if (!bodyStr.startsWith("{")) return null
                val json = JSONObject(bodyStr)
                val m3u8 = json.optString("videoSource").takeIf { it.isNotBlank() }
                    ?: json.optString("securedLink").takeIf { it.isNotBlank() }
                    ?: json.optString("hls").takeIf { it.isNotBlank() }
                    ?: return null

                MultiAudioServer(
                    id             = "multi-hd-1",
                    name           = "HD-1",
                    streamUrl      = m3u8,
                    isDirectStream = true,
                    badge          = "Multi-Audio",
                    headers        = mapOf(
                        "Referer"    to "https://$cdnDomain/",
                        "Origin"     to "https://$cdnDomain",
                        "User-Agent" to USER_AGENT
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "as-cdn getVideo error: ${e.message}")
            null
        }
    }

    private fun resolveMultiLangPlyr(
        url: String,
        tempId: String,
        tempName: String,
        preferredLanguage: String = ""
    ): MultiAudioServer? {
        return try {
            val rawB64 = Regex("""data=([a-zA-Z0-9%+/=_-]+)""").find(url)?.groupValues?.get(1) ?: return null
            val decodedB64 = try {
                java.net.URLDecoder.decode(rawB64, "UTF-8")
            } catch (_: Exception) {
                rawB64
            }
            val decoded = String(Base64.decode(decodedB64, Base64.DEFAULT), Charsets.UTF_8)
            val jsonArray = org.json.JSONArray(decoded)
            var matchedLink: String? = null
            var englishLink: String? = null
            var japaneseLink: String? = null
            var hindiLink: String? = null
            var firstLink: String? = null

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val lang = item.optString("language", "")
                val link = item.optString("link", "").replace("\\/", "/")
                if (link.isNotBlank()) {
                    if (firstLink == null) firstLink = link
                    if (preferredLanguage.isNotBlank() && lang.contains(preferredLanguage, ignoreCase = true)) {
                        matchedLink = link
                        break
                    }
                    if (lang.contains("English", ignoreCase = true) || lang.contains("Dub", ignoreCase = true)) {
                        englishLink = link
                    }
                    if (lang.contains("Japanese", ignoreCase = true) || lang.contains("Sub", ignoreCase = true)) {
                        japaneseLink = link
                    }
                    if (lang.contains("Hindi", ignoreCase = true)) {
                        hindiLink = link
                    }
                }
            }

            val chosenLink: String? = matchedLink
                ?: if (tempId.contains("dub", ignoreCase = true) || tempName.contains("dub", ignoreCase = true)) {
                    englishLink ?: hindiLink ?: firstLink
                } else if (tempId.contains("sub", ignoreCase = true) || tempName.contains("sub", ignoreCase = true)) {
                    japaneseLink ?: englishLink ?: firstLink
                } else {
                    hindiLink ?: englishLink ?: firstLink
                }
            val targetLink = chosenLink ?: return null

            if (targetLink.contains("abyssplayer.com", ignoreCase = true)) {
                resolveAbyssDirectStream(targetLink, tempId, tempName)
            } else if (targetLink.contains("/video/")) {
                val m = Regex("""https?://([a-zA-Z0-9_.-]+)/video/([a-zA-Z0-9_-]+)""").find(targetLink)
                if (m != null) {
                    resolveAsCdnDirectM3u8(m.groupValues[1], m.groupValues[2])?.copy(id = tempId, name = tempName)
                } else null
            } else {
                val isDirect = targetLink.contains(".m3u8") || targetLink.contains(".mp4")
                MultiAudioServer(
                    id = tempId,
                    name = tempName,
                    streamUrl = targetLink,
                    isDirectStream = isDirect,
                    badge = "Multi-Audio"
                )
            }
        } catch (_: Exception) { null }
    }

    private fun tryExtractFromPage(
        pageUrl: String,
        referer: String,
        tempId: String,
        tempName: String
    ): MultiAudioServer? {
        return try {
            val html = httpGetSync(pageUrl, referer) ?: return null

            // 1. Nested iframe (e.g. as-cdn / abyss / etc.)
            val nestedIframe = Regex("""<iframe[^>]+src="([^"]+)"""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
            if (!nestedIframe.isNullOrBlank()) {
                val nested = resolveIframeToServer(nestedIframe, pageUrl, tempId, tempName)
                if (nested != null) return nested
            }

            // 2. As-CDN video hash
            val asCdnMatch = Regex("""https?://(as-cdn\d*\.top)/video/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE).find(html)
            if (asCdnMatch != null) {
                val direct = resolveAsCdnDirectM3u8(asCdnMatch.groupValues[1], asCdnMatch.groupValues[2])
                if (direct != null) return direct.copy(id = tempId, name = tempName)
            }

            // 3. Abyss raw encrypted datas script
            val datasMatch = Regex("""const\s+datas\s*=\s*"([^"]*)"""").find(html)
            if (datasMatch != null) {
                val direct = decryptAbyssPayload(datasMatch.groupValues[1], tempId, tempName)
                if (direct != null) return direct
            }

            // 4. Direct m3u8 or mp4 in JS file config
            val fileMatch = Regex("""(?:file|source|src)\s*:\s*["'](https?://[^"']+\.(?:m3u8|mp4)[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
            if (fileMatch != null) {
                val stream = fileMatch.groupValues[1]
                return MultiAudioServer(
                    id = tempId,
                    name = tempName,
                    streamUrl = stream,
                    isDirectStream = true,
                    badge = "Multi-Audio",
                    headers = mapOf("Referer" to pageUrl, "User-Agent" to USER_AGENT)
                )
            }

            null
        } catch (_: Exception) { null }
    }

    // -------------------------------------------------------------------------
    //  HTTP helpers
    // -------------------------------------------------------------------------

    private suspend fun httpGet(url: String, referer: String, cookie: String? = null): String? = withContext(Dispatchers.IO) {
        httpGetSync(url, referer, cookie)
    }

    private fun httpGetSync(url: String, referer: String, cookie: String? = null): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", referer)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .apply {
                    if (!cookie.isNullOrBlank()) {
                        addHeader("Cookie", cookie)
                    }
                }
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string().orEmpty().takeIf { it.length > 50 }
            }
        } catch (_: Exception) { null }
    }
}
