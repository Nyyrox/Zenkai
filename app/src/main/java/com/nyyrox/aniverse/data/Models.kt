package com.nyyrox.aniverse.data

import java.util.Locale

data class MediaRelation(
    val id: Int,
    val type: String = "ANIME",
    val relationType: String,
    val title: String,
    val romajiTitle: String? = null,
    val cover: String,
    val format: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val score: Int? = null
) {
    val displayRelationType: String
        get() = relationType.replace('_', ' ').lowercase(Locale.US).split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }

    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: romajiTitle?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: "Unknown Anime"
}

data class AnimeCharacter(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val image: String? = null,
    val role: String = "SUPPORTING",
    val voiceActorName: String? = null,
    val voiceActorNative: String? = null,
    val voiceActorImage: String? = null,
    val voiceActorLanguage: String? = null
)

data class AnimeStaff(
    val id: Int,
    val name: String,
    val role: String,
    val image: String? = null
)

data class AnimeRecommendation(
    val id: Int,
    val title: String,
    val romajiTitle: String? = null,
    val cover: String,
    val banner: String? = null,
    val score: Int? = null,
    val format: String? = null,
    val episodes: Int? = null
)

data class Anime(
    val id: Int,
    val idMal: Int? = null,
    val title: String,
    val romajiTitle: String? = null,
    val cover: String,
    val banner: String? = null,
    val episodes: Int? = null,
    val status: String = "UNKNOWN",
    val score: Int? = null,
    val description: String = "",
    val nextEpisode: Int? = null,
    val nextAt: Long? = null,
    val genres: List<String> = emptyList(),
    val seasonYear: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val duration: Int? = null,
    val studios: List<String> = emptyList(),
    val source: String? = null,
    val trailerId: String? = null,
    val relations: List<MediaRelation> = emptyList(),
    val characters: List<AnimeCharacter> = emptyList(),
    val staff: List<AnimeStaff> = emptyList(),
    val recommendations: List<AnimeRecommendation> = emptyList()
) {
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: romajiTitle?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            ?: "Unknown Anime"

    val displayScore: String
        get() = score?.let { "★ ${(it / 10f).let { s -> String.format(Locale.US, "%.1f", s) }}" } ?: "★ N/A"

    val displayPercentage: String
        get() = score?.let { "$it%" } ?: "N/A"

    val cleanDescription: String
        get() = description
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .trim()
            .ifBlank { "No description available." }

    val formattedStatus: String
        get() = status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase(Locale.US) }

    val displayFormat: String
        get() = when (format?.uppercase(Locale.US)) {
            "TV" -> "Series"
            "TV_SHORT" -> "Short"
            "MOVIE" -> "Movie"
            "SPECIAL" -> "Special"
            "OVA" -> "OVA"
            "ONA" -> "ONA"
            else -> format?.replace('_', ' ') ?: "Anime"
        }

    val isMovie: Boolean
        get() = format?.equals("MOVIE", ignoreCase = true) == true || displayFormat.equals("Movie", ignoreCase = true)

    val displayYear: String
        get() = seasonYear?.toString() ?: ""

    val heroBannerUrl: String
        get() = banner ?: cover

    val displaySource: String
        get() = source?.replace('_', ' ')?.lowercase(Locale.US)?.split(" ")?.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        } ?: "Manga"

    val displaySeason: String
        get() = buildString {
            if (!season.isNullOrBlank()) {
                append(season.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) })
                append(" ")
            }
            if (seasonYear != null && seasonYear > 0) {
                append(seasonYear)
            }
        }.trim()

    fun getAiringCountdown(): String? {
        val target = nextAt ?: return null
        val diffSec = target - (System.currentTimeMillis() / 1000)
        if (diffSec <= 0) return null
        val days = diffSec / 86400
        val hours = (diffSec % 86400) / 3600
        val minutes = (diffSec % 3600) / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun getFormattedAiringDate(): String? {
        val target = nextAt ?: return null
        val date = java.util.Date(target * 1000L)
        val sdf = java.text.SimpleDateFormat("EEE, MMM d · h:mm a", Locale.US)
        return sdf.format(date)
    }
}

data class Airing(
    val anime: Anime,
    val episode: Int,
    val at: Long
)

