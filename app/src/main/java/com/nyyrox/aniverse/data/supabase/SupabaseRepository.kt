package com.nyyrox.aniverse.data.supabase







import io.github.jan.supabase.auth.auth



import io.github.jan.supabase.auth.providers.Google



import io.github.jan.supabase.auth.providers.builtin.Email



import io.github.jan.supabase.postgrest.postgrest



import io.github.jan.supabase.postgrest.from



import io.github.jan.supabase.postgrest.query.Order



import kotlinx.coroutines.Dispatchers



import kotlinx.coroutines.launch



import kotlinx.coroutines.withContext



import kotlinx.serialization.SerialName



import kotlinx.serialization.Serializable



import kotlinx.serialization.json.buildJsonObject



import kotlinx.serialization.json.buildJsonArray



import kotlinx.serialization.json.add



import kotlinx.serialization.json.put



import okhttp3.MediaType.Companion.toMediaTypeOrNull



import okhttp3.OkHttpClient



import okhttp3.Request



import okhttp3.RequestBody.Companion.toRequestBody



import com.nyyrox.aniverse.data.Provider







@Serializable



data class CloudProfile(



    val id: String = "",



    @SerialName("display_name") val displayName: String? = null,



    val username: String? = null,



    val bio: String? = null,



    @SerialName("bio_font") val bioFont: String? = null,



    @SerialName("avatar_url") val avatarUrl: String? = null,



    @SerialName("banner_url") val bannerUrl: String? = null,



    @SerialName("favorite_anime") val favoriteAnime: String? = null,



    @SerialName("otaku_rank") val otakuRank: String? = "Genin",



    @SerialName("avatar_decoration") val avatarDecoration: String? = null,



    @SerialName("avatar_frame") val avatarFrame: String? = null,



    val frame: String? = null,



    @SerialName("text_effect") val textEffect: String? = null,



    @SerialName("profile_effect") val profileEffect: String? = null,



    @SerialName("banner_theme") val bannerTheme: String? = null,



    @SerialName("nameplate") val nameplate: String? = null,



    @SerialName("nameplate_emoji") val nameplateEmoji: String? = null,



    @SerialName("is_admin") val isAdmin: Boolean? = false,



    val role: String? = null,



    val roles: List<String>? = emptyList(),



    @SerialName("custom_tag") val customTag: String? = null,



    @SerialName("exp") val exp: Long? = 0L,



    @SerialName("level") val level: Int? = 1,



    @SerialName("streak_count") val streakCount: Int? = 1,



    @SerialName("longest_streak") val longestStreak: Int? = 1,



    @SerialName("last_active_date") val lastActiveDate: String? = null,



    @SerialName("banner_effect") val bannerEffect: String? = null,



    @SerialName("anilist_sync") val aniListSync: String? = null,



    @SerialName("mal_sync") val malSync: String? = null,



    @SerialName("time_spent_anime") val timeSpentAnime: Long? = 0L,



    @SerialName("time_spent_manga") val timeSpentManga: Long? = 0L,



    @SerialName("time_spent_novel") val timeSpentNovel: Long? = 0L,



    @SerialName("time_spent_app") val timeSpentApp: Long? = 0L,



    @SerialName("time_spent_total") val timeSpentTotal: Long? = 0L,



    @SerialName("weekly_time_spent") val weeklyTimeSpent: Long? = 0L,



    @SerialName("daily_time_spent") val dailyTimeSpent: Long? = 0L,



    @SerialName("manga_history_json") val mangaHistoryJson: String? = null,



    @SerialName("novel_history_json") val novelHistoryJson: String? = null,



    @SerialName("manga_bookmarks_json") val mangaBookmarksJson: String? = null,



    @SerialName("current_activity") val currentActivity: String? = null,



    @SerialName("current_activity_type") val currentActivityType: String? = null,



    @SerialName("current_activity_target_id") val currentActivityTargetId: String? = null,



    @SerialName("last_seen_at") val lastSeenAt: String? = null,



    @SerialName("is_online") val isOnline: Boolean? = false,



    @SerialName("is_watchlist_private") val isWatchlistPrivate: Boolean? = false,



    @SerialName("is_history_private") val isHistoryPrivate: Boolean? = false,



    @SerialName("is_manga_private") val isMangaPrivate: Boolean? = false,



    @SerialName("is_novel_private") val isNovelPrivate: Boolean? = false,



    @SerialName("created_at") val createdAt: String? = null,



    @SerialName("updated_at") val updatedAt: String? = null



)







@Serializable



data class CloudWatchlistItem(



    val id: Long = 0,



    @SerialName("user_id") val userId: String = "",



    @SerialName("anime_id") val animeId: Int = 0,



    val title: String = "",



    val cover: String = "",



    val banner: String? = null,



    val score: Int? = null,



    val genres: List<String>? = emptyList(),



    val episodes: Int? = null,



    val status: String? = "plan_to_watch",



    @SerialName("progress_episode") val progressEpisode: Int? = 0,



    @SerialName("release_year") val releaseYear: String? = null,



    @SerialName("custom_tags") val customTags: List<String>? = emptyList(),



    @SerialName("added_at") val addedAt: String? = null,



    @SerialName("updated_at") val updatedAt: String? = null



)







@Serializable



data class CloudHistoryItem(



    val id: Long = 0,



    @SerialName("user_id") val userId: String = "",



    @SerialName("anime_id") val animeId: Int = 0,



    val title: String = "",



    val cover: String = "",



    val banner: String? = null,



    @SerialName("last_episode") val lastEpisode: Int = 1,



    @SerialName("total_episodes") val totalEpisodes: Int? = null,



    @SerialName("position_ms") val positionMs: Long? = 0L,



    @SerialName("duration_ms") val durationMs: Long? = 0L,



    @SerialName("watched_at") val watchedAt: String? = null



)







@Serializable



data class CloudHistoryUpsertDto(



    @SerialName("user_id") val userId: String,



    @SerialName("anime_id") val animeId: Int,



    val title: String,



    val cover: String,



    val banner: String? = null,



    @SerialName("last_episode") val lastEpisode: Int = 1,



    @SerialName("total_episodes") val totalEpisodes: Int? = null,



    @SerialName("position_ms") val positionMs: Long? = 0L,



    @SerialName("duration_ms") val durationMs: Long? = 0L



)







@Serializable



data class UserSyncProfile(



    val name: String? = null,



    val avatar: String? = null,



    val banner: String? = null,



    val frame: String? = null,



    val bio: String? = null



)







@Serializable



data class WebHistoryItem(



    val id: Int,



    val ep: Int = 1,



    val dur: Long = 0L,



    val pos: Long = 0L,



    val image: String? = null,



    val title: String = "",



    val banner: String? = null,



    val t: Long = 0L,



    val score: Int? = null,



    val episodes: Int? = null,



    val audio: String = "sub",



    val color: String? = null,



    val server: String? = null,



    val year: Int? = null,



    val format: String? = null



)







@Serializable



data class WebWatchlistItem(



    val id: Int,



    val title: String = "",



    val image: String = "",



    val banner: String? = null,



    val status: String = "plan_to_watch",



    val progress: Int = 0,



    val score: Int? = null,



    val episodes: Int? = null,



    val addedAt: Long = 0L,



    val updatedAt: Long = 0L,



    val color: String? = null,



    val year: Int? = null,



    val format: String? = null



)







@Serializable



data class WebReadingItem(



    val id: String = "",



    val title: String = "",



    val cover: String = "",



    val kind: String = "manga",



    val label: String = "",



    val resume: String = "",



    val href: String = "",



    val progress: Int = 0,



    val t: Long = 0L



)







@Serializable



data class UserSyncRow(



    @SerialName("user_id") val userId: String = "",



    val username: String? = null,



    val profile: UserSyncProfile? = null,



    val history: List<WebHistoryItem>? = null,



    val watchlist: List<WebWatchlistItem>? = null,



    val reading: List<WebReadingItem>? = null,



    @SerialName("updated_at") val updatedAt: String? = null



)







