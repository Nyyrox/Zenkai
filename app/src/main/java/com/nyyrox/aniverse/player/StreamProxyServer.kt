package com.nyyrox.aniverse.player

import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Client-Side Local HTTP Stream Proxy Server.
 * Runs on http://127.0.0.1:$port inside the application.
 *
 * Solves CDN blocking, CORS errors, and player 403 Forbidden issues by:
 * 1. Listening on 127.0.0.1 loopback interface.
 * 2. Forwarding requests to upstream CDNs with authentic browser Referer, Origin,
 *    Cookie, and User-Agent headers.
 * 3. Dynamically parsing and rewriting HLS (.m3u8) playlists so all nested
 *    sub-playlists, encryption keys, and .ts segment chunks automatically route
 *    through the local proxy with appropriate headers.
 * 4. Handling Range request headers for native seeking support in ExoPlayer.
 */
object StreamProxyServer {
    private const val TAG = "StreamProxyServer"
    private const val DEFAULT_START_PORT = 8765
    private const val MAX_PORT = 8800

    @Volatile
    private var serverPort = DEFAULT_START_PORT

    @Volatile
    private var isRunning = false

    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()

    @Volatile
    private var watchdogStarted = false

    @Synchronized
    fun start() {
        startWatchdog()
        if (isRunning && serverSocket != null && serverSocket?.isClosed == false) return

        try {
            var port = DEFAULT_START_PORT
            while (port < MAX_PORT) {
                try {
                    serverSocket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
                    serverPort = port
                    break
                } catch (_: Exception) {
                    port++
                }
            }
            isRunning = true
            Log.i(TAG, "🚀 Local Stream Proxy listening on http://127.0.0.1:$serverPort")

            val listenerThread = Thread {
                while (isRunning) {
                    try {
                        val ss = serverSocket
                        if (ss == null || ss.isClosed) {
                            if (isRunning) {
                                isRunning = false
                                start()
                            }
                            break
                        }
                        val clientSocket = ss.accept()
                        threadPool.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        try { Thread.sleep(200) } catch (_: Exception) {}
                    }
                }
            }
            listenerThread.isDaemon = true
            listenerThread.name = "AniverseLocalStreamProxy"
            listenerThread.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalStreamProxy: ${e.message}", e)
        }
    }

    private fun startWatchdog() {
        if (watchdogStarted) return
        watchdogStarted = true
        val watchdog = Thread {
            while (true) {
                try {
                    Thread.sleep(2500)
                    if (!isRunning || serverSocket == null || serverSocket?.isClosed == true) {
                        isRunning = false
                        try { serverSocket?.close() } catch (_: Exception) {}
                        serverSocket = null
                        start()
                    }
                } catch (_: Exception) {}
            }
        }
        watchdog.isDaemon = true
        watchdog.name = "AniverseProxyWatchdog"
        watchdog.start()
    }

    fun getPort(): Int = serverPort

    fun isServerRunning(): Boolean = isRunning && serverSocket != null && serverSocket?.isClosed == false

    /**
     * Converts a remote stream URL and its required headers into a client-side proxied URL.
     */
    fun buildProxyUrl(targetUrl: String, headers: Map<String, String>? = null): String {
        start()
        val port = getPort()
        val encUrl = URLEncoder.encode(targetUrl, "UTF-8")
        val sb = java.lang.StringBuilder("http://127.0.0.1:$port/proxy?url=$encUrl")
        if (!headers.isNullOrEmpty()) {
            val json = JSONObject(headers as Map<*, *>).toString()
            sb.append("&headers=").append(URLEncoder.encode(json, "UTF-8"))
        }
        return sb.toString()
    }

    /**
     * Converts target URL with optional referer/origin to client-side proxied URL.
     */
    fun buildApiProxyUrl(targetUrl: String, referer: String? = null, origin: String? = null): String {
        start()
        val port = getPort()
        val encUrl = URLEncoder.encode(targetUrl, "UTF-8")
        val sb = java.lang.StringBuilder("http://127.0.0.1:$port/api/proxy?url=$encUrl")
        if (!referer.isNullOrEmpty()) sb.append("&referer=").append(URLEncoder.encode(referer, "UTF-8"))
        if (!origin.isNullOrEmpty()) sb.append("&origin=").append(URLEncoder.encode(origin, "UTF-8"))
        return sb.toString()
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            val reader = input.bufferedReader(Charsets.UTF_8)
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }

