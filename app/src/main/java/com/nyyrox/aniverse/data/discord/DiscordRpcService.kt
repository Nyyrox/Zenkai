package com.nyyrox.aniverse.data.discord

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object DiscordRpcService {
    private const val TAG = "DiscordRpc"
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lastSequence: Int? = null
    private var pendingPresence: JSONObject? = null
    private var activeToken: String? = null
    private var activeIsBot: Boolean = true

    private val _rpcStatus = MutableStateFlow("Idle")
    val rpcStatus = _rpcStatus.asStateFlow()

    @Volatile
    private var currentAnimeTitle: String? = null
    @Volatile
    private var currentEpisode: Int? = null
    @Volatile
    private var lastProgressMs: Long = 0L

    private val assetCache = ConcurrentHashMap<String, String>()

    /**
     * Resolves the token to use.
     * If user explicitly provided a Discord User Token, returns (userToken, false).
     * Otherwise returns (official Bot token, true).
     */
    private fun resolveToken(): Pair<String, Boolean> {
        val userTok = DiscordAccountManager.accountState.value?.userToken?.trim()
            ?.replace("\"", "")?.replace("Bearer ", "")?.replace("Bot ", "")
        val oauthTok = DiscordAccountManager.accountState.value?.accessToken?.trim()

        // If user explicitly provided a user account token (must not be an OAuth bearer token or Supabase JWT):
        if (!userTok.isNullOrBlank() && !userTok.startsWith("ey") && userTok != oauthTok && userTok.length >= 20) {
            return Pair(userTok, false)
        }

        // Fallback to official Zenkai BOT token
        return Pair(DiscordConfig.BOT_TOKEN, true)
    }

    /**
     * Pre-fetches and caches an external image asset via Discord's external-assets API.
     */
    fun preFetchAsset(url: String?, clientId: String = DiscordConfig.CLIENT_ID) {
        if (url.isNullOrBlank() || !url.startsWith("http")) return
        if (assetCache.containsKey(url)) return
        scope.launch {
            resolveExternalAsset(url, clientId)
        }
    }

    /**
     * Resolves an external image URL to an mp:external/... asset path via Discord's API.
     */
    suspend fun resolveExternalAsset(url: String, clientId: String = DiscordConfig.CLIENT_ID): String? = withContext(Dispatchers.IO) {
        val cached = assetCache[url]
        if (cached != null) return@withContext cached

        val userTok = DiscordAccountManager.accountState.value?.userToken?.trim()
            ?.replace("\"", "")?.replace("Bearer ", "")?.replace("Bot ", "")
        val accessTok = DiscordAccountManager.accountState.value?.accessToken?.trim()

        val tokensToTry = mutableListOf<String>()
        if (!userTok.isNullOrBlank() && !userTok.startsWith("ey") && userTok.length >= 20) {
            tokensToTry.add(userTok)
        }
        if (!accessTok.isNullOrBlank()) {
            tokensToTry.add("Bearer $accessTok")
        }

        for (authHeader in tokensToTry) {
            try {
                val reqBody = JSONObject().apply {
                    put("urls", JSONArray().apply { put(url) })
                }
                val req = Request.Builder()
                    .url("https://discord.com/api/v10/applications/$clientId/external-assets")
                    .post(reqBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string() ?: ""
                        val arr = JSONArray(bodyStr)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            val path = obj.optString("external_asset_path")
                            if (path.isNotBlank()) {
                                val finalPath = when {
                                    path.startsWith("mp:") -> path
                                    path.startsWith("/") -> "mp:${path.removePrefix("/")}"
                                    else -> "mp:$path"
                                }
                                assetCache[url] = finalPath
                                Log.d(TAG, "Resolved external asset successfully: $finalPath")
                                return@withContext finalPath
                            }
                        }
                    } else {
                        Log.w(TAG, "External asset resolve returned HTTP ${resp.code} for auth: $authHeader")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving external asset: ${e.message}")
            }
        }
        return@withContext null
    }

    /**
     * Updates Discord Rich Presence.
     * Shows: "Watching <Anime Title> on Zenkai", details: Anime Title, state: Episode: X, timestamps: elapsed timer.
     */
    fun updatePresence(
        animeTitle: String,
        episodeNum: Int,
        isDub: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        coverUrl: String? = null,
        appClientId: String = DiscordConfig.CLIENT_ID
    ) {
        if (!DiscordAccountManager.isRpcEnabled()) {
            clearPresence()
            return
        }

        val (token, isBot) = resolveToken()
        if (token.isBlank()) return

        // Throttle updates to avoid spamming the Gateway (send at most every 4 seconds unless state changed)
        val now = System.currentTimeMillis()
        if (currentAnimeTitle == animeTitle && currentEpisode == episodeNum && Math.abs(currentPositionMs - lastProgressMs) < 4000L && isConnected.get()) {
            return
        }

        currentAnimeTitle = animeTitle
        currentEpisode = episodeNum
        lastProgressMs = currentPositionMs

        sendPresencePayload(
            animeTitle = animeTitle,
            episodeNum = episodeNum,
            isDub = isDub,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            coverUrl = coverUrl,
            appClientId = appClientId,
            token = token,
            isBot = isBot
        )

        // If coverUrl is provided and not yet cached, resolve asynchronously and push presence once resolved
        if (!coverUrl.isNullOrBlank() && coverUrl.startsWith("http") && !assetCache.containsKey(coverUrl)) {
            scope.launch {
                val resolved = resolveExternalAsset(coverUrl, appClientId)
                if (resolved != null && currentAnimeTitle == animeTitle && currentEpisode == episodeNum) {
                    val (curToken, curIsBot) = resolveToken()
                    sendPresencePayload(
                        animeTitle = animeTitle,
                        episodeNum = episodeNum,
                        isDub = isDub,
                        currentPositionMs = lastProgressMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        coverUrl = coverUrl,
                        appClientId = appClientId,
                        token = curToken,
                        isBot = curIsBot
                    )
                }
            }
        }
    }

    private fun sendPresencePayload(
        animeTitle: String,
        episodeNum: Int,
        isDub: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        coverUrl: String?,
        appClientId: String,
        token: String,
        isBot: Boolean
    ) {
        val now = System.currentTimeMillis()
        val startTimestamp = now - currentPositionMs

        val timestampsObj = JSONObject().apply {
            put("start", startTimestamp)
        }

        val resolvedAsset = if (!coverUrl.isNullOrBlank()) assetCache[coverUrl] else null
        val assetsObj = JSONObject().apply {
            if (!resolvedAsset.isNullOrBlank()) {
                put("large_image", resolvedAsset)
                put("large_text", animeTitle)
            } else if (!coverUrl.isNullOrBlank() && coverUrl.startsWith("http")) {
                put("large_image", coverUrl)
                put("large_text", animeTitle)
            }
        }

        val activity = JSONObject().apply {
            put("name", "$animeTitle on Zenkai") // Renders as "Watching <Anime Title> on Zenkai"
            put("type", 3) // 3 = Watching
            put("details", animeTitle) // Bold primary title
            put("state", "Episode: $episodeNum") // Clean format: "Episode: 1"
            put("timestamps", timestampsObj)
            if (assetsObj.length() > 0) {
                put("assets", assetsObj)
            }
            put("application_id", appClientId)
        }

        val presencePayload = JSONObject().apply {
            put("op", 3) // Presence Update
            val d = JSONObject().apply {
                put("since", if (isPlaying) now else null)
                put("activities", JSONArray().apply { put(activity) })
                put("status", if (isPlaying) "online" else "idle")
                put("afk", !isPlaying)
            }
            put("d", d)
        }

        pendingPresence = presencePayload
        _rpcStatus.value = if (isBot) "Broadcasting (Zenkai Bot)" else "Broadcasting (Personal Profile)"

        if (isConnected.get() && webSocket != null && activeToken == token) {
            try {
                webSocket?.send(presencePayload.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send presence: ${e.message}")
                connectGateway(token, isBot)
            }
        } else {
            connectGateway(token, isBot)
        }
    }

    fun pausePresence() {
        val title = currentAnimeTitle ?: return
        val ep = currentEpisode ?: 1
        if (!isConnected.get() || webSocket == null) return

        val activity = JSONObject().apply {
            put("name", "$title on Zenkai")
            put("type", 3)
            put("details", title)
            put("state", "Episode: $ep (Paused)")
            put("application_id", DiscordConfig.CLIENT_ID)
        }

        val presencePayload = JSONObject().apply {
            put("op", 3)
            val d = JSONObject().apply {
                put("since", System.currentTimeMillis())
                put("activities", JSONArray().apply { put(activity) })
                put("status", "idle")
                put("afk", true)
            }
            put("d", d)
        }
        pendingPresence = presencePayload
        try {
            webSocket?.send(presencePayload.toString())
            _rpcStatus.value = "Paused"
        } catch (_: Exception) {}
    }

    fun clearPresence() {
        currentAnimeTitle = null
        currentEpisode = null
        lastProgressMs = 0L
        pendingPresence = null
        _rpcStatus.value = "Idle"

        val ws = webSocket
        if (ws != null && isConnected.get()) {
            val clearPayload = JSONObject().apply {
                put("op", 3)
                put("d", JSONObject().apply {
                    put("since", null)
                    put("activities", JSONArray())
                    put("status", "online")
                    put("afk", false)
                })
            }
            try {
                ws.send(clearPayload.toString())
                Log.d(TAG, "Sent clear presence to Gateway")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send clear presence: ${e.message}")
            }
            // Delay disconnect so Discord Gateway has time to process and broadcast the cleared presence
            scope.launch {
                delay(2500)
                if (currentAnimeTitle == null) {
                    disconnect()
                }
            }
        } else {
            disconnect()
        }
    }

    @Synchronized
    private fun connectGateway(token: String, isBot: Boolean) {
        if (isConnected.get() && activeToken == token && webSocket != null) return
        disconnect()

        activeToken = token
        activeIsBot = isBot
        _rpcStatus.value = "Connecting..."

        val req = Request.Builder()
            .url(DiscordConfig.GATEWAY_URL)
            .build()

        webSocket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Gateway connection established")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val op = json.optInt("op", -1)
                    val s = json.optInt("s", -1)
                    if (s >= 0) lastSequence = s

                    when (op) {
                        10 -> { // HELLO
                            val d = json.getJSONObject("d")
                            val heartbeatInterval = d.getLong("heartbeat_interval")
                            startHeartbeat(heartbeatInterval)
                            sendIdentify(ws, token, isBot)
                        }
                        0 -> { // DISPATCH
                            val t = json.optString("t")
                            if (t == "READY") {
                                isConnected.set(true)
                                reconnectAttempts = 0
                                Log.d(TAG, "Gateway READY (isBot=$isBot), sending pending presence")
                                _rpcStatus.value = if (isBot) "Broadcasting (Zenkai Bot)" else "Broadcasting (Personal Profile)"
                                pendingPresence?.let { ws.send(it.toString()) }
                            }
                        }
                        1 -> { // Discord requested heartbeat
                            sendHeartbeat()
                        }
                        7, 9 -> { // RECONNECT or INVALID_SESSION
                            Log.w(TAG, "Gateway reconnect opcode $op received")
                            disconnect()
                            scheduleReconnect(token, isBot)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gateway parse error: ${e.message}")
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                webSocket = null
                Log.w(TAG, "Gateway closed: code=$code reason=$reason")
                _rpcStatus.value = "Disconnected"

                if (code == 4004 && !isBot) {
                    // User token was rejected (authentication failed)
                    Log.e(TAG, "User token failed authentication (4004); resetting custom user token and falling back to Zenkai Bot")
                    DiscordAccountManager.setUserToken(null)
                    scope.launch {
                        delay(1000)
                        connectGateway(DiscordConfig.BOT_TOKEN, true)
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                webSocket = null
                Log.w(TAG, "Gateway failure: ${t.message}")
                _rpcStatus.value = "Connection error"
                scheduleReconnect(token, isBot)
            }
        })
    }

    private fun sendIdentify(ws: WebSocket, token: String, isBot: Boolean) {
        val identify = JSONObject().apply {
            put("op", 2)
            val d = JSONObject().apply {
                put("token", token)
                if (isBot) {
                    put("intents", 0)
                }
                put("properties", JSONObject().apply {
                    put("\$os", "windows")
                    put("\$browser", "Discord Client")
                    put("\$device", "discord-client")
                })
                pendingPresence?.optJSONObject("d")?.let { p ->
                    put("presence", p)
                }
            }
            put("d", d)
        }
        ws.send(identify.toString())
    }

    private fun scheduleReconnect(token: String, isBot: Boolean) {
        reconnectJob?.cancel()
        if (reconnectAttempts > 4) {
            Log.w(TAG, "Exceeded maximum reconnect attempts")
            return
        }
        reconnectAttempts++
        val backoffMs = (reconnectAttempts * 3000L).coerceAtMost(20000L)
        reconnectJob = scope.launch {
            delay(backoffMs)
            if (!isConnected.get() && pendingPresence != null) {
                connectGateway(token, isBot)
            }
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val hb = JSONObject().apply {
            put("op", 1)
            put("d", lastSequence ?: JSONObject.NULL)
        }
        try {
            webSocket?.send(hb.toString())
        } catch (_: Exception) {}
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        isConnected.set(false)
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
    }
}
