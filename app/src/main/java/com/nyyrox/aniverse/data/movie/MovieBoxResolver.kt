package com.nyyrox.aniverse.data.movie

import android.util.Base64
import android.util.Log
import com.nyyrox.aniverse.data.tmdb.TmdbRepository
import com.nyyrox.aniverse.player.StreamProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Native MovieBox Stream Resolver for Android.
 * Runs directly on the device with zero dependency on external workers or shield barriers.
 * Implements HMAC-MD5 request signing, token bootstrap, TMDB-to-MovieBox matching,
 * multi-audio dub discovery (Hindi, Tamil, Telugu, Spanish, etc.), and 4K/1080p CDN stream extraction.
 */
object MovieBoxResolver {

    private const val TAG = "MovieBoxResolver"

    private const val SECRET_KEY_DEFAULT_B64 = "NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw=="
    private const val SECRET_KEY_ALT_B64 = "WHFuMm5uTzQxL0w5Mm8xaXVYaFNMSFRiWHZZNFo1Wlo2Mm04bVNMQQ=="
    private const val MODERN_USER_AGENT = "com.community.mbox.in/50020130 (Linux; U; Android 14; en_IN; Pixel 8; Build/UD1A.230803.041; Cronet/145.0.7582.0)"
    private const val WEB_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36"
    private const val NATIVE_API_BASE = "https://api3.aoneroom.com"
    private const val BOOTSTRAP_HOST = "https://apig.inmoviebox.com"
    private const val BOOTSTRAP_URL = "$BOOTSTRAP_HOST/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"

    private val WEB_DOMAINS = listOf(
        "https://123moviesfree.club",
        "https://sflix.film",
        "https://movieboxonline.net"
    )

    private val VALID_DUB_REGEX = Regex("hindi|tamil|telugu|kannada|malayalam|bengali|marathi|punjabi|english|spanish|esla|portuguese|ptbr|french|german|italian|russian|japanese|korean|chinese|mandarin|cantonese|arabic|turkish|thai|indonesian|vietnamese|tagalog|filipino|dub|dual\\s*audio|multi\\s*audio", RegexOption.IGNORE_CASE)
    private val IGNORE_TAG_REGEX = Regex("spoilers?|cameos?|full\\s*movie|clips?|trailers?|teasers?|scenes?|review|bts|behind\\s*the\\s*scenes", RegexOption.IGNORE_CASE)

    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiresAt: Long = 0L
    @Volatile
    private var deviceId: String? = null

