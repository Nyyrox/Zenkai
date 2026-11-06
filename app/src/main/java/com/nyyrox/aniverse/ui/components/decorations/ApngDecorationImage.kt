package com.nyyrox.aniverse.ui.components.decorations

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.StreamLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Helper to ensure decoration URLs are fully valid (spaces encoded, cdn mapped).
 */
fun sanitizeDecorationUrl(url: String): String {
    var clean = url.trim().replace(" ", "%20")
    if (clean.startsWith("https://raw.githubusercontent.com/Nyyrox/SillyTavern-AvatarDecorations-CSS/main/dc-decorations/")) {
        clean = clean.replace(
            "https://raw.githubusercontent.com/Nyyrox/SillyTavern-AvatarDecorations-CSS/main/dc-decorations/",
            "https://cdn.jsdelivr.net/gh/Nyyrox/SillyTavern-AvatarDecorations-CSS@c63faa39f5c863da5bfaa9a642720b50283de090/dc-decorations/"
        )
    }
    return clean
}

/**
 * High-performance animated APNG viewer for Discord avatar decorations.
 * APNG4Android decodes multi-frame APNG chunks (acTL, fcTL, fdAT) and renders
 * at full 60fps hardware-accelerated framerate in an Android ImageView.
 *
 * Note: We intentionally avoid calling APNGDrawable.isRunning to sidestep
 * Animatable2Compat classpath visibility issues on certain build configurations.
 */
object ApngMemoryCache {
    private val cache = ConcurrentHashMap<String, ByteArray>(128)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun get(url: String): ByteArray? = cache[url]

    private fun fetch(url: String): ByteArray? {
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body.bytes()
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    suspend fun loadBytes(rawUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val safeUrl = sanitizeDecorationUrl(rawUrl)
        cache[safeUrl]?.let { return@withContext it }
        cache[rawUrl]?.let { return@withContext it }

        var bytes = fetch(safeUrl)
        if (bytes == null && safeUrl.contains("jsdelivr.net")) {
            val rawFallback = safeUrl.replace(
                "https://cdn.jsdelivr.net/gh/Nyyrox/SillyTavern-AvatarDecorations-CSS@c63faa39f5c863da5bfaa9a642720b50283de090/dc-decorations/",
                "https://raw.githubusercontent.com/Nyyrox/SillyTavern-AvatarDecorations-CSS/main/dc-decorations/"
            )
            bytes = fetch(rawFallback)
        }
        if (bytes != null) {
            cache[safeUrl] = bytes
            cache[rawUrl] = bytes
        }
        bytes
    }
}

@Composable
fun ApngDecorationImage(
    url: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val safeUrl = remember(url) { sanitizeDecorationUrl(url) }
    var apngDrawable by remember(safeUrl) { mutableStateOf<APNGDrawable?>(null) }
    var loadFailed by remember(safeUrl) { mutableStateOf(false) }

    LaunchedEffect(safeUrl) {
        val bytes = ApngMemoryCache.get(safeUrl) ?: ApngMemoryCache.loadBytes(safeUrl)
        if (bytes != null) {
            try {
                val loader = object : StreamLoader() {
                    override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
                }
                val drawable = APNGDrawable(loader).apply {
                    setLoopLimit(0) // Infinite animation loop
                }
                withContext(Dispatchers.Main) {
                    apngDrawable = drawable
                    drawable.start()
                }
            } catch (_: Throwable) {
                loadFailed = true
            }
        } else {
            loadFailed = true
        }
    }

    DisposableEffect(apngDrawable) {
        val d = apngDrawable
        onDispose {
            try { d?.stop() } catch (_: Throwable) {}
        }
    }

    val currentDrawable = apngDrawable
    if (currentDrawable != null && !loadFailed) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                ImageView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(currentDrawable)
                    try { currentDrawable.start() } catch (_: Throwable) {}
                }
            },
            update = { imageView ->
                if (imageView.drawable !== currentDrawable) {
                    imageView.setImageDrawable(currentDrawable)
                    try { currentDrawable.start() } catch (_: Throwable) {}
                }
            }
        )
    } else {
        // Fallback static image while loading or if APNG fails
        AsyncImage(
            model = safeUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
