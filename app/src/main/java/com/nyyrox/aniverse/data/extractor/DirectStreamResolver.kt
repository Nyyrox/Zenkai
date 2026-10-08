package com.nyyrox.aniverse.data.extractor

import android.net.Uri
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.SkipInterval
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class StreamServerOption(
    val serverId: String,
    val serverName: String,
    val streamUrl: String,
    val isHls: Boolean = true,
    val headers: Map<String, String> = emptyMap()
)

data class DirectStreamResult(
    val serverId: String,
    val serverName: String,
    val streamUrl: String,
    val isHls: Boolean = true,
    val audio: String = "sub", // "sub" or "dub"
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val introSkip: SkipInterval? = null,
    val outroSkip: SkipInterval? = null,
    val isDirect: Boolean = true,
    val availableServers: List<StreamServerOption> = emptyList()
)

object DirectStreamResolver {
    private val ANIVEXA_WORKER_BASE: String
        get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.getWorkerUrl(
            "zenkai_stream_proxy",
            "https://zenkai-stream-proxy.ammhfoo.workers.dev"
        )
    private const val ANIVEXA_VERCEL_FALLBACK = "https://aniversee.vercel.app/api/anivexa"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .build()

    private val directStreamCache = ConcurrentHashMap<String, DirectStreamResult>()
    private val availableProvidersCache = ConcurrentHashMap<String, List<Provider>>()

    fun clearDirectStreamCache(providerId: String? = null, anilistId: Int? = null, episode: Int? = null, audio: String? = null) {
        if (providerId != null && anilistId != null && episode != null && audio != null) {
            directStreamCache.remove("$providerId:$anilistId:$episode:$audio")
        } else if (anilistId != null && episode != null) {
            val prefix = ":$anilistId:$episode:"
            directStreamCache.keys.removeIf { it.contains(prefix) }
        } else {
            directStreamCache.clear()
        }
        if (anilistId != null && episode != null) {
            AnikotoResolver.clearStreamCache(anilistId, episode)
            ReAnimeResolver.clearStreamCache(anilistId, episode)
        }
    }

