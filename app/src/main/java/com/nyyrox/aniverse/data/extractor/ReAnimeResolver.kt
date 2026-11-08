package com.nyyrox.aniverse.data.extractor

import com.nyyrox.aniverse.data.SkipInterval
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ReAnimeResolver {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .build()

    private val streamCache = ConcurrentHashMap<String, DirectStreamResult>()

    fun clearStreamCache(anilistId: Int? = null, episode: Int? = null) {
        if (anilistId != null && episode != null) {
            val prefix = "$anilistId:$episode:"
            streamCache.keys.removeIf { it.startsWith(prefix) }
        } else {
            streamCache.clear()
        }
    }

    /**
     * Resolves Re:Anime direct stream mapped natively by AniList ID and episode.
     */
    suspend fun resolveStream(
        anilistId: Int,
        episode: Int,
        audio: String = "sub",
        animeTitle: String = "",
        preferredServer: String = ""
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        val targetAudio = audio.lowercase()
        val cacheKey = "$anilistId:$episode:$targetAudio:$preferredServer"
        val cached = streamCache[cacheKey]
        if (cached != null && !cached.streamUrl.contains("slopnet")) {
            return@withContext cached
        }

        // Tier 1: Client-Side Re:Anime Flix API + EncDec Decryption (Genuine FlixCloud with native + english-2 audio)
        try {
            val reanimeStream = withTimeoutOrNull(8000L) {
                resolveReanimeFlix(anilistId, episode, targetAudio, preferredServer)
            }
            if (reanimeStream != null && !reanimeStream.streamUrl.contains("slopnet")) {
                val tagged = reanimeStream.copy(
                    serverId = "flixcloud-$targetAudio",
                    serverName = "FlixCloud",
                    audio = targetAudio
                )
                streamCache[cacheKey] = tagged
                return@withContext tagged
            }
        } catch (_: Exception) {}

        null
    }

    private suspend fun resolveReanimeFlix(
        anilistId: Int,
        episode: Int,
        audio: String,
        preferredServer: String = ""
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        try {
            val flixReq = Request.Builder()
                .url("https://reanime.to/api/flix/$anilistId/$episode")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .build()

            val flixJson = client.newCall(flixReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            if (!flixJson.optBoolean("success")) return@withContext null
            val servers = flixJson.optJSONArray("servers") ?: return@withContext null
            if (servers.length() == 0) return@withContext null

            val matchingServers = mutableListOf<JSONObject>()
            for (i in 0 until servers.length()) {
                val s = servers.optJSONObject(i) ?: continue
                val dataType = s.optString("dataType")
                if (dataType.equals(audio, ignoreCase = true) ||
                    (audio == "dub" && dataType.contains("dub", ignoreCase = true)) ||
                    (audio == "sub" && (dataType.contains("sub", ignoreCase = true) || dataType.contains("raw", ignoreCase = true)))) {
                    matchingServers.add(s)
                }
            }

            if (matchingServers.isEmpty()) {
                servers.optJSONObject(0)?.let { matchingServers.add(it) }
            }

            val sortedServers = if (preferredServer.contains("2")) {
                matchingServers.sortedByDescending { it.optString("serverName").contains("2") }
            } else {
                matchingServers.sortedByDescending { it.optString("serverName").contains("1") }
            }

            var primaryResult: DirectStreamResult? = null
            val serverOptions = mutableListOf<StreamServerOption>()

            for (server in sortedServers) {
                val res = resolveSingleFlixServer(server, audio)
                if (res != null) {
                    val sName = if (server.optString("serverName").contains("2")) "FlixCloud-2" else "FlixCloud"
                    val optionId = "flixcloud-$audio"
                    serverOptions.add(
                        StreamServerOption(
                            serverId = optionId,
                            serverName = sName,
                            streamUrl = res.streamUrl,
                            isHls = true,
                            headers = res.headers
                        )
                    )
                    if (primaryResult == null) {
                        primaryResult = res
                    }
                }
            }

            primaryResult?.copy(availableServers = serverOptions)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveSingleFlixServer(
        serverObj: JSONObject,
        audio: String
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        try {
            val serverLabel = serverObj.optString("serverName", "FlixCloud")
            val dataLink = serverObj.optString("dataLink")
            if (dataLink.isBlank()) return@withContext null

            // Fetch Flixcloud Embed Page
            val embedReq = Request.Builder()
                .url(dataLink)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://reanime.to/")
                .build()

            val html = client.newCall(embedReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body.string()
            }

            val dataMatch = Regex("""type:\s*"data",\s*data:\s*(\{[\s\S]*?\})\s*,\s*uses:""").find(html)
            val dataJsonStr = dataMatch?.groupValues?.get(1) ?: return@withContext null
            // Fix unquoted JavaScript object keys (e.g. {f3adfb...:"...", iframe_domain:"..."})
            val fixedJson = dataJsonStr.replace(Regex("""([{,])(\s*)([a-zA-Z_${'$'}][a-zA-Z0-9_${'$'}]*)(\s*:\s*)(?=["'\[{0-9tfn\-])"""), """$1$2"$3"$4""")
            val dataObj = JSONObject(fixedJson)
            val subtitlesArray = dataObj.optJSONArray("subtitles")
            dataObj.remove("subtitles")

            // Decrypt Token via enc-dec.app
            val jsonMedia = "application/json; charset=utf-8".toMediaType()
            val tokenPayload = JSONObject().apply { put("data", dataObj) }.toString()
            val tokenReq = Request.Builder()
                .url("https://enc-dec.app/api/dec-flixcloud?type=token")
                .post(tokenPayload.toRequestBody(jsonMedia))
                .build()

            val tokenRespJson = client.newCall(tokenReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            if (tokenRespJson.optInt("status") != 200) return@withContext null
            val tokenResult = tokenRespJson.optJSONObject("result") ?: return@withContext null
            val token = tokenResult.optString("token")
            val context = tokenResult.optJSONObject("context") ?: return@withContext null

            // Fetch Token M3u8 Response from Flixcloud
            val m3u8Req = Request.Builder()
                .url("https://flixcloud.cc/api/m3u8/$token")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", "https://flixcloud.cc/")
                .build()

            val streamData = client.newCall(m3u8Req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            // Decrypt Stream URL via enc-dec.app
            val streamPayload = JSONObject().apply {
                val d = JSONObject().apply {
                    put("context", context)
                    put("stream_response", streamData)
                }
                put("data", d)
            }.toString()

            val streamDecReq = Request.Builder()
                .url("https://enc-dec.app/api/dec-flixcloud?type=stream")
                .post(streamPayload.toRequestBody(jsonMedia))
                .build()

            val streamDecResp = client.newCall(streamDecReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body.string())
            }

            if (streamDecResp.optInt("status") != 200) return@withContext null
            val decResult = streamDecResp.optJSONObject("result") ?: return@withContext null
            val rawStream = decResult.optString("stream")
            if (rawStream.isBlank() || !rawStream.startsWith("http")) return@withContext null
            val wPayload = decResult.optJSONObject("context")?.optString("w_payload").orEmpty()

            val manifestUrl = "https://enc-dec.app/api/parse-flixcloud?url=" +
                    URLEncoder.encode(rawStream, "UTF-8") +
                    "&w_payload=" + URLEncoder.encode(wPayload, "UTF-8")

            val subtitles = mutableListOf<SubtitleTrack>()
            if (subtitlesArray != null) {
                for (i in 0 until subtitlesArray.length()) {
                    val s = subtitlesArray.optJSONObject(i) ?: continue
                    val subUrl = s.optString("url")
                    val subLabel = s.optString("language", "English")
                    if (subUrl.isNotBlank()) {
                        subtitles.add(
                            SubtitleTrack(
                                id = "reanime-sub-$i",
                                label = subLabel,
                                language = subLabel.take(3).lowercase(),
                                url = subUrl,
                                isDefault = s.optBoolean("default", i == 0),
                                source = "FlixCloud"
                            )
                        )
                    }
                }
            }

            val finalPlayableUrl = DirectStreamResolver.buildProxiedStreamUrl(manifestUrl, "https://flixcloud.cc/")

            val skipObj = dataObj.optJSONObject("skip") ?: streamData.optJSONObject("skip") ?: decResult.optJSONObject("skip")
            val introObj = skipObj?.optJSONObject("intro")
                ?: dataObj.optJSONObject("intro")
                ?: dataObj.optJSONObject("intro_chapter")
                ?: streamData.optJSONObject("intro")
                ?: streamData.optJSONObject("intro_chapter")
                ?: decResult.optJSONObject("intro")
                ?: decResult.optJSONObject("intro_chapter")
            var introSkip: SkipInterval? = null
            if (introObj != null) {
                val start = introObj.optDouble("start", -1.0)
                val end = introObj.optDouble("end", -1.0)
                if (start >= 0 && end > start) {
                    introSkip = SkipInterval(Math.round(start * 1000.0), Math.round(end * 1000.0), "Intro (OP)")
                }
            }

            val outroObj = skipObj?.optJSONObject("outro")
                ?: dataObj.optJSONObject("outro")
                ?: dataObj.optJSONObject("outro_chapter")
                ?: streamData.optJSONObject("outro")
                ?: streamData.optJSONObject("outro_chapter")
                ?: decResult.optJSONObject("outro")
                ?: decResult.optJSONObject("outro_chapter")
            var outroSkip: SkipInterval? = null
            if (outroObj != null) {
                val start = outroObj.optDouble("start", -1.0)
                val end = outroObj.optDouble("end", -1.0)
                if (start >= 0 && end > start) {
                    outroSkip = SkipInterval(Math.round(start * 1000.0), Math.round(end * 1000.0), "Outro (ED)")
                }
            }

            DirectStreamResult(
                serverId = "flixcloud-$audio",
                serverName = "FlixCloud",
                streamUrl = finalPlayableUrl,
                isHls = true,
                audio = audio,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to "https://flixcloud.cc/"
                ),
                subtitles = subtitles,
                introSkip = introSkip,
                outroSkip = outroSkip,
                isDirect = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
