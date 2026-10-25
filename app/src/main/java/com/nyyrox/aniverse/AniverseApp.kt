package com.nyyrox.aniverse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyyrox.aniverse.data.StorageManager
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.ui.components.CompulsoryUsernameDialog
import com.nyyrox.aniverse.ui.components.OttBottomDock
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.LocalPrismalBackdrop
import com.nyyrox.aniverse.ui.components.OttNavDrawer
import com.nyyrox.aniverse.ui.components.OttTopBar
import com.nyyrox.aniverse.ui.components.rememberChromeScrollState
import com.nyyrox.aniverse.ui.screens.AiringScreen
import com.nyyrox.aniverse.ui.screens.AvatarPickerScreen
import com.nyyrox.aniverse.ui.screens.BannerPickerScreen
import com.nyyrox.aniverse.ui.screens.BrowseScreen
import com.nyyrox.aniverse.ui.screens.SearchScreen
import com.nyyrox.aniverse.ui.screens.DecorationClosetScreen
import com.nyyrox.aniverse.ui.screens.DirectChatScreen
import com.nyyrox.aniverse.ui.screens.DiscordHubScreen
import com.nyyrox.aniverse.ui.screens.DetailScreen
import com.nyyrox.aniverse.ui.screens.DownloadsScreen
import com.nyyrox.aniverse.ui.screens.HomeScreen
import com.nyyrox.aniverse.ui.screens.LoginScreen
import com.nyyrox.aniverse.ui.screens.MangaDetailScreen
import com.nyyrox.aniverse.ui.screens.MangaHomeScreen
import com.nyyrox.aniverse.ui.screens.MangaReaderScreen
import com.nyyrox.aniverse.ui.screens.NotificationScreen
import com.nyyrox.aniverse.ui.screens.NovelDetailScreen
import com.nyyrox.aniverse.ui.screens.NovelHomeScreen
import com.nyyrox.aniverse.ui.screens.NovelReaderScreen
import com.nyyrox.aniverse.ui.screens.PlayerScreen
import com.nyyrox.aniverse.ui.screens.ProfileScreen
import com.nyyrox.aniverse.ui.screens.PublicProfileScreen
import com.nyyrox.aniverse.ui.screens.WatchTogetherScreen
import com.nyyrox.aniverse.ui.screens.WatchlistScreen
import com.nyyrox.aniverse.ui.screens.ZenkaiSplashScreen
import com.nyyrox.aniverse.ui.screens.ZenkaiUpdateDialog
import com.nyyrox.aniverse.ui.screens.ZenkaiRelease
import com.nyyrox.aniverse.ui.screens.isNewerVersion
import com.nyyrox.aniverse.ui.animation.LocalSharedPosterCoordinator
import com.nyyrox.aniverse.ui.animation.SharedPosterOverlay
import com.nyyrox.aniverse.ui.animation.SharedTransitionPhase
import com.nyyrox.aniverse.ui.animation.rememberSharedPosterCoordinator
import com.nyyrox.aniverse.ui.theme.AniverseTheme
import com.nyyrox.aniverse.ui.theme.ThemeEngineProvider
import com.nyyrox.aniverse.ui.theme.getThemeColors
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App(vm: MainViewModel = viewModel(), deepLinkUri: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME,
                Lifecycle.Event.ON_START -> {
                    vm.setAppForeground(true)
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    vm.setAppForeground(false)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    // Reactively enforce refresh rate across the entire app
    LaunchedEffect(vm.themeSettings.preferredRefreshRate, vm.themeSettings.isPowerSavingEnabled) {
        activity?.let { act ->
            com.nyyrox.aniverse.util.DisplayRefreshRateManager.applyRefreshRate(
                act,
                vm.themeSettings.preferredRefreshRate,
                vm.themeSettings.isPowerSavingEnabled
            )
        }
    }

    var navStack by remember { mutableStateOf(listOf("home")) }
    val currentRoute = navStack.last()
    // Scroll direction of the visible screen drives the collapsing top bar and bottom dock.
    val chrome = rememberChromeScrollState()
    LaunchedEffect(currentRoute) { chrome.reset() }
    var isDrawerOpen by remember { mutableStateOf(false) }
    val sharedPosterCoordinator = rememberSharedPosterCoordinator()

    val isDetailOverlayOpen = vm.activeDetailAnimeId != null || vm.activeDetailMangaId != null || vm.activeDetailNovelSlug != null
    val isDetailRoute = currentRoute.startsWith("detail:") || currentRoute.startsWith("manga_detail:") || currentRoute.startsWith("novel_detail:")
    val isPlayerRoute = currentRoute.startsWith("player:") || currentRoute.startsWith("manga_reader:") || currentRoute.startsWith("novel_reader:")
    val isRootRoute = listOf("home", "airing", "manga", "novel", "profile").contains(currentRoute) && !isDetailOverlayOpen && !isPlayerRoute

    val detailId = if (currentRoute.startsWith("detail:")) {
        currentRoute.removePrefix("detail:").toIntOrNull() ?: 0
    } else 0

    val playerParts = if (currentRoute.startsWith("player:")) {
        currentRoute.removePrefix("player:").split(":")
    } else emptyList()

    val playerId = playerParts.getOrNull(0)?.toIntOrNull() ?: 0
    val playerEpisode = playerParts.getOrNull(1)?.toIntOrNull() ?: 1
    val playerProviderId = playerParts.getOrNull(2)

    val mangaReaderParts = if (currentRoute.startsWith("manga_reader:")) {
        val payload = currentRoute.removePrefix("manga_reader:")
        val lastColon = payload.lastIndexOf(':')
        if (lastColon > 0) {
            listOf(payload.substring(0, lastColon), payload.substring(lastColon + 1))
        } else {
            listOf(payload, "")
        }
    } else emptyList()
    val mangaReaderId = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaReaderParts.getOrNull(0).orEmpty())
    val mangaReaderChapterId = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(mangaReaderParts.getOrNull(1).orEmpty())

    val novelReaderParts = if (currentRoute.startsWith("novel_reader:")) {
        val payload = currentRoute.removePrefix("novel_reader:")
        val lastColon = payload.lastIndexOf(':')
        if (lastColon > 0) {
            listOf(payload.substring(0, lastColon), payload.substring(lastColon + 1))
        } else {
            listOf(payload, "")
        }
    } else emptyList()
    val novelReaderSlug = novelReaderParts.getOrNull(0).orEmpty()
    val novelReaderChapterSlug = novelReaderParts.getOrNull(1).orEmpty()

    var playerOpenedFromDetail by remember { mutableStateOf(false) }

    fun push(route: String) {
        if (!route.startsWith("player:")) {
            playerOpenedFromDetail = false
        }
        navStack = navStack + route
    }

    fun pop() {
        if (navStack.size > 1) {
            navStack = navStack.dropLast(1)
        }
    }

    // Mini player "expand" -> open the full player for the parked episode
    DisposableEffect(Unit) {
        com.nyyrox.aniverse.player.MiniPlayerManager.onExpand = { id, ep, prov ->
            if (!navStack.last().startsWith("player:")) {
                playerOpenedFromDetail = false
                push("player:$id:$ep:${prov.orEmpty()}")
            }
        }
        onDispose { com.nyyrox.aniverse.player.MiniPlayerManager.onExpand = null }
    }

    fun switchTab(tab: String) {
        if (vm.activeDetailAnimeId != null) vm.closeAnimeDetail()
        if (vm.activeDetailMangaId != null) vm.closeMangaDetail()
        if (vm.activeDetailNovelSlug != null) vm.closeNovelDetail()
        if (navStack.lastOrNull() != tab) {
            navStack = listOf(tab)
            when (tab) {
                "home" -> if (vm.trending.isEmpty()) vm.loadHome()
                "manga" -> vm.loadMangaHome()
                "novel" -> vm.loadNovelHome()
                "browse" -> if (vm.browse.isEmpty()) vm.loadBrowse()
                "airing" -> if (vm.airing.isEmpty()) vm.loadAiring()
                "profile" -> vm.refreshStorageData()
                "watchlist" -> vm.refreshStorageData()
            }
        }
    }

    BackHandler(enabled = isPlayerRoute || isDetailOverlayOpen || navStack.size > 1 || currentRoute != "home") {
        if (isPlayerRoute) {
            if (!playerOpenedFromDetail && vm.activeDetailAnimeId != null) {
                vm.closeAnimeDetail()
            }
            pop()
        } else if (vm.activeDetailAnimeId != null) {
            if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                sharedPosterCoordinator.returnBack { vm.closeAnimeDetail() }
            } else {
                vm.closeAnimeDetail()
            }
        } else if (vm.activeDetailMangaId != null) {
            if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                sharedPosterCoordinator.returnBack { vm.closeMangaDetail() }
            } else {
                vm.closeMangaDetail()
            }
        } else if (vm.activeDetailNovelSlug != null) {
            if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                sharedPosterCoordinator.returnBack { vm.closeNovelDetail() }
            } else {
                vm.closeNovelDetail()
            }
        } else if (currentRoute.startsWith("detail:") && (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD)) {
            sharedPosterCoordinator.returnBack { pop() }
        } else if (navStack.size > 1) {
            pop()
        } else if (currentRoute != "home") {
            switchTab("home")
        }
    }

    var pendingUpdateRelease by remember { mutableStateOf<ZenkaiRelease?>(null) }

    LaunchedEffect(Unit) {
        vm.loadInitial()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = java.net.URL("https://api.github.com/repos/Nyyrox/Zenkai/releases/latest").readText()
                val obj = org.json.JSONObject(json)
                val tag = obj.optString("tag_name", "")
                val name = obj.optString("name", tag)
                val body = obj.optString("body", "")
                val publishedAt = obj.optString("published_at", "")
                val date = if (publishedAt.length >= 10) publishedAt.substring(0, 10) else ""
                val assets = obj.optJSONArray("assets")
                var dlUrl: String? = null
                if (assets != null && assets.length() > 0) {
                    dlUrl = assets.getJSONObject(0).optString("browser_download_url").ifBlank { null }
                }
                val currentVer = com.nyyrox.aniverse.BuildConfig.VERSION_NAME
                if (tag.isNotBlank() && isNewerVersion(tag, currentVer)) {
                    pendingUpdateRelease = ZenkaiRelease(tag, name, body, date, dlUrl)
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(deepLinkUri) {
        if (!deepLinkUri.isNullOrBlank()) {
            try {
                // If URL contains login-callback, let existing auth handler own it and resume pending share
                if (deepLinkUri.contains("login-callback")) {
                    vm.checkExistingSession()
                    val storageManager = StorageManager(context)
                    val pending = storageManager.getPendingShare()
                    if (!pending.isNullOrBlank()) {
                        storageManager.setPendingShare(null)
                        push(pending)
                    } else {
                        switchTab("profile")
                    }
                    return@LaunchedEffect
                }

                val uri = android.net.Uri.parse(deepLinkUri)
                val scheme = uri.scheme?.lowercase(java.util.Locale.US)
                val host = uri.host?.lowercase(java.util.Locale.US)
                val segments = uri.pathSegments

                // Check share links: https://<host>/s/... or aniverse://s/...
                // (for the custom scheme the path is "/" + url.host + url.pathname)
                val normalizedPath = when {
                    scheme == "aniverse" && host == "s" -> "/s" + (uri.path ?: "")
                    scheme == "https" && (host == "zenkai-auth-worker.ammhfoo.workers.dev" || uri.path?.startsWith("/s/") == true) -> uri.path.orEmpty()
                    else -> null
                }

                val shareMatch = normalizedPath?.let { Regex("^/s/(anime|manga|novel)/([\\w-]{1,64})/?$").find(it) }
                if (shareMatch != null) {
                    val kind = shareMatch.groupValues[1]
                    val id = shareMatch.groupValues[2]
                    val epParam = uri.getQueryParameter("ep")?.takeIf { it.matches(Regex("^\\d{1,5}$")) }?.toIntOrNull()
                    val chParam = uri.getQueryParameter("ch")?.takeIf { it.matches(Regex("^\\d{1,5}$")) }?.toIntOrNull()

                    val targetRoute = when (kind) {
                        "anime" -> if (epParam != null && epParam > 0) "player:$id:$epParam:" else "detail:$id"
                        "manga" -> "manga_detail:$id"
                        "novel" -> "novel_detail:$id"
                        else -> null
                    }

                    if (targetRoute != null) {
                        val hasSession = vm.authState is AuthState.LoggedIn || (vm.userProfile.id?.takeIf { it.isNotBlank() && it != "guest" } != null)
                        if (hasSession) {
                            if (currentRoute != targetRoute) {
                                push(targetRoute)
                            }
                        } else {
                            StorageManager(context).setPendingShare(targetRoute)
                            push("login")
                        }
                    }
                    return@LaunchedEffect
                }

                val targetRoute = when {
                    scheme == "zenkai" || scheme == "aniverse" -> {
                        val targetType = host ?: segments.firstOrNull()
                        val targetId = if (host != null) segments.firstOrNull() else segments.getOrNull(1)
                        if (targetType == "login-callback" || targetType == "callback" || targetType == "auth") {
                            vm.checkExistingSession()
                            val storageManager = StorageManager(context)
                            val pending = storageManager.getPendingShare()
                            if (!pending.isNullOrBlank()) {
                                storageManager.setPendingShare(null)
                                push(pending)
                            } else {
                                switchTab("profile")
                            }
                            null
                        } else if (targetType == "anilist-auth") {
                            val fragment = uri.fragment.orEmpty()
                            val token = if (fragment.contains("access_token=")) {
                                fragment.substringAfter("access_token=").substringBefore("&")
                            } else {
                                uri.getQueryParameter("access_token")
                            }
                            if (!token.isNullOrBlank()) {
                                vm.handleAniListAuthToken(token)
                                switchTab("profile")
                            }
                            null
                        } else if (targetType == "mal-auth") {
                            val code = uri.getQueryParameter("code")
                            if (!code.isNullOrBlank()) {
                                vm.handleMalAuthCode(code)
                                switchTab("profile")
                            }
                            null
                        } else if (targetType == "notifications") {
                            "notifications"
                        } else if (targetType == "community") {
                            "community"
                        } else if (targetType == "rewards" || targetType == "reward" || targetType == "profile") {
                            switchTab("profile")
                            null
                        } else if (targetType == "downloads") {
                            "downloads"
                        } else if ((targetType == "direct_chat" || targetType == "dm") && !targetId.isNullOrBlank()) {
                            "direct_chat:$targetId"
                        } else if (targetType == "anime" && targetId?.toIntOrNull() != null) {
                            "detail:$targetId"
                        } else if (targetType == "manga" && !targetId.isNullOrBlank()) {
                            "manga_detail:$targetId"
                        } else if (targetType == "novel" && !targetId.isNullOrBlank()) {
                            "novel_detail:$targetId"
                        } else null
                    }
                    segments.isNotEmpty() && (segments[0] == "login-callback" || segments[0] == "callback") -> {
                        vm.checkExistingSession()
                        val storageManager = StorageManager(context)
                        val pending = storageManager.getPendingShare()
                        if (!pending.isNullOrBlank()) {
                            storageManager.setPendingShare(null)
                            push(pending)
                        } else {
                            switchTab("profile")
                        }
                        null
                    }
                    segments.size >= 2 -> {
                        val section = segments[0].lowercase(java.util.Locale.US)
                        val id = segments[1]
                        if (section == "anime" && id.toIntOrNull() != null) {
                            "detail:$id"
                        } else if (section == "manga" && id.isNotBlank()) {
                            "manga_detail:$id"
                        } else if (section == "novel" && id.isNotBlank()) {
                            "novel_detail:$id"
                        } else null
                    }
                    deepLinkUri.startsWith("detail:") || deepLinkUri.startsWith("manga_detail:") ||
                    deepLinkUri.startsWith("novel_detail:") || deepLinkUri.startsWith("direct_chat:") -> deepLinkUri
                    deepLinkUri == "community" || deepLinkUri == "notifications" || deepLinkUri == "downloads" -> deepLinkUri
                    deepLinkUri == "profile" -> {
                        switchTab("profile")
                        null
                    }
                    else -> null
                }

                if (targetRoute != null && currentRoute != targetRoute) {
                    push(targetRoute)
                }
            } catch (_: Exception) {
                // Ignore malformed URI
            }
        }
    }

    ThemeEngineProvider(settings = vm.themeSettings) {
        val currentThemeColors = getThemeColors(vm.themeSettings.theme)
        val isReducedMotion = vm.themeSettings.isReducedMotionEnabled
        val appBackdropLayer = com.styropyr0.prismal.sources.rememberPrismalGlassLayer()

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
            LocalSharedPosterCoordinator provides sharedPosterCoordinator,
            com.styropyr0.prismal.LocalPrismalBackdrop provides appBackdropLayer
        ) {
            AniverseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = currentThemeColors.bg
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Shared-Element Floating Poster Transition Overlay
                    SharedPosterOverlay(coordinator = sharedPosterCoordinator)

                    // Main View Screen Container (Edge-to-edge behind status bar)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(chrome.connection)
                            .prismalGlassLayer(appBackdropLayer)
                    ) {
                        AnimatedContent(
                            targetState = currentRoute,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                                    (fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                            },
                            label = "route_transition"
                        ) { screen ->
                            when {
                                screen == "splash" -> {
                                    LaunchedEffect(Unit) {
                                        navStack = listOf("home")
                                        if (vm.trending.isEmpty()) vm.loadHome()
                                    }
                                }

                                screen == "home" -> {
                                    HomeScreen(
                                        vm = vm,
                                        onAnimeClick = { vm.openAnimeDetail(it) },
                                        onPlayClick = { id, ep -> push("player:$id:$ep:") },
                                        onMangaClick = { vm.openMangaDetail(it) },
                                        onMangaReadClick = { mId, chId ->
                                            val cm = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mId)
                                            val cc = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chId)
                                            push("manga_reader:$cm:$cc")
                                        },
                                        onNovelClick = { vm.openNovelDetail(it) },
                                        onNovelReadClick = { nSlug, chSlug -> push("novel_reader:$nSlug:$chSlug") },
                                        onDownloadsClick = { push("downloads") }
                                    )
                                }

                                screen == "manga" -> {
                                    MangaHomeScreen(
                                        vm = vm,
                                        onMangaClick = { vm.openMangaDetail(it) },
                                        onReadClick = { mId, chId ->
                                            val cm = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mId)
                                            val cc = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chId)
                                            push("manga_reader:$cm:$cc")
                                        },
                                        onDownloadsClick = { push("downloads") }
                                    )
                                }

                                screen == "novel" -> {
                                    NovelHomeScreen(
                                        vm = vm,
                                        onNovelClick = { vm.openNovelDetail(it) },
                                        onReadClick = { nSlug, chSlug -> push("novel_reader:$nSlug:$chSlug") },
                                        onDownloadsClick = { push("downloads") }
                                    )
                                }

                                screen == "browse" -> {
                                    BrowseScreen(
                                        vm = vm,
                                        onBack = if (navStack.size > 1 && navStack.last() == "browse") { { pop() } } else null,
                                        onAnimeClick = { vm.openAnimeDetail(it) }
                                    )
                                }

                                screen == "search" -> {
                                    SearchScreen(
                                        vm = vm,
                                        onBack = { pop() },
                                        onAnimeClick = { vm.openAnimeDetail(it) }
                                    )
                                }

                                screen == "airing" -> {
                                    AiringScreen(
                                        vm = vm,
                                        onAnimeClick = { vm.openAnimeDetail(it) }
                                    )
                                }

                                screen == "profile" -> {
                                    ProfileScreen(
                                        vm = vm,
                                        onAnimeClick = { vm.openAnimeDetail(it) },
                                        onPlayEpisode = { id, ep -> push("player:$id:$ep:") },
                                        onMangaClick = { vm.openMangaDetail(it) },
                                        onOpenSavedManga = { push("watchlist:manga") },
                                        onOpenNovelHistory = { push("watchlist:novel") },
                                        onBack = { switchTab("home") },
                                        onSeeAllWatchlist = { push("watchlist") },
                                        onLogin = { push("login") },
                                        onAvatarPicker = { push("avatar_picker") },
                                        onBannerPicker = { push("banner_picker") },
                                        onOpenDecorations = { push("decorations") },
                                        onOpenNotifications = { push("notifications") },
                                        onOpenRankings = { push("community_rankings") },
                                        onOpenUserProfile = { push("public_profile:$it") }
                                    )
                                }

                                screen == "notifications" -> {
                                    NotificationScreen(
                                        vm = vm,
                                        onBack = { pop() },
                                        onNavigateToCommunity = { _ ->
                                            push("discord_hub")
                                        },
                                        onNavigateToDirectChat = { partnerId ->
                                            push("direct_chat:$partnerId")
                                        },
                                        onNavigateToLogin = { push("login") },
                                        onNavigate = { push(it) }
                                    )
                                }

                                screen == "downloads" -> {
                                    DownloadsScreen(
                                        vm = vm,
                                        onBack = { pop() },
                                        onPlayAnimeEpisode = { animeId, episode ->
                                            push("player:$animeId:$episode:")
                                        },
                                        onReadNovelChapter = { novelSlug, chapterSlug ->
                                            push("novel_reader:$novelSlug:$chapterSlug")
                                        },
                                        onReadMangaChapter = { mangaId, chapterId ->
                                            val cm = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mangaId)
                                            val cc = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chapterId)
                                            push("manga_reader:$cm:$cc")
                                        }
                                    )
                                }

                                screen == "decorations" -> {
                                    DecorationClosetScreen(
                                        vm = vm,
                                        onBack = { pop() }
                                    )
                                }

                                screen == "discord_hub" || screen == "community" || screen == "discord" || screen == "community_rankings" || screen == "rankings" -> {
                                    DiscordHubScreen(
                                        onBack = { pop() }
                                    )
                                }

                                screen.startsWith("direct_chat:") -> {
                                    val targetUserId = screen.removePrefix("direct_chat:")
                                    DirectChatScreen(
                                        partnerUserId = targetUserId,
                                        vm = vm,
                                        onBack = { pop() },
                                        onUserProfileClick = { push("public_profile:$it") },
                                        onAnimeClick = { vm.openAnimeDetail(it) }
                                    )
                                }

                                screen.startsWith("public_profile:") -> {
                                    val targetUserId = screen.removePrefix("public_profile:")
                                    PublicProfileScreen(
                                        userId = targetUserId,
                                        vm = vm,
                                        onBack = { pop() },
                                        onAnimeClick = { vm.openAnimeDetail(it) },
                                        onMangaClick = { vm.openMangaDetail(it) },
                                        onNovelClick = { vm.openNovelDetail(it) },
                                        onDirectChatClick = { push("direct_chat:$it") }
                                    )
                                }

                                screen == "watchtogether" || screen.startsWith("watchtogether:") -> {
                                    val roomId = screen.removePrefix("watchtogether:").takeIf { it != "watchtogether" && it.isNotBlank() }
                                    WatchTogetherScreen(
                                        vm = vm,
                                        onBack = { pop() },
                                        initialRoomId = roomId
                                    )
                                }

                                screen == "login" -> {
                                    LoginScreen(
                                        vm = vm,
                                        onLoginSuccess = {
                                            val pending = StorageManager(context).getPendingShare()
                                            if (!pending.isNullOrBlank()) {
                                                StorageManager(context).setPendingShare(null)
                                                push(pending)
                                            } else {
                                                pop()
                                            }
                                        },
                                        onContinueAsGuest = {
                                            val pending = StorageManager(context).getPendingShare()
                                            if (!pending.isNullOrBlank()) {
                                                StorageManager(context).setPendingShare(null)
                                                push(pending)
                                            } else {
                                                pop()
                                            }
                                        }
                                    )
                                }

                                screen == "avatar_picker" -> {
                                    AvatarPickerScreen(
                                        currentAvatarUrl = vm.userProfile.avatarUrl,
                                        onAvatarSelected = { url ->
                                            vm.updateCloudProfile(avatarUrl = url)
                                            pop()
                                        },
                                        onBack = { pop() }
                                    )
                                }

                                screen == "banner_picker" -> {
                                    BannerPickerScreen(
                                        currentBannerUrl = vm.userProfile.bannerUrl,
                                        onBannerSelected = { url ->
                                            vm.updateCloudProfile(bannerUrl = url)
                                            pop()
                                        },
                                        onBack = { pop() }
                                    )
                                }

                                screen.startsWith("watchlist") -> {
                                    val initialTab = when {
                                        screen.contains(":manga_history") -> 3
                                        screen.contains(":manga") -> 2
                                        screen.contains(":anime_history") -> 1
                                        screen.contains(":novel") -> 4
                                        else -> 0
                                    }
                                    WatchlistScreen(
                                        vm = vm,
                                        initialTab = initialTab,
                                        onAnimeClick = { vm.openAnimeDetail(it) },
                                        onPlay = { id, ep -> push("player:$id:$ep:") },
                                        onMangaClick = { vm.openMangaDetail(it) },
                                        onReadManga = { mId, chId ->
                                            val cm = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mId)
                                            val cc = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chId)
                                            push("manga_reader:$cm:$cc")
                                        },
                                        onNovelClick = { vm.openNovelDetail(it) },
                                        onReadNovel = { nSlug, chSlug -> push("novel_reader:$nSlug:$chSlug") },
                                        onBack = if (navStack.size > 1) { { pop() } } else null
                                    )
                                }

                                screen.startsWith("detail:") -> {
                                    val id = detailId
                                    LaunchedEffect(id) {
                                        if (id > 0) vm.openAnimeDetail(id)
                                        val withoutDetail = navStack.filterNot { it.startsWith("detail:") }
                                        navStack = if (withoutDetail.isNotEmpty()) withoutDetail else listOf("home")
                                    }
                                }

                                screen.startsWith("manga_detail:") -> {
                                    val mId = screen.removePrefix("manga_detail:")
                                    LaunchedEffect(mId) {
                                        if (mId.isNotBlank()) vm.openMangaDetail(mId)
                                        val withoutDetail = navStack.filterNot { it.startsWith("manga_detail:") }
                                        navStack = if (withoutDetail.isNotEmpty()) withoutDetail else listOf("manga")
                                    }
                                }

                                screen.startsWith("novel_detail:") -> {
                                    val nSlug = screen.removePrefix("novel_detail:")
                                    LaunchedEffect(nSlug) {
                                        if (nSlug.isNotBlank()) vm.openNovelDetail(nSlug)
                                        val withoutDetail = navStack.filterNot { it.startsWith("novel_detail:") }
                                        navStack = if (withoutDetail.isNotEmpty()) withoutDetail else listOf("novel")
                                    }
                                }

                                screen.startsWith("player:") ||
                                screen.startsWith("manga_reader:") ||
                                screen.startsWith("novel_reader:") -> {
                                    // Fullscreen media player and readers are rendered in the dedicated top layer above detail overlays
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        // ── IN-PLACE DETAIL OVERLAYS (Continuous, zero unmounting) ──
                        // 1. Anime Detail Overlay
                        val activeAnimeId = vm.activeDetailAnimeId
                        AnimatedVisibility(
                            visible = activeAnimeId != null,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(0))
                        ) {
                            if (activeAnimeId != null) {
                                DetailScreen(
                                    vm = vm,
                                    animeId = activeAnimeId,
                                    onBack = {
                                        if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                                            sharedPosterCoordinator.returnBack { vm.closeAnimeDetail() }
                                        } else {
                                            vm.closeAnimeDetail()
                                        }
                                    },
                                    onPlay = { id, ep, prov ->
                                        playerOpenedFromDetail = true
                                        push("player:$id:$ep:${prov.id}")
                                    },
                                    onWatchTogether = { anime, ep ->
                                        vm.createWatchTogetherRoom(
                                            name = "${anime.title} Watch Party",
                                            anime = anime,
                                            episode = ep
                                        ) { room ->
                                            if (room != null) {
                                                vm.closeAnimeDetail()
                                                push("watchtogether:${room.id}")
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // 2. Manga Detail Overlay
                        val activeMangaId = vm.activeDetailMangaId
                        AnimatedVisibility(
                            visible = activeMangaId != null,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(0))
                        ) {
                            if (activeMangaId != null) {
                                MangaDetailScreen(
                                    vm = vm,
                                    mangaId = activeMangaId,
                                    onBack = {
                                        if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                                            sharedPosterCoordinator.returnBack { vm.closeMangaDetail() }
                                        } else {
                                            vm.closeMangaDetail()
                                        }
                                    },
                                    onReadChapter = { mId, chId ->
                                        val cm = com.nyyrox.aniverse.data.manga.MangaRepository.cleanWeebId(mId)
                                        val cc = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(chId)
                                        push("manga_reader:$cm:$cc")
                                    },
                                    onAnimeClick = { vm.openAnimeDetail(it) }
                                )
                            }
                        }

                        // 3. Novel Detail Overlay
                        val activeNovelSlug = vm.activeDetailNovelSlug
                        AnimatedVisibility(
                            visible = activeNovelSlug != null,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(0))
                        ) {
                            if (activeNovelSlug != null) {
                                NovelDetailScreen(
                                    vm = vm,
                                    novelSlug = activeNovelSlug,
                                    onBack = {
                                        if (sharedPosterCoordinator.phase == SharedTransitionPhase.PRESENTED || sharedPosterCoordinator.phase == SharedTransitionPhase.FORWARD) {
                                            sharedPosterCoordinator.returnBack { vm.closeNovelDetail() }
                                        } else {
                                            vm.closeNovelDetail()
                                        }
                                    },
                                    onReadChapter = { nSlug, chSlug ->
                                        push("novel_reader:$nSlug:$chSlug")
                                    }
                                )
                            }
                        }

                        // ── FULLSCREEN MEDIA PLAYER & READERS (Topmost Layer - Rendered above Detail Overlays) ──
                        // 1. CinePlayer Screen
                        val isPlayerActive = currentRoute.startsWith("player:")
                        AnimatedVisibility(
                            visible = isPlayerActive,
                            enter = fadeIn(tween(220)),
                            exit = androidx.compose.animation.ExitTransition.None
                        ) {
                            if (isPlayerActive) {
                                PlayerScreen(
                                    vm = vm,
                                    animeId = playerId,
                                    initialEpisode = playerEpisode,
                                    initialProviderId = playerProviderId,
                                    onBack = {
                                        if (!playerOpenedFromDetail && vm.activeDetailAnimeId != null) {
                                            vm.closeAnimeDetail()
                                        }
                                        pop()
                                    }
                                )
                            }
                        }

                        // 2. Manga Reader Screen
                        val isMangaReaderActive = currentRoute.startsWith("manga_reader:")
                        AnimatedVisibility(
                            visible = isMangaReaderActive,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200))
                        ) {
                            if (isMangaReaderActive) {
                                MangaReaderScreen(
                                    vm = vm,
                                    mangaId = mangaReaderId,
                                    chapterId = mangaReaderChapterId,
                                    onBack = {
                                        vm.refreshStorageData()
                                        val withoutReader = navStack.filterNot { it.startsWith("manga_reader:") }
                                        navStack = if (withoutReader.isNotEmpty()) withoutReader else listOf("manga")
                                    },
                                     onSelectChapter = { newCh ->
                                         val cleanCh = com.nyyrox.aniverse.data.manga.MangaRepository.cleanChapterId(newCh)
                                         val withoutReader = navStack.filterNot { it.startsWith("manga_reader:") }
                                         navStack = withoutReader + "manga_reader:$mangaReaderId:$cleanCh"
                                     }
                                )
                            }
                        }

                        // 3. Novel Reader Screen
                        val isNovelReaderActive = currentRoute.startsWith("novel_reader:")
                        AnimatedVisibility(
                            visible = isNovelReaderActive,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200))
                        ) {
                            if (isNovelReaderActive) {
                                NovelReaderScreen(
                                    vm = vm,
                                    novelSlug = novelReaderSlug,
                                    chapterSlug = novelReaderChapterSlug,
                                    onBack = {
                                        val withoutReader = navStack.filterNot { it.startsWith("novel_reader:") }
                                        navStack = if (withoutReader.isNotEmpty()) withoutReader else listOf("novel")
                                    },
                                    onSelectChapter = { newCh ->
                                        val withoutReader = navStack.filterNot { it.startsWith("novel_reader:") }
                                        navStack = withoutReader + "novel_reader:$novelReaderSlug:$newCh"
                                    }
                                )
                            }
                        }
                    }

                    // Transparent Dark Gradient Top Bar overlay (Only on home, manga, novel - NOT profile or airing)
                    if (isRootRoute && currentRoute != "profile" && currentRoute != "browse") {
                        OttTopBar(
                            // Schedule has its own header: keep only the profile bubble there.
                            expanded = chrome.topExpanded && currentRoute != "airing",
                            userAvatarUrl = vm.userProfile.avatarUrl,
                            avatarDecorationId = vm.userProfile.avatarDecorationId,
                            avatarFrameId = vm.userProfile.avatarFrameId,
                            onMenuClick = { isDrawerOpen = true },
                            onWatchlistClick = { push("watchlist") },
                            onProfileClick = { switchTab("profile") },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }

                    // Fixed Glassmorphic Bottom Dock (5 primary tabs: Home, Browse, Manga, Novel, Profile)
                    if (isRootRoute) {
                        OttBottomDock(
                            currentRoute = currentRoute,
                            onNavigate = { switchTab(it) },
                            userAvatarUrl = vm.userProfile.avatarUrl,
                            avatarDecorationId = vm.userProfile.avatarDecorationId,
                            avatarFrameId = vm.userProfile.avatarFrameId,
                            backdrop = appBackdropLayer,
                            collapsed = chrome.bottomCollapsed,
                            onExpand = { chrome.expandBottom() },
                            onSearchClick = { push("search") },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Navigation menu: drawn after the dock so its scrim and card sit on top of both bars.
                    OttNavDrawer(
                        isOpen = isDrawerOpen,
                        onDismiss = { isDrawerOpen = false },
                        onNavigate = { target ->
                            if (listOf("home", "airing", "manga", "novel", "profile").contains(target)) {
                                switchTab(target)
                            } else {
                                push(target)
                            }
                        },
                        userAvatarUrl = vm.userProfile.avatarUrl,
                        avatarDecorationId = vm.userProfile.avatarDecorationId,
                        avatarFrameId = vm.userProfile.avatarFrameId
                    )

                    // Compulsory username setup overlay for logged in users who do not have a username yet
                    if (vm.authState is AuthState.LoggedIn && (vm.isUsernameSetupRequired || vm.userProfile.username.isBlank()) && !vm.authLoading && currentRoute != "login") {
                        CompulsoryUsernameDialog(
                            vm = vm,
                            onUsernameSet = { }
                        )
                    }

                    // In-App Release Update Dialog (Prompted if installed version < latest GitHub release)
                    pendingUpdateRelease?.let { release ->
                        ZenkaiUpdateDialog(
                            release = release,
                            onDismiss = { pendingUpdateRelease = null }
                        )
                    }
                }
            }
        }
    }
}
}