    /**
     * Dynamically fetches only the providers that have active episodes for this anime and audio category.
     * Supports audioMode: "sub", "dub", or "multi".
     */
    suspend fun getAvailableDirectProviders(
        anilistId: Int,
        audioMode: String, // "sub" or "dub"
        episode: Int = 1
    ): List<Provider> = withContext(Dispatchers.IO) {
        val targetAudio = if (audioMode.equals("dub", ignoreCase = true)) "dub" else "sub"
        val cacheKey = "$anilistId:$targetAudio:$episode"
        availableProvidersCache[cacheKey]?.let { return@withContext it }

        val dynamicList = mutableListOf<Provider>()
        var apiSucceeded = false

        try {
            val req = Request.Builder()
                .url("$ANIVEXA_WORKER_BASE/episodes/$anilistId")
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    apiSucceeded = true
                    val body = resp.body.string()
                    val json = JSONObject(body)
                    val provObj = json.optJSONObject("providers")
                    if (provObj != null) {
                        val keys = provObj.keys()
                        while (keys.hasNext()) {
                            val provKey = keys.next()
                            val data = provObj.optJSONObject(provKey) ?: continue

                            val eps = data.optJSONArray(targetAudio)
                            val hasEpisode = if (eps != null) {
                                (0 until eps.length()).any { idx ->
                                    val epItem = eps.optJSONObject(idx)
                                    val num = epItem?.optInt("number", idx + 1) ?: (idx + 1)
                                    num == episode || eps.length() >= episode
                                }
                            } else false

                            if (hasEpisode) {
                                val prov = createProviderForDirectKey(provKey, targetAudio)
                                if (prov != null) {
                                    dynamicList.add(prov)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        val defaultList = getDefaultDirectProviders(targetAudio)
        val combined = if (dynamicList.isNotEmpty()) {
            val dynamicIds = dynamicList.map { it.id }.toSet()
            dynamicList + defaultList.filter { it.id !in dynamicIds }
        } else {
            defaultList
        }

        val result = combined.distinctBy { it.id }.sortedBy { prov ->
            val id = prov.id.lowercase()
            when {
                id.contains("flixcloud") || id.startsWith("hd-1") -> 0
                id.contains("megaplay") || id.startsWith("hd-2") -> 1
                id.contains("vidstream") || id.startsWith("hd-3") -> 2
                else -> 3
            }
        }

        availableProvidersCache[cacheKey] = result
        result
    }

    suspend fun getAvailableDirectProviders(
        anilistId: Int,
        isDub: Boolean,
        episode: Int = 1
    ): List<Provider> = getAvailableDirectProviders(anilistId, if (isDub) "dub" else "sub", episode)

    fun createProviderForDirectKey(key: String, audio: String): Provider? {
        val normalized = key.lowercase()
        return when {
            normalized == "flixcloud" || normalized == "reanime" || normalized == "hd1" || normalized == "hd-1" ->
                Provider("flixcloud-$audio", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true)
            normalized == "megaplay" || normalized == "anikoto" || normalized == "hd2" || normalized == "hd-2" ->
                Provider("megaplay-direct-$audio", "MegaPlay", "https://anikoto.to", "HD", isDirectStream = true)
            normalized == "vidstream" || normalized == "hd3" || normalized == "hd-3" ->
                Provider("vidstream-direct-$audio", "Vidstream", "https://anikoto.to", "HD", isDirectStream = true)
            normalized.contains("animesalt") || normalized.contains("acdn") || normalized.contains("salt") ->
                Provider("animesalt-direct-$audio", if (audio == "dub") "ACDN Dub" else "ACDN", "https://animesalt.cx", "HD", isDirectStream = true)
            else -> null
        }
    }

    fun getDefaultDirectProviders(audioMode: String): List<Provider> {
        val audio = if (audioMode.equals("dub", ignoreCase = true)) "dub" else "sub"
        val list = mutableListOf(
            Provider("flixcloud-$audio", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true),
            Provider("megaplay-direct-$audio", "MegaPlay", "https://anikoto.to", "HD", isDirectStream = true),
            Provider("vidstream-direct-$audio", "Vidstream", "https://anikoto.to", "HD", isDirectStream = true)
        )
        if (audio == "dub") {
            list.add(Provider("animesalt-direct-dub", "ACDN Dub", "https://animesalt.cx", "HD", isDirectStream = true))
        } else if (audioMode.equals("multi", ignoreCase = true)) {
            list.add(Provider("animesalt-direct-multi", "ACDN Multi", "https://animesalt.cx", "Multi-Audio", isDirectStream = true, isMultiAudio = true))
        } else {
            list.add(Provider("animesalt-direct-sub", "ACDN", "https://animesalt.cx", "HD", isDirectStream = true))
        }
        return list
    }

    fun getDefaultDirectProviders(isDub: Boolean): List<Provider> = getDefaultDirectProviders(if (isDub) "dub" else "sub")

    /**
     * Resolves a direct playable stream for the given AniList ID, episode, provider ID, and audio category.
     * Routes directly through client-side FlixCloud, MegaPlay (non-tcdn to bypass ISP blocks), Vidstream, and AnimeSalt (ACDN).
     */
    suspend fun resolveDirectStream(
        anilistId: Int,
        episode: Int,
        providerId: String,
        isDub: Boolean,
        animeTitle: String = ""
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        val audio = if (isDub) "dub" else "sub"
        val cacheKey = "$providerId:$anilistId:$episode:$audio"
        directStreamCache[cacheKey]?.let { return@withContext it }

        val normId = providerId.lowercase()
        val isAnimeSalt = normId.contains("animesalt") || normId.contains("acdn") || normId.contains("salt")
        val isVidstream = normId.contains("vidstream") || normId.contains("hd-3") || normId.contains("hd3")
        val isMegaPlay = normId.contains("megaplay") || (normId.contains("anikoto") && !normId.contains("vidstream")) || normId.contains("hd-2") || normId.contains("hd2")
        val isFlixCloud = normId.contains("flixcloud") || normId.contains("reanime") || normId.contains("hd-1") || normId.contains("hd1")

        val result = when {
            isAnimeSalt -> {
                resolveAnimeSaltDirect(anilistId, episode, animeTitle, audio)
            }
            isVidstream -> {
                // Vidstream: Anikoto / MegaPlay vidstream server
                AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "vidstream")
                    ?: AnikotoResolver.resolveDirectMegaPlayWithServer(anilistId, episode, audio, server = "vidstream")
            }
            isMegaPlay -> {
                // MegaPlay: uses bcdn/default to bypass regional ISP blocks on TikTokCDN
                AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "bcdn")
                    ?: AnikotoResolver.resolveDirectMegaPlayWithServer(anilistId, episode, audio, server = "bcdn")
                    ?: AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "vidstream")
            }
            isFlixCloud -> {
                // FlixCloud: ReAnime Flix API (with fallback to MegaPlay/Vidstream)
                ReAnimeResolver.resolveStream(anilistId, episode, audio, animeTitle, preferredServer = "FlixCloud")
                    ?: AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "bcdn")
                    ?: AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "vidstream")
            }
            else -> {
                // Default fallback
                AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "bcdn")
                    ?: AnikotoResolver.resolveStream(anilistId, episode, audio, animeTitle, server = "vidstream")
            }
        }

