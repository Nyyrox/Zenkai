package com.nyyrox.aniverse.data.extractor

import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.MediaRelation
import com.nyyrox.aniverse.data.SkipInterval
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AnikotoResolver {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val MEGAPLAY_KEY = "i?LMTAx0Q6,:}50U".toByteArray(Charsets.UTF_8).copyOf(32)
    private val MEGAPLAY_IV = "W0;27ToaUpl_P%'c".toByteArray(Charsets.UTF_8)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .build()

    private val streamCache = ConcurrentHashMap<String, DirectStreamResult>()
    private val aniListRepo = com.nyyrox.aniverse.data.AniListRepository()

    fun clearStreamCache(anilistId: Int? = null, episode: Int? = null) {
        if (anilistId != null && episode != null) {
            val prefix = "$anilistId:$episode:"
            streamCache.keys.removeIf { it.startsWith(prefix) }
        } else {
            streamCache.clear()
        }
    }

    /**
     * Resolves Anikoto direct stream client-side mapped by AniList ID and episode.
     */
    suspend fun resolveStream(
        anilistId: Int,
        episode: Int,
        audio: String = "sub",
        animeTitle: String = "",
        server: String = "bcdn"
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        val targetAudio = audio.lowercase()
        val cacheKey = "$anilistId:$episode:$targetAudio:$server"
        streamCache[cacheKey]?.let { return@withContext it }

        // Tier 1: Client-Side Direct MegaPlay Extraction via AniList ID
        val directMegaPlay = resolveDirectMegaPlayWithServer(anilistId, episode, targetAudio, server)
        if (directMegaPlay != null) {
            streamCache[cacheKey] = directMegaPlay
            return@withContext directMegaPlay
        }

        // Tier 1b: If server failed, try vidstream as fallback
        val vidstreamFallback = resolveDirectMegaPlayWithServer(anilistId, episode, targetAudio, "vidstream")
        if (vidstreamFallback != null) {
            val tagged = vidstreamFallback.copy(
                serverId = if (server == "vidstream") "vidstream-direct-$targetAudio" else "megaplay-direct-$targetAudio",
                serverName = if (server == "vidstream") "Vidstream" else "MegaPlay"
            )
            streamCache[cacheKey] = tagged
            return@withContext tagged
        }

        // Tier 2: Anikoto TV Site Scraping Fallback
        if (animeTitle.isNotBlank()) {
            val siteStream = resolveFromAnikotoSite(animeTitle, episode, targetAudio, server)
            if (siteStream != null) {
                streamCache[cacheKey] = siteStream
                return@withContext siteStream
            }
        }

        // Tier 3: AniList Relation Fallback (Parent, Prequel, Sequel) for split seasons
        val relationStream = resolveWithRelations(anilistId, episode, targetAudio, animeTitle, server)
        if (relationStream != null) {
            streamCache[cacheKey] = relationStream
            return@withContext relationStream
        }

        null
    }

    suspend fun resolveDirectMegaPlayWithServer(
        anilistId: Int,
        episode: Int,
        audio: String,
        server: String = "bcdn"
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        try {
            val srvParam = if (server.isBlank() || server.equals("tcdn", ignoreCase = true)) "bcdn" else server
            val pageUrl = "https://megaplay.buzz/stream/ani/$anilistId/$episode/$audio?s=$srvParam"
            val req = Request.Builder()
                .url(pageUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://megaplay.buzz/")
                .build()

            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body.string()
            }

            val fileIdMatch = Regex("""data-id=["']([^"']+)["']""").find(html)
            val fileId = fileIdMatch?.groupValues?.get(1) ?: return@withContext null

            val sourceReq = Request.Builder()
                .url("https://megaplay.buzz/stream/getSources?id=$fileId&s=$srvParam")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", pageUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()

            client.newCall(sourceReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body.string()
                val json = JSONObject(body)

                var streamUrl: String? = null
                val enc = json.optString("enc")
                if (enc.isNotBlank()) {
                    streamUrl = decryptMegaPlayPayload(enc)
                }
                if (streamUrl.isNullOrBlank()) {
                    val sources = json.optJSONObject("sources")
                    streamUrl = sources?.optString("file")?.takeIf { it.isNotBlank() }
                        ?: sources?.optString("url")?.takeIf { it.isNotBlank() }
                }

                if (streamUrl.isNullOrBlank()) return@withContext null

                // If upstream somehow returns TikTok CDN (blocked in regions), seamlessly fallback to Vidstream
                if ((streamUrl.contains("tiktokcdn.com") || streamUrl.contains("akirax.buzz")) && !server.equals("vidstream", ignoreCase = true)) {
                    val fallback = resolveDirectMegaPlayWithServer(anilistId, episode, audio, "vidstream")
                    if (fallback != null) return@withContext fallback
                }

                // Parse Subtitles
                val subtitles = mutableListOf<SubtitleTrack>()
                val tracks = json.optJSONArray("tracks")
                if (tracks != null) {
                    for (i in 0 until tracks.length()) {
                        val t = tracks.optJSONObject(i) ?: continue
                        val file = t.optString("file").takeIf { it.isNotBlank() } ?: t.optString("url")
                        val label = t.optString("label", "English").trim()
                        if (file.isNotBlank()) {
                            val normLang = com.nyyrox.aniverse.data.subtitles.SubtitleRepository.normalizeLanguage(label)
                            val isEng = normLang.equals("English", ignoreCase = true) || label.contains("English", ignoreCase = true)
                            val isDef = t.optBoolean("default", isEng || i == 0)
                            subtitles.add(
                                SubtitleTrack(
                                    id = "anikoto-sub-$anilistId-$episode-$i",
                                    label = label,
                                    language = normLang,
                                    url = file,
                                    isDefault = isDef,
                                    source = "Anikoto"
                                )
                            )
                        }
                    }
                }

                // Parse Intro/Outro Skips
                var introSkip: SkipInterval? = null
                val intro = json.optJSONObject("intro")
                if (intro != null) {
                    val start = intro.optDouble("start", -1.0)
                    val end = intro.optDouble("end", -1.0)
                    if (start >= 0 && end > start) {
                        introSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Intro (OP)")
                    }
                }
                var outroSkip: SkipInterval? = null
                val outro = json.optJSONObject("outro")
                if (outro != null) {
                    val start = outro.optDouble("start", -1.0)
                    val end = outro.optDouble("end", -1.0)
                    if (start >= 0 && end > start) {
                        outroSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Outro (ED)")
                    }
                }

                val referer = "https://megaplay.buzz/"
                val finalPlayableUrl = DirectStreamResolver.buildProxiedStreamUrl(streamUrl, referer)

                val isVidstream = server.equals("vidstream", ignoreCase = true)
                val srvId = if (isVidstream) "vidstream-direct-$audio" else "megaplay-direct-$audio"
                val srvName = if (isVidstream) "Vidstream" else "MegaPlay"

                DirectStreamResult(
                    serverId = srvId,
                    serverName = srvName,
                    streamUrl = finalPlayableUrl,
                    isHls = true,
                    audio = audio,
                    headers = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Referer" to referer,
                        "Origin" to "https://megaplay.buzz"
                    ),
                    subtitles = subtitles,
                    introSkip = introSkip,
                    outroSkip = outroSkip,
                    isDirect = true
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveFromAnikotoSite(
        animeTitle: String,
        episode: Int,
        audio: String,
        server: String = "tcdn"
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        if (animeTitle.isBlank()) return@withContext null
        try {
            val searchUrl = "https://anikototv.to/filter?keyword=" + java.net.URLEncoder.encode(animeTitle, "UTF-8")
            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://anikototv.to/")
                .build()

            val searchHtml = client.newCall(searchReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body.string()
            }

            val slugMatch = Regex("""<a\s+class="name d-title"\s+href="https?://anikototv\.to/watch/([^"/]+)""").find(searchHtml)
                ?: Regex("""<a\s+href="https?://anikototv\.to/watch/([^"/]+)""").find(searchHtml)
            val slug = slugMatch?.groupValues?.get(1) ?: return@withContext null

            val watchUrl = "https://anikototv.to/watch/$slug"
            val watchReq = Request.Builder()
                .url(watchUrl)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://anikototv.to/")
                .build()

            val watchHtml = client.newCall(watchReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body.string()
            }

            val showIdMatch = Regex("""data-id="(\d+)"""").find(watchHtml)
            val showId = showIdMatch?.groupValues?.get(1) ?: return@withContext null

            val listReq = Request.Builder()
                .url("https://anikototv.to/ajax/episode/list/$showId")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", watchUrl)
                .build()

            val listJson = client.newCall(listReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            val listHtml = listJson.optString("result")
            val epRegex = Regex("""<a\s+[^>]*data-num="$episode"[^>]*data-ids="([^"]+)"[^>]*>""")
            val epMatch = epRegex.find(listHtml)
                ?: Regex("""<a\s+[^>]*data-ids="([^"]+)"[^>]*data-num="$episode"[^>]*>""").find(listHtml)
            val serverIds = epMatch?.groupValues?.get(1) ?: return@withContext null

            val serverReq = Request.Builder()
                .url("https://anikototv.to/ajax/server/list?servers=" + java.net.URLEncoder.encode(serverIds, "UTF-8"))
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", watchUrl)
                .build()

            val serverJson = client.newCall(serverReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            val serverHtml = serverJson.optString("result")
            val isDub = audio.equals("dub", ignoreCase = true)
            val isVidstream = server.equals("vidstream", ignoreCase = true)

            // Extract the appropriate audio section (SUB or DUB)
            val sectionRegex = if (isDub) {
                Regex("""(?:servers-dub|servers\s+dub|data-type=["']dub["'])[\s\S]*?(?:</div>\s*</div>|$)""", RegexOption.IGNORE_CASE)
            } else {
                Regex("""(?:servers-sub|servers\s+sub|data-type=["']sub["'])[\s\S]*?(?:</div>\s*</div>|$)""", RegexOption.IGNORE_CASE)
            }
            val sectionHtml = sectionRegex.find(serverHtml)?.value ?: serverHtml

            // Find link ID for vidstream or tcdn/hd-1 within the audio section
            val linkId = if (isVidstream) {
                Regex("""<li\s+[^>]*data-link-id=["']([^"']+)["'][^>]*>(?:[^<]*Vidstream[^<]*)</li>""", RegexOption.IGNORE_CASE).find(sectionHtml)?.groupValues?.get(1)
                    ?: Regex("""data-link-id=["']([^"']+)["'][^>]*>[^<]*vidstream""", RegexOption.IGNORE_CASE).find(sectionHtml)?.groupValues?.get(1)
            } else {
                Regex("""<li\s+[^>]*data-link-id=["']([^"']+)["'][^>]*>(?:[^<]*HD-1[^<]*)</li>""", RegexOption.IGNORE_CASE).find(sectionHtml)?.groupValues?.get(1)
                    ?: Regex("""data-link-id=["']([^"']+)["'][^>]*>[^<]*hd-1""", RegexOption.IGNORE_CASE).find(sectionHtml)?.groupValues?.get(1)
                    ?: Regex("""data-link-id=["']([^"']+)["']""").find(sectionHtml)?.groupValues?.get(1)
            } ?: return@withContext null

            val getEmbedReq = Request.Builder()
                .url("https://anikototv.to/ajax/server?get=" + java.net.URLEncoder.encode(linkId, "UTF-8"))
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", watchUrl)
                .build()

            val embedJson = client.newCall(getEmbedReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            val embedUrl = embedJson.optJSONObject("result")?.optString("url") ?: return@withContext null
            if (embedUrl.contains("megaplay")) {
                val mReq = Request.Builder()
                    .url(embedUrl)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Referer", "https://anikototv.to/")
                    .build()
                val mHtml = client.newCall(mReq).execute().use { it.body.string() }
                val mId = Regex("""data-id=["']([^"']+)["']""").find(mHtml)?.groupValues?.get(1) ?: return@withContext null
                val srvParam = if (server.isBlank() || server.equals("tcdn", ignoreCase = true)) "bcdn" else server
                val srcReq = Request.Builder()
                    .url("https://megaplay.buzz/stream/getSources?id=$mId&s=$srvParam")
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Referer", embedUrl)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                client.newCall(srcReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val data = JSONObject(resp.body.string())
                    val enc = data.optString("enc")
                    var streamUrl = if (enc.isNotBlank()) decryptMegaPlayPayload(enc) else data.optJSONObject("sources")?.optString("file")
                    if (!streamUrl.isNullOrBlank()) {
                        val finalUrl = DirectStreamResolver.buildProxiedStreamUrl(streamUrl, "https://megaplay.buzz/")
                        val sId = if (isVidstream) "vidstream-direct-$audio" else "megaplay-direct-$audio"
                        val sName = if (isVidstream) "Vidstream" else "MegaPlay"
                        return@withContext DirectStreamResult(
                            serverId = sId,
                            serverName = sName,
                            streamUrl = finalUrl,
                            isHls = true,
                            audio = audio,
                            headers = mapOf(
                                "User-Agent" to USER_AGENT,
                                "Referer" to "https://megaplay.buzz/",
                                "Origin" to "https://megaplay.buzz"
                            ),
                            isDirect = true
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveWithRelations(
        anilistId: Int,
        episode: Int,
        audio: String,
        animeTitle: String,
        server: String = "tcdn"
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        try {
            val details: Anime? = try { aniListRepo.detail(anilistId) } catch (_: Exception) { null }
            val relList: List<MediaRelation> = details?.relations ?: emptyList()
            val relations = relList.filter { rel ->
                rel.relationType.equals("PREQUEL", ignoreCase = true) ||
                rel.relationType.equals("PARENT", ignoreCase = true) ||
                rel.relationType.equals("SEQUEL", ignoreCase = true)
            }

            for (rel in relations) {
                val res1 = resolveDirectMegaPlayWithServer(rel.id, episode, audio, server)
                if (res1 != null) return@withContext res1

                if (rel.relationType.equals("PREQUEL", ignoreCase = true) && rel.episodes != null && rel.episodes > 0) {
                    val cumulativeEp = rel.episodes + episode
                    val resOffset = resolveDirectMegaPlayWithServer(rel.id, cumulativeEp, audio, server)
                    if (resOffset != null) return@withContext resOffset
                }

                val relTitle = if (rel.title.isNotBlank()) rel.title else (rel.romajiTitle ?: "")
                if (relTitle.isNotBlank()) {
                    val resTitle = resolveFromAnikotoSite(relTitle, episode, audio, server)
                    if (resTitle != null) return@withContext resTitle
                }
            }
        } catch (_: Exception) {}
        null
    }

    private fun decryptMegaPlayPayload(encValue: String): String? {
        return try {
            val encrypted = try {
                android.util.Base64.decode(encValue, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.DEFAULT)
            } catch (_: Throwable) {
                java.util.Base64.getUrlDecoder().decode(encValue)
            }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(MEGAPLAY_KEY, "AES"),
                IvParameterSpec(MEGAPLAY_IV)
            )
            val decrypted = cipher.doFinal(encrypted)
            val json = JSONObject(String(decrypted, Charsets.UTF_8))
            json.optString("file").takeIf { it.isNotBlank() } ?: json.optString("url").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