    private fun getDeviceId(): String {
        var id = deviceId
        if (id == null) {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b.toInt() and 0xFF))
            }
            id = sb.toString()
            deviceId = id
        }
        return id
    }

    private fun md5Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun hmacMd5Base64(key: ByteArray, data: String): String {
        val mac = Mac.getInstance("HmacMD5")
        val secretKey = SecretKeySpec(key, "HmacMD5")
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun getSecretBytes(useAlt: Boolean = false): ByteArray {
        val outer = if (useAlt) SECRET_KEY_ALT_B64 else SECRET_KEY_DEFAULT_B64
        val innerBytes = Base64.decode(outer, Base64.DEFAULT)
        val innerStr = String(innerBytes, Charsets.UTF_8)
        return Base64.decode(innerStr, Base64.DEFAULT)
    }

    private fun generateXClientToken(ts: Long): String {
        val tsStr = ts.toString()
        val reversed = tsStr.reversed()
        val hash = md5Hex(reversed.toByteArray(Charsets.UTF_8))
        return "$tsStr,$hash"
    }

    private fun generateClientInfo(devId: String): String {
        return """{"package_name":"com.community.mbox.in","version_name":"4.0.03.0920.03","version_code":50020130,"os":"android","os_version":"14","device_id":"$devId","install_store":"official","gaid":"1b2212c1-dadf-43c3-a0c8-bd6ce48ae22d","brand":"Google","model":"Pixel 8","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}"""
    }

    private fun buildCanonicalString(
        method: String,
        accept: String,
        contentType: String,
        urlStr: String,
        body: String?,
        ts: Long
    ): String {
        val uri = URI(urlStr)
        val path = if (uri.rawPath.isNullOrEmpty()) "/" else uri.rawPath
        var canonicalUrl = path
        val rawQuery = uri.rawQuery
        if (!rawQuery.isNullOrBlank()) {
            val qParts = rawQuery.split("&").toTypedArray()
            Arrays.sort(qParts)
            canonicalUrl = "$path?${qParts.joinToString("&")}"
        }

        var bodyLength = ""
        var bodyHash = ""
        if (!body.isNullOrEmpty()) {
            val bodyBytes = body.toByteArray(Charsets.UTF_8)
            bodyLength = bodyBytes.size.toString()
            val sliceLen = Math.min(bodyBytes.size, 102400)
            bodyHash = md5Hex(bodyBytes.copyOfRange(0, sliceLen))
        }

        return listOf(
            method.uppercase(),
            accept,
            contentType,
            bodyLength,
            ts.toString(),
            bodyHash,
            canonicalUrl
        ).joinToString("\n")
    }

    private fun generateXTrSignature(
        method: String,
        accept: String,
        contentType: String,
        urlStr: String,
        body: String?,
        ts: Long,
        useAlt: Boolean = false
    ): String {
        val canonical = buildCanonicalString(method, accept, contentType, urlStr, body, ts)
        val key = getSecretBytes(useAlt)
        val sigB64 = hmacMd5Base64(key, canonical)
        return "$ts|2|$sigB64"
    }

    private fun buildAuthHeaders(
        method: String,
        urlStr: String,
        body: String? = null,
        token: String? = null,
        devId: String? = null
    ): Map<String, String> {
        val ts = System.currentTimeMillis() / 1000
        val effectiveDevId = devId ?: getDeviceId()
        val accept = "application/json"
        val contentType = if (body != null) "application/json" else ""

        val headers = mutableMapOf(
            "Accept" to accept,
            "User-Agent" to MODERN_USER_AGENT,
            "x-client-info" to generateClientInfo(effectiveDevId),
            "x-client-status" to "0",
            "x-client-token" to generateXClientToken(ts),
            "x-device-id" to effectiveDevId,
            "x-request-lang" to "en",
            "x-source" to "moviebox",
            "x-tr-signature" to generateXTrSignature(method, accept, contentType, urlStr, body, ts)
        )

        if (contentType.isNotEmpty()) headers["Content-Type"] = contentType
        if (!token.isNullOrEmpty()) headers["authorization"] = "Bearer $token"

        return headers
    }

    private suspend fun bootstrapToken(): String = withContext(Dispatchers.IO) {
        val cur = cachedToken
        if (!cur.isNullOrEmpty() && System.currentTimeMillis() < tokenExpiresAt) {
            return@withContext cur
        }

        val devId = getDeviceId()
        val headers = buildAuthHeaders("GET", BOOTSTRAP_URL, null, null, devId)

        val conn = (URL(BOOTSTRAP_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val errText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            throw IllegalStateException("MovieBox bootstrap failed with HTTP $code: $errText")
        }

        val xUser = conn.getHeaderField("x-user") ?: conn.getHeaderField("X-User")
        conn.disconnect()

        if (xUser.isNullOrBlank()) {
            throw IllegalStateException("MovieBox bootstrap returned no x-user header")
        }

        val json = JSONObject(xUser)
        val token = json.optString("token")
        if (token.isBlank()) {
            throw IllegalStateException("Empty token in MovieBox bootstrap response")
        }

        cachedToken = token
        tokenExpiresAt = System.currentTimeMillis() + (2 * 3600 * 1000L)
        Log.d(TAG, "MovieBox token bootstrapped successfully: ${token.take(15)}...")
        token
    }

    private suspend fun apiRequest(
        method: String,
        pathOrUrl: String,
        bodyStr: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val fullUrl = if (pathOrUrl.startsWith("http")) pathOrUrl else "$NATIVE_API_BASE$pathOrUrl"
        val token = bootstrapToken()
        val headers = buildAuthHeaders(method, fullUrl, bodyStr, token, getDeviceId())

        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method.uppercase()
            connectTimeout = 12000
            readTimeout = 18000
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }

        if (bodyStr != null && (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true))) {
            conn.doOutput = true
            val bytes = bodyStr.toByteArray(Charsets.UTF_8)
            conn.outputStream.use { os ->
                os.write(bytes)
                os.flush()
            }
        }

        val code = conn.responseCode
        if (code == 401 || code == 441 || code == 407) {
            cachedToken = null
            conn.disconnect()
            Log.w(TAG, "MovieBox API returned HTTP $code, retrying with fresh bootstrap token")
            return@withContext apiRequest(method, pathOrUrl, bodyStr)
        }

        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        conn.disconnect()

        JSONObject(text)
    }

    private data class MbCandidate(
        val subjectId: String,
        val title: String,
        val type: String, // "movie" or "tv"
        val releaseDate: String?,
        val detailPath: String
    )

    private suspend fun searchMovieBox(query: String): List<MbCandidate> = withContext(Dispatchers.IO) {
        val escaped = query.replace("\"", "\\\"").trim()
        val body = """{"keyword":"$escaped","page":1,"perPage":15}"""

        val res = apiRequest("POST", "/wefeed-mobile-bff/subject-api/search", body)
        val data = res.optJSONObject("data")
        val items = data?.optJSONArray("items") ?: data?.optJSONArray("list") ?: JSONArray()

        val list = mutableListOf<MbCandidate>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val sub = item.optJSONObject("subject") ?: item
            val subId = sub.optString("subjectId")
            val subTitle = sub.optString("title")
            val subType = if (sub.optInt("subjectType", 1) == 1) "movie" else "tv"
            val relDate = sub.optString("releaseDate")
            val detailUrl = sub.optString("detailUrl").ifBlank { item.optString("detailUrl") }
            val detailPath = detailUrl.substringAfterLast("/", "")

            if (subId.isNotBlank() && subTitle.isNotBlank()) {
                list.add(
                    MbCandidate(
                        subjectId = subId,
                        title = subTitle,
                        type = subType,
                        releaseDate = relDate.takeIf { it.isNotBlank() },
                        detailPath = detailPath
                    )
                )
            }
        }
        list
    }

    private data class MbDetails(
        val subjectId: String,
        val title: String,
        val detailPath: String,
        val resourceDetectors: JSONArray,
        val dubs: List<Pair<String, String>> // (subjectId, label)
    )

    private suspend fun getMovieBoxDetails(subjectId: String): MbDetails = withContext(Dispatchers.IO) {
        val res = apiRequest("GET", "/wefeed-mobile-bff/subject-api/get?subjectId=$subjectId")
        val data = res.optJSONObject("data") ?: JSONObject()
        val title = data.optString("title")
        val detailUrl = data.optString("detailUrl")
        val detailPath = detailUrl.substringAfterLast("/", "")
        val detectors = data.optJSONArray("resourceDetectors") ?: JSONArray()

        val dubs = mutableListOf<Pair<String, String>>()
        val dubsArr = data.optJSONArray("dubs")
        if (dubsArr != null) {
            for (i in 0 until dubsArr.length()) {
                val d = dubsArr.optJSONObject(i) ?: continue
                val dId = d.optString("subjectId")
                val dName = d.optString("lanName").ifBlank { d.optString("language") }
                if (dId.isNotBlank()) {
                    val lower = dName.lowercase()
                    val cleanLabel = when {
                        lower.startsWith("esla") -> "Spanish dub"
                        lower.startsWith("ptbr") -> "Portuguese dub"
                        !lower.contains("dub") && !lower.contains("audio") -> "${dName.replaceFirstChar { it.uppercase() }} Dub"
                        else -> dName.replaceFirstChar { it.uppercase() }
                    }
                    dubs.add(dId to cleanLabel)
                }
            }
        }

        MbDetails(
            subjectId = subjectId,
            title = title,
            detailPath = detailPath,
            resourceDetectors = detectors,
            dubs = dubs
        )
    }

    private fun isFakeStream(url: String, duration: Double, size: Long): Boolean {
        if (url.isBlank()) return true
        if (url.contains("80da52722ee7de7a") ||
            url.contains("b164fbfb4347792950bdfbfb563d39d9") ||
            url.contains("/other/2026/10/10/")) return true
        if (duration in 1.0..59.0) return true
        if (size in 1..(5 * 1024 * 1024) && duration in 1.0..120.0) return true
        return false
    }

    private fun formatSize(bytes: Long): String? {
        if (bytes <= 0) return null
        return if (bytes >= 1024L * 1024L * 1024L) {
            String.format("%.1f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
        } else {
            String.format("%d MB", bytes / (1024 * 1024))
        }
    }

    fun normalizeTitle(t: String?): String {
        if (t.isNullOrBlank()) return ""
        return Normalizer.normalize(t, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("\\b(part|episode|season)\\s+ii\\b"), "$1 2")
            .replace(Regex("\\b(part|episode|season)\\s+iii\\b"), "$1 3")
            .replace(Regex("\\b(part|episode|season)\\s+iv\\b"), "$1 4")
            .replace(Regex("\\b(part|episode|season)\\s+v\\b"), "$1 5")
            .replace(Regex("\\bii\\b"), "2")
            .replace(Regex("\\biii\\b"), "3")
            .replace(Regex("\\biv\\b"), "4")
            .replace(Regex("\\bv\\b"), "5")
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Resolves MovieBox stream directly using native Android cryptographic authentication.
     */
    suspend fun resolveStream(
        tmdbId: Int,
        type: String, // "movie" or "tv"
        season: Int = 1,
        episode: Int = 1,
        title: String? = null,
        year: String? = null,
        dubId: String? = null
    ): MovieStreamResult = withContext(Dispatchers.IO) {
        var resolvedTitle = title?.trim().orEmpty()
        var resolvedYear = year?.trim()?.take(4).orEmpty()
        var originalTitle = ""
        val altTitles = mutableListOf<String>()
        val targetType = if (type.equals("tv", ignoreCase = true)) "tv" else "movie"

        // 1. Fetch metadata & alternate titles from TMDB if missing
        if (resolvedTitle.isBlank() || resolvedYear.isBlank()) {
            try {
                val tmdbRes = TmdbRepository.executeTmdbGet("/$targetType/$tmdbId", "language=en-US&append_to_response=alternative_titles")
                if (!tmdbRes.isNullOrBlank()) {
                    val root = JSONObject(tmdbRes)
                    if (resolvedTitle.isBlank()) {
                        resolvedTitle = root.optString("title").ifBlank { root.optString("name") }
                    }
                    originalTitle = root.optString("original_title").ifBlank { root.optString("original_name") }
                    if (resolvedYear.isBlank()) {
                        val d = root.optString("release_date").ifBlank { root.optString("first_air_date") }
                        resolvedYear = d.take(4)
                    }
                    val altObj = root.optJSONObject("alternative_titles")
                    val altArr = altObj?.optJSONArray(if (targetType == "tv") "results" else "titles")
                    if (altArr != null) {
                        for (i in 0 until altArr.length()) {
                            val at = altArr.optJSONObject(i)?.optString("title")?.ifBlank { altArr.optJSONObject(i)?.optString("name") }
                            if (!at.isNullOrBlank()) altTitles.add(at)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TMDB lookup error in MovieBoxResolver: ${e.message}")
            }
        }
        if (resolvedTitle.isBlank()) resolvedTitle = "Movie"
        val targetYearNum = resolvedYear.toIntOrNull() ?: 0

        Log.d(TAG, "Resolving MovieBox stream for '$resolvedTitle' ($targetType, year=$resolvedYear, dub=$dubId)")

        // 2. Expand queries: primary title, original title, subDivider, alternate titles
        val searchQueries = mutableListOf<String>()
        searchQueries.add(resolvedTitle)
        if (originalTitle.isNotBlank() && !originalTitle.equals(resolvedTitle, ignoreCase = true)) {
            searchQueries.add(originalTitle)
        }
        val subDivider = resolvedTitle.split(Regex("[:\\-–]")).firstOrNull()?.trim()
        if (!subDivider.isNullOrBlank() && subDivider.length >= 3 && !subDivider.equals(resolvedTitle, ignoreCase = true) && !searchQueries.contains(subDivider)) {
            searchQueries.add(subDivider)
        }
        for (alt in altTitles.take(3)) {
            if (!searchQueries.contains(alt)) searchQueries.add(alt)
        }

        var candidates = emptyList<MbCandidate>()
        for (q in searchQueries) {
            try {
                val res = searchMovieBox(q)
                if (res.isNotEmpty()) {
                    candidates = res
                    break
                }
            } catch (_: Exception) {}
        }

        if (candidates.isEmpty()) {
            throw IllegalStateException("No MovieBox results found for '$resolvedTitle'")
        }

        // Clean & robust title matcher
        val normTarget = normalizeTitle(resolvedTitle)
        val normOriginal = if (originalTitle.isNotBlank()) normalizeTitle(originalTitle) else null
        val normAlts = altTitles.map { normalizeTitle(it) }
        val validNormTargets = listOfNotNull(normTarget, normOriginal) + normAlts

        fun isTargetMatch(candTitle: String): Int {
            val cNorm = normalizeTitle(candTitle)
            for (target in validNormTargets) {
                if (target.isBlank()) continue
                if (cNorm == target) return 100
                if (cNorm.startsWith(target) || target.startsWith(cNorm)) return 85
                if (cNorm.contains(target) || target.contains(cNorm)) return 70
            }
            return 0
        }

        fun hasBrackets(t: String) = Regex("\\[.*?\\]|\\(.*?\\)").containsMatchIn(t)
        val targetHasBrackets = hasBrackets(resolvedTitle)

        fun scoreCandidate(c: MbCandidate): Int {
            val baseMatch = isTargetMatch(c.title)
            if (baseMatch == 0) return 0
            var score = baseMatch
            val clean = c.title.trim()
            if (clean.equals(resolvedTitle, ignoreCase = true)) {
                score += 70
            } else if (!targetHasBrackets && !hasBrackets(clean)) {
                score += 45
            } else if (!targetHasBrackets && hasBrackets(clean)) {
                score -= 30
            }
            if (!c.detailPath.isNullOrBlank()) score += 20
            if (c.type == targetType) score += 15
            val cYear = c.releaseDate?.take(4)?.toIntOrNull() ?: 0
            if (targetYearNum > 0 && cYear > 0) {
                val diff = abs(cYear - targetYearNum)
                if (diff == 0) score += 25
                else if (diff == 1) score += 10
                else score -= 30
            }
            return score
        }

        val sortedCandidates = candidates.filter { isTargetMatch(it.title) > 0 }.sortedByDescending { scoreCandidate(it) }
        val matched = sortedCandidates.firstOrNull() ?: candidates.first()

        Log.d(TAG, "Matched candidate: '${matched.title}' (ID: ${matched.subjectId})")

        // 3. Discover Dubs
        val details = getMovieBoxDetails(matched.subjectId)
        val formattedDubs = mutableListOf<MovieDub>()
        formattedDubs.add(MovieDub(matched.subjectId, "Original", selected = (dubId == null || dubId == matched.subjectId), detailPath = matched.detailPath))

        // Dubs from subject API
        details.dubs.forEach { (dId, dLabel) ->
            if (dId != matched.subjectId && formattedDubs.none { it.id == dId }) {
                val sibPath = candidates.find { it.subjectId == dId }?.detailPath
                formattedDubs.add(MovieDub(dId, dLabel, selected = (dubId == dId), detailPath = sibPath))
            }
        }

        // Sibling dub candidates from search results
        for (sr in candidates) {
            if (sr.subjectId == matched.subjectId) continue
            val srYear = sr.releaseDate?.take(4)?.toIntOrNull() ?: 0
            if (targetYearNum > 0 && srYear > 0 && abs(srYear - targetYearNum) > 1) continue
            if (isTargetMatch(sr.title) > 0) {
                val dubMatch = Regex("\\[(.*?)\\]|\\((.*?)\\)").find(sr.title)
                if (dubMatch != null) {
                    val rawTag = (dubMatch.groupValues[1].ifBlank { dubMatch.groupValues[2] }).trim()
                    if (IGNORE_TAG_REGEX.containsMatchIn(rawTag) || !VALID_DUB_REGEX.containsMatchIn(rawTag)) continue
                    val lowerTag = rawTag.lowercase()
                    val cleanLabel = when {
                        lowerTag.startsWith("esla") -> "Spanish dub"
                        lowerTag.startsWith("ptbr") -> "Portuguese dub"
                        !lowerTag.contains("dub") && !lowerTag.contains("audio") -> "${rawTag.replaceFirstChar { it.uppercase() }} Dub"
                        else -> rawTag.replaceFirstChar { it.uppercase() }
                    }
                    if (formattedDubs.none { it.id == sr.subjectId || it.label.equals(cleanLabel, ignoreCase = true) }) {
                        formattedDubs.add(MovieDub(sr.subjectId, cleanLabel, selected = (dubId == sr.subjectId), detailPath = sr.detailPath))
                    }
                }
            }
        }

        // 3. Select Subject to Play (either selected dub or original matched)
        val cleanDubId = dubId?.replace(Regex("^-"), "")?.trim()
        val targetDub = if (!cleanDubId.isNullOrBlank()) {
            formattedDubs.find { it.id == cleanDubId }
        } else null
        val playSubjectId = targetDub?.id ?: matched.subjectId
        val playDetailPath = targetDub?.detailPath?.takeIf { it.isNotBlank() } ?: matched.detailPath

        // Update selected flag on dubs
        val finalDubs = formattedDubs.map { it.copy(selected = (it.id == playSubjectId)) }

        // 4. Extract Streams from resourceDetectors
        val activeDetails = if (playSubjectId == matched.subjectId) details else getMovieBoxDetails(playSubjectId)
        val detectors = activeDetails.resourceDetectors

        data class RawStream(
            val quality: String,
            val resolutionNum: Int,
            val url: String,
            val size: Long,
            val duration: Double
        )

        fun parseResolution(raw: String?, fallbackInt: Int = 1080): Pair<Int, String> {
            val s = raw?.trim().orEmpty()
            if (s.contains("4k", ignoreCase = true) || s.contains("2160")) {
                return Pair(2160, "4K")
            }
            val num = s.filter { it.isDigit() }.toIntOrNull() ?: fallbackInt
            return if (num >= 2160 || num == 4) {
                Pair(2160, "4K")
            } else {
                Pair(num, "${num}p")
            }
        }

        val rawStreams = mutableListOf<RawStream>()
        val seenUrls = mutableSetOf<String>()
        val seenQualities = mutableSetOf<Int>()

        for (i in 0 until detectors.length()) {
            val rd = detectors.optJSONObject(i) ?: continue
            val resList = rd.optJSONArray("resolutionList") ?: JSONArray()
            for (j in 0 until resList.length()) {
                val r = resList.optJSONObject(j) ?: continue
                val resLink = r.optString("resourceLink")
                if (resLink.isBlank() || seenUrls.contains(resLink)) continue

                if (targetType == "tv") {
                    val itemSe = r.optInt("se", r.optInt("season", 0))
                    val itemEp = r.optInt("ep", r.optInt("episode", 0))
                    if (itemSe > 0 && itemEp > 0 && (itemSe != season || itemEp != episode)) continue
                }

                val dur = r.optDouble("duration", 0.0)
                val size = r.optLong("size", 0L)
                if (isFakeStream(resLink, dur, size)) continue

                seenUrls.add(resLink)
                val (resVal, qualLabel) = parseResolution(r.optString("resolution"), 1080)
                seenQualities.add(resVal)

                rawStreams.add(
                    RawStream(
                        quality = qualLabel,
                        resolutionNum = resVal,
                        url = resLink,
                        size = size,
                        duration = dur
                    )
                )
            }

            // Also check direct downloadUrl or resourceLink if resolutionList was empty
            val directUrl = rd.optString("downloadUrl").ifBlank { rd.optString("resourceLink") }
            if (directUrl.isNotBlank() && !seenUrls.contains(directUrl)) {
                val rdSize = rd.optLong("firstSize", rd.optLong("totalSize", 0L))
                val rdDur = rd.optDouble("duration", 0.0)
                if (!isFakeStream(directUrl, rdDur, rdSize)) {
                    seenUrls.add(directUrl)
                    val (resVal, qualLabel) = parseResolution(rd.optString("resolution"), 1080)
                    seenQualities.add(resVal)
                    rawStreams.add(
                        RawStream(
                            quality = qualLabel,
                            resolutionNum = resVal,
                            url = directUrl,
                            size = rdSize,
                            duration = rdDur
                        )
                    )
                }
            }
        }

        var activeReferer = "https://123moviesfree.club/"

        // For TV shows (or if detectors didn't yield streams), fetch via Web CDN mirrors & play-info
        val effectiveDetailPath = playDetailPath.ifBlank { activeDetails.detailPath }
        if (targetType == "tv" || rawStreams.isEmpty()) {
            if (effectiveDetailPath.isNotBlank()) {
                try {
                    val token = bootstrapToken()
                    for (domain in WEB_DOMAINS) {
                        try {
                            val encPath = java.net.URLEncoder.encode(effectiveDetailPath, "UTF-8")
                            val playUrl = "$domain/wefeed-h5api-bff/subject/play?subjectId=$playSubjectId&se=$season&ep=$episode&detailPath=$encPath&streamSignType=1&supportCodecs%5Bhevc%5D=1&supportCodecs%5Bh264%5D=1"
                            val refUrl = "$domain/movies/$effectiveDetailPath?id=$playSubjectId&type=/movie/detail&detailSe=&detailEp=&lang=en"

                            val urlObj = URL(playUrl)
                            val conn = urlObj.openConnection() as HttpURLConnection
                            conn.connectTimeout = 6000
                            conn.readTimeout = 8000
                            conn.requestMethod = "GET"
                            conn.setRequestProperty("User-Agent", WEB_USER_AGENT)
                            conn.setRequestProperty("Referer", refUrl)
                            conn.setRequestProperty("Accept", "application/json")
                            conn.setRequestProperty("x-client-info", """{"timezone":"Asia/Calcutta"}""")
                            conn.setRequestProperty("x-request-lang", "en")
                            conn.setRequestProperty("x-vip-restrict", "0")
                            conn.setRequestProperty("x-no-high-risk-restrict", "0")
                            conn.setRequestProperty("x-source", "moviebox")
                            conn.setRequestProperty("Authorization", "Bearer $token")

                            val code = conn.responseCode
                            if (code in 200..299) {
                                val text = conn.inputStream.bufferedReader().use { it.readText() }
                                val dataObj = JSONObject(text).optJSONObject("data")
                                val sArr = dataObj?.optJSONArray("streams")
                                if (sArr != null && sArr.length() > 0) {
                                    for (k in 0 until sArr.length()) {
                                        val st = sArr.optJSONObject(k) ?: continue
                                        val stUrl = st.optString("url")
                                        val stDur = st.optDouble("duration", 0.0)
                                        val stSize = st.optLong("size", 0L)
                                        if (isFakeStream(stUrl, stDur, stSize) || seenUrls.contains(stUrl)) continue
                                        val rawResStr = st.optString("resolutions").ifBlank { st.optString("resolution") }
                                        val fallback = st.optInt("resolution", 1080)
                                        val (resVal, qualLabel) = parseResolution(rawResStr, fallback)
                                        if (seenQualities.contains(resVal)) continue

                                        seenUrls.add(stUrl)
                                        seenQualities.add(resVal)
                                        rawStreams.add(
                                            RawStream(
                                                quality = qualLabel,
                                                resolutionNum = resVal,
                                                url = stUrl,
                                                size = stSize,
                                                duration = stDur
                                            )
                                        )
                                    }
                                    if (rawStreams.isNotEmpty()) {
                                        activeReferer = "$domain/"
                                        conn.disconnect()
                                        break
                                    }
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            Log.w(TAG, "Web CDN mirror $domain fetch error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Web bootstrapToken error: ${e.message}")
                }
            }

            // 3. Mobile Native CDN fallback (play-info)
            if (rawStreams.isEmpty()) {
                try {
                    val nativeRes = apiRequest("GET", "/wefeed-mobile-bff/subject-api/play-info?subjectId=$playSubjectId&se=$season&ep=$episode")
                    val rawArr = nativeRes.optJSONObject("data")?.optJSONArray("streams")
                    if (rawArr != null) {
                        for (k in 0 until rawArr.length()) {
                            val st = rawArr.optJSONObject(k) ?: continue
                            val stUrl = st.optString("url")
                            val stDur = st.optDouble("duration", 0.0)
                            val stSize = st.optLong("size", 0L)
                            if (isFakeStream(stUrl, stDur, stSize) || seenUrls.contains(stUrl)) continue
                            val rawResStr = st.optString("resolutions").ifBlank { st.optString("resolution") }
                            val fallback = st.optInt("resolution", 1080)
                            val (resVal, qualLabel) = parseResolution(rawResStr, fallback)
                            if (seenQualities.contains(resVal)) continue

                            seenUrls.add(stUrl)
                            seenQualities.add(resVal)
                            rawStreams.add(
                                RawStream(
                                    quality = qualLabel,
                                    resolutionNum = resVal,
                                    url = stUrl,
                                    size = stSize,
                                    duration = stDur
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Mobile play-info fallback error: ${e.message}")
                }
            }
        }

        // Sort descending by resolution (2160p -> 1080p -> 720p -> 480p -> 360p)
        rawStreams.sortByDescending { it.resolutionNum }

        if (rawStreams.isEmpty()) {
            if (playSubjectId != matched.subjectId) {
                Log.w(TAG, "Selected dub '$playSubjectId' had no streams. Falling back to primary stream.")
                return@withContext resolveStream(
                    tmdbId = tmdbId,
                    type = type,
                    season = season,
                    episode = episode,
                    title = title,
                    year = year,
                    dubId = null
                )
            }
            throw IllegalStateException("No valid streams available on MovieBox for '${matched.title}'")
        }

        // Build stream items with client proxy to prevent 428/429
        val isTv = targetType == "tv"
        val epTag = if (isTv) "S%02dE%02d".format(season, episode) else (if (!resolvedYear.isNullOrBlank()) "($resolvedYear)" else "")
        val activeDubObj = finalDubs.find { it.id == playSubjectId }
        val dubTag = if (activeDubObj != null && !activeDubObj.label.startsWith("Original")) "[${activeDubObj.label}]" else ""

        val formattedStreams = rawStreams.map { s ->
            val fileBase = listOf(matched.title, epTag, dubTag, "[${s.quality}]").filter { it.isNotBlank() }.joinToString(" ").trim()
            val downloadFileName = "$fileBase.mp4"

            // Hybrid stream URL resolution:
            // Web CDN (bcdnxw / series) routes through StreamProxyServer with referer (123moviesfree.club)
            // Native mobile CDN (bcdn / movies) routes through StreamProxyServer with ExoPlayer UA & NO referer to avoid 429.
            val isWebCdn = s.url.contains("bcdnxw") || s.url.contains("hakunaymatata.com/bt/")
            val directStreamUrl = if (isWebCdn) {
                StreamProxyServer.buildProxyUrl(s.url, mapOf("Referer" to activeReferer))
            } else {
                StreamProxyServer.buildProxyUrl(s.url)
            }
            val downloadUrl = s.url

            MovieStream(
                quality = s.quality,
                resolutionNum = s.resolutionNum,
                format = "mp4",
                size = s.size.takeIf { it > 0 },
                sizeLabel = formatSize(s.size),
                duration = s.duration.takeIf { it > 0 },
                streamUrl = directStreamUrl,
                downloadUrl = downloadUrl,
                downloadFileName = downloadFileName
            )
        }

        val best = formattedStreams.first()

        MovieStreamResult(
            success = true,
            server = "MovieBox",
            title = matched.title,
            streamUrl = best.streamUrl,
            downloadUrl = best.downloadUrl,
            quality = best.quality,
            format = "mp4",
            duration = best.duration,
            dubs = finalDubs,
            streams = formattedStreams,
            subtitles = emptyList()
        )
    }
}