data class Provider(
    val id: String,
    val name: String,
    val pattern: String,
    val serverBadge: String = "HD",
    val isDirectStream: Boolean = false,
    val isEmbedOnly: Boolean = false,
    val isMultiAudio: Boolean = false
) {
    fun url(
        anilistId: Int,
        episode: Int,
        slug: String? = null,
        malId: Int? = null,
        tmdbId: Int? = null,
        isMovie: Boolean = false
    ): String {
        val effectiveMalId = (malId?.takeIf { it > 0 } ?: anilistId).toString()
        val effectiveTmdbId = (tmdbId?.takeIf { it > 0 } ?: anilistId).toString()
        val audioType = if (isDub) "dub" else "sub"
        val audioLang = if (isDub) "dub" else "sub"

        var result = pattern
            .replace("{anilist_id}", anilistId.toString())
            .replace("{id}", anilistId.toString())
            .replace("{mal_id}", effectiveMalId)
            .replace("{id_mal}", effectiveMalId)
            .replace("{tmdb_id}", effectiveTmdbId)
            .replace("{episode}", episode.toString())
            .replace("{ep_num}", episode.toString())
            .replace("{ep}", episode.toString())
            .replace("{e}", episode.toString())
            .replace("{type}", audioType)
            .replace("{lang}", audioLang)
            .replace("{language}", audioLang)
            .replace("{slug}", slug ?: "$anilistId-1x$episode")

        if (isMovie) {
            result = result
                .replace(Regex("""/embed/tv/([^/?#]+)/1/[^/?#]+"""), "/embed/movie/$1")
                .replace(Regex("""cinemaos\.tech/player/([^/?#]+)/1/[^/?#]+"""), "cinemaos.tech/player/$1")
                .replace(Regex("""/tv/([^/?#]+)-1-[^/?#]+"""), "/movie/$1")
                .replace(Regex("""/tv/([^/?#]+)/1/[^/?#]+"""), "/movie/$1")
        }

        return result
    }

    val requiresMalId: Boolean get() = pattern.contains("{mal_id}") || pattern.contains("{id_mal}")
    val requiresTmdbId: Boolean get() = pattern.contains("{tmdb_id}")

    val isDub: Boolean get() = id.contains("dub", ignoreCase = true) && !id.contains("multi", ignoreCase = true)
    val isMulti: Boolean get() = isMultiAudio || id.contains("multi", ignoreCase = true)
}

