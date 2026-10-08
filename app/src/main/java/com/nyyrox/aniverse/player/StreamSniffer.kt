package com.nyyrox.aniverse.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.nyyrox.aniverse.data.subtitles.SubtitleRepository
import com.nyyrox.aniverse.data.subtitles.SubtitleTrack
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

data class SniffedStream(
    val streamUrl: String,
    val isHls: Boolean,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

object StreamSniffer {
    const val TARGET_UA =
        "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"

    private val AD_DOMAINS = listOf(
        "doubleclick", "googlesyndication", "google-analytics", "moatads",
        "scorecardresearch", "popads", "popcash", "clickadu", "propellerads",
        "adsterra", "monetag", "adnxs", "criteo", "trafficjunky", "exoclick"
    )

    private fun isAdUrl(url: String): Boolean {
        val low = url.lowercase()
        return AD_DOMAINS.any { low.contains(it) }
    }

    private fun isMediaUrl(url: String): Boolean {
        val low = url.lowercase()
        if (isAdUrl(low)) return false

        val cleanPath = try {
            val uri = Uri.parse(low)
            uri.path ?: low.split('?').first().split('#').first()
        } catch (_: Exception) {
            low.split('?').first().split('#').first()
        }

        // Never match non-media web assets
        val isNonMedia = cleanPath.endsWith(".js") || cleanPath.endsWith(".css") ||
                cleanPath.endsWith(".png") || cleanPath.endsWith(".jpg") ||
                cleanPath.endsWith(".jpeg") || cleanPath.endsWith(".gif") ||
                cleanPath.endsWith(".webp") || cleanPath.endsWith(".svg") ||
                cleanPath.endsWith(".html") || cleanPath.endsWith(".htm") ||
                cleanPath.endsWith(".json") || cleanPath.endsWith(".woff") ||
                cleanPath.endsWith(".woff2") || cleanPath.endsWith(".ttf")
        if (isNonMedia) return false

        // Check if path or full url (including query params for Vidnest/Megaplay) contains media format
        val hasHls = (cleanPath.endsWith(".m3u8") || low.contains(".m3u8")) && !cleanPath.endsWith(".js")
        val hasMp4 = (cleanPath.endsWith(".mp4") || low.contains(".mp4")) && !cleanPath.endsWith(".js")
        val hasMpd = (cleanPath.endsWith(".mpd") || low.contains(".mpd")) && !cleanPath.endsWith(".js")
        val hasProxyStream = (low.contains("/hls-proxy") || low.contains("/m3u8-proxy") || low.contains("/ts-proxy") || 
                (low.contains("/proxy?url=") && (low.contains(".m3u8") || low.contains("hls")))) && !cleanPath.endsWith(".js")

        return hasHls || hasMp4 || hasMpd || hasProxyStream
    }

    private fun isSubtitleUrl(url: String): Boolean {
        val low = url.lowercase()
        return (low.contains(".vtt") || low.contains(".srt") || low.contains("subtitles") || low.contains("caption")) && !isAdUrl(low)
    }

    /**
     * Attempts to sniff a direct media stream URL (.m3u8 or .mp4) and any subtitle/caption
     * URLs (.vtt or .srt) from an embed URL using a headless WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun sniff(
        context: Context,
        embedUrl: String,
        timeoutMs: Long = 15000L
    ): SniffedStream? {
        if (embedUrl.isBlank() || embedUrl == "about:blank") return null

        // If the URL is already a direct stream, return immediately
        if (embedUrl.contains(".m3u8", ignoreCase = true) || embedUrl.contains(".mp4", ignoreCase = true)) {
            val isHls = embedUrl.contains(".m3u8", ignoreCase = true)
            return SniffedStream(embedUrl, isHls)
        }

        return suspendCancellableCoroutine { continuation ->
            val mainHandler = Handler(Looper.getMainLooper())
            val resolved = AtomicBoolean(false)
            val sniffedSubs = CopyOnWriteArrayList<SubtitleTrack>()
            var webView: WebView? = null

            fun cleanup() {
                mainHandler.post {
                    try {
                        webView?.stopLoading()
                        webView?.webViewClient = object : WebViewClient() {}
                        webView?.destroy()
                        webView = null
                    } catch (_: Exception) {}
                }
            }

            val timeoutRunnable = Runnable {
                if (resolved.compareAndSet(false, true)) {
                    cleanup()
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }

            mainHandler.post {
                try {
                    val wv = WebView(context)
                    webView = wv

                    wv.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = TARGET_UA
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

                    wv.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString().orEmpty()

                            // Intercept caption / subtitle URLs (.vtt or .srt)
                            if (isSubtitleUrl(reqUrl)) {
                                val low = reqUrl.lowercase()
                                val lang = when {
                                    low.contains("eng") || low.contains("en-") || low.contains("english") -> "English"
                                    low.contains("spa") || low.contains("es-") || low.contains("spanish") -> "Spanish"
                                    low.contains("fre") || low.contains("fr-") || low.contains("french") -> "French"
                                    low.contains("ger") || low.contains("de-") || low.contains("german") -> "German"
                                    low.contains("ara") || low.contains("ar-") || low.contains("arabic") -> "Arabic"
                                    low.contains("hin") || low.contains("hi-") || low.contains("hindi") -> "Hindi"
                                    low.contains("jpn") || low.contains("ja-") || low.contains("japanese") -> "Japanese"
                                    else -> "Caption ${sniffedSubs.size + 1}"
                                }
                                if (sniffedSubs.none { it.url == reqUrl }) {
                                    sniffedSubs.add(
                                        SubtitleTrack(
                                            id = "sniffed_${sniffedSubs.size}",
                                            label = lang,
                                            language = lang,
                                            url = reqUrl,
                                            isDefault = lang == "English"
                                        )
                                    )
                                }
                            }

                            // Intercept direct media streams
                            if (isMediaUrl(reqUrl)) {
                                if (resolved.compareAndSet(false, true)) {
                                    mainHandler.removeCallbacks(timeoutRunnable)

                                    val isHls = reqUrl.contains("m3u8", ignoreCase = true)
                                    val headers = mutableMapOf<String, String>()

                                    // Extract useful request headers (case-insensitive for HTTP/2 and modern webviews)
                                    request?.requestHeaders?.let { h ->
                                        val ref = h.entries.firstOrNull { it.key.equals("Referer", ignoreCase = true) }?.value
                                        val orig = h.entries.firstOrNull { it.key.equals("Origin", ignoreCase = true) }?.value
                                        val ua = h.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
                                        if (!ref.isNullOrBlank()) headers["Referer"] = ref
                                        if (!orig.isNullOrBlank()) headers["Origin"] = orig
                                        if (!ua.isNullOrBlank()) headers["User-Agent"] = ua
                                    }

                                    if (!headers.containsKey("Referer")) {
                                        try {
                                            val host = Uri.parse(embedUrl).host
                                            headers["Referer"] = "https://$host/"
                                            headers["Origin"] = "https://$host"
                                        } catch (_: Exception) {
                                            headers["Referer"] = embedUrl
                                        }
                                    }
                                    if (!headers.containsKey("Origin")) {
                                        try {
                                            val host = Uri.parse(embedUrl).host
                                            headers["Origin"] = "https://$host"
                                        } catch (_: Exception) {}
                                    }

                                    // Attach session cookies from WebView
                                    try {
                                        val cookies = CookieManager.getInstance().getCookie(reqUrl)
                                            ?: CookieManager.getInstance().getCookie(embedUrl)
                                        if (!cookies.isNullOrBlank()) {
                                            headers["Cookie"] = cookies
                                        }
                                    } catch (_: Exception) {}

                                    headers["User-Agent"] = TARGET_UA

                                    // Inspect DOM for any <track> tags before returning
                                    mainHandler.post {
                                        try {
                                            wv.evaluateJavascript(
                                                "(function() { return JSON.stringify(Array.from(document.querySelectorAll('track')).map(function(t) { return { src: t.src, label: t.label, srclang: t.srclang }; })); })();"
                                            ) { jsResult ->
                                                if (!jsResult.isNullOrBlank() && jsResult != "null" && jsResult != "\"[]\"") {
                                                    try {
                                                        val cleanJson = if (jsResult.startsWith("\"") && jsResult.endsWith("\"")) {
                                                            org.json.JSONTokener(jsResult).nextValue().toString()
                                                        } else jsResult
                                                        val arr = JSONArray(cleanJson)
                                                        for (i in 0 until arr.length()) {
                                                            val obj = arr.getJSONObject(i)
                                                            val src = obj.optString("src")
                                                            if (src.isNotBlank() && sniffedSubs.none { it.url == src }) {
                                                                val label = obj.optString("label").takeIf { it.isNotBlank() }
                                                                    ?: SubtitleRepository.normalizeLanguage(obj.optString("srclang", "und"))
                                                                sniffedSubs.add(
                                                                    SubtitleTrack(
                                                                        id = "dom_track_$i",
                                                                        label = label,
                                                                        language = label,
                                                                        url = src,
                                                                        isDefault = label.equals("English", ignoreCase = true)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                                cleanup()
                                                if (continuation.isActive) {
                                                    continuation.resume(SniffedStream(reqUrl, isHls, headers, sniffedSubs.toList()))
                                                }
                                            }
                                        } catch (_: Exception) {
                                            cleanup()
                                            if (continuation.isActive) {
                                                continuation.resume(SniffedStream(reqUrl, isHls, headers, sniffedSubs.toList()))
                                            }
                                        }
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            try {
                                view?.evaluateJavascript("""
                                    (function() {
                                        try {
                                            delete window.SandboxDetector;
                                            window.SandboxDetector = { run: function() {}, detect: function() { return Promise.resolve(false); } };
                                        } catch(e) {}

                                        function triggerAutoPlay() {
                                            try {
                                                if (window.jwplayer) {
                                                    try { jwplayer().play(); } catch(e) {}
                                                    try { if (typeof jwplayer === 'function') jwplayer(0).play(); } catch(e) {}
                                                }
                                                if (window.videojs) {
                                                    try {
                                                        const players = videojs.getPlayers ? videojs.getPlayers() : {};
                                                        Object.keys(players).forEach(k => { try { players[k].play(); } catch(e) {} });
                                                    } catch(e) {}
                                                }
                                                if (window.player && typeof window.player.play === 'function') {
                                                    try { window.player.play(); } catch(e) {}
                                                }
                                                if (window.art && typeof window.art.play === 'function') {
                                                    try { window.art.play(); } catch(e) {}
                                                }
                                                if (window.dp && typeof window.dp.play === 'function') {
                                                    try { window.dp.play(); } catch(e) {}
                                                }

                                                const videos = document.querySelectorAll('video');
                                                videos.forEach(v => {
                                                    try {
                                                        v.muted = true;
                                                        v.autoplay = true;
                                                        const p = v.play();
                                                        if (p && p.catch) p.catch(() => {});
                                                    } catch(e) {}
                                                });

                                                const selectors = [
                                                    '.jw-display-icon-container', '.jw-icon-play', '.jw-display-icon-display', '.jw-preview',
                                                    '.vjs-big-play-button', '.vjs-play-control', '#playButton', '.btn-play',
                                                    'button[data-plyr="play"]', '.play-button', '[aria-label="Play"]', '.play-btn',
                                                    '#megaplay-player', '.fix-area', '.load-circle', '.mg3-player',
                                                    '#player', '.player', '#player-holder', '.play-wrapper', '.cover', '.poster',
                                                    '#overlay', '.overlay', '.play-icon', '.fa-play', '.bi-play-fill',
                                                    '.recloud-player', '.animo-player', '.video-js', '.plyr__control--overlaid',
                                                    '.loading-overlay', '.play-backdrop', '#play-btn', '#start-video'
                                                ];
                                                selectors.forEach(s => {
                                                    document.querySelectorAll(s).forEach(el => {
                                                        try {
                                                            el.click();
                                                            ['mousedown', 'mouseup', 'click', 'touchstart', 'touchend'].forEach(evt => {
                                                                el.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true, view: window }));
                                                            });
                                                        } catch(e) {}
                                                    });
                                                });

                                                const cx = (window.innerWidth || 300) / 2;
                                                const cy = (window.innerHeight || 300) / 2;
                                                const centerEl = document.elementFromPoint(cx, cy);
                                                if (centerEl && centerEl !== document.body && centerEl !== document.documentElement) {
                                                    try { centerEl.click(); } catch(e) {}
                                                }
                                            } catch(e) {}
                                        }

                                        triggerAutoPlay();
                                        if (!window.__aniverseSnifferInterval) {
                                            window.__aniverseSnifferInterval = setInterval(triggerAutoPlay, 250);
                                        }
                                    })();
                                """.trimIndent(), null)
                            } catch (_: Exception) {}
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!resolved.get()) {
                                try {
                                    wv.evaluateJavascript("""
                                        (function() {
                                            try {
                                                const v = document.querySelector('video');
                                                if (v) { v.muted = true; v.play().catch(()=>{}); }
                                                const sels = [
                                                    '.jw-display-icon-container', '.jw-icon-play', '.jw-display-icon-display',
                                                    '.vjs-big-play-button', '.vjs-play-control', '#playButton', '.btn-play',
                                                    'button[data-plyr="play"]', '.play-button', '[aria-label="Play"]', '.play-btn',
                                                    '#megaplay-player', '.fix-area', '.load-circle', '.mg3-player',
                                                    '#player', '.player', '#player-holder', '.play-wrapper', '.cover', '.poster',
                                                    '#overlay', '.overlay', '.recloud-player', '.animo-player'
                                                ];
                                                sels.forEach(s => {
                                                    document.querySelectorAll(s).forEach(b => {
                                                        try { b.click(); } catch(e) {}
                                                    });
                                                });
                                                const cx = (window.innerWidth || 300) / 2;
                                                const cy = (window.innerHeight || 300) / 2;
                                                const centerEl = document.elementFromPoint(cx, cy);
                                                if (centerEl) { try { centerEl.click(); } catch(e) {} }
                                            } catch(_) {}
                                        })();
                                    """.trimIndent(), null)
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    mainHandler.postDelayed(timeoutRunnable, timeoutMs)

                    // Autoplay helpers: attempt to trigger playback at rapid intervals
                    fun triggerPlay() {
                        if (!resolved.get()) {
                            try {
                                wv.evaluateJavascript("""
                                    (function() {
                                        try {
                                            if (window.jwplayer) {
                                                try { jwplayer().play(); } catch(e) {}
                                            }
                                            const v = document.querySelector('video');
                                            if (v) { v.muted = true; v.play().catch(()=>{}); }
                                            const sels = [
                                                '.jw-display-icon-container', '.jw-icon-play', '.vjs-big-play-button',
                                                '#playButton', '.btn-play', 'button[data-plyr="play"]', '.play-button',
                                                '[aria-label="Play"]', '.play-btn', '#megaplay-player', '.fix-area',
                                                '#player', '.player', '.recloud-player', '.animo-player'
                                            ];
                                            sels.forEach(s => {
                                                document.querySelectorAll(s).forEach(b => {
                                                    try { b.click(); } catch(e) {}
                                                });
                                            });
                                            const cx = (window.innerWidth || 300) / 2;
                                            const cy = (window.innerHeight || 300) / 2;
                                            const centerEl = document.elementFromPoint(cx, cy);
                                            if (centerEl) { try { centerEl.click(); } catch(e) {} }
                                        } catch(_) {}
                                    })();
                                """.trimIndent(), null)
                            } catch (_: Exception) {}
                        }
                    }

                    mainHandler.postDelayed({ triggerPlay() }, 200L)
                    mainHandler.postDelayed({ triggerPlay() }, 500L)
                    mainHandler.postDelayed({ triggerPlay() }, 1000L)
                    mainHandler.postDelayed({ triggerPlay() }, 2000L)
                    mainHandler.postDelayed({ triggerPlay() }, 3500L)
                    mainHandler.postDelayed({ triggerPlay() }, 6000L)

                    val loadReferer = when {
                        embedUrl.contains("megaplay") -> "https://megaplay.buzz/"
                        embedUrl.contains("vidnest") -> "https://vidnest.fun/"
                        embedUrl.contains("tryembed") -> "https://tryembed.us.cc/"
                        embedUrl.contains("zokoanime") -> "https://zokoanime.video/"
                        embedUrl.contains("4animo") || embedUrl.contains("recloud") -> "https://cdn.4animo.xyz/"
                        embedUrl.contains("yumezone") -> "https://yumezone.qzz.io/"
                        else -> embedUrl
                    }
                    val loadHeaders = mapOf(
                        "User-Agent" to TARGET_UA,
                        "Referer" to loadReferer
                    )
                    wv.loadUrl(embedUrl, loadHeaders)

                } catch (e: Exception) {
                    if (resolved.compareAndSet(false, true)) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        cleanup()
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            continuation.invokeOnCancellation {
                if (resolved.compareAndSet(false, true)) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    cleanup()
                }
            }
        }
    }
}
