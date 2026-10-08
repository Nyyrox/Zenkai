package com.nyyrox.aniverse.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class AvatarItem(
    val id: Int,
    val tags: List<String>,
    val url: String
)

data class BannerItem(
    val id: Int,
    val category: String,
    val url: String
)

object AvatarBannerRepository {
    private val REMOTE_AVATAR_URLS = listOf(
        "https://zenkai-stream-proxy.ammhfoo.workers.dev/avatars.json",
        "https://raw.githubusercontent.com/Nyyrox/zenkai-stream-proxy/main/avatars.json",
        "https://raw.githubusercontent.com/Nyyrox/avatar-banner-/main/avatars.json"
    )
    private val REMOTE_BANNER_URLS = listOf(
        "https://zenkai-stream-proxy.ammhfoo.workers.dev/banners.json",
        "https://raw.githubusercontent.com/Nyyrox/zenkai-stream-proxy/main/banners.json",
        "https://raw.githubusercontent.com/Nyyrox/avatar-banner-/main/banners.json"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _avatarsFlow = MutableStateFlow<List<AvatarItem>>(emptyList())
    val avatarsFlow: StateFlow<List<AvatarItem>> = _avatarsFlow.asStateFlow()

    private val _bannersFlow = MutableStateFlow<List<BannerItem>>(emptyList())
    val bannersFlow: StateFlow<List<BannerItem>> = _bannersFlow.asStateFlow()

    private var cachedAvatars: List<AvatarItem> = emptyList()
    private var cachedBanners: List<BannerItem> = emptyList()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        loadLocalAssets(context)
        isInitialized = true

        CoroutineScope(Dispatchers.IO).launch {
            syncRemote()
        }
    }

    @Synchronized
    private fun loadLocalAssets(context: Context) {
        try {
            if (cachedAvatars.isEmpty()) {
                context.assets.open("avatars.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        cachedAvatars = parseAvatarsJson(reader.readText())
                        _avatarsFlow.value = cachedAvatars
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (cachedBanners.isEmpty()) {
                context.assets.open("banners.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        cachedBanners = parseBannersJson(reader.readText())
                        _bannersFlow.value = cachedBanners
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAvatars(context: Context): List<AvatarItem> {
        if (cachedAvatars.isEmpty()) {
            loadLocalAssets(context)
        }
        return cachedAvatars
    }

    fun getBanners(context: Context): List<BannerItem> {
        if (cachedBanners.isEmpty()) {
            loadLocalAssets(context)
        }
        return cachedBanners
    }

    fun getAvatarCategories(context: Context): List<String> {
        val avatars = getAvatars(context)
        val tags = mutableSetOf<String>()
        avatars.forEach { item ->
            tags.addAll(item.tags)
        }
        return listOf("All") + tags.filter { it.isNotBlank() }.sorted()
    }

    fun getBannerCategories(context: Context): List<String> {
        val banners = getBanners(context)
        val categories = mutableSetOf<String>()
        banners.forEach { item ->
            if (item.category.isNotBlank()) {
                categories.add(item.category)
            }
        }
        return listOf("All") + categories.sorted()
    }

    private fun parseAvatarsJson(jsonStr: String): List<AvatarItem> {
        val list = mutableListOf<AvatarItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optInt("id", i + 1)
                val url = obj.optString("url")
                val tagsArr = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        val tag = tagsArr.optString(t)
                        if (tag.isNotBlank()) tags.add(tag)
                    }
                }
                if (url.isNotBlank()) {
                    list.add(AvatarItem(id = id, tags = tags, url = url))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseBannersJson(jsonStr: String): List<BannerItem> {
        val list = mutableListOf<BannerItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optInt("id", i + 1)
                val category = obj.optString("category", "General")
                val url = obj.optString("url")
                if (url.isNotBlank()) {
                    list.add(BannerItem(id = id, category = category, url = url))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private suspend fun syncRemote() = withContext(Dispatchers.IO) {
        for (url in REMOTE_AVATAR_URLS) {
            try {
                val avatarReq = Request.Builder().url(url).build()
                val success = client.newCall(avatarReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body.string()
                        val parsed = parseAvatarsJson(body)
                        if (parsed.isNotEmpty()) {
                            cachedAvatars = parsed
                            _avatarsFlow.value = parsed
                            true
                        } else false
                    } else false
                }
                if (success) break
            } catch (_: Exception) {}
        }

        for (url in REMOTE_BANNER_URLS) {
            try {
                val bannerReq = Request.Builder().url(url).build()
                val success = client.newCall(bannerReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body.string()
                        val parsed = parseBannersJson(body)
                        if (parsed.isNotEmpty()) {
                            cachedBanners = parsed
                            _bannersFlow.value = parsed
                            true
                        } else false
                    } else false
                }
                if (success) break
            } catch (_: Exception) {}
        }
    }
}