            val method = parts[0].uppercase()
            val fullPath = parts[1]

            if (method == "OPTIONS") {
                sendCorsOptionsResponse(output)
                socket.close()
                return
            }

            var rangeHeader: String? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                if (line!!.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line!!.substring(6).trim()
                }
            }

            val queryMap = HashMap<String, String>()
            val queryIdx = fullPath.indexOf("?")
            val pathOnly = if (queryIdx != -1) fullPath.substring(0, queryIdx) else fullPath
            if (queryIdx != -1) {
                val queryStr = fullPath.substring(queryIdx + 1)
                for (param in queryStr.split("&")) {
                    val eq = param.indexOf("=")
                    if (eq > 0) {
                        val key = param.substring(0, eq)
                        val value = param.substring(eq + 1)
                        try {
                            queryMap[URLDecoder.decode(key, "UTF-8")] = URLDecoder.decode(value, "UTF-8")
                        } catch (_: Exception) {
                            queryMap[key] = value
                        }
                    }
                }
            }

            // Route: /proxy, /api/proxy, /stream or query containing url
            if (pathOnly == "/proxy" || pathOnly == "/stream" || pathOnly == "/api/proxy" || queryMap.containsKey("url")) {
                val targetUrl = queryMap["url"]
                if (targetUrl.isNullOrEmpty()) {
                    sendHttpError(output, 400, "Missing 'url' parameter")
                    socket.close()
                    return
                }

                // Extract custom headers
                val customHeaders = mutableMapOf<String, String>()
                queryMap["headers"]?.let { hJson ->
                    try {
                        val obj = JSONObject(hJson)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            customHeaders[k] = obj.optString(k)
                        }
                    } catch (_: Exception) {}
                }

                // Also check explicit query params
                queryMap["referer"]?.let { customHeaders["Referer"] = it }
                queryMap["origin"]?.let { customHeaders["Origin"] = it }
                queryMap["cookie"]?.let { customHeaders["Cookie"] = it }
                queryMap["ua"]?.let { customHeaders["User-Agent"] = it }

                proxyRequest(targetUrl, customHeaders, rangeHeader, method, output)
                return
            }

            sendHttpError(output, 404, "Endpoint not found on StreamProxyServer")
        } catch (e: Exception) {
            // Client closed connection during playback or seek
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun proxyRequest(
        targetUrl: String,
        headers: Map<String, String>,
        rangeHeader: String?,
        method: String,
        output: OutputStream
    ) {
        val lowUrl = targetUrl.lowercase()
        val isM3u8 = (lowUrl.contains(".m3u8") || lowUrl.contains("/hls-proxy") || lowUrl.contains("/m3u8-proxy") ||
                lowUrl.contains("type=m3u8") || lowUrl.contains("format=m3u8") ||
                lowUrl.contains("/master") || lowUrl.contains("/playlist") ||
                lowUrl.contains("/hls") || lowUrl.contains("/index") || lowUrl.contains("parse-flixcloud")) &&
                !lowUrl.contains(".mp4") && !lowUrl.contains(".ts") && !lowUrl.contains(".mkv") && !lowUrl.contains(".webm") &&
                !lowUrl.contains(".jpg") && !lowUrl.contains(".jpeg") && !lowUrl.contains(".png") && !lowUrl.contains(".webp") &&
                !lowUrl.contains(".key") && !lowUrl.contains(".bin")

        if (isM3u8 && method != "HEAD") {
            proxyM3u8Playlist(targetUrl, headers, output)
        } else {
            proxyMediaStream(targetUrl, headers, rangeHeader, method, output)
        }
    }

    /**
     * Rewrites HLS .m3u8 playlists dynamically so all nested relative and absolute URIs
     * also route through this client-side proxy with the required headers.
     */
    private fun proxyM3u8Playlist(
        targetUrl: String,
        headers: Map<String, String>,
        output: OutputStream
    ) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnectionWithRedirects(targetUrl, headers, null, "GET")
            val code = conn.responseCode
            if (code !in 200..299) {
                sendHttpError(output, code, "Upstream returned HTTP $code")
                conn.disconnect()
                return
            }

            val contentType = conn.contentType?.lowercase().orEmpty()
            val contentLength = conn.contentLengthLong
            val isBinary = contentType.contains("video") || contentType.contains("mp4") || contentType.contains("octet-stream") || contentLength > 2 * 1024 * 1024L
            if (isBinary) {
                // Upstream returned binary media instead of M3U8 text; stream directly to avoid OutOfMemoryError
                conn.disconnect()
                proxyMediaStream(targetUrl, headers, null, "GET", output)
                return
            }

            val rawText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val trimmedRaw = rawText.trim()
            if (!trimmedRaw.startsWith("#EXTM3U") && !trimmedRaw.contains("#EXT-X-") && !trimmedRaw.contains("#EXTINF")) {
                // Not an M3U8 playlist text; stream directly
                proxyMediaStream(targetUrl, headers, null, "GET", output)
                return
            }

            val finalUrl = conn.url.toString()
            // If the URL is an enc-dec parser proxy (e.g. enc-dec.app/api/parse-flixcloud?url=...),
            // relative URIs inside the playlist (like key.bin or segments) must resolve against the
            // embedded upstream stream URL, NOT the parser endpoint itself.
            val embeddedStreamUrl = try {
                val candidate = if (targetUrl.contains("url=")) targetUrl else finalUrl
                val match = Regex("""[?&]url=([^&]+)""").find(candidate)
                match?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
            } catch (_: Exception) { null }

            val effectiveBaseUrlStr = embeddedStreamUrl ?: finalUrl
            val baseUrl = try { URL(effectiveBaseUrlStr) } catch (_: Exception) { URL(targetUrl) }
            val port = getPort()
            val headersJson = if (headers.isNotEmpty()) {
                URLEncoder.encode(JSONObject(headers as Map<*, *>).toString(), "UTF-8")
            } else ""

            // Regex supporting URI="...", URI='...', and URI=...
            val uriAttrRegex = Regex("""URI=(?:"([^"]+)"|'([^']+)'|([^,\s]+))""")
            val rewrittenLines = rawText.lines().map { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("#") -> {
                        if (trimmed.contains("URI=")) {
                            uriAttrRegex.replace(trimmed) { match ->
                                val subUri = match.groupValues[1].takeIf { it.isNotEmpty() }
                                    ?: match.groupValues[2].takeIf { it.isNotEmpty() }
                                    ?: match.groupValues[3]
                                val resolvedSub = try {
                                    URL(baseUrl, subUri).toString()
                                } catch (_: Exception) {
                                    subUri
                                }
                                val proxiedSub = "http://127.0.0.1:$port/proxy?url=${URLEncoder.encode(resolvedSub, "UTF-8")}" +
                                        if (headersJson.isNotEmpty()) "&headers=$headersJson" else ""
                                """URI="$proxiedSub""""
                            }
                        } else {
                            trimmed
                        }
                    }
                    trimmed.isEmpty() -> {
                        trimmed
                    }
                    else -> {
                        // Segment or sub-playlist URI
                        val resolvedSegment = try {
                            URL(baseUrl, trimmed).toString()
                        } catch (_: Exception) {
                            trimmed
                        }
                        "http://127.0.0.1:$port/proxy?url=${URLEncoder.encode(resolvedSegment, "UTF-8")}" +
                                if (headersJson.isNotEmpty()) "&headers=$headersJson" else ""
                    }
                }
            }

            val rewrittenContent = rewrittenLines.joinToString("\n")
            val contentBytes = rewrittenContent.toByteArray(Charsets.UTF_8)

            val headerSb = java.lang.StringBuilder()
            headerSb.append("HTTP/1.1 200 OK\r\n")
            headerSb.append("Access-Control-Allow-Origin: *\r\n")
            headerSb.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
            headerSb.append("Access-Control-Allow-Headers: *\r\n")
            headerSb.append("Content-Type: application/vnd.apple.mpegurl\r\n")
            headerSb.append("Content-Length: ${contentBytes.size}\r\n")
            headerSb.append("Connection: close\r\n\r\n")

            output.write(headerSb.toString().toByteArray(Charsets.UTF_8))
            output.write(contentBytes)
            output.flush()

        } catch (e: Exception) {
            Log.w(TAG, "M3U8 proxy error: ${e.message}")
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * Streams binary video chunks (.ts, .mp4, keys) with Range header forwarding and CORS headers.
     */
    private fun proxyMediaStream(
        targetUrl: String,
        headers: Map<String, String>,
        rangeHeader: String?,
        method: String,
        output: OutputStream
    ) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnectionWithRedirects(targetUrl, headers, rangeHeader, method)
            val code = conn.responseCode

            val upstreamType = conn.contentType?.lowercase().orEmpty()
            if ((upstreamType.contains("mpegurl") || upstreamType.contains("application/x-mpegurl")) && method != "HEAD") {
                conn.disconnect()
                proxyM3u8Playlist(targetUrl, headers, output)
                return
            }

            val statusMsg = when (code) {
                200 -> "OK"
                206 -> "Partial Content"
                else -> conn.responseMessage ?: "OK"
            }

            val headerSb = java.lang.StringBuilder()
            headerSb.append("HTTP/1.1 $code $statusMsg\r\n")
            headerSb.append("Access-Control-Allow-Origin: *\r\n")
            headerSb.append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
            headerSb.append("Access-Control-Allow-Headers: *\r\n")
            headerSb.append("Access-Control-Expose-Headers: Content-Length, Content-Range, Content-Type, Accept-Ranges, ETag, Last-Modified\r\n")
            headerSb.append("Accept-Ranges: bytes\r\n")

            val low = targetUrl.lowercase()
            val isKey = low.endsWith(".bin") || low.endsWith(".key") || low.contains("key.bin") || low.contains("k=1")
            val isSubtitle = low.endsWith(".vtt") || low.endsWith(".srt") || low.contains("/subtitles/") || low.contains("type=vtt")
            val isMp4 = low.contains(".mp4") || low.contains(".m4s") || low.contains(".m4v")
            val isImage = (upstreamType.startsWith("image/") ||
                    low.contains("/covers/") || low.contains("/chapters/") ||
                    low.contains("comick") || low.contains("asurascans") ||
                    low.contains("mangadex") || low.contains("weebcentral") ||
                    low.endsWith(".png") || low.endsWith(".webp") || low.endsWith(".avif")) &&
                    !low.contains(".ts") && !low.contains(".m3u8")

            val finalContentType = when {
                isKey -> "application/octet-stream"
                isSubtitle -> "text/vtt"
                isMp4 -> "video/mp4"
                isImage -> if (upstreamType.startsWith("image/")) upstreamType else if (low.endsWith(".png")) "image/png" else if (low.endsWith(".webp")) "image/webp" else "image/jpeg"
                upstreamType.contains("mp4") -> "video/mp4"
                upstreamType.contains("webm") -> "video/webm"
                // Crucial fix: Upstream CDNs (MegaPlay, Vidstream, TikTokCDN, etc.) disguise video TS chunks with
                // fake extensions like .jpg, .html, .image and Content-Type: image/jpeg or text/html.
                // ExoPlayer requires video/MP2T for HLS MPEG-2 transport stream segments to decode properly.
                else -> "video/MP2T"
            }
            headerSb.append("Content-Type: $finalContentType\r\n")

            val contentLength = conn.contentLengthLong
            if (contentLength > 0) {
                headerSb.append("Content-Length: $contentLength\r\n")
            }

            conn.getHeaderField("Content-Range")?.let {
                headerSb.append("Content-Range: $it\r\n")
            }
            conn.getHeaderField("ETag")?.let {
                headerSb.append("ETag: $it\r\n")
            }
            conn.getHeaderField("Last-Modified")?.let {
                headerSb.append("Last-Modified: $it\r\n")
            }

            headerSb.append("Connection: close\r\n\r\n")

            output.write(headerSb.toString().toByteArray(Charsets.UTF_8))
            output.flush()

            if (method == "HEAD") {
                conn.disconnect()
                return
            }

            val stream: InputStream = if (code in 200..299 || code == 206) {
                conn.inputStream
            } else {
                conn.errorStream ?: run {
                    conn.disconnect()
                    return
                }
            }

            val buffer = ByteArray(65536)
            var read: Int
            try {
                while (stream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    output.flush()
                }
            } catch (_: SocketException) {
                // Client closed socket upon seek/pause
            } finally {
                try { stream.close() } catch (_: Exception) {}
                conn.disconnect()
            }

        } catch (e: Exception) {
            Log.w(TAG, "Media proxy stream error: ${e.message}")
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun openConnectionWithRedirects(
        targetUrl: String,
        headers: Map<String, String>,
        rangeHeader: String?,
        method: String
    ): HttpURLConnection {
        var currentUrl = targetUrl
        var hops = 0

        while (hops < 6) {
            val urlObj = URL(currentUrl)
            val conn = urlObj.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.requestMethod = if (method == "HEAD") "HEAD" else "GET"
            conn.instanceFollowRedirects = false

            // Standard browser headers
            conn.setRequestProperty("User-Agent", headers["User-Agent"] ?: StreamSniffer.TARGET_UA)
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            val host = urlObj.host
            val isAsCdn = host.contains("as-cdn") || currentUrl.contains("as-cdn")
            val effReferer = if (isAsCdn) "https://$host/" else headers["Referer"]
            val effOrigin = if (isAsCdn) "https://$host" else headers["Origin"]

            // Attach Referer and Origin
            effReferer?.let { conn.setRequestProperty("Referer", it) }
            effOrigin?.let { conn.setRequestProperty("Origin", it) }
            headers["Cookie"]?.let { conn.setRequestProperty("Cookie", it) }

            // Attach any other custom headers
            headers.forEach { (k, v) ->
                val lk = k.lowercase()
                if (lk != "referer" && lk != "origin" && lk != "cookie" && lk != "user-agent" && lk != "range") {
                    conn.setRequestProperty(k, v)
                }
            }

            if (!rangeHeader.isNullOrEmpty()) {
                conn.setRequestProperty("Range", rangeHeader)
            }

            val code = conn.responseCode
            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                if (!location.isNullOrEmpty()) {
                    currentUrl = URL(URL(currentUrl), location).toString()
                    conn.disconnect()
                    hops++
                    continue
                }
            }
            if ((code == 403 || code == 404 || code == 401 || code == 503) &&
                (headers.containsKey("Referer") || headers.containsKey("Origin") ||
                 currentUrl.contains("megaplay") || currentUrl.contains("vidnest") ||
                 currentUrl.contains("tryembed") || currentUrl.contains("zoko") ||
                 currentUrl.contains("4animo") || currentUrl.contains("recloud") ||
                 currentUrl.contains("yumezone") || currentUrl.contains("megacloud") ||
                 currentUrl.contains("vidstream") || currentUrl.contains("eclipseharbor") ||
                  currentUrl.contains("nexabloom") || currentUrl.contains("akirax") ||
                  currentUrl.contains("stellarfrontier") ||
                  currentUrl.contains("flixcloud") || currentUrl.contains("rundowncdn"))
            ) {
                conn.disconnect()
                val host = URL(currentUrl).host
                val isMegaplay = currentUrl.contains("megaplay") || currentUrl.contains("megacloud") ||
                        currentUrl.contains("vidstream") || currentUrl.contains("rapid-cloud") ||
                        currentUrl.contains("eclipseharbor") || currentUrl.contains("nexabloom") ||
                  currentUrl.contains("nexabloom") || currentUrl.contains("akirax") ||
                  currentUrl.contains("stellarfrontier") ||
                        (headers["Referer"] ?: "").contains("megaplay")
                val isVidnest = currentUrl.contains("vidnest") || (headers["Referer"] ?: "").contains("vidnest")
                val isTryEmbed = currentUrl.contains("tryembed") || (headers["Referer"] ?: "").contains("tryembed")
                val isZoko = currentUrl.contains("zoko") || (headers["Referer"] ?: "").contains("zoko")
                val isReCloud = currentUrl.contains("4animo") || currentUrl.contains("recloud") || (headers["Referer"] ?: "").contains("4animo")
                val isYumeZone = currentUrl.contains("yumezone") || currentUrl.contains("yumizone") || (headers["Referer"] ?: "").contains("yumezone")
                val isAnimeSalt = currentUrl.contains("animesalt") || currentUrl.contains("as-cdn") ||
                        (headers["Referer"] ?: "").contains("animesalt")
                val isFlixCloud = currentUrl.contains("flixcloud") || currentUrl.contains("rundowncdn") ||
                        (headers["Referer"] ?: "").contains("flixcloud")

                // Candidate header strategies to overcome CDN 403/404 restrictions
                val retryStrategies = mutableListOf<Pair<String?, String?>>()
                if (isMegaplay) {
                    retryStrategies.add("https://megaplay.buzz/" to "https://megaplay.buzz")
                    retryStrategies.add("https://megacloud.tv/" to "https://megacloud.tv")
                    retryStrategies.add("https://vidstream.pro/" to "https://vidstream.pro")
                }
                if (isVidnest) {
                    retryStrategies.add("https://vidnest.fun/" to "https://vidnest.fun")
                }
                if (isTryEmbed) {
                    retryStrategies.add("https://tryembed.us.cc/" to "https://tryembed.us.cc")
                }
                if (isZoko) {
                    retryStrategies.add("https://zokoanime.video/" to "https://zokoanime.video")
                }
                if (isReCloud) {
                    retryStrategies.add("https://cdn.4animo.xyz/" to "https://cdn.4animo.xyz")
                }
                if (isYumeZone) {
                    retryStrategies.add("https://yumezone.qzz.io/" to "https://yumezone.qzz.io")
                }
                if (isAnimeSalt) {
                    retryStrategies.add("https://animesalt.cc/" to "https://animesalt.cc")
                }
                if (isFlixCloud) {
                    retryStrategies.add("https://flixcloud.cc/" to "https://flixcloud.cc")
                    retryStrategies.add("https://reanime.to/" to "https://reanime.to")
                }
                retryStrategies.add("https://$host/" to "https://$host")
                retryStrategies.add(null to null) // Neutral: no Referer/Origin

                for ((ref, orig) in retryStrategies) {
                    try {
                        val retryConn = URL(currentUrl).openConnection() as HttpURLConnection
                        retryConn.connectTimeout = 12000
                        retryConn.readTimeout = 20000
                        retryConn.requestMethod = if (method == "HEAD") "HEAD" else "GET"
                        retryConn.instanceFollowRedirects = true
                        retryConn.setRequestProperty("User-Agent", headers["User-Agent"] ?: StreamSniffer.TARGET_UA)
                        retryConn.setRequestProperty("Accept", "*/*")
                        if (ref != null) retryConn.setRequestProperty("Referer", ref)
                        if (orig != null) retryConn.setRequestProperty("Origin", orig)
                        headers["Cookie"]?.let { retryConn.setRequestProperty("Cookie", it) }
                        if (!rangeHeader.isNullOrEmpty()) {
                            retryConn.setRequestProperty("Range", rangeHeader)
                        }
                        val rCode = retryConn.responseCode
                        if (rCode in 200..299 || rCode == 206) {
                            return retryConn
                        }
                        retryConn.disconnect()
                    } catch (_: Exception) {}
                }
            }
            return conn
        }

        return URL(currentUrl).openConnection() as HttpURLConnection
    }

    private fun sendCorsOptionsResponse(output: OutputStream) {
        val res = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, HEAD, OPTIONS, POST, PUT, DELETE\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Allow-Credentials: true\r\n" +
                "Access-Control-Max-Age: 86400\r\n" +
                "Connection: close\r\n\r\n"
        output.write(res.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendHttpError(output: OutputStream, code: Int, message: String) {
        try {
            val json = "{\"error\":${JSONObject.quote(message)}}"
            val res = "HTTP/1.1 $code Error\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Content-Length: ${json.toByteArray().size}\r\n" +
                    "Connection: close\r\n\r\n$json"
            output.write(res.toByteArray(Charsets.UTF_8))
            output.flush()
        } catch (_: Exception) {}
    }
}
