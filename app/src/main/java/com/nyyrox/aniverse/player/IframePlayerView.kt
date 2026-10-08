package com.nyyrox.aniverse.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class WebAppInterface(
    private val onNextEpisode: (() -> Unit)?,
    private val onTimeUpdate: ((currentTime: Double, duration: Double) -> Unit)? = null,
    private val onPlayerEvent: ((event: String, currentTime: Double, duration: Double, isPaused: Boolean) -> Unit)? = null
) {
    @android.webkit.JavascriptInterface
    fun onNextEpisode() {
        onNextEpisode?.invoke()
    }

    @android.webkit.JavascriptInterface
    fun onTimeUpdate(currentTime: Double, duration: Double) {
        onTimeUpdate?.invoke(currentTime, duration)
    }

    @android.webkit.JavascriptInterface
    fun onPlayerEvent(event: String, currentTime: Double, duration: Double, isPaused: Boolean) {
        onPlayerEvent?.invoke(event, currentTime, duration, isPaused)
    }
}

/**
 * High-performance, ad-intercepted Iframe Player View.
 * Wraps embed streams inside a sandboxed iframe that allows video controls
 * and autoplay while strictly blocking click-popups, ad redirects, and external window opens.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IframePlayerView(
    embedUrl: String,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onTimeUpdate: ((currentTime: Double, duration: Double) -> Unit)? = null,
    onPlayerEvent: ((event: String, currentTime: Double, duration: Double, isPaused: Boolean) -> Unit)? = null
) {
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun buildIframeHtml(url: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                    iframe { border: none; width: 100%; height: 100%; display: block; }
                </style>
                <script>
                    window.addEventListener('message', function(e) {
                        var d = e.data;
                        if (typeof d === 'string') {
                            try { d = JSON.parse(d); } catch(err) {}
                        }
                        if (!d) return;

                        // MegaPlay / MegaCloud channel handling
                        if (d.channel === 'megacloud' || d.channel === 'megaplay' || d.event === 'time' || d.type === 'watching-log') {
                            var cur = d.currentTime || (d.data && d.data.currentTime) || 0;
                            var dur = d.duration || (d.data && d.data.duration) || 0;
                            if (window.AndroidPlayer && window.AndroidPlayer.onTimeUpdate) {
                                window.AndroidPlayer.onTimeUpdate(cur, dur);
                            }
                        }

                        // Aggressive State Broadcast (Every 100ms)
                        if (d.type === 'PLAYER_EVENT' && d.data) {
                            var p = d.data;
                            if (window.AndroidPlayer && window.AndroidPlayer.onPlayerEvent) {
                                window.AndroidPlayer.onPlayerEvent(p.event || 'timeupdate', p.currentTime || 0, p.duration || 0, p.paused === true);
                            }
                        } else if (d.type === 'TIME_UPDATE' || d.event === 'timeupdate') {
                            if (window.AndroidPlayer && window.AndroidPlayer.onTimeUpdate) {
                                window.AndroidPlayer.onTimeUpdate(d.currentTime || 0, d.duration || 0);
                            }
                        }

                        // Ended / Next Episode
                        if (d === 'ended' || d === 'next' || d === 'next_episode' || d === 'complete' ||
                            d.type === 'NEXT_EPISODE' || d.type === 'complete' || d.event === 'complete' ||
                            (d && (d.event === 'ended' || d.type === 'ended' || d.event === 'next' || d.event === 'next_episode'))) {
                            if (window.AndroidPlayer && window.AndroidPlayer.onNextEpisode) {
                                window.AndroidPlayer.onNextEpisode();
                            }
                        }
                    });

                    function sendCommandToPlayer(cmd) {
                        var ifr = document.getElementById('player_iframe');
                        if (ifr && ifr.contentWindow) {
                            ifr.contentWindow.postMessage(cmd, '*');
                        }
                    }
                </script>
            </head>
            <body>
                <iframe id="player_iframe" src="$url"
                    allow="autoplay; fullscreen; encrypted-media; picture-in-picture; accelerometer; gyroscope"
                    allowfullscreen>
                </iframe>
            </body>
            </html>
        """.trimIndent()
    }

    fun loadEmbedUrl(wv: WebView, url: String) {
        wv.tag = url
        val embedUri = try { Uri.parse(url) } catch (_: Exception) { null }
        val host = embedUri?.host.orEmpty()
        val referer = when {
            host.contains("megacloud") || host.contains("megaplay") || host.contains("vidstream") || host.contains("rapid-cloud") -> "https://megaplay.buzz/"
            host.contains("vidnest") -> "https://vidnest.fun/"
            host.contains("tryembed") -> "https://tryembed.us.cc/"
            host.contains("zokoanime") -> "https://zokoanime.video/"
            host.contains("4animo") || host.contains("recloud") -> "https://cdn.4animo.xyz/"
            host.contains("yumezone") -> "https://yumezone.qzz.io/"
            host.contains("streamwish") || host.contains("filemoon") || host.contains("dood") -> "https://$host/"
            host.isNotBlank() -> "https://$host/"
            else -> "https://megaplay.buzz/"
        }
        val headers = mapOf(
            "Referer" to referer,
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        )
        wv.loadUrl(url, headers)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        if (customView != null) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.BLACK)

                        android.webkit.CookieManager.getInstance().let { cm ->
                            cm.setAcceptCookie(true)
                            cm.setAcceptThirdPartyCookies(this, true)
                        }

                        addJavascriptInterface(
                            WebAppInterface(onNextEpisode, onTimeUpdate, onPlayerEvent),
                            "AndroidPlayer"
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = true
                            allowContentAccess = true
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                                if (!isFullscreen) {
                                    onToggleFullscreen?.invoke()
                                }
                            }

                            override fun onHideCustomView() {
                                customViewCallback?.onCustomViewHidden()
                                customView = null
                                customViewCallback = null
                                if (isFullscreen) {
                                    onToggleFullscreen?.invoke()
                                }
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                // Block ad popup windows spawned by video player click
                                return false
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val targetUrl = request?.url?.toString().orEmpty()
                                if (targetUrl.isBlank()) return true

                                // Block intent://, market://, and other app redirects
                                if (targetUrl.startsWith("intent://") ||
                                    targetUrl.startsWith("market://") ||
                                    targetUrl.startsWith("snssdk") ||
                                    targetUrl.startsWith("viber:") ||
                                    targetUrl.startsWith("whatsapp:")
                                ) {
                                    return true
                                }

                                // Sub-frames inside the page should be allowed to load player dependencies
                                if (request?.isForMainFrame == false) {
                                    return false
                                }

                                val embedHost = try { Uri.parse(embedUrl).host.orEmpty() } catch (_: Exception) { "" }
                                val reqHost = try { Uri.parse(targetUrl).host.orEmpty() } catch (_: Exception) { "" }

                                val isSameHost = embedHost.isNotBlank() && (reqHost.equals(embedHost, ignoreCase = true) || reqHost.endsWith(".$embedHost", ignoreCase = true))
                                val isMedia = targetUrl.contains(".m3u8", ignoreCase = true) ||
                                        targetUrl.contains(".mp4", ignoreCase = true) ||
                                        targetUrl.startsWith("blob:")

                                if (isSameHost || isMedia) {
                                    return false
                                }

                                // For Vidnest internal routing / animepahe subdomains
                                if (embedHost.contains("vidnest", ignoreCase = true) &&
                                    (reqHost.contains("vidnest", ignoreCase = true) || reqHost.contains("animepahe", ignoreCase = true))
                                ) {
                                    return false
                                }

                                // For AnimeSalt routing, CDN servers, and multi-lang player links
                                if ((embedHost.contains("animesalt", ignoreCase = true) || embedHost.contains("as-cdn", ignoreCase = true)) &&
                                    (reqHost.contains("animesalt", ignoreCase = true) ||
                                     reqHost.contains("as-cdn", ignoreCase = true) ||
                                     reqHost.contains("short.icu", ignoreCase = true))
                                ) {
                                    return false
                                }

                                // For YouTube embeds
                                if (embedHost.contains("youtube", ignoreCase = true) &&
                                    (reqHost.contains("youtube", ignoreCase = true) ||
                                     reqHost.contains("googlevideo", ignoreCase = true) ||
                                     reqHost.contains("ytimg", ignoreCase = true) ||
                                     reqHost.contains("google.com", ignoreCase = true))
                                ) {
                                    return false
                                }

                                // Intercept and block ad redirects/app market intents
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val msgListenerJs = """
                                    (function() {
                                        if (window.__aniverseListenerAdded) return;
                                        window.__aniverseListenerAdded = true;
                                        window.addEventListener('message', function(e) {
                                            var d = e.data;
                                            if (typeof d === 'string') {
                                                try { d = JSON.parse(d); } catch(err) {}
                                            }
                                            if (d === 'ended' || d === 'next' || d === 'next_episode' || (d && (d.event === 'ended' || d.type === 'ended' || d.event === 'next' || d.event === 'next_episode'))) {
                                                if (window.AndroidPlayer && window.AndroidPlayer.onNextEpisode) {
                                                    window.AndroidPlayer.onNextEpisode();
                                                }
                                            }
                                        });
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(msgListenerJs, null)

                                if (url?.contains("animesalt", ignoreCase = true) == true) {
                                    val js = """
                                        (function() {
                                            var css = 'header, footer, .sidebar, .comments-area, #comments, .breadcrumb, .widget, .as-header, .top-bar, .entry-header, .site-header, .site-footer { display: none !important; } ' +
                                                      'body, html, .site-content, .content-area, .video-player, .torofilm-video { width: 100% !important; height: 100% !important; margin: 0 !important; padding: 0 !important; background: #000 !important; }';
                                            var style = document.createElement('style');
                                            style.type = 'text/css';
                                            style.appendChild(document.createTextNode(css));
                                            document.head.appendChild(style);
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                }
                                val autoPlayRecoveryJs = """
                                    (function() {
                                        function triggerPlay() {
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                var v = vids[i];
                                                v.setAttribute('playsinline', 'true');
                                                v.setAttribute('webkit-playsinline', 'true');
                                                if (v.paused && v.readyState >= 1) {
                                                    var p = v.play();
                                                    if (p && p.catch) p.catch(function(){});
                                                }
                                            }
                                        }
                                        triggerPlay();
                                        setTimeout(triggerPlay, 1000);
                                        setTimeout(triggerPlay, 2500);
                                        setTimeout(triggerPlay, 5000);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(autoPlayRecoveryJs, null)
                            }

                            // Capacitor HTTP style CORS bypass: intercept OPTIONS preflight & cross-origin stream/API requests
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val req = request ?: return null
                                val targetUrl = req.url?.toString().orEmpty()
                                val method = req.method?.uppercase() ?: "GET"

                                // 1. Handle OPTIONS preflight immediately with 204 No Content & CORS bypass headers
                                if (method == "OPTIONS") {
                                    val headers = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Access-Control-Allow-Methods" to "GET, POST, PUT, DELETE, OPTIONS, HEAD",
                                        "Access-Control-Allow-Headers" to "*",
                                        "Access-Control-Allow-Credentials" to "true",
                                        "Access-Control-Max-Age" to "86400"
                                    )
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        204,
                                        "No Content",
                                        headers,
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }

                                // 2. Intercept video streams and API fetches to bypass CORS restrictions
                                val lowUrl = targetUrl.lowercase()
                                val isStreamOrApi = lowUrl.contains(".m3u8") || lowUrl.contains(".ts") ||
                                        lowUrl.contains(".m4s") || lowUrl.contains("/hls") ||
                                        lowUrl.contains("/api/") || lowUrl.contains("playlist")
                                if (isStreamOrApi && method != "POST") {
                                    try {
                                        val urlObj = URL(targetUrl)
                                        var conn = urlObj.openConnection() as HttpURLConnection
                                        conn.requestMethod = method
                                        conn.connectTimeout = 12000
                                        conn.readTimeout = 20000
                                        conn.instanceFollowRedirects = true

                                        req.requestHeaders?.forEach { (k, v) ->
                                            if (!k.equals("Host", ignoreCase = true) && !k.equals("Accept-Encoding", ignoreCase = true)) {
                                                conn.setRequestProperty(k, v)
                                            }
                                        }

                                        val embedHost = try { Uri.parse(embedUrl).host.orEmpty() } catch (_: Exception) { "" }
                                        val defaultReferer = when {
                                            embedHost.contains("megacloud") || embedHost.contains("megaplay") || embedHost.contains("vidstream") || embedHost.contains("rapid-cloud") -> "https://megaplay.buzz/"
                                            embedHost.contains("vidnest") -> "https://vidnest.fun/"
                                            embedHost.isNotBlank() -> "https://$embedHost/"
                                            else -> "https://megaplay.buzz/"
                                        }
                                        if (conn.getRequestProperty("Referer").isNullOrBlank()) {
                                            conn.setRequestProperty("Referer", defaultReferer)
                                        }
                                        if (conn.getRequestProperty("User-Agent").isNullOrBlank()) {
                                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                        }

                                        var code = conn.responseCode
                                        if ((code == 403 || code == 404 || code == 401 || code == 503) &&
                                            (lowUrl.contains("megaplay") || lowUrl.contains("vidnest") ||
                                             lowUrl.contains("megacloud") || lowUrl.contains("vidstream") ||
                                             defaultReferer.isNotBlank())
                                        ) {
                                            conn.disconnect()
                                            val host = try { URL(targetUrl).host } catch (_: Exception) { "" }
                                            val retryStrategies = listOf(
                                                "https://megaplay.buzz/" to "https://megaplay.buzz",
                                                "https://vidnest.fun/" to "https://vidnest.fun",
                                                "https://$host/" to "https://$host",
                                                null to null
                                            )
                                            for ((ref, orig) in retryStrategies) {
                                                try {
                                                    val retryConn = URL(targetUrl).openConnection() as HttpURLConnection
                                                    retryConn.requestMethod = method
                                                    retryConn.connectTimeout = 10000
                                                    retryConn.readTimeout = 15000
                                                    retryConn.instanceFollowRedirects = true
                                                    req.requestHeaders?.forEach { (k, v) ->
                                                        if (!k.equals("Host", ignoreCase = true) && !k.equals("Accept-Encoding", ignoreCase = true)) {
                                                            retryConn.setRequestProperty(k, v)
                                                        }
                                                    }
                                                    if (ref != null) retryConn.setRequestProperty("Referer", ref)
                                                    if (orig != null) retryConn.setRequestProperty("Origin", orig)
                                                    retryConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                                    val rCode = retryConn.responseCode
                                                    if (rCode in 200..299 || rCode == 206) {
                                                        conn = retryConn
                                                        code = rCode
                                                        break
                                                    }
                                                    retryConn.disconnect()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        val statusMsg = conn.responseMessage ?: "OK"
                                        val mimeType = conn.contentType?.split(";")?.firstOrNull()?.trim()
                                            ?: when {
                                                lowUrl.contains(".m3u8") -> "application/vnd.apple.mpegurl"
                                                lowUrl.contains(".ts") -> "video/mp2t"
                                                lowUrl.contains(".mp4") -> "video/mp4"
                                                lowUrl.contains(".m4s") -> "video/iso.segment"
                                                lowUrl.contains(".vtt") -> "text/vtt"
                                                else -> "application/octet-stream"
                                            }

                                        // For binary media (like .ts, .mp4, .m4s), WebResourceResponse encoding MUST BE NULL!
                                        // Specifying "UTF-8" forces Android WebView to decode binary video bytes as text, corrupting video packets and causing infinite loading spinner!
                                        val isBinary = lowUrl.contains(".ts") || lowUrl.contains(".mp4") || lowUrl.contains(".m4s") ||
                                                lowUrl.contains(".key") || mimeType.startsWith("video/") || mimeType.startsWith("audio/") ||
                                                mimeType == "application/octet-stream"
                                        val encoding = if (isBinary) null else (conn.contentEncoding ?: "UTF-8")

                                        val resHeaders = mutableMapOf<String, String>()
                                        conn.headerFields.forEach { (k, vList) ->
                                            if (!k.isNullOrBlank() && vList.isNotEmpty()) {
                                                if (!k.equals("Transfer-Encoding", ignoreCase = true) &&
                                                    !k.equals("Content-Encoding", ignoreCase = true)
                                                ) {
                                                    resHeaders[k] = vList.first()
                                                }
                                            }
                                        }
                                        // Capacitor HTTP Universal CORS Permissions
                                        resHeaders["Access-Control-Allow-Origin"] = "*"
                                        resHeaders["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS, HEAD"
                                        resHeaders["Access-Control-Allow-Headers"] = "*"
                                        resHeaders["Access-Control-Allow-Credentials"] = "true"

                                        val rawStream = if (code in 200..299 || code == 206) conn.inputStream else (conn.errorStream ?: ByteArrayInputStream(ByteArray(0)))
                                        val isStream = java.io.BufferedInputStream(rawStream, 65536)
                                        return WebResourceResponse(mimeType, encoding, code, statusMsg, resHeaders, isStream)
                                    } catch (_: Exception) {
                                        // Fallback to standard webview pipeline
                                    }
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                // Silently swallow sub-resource tracker/ad errors
                            }
                        }

                        loadEmbedUrl(this, embedUrl)
                    }
                },
                update = { wv ->
                    if (wv.tag != embedUrl) {
                        loadEmbedUrl(wv, embedUrl)
                    }
                },
                onRelease = { wv ->
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.destroy()
                }
            )
        }

        // Fullscreen Toggle Overlay Button
        if (onToggleFullscreen != null && customView == null) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0x88000000))
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
