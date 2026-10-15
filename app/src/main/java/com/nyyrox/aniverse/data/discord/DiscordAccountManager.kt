package com.nyyrox.aniverse.data.discord

import android.content.Context
import com.nyyrox.aniverse.data.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DiscordAccount(
    val userId: String,
    val username: String,
    val globalName: String?,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String? = null,
    val userToken: String? = null, // Gateway user token for rich presence
    val isRpcEnabled: Boolean = true
)

object DiscordAccountManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _accountState = MutableStateFlow<DiscordAccount?>(null)
    val accountState = _accountState.asStateFlow()

    private var storageManager: StorageManager? = null

    fun init(context: Context) {
        val sm = StorageManager(context)
        storageManager = sm
        _accountState.value = sm.getDiscordAccount()
    }

    fun isRpcEnabled(): Boolean {
        return _accountState.value?.isRpcEnabled ?: true
    }

    suspend fun exchangeCodeForToken(
        code: String,
        clientId: String = DiscordConfig.CLIENT_ID,
        clientSecret: String = DiscordConfig.CLIENT_SECRET
    ): Result<DiscordAccount> = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", DiscordConfig.REDIRECT_URI)
                .build()

            val req = Request.Builder()
                .url(DiscordConfig.OAUTH_TOKEN_URL)
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val resp = client.newCall(req).execute()
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed token exchange ($resp): $raw"))
            }

            val tokenJson = JSONObject(raw)
            val accessToken = tokenJson.getString("access_token")
            val refreshToken = tokenJson.optString("refresh_token").takeIf { it.isNotBlank() }

            // Fetch @me profile
            val meReq = Request.Builder()
                .url(DiscordConfig.USER_ME_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val meResp = client.newCall(meReq).execute()
            val meRaw = meResp.body?.string().orEmpty()
            if (!meResp.isSuccessful) {
                return@withContext Result.failure(Exception("Failed user profile fetch ($meResp): $meRaw"))
            }

            val meJson = JSONObject(meRaw)
            val userId = meJson.getString("id")
            val username = meJson.getString("username")
            val globalName = meJson.optString("global_name").takeIf { it.isNotBlank() }
            val avatarHash = meJson.optString("avatar").takeIf { it.isNotBlank() }
            val avatarUrl = if (avatarHash != null) {
                "https://cdn.discordapp.com/avatars/$userId/$avatarHash.png?size=256"
            } else null

            val account = DiscordAccount(
                userId = userId,
                username = username,
                globalName = globalName,
                avatarUrl = avatarUrl,
                accessToken = accessToken,
                refreshToken = refreshToken,
                userToken = _accountState.value?.userToken,
                isRpcEnabled = true
            )

            storageManager?.saveDiscordAccount(account)
            _accountState.value = account
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfileWithToken(
        accessToken: String,
        refreshToken: String? = null
    ): Result<DiscordAccount> = withContext(Dispatchers.IO) {
        try {
            val meReq = Request.Builder()
                .url(DiscordConfig.USER_ME_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val meResp = client.newCall(meReq).execute()
            val meRaw = meResp.body?.string().orEmpty()
            if (meResp.isSuccessful) {
                val meJson = JSONObject(meRaw)
                val userId = meJson.getString("id")
                val username = meJson.getString("username")
                val globalName = meJson.optString("global_name").takeIf { it.isNotBlank() }
                val avatarHash = meJson.optString("avatar").takeIf { it.isNotBlank() }
                val avatarUrl = if (avatarHash != null) {
                    "https://cdn.discordapp.com/avatars/$userId/$avatarHash.png?size=256"
                } else null

                val account = DiscordAccount(
                    userId = userId,
                    username = username,
                    globalName = globalName,
                    avatarUrl = avatarUrl,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userToken = _accountState.value?.userToken,
                    isRpcEnabled = true
                )

                storageManager?.saveDiscordAccount(account)
                _accountState.value = account
                return@withContext Result.success(account)
            }

            // Fallback: If token was a Supabase JWT from OAuth redirect, fetch user from Supabase
            val supaReq = Request.Builder()
                .url("${com.nyyrox.aniverse.data.supabase.SupabaseManager.SUPABASE_URL}/auth/v1/user")
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("apikey", com.nyyrox.aniverse.data.supabase.SupabaseManager.ANON_KEY)
                .build()

            val supaResp = client.newCall(supaReq).execute()
            val supaRaw = supaResp.body?.string().orEmpty()
            if (supaResp.isSuccessful) {
                val supaJson = JSONObject(supaRaw)
                val meta = supaJson.optJSONObject("user_metadata")
                val userId = meta?.optString("provider_id")?.takeIf { it.isNotBlank() }
                    ?: supaJson.optString("id").takeIf { it.isNotBlank() }
                    ?: "discord_user"
                val username = meta?.optString("user_name")?.takeIf { it.isNotBlank() }
                    ?: meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                    ?: "Discord User"
                val globalName = meta?.optString("full_name")?.takeIf { it.isNotBlank() } ?: username
                val avatarUrl = meta?.optString("avatar_url")?.takeIf { it.isNotBlank() }

                val account = DiscordAccount(
                    userId = userId,
                    username = username,
                    globalName = globalName,
                    avatarUrl = avatarUrl,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userToken = _accountState.value?.userToken,
                    isRpcEnabled = true
                )

                storageManager?.saveDiscordAccount(account)
                _accountState.value = account
                return@withContext Result.success(account)
            }

            Result.failure(Exception("Failed user profile fetch ($meResp): $meRaw"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setRpcEnabled(enabled: Boolean) {
        val curr = _accountState.value ?: return
        val updated = curr.copy(isRpcEnabled = enabled)
        storageManager?.saveDiscordAccount(updated)
        _accountState.value = updated
        if (!enabled) {
            DiscordRpcService.clearPresence()
        }
    }

    fun setUserToken(userToken: String?) {
        val curr = _accountState.value
        val updated = if (curr != null) {
            curr.copy(userToken = userToken)
        } else if (!userToken.isNullOrBlank()) {
            DiscordAccount(
                userId = "custom",
                username = "Discord User",
                globalName = "Discord User",
                avatarUrl = null,
                accessToken = userToken,
                userToken = userToken,
                isRpcEnabled = true
            )
        } else null

        storageManager?.saveDiscordAccount(updated)
        _accountState.value = updated
    }

    fun logout() {
        storageManager?.saveDiscordAccount(null)
        _accountState.value = null
        DiscordRpcService.clearPresence()
    }

    suspend fun validateDiscordToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim().replace("\"", "").replace("Bot ", "").replace("Bearer ", "")
            val authHeader = if (cleanToken.startsWith("MTU") || cleanToken.contains(".GP")) {
                "Bot $cleanToken"
            } else {
                cleanToken
            }
            val req = Request.Builder()
                .url(DiscordConfig.USER_ME_URL)
                .addHeader("Authorization", authHeader)
                .build()
            val resp = client.newCall(req).execute()
            val raw = resp.body?.string().orEmpty()
            if (resp.isSuccessful) {
                val json = JSONObject(raw)
                val username = json.optString("username", "Discord User")
                Result.success(username)
            } else {
                Result.failure(Exception("Invalid token (HTTP ${resp.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
