package com.nyyrox.aniverse.data.discord

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DiscordGuildInfo(
    val guildId: String,
    val name: String,
    val iconUrl: String?,
    val memberCount: Int,
    val presenceCount: Int,
    val channelName: String?,
    val channelId: String?
)

object DiscordRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getGuildInfo(inviteCode: String = DiscordConfig.INVITE_CODE): Result<DiscordGuildInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://discord.com/api/v10/invites/$inviteCode?with_counts=true"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val raw = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(raw)
                val guildObj = json.optJSONObject("guild")
                val guildId = guildObj?.optString("id") ?: DiscordConfig.GUILD_ID
                val name = guildObj?.optString("name") ?: "ZENKAI"
                val iconHash = guildObj?.optString("icon")
                val iconUrl = if (!iconHash.isNullOrBlank() && iconHash != "null") {
                    "https://cdn.discordapp.com/icons/$guildId/$iconHash.png?size=256"
                } else null

                val memberCount = json.optInt("approximate_member_count", 220)
                val presenceCount = json.optInt("approximate_presence_count", 40)

                val channelObj = json.optJSONObject("channel")
                val channelName = channelObj?.optString("name")
                val channelId = channelObj?.optString("id")

                Result.success(
                    DiscordGuildInfo(
                        guildId = guildId,
                        name = name,
                        iconUrl = iconUrl,
                        memberCount = memberCount,
                        presenceCount = presenceCount,
                        channelName = channelName,
                        channelId = channelId
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