data class CloudWatchlistUpsertDto(



    @SerialName("user_id") val userId: String,



    @SerialName("anime_id") val animeId: Int,



    val title: String,



    val cover: String,



    val banner: String? = null,



    val score: Int? = null,



    val genres: List<String>? = emptyList(),



    val episodes: Int? = null,



    val status: String? = "plan_to_watch",



    @SerialName("progress_episode") val progressEpisode: Int? = 0,



    @SerialName("release_year") val releaseYear: String? = null,



    @SerialName("custom_tags") val customTags: List<String>? = emptyList()



)







@Serializable



data class CommunityMessage(



    val id: String = "",



    @SerialName("user_id") val userId: String = "",



    val channel: String = "general",



    val message: String = "",



    @SerialName("display_name") val displayName: String? = null,



    val username: String? = null,



    @SerialName("avatar_url") val avatarUrl: String? = null,



    val role: String? = "USER",



    val roles: List<String>? = emptyList(),



    @SerialName("custom_tag") val customTag: String? = null,



    @SerialName("avatar_decoration") val avatarDecoration: String? = null,



    @SerialName("avatar_frame") val avatarFrame: String? = null,



    @SerialName("text_effect") val textEffect: String? = null,



    @SerialName("streak_count") val streakCount: Int? = null,



    @SerialName("reactions") val reactions: Map<String, Int>? = emptyMap(),



    @SerialName("reply_to_id") val replyToId: String? = null,



    @SerialName("reply_to_username") val replyToUsername: String? = null,



    @SerialName("reply_to_text") val replyToText: String? = null,



    @SerialName("reply_to_user_id") val replyToUserId: String? = null,



    @SerialName("is_mention") val isMention: Boolean? = false,



    @SerialName("created_at") val createdAt: String? = null



)







@Serializable



data class AppNotification(



    val id: String = "",



    @SerialName("user_id") val userId: String? = null, // null = global



    @SerialName("sender_id") val senderId: String? = null,



    @SerialName("sender_name") val senderName: String? = null,



    @SerialName("sender_avatar") val senderAvatar: String? = null,



    @SerialName("sender_decoration") val senderDecoration: String? = null,



    @SerialName("sender_frame") val senderFrame: String? = null,



    val title: String = "",



    val message: String = "",



    val type: String = "mention", // mention, reply, global, admin, system, reward, alert, announcement, update



    val channel: String? = null,



    @SerialName("target_id") val targetId: String? = null,



    @SerialName("image_url") val imageUrl: String? = null,



    @SerialName("action_url") val actionUrl: String? = null,



    @SerialName("action_label") val actionLabel: String? = null,



    @SerialName("is_claimed") val isClaimed: Boolean = false,



    @SerialName("is_read") val isRead: Boolean = false,



    @SerialName("created_at") val createdAt: String? = null



)







@Serializable



data class AdminUserSearchResult(



    val id: String = "",



    @SerialName("display_name") val displayName: String? = null,



    val username: String? = null,



    @SerialName("avatar_url") val avatarUrl: String? = null,



    val role: String? = "USER",



    val roles: List<String>? = emptyList(),



    val email: String? = null,



    @SerialName("created_at") val createdAt: String? = null



)







@Serializable



data class UserReactionItem(



    @SerialName("message_id") val messageId: String = "",



    val emoji: String = ""



)







@Serializable



data class UserNotificationState(



    @SerialName("notification_id") val notificationId: String = "",



    @SerialName("is_read") val isRead: Boolean = false,



    @SerialName("is_claimed") val isClaimed: Boolean = false



)







@Serializable



data class WelcomeTemplateDto(



    val id: String = "default",



    val title: String = "Welcome to Zenkai",



    val message: String = "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey",



    @SerialName("image_url") val imageUrl: String? = null,



    @SerialName("action_label") val actionLabel: String? = null,



    @SerialName("action_url") val actionUrl: String? = null,



    @SerialName("exp_reward") val expReward: Long? = 100L



)







@Serializable



data class CloudStreamingServer(



    val id: String = "",



    val name: String = "",



    val pattern: String = "",



    @SerialName("server_badge") val serverBadge: String = "HD",



    @SerialName("is_direct_stream") val isDirectStream: Boolean = false,



    @SerialName("is_embed_only") val isEmbedOnly: Boolean = false,



    @SerialName("is_multi_audio") val isMultiAudio: Boolean = false,



    @SerialName("is_active") val isActive: Boolean = true,



    @SerialName("sort_order") val sortOrder: Int = 0



)







@Serializable



data class CloudWorkerConfig(



    val key: String = "",



    val value: String = "",



    val description: String? = null,



    @SerialName("is_active") val isActive: Boolean = true



)







class SupabaseRepository {



    private val client = SupabaseManager.client







    suspend fun signUpWithEmail(email: String, password: String, displayName: String = ""): Result<String> {



        return try {



            client.auth.signUpWith(Email) {



                this.email = email



                this.password = password



                if (displayName.isNotBlank()) {



                    this.data = buildJsonObject {



                        put("display_name", displayName)



                    }



                }



            }



            // Auto sign in to guarantee an active authenticated session



            try {



                client.auth.signInWith(Email) {



                    this.email = email



                    this.password = password



                }



            } catch (_: Exception) { }



            val userId = client.auth.currentUserOrNull()?.id ?: ""



            Result.success(userId)



        } catch (e: Exception) {



            Result.failure(e)



        }



    }







    suspend fun signInWithEmail(email: String, password: String): Result<String> {



        return try {



            client.auth.signInWith(Email) {



                this.email = email



                this.password = password



            }



            Result.success(client.auth.currentUserOrNull()?.id ?: "")



        } catch (e: Exception) {



            Result.failure(e)



        }



    }







    suspend fun signInWithGoogle() {



        try {



            client.auth.signInWith(Google)



        } catch (e: Exception) {



            e.printStackTrace()



        }



    }







    suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {



        return try {



            client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.IDToken) {



                provider = Google



                this.idToken = idToken



            }



            Result.success(client.auth.currentUserOrNull()?.id ?: "")



        } catch (e: Exception) {



            Result.failure(e)



        }



    }







    suspend fun signOut() {



        try {



            client.auth.signOut()



        } catch (e: Exception) {



            e.printStackTrace()



        }



    }







    fun getCurrentUserId(): String? {



        return client.auth.currentUserOrNull()?.id



    }







    fun getCurrentUserEmail(): String? {



        return client.auth.currentUserOrNull()?.email



    }







    fun getAuthState(): AuthState {



        val user = client.auth.currentUserOrNull()



        return if (user != null) {



            AuthState.LoggedIn(user.id, user.email)



        } else {



            AuthState.LoggedOut



        }



    }







    suspend fun refreshSession() {



        try {



            client.auth.refreshCurrentSession()



        } catch (e: Exception) {



            e.printStackTrace()



        }



    }







    suspend fun getProfile(userId: String): CloudProfile? {



        return try {



            client.postgrest.from("profiles")



                .select { filter { eq("id", userId) } }



                .decodeSingleOrNull<CloudProfile>()



        } catch (e: Exception) {



            e.printStackTrace()



            null



        }



    }







    suspend fun getPublicProfile(userId: String): CloudProfile? = getProfile(userId)







    suspend fun isUsernameAvailable(username: String, currentUserId: String? = null): Boolean = withContext(Dispatchers.IO) {



        val clean = username.trim().lowercase().removePrefix("@")



        if (clean.isBlank()) return@withContext false



        try {



            val list = client.postgrest.from("profiles")



                .select {



                    filter {



                        ilike("username", clean)



                    }



                }



                .decodeList<CloudProfile>()



            if (currentUserId != null) {



                list.none { it.id != currentUserId }



            } else {



                list.isEmpty()



            }



        } catch (e: Exception) {



            true



        }



    }







    suspend fun getProfilesByIds(userIds: List<String>): Map<String, CloudProfile> = withContext(Dispatchers.IO) {



        val cleanIds = userIds.filter { it.isNotBlank() }.distinct()



        if (cleanIds.isEmpty()) return@withContext emptyMap()



        try {



            client.postgrest.from("profiles")



                .select {



                    filter {



                        isIn("id", cleanIds)



                    }



                }



                .decodeList<CloudProfile>()



                .associateBy { it.id }



        } catch (e: Exception) {



            e.printStackTrace()



            emptyMap()



        }



    }







    suspend fun updateProfile(



        userId: String,



        displayName: String? = null,



        username: String? = null,



        bio: String? = null,



        bioFont: String? = null,



        avatarUrl: String? = null,



        bannerUrl: String? = null,



        favoriteAnime: String? = null,



        otakuRank: String? = null,



        customTag: String? = null,



        roles: List<String>? = null,



        avatarDecoration: String? = null,



        profileEffect: String? = null,



        bannerTheme: String? = null,



        nameplate: String? = null,



        bannerEffect: String? = null,



        exp: Long? = null,



        level: Int? = null,



        streakCount: Int? = null,



        longestStreak: Int? = null,



        lastActiveDate: String? = null,



        aniListSync: String? = null,



        malSync: String? = null,



        timeSpentAnime: Long? = null,



        timeSpentManga: Long? = null,



        timeSpentNovel: Long? = null,



        timeSpentApp: Long? = null,



        timeSpentTotal: Long? = null,



        weeklyTimeSpent: Long? = null,



        dailyTimeSpent: Long? = null,



        mangaHistoryJson: String? = null,



        novelHistoryJson: String? = null,



        mangaBookmarksJson: String? = null,



        currentActivity: String? = null,



        currentActivityType: String? = null,



        currentActivityTargetId: String? = null,



        lastSeenAt: String? = null,



        isOnline: Boolean? = null,



        isWatchlistPrivate: Boolean? = null,



        isHistoryPrivate: Boolean? = null,



        isMangaPrivate: Boolean? = null,



        isNovelPrivate: Boolean? = null



    ) {



        try {



            val nowIso = java.time.Instant.now().toString()

            val effFrame = avatarDecoration

            val mapOfChanges = buildJsonObject {

                displayName?.let { put("display_name", it) }

                username?.let { put("username", it.trim().lowercase().removePrefix("@")) }

                bio?.let { put("bio", it) }

                avatarUrl?.let { put("avatar_url", it) }

                bannerUrl?.let { put("banner_url", it) }

                effFrame?.let { put("frame", it) }

                put("updated_at", nowIso)

            }



            if (mapOfChanges.isNotEmpty()) {

                client.postgrest.from("profiles")

                    .update(mapOfChanges) { filter { eq("id", userId) } }



                // Keep user_sync profile field aligned

                val syncProfile = buildJsonObject {

                    displayName?.let { put("name", it) }

                    avatarUrl?.let { put("avatar", it) }

                    bannerUrl?.let { put("banner", it) }

                    effFrame?.let { put("frame", it) }

                    bio?.let { put("bio", it) }

                }

                try {

                    client.postgrest.from("user_sync").update(buildJsonObject {

                        put("profile", syncProfile)

                        username?.let { put("username", it.trim().lowercase().removePrefix("@")) }

                        put("updated_at", nowIso)

                    }) { filter { eq("user_id", userId) } }

                } catch (_: Exception) {}

            }



        } catch (e: Exception) {



            e.printStackTrace()



        }



    }







    suspend fun updateDecorations(

        userId: String,

        avatarDecoration: String?,

        avatarFrame: String? = null,

        textEffect: String? = null,

        profileEffect: String? = null,

        bannerTheme: String? = null,

        nameplate: String? = null,

        nameplateEmoji: String? = null,

        bannerEffect: String? = null

    ) {

        try {

            val effFrame = avatarDecoration

            val nowIso = java.time.Instant.now().toString()

            val mapOfChanges = buildJsonObject {

                effFrame?.let { put("frame", it) }

                put("updated_at", nowIso)

            }

            client.postgrest.from("profiles")

                .update(mapOfChanges) { filter { eq("id", userId) } }



            try {

                val syncProf = buildJsonObject {

                    effFrame?.let { put("frame", it) }

                }

                client.postgrest.from("user_sync").update(buildJsonObject {

                    put("profile", syncProf)

                    put("updated_at", nowIso)

                }) { filter { eq("user_id", userId) } }

            } catch (_: Exception) {}

        } catch (e: Exception) {



            e.printStackTrace()



        }



    }







    suspend fun pushUserSync(



        userId: String,



        username: String? = null,



        profile: UserSyncProfile,



        history: List<WebHistoryItem>,



        watchlist: List<WebWatchlistItem>,



        reading: List<WebReadingItem>



    ): Boolean = withContext(Dispatchers.IO) {



        try {



            val nowIso = java.time.Instant.now().toString()



            val syncDto = UserSyncRow(



                userId = userId,



                username = username,



                profile = profile,



                history = history,



                watchlist = watchlist,



                reading = reading,



                updatedAt = nowIso



            )



            client.postgrest.from("user_sync").upsert(syncDto) {



                onConflict = "user_id"



            }



            // Also keep public.profiles in sync



            val profilePayload = buildJsonObject {



                put("id", userId)



                username?.let { put("username", it) }



                profile.name?.let { put("display_name", it) }



                profile.avatar?.let { put("avatar_url", it) }



                profile.banner?.let { put("banner_url", it) }



                profile.frame?.let { put("frame", it) }



                profile.bio?.let { put("bio", it) }



                put("updated_at", nowIso)



            }



            client.postgrest.from("profiles").upsert(profilePayload) {



                onConflict = "id"



            }



            true



        } catch (e: Exception) {



            e.printStackTrace()



            false



        }



    }







    suspend fun fetchUserSync(userId: String): UserSyncRow? = withContext(Dispatchers.IO) {



        try {



            client.postgrest.from("user_sync")



                .select { filter { eq("user_id", userId) } }



                .decodeSingleOrNull<UserSyncRow>()



        } catch (e: Exception) {



            e.printStackTrace()



            null



        }



    }







    suspend fun fetchWatchlist(userId: String): List<CloudWatchlistItem> {



        return try {



            val sync = fetchUserSync(userId)



            sync?.watchlist?.map { item ->



                CloudWatchlistItem(



                    userId = userId,



                    animeId = item.id,



                    title = item.title,



                    cover = item.image,



                    banner = item.banner,



                    score = item.score,



                    genres = emptyList(),



                    episodes = item.episodes,



                    status = item.status,



                    progressEpisode = item.progress,



                    addedAt = item.addedAt.toString(),



                    updatedAt = item.updatedAt.toString()



                )



            } ?: emptyList()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun upsertWatchlistItem(userId: String, item: CloudWatchlistItem) {



        // user_sync handles watchlist storage



    }







    suspend fun deleteWatchlistItem(userId: String, animeId: Int) {



        // user_sync handles watchlist storage



    }







    suspend fun syncWatchlist(userId: String, localItems: List<CloudWatchlistItem>) {



        // user_sync handles watchlist storage



    }







    suspend fun fetchWatchHistory(userId: String): List<CloudHistoryItem> {



        return try {



            val sync = fetchUserSync(userId)



            sync?.history?.map { item ->



                CloudHistoryItem(



                    userId = userId,



                    animeId = item.id,



                    title = item.title,



                    cover = item.image ?: "",



                    banner = item.banner,



                    lastEpisode = item.ep,



                    totalEpisodes = item.episodes,



                    positionMs = item.pos * 1000L,



                    durationMs = item.dur * 1000L,



                    watchedAt = item.t.toString()



                )



            } ?: emptyList()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun upsertWatchHistory(userId: String, item: CloudHistoryItem) {



        // user_sync handles watch history storage



    }







    suspend fun deleteWatchHistory(userId: String, animeId: Int) {



        // user_sync handles watch history storage



    }







    suspend fun syncWatchHistory(userId: String, localItems: List<CloudHistoryItem>) {



        // user_sync handles watch history storage



    }







    suspend fun uploadImage(userId: String, bytes: ByteArray, mimeType: String = "image/jpeg", prefix: String = "avatar"): Result<String> {



        return withContext(Dispatchers.IO) {



            try {



                // 1. Max 30 MB limit check



                val maxBytes = 30L * 1024 * 1024 // 31,457,280 bytes



                if (bytes.size > maxBytes) {



                    val mb = String.format("%.2f", bytes.size / (1024.0 * 1024.0))



                    return@withContext Result.failure(Exception("File size exceeds 30MB limit (${mb}MB)"))



                }







                // 2. Server-side Rate Limit check via RPC (3 uploads / 7 days)



                val effectiveUserId = if (userId.isNotBlank()) userId else "local_user"



                val rpcPayload = buildJsonObject {



                    put("p_user_id", effectiveUserId)



                    put("p_upload_type", prefix)



                    put("p_file_size", bytes.size.toLong())



                }







                val token = client.auth.currentSessionOrNull()?.accessToken ?: SupabaseManager.ANON_KEY



                val rpcUrl = "${SupabaseManager.SUPABASE_URL}/rest/v1/rpc/check_and_record_upload"



                val rpcRequest = Request.Builder()



                    .url(rpcUrl)



                    .addHeader("apikey", SupabaseManager.ANON_KEY)



                    .addHeader("Authorization", "Bearer $token")



                    .addHeader("Content-Type", "application/json")



                    .post(rpcPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))



                    .build()







                val okHttpClient = OkHttpClient()



                val rpcResponse = okHttpClient.newCall(rpcRequest).execute()



                val rpcBody = rpcResponse.body?.string() ?: ""







                if (!rpcResponse.isSuccessful) {



                    return@withContext Result.failure(Exception("Server validation failed: $rpcBody"))



                }







                val jsonElement = org.json.JSONObject(rpcBody)



                val allowed = jsonElement.optBoolean("allowed", false)



                val message = jsonElement.optString("message", "")



                if (!allowed) {



                    return@withContext Result.failure(Exception(if (message.isNotBlank()) message else "Weekly upload limit reached (3 uploads per week)."))



                }







                val ext = when {



                    mimeType.contains("gif", ignoreCase = true) -> "gif"



                    mimeType.contains("png", ignoreCase = true) -> "png"



                    mimeType.contains("webp", ignoreCase = true) -> "webp"



                    else -> "jpg"



                }



                val fileName = "${effectiveUserId}_${prefix}_${System.currentTimeMillis()}.$ext"



                val uploadUrl = "${SupabaseManager.SUPABASE_URL}/storage/v1/object/user_uploads/$fileName"







                val mediaType = mimeType.toMediaTypeOrNull()



                val requestBody = bytes.toRequestBody(mediaType)



                val uploadRequest = Request.Builder()



                    .url(uploadUrl)



                    .addHeader("apikey", SupabaseManager.ANON_KEY)



                    .addHeader("Authorization", "Bearer $token")



                    .addHeader("Content-Type", mimeType)



                    .post(requestBody)



                    .build()







                val response = okHttpClient.newCall(uploadRequest).execute()



                if (response.isSuccessful) {



                    val publicUrl = "${SupabaseManager.SUPABASE_URL}/storage/v1/object/public/user_uploads/$fileName"



                    Result.success(publicUrl)



                } else {



                    val err = response.body?.string() ?: "Unknown error"



                    Result.failure(Exception("Upload failed ($err)"))



                }



            } catch (e: Exception) {



                Result.failure(e)



            }



        }



    }







    suspend fun fetchCommunityMessages(channel: String = "general", limitCount: Long = 80): List<CommunityMessage> {



        return try {



            val msgs = client.postgrest.from("community_messages")



                .select {



                    filter { eq("channel", channel) }



                    order("created_at", Order.DESCENDING)



                    limit(limitCount)



                }



                .decodeList<CommunityMessage>()



            msgs.reversed()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    private fun isValidUuid(str: String?): Boolean {



        if (str.isNullOrBlank()) return false



        return try {



            java.util.UUID.fromString(str.trim())



            true



        } catch (_: Exception) {



            false



        }



    }







    suspend fun sendCommunityMessage(



        channel: String = "general",



        messageText: String,



        profile: com.nyyrox.aniverse.data.UserProfile,



        replyToId: String? = null,



        replyToUsername: String? = null,



        replyToText: String? = null,



        replyToUserId: String? = null,



        isMention: Boolean = false



    ): Result<Unit> {



        return try {



            val authId = client.auth.currentUserOrNull()?.id



            val profileId = profile.id?.takeIf { it.isNotBlank() }



            val currentUserId = when {



                isValidUuid(authId) -> authId!!



                isValidUuid(profileId) -> profileId!!



                !profileId.isNullOrBlank() -> java.util.UUID.nameUUIDFromBytes(profileId.toByteArray()).toString()



                else -> java.util.UUID.randomUUID().toString()



            }







            try {



                val payload = buildJsonObject {



                    put("user_id", currentUserId)



                    put("channel", channel)



                    put("message", messageText.trim())



                    put("display_name", profile.displayName.ifBlank { "Otaku" })



                    put("username", profile.username ?: "")



                    put("avatar_url", profile.avatarUrl ?: "")



                    put("role", profile.role ?: "USER")



                    if (profile.roles.isNotEmpty()) {



                        put("roles", buildJsonArray { profile.roles.forEach { add(it) } })



                    }



                    profile.customTag?.let { put("custom_tag", it) }



                    profile.avatarDecorationId?.let { put("avatar_decoration", it) }



                    profile.avatarFrameId?.let { put("avatar_frame", it) }



                    profile.textEffectId?.let { put("text_effect", it) }



                    if (profile.streakCount > 0) put("streak_count", profile.streakCount)



                    if (!replyToId.isNullOrBlank() && isValidUuid(replyToId)) {



                        put("reply_to_id", replyToId.trim())



                        put("reply_to_username", replyToUsername ?: "")



                        put("reply_to_text", replyToText ?: "")



                        if (!replyToUserId.isNullOrBlank() && isValidUuid(replyToUserId)) {



                            put("reply_to_user_id", replyToUserId.trim())



                        }



                        put("is_mention", isMention)



                    }



                }



                client.postgrest.from("community_messages").insert(payload)



            } catch (inner: Exception) {



                // Fallback to essential columns in case custom columns are missing in PostgREST schema cache



                val basicPayload = buildJsonObject {



                    put("user_id", currentUserId)



                    put("channel", channel)



                    put("message", messageText.trim())



                    put("display_name", profile.displayName.ifBlank { "Otaku" })



                    put("username", profile.username ?: "")



                    put("avatar_url", profile.avatarUrl ?: "")



                    put("role", profile.role ?: "USER")



                    if (!replyToId.isNullOrBlank() && isValidUuid(replyToId)) {



                        put("reply_to_id", replyToId.trim())



                        put("reply_to_username", replyToUsername ?: "")



                        put("reply_to_text", replyToText ?: "")



                    }



                }



                client.postgrest.from("community_messages").insert(basicPayload)



            }







            // If mention is enabled, dispatch in-app/push notification to the mentioned user



            if (isMention && !replyToUserId.isNullOrBlank() && isValidUuid(replyToUserId) && replyToUserId != currentUserId) {



                try {



                    val notifPreview = formatNotificationPreview(messageText.trim())



                    val notifPayload = buildJsonObject {



                        put("user_id", replyToUserId.trim())



                        put("sender_id", currentUserId)



                        put("sender_name", profile.displayName.ifBlank { profile.username ?: "Otaku" })



                        put("sender_avatar", profile.avatarUrl ?: "")



                        profile.avatarDecorationId?.let { put("sender_decoration", it) }



                        profile.avatarFrameId?.let { put("sender_frame", it) }



                        put("title", "Mentioned you in #$channel")



                        put("message", notifPreview)



                        put("type", "mention")



                        put("channel", channel)



                        if (!replyToId.isNullOrBlank() && isValidUuid(replyToId)) {



                            put("target_id", replyToId.trim())



                        }



                        put("is_read", false)



                    }



                    client.postgrest.from("notifications").insert(notifPayload)



                } catch (ne: Exception) {



                    ne.printStackTrace()



                }



            }







            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun editCommunityMessage(



        messageId: String,



        newMessageText: String



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id



                ?: return Result.failure(IllegalStateException("Please sign in to edit messages"))



            client.postgrest.from("community_messages")



                .update(mapOf("message" to newMessageText.trim())) {



                    filter {



                        eq("id", messageId)



                    }



                }



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun deleteCommunityMessage(



        messageId: String



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id



                ?: return Result.failure(IllegalStateException("Please sign in to delete messages"))



            client.postgrest.from("community_messages")



                .delete {



                    filter {



                        eq("id", messageId)



                    }



                }



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun toggleCommunityReaction(



        messageId: String,



        emoji: String



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id



                ?: return Result.failure(IllegalStateException("Please sign in to react"))



            client.postgrest.rpc(



                "toggle_community_reaction",



                buildJsonObject {



                    put("p_message_id", messageId)



                    put("p_emoji", emoji)



                }



            )



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun fetchUserReactions(messageIds: List<String>): Map<String, Set<String>> {



        if (messageIds.isEmpty()) return emptyMap()



        return try {



            val response = client.postgrest.rpc(



                "get_user_community_reactions",



                buildJsonObject {



                    put("p_message_ids", buildJsonArray { messageIds.forEach { add(it) } })



                }



            ).decodeList<UserReactionItem>()



            val map = mutableMapOf<String, MutableSet<String>>()



            response.forEach { item ->



                map.getOrPut(item.messageId) { mutableSetOf() }.add(item.emoji)



            }



            map



        } catch (e: Exception) {



            e.printStackTrace()



            emptyMap()



        }



    }







    // ── Notifications System Methods ──────────────────────────────



    suspend fun fetchNotifications(userId: String?): List<AppNotification> {



        return try {



            val rawNotifs = client.postgrest.from("notifications")



                .select {



                    order("created_at", Order.DESCENDING)



                    limit(60)



                }



                .decodeList<AppNotification>()







            // Strictly filter out notifications meant for other specific users



            val notifs = rawNotifs.filter { notif ->



                notif.userId.isNullOrBlank() || (!userId.isNullOrBlank() && notif.userId == userId)



            }







            if (!userId.isNullOrBlank()) {



                val states = try {



                    client.postgrest.from("user_notification_states")



                        .select {



                            filter {



                                eq("user_id", userId)



                            }



                        }



                        .decodeList<UserNotificationState>()



                } catch (_: Exception) {



                    emptyList()



                }







                if (states.isNotEmpty()) {



                    val stateMap = states.associateBy { it.notificationId }



                    return notifs.map { notif ->



                        val userState = stateMap[notif.id]



                        if (userState != null) {



                            notif.copy(



                                isRead = notif.isRead || userState.isRead,



                                isClaimed = notif.isClaimed || userState.isClaimed



                            )



                        } else {



                            notif



                        }



                    }



                }



            }



            notifs



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun markNotificationAsRead(id: String, userId: String? = null): Result<Unit> {



        return try {



            client.postgrest.rpc(



                "mark_notification_read",



                buildJsonObject {



                    put("p_notification_id", id)



                }



            )



            if (!userId.isNullOrBlank()) {



                try {



                    client.postgrest.from("user_notification_states").upsert(



                        mapOf(



                            "user_id" to userId,



                            "notification_id" to id,



                            "is_read" to true



                        )



                    )



                } catch (_: Exception) {}



            }



            Result.success(Unit)



        } catch (e: Exception) {



            try {



                client.postgrest.from("notifications")



                    .update(mapOf("is_read" to true)) {



                        filter { eq("id", id) }



                    }



                if (!userId.isNullOrBlank()) {



                    try {



                        client.postgrest.from("user_notification_states").upsert(



                            mapOf(



                                "user_id" to userId,



                                "notification_id" to id,



                                "is_read" to true



                            )



                        )



                    } catch (_: Exception) {}



                }



                Result.success(Unit)



            } catch (e2: Exception) {



                e2.printStackTrace()



                Result.failure(e2)



            }



        }



    }







    suspend fun markAllNotificationsAsRead(userId: String, notifIds: List<String> = emptyList()): Result<Unit> {



        return try {



            client.postgrest.rpc("mark_all_notifications_read")



            if (notifIds.isNotEmpty()) {



                try {



                    val statesToInsert = notifIds.map { notifId ->



                        mapOf(



                            "user_id" to userId,



                            "notification_id" to notifId,



                            "is_read" to true



                        )



                    }



                    client.postgrest.from("user_notification_states").upsert(statesToInsert)



                } catch (_: Exception) {}



            }



            Result.success(Unit)



        } catch (e: Exception) {



            try {



                client.postgrest.from("notifications")



                    .update(mapOf("is_read" to true)) {



                        filter {



                            eq("user_id", userId)



                            eq("is_read", false)



                        }



                    }



                if (notifIds.isNotEmpty()) {



                    try {



                        val statesToInsert = notifIds.map { notifId ->



                            mapOf(



                                "user_id" to userId,



                                "notification_id" to notifId,



                                "is_read" to true



                            )



                        }



                        client.postgrest.from("user_notification_states").upsert(statesToInsert)



                    } catch (_: Exception) {}



                }



                Result.success(Unit)



            } catch (e2: Exception) {



                e2.printStackTrace()



                Result.failure(e2)



            }



        }



    }







    suspend fun deleteNotification(id: String, userId: String? = null): Result<Unit> {



        return try {



            client.postgrest.from("notifications")



                .delete {



                    filter { eq("id", id) }



                }



            if (!userId.isNullOrBlank()) {



                try {



                    client.postgrest.from("user_notification_states").upsert(



                        mapOf(



                            "user_id" to userId,



                            "notification_id" to id,



                            "is_read" to true



                        )



                    )



                } catch (_: Exception) {}



            }



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun adminSearchUsers(queryText: String): List<AdminUserSearchResult> {



        if (queryText.isBlank()) return emptyList()



        return try {



            client.postgrest.rpc(



                "admin_search_users",



                buildJsonObject {



                    put("p_query", queryText.trim())



                }



            ).decodeList<AdminUserSearchResult>()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun markNotificationClaimed(id: String): Result<Unit> {



        return try {



            client.postgrest.rpc(



                "claim_notification_reward",



                buildJsonObject {



                    put("p_notification_id", id)



                }



            )



            Result.success(Unit)



        } catch (e: Exception) {



            try {



                client.postgrest.from("notifications")



                    .update(mapOf("is_claimed" to true, "is_read" to true)) {



                        filter { eq("id", id) }



                    }



                Result.success(Unit)



            } catch (e2: Exception) {



                e2.printStackTrace()



                Result.failure(e2)



            }



        }



    }







    suspend fun sendAdminBroadcast(



        title: String,



        message: String,



        type: String = "global",



        imageUrl: String? = null,



        actionUrl: String? = null,



        actionLabel: String? = null,



        senderProfile: com.nyyrox.aniverse.data.UserProfile



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id



            val payload = buildJsonObject {



                put("user_id", null as String?)



                if (currentUserId != null) put("sender_id", currentUserId)



                put("sender_name", senderProfile.displayName.ifBlank { "ZENKAI Staff" })



                put("sender_avatar", senderProfile.avatarUrl)



                senderProfile.avatarDecorationId?.let { put("sender_decoration", it) }



                senderProfile.avatarFrameId?.let { put("sender_frame", it) }



                put("title", title.trim())



                put("message", message.trim())



                put("type", type)



                if (!imageUrl.isNullOrBlank()) put("image_url", imageUrl.trim())



                if (!actionUrl.isNullOrBlank()) put("action_url", actionUrl.trim())



                if (!actionLabel.isNullOrBlank()) put("action_label", actionLabel.trim())



                put("is_claimed", false)



                put("is_read", false)



            }



            client.postgrest.from("notifications").insert(payload)



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun sendAdminDirectNotification(



        targetUserId: String,



        title: String,



        message: String,



        type: String = "admin",



        imageUrl: String? = null,



        actionUrl: String? = null,



        actionLabel: String? = null,



        senderProfile: com.nyyrox.aniverse.data.UserProfile



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id



            val payload = buildJsonObject {



                put("user_id", targetUserId)



                if (currentUserId != null) put("sender_id", currentUserId)



                put("sender_name", senderProfile.displayName.ifBlank { "ZENKAI Staff" })



                put("sender_avatar", senderProfile.avatarUrl)



                senderProfile.avatarDecorationId?.let { put("sender_decoration", it) }



                senderProfile.avatarFrameId?.let { put("sender_frame", it) }



                put("title", title.trim())



                put("message", message.trim())



                put("type", type)



                if (!imageUrl.isNullOrBlank()) put("image_url", imageUrl.trim())



                if (!actionUrl.isNullOrBlank()) put("action_url", actionUrl.trim())



                if (!actionLabel.isNullOrBlank()) put("action_label", actionLabel.trim())



                put("is_claimed", false)



                put("is_read", false)



            }



            client.postgrest.from("notifications").insert(payload)



            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun updateUserRolesAndAdmin(



        userId: String,



        isAdmin: Boolean,



        roles: List<String>,



        primaryRole: String? = null



    ): Boolean {



        return try {



            val calculatedRole = primaryRole ?: when {



                roles.contains("OWNER") -> "OWNER"



                isAdmin || roles.contains("ADMIN") -> "ADMIN"



                roles.contains("MODERATOR") -> "MODERATOR"



                roles.contains("VIP") -> "VIP"



                else -> "MEMBER"



            }



            val changes = buildJsonObject {



                put("is_admin", isAdmin)



                put("role", calculatedRole)



                put("roles", buildJsonArray { roles.forEach { add(it) } })



            }



            client.postgrest.from("profiles")



                .update(changes) { filter { eq("id", userId) } }



            true



        } catch (e: Exception) {



            e.printStackTrace()



            false



        }



    }







    suspend fun fetchWelcomeTemplate(): com.nyyrox.aniverse.data.WelcomeNotificationConfig? {



        return try {



            val list = client.postgrest.from("welcome_template")



                .select {



                    filter {



                        eq("id", "default")



                    }



                    limit(1)



                }.decodeList<WelcomeTemplateDto>()



            val item = list.firstOrNull() ?: return null



            com.nyyrox.aniverse.data.WelcomeNotificationConfig(



                title = item.title.ifBlank { "Welcome to Zenkai" },



                message = item.message.ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" },



                imageUrl = item.imageUrl.orEmpty(),



                actionLabel = item.actionLabel ?: "Claim +100 Starter EXP",



                actionUrl = item.actionUrl ?: "zenkai://rewards/welcome"



            )



        } catch (e: Exception) {



            e.printStackTrace()



            null



        }



    }







    suspend fun saveWelcomeTemplate(config: com.nyyrox.aniverse.data.WelcomeNotificationConfig): Boolean {



        return try {



            val payload = buildJsonObject {



                put("id", "default")



                put("title", config.title.trim().ifBlank { "Welcome to Zenkai" })



                put("message", config.message.trim().ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" })



                put("image_url", config.imageUrl.trim())



                put("action_label", config.actionLabel.trim().ifBlank { "Claim +100 Starter EXP" })



                put("action_url", config.actionUrl.trim().ifBlank { "zenkai://rewards/welcome" })



                put("updated_at", com.nyyrox.aniverse.util.DateTimeUtils.getCurrentIsoUtc())



            }



            client.postgrest.from("welcome_template").upsert(payload)



            true



        } catch (e: Exception) {



            e.printStackTrace()



            false



        }



    }







    // ── Social, Presence & Direct Messaging ──────────────────────────────



    fun getDmChannelId(userA: String, userB: String): String {



        return if (userA < userB) "dm_${userA}_$userB" else "dm_${userB}_$userA"



    }







    suspend fun updateUserActivity(



        userId: String,



        activity: String?,



        activityType: String? = null,



        targetId: String? = null,



        isOnline: Boolean = true



    ) {



        // Disabled: Activity status tracking removed per user request to conserve Supabase quota/cache



        return



    }







    suspend fun searchUsers(query: String, limit: Int = 30): List<CloudProfile> {



        val q = query.trim()



        if (q.isBlank()) return emptyList()



        return try {



            client.postgrest.from("profiles")



                .select {



                    filter {



                        or {



                            ilike("username", "%$q%")



                            ilike("display_name", "%$q%")



                        }



                    }



                    limit(limit.toLong())



                }



                .decodeList<CloudProfile>()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun fetchActiveUsers(limit: Int = 40): List<CloudProfile> {



        return try {



            client.postgrest.from("profiles")



                .select {



                    order("last_seen_at", Order.DESCENDING)



                    limit(limit.toLong())



                }



                .decodeList<CloudProfile>()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun fetchWatchTimeRankings(timeframe: String = "weekly", limit: Int = 100): List<CloudProfile> {



        val column = when (timeframe.lowercase()) {



            "day", "daily" -> "daily_time_spent"



            "week", "weekly" -> "weekly_time_spent"



            else -> "time_spent_total"



        }



        return try {



            val profiles = client.postgrest.from("profiles")



                .select()



                .decodeList<CloudProfile>()







            profiles



                .sortedByDescending { profile ->



                    val anime = profile.timeSpentAnime ?: 0L



                    val manga = profile.timeSpentManga ?: 0L



                    val novel = profile.timeSpentNovel ?: 0L



                    val mediaTotal = anime + manga + novel



                    when (column) {



                        "daily_time_spent" -> (profile.dailyTimeSpent ?: 0L).coerceAtMost(mediaTotal)



                        "weekly_time_spent" -> (profile.weeklyTimeSpent ?: 0L).coerceAtMost(mediaTotal)



                        else -> mediaTotal



                    }



                }



                .filter { profile ->



                    val anime = profile.timeSpentAnime ?: 0L



                    val manga = profile.timeSpentManga ?: 0L



                    val novel = profile.timeSpentNovel ?: 0L



                    val mediaTotal = anime + manga + novel



                    val time = when (column) {



                        "daily_time_spent" -> (profile.dailyTimeSpent ?: 0L).coerceAtMost(mediaTotal)



                        "weekly_time_spent" -> (profile.weeklyTimeSpent ?: 0L).coerceAtMost(mediaTotal)



                        else -> mediaTotal



                    }



                    time > 0L || (profile.exp ?: 0L) > 0L



                }



                .take(limit)



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun loadDirectMessages(dmChannel: String, limit: Int = 100): List<CommunityMessage> {



        return try {



            val msgs = client.postgrest.from("community_messages")



                .select {



                    filter {



                        eq("channel", dmChannel)



                    }



                    order("created_at", Order.DESCENDING)



                    limit(limit.toLong())



                }



                .decodeList<CommunityMessage>()



            msgs.reversed()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    private fun formatNotificationPreview(text: String): String {



        val clean = text.trim()



        if (clean.contains("[GIF|")) {



            val payload = clean.substringAfter("[GIF|").substringBefore("]")



            val parts = payload.split("|")



            val title = parts.getOrNull(1)?.trim()



            val comment = clean.replace("\\[GIF\\|.*?\\]".toRegex(), "").trim()



            return when {



                !title.isNullOrBlank() && comment.isNotBlank() -> "🎬 GIF: $title • $comment"



                !title.isNullOrBlank() -> "🎬 GIF: $title"



                comment.isNotBlank() -> "🎬 GIF: $comment"



                else -> "🎬 GIF"



            }



        }



        if (clean.contains("[IMG|") || clean.contains("[IMAGE|")) {



            val comment = clean.replace("\\[(IMG|IMAGE)\\|.*?\\]".toRegex(), "").trim()



            return if (comment.isNotBlank()) "🖼️ Image: $comment" else "🖼️ Image"



        }



        if (clean.contains(".gif", true) || clean.contains("tenor.com", true) || clean.contains("giphy.com", true)) {



            val words = clean.split("\\s+".toRegex())



            val comment = words.filter { !it.contains(".gif", true) && !it.contains("tenor.com", true) && !it.contains("giphy.com", true) }.joinToString(" ").trim()



            return if (comment.isNotBlank()) "🎬 GIF: $comment" else "🎬 GIF"



        }



        if (clean.endsWith(".png", true) || clean.endsWith(".jpg", true) || clean.endsWith(".jpeg", true) || clean.endsWith(".webp", true)) {



            val words = clean.split("\\s+".toRegex())



            val comment = words.filter { !it.startsWith("http") }.joinToString(" ").trim()



            return if (comment.isNotBlank()) "🖼️ Image: $comment" else "🖼️ Image"



        }



        return clean.take(120)



    }







    suspend fun sendDirectMessage(



        targetUserId: String,



        text: String,



        replyToId: String? = null,



        replyToUsername: String? = null,



        replyToText: String? = null,



        senderProfile: com.nyyrox.aniverse.data.UserProfile



    ): Result<Unit> {



        return try {



            val currentUserId = client.auth.currentUserOrNull()?.id ?: senderProfile.id?.takeIf { it.isNotBlank() }



                ?: return Result.failure(IllegalStateException("Please sign in to send direct messages"))



            



            val dmChannel = getDmChannelId(currentUserId, targetUserId)



            val cleanText = text.trim()



            if (cleanText.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty"))







            val msgPayload = buildJsonObject {



                put("user_id", currentUserId)



                put("channel", dmChannel)



                put("message", cleanText)



                put("display_name", senderProfile.displayName.ifBlank { "Otaku" })



                if (senderProfile.username.isNotBlank()) put("username", senderProfile.username)



                if (senderProfile.avatarUrl.isNotBlank()) put("avatar_url", senderProfile.avatarUrl)



                senderProfile.role?.let { put("role", it) }



                senderProfile.customTag?.let { put("custom_tag", it) }



                senderProfile.avatarDecorationId?.let { put("avatar_decoration", it) }



                senderProfile.avatarFrameId?.let { put("avatar_frame", it) }



                senderProfile.textEffectId?.let { put("text_effect", it) }



                put("streak_count", senderProfile.streakCount)



                if (!replyToId.isNullOrBlank()) {



                    put("reply_to_id", replyToId)



                    put("reply_to_username", replyToUsername ?: "")



                    put("reply_to_text", replyToText ?: "")



                    put("reply_to_user_id", targetUserId)



                    put("is_mention", true)



                }



            }







            client.postgrest.from("community_messages").insert(msgPayload)







            // Also dispatch real-time push notification to target user



            try {



                val notifMsg = formatNotificationPreview(cleanText)



                val notifPayload = buildJsonObject {



                    put("user_id", targetUserId)



                    put("sender_id", currentUserId)



                    put("sender_name", senderProfile.displayName.ifBlank { "Otaku" })



                    put("sender_avatar", senderProfile.avatarUrl)



                    senderProfile.avatarDecorationId?.let { put("sender_decoration", it) }



                    senderProfile.avatarFrameId?.let { put("sender_frame", it) }



                    put("title", "💬 Direct Message from ${senderProfile.displayName.ifBlank { "Otaku" }}")



                    put("message", notifMsg)



                    put("type", "dm")



                    put("channel", dmChannel)



                    put("target_id", currentUserId)



                    put("is_read", false)



                    put("is_claimed", false)



                }



                client.postgrest.from("notifications").insert(notifPayload)



            } catch (_: Exception) {}







            Result.success(Unit)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun fetchRecentDmPartners(currentUserId: String): List<CloudProfile> {



        return try {



            // Find recent community messages where channel starts with dm_ and contains currentUserId



            val recentMessages = client.postgrest.from("community_messages")



                .select {



                    filter {



                        ilike("channel", "%dm_%$currentUserId%")



                    }



                    order("created_at", Order.DESCENDING)



                    limit(150)



                }



                .decodeList<CommunityMessage>()







            val partnerIds = recentMessages.mapNotNull { msg ->



                val parts = msg.channel.removePrefix("dm_").split("_")



                parts.firstOrNull { it != currentUserId && it.isNotBlank() }



            }.distinct()







            if (partnerIds.isEmpty()) return emptyList()







            client.postgrest.from("profiles")



                .select {



                    filter {



                        isIn("id", partnerIds)



                    }



                }



                .decodeList<CloudProfile>()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    // ── Watch Together Real-time Synchronization Engine ───────────────────



    private val localWatchRoomsCache = java.util.concurrent.ConcurrentHashMap<String, WatchTogetherRoom>()







    suspend fun createWatchTogetherRoom(



        name: String,



        hostProfile: com.nyyrox.aniverse.data.UserProfile,



        animeId: Int,



        animeTitle: String,



        animeCover: String,



        idMal: Int? = null,



        episode: Int = 1,



        providerId: String = "reanime-sub",



        isPrivate: Boolean = false



    ): Result<WatchTogetherRoom> {



        return try {



            val roomId = "room_${System.currentTimeMillis().toString().takeLast(6)}_${(1000..9999).random()}"



            val roomCode = ((100000..999999).random()).toString()



            val hostMember = WatchTogetherMember(



                userId = hostProfile.id ?: "host_${System.currentTimeMillis()}",



                username = hostProfile.username.ifBlank { "Otaku" },



                displayName = hostProfile.displayName.ifBlank { "Host" },



                avatarUrl = hostProfile.avatarUrl,



                isHost = true



            )



            val room = WatchTogetherRoom(



                id = roomId,



                name = name.ifBlank { "$animeTitle Watch Party" },



                hostId = hostMember.userId,



                hostName = hostMember.displayName,



                hostAvatar = hostMember.avatarUrl,



                animeId = animeId,



                animeTitle = animeTitle,



                animeCover = animeCover,



                idMal = idMal,



                episode = episode,



                providerId = providerId,



                isPlaying = true,



                positionMs = 0L,



                updatedAt = System.currentTimeMillis(),



                createdAt = System.currentTimeMillis(),



                members = listOf(hostMember),



                isPrivate = isPrivate,



                roomCode = roomCode



            )







            localWatchRoomsCache[roomId] = room



            localWatchRoomsCache[roomCode] = room







            try {



                val payload = buildJsonObject {



                    put("id", roomId)



                    put("name", room.name)



                    put("host_id", room.hostId)



                    put("host_name", room.hostName)



                    put("host_avatar", room.hostAvatar)



                    put("anime_id", room.animeId)



                    put("anime_title", room.animeTitle)



                    put("anime_cover", room.animeCover)



                    if (idMal != null) put("id_mal", idMal)



                    put("episode", room.episode)



                    put("provider_id", room.providerId)



                    put("is_playing", room.isPlaying)



                    put("position_ms", room.positionMs)



                    put("updated_at", room.updatedAt)



                    put("is_private", room.isPrivate)



                    put("room_code", room.roomCode)



                }



                client.postgrest.from("watch_parties").insert(payload)



            } catch (_: Exception) {}







            Result.success(room)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun fetchWatchTogetherRoom(roomIdOrCode: String): WatchTogetherRoom? {



        return try {



            val cached = localWatchRoomsCache[roomIdOrCode]



            try {



                val remoteRoom = client.postgrest.from("watch_parties")



                    .select {



                        filter {



                            or {



                                eq("id", roomIdOrCode)



                                eq("room_code", roomIdOrCode)



                            }



                        }



                        limit(1)



                    }



                    .decodeSingleOrNull<WatchTogetherRoom>()







                if (remoteRoom != null) {



                    localWatchRoomsCache[remoteRoom.id] = remoteRoom



                    localWatchRoomsCache[remoteRoom.roomCode] = remoteRoom



                    return remoteRoom



                }



            } catch (_: Exception) {}



            cached



        } catch (e: Exception) {



            e.printStackTrace()



            localWatchRoomsCache[roomIdOrCode]



        }



    }







    suspend fun deleteWatchTogetherRoom(roomId: String): Boolean {



        return try {



            val room = localWatchRoomsCache.remove(roomId)



            if (room != null) {



                localWatchRoomsCache.remove(room.roomCode)



            }



            try {



                client.postgrest.from("watch_parties").delete {



                    filter { eq("id", roomId) }



                }



            } catch (_: Exception) {}



            true



        } catch (e: Exception) {



            e.printStackTrace()



            false



        }



    }







    suspend fun pingHostActivity(roomId: String): Boolean {



        return try {



            val current = localWatchRoomsCache[roomId] ?: fetchWatchTogetherRoom(roomId)



            if (current != null) {



                val updated = current.copy(



                    hostLastActive = System.currentTimeMillis(),



                    isActive = true



                )



                localWatchRoomsCache[roomId] = updated



                localWatchRoomsCache[updated.roomCode] = updated



                try {



                    val payload = buildJsonObject {



                        put("host_last_active", updated.hostLastActive)



                        put("is_active", true)



                    }



                    client.postgrest.from("watch_parties").update(payload) {



                        filter { eq("id", roomId) }



                    }



                } catch (_: Exception) {}



            }



            true



        } catch (e: Exception) {



            e.printStackTrace()



            false



        }



    }







    suspend fun fetchActiveWatchTogetherRooms(): List<WatchTogetherRoom> {



        return try {



            val now = System.currentTimeMillis()



            val fiveMinutesMs = 5 * 60 * 1000L







            // Clean up locally expired rooms



            val expiredKeys = localWatchRoomsCache.entries



                .filter { now - it.value.hostLastActive > fiveMinutesMs || !it.value.isActive }



                .map { it.key }



            expiredKeys.forEach { localWatchRoomsCache.remove(it) }







            val remoteList = try {



                client.postgrest.from("watch_parties")



                    .select {



                        filter { eq("is_private", false) }



                        order("updated_at", Order.DESCENDING)



                        limit(30)



                    }



                    .decodeList<WatchTogetherRoom>()



            } catch (_: Exception) {



                emptyList()



            }







            // Filter out inactive rooms (host disconnected > 5 mins)



            val validRemote = remoteList.filter { room ->



                room.isActive && (now - room.hostLastActive <= fiveMinutesMs)



            }







            // Fire and forget cleanup for stale remote rooms



            val staleRemote = remoteList.filter { room ->



                !room.isActive || (now - room.hostLastActive > fiveMinutesMs)



            }



            if (staleRemote.isNotEmpty()) {



                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {



                    staleRemote.forEach { stale ->



                        try {



                            client.postgrest.from("watch_parties").delete {



                                filter { eq("id", stale.id) }



                            }



                        } catch (_: Exception) {}



                    }



                }



            }







            (validRemote + localWatchRoomsCache.values.filter { !it.isPrivate && it.isActive && (now - it.hostLastActive <= fiveMinutesMs) }).distinctBy { it.id }



        } catch (e: Exception) {



            e.printStackTrace()



            val now = System.currentTimeMillis()



            localWatchRoomsCache.values.filter { !it.isPrivate && it.isActive && (now - it.hostLastActive <= 300_000L) }.toList()



        }



    }







    suspend fun updateWatchTogetherPlayback(



        roomId: String,



        episode: Int? = null,



        providerId: String? = null,



        isPlaying: Boolean? = null,



        positionMs: Long? = null



    ): Result<WatchTogetherRoom> {



        return try {



            val current = fetchWatchTogetherRoom(roomId)



                ?: return Result.failure(IllegalStateException("Room not found"))







            val updated = current.copy(



                episode = episode ?: current.episode,



                providerId = providerId ?: current.providerId,



                isPlaying = isPlaying ?: current.isPlaying,



                positionMs = positionMs ?: current.positionMs,



                updatedAt = System.currentTimeMillis(),



                hostLastActive = System.currentTimeMillis(),



                isActive = true



            )







            localWatchRoomsCache[roomId] = updated



            localWatchRoomsCache[updated.roomCode] = updated







            try {



                val payload = buildJsonObject {



                    put("episode", updated.episode)



                    put("provider_id", updated.providerId)



                    put("is_playing", updated.isPlaying)



                    put("position_ms", updated.positionMs)



                    put("updated_at", updated.updatedAt)



                    put("host_last_active", updated.hostLastActive)



                    put("is_active", true)



                }



                client.postgrest.from("watch_parties").update(payload) {



                    filter { eq("id", roomId) }



                }



            } catch (_: Exception) {}







            Result.success(updated)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun joinWatchTogetherRoom(



        roomIdOrCode: String,



        profile: com.nyyrox.aniverse.data.UserProfile



    ): Result<WatchTogetherRoom> {



        return try {



            val room = fetchWatchTogetherRoom(roomIdOrCode)



                ?: return Result.failure(IllegalStateException("Party room not found"))







            val userId = profile.id ?: "member_${System.currentTimeMillis()}"



            val existing = room.members.find { it.userId == userId }



            val newMembers = if (existing != null) {



                room.members



            } else {



                room.members + WatchTogetherMember(



                    userId = userId,



                    username = profile.username.ifBlank { "Otaku" },



                    displayName = profile.displayName.ifBlank { "Otaku" },



                    avatarUrl = profile.avatarUrl,



                    isHost = (userId == room.hostId)



                )



            }



            val updated = room.copy(members = newMembers)



            localWatchRoomsCache[room.id] = updated



            localWatchRoomsCache[room.roomCode] = updated



            Result.success(updated)



        } catch (e: Exception) {



            e.printStackTrace()



            Result.failure(e)



        }



    }







    suspend fun fetchStreamingServers(): List<Provider> = withContext(Dispatchers.IO) {



        try {



            val list = client.postgrest.from("streaming_servers")



                .select {



                    filter {



                        eq("is_active", true)



                    }



                    order("sort_order", Order.ASCENDING)



                }



                .decodeList<CloudStreamingServer>()







            if (list.isNotEmpty()) {



                list.map {



                    Provider(



                        id = it.id,



                        name = it.name,



                        pattern = it.pattern,



                        serverBadge = it.serverBadge,



                        isDirectStream = it.isDirectStream,



                        isEmbedOnly = it.isEmbedOnly,



                        isMultiAudio = it.isMultiAudio



                    )



                }



            } else emptyList()



        } catch (e: Exception) {



            e.printStackTrace()



            emptyList()



        }



    }







    suspend fun fetchWorkerConfigs(): Map<String, String> = withContext(Dispatchers.IO) {



        try {



            val list = client.postgrest.from("worker_configs")



                .select {



                    filter {



                        eq("is_active", true)



                    }



                }



                .decodeList<CloudWorkerConfig>()







            list.associate { it.key to it.value }



        } catch (e: Exception) {



            e.printStackTrace()



            emptyMap()



        }



    }



}







@Serializable



data class WatchTogetherMember(



    @SerialName("user_id") val userId: String = "",



    @SerialName("username") val username: String = "Otaku",



    @SerialName("display_name") val displayName: String = "Otaku",



    @SerialName("avatar_url") val avatarUrl: String = "",



    @SerialName("is_host") val isHost: Boolean = false,



    @SerialName("joined_at") val joinedAt: Long = System.currentTimeMillis()



)







@Serializable



data class WatchTogetherRoom(



    val id: String = "",



    val name: String = "Anime Watch Party",



    @SerialName("host_id") val hostId: String = "",



    @SerialName("host_name") val hostName: String = "Otaku",



    @SerialName("host_avatar") val hostAvatar: String = "",



    @SerialName("anime_id") val animeId: Int = 0,



    @SerialName("anime_title") val animeTitle: String = "",



    @SerialName("anime_cover") val animeCover: String = "",



    @SerialName("id_mal") val idMal: Int? = null,



    val episode: Int = 1,



    @SerialName("provider_id") val providerId: String = "reanime-sub",



    @SerialName("is_playing") val isPlaying: Boolean = true,



    @SerialName("position_ms") val positionMs: Long = 0L,



    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),



    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),



    val members: List<WatchTogetherMember> = emptyList(),



    @SerialName("is_private") val isPrivate: Boolean = false,



    @SerialName("room_code") val roomCode: String = "",



    @SerialName("host_last_active") val hostLastActive: Long = System.currentTimeMillis(),



    @SerialName("is_active") val isActive: Boolean = true



)