val DEFAULT_STATIC_PROVIDERS = listOf(
    // === DIRECT SUBTITLE SERVERS (FlixCloud, MegaPlay, Vidstream) ===
    Provider("flixcloud-sub", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true),
    Provider("megaplay-direct-sub", "MegaPlay", "https://anikoto.to", "HD", isDirectStream = true),
    Provider("vidstream-direct-sub", "Vidstream", "https://anikoto.to", "HD", isDirectStream = true),

    // === DIRECT DUBBED SERVERS (FlixCloud, MegaPlay, Vidstream) ===
    Provider("flixcloud-dub", "FlixCloud", "https://reanime.to", "HD", isDirectStream = true),
    Provider("megaplay-direct-dub", "MegaPlay", "https://anikoto.to", "HD", isDirectStream = true),
    Provider("vidstream-direct-dub", "Vidstream", "https://anikoto.to", "HD", isDirectStream = true),

    // === EMBED SERVERS (SUB) ===
    Provider("animesalt-embed-sub", "ACDN Sub", "https://animesalt.cx/episode/{slug}/", "Embed HD", isEmbedOnly = true),
    Provider("zokoanime-sub", "ZokoAnime", "https://zokoanime.video/stream/mal/{mal_id}/{episode}/sub?color=35d5bf", "Embed HD", isEmbedOnly = true),
    Provider("tryembed-sub", "TryEmbed", "https://tryembed.us.cc/embed/anime/mal/{mal_id}/{episode}/sub?skin=silicon&autoSkip=true", "Embed HD", isEmbedOnly = true),
    Provider("megaplay-sub", "Megaplay", "https://megaplay.buzz/stream/mal/{mal_id}/{episode}/sub?autonext=1&autoskip=1", "Embed HD", isEmbedOnly = true),
    Provider("megaplay-s2-sub", "Vidstream", "https://megaplay.buzz/stream/mal/{mal_id}/{episode}/sub?server=vidstream&autonext=1&autoskip=1", "Megaplay S2", isEmbedOnly = true),
    Provider("vidnest-anime-sub", "Vidnest", "https://vidnest.fun/anime/{id}/{e}/sub", "Embed HD", isEmbedOnly = true),
    Provider("recloud-sub", "ReCloud", "https://cdn.4animo.xyz/embed/hd-2/ani/{anilist_id}/{episode}/sub?k=1", "Embed HD", isEmbedOnly = true),
    Provider("yumezone-megaplay-sub", "YumeZone", "https://yumezone.qzz.io/embed/megaplay/ani/{anilist_id}/{episode}/sub", "Embed HD", isEmbedOnly = true),
    // TMDB-based embed servers (anime via TV/Movie TMDB ID)
    Provider("vidzee-sub", "Captain America", "https://player.vidzee.wtf/embed/tv/{tmdb_id}/1/{episode}?lang=en", "TMDB Embed", isEmbedOnly = true),
    Provider("vidlove-sub", "Hawkeye", "https://player.vidlove.cc/embed/tv/{tmdb_id}/1/{episode}", "TMDB Embed", isEmbedOnly = true),
    Provider("1embed-sub", "Spider-Man", "https://1embed.cc/embed/tv/{tmdb_id}/1/{episode}", "TMDB Embed", isEmbedOnly = true),
    Provider("cinemaos-sub", "Scarlet Witch", "https://cinemaos.tech/player/{tmdb_id}/1/{episode}?lang=en", "TMDB Embed", isEmbedOnly = true),
    Provider("2embed-sub", "2Embed", "https://www.2embed.stream/embed/tv/{tmdb_id}/1/{episode}", "TMDB Embed", isEmbedOnly = true),

    // === EMBED SERVERS (DUB) ===
    Provider("animesalt-embed-dub", "ACDN Dub", "https://animesalt.cx/episode/{slug}/", "Embed HD", isEmbedOnly = true),
    Provider("zokoanime-dub", "ZokoAnime Dub", "https://zokoanime.video/stream/mal/{mal_id}/{episode}/dub?color=35d5bf", "Embed HD", isEmbedOnly = true),
    Provider("tryembed-dub", "TryEmbed Dub", "https://tryembed.us.cc/embed/anime/mal/{mal_id}/{episode}/dub?skin=silicon&autoSkip=true", "Embed HD", isEmbedOnly = true),
    Provider("megaplay-dub", "Megaplay Dub", "https://megaplay.buzz/stream/mal/{mal_id}/{episode}/dub?autonext=1&autoskip=1", "Embed HD", isEmbedOnly = true),
    Provider("megaplay-s2-dub", "Vidstream Dub", "https://megaplay.buzz/stream/mal/{mal_id}/{episode}/dub?server=vidstream&autonext=1&autoskip=1", "Megaplay S2", isEmbedOnly = true),
    Provider("vidnest-anime-dub", "Vidnest Dub", "https://vidnest.fun/anime/{id}/{e}/dub", "Embed HD", isEmbedOnly = true),
    Provider("recloud-dub", "ReCloud Dub", "https://cdn.4animo.xyz/embed/hd-2/ani/{anilist_id}/{episode}/dub?k=1", "Embed HD", isEmbedOnly = true),
    Provider("yumezone-megaplay-dub", "YumeZone Dub", "https://yumezone.qzz.io/embed/megaplay/ani/{anilist_id}/{episode}/dub", "Embed HD", isEmbedOnly = true),
    // TMDB-based embed servers (anime via TV/Movie TMDB ID)
    Provider("vidzee-dub", "Captain America Dub", "https://player.vidzee.wtf/embed/tv/{tmdb_id}/1/{episode}?lang=en", "TMDB Dub", isEmbedOnly = true),
    Provider("cinemaos-dub", "Scarlet Witch Dub", "https://cinemaos.tech/player/{tmdb_id}/1/{episode}?lang=en", "TMDB Dub", isEmbedOnly = true),
    Provider("2embed-dub", "2Embed Dub", "https://www.2embed.stream/embed/tv/{tmdb_id}/1/{episode}", "TMDB Embed", isEmbedOnly = true),

    // === EMBED SERVERS (MULTI-AUDIO) ===
    Provider("animesalt-embed-multi", "ACDN Multi", "https://animesalt.cx/episode/{slug}/", "Multi-Audio", isEmbedOnly = true, isMultiAudio = true),
    Provider("animesalt-mystream-multi", "HD-1", "https://animesalt.cx/episode/{slug}/?server=mystream", "Multi-Audio", isEmbedOnly = true, isMultiAudio = true),
    // TMDB-based multi-audio embed servers
    Provider("vidzee-multi", "Captain America", "https://player.vidzee.wtf/embed/tv/{tmdb_id}/1/{episode}?lang=en", "TMDB Multi", isEmbedOnly = true, isMultiAudio = true),
    Provider("vidlove-multi", "Hawkeye", "https://player.vidlove.cc/embed/tv/{tmdb_id}/1/{episode}", "TMDB Multi", isEmbedOnly = true, isMultiAudio = true),
    Provider("1embed-multi", "Spider-Man", "https://1embed.cc/embed/tv/{tmdb_id}/1/{episode}", "TMDB Multi", isEmbedOnly = true, isMultiAudio = true),
    Provider("cinemaos-multi", "Scarlet Witch", "https://cinemaos.tech/player/{tmdb_id}/1/{episode}?lang=en", "TMDB Multi", isEmbedOnly = true, isMultiAudio = true)
)

