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
 * High-performance animated APNG viewer for Discord avatar decorations.
 * APNG4Android decodes multi-frame APNG chunks (acTL, fcTL, fdAT) and renders
 * at full 60fps hardware-accelerated framerate in an Android ImageView.
 *
 * Note: We intentionally avoid calling APNGDrawable.isRunning to sidestep
 * Animatable2Compat classpath visibility issues on certain build configurations.
 */
object ApngMemoryCache {
    private val cache = ConcurrentHashMap<String, ByteArray>(64)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun get(url: String): ByteArray? = cache[url]

    suspend fun loadBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body.bytes()
                    cache[url] = bytes
                    return@withContext bytes
                }
            }
        } catch (_: Throwable) {}
        null
    }
}

@Composable
fun ApngDecorationImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var apngDrawable by remember(url) { mutableStateOf<APNGDrawable?>(null) }
    var loadFailed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        val bytes = ApngMemoryCache.get(url) ?: ApngMemoryCache.loadBytes(url)
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
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(currentDrawable)
                    // start() already called in LaunchedEffect; safe to call again
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
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
