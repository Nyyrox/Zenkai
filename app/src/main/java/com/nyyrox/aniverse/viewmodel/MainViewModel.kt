package com.nyyrox.aniverse.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyyrox.aniverse.data.Airing
import com.nyyrox.aniverse.data.AniListRepository
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.data.UserProfile
import com.nyyrox.aniverse.data.WatchHistoryItem
import com.nyyrox.aniverse.data.WatchlistItem
import com.nyyrox.aniverse.data.WatchlistStatus
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.data.manga.MangaCard
import com.nyyrox.aniverse.data.manga.MangaHistoryItem
import com.nyyrox.aniverse.data.manga.MangaHome
import com.nyyrox.aniverse.data.manga.MangaRepository
import com.nyyrox.aniverse.ui.theme.AnimeTheme
import com.nyyrox.aniverse.ui.theme.ThemeMode
import com.nyyrox.aniverse.ui.theme.ThemeModeState
import com.nyyrox.aniverse.ui.theme.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.data.supabase.CloudHistoryItem
import com.nyyrox.aniverse.data.supabase.CloudProfile
import com.nyyrox.aniverse.data.supabase.CloudWatchlistItem
import com.nyyrox.aniverse.data.supabase.SupabaseRepository
import com.nyyrox.aniverse.data.supabase.AppNotification
import com.nyyrox.aniverse.data.supabase.AdminUserSearchResult
import com.nyyrox.aniverse.notifications.ZenkaiNotificationWorker
import com.nyyrox.aniverse.notifications.ZenkaiNotificationHelper
import com.nyyrox.aniverse.data.CurrentActivityMode
import com.nyyrox.aniverse.data.TimeSpentRecord
import com.nyyrox.aniverse.data.ActivityTimeStats
import com.nyyrox.aniverse.data.TimeSpentExpSystem
import com.nyyrox.aniverse.data.tracker.AniListAccount
import com.nyyrox.aniverse.data.tracker.AniListTrackerClient
import com.nyyrox.aniverse.data.tracker.MalAccount
import com.nyyrox.aniverse.data.tracker.MalTrackerClient
import com.nyyrox.aniverse.data.tracker.TrackerSettings
import com.nyyrox.aniverse.data.tracker.TrackerSyncManager
import com.nyyrox.aniverse.data.tracker.TrackerSyncResult

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AniListRepository()
    private val storage = StorageManager(application.applicationContext)
    // Light/dark choice is saved separately so it always survives restarts.
    private val themePrefs = application.getSharedPreferences("aniverse_theme_mode", Context.MODE_PRIVATE)
    private fun loadThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(themePrefs.getString("mode", null) ?: "DARK") }.getOrDefault(ThemeMode.DARK)
    
    private val supabaseRepo = SupabaseRepository()
    
    // Auth state
    var authState by mutableStateOf<AuthState>(AuthState.LoggedOut); private set
    var authError by mutableStateOf<String?>(null); private set
    var authLoading by mutableStateOf(false); private set
    var cloudProfile by mutableStateOf<CloudProfile?>(null); private set
    var isUsernameSetupRequired by mutableStateOf(false); private set

    var userProfile by mutableStateOf(storage.getUserProfile()); private set
    var themeSettings by mutableStateOf(
        storage.getThemeSettings().copy(mode = loadThemeMode().also { ThemeModeState.mode = it })
    ); private set
    var preferredServerId by mutableStateOf(storage.getPreferredServerId()); private set

    // Dynamic Providers & Server Configurations (from Supabase)
    var allProviders by mutableStateOf(com.nyyrox.aniverse.data.config.DynamicConfigManager.currentProviders); private set
    var subProviders by mutableStateOf(com.nyyrox.aniverse.data.config.DynamicConfigManager.currentSubProviders); private set
    var dubProviders by mutableStateOf(com.nyyrox.aniverse.data.config.DynamicConfigManager.currentDubProviders); private set
    var multiProviders by mutableStateOf(com.nyyrox.aniverse.data.config.DynamicConfigManager.currentMultiProviders); private set

    fun refreshStreamingServersAndConfigs() {
        viewModelScope.launch {
            try {
                val servers = supabaseRepo.fetchStreamingServers()
                if (servers.isNotEmpty()) {
                    com.nyyrox.aniverse.data.config.DynamicConfigManager.updateProviders(servers)
                    allProviders = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentProviders
                    subProviders = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentSubProviders
                    dubProviders = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentDubProviders
                    multiProviders = com.nyyrox.aniverse.data.config.DynamicConfigManager.currentMultiProviders
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val configs = supabaseRepo.fetchWorkerConfigs()
                if (configs.isNotEmpty()) {
                    com.nyyrox.aniverse.data.config.DynamicConfigManager.updateWorkerConfigs(configs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Tracking Services State (AniList & MyAnimeList)
    var aniListAccount by mutableStateOf(storage.getAniListAccount()); private set
    var malAccount by mutableStateOf(storage.getMalAccount()); private set
    var trackerSettings by mutableStateOf(storage.getTrackerSettings()); private set
    var isTrackerSyncing by mutableStateOf(false); private set
    var trackerSyncMessage by mutableStateOf<String?>(null); private set

    // Community Feed Data
    var communityMessages by mutableStateOf<List<com.nyyrox.aniverse.data.supabase.CommunityMessage>>(emptyList()); private set
    var communityProfilesMap by mutableStateOf<Map<String, CloudProfile>>(emptyMap()); private set
    var userReactionsMap by mutableStateOf<Map<String, Set<String>>>(emptyMap()); private set
    var isCommunityLoading by mutableStateOf(false); private set
    var isSendingCommunityMessage by mutableStateOf(false); private set

    // Notifications State
    var notifications by mutableStateOf<List<AppNotification>>(emptyList()); private set
    var isNotificationsLoading by mutableStateOf(false); private set
    var adminSearchResults by mutableStateOf<List<AdminUserSearchResult>>(emptyList()); private set
    var isAdminSearching by mutableStateOf(false); private set
    var isSendingAdminNotification by mutableStateOf(false); private set
    var welcomeConfig by mutableStateOf(storage.getWelcomeNotificationConfig())
    var isUpdatingUserRoles by mutableStateOf(false); private set

    // Social Hub, Friends & Direct Messaging State
    var socialUserSearchResults by mutableStateOf<List<CloudProfile>>(emptyList()); private set
    var isSocialUserSearching by mutableStateOf(false); private set
    var activeOnlineUsers by mutableStateOf<List<CloudProfile>>(emptyList()); private set
    var isActiveUsersLoading by mutableStateOf(false); private set
    var recentDmPartners by mutableStateOf<List<CloudProfile>>(emptyList()); private set
    var isRecentDmsLoading by mutableStateOf(false); private set
    var directChatMessages by mutableStateOf<List<com.nyyrox.aniverse.data.supabase.CommunityMessage>>(emptyList()); private set
    var isDirectChatLoading by mutableStateOf(false); private set
    var isSendingDirectMessage by mutableStateOf(false); private set
    var activeDmPartnerProfile by mutableStateOf<CloudProfile?>(null); private set

    // Watch Time Rankings & Leaderboard State
    var watchTimeRankingTimeframe by mutableStateOf("weekly"); private set
    var watchTimeRankings by mutableStateOf<List<CloudProfile>>(emptyList()); private set
    var isWatchTimeRankingLoading by mutableStateOf(false); private set

    // Watch Together Real-time State
    var activeWatchRoom by mutableStateOf<com.nyyrox.aniverse.data.supabase.WatchTogetherRoom?>(null); private set
    var activeWatchRoomsList by mutableStateOf<List<com.nyyrox.aniverse.data.supabase.WatchTogetherRoom>>(emptyList()); private set
    var isWatchRoomLoading by mutableStateOf(false); private set
    var isCreatingWatchRoom by mutableStateOf(false); private set
    var watchRoomChatMessages by mutableStateOf<List<com.nyyrox.aniverse.data.supabase.CommunityMessage>>(emptyList()); private set

    // Live Activity Presence
    var myCurrentActivity by mutableStateOf<String?>(null); private set
    var myCurrentActivityType by mutableStateOf<String?>(null); private set
    var myCurrentActivityTargetId by mutableStateOf<String?>(null); private set

    val unreadNotificationsCount: Int
        get() {
            val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: userProfile.id
            return notifications.count { notif ->
                !notif.isRead && (notif.userId.isNullOrBlank() || notif.userId == currentUserId)
            }
        }

    private val _isAutoPlayNext = mutableStateOf(storage.isAutoPlayNext())
    val isAutoPlayNext: Boolean get() = _isAutoPlayNext.value

    private val _isAutoSkipIntroOutro = mutableStateOf(storage.isAutoSkipIntroOutro())
    val isAutoSkipIntroOutro: Boolean get() = _isAutoSkipIntroOutro.value

    private val _isPlayEmbed = mutableStateOf(false)
    val isPlayEmbed: Boolean get() = _isPlayEmbed.value

    // Persistent Navigation Scroll States
    val homeLazyListState = LazyListState()
    val mangaLazyListState = LazyListState()
    val novelLazyListState = LazyListState()

    // In-Place Detail Overlay States (combines feed + detail seamlessly)
    var activeDetailAnimeId by mutableStateOf<Int?>(null)
    var activeDetailMangaId by mutableStateOf<String?>(null)
    var activeDetailNovelSlug by mutableStateOf<String?>(null)

    var pendingMangaTitleHint: String? = null
    fun openAnimeDetail(id: Int) {
        com.nyyrox.aniverse.util.AppHaptics.tick(getApplication())
        activeDetailAnimeId = id
        openDetail(id)
    }
    fun closeAnimeDetail() { activeDetailAnimeId = null }
    fun openMangaDetail(id: String, titleHint: String? = null) {
        com.nyyrox.aniverse.util.AppHaptics.tick(getApplication())
        pendingMangaTitleHint = titleHint
        activeDetailMangaId = id
    }
    fun closeMangaDetail() {
        pendingMangaTitleHint = null
        activeDetailMangaId = null
    }
    fun openNovelDetail(slug: String) {
        com.nyyrox.aniverse.util.AppHaptics.tick(getApplication())
        activeDetailNovelSlug = slug
        loadNovelDetail(slug)
    }
    fun closeNovelDetail() { activeDetailNovelSlug = null }

    // Manga Data
    var activeMangaSource by mutableStateOf(com.nyyrox.aniverse.data.manga.MangaSource.COMICK); private set
    var mangaHome by mutableStateOf(MangaHome()); private set
    var mangaHomeLoading by mutableStateOf(false); private set
    var mangaHomeError by mutableStateOf<String?>(null); private set
    var mangaSearchResults by mutableStateOf<List<MangaCard>>(emptyList()); private set
    var mangaSearchLoading by mutableStateOf(false); private set
    var mangaBookmarks by mutableStateOf<List<MangaBookmarkItem>>(emptyList()); private set
    var mangaHistory by mutableStateOf<List<MangaHistoryItem>>(emptyList()); private set
    private var mangaSearchJob: Job? = null

    // Watchtime EXP: tracks the last video positionMs at which EXP was awarded, keyed by animeId.
    // EXP is given once per 60,000ms (1 minute) of actual playback progress — not per recordHistory call.
    private val watchtimeExpTrackerMs = mutableMapOf<Int, Long>()

    // Novel Screen Data & Multi-Source Unified Repository
    val novelRepo = com.nyyrox.aniverse.data.novel.UnifiedNovelRepository()
    val downloadManager = com.nyyrox.aniverse.data.download.DownloadManager(application)
    var activeNovelSource by mutableStateOf(com.nyyrox.aniverse.data.novel.NovelSource.NOVEL_FULL); private set
    var novelRankings by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelItem>>(emptyList()); private set
    var novelPopular by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelItem>>(emptyList()); private set
    var novelLatest by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelItem>>(emptyList()); private set
    var novelLightNovels by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelItem>>(emptyList()); private set
    var novelHomeLoading by mutableStateOf(false); private set
    var novelHomeError by mutableStateOf<String?>(null); private set
    var selectedNovelGenre by mutableStateOf<String?>("All")

    var novelSearchResults by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelItem>>(emptyList()); private set
    var novelSearchLoading by mutableStateOf(false); private set
    private var novelSearchJob: Job? = null

    var selectedNovel by mutableStateOf<com.nyyrox.aniverse.data.novel.NovelDetail?>(null); private set
    var novelDetailLoading by mutableStateOf(false); private set
    var novelDetailError by mutableStateOf<String?>(null); private set

    var novelChapters by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelChapter>>(emptyList()); private set
    var novelChaptersPage by mutableStateOf(1); private set
    var novelChaptersMaxPage by mutableStateOf(1); private set
    var novelChaptersLoading by mutableStateOf(false); private set

    var activeChapterContent by mutableStateOf<com.nyyrox.aniverse.data.novel.NovelChapterContent?>(null); private set
    var chapterContentLoading by mutableStateOf(false); private set
    var chapterContentError by mutableStateOf<String?>(null); private set

    var novelReaderSettings by mutableStateOf(com.nyyrox.aniverse.data.novel.NovelReaderSettings())
    var novelBookmarks by mutableStateOf<Set<String>>(emptySet()); private set
    var novelHistory by mutableStateOf<Map<String, String>>(emptyMap()); private set // novelSlug -> lastChapterSlug
    var novelHistoryList by mutableStateOf<List<com.nyyrox.aniverse.data.novel.NovelHistoryItem>>(emptyList()); private set

    // Home Screen Data
    var trending by mutableStateOf<List<Anime>>(emptyList()); private set
    var popular by mutableStateOf<List<Anime>>(emptyList()); private set
    var topRated by mutableStateOf<List<Anime>>(emptyList()); private set
    var actionAnime by mutableStateOf<List<Anime>>(emptyList()); private set
    var fantasyAnime by mutableStateOf<List<Anime>>(emptyList()); private set
    var newReleases by mutableStateOf<List<Anime>>(emptyList()); private set
    var upcomingAnime by mutableStateOf<List<Anime>>(emptyList()); private set
    var scifiAnime by mutableStateOf<List<Anime>>(emptyList()); private set
    var romanceAnime by mutableStateOf<List<Anime>>(emptyList()); private set
    var homeLoading by mutableStateOf(true); private set
    var homeError by mutableStateOf<String?>(null); private set

    // Browse Screen Data
    var browse by mutableStateOf<List<Anime>>(emptyList()); private set
    var browsePage by mutableStateOf(1); private set
    var browseLoading by mutableStateOf(false); private set
    var browseError by mutableStateOf<String?>(null); private set
    private var browseJob: Job? = null

    // Home Infinite Explore Data
    var homeExplore by mutableStateOf<List<Anime>>(emptyList()); private set
    var homeExplorePage by mutableStateOf(1); private set
    var homeExploreLoading by mutableStateOf(false); private set
    var homeExploreHasMore by mutableStateOf(true); private set

    // Airing / Schedule Data
    var airing by mutableStateOf<List<Airing>>(emptyList()); private set
    var airingLoading by mutableStateOf(true); private set
    var airingError by mutableStateOf<String?>(null); private set

    // Detail Screen Data
    var selected by mutableStateOf<Anime?>(null); private set
    var detailLoading by mutableStateOf(false); private set
    var detailError by mutableStateOf<String?>(null); private set

    // Watchlist & History Data
    var watchlist by mutableStateOf<List<WatchlistItem>>(emptyList()); private set
    var history by mutableStateOf<List<WatchHistoryItem>>(emptyList()); private set

    // TMDB Artwork Cache (Backdrops & Title Logo PNGs)
    var tmdbArtworks by mutableStateOf<Map<Int, com.nyyrox.aniverse.data.tmdb.TmdbArtwork>>(emptyMap()); private set
    var tmdbEpisodeStills by mutableStateOf<Map<String, String?>>(emptyMap()); private set

    // Temporary memory cache for Manga details & chapters
    data class CachedMangaDetail(
        val resolvedId: String,
        val info: com.nyyrox.aniverse.data.manga.MangaInfo,
        val chapters: List<com.nyyrox.aniverse.data.manga.MangaChapter>
    )
    val mangaDetailCache = mutableMapOf<String, CachedMangaDetail>()

    // Temporary memory cache for Novel details & chapters
    data class CachedNovelDetail(
        val detail: com.nyyrox.aniverse.data.novel.NovelDetail,
        val chapters: List<com.nyyrox.aniverse.data.novel.NovelChapter>,
        val maxPage: Int
    )
    val novelDetailCache = mutableMapOf<String, CachedNovelDetail>()

    // Time Spent Activity Tracking & App Lifecycle
    var timeSpentRecord by mutableStateOf(storage.getTimeSpentRecord(storage.getUserProfile().id)); private set
    var currentActivityMode by mutableStateOf(CurrentActivityMode.EXPLORING_APP); private set
    var isAppInForeground by mutableStateOf(true); private set

    fun setAppForeground(isInForeground: Boolean) {
        if (isAppInForeground == isInForeground) return
        isAppInForeground = isInForeground
        if (!isInForeground) {
            syncDailyToCloudIfDue()
        }
    }

    fun setActivityMode(mode: CurrentActivityMode) {
        currentActivityMode = mode
        if (mode == CurrentActivityMode.EXPLORING_APP) {
            myCurrentActivity = null
            myCurrentActivityType = null
            myCurrentActivityTargetId = null
            updateCurrentActivity(null, null, null)
        }
    }

    init {
        com.styropyr0.prismal.PrismalGlass.isGlassmorphismEnabled = themeSettings.isGlassmorphismEnabled
        com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.init(application.applicationContext)
        com.nyyrox.aniverse.data.AvatarBannerRepository.init(application.applicationContext)
        viewModelScope.launch {
            com.nyyrox.aniverse.data.mapping.AnimeMappingRepository.syncRemoteMappings()
        }
        viewModelScope.launch {
            try {
                val savedSourceId = com.nyyrox.aniverse.util.UserPreferences.getActiveNovelSource(application.applicationContext).first()
                val savedSource = com.nyyrox.aniverse.data.novel.NovelSource.fromId(savedSourceId)
                if (activeNovelSource != savedSource) {
                    activeNovelSource = savedSource
                    novelRepo.setSource(savedSource)
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val savedMangaSourceId = com.nyyrox.aniverse.util.UserPreferences.getActiveMangaSource(application.applicationContext).first()
                val savedMangaSource = com.nyyrox.aniverse.data.manga.MangaSource.fromId(savedMangaSourceId)
                if (activeMangaSource != savedMangaSource) {
                    activeMangaSource = savedMangaSource
                    com.nyyrox.aniverse.data.manga.MangaRepository.setSource(savedMangaSource)
                }
            } catch (_: Exception) {}
        }
        refreshStorageData()
        loadInitial()
        refreshStreamingServersAndConfigs()
        // Check for existing Supabase session
        checkExistingSession()
        loadNotifications()

        // Background notification worker (even if app is closed)
        try {
            ZenkaiNotificationWorker.enqueuePeriodicWork(application.applicationContext)
        } catch (_: Exception) {}

        // Active Time Spent & EXP Ticker (runs every 10 seconds)
        viewModelScope.launch {
            var tickerCount = 0
            while (true) {
                delay(10000L)

                // STRICT: Never count time, grant EXP, or broadcast active state while app is in background
                if (!isAppInForeground) {
                    continue
                }

                tickerCount++

                // Record 10s into active category for currently active user (or guest)
                val currentUid = (authState as? AuthState.LoggedIn)?.userId
                val updated = storage.getTimeSpentRecord(currentUid).addElapsed(currentActivityMode, 10L)
                storage.saveTimeSpentRecord(updated, currentUid)
                timeSpentRecord = updated

                // Keep local userProfile in sync
                userProfile = userProfile.copy(
                    timeSpentAnime = updated.allTime.animeSeconds,
                    timeSpentManga = updated.allTime.mangaSeconds,
                    timeSpentNovel = updated.allTime.novelSeconds,
                    timeSpentApp = updated.allTime.appSeconds,
                    timeSpentTotal = updated.allTime.totalSeconds
                )

                // Every 60 seconds (6 ticks), grant active EXP based on activity mode
                if (tickerCount % 6 == 0 && authState is AuthState.LoggedIn) {
                    val expToGrant = when (currentActivityMode) {
                        CurrentActivityMode.WATCHING_ANIME -> 20L
                        CurrentActivityMode.READING_MANGA -> 15L
                        CurrentActivityMode.READING_NOVEL -> 15L
                        CurrentActivityMode.EXPLORING_APP -> 4L
                    }
                    val reason = when (currentActivityMode) {
                        CurrentActivityMode.WATCHING_ANIME -> "Watching Anime"
                        CurrentActivityMode.READING_MANGA -> "Reading Manga"
                        CurrentActivityMode.READING_NOVEL -> "Reading Novel"
                        CurrentActivityMode.EXPLORING_APP -> "Active App Time"
                    }
                    addExp(expToGrant, reason)
                }

                // Every 2-3 minutes (15 ticks = 150s), poll for new notifications and post to status bar
                if (tickerCount % 15 == 0) {
                    loadNotifications(isSilent = true)
                }

                // Daily cloud sync check (runs periodically, syncs once every 24 hours)
                if (tickerCount % 60 == 0 && authState is AuthState.LoggedIn) {
                    syncDailyToCloudIfDue()
                }
            }
        }
    }

    fun updatePresenceHeartbeat() {
        // Disabled: Presence and activity status tracking removed per user request
    }

    private var isInitialLoaded = false

    fun loadTmdbArtwork(anime: Anime) {
        if (tmdbArtworks.containsKey(anime.id)) return
        viewModelScope.launch {
            try {
                val artwork = withContext(Dispatchers.IO) {
                    com.nyyrox.aniverse.data.tmdb.TmdbRepository.getArtwork(
                        anilistId = anime.id,
                        title = anime.title,
                        year = anime.seasonYear,
                        format = anime.format
                    )
                }
                tmdbArtworks = tmdbArtworks + (anime.id to (artwork ?: com.nyyrox.aniverse.data.tmdb.TmdbArtwork(0, "", null, null)))
            } catch (_: Exception) {
                tmdbArtworks = tmdbArtworks + (anime.id to com.nyyrox.aniverse.data.tmdb.TmdbArtwork(0, "", null, null))
            }
        }
    }

    fun loadTmdbEpisodeStill(anime: Anime, episodeNumber: Int) {
        val key = "${anime.id}_$episodeNumber"
        if (tmdbEpisodeStills.containsKey(key)) return
        viewModelScope.launch {
            try {
                val still = withContext(Dispatchers.IO) {
                    val season = com.nyyrox.aniverse.data.mapping.AnimeSeasonMapper.resolveSeason(anime.title, anime.romajiTitle)
                    val mapped = com.nyyrox.aniverse.data.tmdb.TmdbRepository.fetchEpisodesMapped(
                        anilistId = anime.id,
                        title = anime.title,
                        romajiTitle = anime.romajiTitle,
                        season = season,
                        totalEpisodes = anime.episodes ?: 24,
                        format = anime.format
                    )
                    mapped[episodeNumber]?.stillUrl?.takeIf { it.isNotBlank() }
                        ?: mapped[1]?.stillUrl?.takeIf { it.isNotBlank() }
                        ?: tmdbArtworks[anime.id]?.backdropUrl?.takeIf { it.isNotBlank() }
                        ?: anime.banner?.takeIf { it.isNotBlank() }
                        ?: anime.cover
                }
                val finalStill = still?.takeIf { it.isNotBlank() }
                    ?: anime.banner?.takeIf { it.isNotBlank() }
                    ?: anime.cover
                tmdbEpisodeStills = tmdbEpisodeStills + (key to finalStill)
                if (!finalStill.isNullOrBlank()) {
                    updateHistoryEpisodeStill(anime.id, episodeNumber, finalStill)
                }
            } catch (_: Exception) {
                val fallbackStill = anime.banner?.takeIf { it.isNotBlank() } ?: anime.cover
                tmdbEpisodeStills = tmdbEpisodeStills + (key to fallbackStill)
            }
        }
    }

    fun loadHistoryEpisodeStill(animeId: Int, episode: Int) {
        val key = "${animeId}_$episode"
        if (tmdbEpisodeStills.containsKey(key)) return
        val known = findKnownAnime(animeId) ?: return
        loadTmdbEpisodeStill(known, episode)
    }

    fun updateHistoryEpisodeStill(animeId: Int, episode: Int, stillUrl: String) {
        if (stillUrl.isBlank()) return
        storage.updateHistoryEpisodeStill(animeId, episode, stillUrl)
        history = storage.getHistory()
    }

    suspend fun getMultiAudioServers(anilistId: Int, episode: Int, title: String): List<com.nyyrox.aniverse.data.extractor.MultiAudioServer> {
        return com.nyyrox.aniverse.data.extractor.MultiAudioExtractor.extractServers(anilistId, episode, title)
    }

    fun refreshStorageData() {
        watchlist = storage.getWatchlist()
        history = storage.getHistory()
        mangaBookmarks = storage.getMangaBookmarks()
        mangaHistory = storage.getMangaHistory()
        novelHistoryList = storage.getNovelHistory()
        novelHistory = novelHistoryList.associate { it.slug to it.lastChapterSlug }
        val loadedProfile = storage.getUserProfile()

        val streakEval = com.nyyrox.aniverse.data.LevelAndStreakSystem.evaluateDailyStreak(
            lastActiveDateStr = loadedProfile.lastActiveDate,
            currentStreak = loadedProfile.streakCount,
            longestStreak = loadedProfile.longestStreak
        )
        val newExp = if (streakEval.isNewDay) loadedProfile.exp + streakEval.earnedExp else loadedProfile.exp
        val newLevel = com.nyyrox.aniverse.data.LevelAndStreakSystem.calculateLevel(newExp)
        val defaultRank = com.nyyrox.aniverse.data.LevelAndStreakSystem.getDefaultOtakuRank(newLevel)
        val finalRank = if (loadedProfile.otakuRank == "Genin" || loadedProfile.otakuRank == "Novice" || loadedProfile.otakuRank.isBlank()) {
            defaultRank
        } else {
            loadedProfile.otakuRank
        }
        val updatedProfile = loadedProfile.copy(
            exp = newExp,
            level = newLevel,
            otakuRank = finalRank,
            streakCount = streakEval.newStreak,
            longestStreak = streakEval.newLongest,
            lastActiveDate = streakEval.todayStr
        )
        if (streakEval.isNewDay) {
            storage.saveUserProfile(updatedProfile)
        }
        userProfile = updatedProfile
        themeSettings = storage.getThemeSettings().copy(mode = ThemeModeState.mode)
        preferredServerId = storage.getPreferredServerId()
    }

    fun loadInitial() {
        if (isInitialLoaded) return
        isInitialLoaded = true
        loadHome()
        loadAiring()
    }

    fun loadHome() = viewModelScope.launch {
        homeLoading = true
        homeError = null
        var usedTmdbFallback = false

        try {
            supervisorScope {
                // ── Tier 1: AniList direct ──
                val trendingDeferred = async(Dispatchers.IO) {
                    try { repo.list("TRENDING_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                }
                val popularDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                }
                val topRatedDeferred = async(Dispatchers.IO) {
                    try { repo.list("SCORE_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                }
                val actionDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15, genre = "Action") } catch (_: Exception) { emptyList() }
                }
                val fantasyDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15, genre = "Fantasy") } catch (_: Exception) { emptyList() }
                }
                val newReleasesDeferred = async(Dispatchers.IO) {
                    try { repo.list("START_DATE_DESC", perPage = 15, status = "RELEASING") } catch (_: Exception) { emptyList() }
                }
                val upcomingDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15, status = "NOT_YET_RELEASED") } catch (_: Exception) { emptyList() }
                }
                val scifiDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15, genre = "Sci-Fi") } catch (_: Exception) { emptyList() }
                }
                val romanceDeferred = async(Dispatchers.IO) {
                    try { repo.list("POPULARITY_DESC", perPage = 15, genre = "Romance") } catch (_: Exception) { emptyList() }
                }

                val t  = trendingDeferred.await()
                val p  = popularDeferred.await()
                val tr = topRatedDeferred.await()
                val a  = actionDeferred.await()
                val f  = fantasyDeferred.await()
                val nr = newReleasesDeferred.await()
                val up = upcomingDeferred.await()
                val sf = scifiDeferred.await()
                val ro = romanceDeferred.await()

                if (t.isNotEmpty())  trending     = t
                if (p.isNotEmpty())  popular      = p
                if (tr.isNotEmpty()) topRated     = tr
                if (a.isNotEmpty())  actionAnime  = a
                if (f.isNotEmpty())  fantasyAnime = f
                if (nr.isNotEmpty()) newReleases  = nr
                if (up.isNotEmpty()) upcomingAnime = up
                if (sf.isNotEmpty()) scifiAnime   = sf
                if (ro.isNotEmpty()) romanceAnime = ro

                // ── Tier 2: TMDB fallback via aniversee.vercel.app ──
                if (trending.isEmpty() && popular.isEmpty()) {
                    usedTmdbFallback = true
                    val tmdbTrendingDeferred = async(Dispatchers.IO) {
                        try { repo.listFromTmdb("TRENDING_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                    }
                    val tmdbPopularDeferred = async(Dispatchers.IO) {
                        try { repo.listFromTmdb("POPULARITY_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                    }
                    val tmdbTopRatedDeferred = async(Dispatchers.IO) {
                        try { repo.listFromTmdb("SCORE_DESC", perPage = 15) } catch (_: Exception) { emptyList() }
                    }

                    val tt  = tmdbTrendingDeferred.await()
                    val tp  = tmdbPopularDeferred.await()
                    val ttr = tmdbTopRatedDeferred.await()

                    if (tt.isNotEmpty())  trending  = tt
                    if (tp.isNotEmpty())  popular   = tp
                    if (ttr.isNotEmpty()) topRated  = ttr
                }
            }

            when {
                trending.isEmpty() && popular.isEmpty() ->
                    homeError = "No internet connection. Check your network and retry."
                usedTmdbFallback ->
                    homeError = "AniList is unavailable — showing backup data via TMDB"
                else -> homeError = null
            }

            trending.take(6).forEach { loadTmdbArtwork(it) }
            if (mangaHome.trending.isEmpty() && mangaHome.latest.isEmpty()) {
                loadMangaHome()
            }
            if (novelPopular.isEmpty() && novelRankings.isEmpty()) {
                loadNovelHome()
            }
        } catch (e: Exception) {
            if (trending.isEmpty()) {
                homeError = e.message?.take(120)
                    ?: "Something went wrong. Please check your connection and retry."
            }
        } finally {
            homeLoading = false
        }
    }

    fun loadMoreHomeExplore() {
        if (homeExploreLoading || !homeExploreHasMore) return
        viewModelScope.launch {
            homeExploreLoading = true
            try {
                val nextPage = homeExplorePage + 1
                val more = repo.list("TRENDING_DESC", page = nextPage, perPage = 20)
                if (more.isEmpty()) {
                    homeExploreHasMore = false
                } else {
                    homeExplore = homeExplore + more
                    homeExplorePage = nextPage
                }
            } catch (_: Exception) {
            } finally {
                homeExploreLoading = false
            }
        }
    }

    fun loadBrowse(
        page: Int = 1,
        search: String = "",
        genre: String? = null,
        status: String? = null,
        sort: String = "Popular",
        year: Int? = null,
        format: String? = null,
        isTmdbSource: Boolean = false
    ) {
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            if (search.isNotBlank()) delay(300)
            browseLoading = true
            browseError = null
            browsePage = page

            val apiSort = when (sort) {
                "Trending" -> "TRENDING_DESC"
                "Top Rated" -> "SCORE_DESC"
                "Recently Updated" -> "UPDATED_AT_DESC"
                "Newest" -> "START_DATE_DESC"
                else -> "POPULARITY_DESC"
            }

            try {
                val finalResult = if (isTmdbSource) {
                    try {
                        repo.listFromTmdb(
                            sort = apiSort,
                            page = page,
                            search = search,
                            genre = genre,
                            year = year,
                            format = format
                        )
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    val result = try {
                        repo.list(
                            sort = apiSort,
                            page = page,
                            search = search,
                            genre = genre,
                            status = status,
                            seasonYear = year,
                            format = format
                        )
                    } catch (_: Exception) {
                        emptyList()
                    }

                    if (result.isEmpty()) {
                        try {
                            repo.listFromTmdb(
                                sort = apiSort,
                                page = page,
                                search = search,
                                genre = genre,
                                year = year,
                                format = format
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } else result
                }

                browse = if (page == 1) finalResult else browse + finalResult
                if (browse.isEmpty()) {
                    browseError = "No anime found. Try adjusting your search or filters."
                }
            } catch (e: Exception) {
                browseError = e.message ?: "Could not load anime."
            } finally {
                browseLoading = false
            }
        }
    }

    fun loadAiring(forceRefresh: Boolean = false) = viewModelScope.launch {
        if (!forceRefresh && airing.isNotEmpty()) return@launch
        airingLoading = true
        airingError = null
        try {
            airing = withContext(Dispatchers.IO) {
                try {
                    repo.airing()
                } catch (_: Exception) {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            airingError = e.message ?: "Could not load airing anime schedule."
        } finally {
            airingLoading = false
        }
    }

    fun findKnownAnime(id: Int): Anime? {
        if (selected?.id == id) return selected
        return trending.find { it.id == id }
            ?: popular.find { it.id == id }
            ?: topRated.find { it.id == id }
            ?: newReleases.find { it.id == id }
            ?: upcomingAnime.find { it.id == id }
            ?: actionAnime.find { it.id == id }
            ?: fantasyAnime.find { it.id == id }
            ?: scifiAnime.find { it.id == id }
            ?: romanceAnime.find { it.id == id }
            ?: browse.find { it.id == id }
            ?: homeExplore.find { it.id == id }
            ?: airing.find { it.anime.id == id }?.anime
            ?: history.find { it.animeId == id }?.let { item ->
                Anime(
                    id = item.animeId,
                    title = item.title,
                    cover = item.cover,
                    banner = item.banner,
                    score = null,
                    genres = emptyList(),
                    episodes = item.totalEpisodes
                )
            }
            ?: watchlist.find { it.animeId == id }?.let { item ->
                Anime(
                    id = item.animeId,
                    title = item.title,
                    cover = item.cover,
                    banner = item.banner,
                    score = item.score,
                    genres = item.genres,
                    episodes = item.episodes
                )
            }
            ?: downloadManager.downloadedAnime.value.find { it.animeId == id }?.let { item ->
                Anime(
                    id = item.animeId,
                    title = item.title,
                    cover = item.cover,
                    banner = item.cover,
                    score = null,
                    genres = emptyList(),
                    episodes = item.episodesCount
                )
            }
    }

    fun findKnownManga(mangaId: String): MangaCard? {
        val allManga = listOf(
            mangaHome.trending,
            mangaHome.latest,
            mangaHome.newRelease,
            mangaSearchResults,
            mangaBookmarks.map { MangaCard(it.mangaId, it.title, it.poster, type = it.type) },
            mangaHistory.map { MangaCard(it.mangaId, it.title, it.poster, type = it.type) },
            downloadManager.downloadedManga.value.map { MangaCard(it.mangaId, it.title, it.poster) }
        ).flatten()
        return allManga.firstOrNull { it.id == mangaId || (it.id.isNotBlank() && mangaId.contains(it.id)) }
    }

    fun findKnownNovel(slug: String): com.nyyrox.aniverse.data.novel.NovelItem? {
        val allNovels = listOf(
            novelRankings,
            novelPopular,
            novelLatest,
            novelLightNovels,
            novelSearchResults,
            novelHistoryList.map { com.nyyrox.aniverse.data.novel.NovelItem(it.slug, it.title, it.cover) },
            downloadManager.downloadedNovels.value.map { com.nyyrox.aniverse.data.novel.NovelItem(it.novelSlug, it.title, it.cover) }
        ).flatten()
        return allNovels.firstOrNull { it.slug == slug || (it.slug.isNotBlank() && slug.contains(it.slug)) }
    }

    fun loadAnimeForPlayer(id: Int) = viewModelScope.launch {
        if (selected?.id == id && !detailLoading) return@launch
        val known = findKnownAnime(id)
        if (known != null && selected?.id != id) {
            selected = known
        }
        detailLoading = true
        detailError = null
        try {
            val fetched = repo.detail(id)
            selected = fetched
        } catch (e: Exception) {
            if (selected == null) {
                detailError = e.message ?: "Could not load anime details."
            }
        } finally {
            detailLoading = false
        }
    }

    fun openDetail(id: Int) = viewModelScope.launch {
        activeDetailAnimeId = id
        if (selected?.id == id && !detailLoading) return@launch
        val known = findKnownAnime(id)
        selected = known
        detailLoading = true
        detailError = null
        try {
            val fetched = repo.detail(id)
            selected = fetched
        } catch (e: Exception) {
            if (selected == null) {
                detailError = e.message ?: "Could not load anime details."
            }
        } finally {
            detailLoading = false
        }
    }

    // Watchlist actions
    fun toggleWatchlist(anime: Anime, defaultStatus: WatchlistStatus = WatchlistStatus.PLAN_TO_WATCH): Boolean {
        val wasInWatchlist = isInWatchlist(anime.id)
        val result = storage.toggleWatchlist(anime, defaultStatus.id)
        watchlist = storage.getWatchlist()
        if (wasInWatchlist) {
            deleteWatchlistItemFromCloud(anime.id)
            deleteWatchlistItemFromTrackers(anime.id, anime.idMal)
        } else {
            syncWatchlistItemToCloud(anime.id)
            syncWatchlistItemToTrackers(anime.id, anime.idMal)
        }
        return result
    }

    fun setWatchlistStatus(anime: Anime, status: WatchlistStatus) {
        storage.setWatchlistStatus(anime.id, status.id, anime)
        watchlist = storage.getWatchlist()
        syncWatchlistItemToCloud(anime.id)
        syncWatchlistItemToTrackers(anime.id, anime.idMal)
    }

    fun setWatchlistStatus(animeId: Int, status: WatchlistStatus) {
        storage.setWatchlistStatus(animeId, status.id)
        watchlist = storage.getWatchlist()
        syncWatchlistItemToCloud(animeId)
        syncWatchlistItemToTrackers(animeId)
    }

    fun updateWatchlistDetails(
        animeId: Int,
        status: WatchlistStatus? = null,
        progressEpisode: Int? = null,
        customTags: List<String>? = null,
        anime: Anime? = null
    ) {
        storage.setWatchlistDetails(animeId, status?.id, progressEpisode, customTags, anime)
        watchlist = storage.getWatchlist()
        syncWatchlistItemToCloud(animeId)
        syncWatchlistItemToTrackers(animeId, anime?.idMal)
    }

    fun incrementWatchlistProgress(animeId: Int) {
        storage.incrementWatchlistProgress(animeId)
        watchlist = storage.getWatchlist()
        syncWatchlistItemToCloud(animeId)
        syncWatchlistItemToTrackers(animeId)
    }

    fun addWatchlistCustomTag(animeId: Int, tag: String) {
        val item = getWatchlistItem(animeId) ?: return
        if (!item.customTags.contains(tag)) {
            val updated = item.customTags + tag
            storage.setWatchlistDetails(animeId, customTags = updated)
            watchlist = storage.getWatchlist()
        }
    }

    fun removeWatchlistCustomTag(animeId: Int, tag: String) {
        val item = getWatchlistItem(animeId) ?: return
        val updated = item.customTags.filterNot { it == tag }
        storage.setWatchlistDetails(animeId, customTags = updated)
        watchlist = storage.getWatchlist()
    }

    fun getWatchlistItem(animeId: Int): WatchlistItem? {
        return watchlist.firstOrNull { it.animeId == animeId }
    }

    fun getWatchlistStatus(animeId: Int): WatchlistStatus? {
        return getWatchlistItem(animeId)?.watchlistStatus
    }

    suspend fun searchAniListAnime(query: String): List<Anime> {
        return withContext(Dispatchers.IO) {
            try {
                repo.list(search = query, page = 1, perPage = 20)
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    fun getWatchlistCount(status: WatchlistStatus): Int {
        return if (status == WatchlistStatus.ALL) {
            watchlist.size
        } else {
            watchlist.count { it.watchlistStatus == status }
        }
    }

    fun isInWatchlist(animeId: Int): Boolean {
        return watchlist.any { it.animeId == animeId }
    }

    fun removeFromWatchlist(animeId: Int) {
        storage.removeFromWatchlist(animeId)
        watchlist = storage.getWatchlist()
        deleteWatchlistItemFromCloud(animeId)
        deleteWatchlistItemFromTrackers(animeId)
    }

    // History actions
    fun recordHistory(
        anime: Anime,
        episode: Int,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        episodeStill: String? = null
    ) {
        val resolvedStill = episodeStill?.takeIf { it.isNotBlank() }
            ?: tmdbEpisodeStills["${anime.id}_$episode"]?.takeIf { it.isNotBlank() }
        storage.recordProgress(anime, episode, positionMs, durationMs, resolvedStill)
        history = storage.getHistory()
        watchlist = storage.getWatchlist()
        // Daily cloud sync check (only upserts to Supabase once per 24 hours)
        syncDailyToCloudIfDue()
        // Auto-sync watchlist progress to connected trackers
        if (isInWatchlist(anime.id)) {
            syncWatchlistItemToTrackers(anime.id, anime.idMal)
        } else if (trackerSettings.syncOnEpisodeComplete) {
            // Push episode progress to tracker even if not explicitly bookmarked
            viewModelScope.launch {
                TrackerSyncManager.pushWatchlistUpdate(
                    storage = storage,
                    animeId = anime.id,
                    malId = anime.idMal,
                    status = "watching",
                    progress = episode,
                    score = anime.score?.toDouble()
                )
            }
        }

        // ── Watchtime-based EXP ─────────────────────────────────────────────
        // Award EXP once per real minute of video watched (not per recordHistory call).
        // positionMs is the current playback position in milliseconds.
        // We give 20 EXP for every 60 seconds of new playback since last award.
        if (positionMs > 0L && authState is AuthState.LoggedIn) {
            val lastAwarded = watchtimeExpTrackerMs[anime.id] ?: 0L
            val minutesWatched = (positionMs - lastAwarded) / 60_000L
            if (minutesWatched >= 1L) {
                val expToGrant = minutesWatched * com.nyyrox.aniverse.data.LevelAndStreakSystem.EXP_WATCHTIME_PER_MIN
                watchtimeExpTrackerMs[anime.id] = lastAwarded + (minutesWatched * 60_000L)
                addExp(expToGrant, "Watchtime: ${minutesWatched}m of ${anime.title}")
            }
        }
    }

    fun getSavedPosition(animeId: Int, episode: Int): Long {
        return storage.getEpisodePosition(animeId, episode)
    }

    fun removeHistory(animeId: Int) {
        storage.removeHistory(animeId)
        history = storage.getHistory()
        deleteHistoryItemFromCloud(animeId)
    }

    // ── Tracker Sync & Auth Methods (AniList & MAL) ───────────────────────────
    fun syncWatchlistItemToTrackers(animeId: Int, malId: Int? = null) {
        viewModelScope.launch {
            val item = getWatchlistItem(animeId)
            if (item != null) {
                TrackerSyncManager.pushWatchlistUpdate(
                    storage = storage,
                    animeId = animeId,
                    malId = malId,
                    status = item.status,
                    progress = item.progressEpisode,
                    score = item.score?.toDouble()
                )
            }
        }
    }

    fun deleteWatchlistItemFromTrackers(animeId: Int, malId: Int? = null) {
        viewModelScope.launch {
            TrackerSyncManager.pushWatchlistDelete(
                storage = storage,
                animeId = animeId,
                malId = malId
            )
        }
    }

    fun pushAniListSyncToCloud(account: AniListAccount?) {
        val state = authState
        if (state !is AuthState.LoggedIn) return
        viewModelScope.launch {
            try {
                val syncJson = if (account != null) {
                    org.json.JSONObject().apply {
                        put("userId", account.userId)
                        put("username", account.username)
                        put("avatarUrl", account.avatarUrl.orEmpty())
                        put("accessToken", account.accessToken)
                        put("lastSyncedAt", account.lastSyncedAt)
                    }.toString()
                } else ""
                supabaseRepo.updateProfile(userId = state.userId, aniListSync = syncJson)
            } catch (_: Exception) {}
        }
    }

    fun pushMalSyncToCloud(account: MalAccount?) {
        val state = authState
        if (state !is AuthState.LoggedIn) return
        viewModelScope.launch {
            try {
                val syncJson = if (account != null) {
                    org.json.JSONObject().apply {
                        put("username", account.username)
                        put("avatarUrl", account.avatarUrl.orEmpty())
                        put("accessToken", account.accessToken)
                        put("refreshToken", account.refreshToken.orEmpty())
                        put("expiresAt", account.expiresAt)
                        put("lastSyncedAt", account.lastSyncedAt)
                    }.toString()
                } else ""
                supabaseRepo.updateProfile(userId = state.userId, malSync = syncJson)
            } catch (_: Exception) {}
        }
    }

    fun handleAniListAuthToken(token: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val cleanToken = token.trim()
                val acc = AniListTrackerClient.fetchViewer(cleanToken)
                storage.saveAniListAccount(acc)
                aniListAccount = acc
                trackerSyncMessage = "Connected to AniList as ${acc.username}"
                pushAniListSyncToCloud(acc)
                syncTrackers()
                onComplete?.invoke(true, "Connected to AniList as ${acc.username}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "AniList auth failed", e)
                val msg = "AniList connection error: ${e.message}"
                trackerSyncMessage = msg
                onComplete?.invoke(false, msg)
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    fun connectAniListByUsername(username: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val acc = AniListTrackerClient.fetchUserByUsername(username)
                storage.saveAniListAccount(acc)
                aniListAccount = acc
                trackerSyncMessage = "Imported AniList user @${acc.username}"
                pushAniListSyncToCloud(acc)
                syncTrackers()
                onComplete?.invoke(true, "Imported AniList profile for @${acc.username}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "AniList username import failed", e)
                val msg = "AniList import failed: ${e.message}"
                trackerSyncMessage = msg
                onComplete?.invoke(false, msg)
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    fun handleMalAuthToken(token: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val cleanToken = token.trim()
                val acc = MalTrackerClient.fetchViewer(cleanToken)
                storage.saveMalAccount(acc)
                malAccount = acc
                trackerSyncMessage = "Connected to MyAnimeList as ${acc.username}"
                pushMalSyncToCloud(acc)
                syncTrackers()
                onComplete?.invoke(true, "Connected to MyAnimeList as ${acc.username}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "MAL auth token failed", e)
                val msg = "MAL connection error: ${e.message}"
                trackerSyncMessage = msg
                onComplete?.invoke(false, msg)
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    fun handleMalAuthCode(code: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val acc = MalTrackerClient.exchangeCodeForToken(code)
                storage.saveMalAccount(acc)
                malAccount = acc
                trackerSyncMessage = "Connected to MyAnimeList as ${acc.username}"
                pushMalSyncToCloud(acc)
                syncTrackers()
                onComplete?.invoke(true, "Connected to MyAnimeList as ${acc.username}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "MAL auth failed", e)
                val msg = "MAL connection error: ${e.message}"
                trackerSyncMessage = msg
                onComplete?.invoke(false, msg)
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    fun connectMalByUsername(username: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val acc = MalTrackerClient.fetchUserByUsername(username)
                storage.saveMalAccount(acc)
                malAccount = acc
                trackerSyncMessage = "Imported MyAnimeList user @${acc.username}"
                pushMalSyncToCloud(acc)
                syncTrackers()
                onComplete?.invoke(true, "Imported MAL profile for @${acc.username}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "MAL username import failed", e)
                val msg = "MAL import failed: ${e.message}"
                trackerSyncMessage = msg
                onComplete?.invoke(false, msg)
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    fun disconnectAniList() {
        storage.saveAniListAccount(null)
        aniListAccount = null
        pushAniListSyncToCloud(null)
        trackerSyncMessage = "Disconnected from AniList"
    }

    fun disconnectMal() {
        storage.saveMalAccount(null)
        malAccount = null
        pushMalSyncToCloud(null)
        trackerSyncMessage = "Disconnected from MyAnimeList"
    }

    fun updateTrackerSettings(settings: TrackerSettings) {
        storage.saveTrackerSettings(settings)
        trackerSettings = settings
    }

    fun syncTrackers(onComplete: ((TrackerSyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            isTrackerSyncing = true
            try {
                val result = TrackerSyncManager.pullFromTrackers(storage)
                watchlist = storage.getWatchlist()
                history = storage.getHistory()
                aniListAccount = storage.getAniListAccount()
                malAccount = storage.getMalAccount()
                trackerSettings = storage.getTrackerSettings()
                trackerSyncMessage = "Synced ${result.totalPulled} items (${result.updatedLocal} updated)"
                onComplete?.invoke(result)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Tracker sync failed", e)
                trackerSyncMessage = "Sync failed: ${e.message}"
            } finally {
                isTrackerSyncing = false
            }
        }
    }

    // Manga actions
    fun setMangaSource(source: com.nyyrox.aniverse.data.manga.MangaSource) {
        if (activeMangaSource == source) return
        activeMangaSource = source
        com.nyyrox.aniverse.data.manga.MangaRepository.setSource(source)
        viewModelScope.launch {
            com.nyyrox.aniverse.util.UserPreferences.saveActiveMangaSource(getApplication(), source.id)
        }
        mangaHome = com.nyyrox.aniverse.data.manga.MangaHome()
        loadMangaHome(forceRefresh = true)
    }

    fun loadMangaHome(forceRefresh: Boolean = false) = viewModelScope.launch {
        if (!forceRefresh && (mangaHome.trending.isNotEmpty() || mangaHome.latest.isNotEmpty())) return@launch
        mangaHomeLoading = true
        mangaHomeError = null
        try {
            val home = MangaRepository.getHome()
            mangaHome = home
            if (home.trending.isEmpty() && home.latest.isEmpty()) {
                mangaHomeError = "Unable to reach manga servers. Please try again."
            }
        } catch (e: Exception) {
            mangaHomeError = e.message ?: "Failed to load manga feed"
        } finally {
            mangaHomeLoading = false
        }
    }

    fun searchManga(query: String) {
        mangaSearchJob?.cancel()
        mangaSearchJob = viewModelScope.launch {
            if (query.isBlank()) {
                mangaSearchResults = emptyList()
                mangaSearchLoading = false
                return@launch
            }
            delay(350)
            mangaSearchLoading = true
            try {
                mangaSearchResults = MangaRepository.search(query)
            } catch (e: Exception) {
                mangaSearchResults = emptyList()
            } finally {
                mangaSearchLoading = false
            }
        }
    }

    fun toggleMangaBookmark(
        mangaId: String,
        title: String,
        poster: String? = null,
        type: String? = "Manga",
        latestChapter: String? = null,
        bookmarkType: com.nyyrox.aniverse.data.manga.MangaBookmarkType = com.nyyrox.aniverse.data.manga.MangaBookmarkType.SERIES,
        chapterId: String? = null,
        chapterNumber: String? = null,
        pageIndex: Int = 0,
        pageCount: Int = 0
    ): Boolean {
        val res = storage.toggleMangaBookmark(
            mangaId = mangaId,
            title = title,
            poster = poster,
            type = type,
            latestChapter = latestChapter,
            bookmarkType = bookmarkType,
            chapterId = chapterId,
            chapterNumber = chapterNumber,
            pageIndex = pageIndex,
            pageCount = pageCount
        )
        mangaBookmarks = storage.getMangaBookmarks()
        syncTimeSpentAndReadingToCloud()
        return res
    }

    fun isMangaBookmarked(mangaId: String): Boolean {
        return storage.isMangaBookmarked(mangaId)
    }

    fun isChapterBookmarked(mangaId: String, chapterId: String?, chapterNumber: String? = null): Boolean {
        return storage.isChapterBookmarked(mangaId, chapterId, chapterNumber)
    }

    fun isPageBookmarked(mangaId: String, chapterId: String?, pageIndex: Int): Boolean {
        return storage.isPageBookmarked(mangaId, chapterId, pageIndex)
    }

    fun removeMangaBookmark(
        mangaId: String,
        bookmarkType: com.nyyrox.aniverse.data.manga.MangaBookmarkType? = null,
        chapterId: String? = null,
        pageIndex: Int? = null
    ) {
        storage.removeMangaBookmark(mangaId, bookmarkType, chapterId, pageIndex)
        mangaBookmarks = storage.getMangaBookmarks()
        syncTimeSpentAndReadingToCloud()
    }

    fun recordMangaProgress(
        mangaId: String,
        title: String,
        poster: String? = null,
        type: String? = "Manga",
        chapterId: String,
        chapterNumber: String,
        chapterTitle: String? = null,
        pageIndex: Int = 0,
        pageCount: Int = 0
    ) {
        storage.recordMangaProgress(
            mangaId, title, poster, type,
            chapterId, chapterNumber, chapterTitle,
            pageIndex, pageCount
        )
        mangaHistory = storage.getMangaHistory()
        syncDailyToCloudIfDue()
    }

    fun getMangaHistoryItem(mangaId: String): MangaHistoryItem? {
        val clean = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
        return mangaHistory.firstOrNull {
            it.mangaId == mangaId || (clean.isNotBlank() && com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(it.mangaId) == clean)
        } ?: storage.getMangaHistoryItem(mangaId)
    }

    fun getMangaChapterPageProgress(mangaId: String, chapterId: String): Int {
        return storage.getMangaChapterPageProgress(mangaId, chapterId)
    }

    fun removeMangaHistory(mangaId: String) {
        storage.removeMangaHistory(mangaId)
        mangaHistory = storage.getMangaHistory()
        syncTimeSpentAndReadingToCloud()
    }

    fun getNovelHistoryItem(slug: String): com.nyyrox.aniverse.data.novel.NovelHistoryItem? {
        return novelHistoryList.firstOrNull { it.slug == slug } ?: storage.getNovelHistoryItem(slug)
    }

    fun removeNovelHistory(slug: String) {
        storage.removeNovelHistory(slug)
        novelHistoryList = storage.getNovelHistory()
        novelHistory = novelHistoryList.associate { it.slug to it.lastChapterSlug }
        syncTimeSpentAndReadingToCloud()
    }

    // Profile & Theme Customization actions
    fun updateUserProfile(profile: UserProfile) {
        storage.saveUserProfile(profile)
        userProfile = profile
    }

    fun setTheme(theme: AnimeTheme) {
        val updated = themeSettings.copy(theme = theme)
        storage.saveThemeSettings(updated)
        themeSettings = updated
    }

    fun setThemeMode(mode: ThemeMode) {
        themePrefs.edit().putString("mode", mode.name).apply()
        ThemeModeState.mode = mode
        val updated = themeSettings.copy(mode = mode)
        storage.saveThemeSettings(updated)
        themeSettings = updated
    }

    fun setReducedMotion(enabled: Boolean) {
        val updated = themeSettings.copy(isReducedMotionEnabled = enabled)
        storage.saveThemeSettings(updated)
        themeSettings = updated
    }

    fun setPowerSaving(enabled: Boolean) {
        val updated = themeSettings.copy(isPowerSavingEnabled = enabled)
        storage.saveThemeSettings(updated)
        themeSettings = updated
    }

    fun setGlassmorphism(enabled: Boolean) {
        val updated = themeSettings.copy(isGlassmorphismEnabled = enabled)
        storage.saveThemeSettings(updated)
        themeSettings = updated
        com.styropyr0.prismal.PrismalGlass.isGlassmorphismEnabled = enabled
    }

    fun setPreferredRefreshRate(hz: Int) {
        val updated = themeSettings.copy(preferredRefreshRate = hz)
        storage.saveThemeSettings(updated)
        themeSettings = updated
    }

    fun setPreferredServer(serverId: String) {
        preferredServerId = serverId
        storage.savePreferredServerId(serverId)
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _isAutoPlayNext.value = enabled
        storage.setAutoPlayNext(enabled)
    }

    fun setAutoSkipIntroOutro(enabled: Boolean) {
        _isAutoSkipIntroOutro.value = enabled
        storage.setAutoSkipIntroOutro(enabled)
    }

    fun setPlayEmbed(enabled: Boolean) {
        _isPlayEmbed.value = enabled
    }

    // ── Multi-Source Novel Actions ──────────────────────────────────────────
    fun setNovelSource(source: com.nyyrox.aniverse.data.novel.NovelSource) {
        if (activeNovelSource == source) return
        activeNovelSource = source
        novelRepo.setSource(source)
        viewModelScope.launch {
            try {
                com.nyyrox.aniverse.util.UserPreferences.saveActiveNovelSource(getApplication(), source.id)
            } catch (_: Exception) {}
        }
        novelRankings = emptyList()
        novelPopular = emptyList()
        novelLatest = emptyList()
        novelLightNovels = emptyList()
        loadNovelHome(force = true)
    }

    fun loadNovelHome(force: Boolean = false) {
        if (!force && novelRankings.isNotEmpty() && novelPopular.isNotEmpty() && !novelHomeLoading) return
        viewModelScope.launch {
            novelHomeLoading = true
            novelHomeError = null
            try {
                val homeData = novelRepo.getHome(1)
                novelRankings = homeData.rankings
                novelPopular = homeData.popular
                novelLatest = homeData.latest
                novelLightNovels = homeData.lightNovels
            } catch (e: Exception) {
                novelHomeError = e.message ?: "Failed to load novels"
            } finally {
                novelHomeLoading = false
            }
        }
    }

    fun filterNovelsByGenre(genre: String) {
        selectedNovelGenre = genre
        viewModelScope.launch {
            novelHomeLoading = true
            novelHomeError = null
            try {
                if (genre == "All") {
                    novelPopular = novelRepo.getPopular(1)
                } else if (genre.equals("Light Novel", ignoreCase = true) || genre.equals("Light Novels", ignoreCase = true)) {
                    novelPopular = novelRepo.getLightNovels(1)
                } else {
                    novelPopular = if (activeNovelSource == com.nyyrox.aniverse.data.novel.NovelSource.NOVEL_FIRE) {
                        novelRepo.novelFire.getByGenre(genre, 1)
                    } else {
                        novelRepo.search(genre, 1)
                    }
                }
            } catch (e: Exception) {
                novelHomeError = e.message ?: "Failed to filter novels"
            } finally {
                novelHomeLoading = false
            }
        }
    }

    fun searchNovels(query: String) {
        novelSearchJob?.cancel()
        if (query.isBlank()) {
            novelSearchResults = emptyList()
            novelSearchLoading = false
            return
        }
        novelSearchJob = viewModelScope.launch {
            delay(350)
            novelSearchLoading = true
            try {
                novelSearchResults = novelRepo.search(query, 1)
            } catch (_: Exception) {
                novelSearchResults = emptyList()
            } finally {
                novelSearchLoading = false
            }
        }
    }

    fun loadNovelDetail(slug: String) {
        viewModelScope.launch {
            val cached = novelDetailCache[slug]
            if (cached != null) {
                selectedNovel = cached.detail
                novelChapters = cached.chapters
                novelChaptersMaxPage = cached.maxPage
                novelDetailLoading = false
                novelDetailError = null
            } else {
                novelDetailLoading = true
                novelDetailError = null
                selectedNovel = null
                novelChapters = emptyList()
                novelChaptersPage = 1
            }
            try {
                supervisorScope {
                    val dDeferred = async { novelRepo.getDetail(slug) }
                    val cDeferred = async { novelRepo.getChapters(slug, 1) }
                    val fetchedDetail = dDeferred.await()
                    val (chapters, maxP) = cDeferred.await()
                    val enrichedDetail = if (fetchedDetail != null) {
                        val ani = try {
                            AniListRepository.fetchMangaDetails(title = fetchedDetail.title)
                        } catch (_: Exception) { null }
                        if (ani != null) {
                            val aniBanner = ani.optString("bannerImage").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            val aniScore = ani.optInt("averageScore").takeIf { it > 0 }?.let { "$it%" }
                            val aniDesc = ani.optString("description")?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                ?.replace(Regex("<[^>]*>"), "")?.trim()
                            val aniCover = ani.optJSONObject("coverImage")?.let { c ->
                                c.optString("extraLarge").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                    ?: c.optString("large").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                            }
                            val aniRelations = MangaRepository.parseRelations(ani)
                            fetchedDetail.copy(
                                cover = aniCover ?: fetchedDetail.cover,
                                banner = aniBanner ?: fetchedDetail.banner,
                                rating = aniScore ?: fetchedDetail.rating,
                                synopsis = if (fetchedDetail.synopsis.isBlank() || fetchedDetail.synopsis.length < 50) (aniDesc ?: fetchedDetail.synopsis) else fetchedDetail.synopsis,
                                anilistId = ani.optInt("id").takeIf { it > 0 },
                                relations = aniRelations
                            )
                        } else {
                            fetchedDetail
                        }
                    } else null
                    selectedNovel = enrichedDetail
                    novelChapters = chapters
                    novelChaptersMaxPage = maxP
                    if (enrichedDetail != null) {
                        novelDetailCache[slug] = CachedNovelDetail(enrichedDetail, chapters, maxP)
                    }
                }
            } catch (e: Exception) {
                if (selectedNovel == null) {
                    novelDetailError = e.message ?: "Failed to load novel details"
                }
            } finally {
                novelDetailLoading = false
            }
        }
    }

    fun loadMoreNovelChapters(slug: String) {
        if (novelChaptersLoading || novelChaptersPage >= novelChaptersMaxPage) return
        viewModelScope.launch {
            novelChaptersLoading = true
            try {
                val nextPage = novelChaptersPage + 1
                val (newChapters, maxP) = novelRepo.getChapters(slug, nextPage)
                novelChapters = novelChapters + newChapters
                novelChaptersPage = nextPage
                novelChaptersMaxPage = maxP
            } catch (_: Exception) {
            } finally {
                novelChaptersLoading = false
            }
        }
    }

    fun loadNovelChapter(novelSlug: String, chapterSlug: String) {
        viewModelScope.launch {
            chapterContentLoading = true
            chapterContentError = null
            activeChapterContent = null
            try {
                // Check offline downloaded content first!
                val offlineContent = downloadManager.getDownloadedNovelChapter(novelSlug, chapterSlug)
                val loadedContent = offlineContent ?: novelRepo.getChapterContent(novelSlug, chapterSlug)
                    ?: throw IllegalStateException("Chapter content could not be retrieved")

                activeChapterContent = loadedContent
                novelHistory = novelHistory + (novelSlug to chapterSlug)

                // Save to persistent storage
                val nTitle = selectedNovel?.title?.takeIf { it.isNotBlank() }
                    ?: storage.getNovelHistoryItem(novelSlug)?.title
                    ?: novelSlug.replace("-", " ").replaceFirstChar { it.uppercase() }
                val nCover = selectedNovel?.cover?.takeIf { it.isNotBlank() }
                    ?: storage.getNovelHistoryItem(novelSlug)?.cover
                    ?: ""
                val cTitle = loadedContent.title.takeIf { it.isNotBlank() }
                    ?: novelChapters.firstOrNull { it.slug == chapterSlug }?.title
                    ?: chapterSlug

                storage.recordNovelProgress(
                    slug = novelSlug,
                    title = nTitle,
                    cover = nCover,
                    lastChapterSlug = chapterSlug,
                    lastChapterTitle = cTitle
                )
                novelHistoryList = storage.getNovelHistory()
                syncDailyToCloudIfDue()
            } catch (e: Exception) {
                chapterContentError = e.message ?: "Failed to load chapter content"
            } finally {
                chapterContentLoading = false
            }
        }
    }

    fun downloadAnimeEpisode(
        anime: Anime,
        episode: Int,
        provider: Provider? = null,
        episodeTitle: String? = null,
        quality: String = "Auto HD",
        audio: String = "Default"
    ) {
        val targetAudioMode = if (audio.contains("DUB", ignoreCase = true)) "dub" else "sub"
        val targetProvider = provider
            ?: storage.getLastDownloadServerId()?.let { lastId -> ALL_PROVIDERS.firstOrNull { it.id == lastId } }
            ?: preferredServerId?.let { prefId -> ALL_PROVIDERS.firstOrNull { it.id == prefId } }
            ?: com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(targetAudioMode).first()
        storage.saveDownloadSettings(audio, quality, targetProvider.id)
        downloadManager.downloadAnimeEpisode(
            anime = anime,
            episode = episode,
            provider = targetProvider,
            episodeTitle = episodeTitle,
            quality = quality,
            audio = audio
        )
    }

    fun downloadAnimeEpisodes(
        anime: Anime,
        episodes: List<Int>,
        provider: Provider? = null,
        quality: String = "Auto HD",
        audio: String = "Default"
    ) {
        val targetAudioMode = if (audio.contains("DUB", ignoreCase = true)) "dub" else "sub"
        val targetProvider = provider
            ?: storage.getLastDownloadServerId()?.let { lastId -> ALL_PROVIDERS.firstOrNull { it.id == lastId } }
            ?: preferredServerId?.let { prefId -> ALL_PROVIDERS.firstOrNull { it.id == prefId } }
            ?: com.nyyrox.aniverse.data.extractor.DirectStreamResolver.getDefaultDirectProviders(targetAudioMode).first()
        storage.saveDownloadSettings(audio, quality, targetProvider.id)
        downloadManager.downloadAnimeEpisodes(
            anime = anime,
            episodes = episodes,
            provider = targetProvider,
            quality = quality,
            audio = audio
        )
    }

    fun getLastDownloadAudio(): String? = storage.getLastDownloadAudio()
    fun getLastDownloadQuality(): String? = storage.getLastDownloadQuality()
    fun getLastDownloadServerId(): String? = storage.getLastDownloadServerId()
    fun saveDownloadSettings(audio: String, quality: String, serverId: String) = storage.saveDownloadSettings(audio, quality, serverId)

    fun retryAnimeDownload(
        anime: Anime,
        episode: Int,
        provider: Provider? = null,
        episodeTitle: String? = null,
        quality: String = "Auto HD",
        audio: String = "Default"
    ) {
        downloadManager.retryAnimeDownload(
            anime = anime,
            episode = episode,
            provider = provider,
            episodeTitle = episodeTitle,
            quality = quality,
            audio = audio
        )
    }

    fun isAnimeEpisodeDownloaded(animeId: Int, episode: Int): Boolean {
        return downloadManager.isAnimeEpisodeDownloaded(animeId, episode)
    }

    fun getDownloadedAnimeEpisode(animeId: Int, episode: Int): com.nyyrox.aniverse.data.download.DownloadedAnimeEpisode? {
        return downloadManager.getDownloadedAnimeEpisode(animeId, episode)
    }

    fun deleteDownloadedAnimeEpisode(animeId: Int, episode: Int) {
        downloadManager.deleteDownloadedAnimeEpisode(animeId, episode)
    }

    fun deleteDownloadedAnime(animeId: Int) {
        downloadManager.deleteDownloadedAnime(animeId)
    }

    fun downloadNovelChapter(novelSlug: String, novelTitle: String, novelCover: String, chapterSlug: String, chapterTitle: String) {
        downloadManager.downloadNovelChapter(
            novelSlug = novelSlug,
            novelTitle = novelTitle,
            novelCover = novelCover,
            chapterSlug = chapterSlug,
            chapterTitle = chapterTitle,
            repo = novelRepo
        )
    }

    fun downloadNovelChapter(chapter: com.nyyrox.aniverse.data.novel.NovelChapter, novel: com.nyyrox.aniverse.data.novel.NovelDetail? = selectedNovel) {
        val n = novel ?: selectedNovel ?: return
        downloadManager.downloadNovelChapter(
            novelSlug = n.slug,
            novelTitle = n.title,
            novelCover = n.cover,
            chapter = chapter,
            repo = novelRepo
        )
    }

    fun downloadNovelChaptersBatch(novelSlug: String, novelTitle: String, novelCover: String, chapters: List<Pair<String, String>>) {
        downloadManager.downloadNovelChapters(
            novelSlug = novelSlug,
            novelTitle = novelTitle,
            novelCover = novelCover,
            chapters = chapters,
            repo = novelRepo
        )
    }

    fun downloadNovelChaptersBatch(chapters: List<com.nyyrox.aniverse.data.novel.NovelChapter>, novel: com.nyyrox.aniverse.data.novel.NovelDetail? = selectedNovel) {
        val n = novel ?: selectedNovel ?: return
        downloadManager.downloadNovelChapters(
            novelSlug = n.slug,
            novelTitle = n.title,
            novelCover = n.cover,
            chapters = chapters,
            repo = novelRepo
        )
    }

    fun downloadMangaChapter(mangaId: String, mangaTitle: String, mangaPoster: String?, chapterId: String, chapterTitle: String) {
        downloadManager.downloadMangaChapter(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapterId = chapterId,
            chapterTitle = chapterTitle
        )
    }

    fun downloadMangaChapter(mangaId: String, mangaTitle: String, mangaPoster: String?, chapter: com.nyyrox.aniverse.data.manga.MangaChapter) {
        downloadManager.downloadMangaChapter(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapter = chapter
        )
    }

    @JvmName("downloadMangaChaptersBatchPairs")
    fun downloadMangaChaptersBatch(mangaId: String, mangaTitle: String, mangaPoster: String?, chapters: List<Pair<String, String>>) {
        downloadManager.downloadMangaChapters(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapters = chapters
        )
    }

    @JvmName("downloadMangaChaptersBatchModels")
    fun downloadMangaChaptersBatch(mangaId: String, mangaTitle: String, mangaPoster: String?, chapters: List<com.nyyrox.aniverse.data.manga.MangaChapter>) {
        downloadManager.downloadMangaChapters(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaPoster = mangaPoster,
            chapters = chapters
        )
    }

    fun updateNovelReaderSettings(settings: com.nyyrox.aniverse.data.novel.NovelReaderSettings) {
        novelReaderSettings = settings
    }

    fun toggleNovelBookmark(slug: String) {
        novelBookmarks = if (novelBookmarks.contains(slug)) {
            novelBookmarks - slug
        } else {
            novelBookmarks + slug
        }
    }

    // ── Supabase Auth & Sync ─────────────────────────────────────────────

    fun checkExistingSession() {
        viewModelScope.launch {
            try {
                supabaseRepo.refreshSession()
                val state = supabaseRepo.getAuthState()
                authState = state
                if (state is AuthState.LoggedIn) {
                    loadCloudProfile(state.userId)
                    isUsernameSetupRequired = userProfile.username.isBlank()
                    syncFromCloud(state.userId)
                    syncDailyToCloudIfDue()
                }
            } catch (_: Throwable) {
                authState = AuthState.LoggedOut
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
        return supabaseRepo.isUsernameAvailable(username, currentUserId)
    }

    fun submitUsername(rawUsername: String, onComplete: (Boolean, String?) -> Unit) {
        val clean = rawUsername.trim().lowercase().removePrefix("@")
        if (clean.length < 3) {
            onComplete(false, "Username must be at least 3 characters.")
            return
        }
        if (clean.length > 20) {
            onComplete(false, "Username must be at most 20 characters.")
            return
        }
        if (!Regex("^[a-z0-9_]+$").matches(clean)) {
            onComplete(false, "Username can only contain letters, numbers, and underscores.")
            return
        }

        val state = authState
        val userId = (state as? AuthState.LoggedIn)?.userId
            ?: userProfile.id?.takeIf { it.isNotBlank() && it != "guest" }
            ?: supabaseRepo.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            onComplete(false, "You must be signed in to set a username.")
            return
        }

        viewModelScope.launch {
            val available = supabaseRepo.isUsernameAvailable(clean, userId)
            if (!available) {
                onComplete(false, "Username '@$clean' is already taken. Try another.")
                return@launch
            }

            updateCloudProfile(username = clean)
            isUsernameSetupRequired = false
            onComplete(true, null)
        }
    }

    fun signIn(email: String, password: String) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val result = supabaseRepo.signInWithEmail(email, password)
            result.onSuccess { userId ->
                if (userId.isNotBlank()) {
                    loadCloudProfile(userId)
                    userProfile = userProfile.copy(
                        id = userId,
                        email = email,
                        displayName = if (userProfile.displayName == "Guest" || userProfile.displayName.isBlank()) email.substringBefore("@") else userProfile.displayName
                    )
                    storage.saveUserProfile(userProfile)
                    isUsernameSetupRequired = userProfile.username.isBlank()
                    authState = AuthState.LoggedIn(userId, email)
                    syncFromCloud(userId)
                    syncDailyToCloudIfDue()
                } else {
                    authError = "Sign in returned no user ID. Please check your credentials."
                    authState = AuthState.Error(authError!!)
                }
                authLoading = false
            }.onFailure { e ->
                val msg = e.message ?: ""
                authError = when {
                    msg.contains("Email not confirmed", ignoreCase = true) ->
                        "Your email has not been verified yet. Please check your inbox or spam folder."
                    msg.contains("Invalid login credentials", ignoreCase = true) ->
                        "Incorrect email or password. Please try again."
                    else -> msg.ifBlank { "Sign in failed" }
                }
                authState = AuthState.Error(authError!!)
                authLoading = false
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            val result = supabaseRepo.signUpWithEmail(email, password, displayName)
            result.onSuccess { userId ->
                if (userId.isNotBlank()) {
                    val resolvedName = displayName.ifBlank { email.substringBefore("@") }
                    userProfile = userProfile.copy(
                        id = userId,
                        email = email,
                        displayName = resolvedName
                    )
                    storage.saveUserProfile(userProfile)

                    if (displayName.isNotBlank()) {
                        supabaseRepo.updateProfile(userId, displayName = displayName, username = null, bio = null, avatarUrl = null, bannerUrl = null, favoriteAnime = null)
                    }
                    loadCloudProfile(userId)
                    isUsernameSetupRequired = userProfile.username.isBlank()
                    authState = AuthState.LoggedIn(userId, email)
                    syncFromCloud(userId)
                    syncDailyToCloudIfDue()
                    loadNotifications()
                } else {
                    authError = "Account created! A confirmation email has been sent to $email. Please check your inbox and verify before signing in."
                    authState = AuthState.Error(authError!!)
                }
                authLoading = false
            }.onFailure { e ->
                val msg = e.message ?: ""
                authError = when {
                    msg.contains("User already registered", ignoreCase = true) ->
                        "An account with this email already exists. Please Sign In."
                    msg.contains("Password should be", ignoreCase = true) ->
                        "Password must be at least 6 characters long."
                    else -> msg.ifBlank { "Sign up failed" }
                }
                authState = AuthState.Error(authError!!)
                authLoading = false
            }
        }
    }

    fun handleGoogleSignInSuccess(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String,
        idToken: String?
    ) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            try {
                var finalUserId = userId
                if (!idToken.isNullOrBlank()) {
                    val supabaseResult = supabaseRepo.signInWithGoogleIdToken(idToken)
                    supabaseResult.onSuccess { sId ->
                        if (sId.isNotBlank()) finalUserId = sId
                    }
                }
                if (finalUserId.isBlank()) {
                    finalUserId = "google_${email.replace("@", "_").replace(".", "_")}"
                }

                // 1. Fetch existing profile from Supabase first to preserve existing custom pfp and name
                val existingCloudProfile = try {
                    supabaseRepo.getProfile(finalUserId)
                } catch (_: Throwable) {
                    null
                }

                // 2. Resolve avatar: prioritize existing cloud avatar, then local custom avatar, only falling back to Google photo if user has none
                val resolvedAvatarUrl = when {
                    !existingCloudProfile?.avatarUrl.isNullOrBlank() -> existingCloudProfile!!.avatarUrl!!
                    userProfile.avatarUrl.isNotBlank() && userProfile.avatarUrl != photoUrl -> userProfile.avatarUrl
                    photoUrl.isNotBlank() -> photoUrl
                    else -> ""
                }

                // 3. Resolve display name: prioritize existing cloud name, then local custom name, then Google display name, then email prefix
                val resolvedDisplayName = when {
                    !existingCloudProfile?.displayName.isNullOrBlank() &&
                        existingCloudProfile?.displayName != "Otaku" &&
                        existingCloudProfile?.displayName != "Guest" -> existingCloudProfile!!.displayName!!
                    userProfile.displayName.isNotBlank() &&
                        userProfile.displayName != "Guest" &&
                        userProfile.displayName != "Otaku" -> userProfile.displayName
                    displayName.isNotBlank() -> displayName
                    else -> email.substringBefore("@").ifBlank { "Otaku" }
                }

                if (existingCloudProfile != null) {
                    cloudProfile = existingCloudProfile
                    userProfile = UserProfile(
                        id = existingCloudProfile.id.ifBlank { finalUserId },
                        email = email.ifBlank { supabaseRepo.getCurrentUserEmail() },
                        avatarUrl = resolvedAvatarUrl,
                        bannerUrl = existingCloudProfile.bannerUrl ?: userProfile.bannerUrl,
                        displayName = resolvedDisplayName,
                        username = existingCloudProfile.username ?: userProfile.username,
                        bio = existingCloudProfile.bio ?: userProfile.bio,
                        bioFont = existingCloudProfile.bioFont ?: userProfile.bioFont,
                        favoriteAnime = existingCloudProfile.favoriteAnime ?: userProfile.favoriteAnime,
                        otakuRank = existingCloudProfile.otakuRank ?: userProfile.otakuRank.ifBlank { "Genin" },
                        avatarDecorationId = existingCloudProfile.avatarDecoration ?: userProfile.avatarDecorationId,
                        avatarFrameId = existingCloudProfile.avatarFrame ?: userProfile.avatarFrameId,
                        textEffectId = existingCloudProfile.textEffect ?: userProfile.textEffectId,
                        profileEffectId = existingCloudProfile.profileEffect ?: userProfile.profileEffectId,
                        bannerThemeId = existingCloudProfile.bannerTheme ?: userProfile.bannerThemeId,
                        nameplateId = existingCloudProfile.nameplate ?: userProfile.nameplateId,
                        nameplateEmoji = existingCloudProfile.nameplateEmoji ?: userProfile.nameplateEmoji,
                        isAdmin = existingCloudProfile.isAdmin ?: userProfile.isAdmin,
                        role = existingCloudProfile.role ?: userProfile.role,
                        roles = existingCloudProfile.roles?.takeIf { it.isNotEmpty() }
                            ?: (if (existingCloudProfile.isAdmin == true) listOf("ADMIN") else userProfile.roles),
                        customTag = existingCloudProfile.customTag ?: userProfile.customTag,
                        exp = existingCloudProfile.exp ?: userProfile.exp,
                        level = existingCloudProfile.level ?: userProfile.level,
                        streakCount = existingCloudProfile.streakCount ?: userProfile.streakCount,
                        longestStreak = existingCloudProfile.longestStreak ?: userProfile.longestStreak,
                        lastActiveDate = existingCloudProfile.lastActiveDate ?: userProfile.lastActiveDate,
                        bannerEffectId = existingCloudProfile.bannerEffect ?: userProfile.bannerEffectId,
                        isWatchlistPrivate = existingCloudProfile.isWatchlistPrivate ?: userProfile.isWatchlistPrivate,
                        isWatchHistoryPrivate = existingCloudProfile.isHistoryPrivate ?: userProfile.isWatchHistoryPrivate,
                        isMangaPrivate = existingCloudProfile.isMangaPrivate ?: userProfile.isMangaPrivate,
                        isNovelPrivate = existingCloudProfile.isNovelPrivate ?: userProfile.isNovelPrivate
                    )
                    storage.saveUserProfile(userProfile)

                    // Only backfill missing fields in cloud, NEVER overwrite existing custom avatar or display name
                    if (existingCloudProfile.avatarUrl.isNullOrBlank() && resolvedAvatarUrl.isNotBlank()) {
                        try {
                            supabaseRepo.updateProfile(userId = finalUserId, avatarUrl = resolvedAvatarUrl)
                        } catch (_: Throwable) {}
                    }
                    if (existingCloudProfile.displayName.isNullOrBlank() && resolvedDisplayName.isNotBlank()) {
                        try {
                            supabaseRepo.updateProfile(userId = finalUserId, displayName = resolvedDisplayName)
                        } catch (_: Throwable) {}
                    }
                } else {
                    // Brand new user without a profile in Supabase
                    userProfile = userProfile.copy(
                        id = finalUserId,
                        email = email,
                        displayName = resolvedDisplayName,
                        avatarUrl = resolvedAvatarUrl
                    )
                    storage.saveUserProfile(userProfile)

                    try {
                        supabaseRepo.updateProfile(
                            userId = finalUserId,
                            displayName = resolvedDisplayName,
                            avatarUrl = resolvedAvatarUrl.ifBlank { null }
                        )
                    } catch (_: Throwable) {}
                }

                loadCloudProfile(finalUserId)
                isUsernameSetupRequired = userProfile.username.isBlank()
                authState = AuthState.LoggedIn(finalUserId, email)
                syncFromCloud(finalUserId)
                syncDailyToCloudIfDue()
                loadNotifications()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                authLoading = false
            }
        }
    }

    fun handleGoogleSignInError(error: String) {
        authLoading = false
        authError = error
        authState = AuthState.Error(error)
    }

    fun handleDiscordSignIn(account: com.nyyrox.aniverse.data.discord.DiscordAccount) {
        authLoading = true
        authError = null
        viewModelScope.launch {
            try {
                val userId = "discord_${account.userId}"
                val email = "${account.username.lowercase().replace(" ", "_")}@discord.com"
                val displayName = account.globalName ?: account.username
                val avatarUrl = account.avatarUrl.orEmpty()

                userProfile = userProfile.copy(
                    id = userId,
                    email = email,
                    displayName = displayName,
                    username = account.username.lowercase().replace(" ", "_"),
                    avatarUrl = avatarUrl.ifBlank { userProfile.avatarUrl }
                )
                storage.saveUserProfile(userProfile)
                authState = AuthState.LoggedIn(userId, email)

                try {
                    supabaseRepo.updateProfile(
                        userId = userId,
                        displayName = displayName,
                        username = account.username.lowercase().replace(" ", "_"),
                        avatarUrl = avatarUrl.ifBlank { null }
                    )
                } catch (_: Throwable) {}

                syncFromCloud(userId)
                syncDailyToCloudIfDue()
                loadNotifications()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                authLoading = false
            }
        }
    }

    fun signInWithGoogle() {
        authLoading = true
        authError = null
        viewModelScope.launch {
            try {
                supabaseRepo.signInWithGoogle()
                delay(2000)
                val state = supabaseRepo.getAuthState()
                authState = state
                if (state is AuthState.LoggedIn) {
                    loadCloudProfile(state.userId)
                    isUsernameSetupRequired = userProfile.username.isBlank()
                    syncFromCloud(state.userId)
                    syncDailyToCloudIfDue()
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                authError = if (msg.contains("provider is not enabled", ignoreCase = true) || msg.contains("validation_failed", ignoreCase = true)) {
                    "Google Provider is not enabled in Supabase dashboard yet. Please use Email Login or enable Google in Supabase Auth Providers."
                } else {
                    msg.ifBlank { "Google sign in failed" }
                }
                authState = AuthState.Error(authError!!)
            } finally {
                authLoading = false
            }
        }
    }

    fun signOut() {
        val currentUid = (authState as? AuthState.LoggedIn)?.userId
        viewModelScope.launch {
            if (!currentUid.isNullOrBlank()) {
                try {
                    syncTimeSpentAndReadingToCloud()
                } catch (_: Exception) {}
            }
            supabaseRepo.signOut()
            authState = AuthState.LoggedOut
            cloudProfile = null
            authError = null
            isUsernameSetupRequired = false
            userProfile = UserProfile() // Reset to default guest profile
            storage.saveUserProfile(userProfile)

            // Switch in-memory and UI state to clean Guest record so previous user's time is not shown
            val guestRecord = storage.getTimeSpentRecord(null)
            timeSpentRecord = guestRecord
            watchtimeExpTrackerMs.clear()
        }
    }

    private suspend fun loadCloudProfile(userId: String) {
        try {
            val profile = supabaseRepo.getProfile(userId)
            cloudProfile = profile
            if (profile != null) {
                // Clear any playback-exp tracking from earlier session
                watchtimeExpTrackerMs.clear()

                val isSameUser = userProfile.id == userId
                val localStreak = if (isSameUser) userProfile.streakCount else 1
                val localLongest = if (isSameUser) userProfile.longestStreak else 1
                val localLastActive = if (isSameUser) userProfile.lastActiveDate else null
                val localExp = if (isSameUser) userProfile.exp else 0L

                val cloudStreak = profile.streakCount ?: localStreak
                val cloudLongest = profile.longestStreak ?: localLongest
                val cloudLastActive = profile.lastActiveDate ?: localLastActive
                val cloudExp = maxOf(profile.exp ?: 0L, localExp)

                val streakEval = com.nyyrox.aniverse.data.LevelAndStreakSystem.evaluateDailyStreak(
                    lastActiveDateStr = cloudLastActive,
                    currentStreak = cloudStreak,
                    longestStreak = cloudLongest
                )
                val finalExp = if (streakEval.isNewDay) cloudExp + streakEval.earnedExp else cloudExp
                val finalLevel = com.nyyrox.aniverse.data.LevelAndStreakSystem.calculateLevel(finalExp)
                val defaultRank = com.nyyrox.aniverse.data.LevelAndStreakSystem.getDefaultOtakuRank(finalLevel)
                val finalRank = profile.otakuRank?.ifBlank { defaultRank } ?: defaultRank

                userProfile = UserProfile(
                    id = profile.id,
                    email = supabaseRepo.getCurrentUserEmail() ?: if (isSameUser) userProfile.email else null,
                    avatarUrl = if (!profile.avatarUrl.isNullOrBlank()) profile.avatarUrl else if (isSameUser) userProfile.avatarUrl else "",
                    bannerUrl = if (!profile.bannerUrl.isNullOrBlank()) profile.bannerUrl else if (isSameUser) userProfile.bannerUrl else "",
                    displayName = if (!profile.displayName.isNullOrBlank() && profile.displayName != "Otaku" && profile.displayName != "Guest") {
                        profile.displayName
                    } else if (isSameUser && userProfile.displayName.isNotBlank() && userProfile.displayName != "Guest") {
                        userProfile.displayName
                    } else {
                        profile.displayName?.ifBlank { "Otaku" } ?: "Otaku"
                    },
                    username = profile.username ?: if (isSameUser) userProfile.username else "",
                    bio = profile.bio ?: if (isSameUser) userProfile.bio else "",
                    bioFont = profile.bioFont ?: if (isSameUser) userProfile.bioFont else "default",
                    favoriteAnime = profile.favoriteAnime ?: if (isSameUser) userProfile.favoriteAnime else "",
                    otakuRank = finalRank,
                    avatarDecorationId = profile.avatarDecoration ?: if (isSameUser) userProfile.avatarDecorationId else null,
                    avatarFrameId = profile.avatarFrame ?: if (isSameUser) userProfile.avatarFrameId else null,
                    textEffectId = profile.textEffect ?: if (isSameUser) userProfile.textEffectId else null,
                    profileEffectId = profile.profileEffect ?: if (isSameUser) userProfile.profileEffectId else null,
                    bannerThemeId = profile.bannerTheme ?: if (isSameUser) userProfile.bannerThemeId else null,
                    nameplateId = profile.nameplate ?: if (isSameUser) userProfile.nameplateId else null,
                    nameplateEmoji = profile.nameplateEmoji ?: if (isSameUser) userProfile.nameplateEmoji else null,
                    isAdmin = profile.isAdmin ?: if (isSameUser) userProfile.isAdmin else false,
                    role = profile.role ?: if (isSameUser) userProfile.role else "USER",
                    roles = profile.roles?.takeIf { it.isNotEmpty() }
                        ?: (if (profile.isAdmin == true) listOf("ADMIN") else if (isSameUser) userProfile.roles else emptyList()),
                    customTag = profile.customTag ?: if (isSameUser) userProfile.customTag else null,
                    exp = finalExp,
                    level = finalLevel,
                    streakCount = streakEval.newStreak,
                    longestStreak = streakEval.newLongest,
                    lastActiveDate = streakEval.todayStr,
                    bannerEffectId = profile.bannerEffect ?: if (isSameUser) userProfile.bannerEffectId else "none"
                )

                // ── Restore & Merge App Usage / Time Spent from Cloud (Strictly for this userId) ──
                val localRecord = storage.getTimeSpentRecord(userId).checkAndReset()
                val todayStr = com.nyyrox.aniverse.data.TimeSpentRecord.getTodayDateString()
                val isCloudToday = profile.lastActiveDate == todayStr

                val cloudAnime = profile.timeSpentAnime ?: 0L
                val cloudManga = profile.timeSpentManga ?: 0L
                val cloudNovel = profile.timeSpentNovel ?: 0L
                val cloudApp = profile.timeSpentApp ?: 0L
                val cloudWeekly = if (isCloudToday) (profile.weeklyTimeSpent ?: 0L) else 0L
                val cloudDaily = if (isCloudToday) (profile.dailyTimeSpent ?: 0L) else 0L

                val mergedAllTime = ActivityTimeStats(
                    animeSeconds = maxOf(localRecord.allTime.animeSeconds, cloudAnime),
                    mangaSeconds = maxOf(localRecord.allTime.mangaSeconds, cloudManga),
                    novelSeconds = maxOf(localRecord.allTime.novelSeconds, cloudNovel),
                    appSeconds = maxOf(localRecord.allTime.appSeconds, cloudApp)
                )
                val mergedWeekly = if (isCloudToday && localRecord.weekly.totalSeconds < cloudWeekly) {
                    val diff = cloudWeekly - localRecord.weekly.totalSeconds
                    localRecord.weekly.copy(appSeconds = localRecord.weekly.appSeconds + diff)
                } else {
                    localRecord.weekly
                }
                val mergedDaily = if (isCloudToday && localRecord.daily.totalSeconds < cloudDaily) {
                    val diff = cloudDaily - localRecord.daily.totalSeconds
                    localRecord.daily.copy(appSeconds = localRecord.daily.appSeconds + diff)
                } else {
                    localRecord.daily
                }
                val mergedRecord = localRecord.copy(
                    allTime = mergedAllTime,
                    weekly = mergedWeekly,
                    daily = mergedDaily
                )
                storage.saveTimeSpentRecord(mergedRecord, userId)
                timeSpentRecord = mergedRecord

                userProfile = userProfile.copy(
                    timeSpentAnime = mergedAllTime.animeSeconds,
                    timeSpentManga = mergedAllTime.mangaSeconds,
                    timeSpentNovel = mergedAllTime.novelSeconds,
                    timeSpentApp = mergedAllTime.appSeconds,
                    timeSpentTotal = mergedAllTime.totalSeconds
                )
                storage.saveUserProfile(userProfile)

                if (streakEval.isNewDay || profile.exp != finalExp || profile.level != finalLevel) {
                    try {
                        supabaseRepo.updateProfile(
                            userId = userId,
                            exp = finalExp,
                            level = finalLevel,
                            streakCount = streakEval.newStreak,
                            longestStreak = streakEval.newLongest,
                            lastActiveDate = streakEval.todayStr,
                            otakuRank = finalRank
                        )
                    } catch (_: Exception) {}
                }

                // ── Restore Tracker Configurations (AniList & MAL) from Cloud ──
                var hasNewTrackers = false
                if (!profile.aniListSync.isNullOrBlank()) {
                    try {
                        val obj = org.json.JSONObject(profile.aniListSync)
                        val acc = AniListAccount(
                            userId = obj.getInt("userId"),
                            username = obj.getString("username"),
                            avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
                            accessToken = obj.getString("accessToken"),
                            lastSyncedAt = obj.optLong("lastSyncedAt", 0L)
                        )
                        storage.saveAniListAccount(acc)
                        aniListAccount = acc
                        hasNewTrackers = true
                    } catch (_: Exception) {}
                } else if (aniListAccount != null) {
                    pushAniListSyncToCloud(aniListAccount)
                }

                if (!profile.malSync.isNullOrBlank()) {
                    try {
                        val obj = org.json.JSONObject(profile.malSync)
                        val acc = MalAccount(
                            username = obj.getString("username"),
                            avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotBlank() },
                            accessToken = obj.getString("accessToken"),
                            refreshToken = obj.optString("refreshToken").takeIf { it.isNotBlank() },
                            expiresAt = obj.optLong("expiresAt", 0L),
                            lastSyncedAt = obj.optLong("lastSyncedAt", 0L)
                        )
                        storage.saveMalAccount(acc)
                        malAccount = acc
                        hasNewTrackers = true
                    } catch (_: Exception) {}
                } else if (malAccount != null) {
                    pushMalSyncToCloud(malAccount)
                }

                if (hasNewTrackers || aniListAccount != null || malAccount != null) {
                    syncTrackers()
                }

                loadWelcomeTemplate()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun regenerateWelcomeNotification(force: Boolean = true) {
        // Welcome notification is managed strictly server-side on registration.
        loadNotifications()
    }

    fun testWelcomeNotification() {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: userProfile.id
        if (currentUserId.isNullOrBlank()) return
        viewModelScope.launch {
            val template = welcomeConfig
            val welcomeTitle = template.title.ifBlank { "Welcome to Zenkai" }
            val welcomeMessage = template.message.ifBlank { "Welcome to Zenkai\nWatch Read Explore\nHave a happy journey" }
            val actionLabel = template.actionLabel.ifBlank { "Claim +100 Starter EXP" }
            val actionUrl = template.actionUrl.ifBlank { "zenkai://rewards/welcome" }
            val imageUrl = template.imageUrl.ifBlank { null }
            try {
                supabaseRepo.sendAdminDirectNotification(
                    targetUserId = currentUserId,
                    title = welcomeTitle,
                    message = welcomeMessage,
                    type = "reward",
                    imageUrl = imageUrl,
                    actionUrl = actionUrl,
                    actionLabel = actionLabel,
                    senderProfile = userProfile.copy(displayName = "Nano Banana 🍌")
                )
                loadNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Throttle: only push EXP to Supabase at most once every 5 minutes to avoid hammering the API
    private var lastExpCloudSyncMs = 0L

    fun addExp(amount: Long, reason: String = "") {
        val oldLevel = userProfile.level
        val newExp = userProfile.exp + amount
        val newLevel = com.nyyrox.aniverse.data.LevelAndStreakSystem.calculateLevel(newExp)
        val defaultRank = com.nyyrox.aniverse.data.LevelAndStreakSystem.getDefaultOtakuRank(newLevel)
        val currentRank = if (userProfile.otakuRank == "Genin" || userProfile.otakuRank == "Novice" || userProfile.otakuRank.isBlank()) {
            defaultRank
        } else {
            userProfile.otakuRank
        }

        // Always update local state + SharedPrefs immediately
        userProfile = userProfile.copy(
            exp = newExp,
            level = newLevel,
            otakuRank = currentRank
        )
        storage.saveUserProfile(userProfile)

        // Only push to Supabase once per 5 minutes (or on level-up)
        val now = System.currentTimeMillis()
        val leveledUp = newLevel > oldLevel
        val shouldSync = leveledUp || (now - lastExpCloudSyncMs >= 5 * 60 * 1000L)
        val state = authState
        if (shouldSync && state is AuthState.LoggedIn) {
            lastExpCloudSyncMs = now
            viewModelScope.launch {
                try {
                    supabaseRepo.updateProfile(
                        userId = state.userId,
                        exp = newExp,
                        level = newLevel,
                        otakuRank = currentRank
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun equipAvatarDecoration(id: String?) {
        userProfile = userProfile.copy(avatarDecorationId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipAvatarFrame(id: String?) {
        userProfile = userProfile.copy(avatarFrameId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipTextEffect(id: String?) {
        userProfile = userProfile.copy(textEffectId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipProfileEffect(id: String?) {
        userProfile = userProfile.copy(profileEffectId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipBannerTheme(id: String?) {
        userProfile = userProfile.copy(bannerThemeId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipBannerEffect(id: String?) {
        val effectId = id ?: "none"
        userProfile = userProfile.copy(bannerEffectId = effectId)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun equipCustomTag(tag: String?) {
        userProfile = userProfile.copy(customTag = tag)
        storage.saveUserProfile(userProfile)
        val state = authState
        if (state is AuthState.LoggedIn) {
            viewModelScope.launch {
                try {
                    supabaseRepo.updateProfile(
                        userId = state.userId,
                        customTag = tag
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun equipNameplate(id: String?) {
        userProfile = userProfile.copy(nameplateId = id)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    fun updateNameplateEmoji(emoji: String?) {
        userProfile = userProfile.copy(nameplateEmoji = emoji)
        storage.saveUserProfile(userProfile)
        syncDecorationsToCloud()
    }

    private fun syncDecorationsToCloud() {
        val state = authState
        if (state is AuthState.LoggedIn) {
            viewModelScope.launch {
                try {
                    supabaseRepo.updateDecorations(
                        userId = state.userId,
                        avatarDecoration = userProfile.avatarDecorationId,
                        avatarFrame = userProfile.avatarFrameId,
                        textEffect = userProfile.textEffectId,
                        profileEffect = userProfile.profileEffectId,
                        bannerTheme = userProfile.bannerThemeId,
                        nameplate = userProfile.nameplateId,
                        nameplateEmoji = userProfile.nameplateEmoji,
                        bannerEffect = userProfile.bannerEffectId
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun fetchPublicProfile(userId: String, onResult: (CloudProfile?) -> Unit) {
        viewModelScope.launch {
            val p = try {
                supabaseRepo.getPublicProfile(userId)
            } catch (_: Throwable) {
                null
            }
            onResult(p)
        }
    }

    fun fetchPublicWatchlist(userId: String, onResult: (List<CloudWatchlistItem>) -> Unit) {
        viewModelScope.launch {
            val list = try {
                supabaseRepo.fetchWatchlist(userId)
            } catch (_: Throwable) {
                emptyList()
            }
            onResult(list)
        }
    }

    fun fetchPublicWatchHistory(userId: String, onResult: (List<CloudHistoryItem>) -> Unit) {
        viewModelScope.launch {
            val list = try {
                supabaseRepo.fetchWatchHistory(userId)
            } catch (_: Throwable) {
                emptyList()
            }
            onResult(list)
        }
    }

    fun updateCloudProfile(
        displayName: String? = null,
        username: String? = null,
        bio: String? = null,
        bioFont: String? = null,
        avatarUrl: String? = null,
        bannerUrl: String? = null,
        favoriteAnime: String? = null,
        roles: List<String>? = null
    ) {
        val state = authState
        // Update local state immediately for instant feedback
        userProfile = userProfile.copy(
            displayName = displayName ?: userProfile.displayName,
            username = username ?: userProfile.username,
            bio = bio ?: userProfile.bio,
            bioFont = bioFont ?: userProfile.bioFont,
            avatarUrl = avatarUrl ?: userProfile.avatarUrl,
            bannerUrl = bannerUrl ?: userProfile.bannerUrl,
            favoriteAnime = favoriteAnime ?: userProfile.favoriteAnime,
            roles = roles ?: userProfile.roles
        )
        storage.saveUserProfile(userProfile)

        if (state !is AuthState.LoggedIn) return
        viewModelScope.launch {
            supabaseRepo.updateProfile(
                userId = state.userId,
                displayName = displayName,
                username = username,
                bio = bio,
                bioFont = bioFont,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                favoriteAnime = favoriteAnime,
                roles = roles
            )
            loadCloudProfile(state.userId)
        }
    }

    fun uploadAvatarBytes(bytes: ByteArray, mimeType: String = "image/png", onResult: ((Boolean, String?) -> Unit)? = null) {
        val state = authState
        viewModelScope.launch {
            try {
                val userId = if (state is AuthState.LoggedIn) state.userId else "local_user"
                val result = supabaseRepo.uploadImage(userId, bytes, mimeType, "avatar")
                result.onSuccess { url ->
                    updateCloudProfile(avatarUrl = url)
                    onResult?.invoke(true, null)
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Upload failed"
                    onResult?.invoke(false, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult?.invoke(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri, onResult: ((Boolean, String?) -> Unit)? = null) {
        val state = authState
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val userId = if (state is AuthState.LoggedIn) state.userId else "local_user"

                val result = supabaseRepo.uploadImage(userId, bytes, mimeType, "avatar")
                result.onSuccess { url ->
                    updateCloudProfile(avatarUrl = url)
                    onResult?.invoke(true, null)
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Upload failed"
                    onResult?.invoke(false, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult?.invoke(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun uploadBanner(context: Context, uri: Uri, onResult: ((Boolean, String?) -> Unit)? = null) {
        val state = authState
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val userId = if (state is AuthState.LoggedIn) state.userId else "local_user"

                val result = supabaseRepo.uploadImage(userId, bytes, mimeType, "banner")
                result.onSuccess { url ->
                    updateCloudProfile(bannerUrl = url)
                    onResult?.invoke(true, null)
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Upload failed"
                    onResult?.invoke(false, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult?.invoke(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun loadCommunityMessages(channel: String = "general") {
        viewModelScope.launch {
            isCommunityLoading = true
            try {
                val msgs = supabaseRepo.fetchCommunityMessages(channel)
                communityMessages = msgs
                val distinctUserIds = msgs.map { it.userId }.filter { it.isNotBlank() }.distinct()
                if (distinctUserIds.isNotEmpty()) {
                    val profiles = supabaseRepo.getProfilesByIds(distinctUserIds)
                    if (profiles.isNotEmpty()) {
                        communityProfilesMap = communityProfilesMap + profiles
                    }
                }
                val msgIds = msgs.map { it.id }.filter { it.isNotBlank() }
                if (msgIds.isNotEmpty() && authState is AuthState.LoggedIn) {
                    val userReactions = supabaseRepo.fetchUserReactions(msgIds)
                    userReactionsMap = userReactionsMap + userReactions
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCommunityLoading = false
            }
        }
    }

    fun sendCommunityMessage(
        channel: String = "general",
        text: String,
        replyToId: String? = null,
        replyToUsername: String? = null,
        replyToText: String? = null,
        replyToUserId: String? = null,
        isMention: Boolean = false,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            isSendingCommunityMessage = true
            try {
                val result = supabaseRepo.sendCommunityMessage(
                    channel = channel,
                    messageText = text,
                    profile = userProfile,
                    replyToId = replyToId,
                    replyToUsername = replyToUsername,
                    replyToText = replyToText,
                    replyToUserId = replyToUserId,
                    isMention = isMention
                )
                if (result.isSuccess) {
                    addExp(com.nyyrox.aniverse.data.LevelAndStreakSystem.EXP_COMMUNITY_CHAT, "Community chat")
                    communityMessages = supabaseRepo.fetchCommunityMessages(channel)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isSendingCommunityMessage = false
            }
        }
    }

    // ── Watch Together Party Methods ──────────────────────────────
    fun loadActiveWatchRooms() {
        viewModelScope.launch {
            isWatchRoomLoading = true
            try {
                activeWatchRoomsList = supabaseRepo.fetchActiveWatchTogetherRooms()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isWatchRoomLoading = false
            }
        }
    }

    fun createWatchTogetherRoom(
        name: String,
        anime: com.nyyrox.aniverse.data.Anime,
        episode: Int = 1,
        providerId: String = "reanime-sub",
        isPrivate: Boolean = false,
        onComplete: (com.nyyrox.aniverse.data.supabase.WatchTogetherRoom?) -> Unit = {}
    ) {
        viewModelScope.launch {
            isCreatingWatchRoom = true
            try {
                val res = supabaseRepo.createWatchTogetherRoom(
                    name = name,
                    hostProfile = userProfile,
                    animeId = anime.id,
                    animeTitle = anime.title,
                    animeCover = anime.cover,
                    idMal = anime.idMal,
                    episode = episode,
                    providerId = providerId,
                    isPrivate = isPrivate
                )
                val room = res.getOrNull()
                if (room != null) {
                    activeWatchRoom = room
                    loadWatchRoomChatMessages(room.id)
                    onComplete(room)
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            } finally {
                isCreatingWatchRoom = false
            }
        }
    }

    fun joinWatchTogetherRoom(
        roomIdOrCode: String,
        onComplete: (com.nyyrox.aniverse.data.supabase.WatchTogetherRoom?) -> Unit = {}
    ) {
        viewModelScope.launch {
            isWatchRoomLoading = true
            try {
                val res = supabaseRepo.joinWatchTogetherRoom(roomIdOrCode, userProfile)
                val room = res.getOrNull()
                if (room != null) {
                    activeWatchRoom = room
                    loadWatchRoomChatMessages(room.id)
                    onComplete(room)
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            } finally {
                isWatchRoomLoading = false
            }
        }
    }

    fun leaveWatchTogetherRoom(closeRoomIfHost: Boolean = false) {
        val current = activeWatchRoom
        activeWatchRoom = null
        watchRoomChatMessages = emptyList()
        if (current != null && closeRoomIfHost && current.hostId == userProfile.id) {
            viewModelScope.launch {
                try {
                    supabaseRepo.deleteWatchTogetherRoom(current.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun pingWatchRoomHostActivity() {
        val current = activeWatchRoom ?: return
        viewModelScope.launch {
            try {
                supabaseRepo.pingHostActivity(current.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun closeWatchTogetherRoom(roomId: String) {
        viewModelScope.launch {
            try {
                supabaseRepo.deleteWatchTogetherRoom(roomId)
                if (activeWatchRoom?.id == roomId) {
                    activeWatchRoom = null
                    watchRoomChatMessages = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateWatchPlaybackState(
        episode: Int? = null,
        providerId: String? = null,
        isPlaying: Boolean? = null,
        positionMs: Long? = null
    ) {
        val current = activeWatchRoom ?: return
        viewModelScope.launch {
            try {
                val res = supabaseRepo.updateWatchTogetherPlayback(
                    roomId = current.id,
                    episode = episode,
                    providerId = providerId,
                    isPlaying = isPlaying,
                    positionMs = positionMs
                )
                res.getOrNull()?.let {
                    activeWatchRoom = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshWatchRoomState(onSynced: ((com.nyyrox.aniverse.data.supabase.WatchTogetherRoom) -> Unit)? = null) {
        val current = activeWatchRoom ?: return
        viewModelScope.launch {
            try {
                val updated = supabaseRepo.fetchWatchTogetherRoom(current.id)
                if (updated != null) {
                    activeWatchRoom = updated
                    onSynced?.invoke(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadWatchRoomChatMessages(roomId: String? = activeWatchRoom?.id) {
        val targetId = roomId ?: return
        viewModelScope.launch {
            try {
                val msgs = supabaseRepo.fetchCommunityMessages("wt_$targetId")
                watchRoomChatMessages = msgs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendWatchRoomChatMessage(
        text: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val room = activeWatchRoom ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val res = supabaseRepo.sendCommunityMessage(
                    channel = "wt_${room.id}",
                    messageText = text,
                    profile = userProfile
                )
                if (res.isSuccess) {
                    watchRoomChatMessages = supabaseRepo.fetchCommunityMessages("wt_${room.id}")
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    // ── Notification Management Methods ─────────────────────────
    fun loadNotifications(isSilent: Boolean = false) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
            ?: userProfile.id
        viewModelScope.launch {
            if (!isSilent) isNotificationsLoading = true
            try {
                val fetched = supabaseRepo.fetchNotifications(currentUserId)
                notifications = fetched

                // Post unnotified items to system status bar tray
                val context = getApplication<Application>().applicationContext
                val prefs = context.getSharedPreferences("zenkai_notifs_cache", Context.MODE_PRIVATE)
                val notifiedIds = prefs.getStringSet("notified_ids", emptySet()) ?: emptySet()
                val newNotifiedIds = notifiedIds.toMutableSet()
                var newlyShown = 0
                for (notif in fetched) {
                    if (!notif.isRead && !notifiedIds.contains(notif.id)) {
                        if (currentUserId == null || notif.senderId != currentUserId) {
                            ZenkaiNotificationHelper.showNotification(context, notif)
                            newNotifiedIds.add(notif.id)
                            newlyShown++
                            if (newlyShown >= 3) break
                        }
                    }
                }
                if (newlyShown > 0) {
                    prefs.edit().putStringSet("notified_ids", newNotifiedIds).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (!isSilent) isNotificationsLoading = false
            }
        }
    }

    fun testPushNotification() {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            ZenkaiNotificationHelper.showNotification(
                context = context,
                notification = AppNotification(
                    id = "test_${System.currentTimeMillis()}",
                    userId = (authState as? AuthState.LoggedIn)?.userId ?: userProfile.id,
                    senderName = "Nano Banana 🍌",
                    senderAvatar = "android.resource://${context.packageName}/drawable/welcome_nano_banana",
                    title = "ZENKAI Alerts Active! 🍌🔔",
                    message = "Push notifications and redirect links are working properly in foreground and background.",
                    type = "system",
                    imageUrl = "android.resource://${context.packageName}/drawable/welcome_nano_banana",
                    actionLabel = "Open Community",
                    actionUrl = "zenkai://community",
                    isRead = false
                )
            )
        }
    }

    fun markNotificationAsRead(id: String) {
        notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }
        viewModelScope.launch {
            try {
                supabaseRepo.markNotificationAsRead(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        notifications = notifications.map { it.copy(isRead = true) }
        viewModelScope.launch {
            try {
                supabaseRepo.markAllNotificationsAsRead(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteNotification(id: String) {
        notifications = notifications.filter { it.id != id }
        viewModelScope.launch {
            try {
                supabaseRepo.deleteNotification(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adminSearchUsers(query: String) {
        if (query.isBlank()) {
            adminSearchResults = emptyList()
            return
        }
        viewModelScope.launch {
            isAdminSearching = true
            try {
                adminSearchResults = supabaseRepo.adminSearchUsers(query)
            } catch (e: Exception) {
                e.printStackTrace()
                adminSearchResults = emptyList()
            } finally {
                isAdminSearching = false
            }
        }
    }

    fun claimNotificationReward(notif: AppNotification, onComplete: (Boolean) -> Unit = {}) {
        if (notif.isClaimed) return
        notifications = notifications.map { if (it.id == notif.id) it.copy(isClaimed = true, isRead = true) else it }
        addExp(100L, "Notification Reward: ${notif.title.ifBlank { "Reward Claimed" }}")
        viewModelScope.launch {
            try {
                supabaseRepo.markNotificationClaimed(notif.id)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun sendAdminBroadcast(
        title: String,
        message: String,
        type: String = "global",
        imageUrl: String? = null,
        actionUrl: String? = null,
        actionLabel: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            isSendingAdminNotification = true
            try {
                val res = supabaseRepo.sendAdminBroadcast(
                    title = title,
                    message = message,
                    type = type,
                    imageUrl = imageUrl,
                    actionUrl = actionUrl,
                    actionLabel = actionLabel,
                    senderProfile = userProfile
                )
                if (res.isSuccess) loadNotifications()
                onComplete(res.isSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isSendingAdminNotification = false
            }
        }
    }

    fun sendAdminDirectNotification(
        targetUserId: String,
        title: String,
        message: String,
        type: String = "admin",
        imageUrl: String? = null,
        actionUrl: String? = null,
        actionLabel: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            isSendingAdminNotification = true
            try {
                val res = supabaseRepo.sendAdminDirectNotification(
                    targetUserId = targetUserId,
                    title = title,
                    message = message,
                    type = type,
                    imageUrl = imageUrl,
                    actionUrl = actionUrl,
                    actionLabel = actionLabel,
                    senderProfile = userProfile
                )
                if (res.isSuccess) loadNotifications()
                onComplete(res.isSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isSendingAdminNotification = false
            }
        }
    }

    fun isCurrentUserOwner(): Boolean {
        val roles = userProfile.roles
        val role = userProfile.role
        val username = userProfile.username
        val email = userProfile.email
        return roles.any { it.equals("OWNER", ignoreCase = true) } ||
                role?.equals("OWNER", ignoreCase = true) == true ||
                username.equals("nyyrox", ignoreCase = true) ||
                email?.startsWith("nyyrox", ignoreCase = true) == true
    }

    fun loadWelcomeTemplate() {
        viewModelScope.launch {
            try {
                val cloudTemplate = supabaseRepo.fetchWelcomeTemplate()
                if (cloudTemplate != null) {
                    welcomeConfig = cloudTemplate
                    storage.saveWelcomeNotificationConfig(cloudTemplate)
                }
            } catch (_: Exception) {}
        }
    }

    fun saveWelcomeTemplateConfig(
        newConfig: com.nyyrox.aniverse.data.WelcomeNotificationConfig,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        welcomeConfig = newConfig
        storage.saveWelcomeNotificationConfig(newConfig)
        viewModelScope.launch {
            val success = supabaseRepo.saveWelcomeTemplate(newConfig)
            onComplete?.invoke(success)
        }
    }

    fun updateTargetUserRolesAndAdmin(
        targetUserId: String,
        targetUsername: String,
        isAdmin: Boolean,
        roles: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isCurrentUserOwner()) {
            onComplete(false)
            return
        }
        isUpdatingUserRoles = true
        viewModelScope.launch {
            val success = supabaseRepo.updateUserRolesAndAdmin(
                userId = targetUserId,
                isAdmin = isAdmin,
                roles = roles
            )
            if (success) {
                val roleTitle = if (isAdmin) "Promoted to Administrator" else if (roles.contains("OWNER")) "Designated as Owner" else "Roles Updated"
                val roleDetails = if (isAdmin) "You have been granted Administrator permissions by the App Owner." else "Your assigned roles are now: ${roles.joinToString().ifBlank { "Member" }}."
                try {
                    supabaseRepo.sendAdminDirectNotification(
                        targetUserId = targetUserId,
                        title = "👑 $roleTitle",
                        message = roleDetails,
                        type = "admin",
                        senderProfile = userProfile.copy(displayName = "Owner 👑")
                    )
                } catch (_: Exception) {}
            }
            isUpdatingUserRoles = false
            onComplete(success)
        }
    }

    fun editCommunityMessage(messageId: String, channel: String, newText: String, onComplete: (Boolean) -> Unit = {}) {
        // Optimistically update message text locally
        communityMessages = communityMessages.map { if (it.id == messageId) it.copy(message = newText) else it }
        viewModelScope.launch {
            try {
                val result = supabaseRepo.editCommunityMessage(messageId, newText)
                if (result.isSuccess) {
                    communityMessages = supabaseRepo.fetchCommunityMessages(channel)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun deleteCommunityMessage(messageId: String, channel: String, onComplete: (Boolean) -> Unit = {}) {
        // Optimistically remove message locally
        communityMessages = communityMessages.filter { it.id != messageId }
        viewModelScope.launch {
            try {
                val result = supabaseRepo.deleteCommunityMessage(messageId)
                if (result.isSuccess) {
                    communityMessages = supabaseRepo.fetchCommunityMessages(channel)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun toggleCommunityReaction(messageId: String, emoji: String) {
        if (authState !is AuthState.LoggedIn) return
        val currentSet = userReactionsMap[messageId] ?: emptySet()
        val hasReacted = currentSet.contains(emoji)
        val newSet = if (hasReacted) currentSet - emoji else currentSet + emoji
        userReactionsMap = userReactionsMap + (messageId to newSet)

        // Optimistically update reactions map on the message
        communityMessages = communityMessages.map { msg ->
            if (msg.id == messageId) {
                val currentCounts = (msg.reactions ?: emptyMap()).toMutableMap()
                val currentCount = currentCounts[emoji] ?: 0
                if (hasReacted) {
                    val nextCount = (currentCount - 1).coerceAtLeast(0)
                    if (nextCount > 0) currentCounts[emoji] = nextCount else currentCounts.remove(emoji)
                } else {
                    currentCounts[emoji] = currentCount + 1
                }
                msg.copy(reactions = currentCounts)
            } else msg
        }

        viewModelScope.launch {
            try {
                supabaseRepo.toggleCommunityReaction(messageId, emoji)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Social Hub, Presence & Direct Messaging ─────────────────────────
    fun updateCurrentActivity(activity: String?, activityType: String? = null, targetId: String? = null) {
        myCurrentActivity = activity
        myCurrentActivityType = activityType
        myCurrentActivityTargetId = targetId
        // Network presence / activity tracking disabled per user request
    }

    fun searchSocialUsers(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            socialUserSearchResults = emptyList()
            isSocialUserSearching = false
            return
        }
        viewModelScope.launch {
            isSocialUserSearching = true
            try {
                socialUserSearchResults = supabaseRepo.searchUsers(q)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSocialUserSearching = false
            }
        }
    }

    fun loadActiveOnlineUsers() {
        // Disabled per user request to conserve Supabase quota/cache
        activeOnlineUsers = emptyList()
        isActiveUsersLoading = false
    }

    fun selectWatchTimeRankingTimeframe(timeframe: String) {
        watchTimeRankingTimeframe = timeframe
        loadWatchTimeRankings(timeframe)
    }

    fun loadWatchTimeRankings(timeframe: String = watchTimeRankingTimeframe) {
        viewModelScope.launch {
            isWatchTimeRankingLoading = true
            try {
                if (authState is AuthState.LoggedIn) {
                    syncTimeSpentAndReadingToCloud()
                }
                watchTimeRankings = supabaseRepo.fetchWatchTimeRankings(timeframe, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isWatchTimeRankingLoading = false
            }
        }
    }

    fun loadRecentDmPartners() {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        viewModelScope.launch {
            isRecentDmsLoading = true
            try {
                recentDmPartners = supabaseRepo.fetchRecentDmPartners(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRecentDmsLoading = false
            }
        }
    }

    fun openDirectChat(partner: CloudProfile) {
        activeDmPartnerProfile = partner
        loadDirectChatMessages(partner.id)
    }

    fun loadDirectChatMessages(partnerUserId: String) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        val dmChannel = supabaseRepo.getDmChannelId(currentUserId, partnerUserId)
        viewModelScope.launch {
            isDirectChatLoading = true
            try {
                directChatMessages = supabaseRepo.loadDirectMessages(dmChannel)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isDirectChatLoading = false
            }
        }
    }

    fun sendDirectMessage(
        partnerUserId: String,
        text: String,
        replyToId: String? = null,
        replyToUsername: String? = null,
        replyToText: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (text.isBlank()) return
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        val dmChannel = supabaseRepo.getDmChannelId(currentUserId, partnerUserId)
        viewModelScope.launch {
            isSendingDirectMessage = true
            try {
                val res = supabaseRepo.sendDirectMessage(
                    targetUserId = partnerUserId,
                    text = text,
                    replyToId = replyToId,
                    replyToUsername = replyToUsername,
                    replyToText = replyToText,
                    senderProfile = userProfile
                )
                if (res.isSuccess) {
                    addExp(com.nyyrox.aniverse.data.LevelAndStreakSystem.EXP_COMMUNITY_CHAT, "Direct Chat")
                    directChatMessages = supabaseRepo.loadDirectMessages(dmChannel)
                    loadRecentDmPartners()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isSendingDirectMessage = false
            }
        }
    }

    fun getDmChannelId(partnerUserId: String): String {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: userProfile.id ?: ""
        return supabaseRepo.getDmChannelId(currentUserId, partnerUserId)
    }

    fun deleteDirectMessage(messageId: String, partnerUserId: String, onComplete: (Boolean) -> Unit = {}) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        val dmChannel = supabaseRepo.getDmChannelId(currentUserId, partnerUserId)
        viewModelScope.launch {
            try {
                val result = supabaseRepo.deleteCommunityMessage(messageId)
                if (result.isSuccess) {
                    directChatMessages = supabaseRepo.loadDirectMessages(dmChannel)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    private suspend fun syncToCloud(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Convert local watchlist to cloud format and upload
                val cloudWatchlist = watchlist.map { item ->
                    CloudWatchlistItem(
                        userId = userId,
                        animeId = item.animeId,
                        title = item.title,
                        cover = item.cover,
                        banner = item.banner,
                        score = item.score,
                        genres = item.genres,
                        episodes = item.episodes,
                        status = item.status,
                        progressEpisode = item.progressEpisode,
                        releaseYear = item.releaseYear,
                        customTags = item.customTags
                    )
                }
                supabaseRepo.syncWatchlist(userId, cloudWatchlist)

                // Convert local history to cloud format and upload
                val cloudHistory = history.map { item ->
                    CloudHistoryItem(
                        userId = userId,
                        animeId = item.animeId,
                        title = item.title,
                        cover = item.cover,
                        banner = item.banner,
                        lastEpisode = item.lastEpisode,
                        totalEpisodes = item.totalEpisodes,
                        positionMs = item.positionMs,
                        durationMs = item.durationMs
                    )
                }
                supabaseRepo.syncWatchHistory(userId, cloudHistory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncFromCloud(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Pull watchlist from cloud
                val cloudWatchlist = supabaseRepo.fetchWatchlist(userId)
                for (cloudItem in cloudWatchlist) {
                    storage.addToWatchlistFromCloud(
                        animeId = cloudItem.animeId,
                        title = cloudItem.title,
                        cover = cloudItem.cover,
                        banner = cloudItem.banner,
                        score = cloudItem.score,
                        genres = cloudItem.genres ?: emptyList(),
                        episodes = cloudItem.episodes,
                        status = cloudItem.status ?: "plan_to_watch",
                        progressEpisode = cloudItem.progressEpisode ?: 0,
                        releaseYear = cloudItem.releaseYear,
                        customTags = cloudItem.customTags ?: emptyList()
                    )
                }

                // Pull history from cloud
                val cloudHistory = supabaseRepo.fetchWatchHistory(userId)
                for (cloudItem in cloudHistory) {
                    if (storage.isHistoryDeleted(cloudItem.animeId)) {
                        try {
                            supabaseRepo.deleteWatchHistory(userId, cloudItem.animeId)
                        } catch (_: Exception) {}
                        continue
                    }
                    val localItem = history.firstOrNull { it.animeId == cloudItem.animeId }
                    if (localItem == null || cloudItem.lastEpisode >= localItem.lastEpisode) {
                        storage.addToHistoryFromCloud(
                            animeId = cloudItem.animeId,
                            title = cloudItem.title,
                            cover = cloudItem.cover,
                            banner = cloudItem.banner,
                            lastEpisode = cloudItem.lastEpisode,
                            totalEpisodes = cloudItem.totalEpisodes,
                            positionMs = cloudItem.positionMs ?: 0L,
                            durationMs = cloudItem.durationMs ?: 0L
                        )
                    }
                }

                // Pull cloud profile for manga & novel history, bookmarks and privacy
                val cloudProf = supabaseRepo.getProfile(userId)
                if (cloudProf != null) {
                    var needsResync = false
                    val cManga = parsePublicMangaHistory(cloudProf.mangaHistoryJson)
                    for (m in cManga.reversed()) {
                        if (storage.isMangaHistoryDeleted(m.mangaId)) {
                            needsResync = true
                            continue
                        }
                        val localExisting = storage.getMangaHistoryItem(m.mangaId)
                        val bestChapterId = if (m.chapterId.isNotBlank()) m.chapterId else (localExisting?.chapterId ?: "")
                        val bestChapterNum = if (m.chapterNumber.isNotBlank()) m.chapterNumber else (localExisting?.chapterNumber ?: "1")
                        val bestPageIndex = if (m.pageIndex > 0) m.pageIndex else (if (bestChapterId == localExisting?.chapterId) localExisting?.pageIndex ?: 0 else 0)
                        val bestPageCount = if (m.pageCount > 0) m.pageCount else (if (bestChapterId == localExisting?.chapterId) localExisting?.pageCount ?: 0 else 0)

                        storage.recordMangaProgress(
                            mangaId = m.mangaId,
                            title = m.title,
                            poster = m.poster ?: localExisting?.poster,
                            type = m.type,
                            chapterId = bestChapterId,
                            chapterNumber = bestChapterNum,
                            chapterTitle = m.chapterTitle ?: localExisting?.chapterTitle,
                            pageIndex = bestPageIndex,
                            pageCount = bestPageCount
                        )
                    }

                    val cNovel = parsePublicNovelHistory(cloudProf.novelHistoryJson)
                    for (n in cNovel.reversed()) {
                        if (storage.isNovelHistoryDeleted(n.slug)) {
                            needsResync = true
                            continue
                        }
                        storage.recordNovelProgress(
                            slug = n.slug,
                            title = n.title,
                            cover = n.cover,
                            lastChapterSlug = n.lastChapterSlug,
                            lastChapterTitle = n.lastChapterTitle
                        )
                    }

                    val cBookmarks = parsePublicMangaBookmarks(cloudProf.mangaBookmarksJson)
                    for (bm in cBookmarks.reversed()) {
                        if (!storage.isMangaBookmarked(bm.mangaId)) {
                            storage.toggleMangaBookmark(
                                mangaId = bm.mangaId,
                                title = bm.title,
                                poster = bm.poster,
                                type = bm.type,
                                latestChapter = bm.latestChapter
                            )
                        }
                    }

                    // Restore privacy settings from cloud profile
                    userProfile = userProfile.copy(
                        isWatchlistPrivate = cloudProf.isWatchlistPrivate ?: userProfile.isWatchlistPrivate,
                        isWatchHistoryPrivate = cloudProf.isHistoryPrivate ?: userProfile.isWatchHistoryPrivate,
                        isMangaPrivate = cloudProf.isMangaPrivate ?: userProfile.isMangaPrivate,
                        isNovelPrivate = cloudProf.isNovelPrivate ?: userProfile.isNovelPrivate
                    )
                    storage.saveUserProfile(userProfile)

                    if (needsResync) {
                        syncTimeSpentAndReadingToCloud()
                    }
                }

                // Refresh local Compose state immediately
                withContext(Dispatchers.Main) {
                    refreshStorageData()
                    mangaHistory = storage.getMangaHistory()
                    novelHistoryList = storage.getNovelHistory()
                    novelHistory = novelHistoryList.associate { it.slug to it.lastChapterSlug }
                    mangaBookmarks = storage.getMangaBookmarks()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncDailyToCloudIfDue(force: Boolean = false) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
            ?: userProfile.id?.takeIf { it.isNotBlank() && it != "guest" }
            ?: supabaseRepo.getCurrentUserId()
            ?: return
        val now = System.currentTimeMillis()
        val lastSync = storage.getLastCloudSyncTimestamp()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (!force && (now - lastSync < oneDayMs)) {
            return
        }
        storage.setLastCloudSyncTimestamp(now)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncToCloud(currentUserId)
                performSyncTimeSpentAndReadingToCloud(currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Sync a single watchlist change to cloud (now throttled to daily interval) */
    private fun syncWatchlistItemToCloud(animeId: Int) {
        syncDailyToCloudIfDue(force = false)
    }

    /** Sync a single history change to cloud (now throttled to daily interval) */
    private fun syncHistoryItemToCloud(animeId: Int) {
        syncDailyToCloudIfDue(force = false)
    }

    private fun deleteHistoryItemFromCloud(animeId: Int) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
            ?: userProfile.id?.takeIf { it.isNotBlank() && it != "guest" }
            ?: supabaseRepo.getCurrentUserId()
            ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepo.deleteWatchHistory(currentUserId, animeId)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to delete watch history from cloud", e)
            }
        }
    }

    private fun deleteWatchlistItemFromCloud(animeId: Int) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
            ?: userProfile.id?.takeIf { it.isNotBlank() && it != "guest" }
            ?: supabaseRepo.getCurrentUserId()
            ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepo.deleteWatchlistItem(currentUserId, animeId)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to delete watchlist item from cloud", e)
            }
        }
    }

    // --- Cloud Sync for Time Spent & Manga / Novel Reading Data ---
    fun syncTimeSpentAndReadingToCloud(force: Boolean = false) {
        syncDailyToCloudIfDue(force = force)
    }

    private suspend fun performSyncTimeSpentAndReadingToCloud(currentUserId: String) {
        try {
            val record = storage.getTimeSpentRecord(currentUserId)
            val mangaHistJson = org.json.JSONArray().apply {
                storage.getMangaHistory().take(25).forEach { item ->
                    put(org.json.JSONObject().apply {
                        put("mangaId", item.mangaId)
                        put("title", item.title)
                        put("poster", item.poster ?: "")
                        put("type", item.type ?: "Manga")
                        put("chapterId", item.chapterId)
                        put("chapterNumber", item.chapterNumber)
                        put("chapterTitle", item.chapterTitle ?: "")
                        put("pageIndex", item.pageIndex)
                        put("pageCount", item.pageCount)
                        put("updatedAt", item.updatedAt)
                    })
                }
            }.toString()

            val novelHistJson = org.json.JSONArray().apply {
                storage.getNovelHistory().take(20).forEach { item ->
                    put(org.json.JSONObject().apply {
                        put("slug", item.slug)
                        put("title", item.title)
                        put("cover", item.cover)
                        put("lastChapterSlug", item.lastChapterSlug)
                        put("lastChapterTitle", item.lastChapterTitle)
                        put("timestamp", item.timestamp)
                    })
                }
            }.toString()

            val mangaBookmarksJson = org.json.JSONArray().apply {
                storage.getMangaBookmarks().take(25).forEach { item ->
                    put(org.json.JSONObject().apply {
                        put("mangaId", item.mangaId)
                        put("title", item.title)
                        put("poster", item.poster ?: "")
                        put("type", item.type ?: "Manga")
                        put("latestChapter", item.latestChapter ?: "")
                        put("updatedAt", item.updatedAt)
                    })
                }
            }.toString()

            val cloudP = cloudProfile
            val todayStr = com.nyyrox.aniverse.data.TimeSpentRecord.getTodayDateString()
            val isCloudToday = cloudP?.lastActiveDate == todayStr

            val finalAnime = maxOf(record.allTime.animeSeconds, cloudP?.timeSpentAnime ?: 0L)
            val finalManga = maxOf(record.allTime.mangaSeconds, cloudP?.timeSpentManga ?: 0L)
            val finalNovel = maxOf(record.allTime.novelSeconds, cloudP?.timeSpentNovel ?: 0L)
            val finalApp = maxOf(record.allTime.appSeconds, cloudP?.timeSpentApp ?: 0L)
            val finalTotal = finalAnime + finalManga + finalNovel + finalApp

            val localDailyMedia = (record.daily.animeSeconds + record.daily.mangaSeconds + record.daily.novelSeconds).coerceAtLeast(0L)
            val localWeeklyMedia = (record.weekly.animeSeconds + record.weekly.mangaSeconds + record.weekly.novelSeconds).coerceAtLeast(0L)
            val cloudMediaTotal = (finalAnime + finalManga + finalNovel).coerceAtLeast(0L)

            val finalDaily = minOf(
                if (isCloudToday) maxOf(localDailyMedia, minOf(cloudP?.dailyTimeSpent ?: 0L, cloudMediaTotal)) else localDailyMedia,
                cloudMediaTotal
            )
            val finalWeekly = minOf(
                maxOf(localWeeklyMedia, if (isCloudToday) minOf(cloudP?.weeklyTimeSpent ?: 0L, cloudMediaTotal) else 0L),
                cloudMediaTotal
            )

            supabaseRepo.updateProfile(
                userId = currentUserId,
                timeSpentAnime = finalAnime,
                timeSpentManga = finalManga,
                timeSpentNovel = finalNovel,
                timeSpentApp = finalApp,
                timeSpentTotal = finalTotal,
                weeklyTimeSpent = finalWeekly,
                dailyTimeSpent = finalDaily,
                mangaHistoryJson = mangaHistJson,
                novelHistoryJson = novelHistJson,
                mangaBookmarksJson = mangaBookmarksJson,
                isWatchlistPrivate = userProfile.isWatchlistPrivate,
                isHistoryPrivate = userProfile.isWatchHistoryPrivate,
                isMangaPrivate = userProfile.isMangaPrivate,
                isNovelPrivate = userProfile.isNovelPrivate
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resets active usage and time spent statistics back to 0 for the active account (locally and on cloud).
     * Useful if cross-account contamination occurred on previous logins.
     */
    fun resetTimeSpentStats(onComplete: (() -> Unit)? = null) {
        val currentUserId = (authState as? AuthState.LoggedIn)?.userId
        viewModelScope.launch {
            val cleanRecord = TimeSpentRecord().checkAndReset()
            storage.saveTimeSpentRecord(cleanRecord, currentUserId)
            timeSpentRecord = cleanRecord
            userProfile = userProfile.copy(
                timeSpentAnime = 0L,
                timeSpentManga = 0L,
                timeSpentNovel = 0L,
                timeSpentApp = 0L,
                timeSpentTotal = 0L
            )
            storage.saveUserProfile(userProfile)
            if (!currentUserId.isNullOrBlank()) {
                try {
                    supabaseRepo.updateProfile(
                        userId = currentUserId,
                        timeSpentAnime = 0L,
                        timeSpentManga = 0L,
                        timeSpentNovel = 0L,
                        timeSpentApp = 0L,
                        timeSpentTotal = 0L,
                        weeklyTimeSpent = 0L,
                        dailyTimeSpent = 0L
                    )
                    cloudProfile = cloudProfile?.copy(
                        timeSpentAnime = 0L,
                        timeSpentManga = 0L,
                        timeSpentNovel = 0L,
                        timeSpentApp = 0L,
                        timeSpentTotal = 0L,
                        weeklyTimeSpent = 0L,
                        dailyTimeSpent = 0L
                    )
                } catch (_: Exception) {}
            }
            onComplete?.invoke()
        }
    }

    fun updatePrivacySettings(
        isWatchlistPrivate: Boolean? = null,
        isWatchHistoryPrivate: Boolean? = null,
        isMangaPrivate: Boolean? = null,
        isNovelPrivate: Boolean? = null
    ) {
        val updated = userProfile.copy(
            isWatchlistPrivate = isWatchlistPrivate ?: userProfile.isWatchlistPrivate,
            isWatchHistoryPrivate = isWatchHistoryPrivate ?: userProfile.isWatchHistoryPrivate,
            isMangaPrivate = isMangaPrivate ?: userProfile.isMangaPrivate,
            isNovelPrivate = isNovelPrivate ?: userProfile.isNovelPrivate
        )
        userProfile = updated
        storage.saveUserProfile(updated)

        val currentUserId = (authState as? AuthState.LoggedIn)?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                supabaseRepo.updateProfile(
                    userId = currentUserId,
                    isWatchlistPrivate = updated.isWatchlistPrivate,
                    isHistoryPrivate = updated.isWatchHistoryPrivate,
                    isMangaPrivate = updated.isMangaPrivate,
                    isNovelPrivate = updated.isNovelPrivate
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun parsePublicMangaHistory(json: String?): List<MangaHistoryItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        MangaHistoryItem(
                            mangaId = obj.optString("mangaId"),
                            title = obj.optString("title"),
                            poster = obj.optString("poster").takeIf { it.isNotBlank() },
                            type = obj.optString("type", "Manga"),
                            chapterId = obj.optString("chapterId", ""),
                            chapterNumber = obj.optString("chapterNumber", ""),
                            chapterTitle = obj.optString("chapterTitle").takeIf { it.isNotBlank() },
                            pageIndex = obj.optInt("pageIndex", 0),
                            pageCount = obj.optInt("pageCount", 0),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parsePublicNovelHistory(json: String?): List<com.nyyrox.aniverse.data.novel.NovelHistoryItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        com.nyyrox.aniverse.data.novel.NovelHistoryItem(
                            slug = obj.optString("slug"),
                            title = obj.optString("title"),
                            cover = obj.optString("cover"),
                            lastChapterSlug = obj.optString("lastChapterSlug"),
                            lastChapterTitle = obj.optString("lastChapterTitle"),
                            timestamp = obj.optLong("timestamp", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parsePublicMangaBookmarks(json: String?): List<com.nyyrox.aniverse.data.manga.MangaBookmarkItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        com.nyyrox.aniverse.data.manga.MangaBookmarkItem(
                            mangaId = obj.optString("mangaId"),
                            title = obj.optString("title"),
                            poster = obj.optString("poster").takeIf { it.isNotBlank() },
                            type = obj.optString("type", "Manga"),
                            latestChapter = obj.optString("latestChapter").takeIf { it.isNotBlank() },
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