val ALL_PROVIDERS: List<Provider>
    get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentProviders

val SUB_PROVIDERS: List<Provider>
    get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentSubProviders

val DUB_PROVIDERS: List<Provider>
    get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentDubProviders

val MULTI_PROVIDERS: List<Provider>
    get() = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentMultiProviders

val GENRES_LIST = listOf("All", "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological", "Romance", "Rom-Com", "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller", "Isekai", "Demons", "Magic", "School", "Harem", "Martial Arts", "Vampire")

val STATUS_LIST = listOf("All", "Airing", "Finished", "Not Yet Released")
val SORT_LIST = listOf("Popular", "Trending", "Top Rated", "Recently Updated", "Newest")

enum class WatchlistStatus(val id: String, val label: String) {
    ALL("all", "All"),
    WATCHING("watching", "Watching"),
    PLAN_TO_WATCH("plan_to_watch", "Plan to Watch"),
    COMPLETED("completed", "Completed"),
    DROPPED("dropped", "Dropped"),
    REWATCHING("rewatching", "Rewatching");

    companion object {
        val FILTERS = listOf(ALL, WATCHING, PLAN_TO_WATCH, COMPLETED, DROPPED, REWATCHING)
        val ASSIGNABLE = listOf(WATCHING, PLAN_TO_WATCH, COMPLETED, DROPPED, REWATCHING)

        fun fromString(value: String?): WatchlistStatus {
            return when (value?.trim()?.lowercase()?.replace(" ", "_")?.replace("-", "_")) {
                "watching" -> WATCHING
                "plan_to_watch", "plantowatch" -> PLAN_TO_WATCH
                "completed" -> COMPLETED
                "dropped" -> DROPPED
                "rewatching" -> REWATCHING
                else -> PLAN_TO_WATCH
            }
        }
    }
}

data class WatchlistItem(
    val animeId: Int,
    val title: String,
    val cover: String,
    val banner: String? = null,
    val score: Int? = null,
    val genres: List<String> = emptyList(),
    val episodes: Int? = null,
    val status: String = "plan_to_watch",
    val progressEpisode: Int = 0,
    val releaseYear: String? = null,
    val customTags: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val watchlistStatus: WatchlistStatus
        get() = WatchlistStatus.fromString(status)
}

data class WatchHistoryItem(
    val animeId: Int,
    val title: String,
    val cover: String,
    val banner: String? = null,
    val lastEpisode: Int,
    val totalEpisodes: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Unknown Anime"
}

data class UserProfile(
    val id: String? = null,
    val email: String? = null,
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val displayName: String = "Guest",
    val username: String = "",
    val bio: String = "",
    val bioFont: String = "default", // default, serif, monospace, cursive, shonen
    val favoriteAnime: String = "",
    val otakuRank: String = "Genin",
    val isAdmin: Boolean = false,
    val role: String? = "USER",
    val roles: List<String> = emptyList(),
    val customTag: String? = null,
    val avatarDecorationId: String? = null, // Avatar Overlay (animated design)
    val avatarFrameId: String? = null,      // Circular 3D Avatar Frame
    val textEffectId: String? = null,       // Username Text Effect (e.g. RGB Glitch, Shimmer, Aurora)
    val profileEffectId: String? = null,
    val bannerThemeId: String? = null,
    val nameplateId: String? = null,
    val nameplateEmoji: String? = null,
    val exp: Long = 0L,
    val level: Int = 1,
    val streakCount: Int = 1,
    val longestStreak: Int = 1,
    val lastActiveDate: String? = null,
    val bannerEffectId: String = "none",
    val timeSpentAnime: Long = 0L,
    val timeSpentManga: Long = 0L,
    val timeSpentNovel: Long = 0L,
    val timeSpentApp: Long = 0L,
    val timeSpentTotal: Long = 0L,
    val currentActivity: String? = null,
    val currentActivityType: String? = null,
    val currentActivityTargetId: String? = null,
    val lastSeenAt: String? = null,
    val isOnline: Boolean? = false,
    val isWatchlistPrivate: Boolean = false,
    val isWatchHistoryPrivate: Boolean = false,
    val isMangaPrivate: Boolean = false,
    val isNovelPrivate: Boolean = false
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class SkipInterval(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val skipType: String
)

data class WelcomeNotificationConfig(
    val title: String = "Welcome to Zenkai",
    val message: String = "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey",
    val imageUrl: String = "",
    val actionLabel: String = "Claim +100 Starter EXP",
    val actionUrl: String = "zenkai://rewards/welcome"
)