        val targetServerName = when {
            isAnimeSalt -> if (audio == "dub") "ACDN Dub" else "ACDN"
            isFlixCloud -> "FlixCloud"
            isMegaPlay -> "MegaPlay"
            isVidstream -> "Vidstream"
            else -> result?.serverName ?: "Direct HD"
        }
        val targetServerId = when {
            isAnimeSalt -> "animesalt-direct-$audio"
            isFlixCloud -> "flixcloud-$audio"
            isMegaPlay -> "megaplay-direct-$audio"
            isVidstream -> "vidstream-direct-$audio"
            else -> result?.serverId ?: providerId
        }

        val taggedResult = result?.copy(
            serverId = targetServerId,
            serverName = targetServerName,
            audio = audio
        )

        if (taggedResult != null) {
            directStreamCache[cacheKey] = taggedResult
        }
        taggedResult
    }

    private suspend fun resolveAnimeSaltDirect(
        anilistId: Int,
        episode: Int,
        animeTitle: String,
        audio: String = "dub"
    ): DirectStreamResult? {
        try {
            val candidate = com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.resolveCandidate(anilistId, episode, animeTitle)
            val saltId = candidate.saltId.ifBlank {
                com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.normalizeAnimeSaltTitle(animeTitle)
            }
            val season = candidate.season ?: 1
            val finalEp = candidate.finalEp
            val isMovie = candidate.isMovie
            val slug = if (isMovie) saltId else "$saltId-${season}x$finalEp"

            val serverTag = if (audio == "dub") "ACDN Dub" else "ACDN"
            val serverId = "animesalt-direct-$audio"

            // 1. AnimeSaltResolver.extractServers (options-0 / options-1)
            val extractedServers = com.nyyrox.aniverse.data.animesalt.AnimeSaltResolver.extractServers(slug, isMovie)

            if (audio == "dub") {
                // For DUB: try options-1 (Abyss player multi-lang) first for native English dub track
                val abyssIframe = extractedServers.abyssUrl
                if (!abyssIframe.isNullOrBlank()) {
                    val direct = MultiAudioExtractor.resolveIframeToServer(
                        abyssIframe,
                        "https://animesalt.cx/",
                        serverId,
                        serverTag,
                        preferredLanguage = "English"
                    )
                    if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                        return DirectStreamResult(
                            serverId = serverId,
                            serverName = serverTag,
                            streamUrl = direct.streamUrl,
                            isHls = direct.streamUrl.contains(".m3u8", ignoreCase = true),
                            audio = audio,
                            headers = direct.headers,
                            isDirect = true
                        )
                    }
                }

                // Fallback for DUB: options-0 (ravok.buzz / as-cdn) which contains multi-audio master HLS (including English)
                val myStreamIframe = extractedServers.myStreamUrl
                if (!myStreamIframe.isNullOrBlank()) {
                    val direct = MultiAudioExtractor.resolveIframeToServer(
                        myStreamIframe,
                        "https://animesalt.cx/",
                        serverId,
                        serverTag
                    )
                    if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                        return DirectStreamResult(
                            serverId = serverId,
                            serverName = serverTag,
                            streamUrl = direct.streamUrl,
                            isHls = true,
                            audio = audio,
                            headers = direct.headers,
                            isDirect = true
                        )
                    }
                }
            } else {
                // For SUB: try options-0 (ravok.buzz / as-cdn) master HLS first
                val myStreamIframe = extractedServers.myStreamUrl
                if (!myStreamIframe.isNullOrBlank()) {
                    val direct = MultiAudioExtractor.resolveIframeToServer(
                        myStreamIframe,
                        "https://animesalt.cx/",
                        serverId,
                        serverTag
                    )
                    if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                        return DirectStreamResult(
                            serverId = serverId,
                            serverName = serverTag,
                            streamUrl = direct.streamUrl,
                            isHls = true,
                            audio = audio,
                            headers = direct.headers,
                            isDirect = true
                        )
                    }
                }

                // Fallback for SUB: options-1 (Abyss player multi-lang) with Japanese audio
                val abyssIframe = extractedServers.abyssUrl
                if (!abyssIframe.isNullOrBlank()) {
                    val direct = MultiAudioExtractor.resolveIframeToServer(
                        abyssIframe,
                        "https://animesalt.cx/",
                        serverId,
                        serverTag,
                        preferredLanguage = "Japanese"
                    )
                    if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                        return DirectStreamResult(
                            serverId = serverId,
                            serverName = serverTag,
                            streamUrl = direct.streamUrl,
                            isHls = direct.streamUrl.contains(".m3u8", ignoreCase = true),
                            audio = audio,
                            headers = direct.headers,
                            isDirect = true
                        )
                    }
                }
            }

            // 2. Candidate episode HTML mirrors fallback
            val path = if (isMovie) "/movies/$saltId/" else "/episode/$saltId-${season}x$finalEp/"
            val candidateUrls = listOf(
                "https://animesalt.cx$path",
                "https://animesalt.ro$path",
                "https://animesalt.ac$path"
            )

            for (url in candidateUrls) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", USER_AGENT)
                        .addHeader("Referer", "https://animesalt.cx/")
                        .build()

                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val html = resp.body.string()

                            // If dub, check options-1 first
                            if (audio == "dub") {
                                val opt1Match = Regex("""<div[^>]+id=["']options-1["'][^>]*>[\s\S]*?<iframe[^>]+(?:data-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
                                    ?: Regex("""(?:data-src|src)=["']([^"']*player\.php[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
                                if (opt1Match != null) {
                                    val opt1Url = opt1Match.groupValues[1].let { if (it.startsWith("/")) "https://animesalt.cx$it" else it }
                                    val direct = MultiAudioExtractor.resolveIframeToServer(
                                        opt1Url, "https://animesalt.cx/", serverId, serverTag, preferredLanguage = "English"
                                    )
                                    if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                                        return DirectStreamResult(
                                            serverId = serverId,
                                            serverName = serverTag,
                                            streamUrl = direct.streamUrl,
                                            isHls = direct.streamUrl.contains(".m3u8", ignoreCase = true),
                                            audio = audio,
                                            headers = direct.headers,
                                            isDirect = true
                                        )
                                    }
                                }
                            }

                            // Match any CDN video iframe (ravok.buzz, as-cdn, etc.)
                            val cdnMatch = Regex("""(?:https?:)?//([a-zA-Z0-9_.-]+)/video/([a-f0-9]+)""", RegexOption.IGNORE_CASE).find(html)
                            if (cdnMatch != null) {
                                val cdnDomain = cdnMatch.groupValues[1]
                                val hash = cdnMatch.groupValues[2]
                                val direct = MultiAudioExtractor.resolveAsCdnDirectM3u8(cdnDomain, hash)
                                if (direct != null && direct.streamUrl.isNotBlank()) {
                                    return DirectStreamResult(
                                        serverId = serverId,
                                        serverName = serverTag,
                                        streamUrl = direct.streamUrl,
                                        isHls = true,
                                        audio = audio,
                                        headers = direct.headers,
                                        isDirect = true
                                    )
                                }
                            }

                            // If sub, check options-1 as fallback
                            val opt1Match = Regex("""<div[^>]+id=["']options-1["'][^>]*>[\s\S]*?<iframe[^>]+(?:data-src|src)=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
                                ?: Regex("""(?:data-src|src)=["']([^"']*player\.php[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
                            if (opt1Match != null) {
                                val opt1Url = opt1Match.groupValues[1].let { if (it.startsWith("/")) "https://animesalt.cx$it" else it }
                                val direct = MultiAudioExtractor.resolveIframeToServer(
                                    opt1Url, "https://animesalt.cx/", serverId, serverTag, preferredLanguage = "Japanese"
                                )
                                if (direct != null && direct.isDirectStream && direct.streamUrl.isNotBlank()) {
                                    return DirectStreamResult(
                                        serverId = serverId,
                                        serverName = serverTag,
                                        streamUrl = direct.streamUrl,
                                        isHls = direct.streamUrl.contains(".m3u8", ignoreCase = true),
                                        audio = audio,
                                        headers = direct.headers,
                                        isDirect = true
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Fallback: MultiAudioExtractor extraction (enforcing isDirectStream)
            val extracted = MultiAudioExtractor.extractServers(anilistId, episode, animeTitle)
            val first = extracted.firstOrNull { it.isDirectStream && it.streamUrl.isNotBlank() }
            if (first != null) {
                return DirectStreamResult(
                    serverId = serverId,
                    serverName = serverTag,
                    streamUrl = first.streamUrl,
                    isHls = first.streamUrl.contains(".m3u8", ignoreCase = true),
                    audio = audio,
                    headers = first.headers,
                    isDirect = true
                )
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun resolveYumeZoneZoko(
        anilistId: Int,
        episode: Int,
        audio: String = "sub",
        malId: Int? = null
    ): DirectStreamResult? = withContext(Dispatchers.IO) {
        try {
            val effMalId = malId ?: anilistId
            val url = "https://yumezone.qzz.io/api/zoko/source?mal=$effMalId&ani=$anilistId&ep=$episode&lang=$audio"
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body.string()
                val json = JSONObject(body)
                val rawStream = json.optString("stream_url")
                val proxiedM3u8 = json.optString("proxied_m3u8")
                val streamUrl = if (proxiedM3u8.isNotBlank()) {
                    if (proxiedM3u8.startsWith("http")) proxiedM3u8 else "https://yumezone.qzz.io$proxiedM3u8"
                } else rawStream

                if (streamUrl.isBlank()) return@use null

                val subtitles = mutableListOf<SubtitleTrack>()
                val subsArray = json.optJSONArray("subtitles") ?: json.optJSONArray("tracks")
                if (subsArray != null) {
                    for (i in 0 until subsArray.length()) {
                        val s = subsArray.optJSONObject(i) ?: continue
                        val subUrl = s.optString("url") ?: s.optString("file")
                        val subLang = s.optString("language", s.optString("label", "English"))
                        if (!subUrl.isNullOrBlank()) {
                            subtitles.add(
                                SubtitleTrack(
                                    id = "zoko-sub-$i",
                                    label = subLang,
                                    language = subLang.take(3).lowercase(),
                                    url = subUrl,
                                    isDefault = s.optBoolean("default", i == 0),
                                    source = "Zoko"
                                )
                            )
                        }
                    }
                }

                var introSkip: SkipInterval? = null
                val skipObj = json.optJSONObject("skip")
                val introObj = skipObj?.optJSONObject("intro") ?: json.optJSONObject("intro")
                if (introObj != null) {
                    val start = introObj.optDouble("start", -1.0)
                    val end = introObj.optDouble("end", -1.0)
                    if (start >= 0 && end > start) {
                        introSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Intro (OP)")
                    }
                }
                var outroSkip: SkipInterval? = null
                val outroObj = skipObj?.optJSONObject("outro") ?: json.optJSONObject("outro")
                if (outroObj != null) {
                    val start = outroObj.optDouble("start", -1.0)
                    val end = outroObj.optDouble("end", -1.0)
                    if (start >= 0 && end > start) {
                        outroSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Outro (ED)")
                    }
                }

                DirectStreamResult(
                    serverId = "yumezone-zoko-direct-$audio",
                    serverName = "Zoko HD",
                    streamUrl = streamUrl,
                    isHls = true,
                    audio = audio,
                    headers = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Referer" to "https://zokoanime.video/"
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

    fun fetchAnivexaProviderStream(
        provider: String,
        anilistId: Int,
        episode: Int,
        audio: String
    ): DirectStreamResult? {
        val endpoints = listOf(
            "$ANIVEXA_WORKER_BASE/watch/$provider/$anilistId/$audio/$provider-$episode",
            "$ANIVEXA_VERCEL_FALLBACK?provider=$provider&id=$anilistId&audio=$audio&ep=$episode"
        )

        for (endpoint in endpoints) {
            try {
                val req = Request.Builder()
                    .url(endpoint)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept", "application/json")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body.string()
                    val json = JSONObject(body)

                    var streamUrl = json.optString("stream_url").takeIf { it.isNotBlank() }
                    var referer = json.optJSONObject("headers")?.optString("Referer")
                    
                    if (streamUrl.isNullOrBlank()) {
                        val streamsArray = json.optJSONArray("streams")
                        if (streamsArray != null && streamsArray.length() > 0) {
                            val first = streamsArray.optJSONObject(0)
                            streamUrl = first?.optString("url")
                            if (referer.isNullOrBlank()) {
                                referer = first?.optJSONObject("headers")?.optString("Referer") ?: first?.optString("referer")
                            }
                        }
                    }

                    if (streamUrl.isNullOrBlank()) {
                        val sourcesArray = json.optJSONArray("sources")
                        if (sourcesArray != null && sourcesArray.length() > 0) {
                            val first = sourcesArray.optJSONObject(0)
                            streamUrl = first?.optString("url") ?: first?.optString("file")
                            if (referer.isNullOrBlank()) {
                                referer = first?.optJSONObject("headers")?.optString("Referer") ?: first?.optString("referer")
                            }
                        }
                    }

                    if (!streamUrl.isNullOrBlank()) {
                        if (streamUrl.contains("flixcloud", ignoreCase = true) || streamUrl.contains("megaplay", ignoreCase = true)) {
                            // Skip encrypted flixcloud payload or embed page
                            return@use
                        }
                        val serverName = json.optString("server", provider.replaceFirstChar { it.uppercase() })
                        val subtitles = mutableListOf<SubtitleTrack>()
                        val subsArray = json.optJSONArray("subtitles") ?: json.optJSONArray("tracks")
                        if (subsArray != null) {
                            for (i in 0 until subsArray.length()) {
                                val s = subsArray.optJSONObject(i) ?: continue
                                val subUrl = s.optString("url") ?: s.optString("file")
                                val subLang = s.optString("language", s.optString("label", "English"))
                                if (!subUrl.isNullOrBlank()) {
                                    subtitles.add(
                                        SubtitleTrack(
                                            id = "prov-sub-$i",
                                            label = subLang,
                                            language = subLang.take(3).lowercase(),
                                            url = subUrl,
                                            isDefault = s.optBoolean("default", i == 0),
                                            source = provider.replaceFirstChar { it.uppercase() }
                                        )
                                    )
                                }
                            }
                        }

                        // Intro/Outro Skip timestamps
                        var introSkip: SkipInterval? = null
                        val introObj = json.optJSONObject("intro")
                        if (introObj != null) {
                            val start = introObj.optDouble("start", -1.0)
                            val end = introObj.optDouble("end", -1.0)
                            if (start >= 0 && end > start) {
                                introSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Intro (OP)")
                            }
                        }
                        var outroSkip: SkipInterval? = null
                        val outroObj = json.optJSONObject("outro")
                        if (outroObj != null) {
                            val start = outroObj.optDouble("start", -1.0)
                            val end = outroObj.optDouble("end", -1.0)
                            if (start >= 0 && end > start) {
                                outroSkip = SkipInterval((start * 1000).toLong(), (end * 1000).toLong(), "Outro (ED)")
                            }
                        }

                        // Required headers for stream playback
                        val headers = mutableMapOf<String, String>()
                        headers["User-Agent"] = USER_AGENT
                        
                        val resolvedReferer = when {
                            !referer.isNullOrBlank() -> referer
                            provider == "reanime" -> "https://flixcloud.cc/"
                            provider == "kaa" -> "https://krussdomi.com/"
                            provider == "anibd" -> "https://playeng.animeapps.top/"
                            provider == "anikoto" -> "https://megaplay.buzz/"
                            provider == "anizone" -> "https://anizone.to/"
                            provider == "aniwaves" -> "https://aniwave.to/"
                            else -> {
                                val host = try { Uri.parse(streamUrl).host.orEmpty() } catch (_: Exception) { "" }
                                if (host.isNotBlank()) "https://$host/" else "https://flixcloud.cc/"
                            }
                        }

                        headers["Referer"] = resolvedReferer
                        headers["Origin"] = resolvedReferer.removeSuffix("/")

                        // Proxy through Cloudflare Worker proxy matching test_shaka_player.html (super fast playback & edge caching)
                        val finalPlayableUrl = if (!streamUrl.contains("/proxy?url=")) {
                            buildProxiedStreamUrl(streamUrl, resolvedReferer)
                        } else {
                            streamUrl
                        }

                        return DirectStreamResult(
                            serverId = "$provider-$audio",
                            serverName = "$serverName (Direct HD)",
                            streamUrl = finalPlayableUrl,
                            isHls = streamUrl.contains(".m3u8", ignoreCase = true) || finalPlayableUrl.contains(".m3u8", ignoreCase = true),
                            audio = audio,
                            headers = headers,
                            subtitles = subtitles,
                            introSkip = introSkip,
                            outroSkip = outroSkip,
                            isDirect = true
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }


    /**
     * Builds a proxied HLS master stream URL through the local client-side StreamProxyServer.
     */
    fun buildProxiedStreamUrl(originalUrl: String, referer: String = "https://flixcloud.cc/"): String {
        return try {
            com.nyyrox.aniverse.player.StreamProxyServer.buildProxyUrl(
                originalUrl,
                mapOf(
                    "Referer" to referer,
                    "Origin" to referer.removeSuffix("/"),
                    "User-Agent" to USER_AGENT
                )
            )
        } catch (_: Exception) {
            originalUrl
        }
    }
}
