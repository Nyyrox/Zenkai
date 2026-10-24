package com.nyyrox.aniverse.ui.screens

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import com.nyyrox.aniverse.ui.components.DownloadSettingsSheet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import com.nyyrox.aniverse.data.WatchlistStatus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.CloudSync
import com.nyyrox.aniverse.data.LevelAndStreakSystem
import com.nyyrox.aniverse.ui.components.decorations.BANNER_EFFECT_PRESETS
import com.nyyrox.aniverse.ui.components.decorations.BannerOverlayRenderer
import com.nyyrox.aniverse.ui.components.decorations.ProfileBannerRenderer
import com.nyyrox.aniverse.ui.components.TimeSpentSectionCard
import com.nyyrox.aniverse.util.DisplayRefreshRateManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.WatchHistoryItem
import com.nyyrox.aniverse.data.WatchlistItem
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.ui.theme.LocalThemeSettings
import com.nyyrox.aniverse.ui.theme.OttAccentGold
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSecondary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.components.decorations.AvatarDecorationRenderer
import com.nyyrox.aniverse.ui.components.decorations.TextEffectRenderer
import com.nyyrox.aniverse.ui.components.decorations.RoleBadgeRow
import com.nyyrox.aniverse.ui.components.decorations.LevelRankTagBadge
import com.nyyrox.aniverse.ui.components.decorations.AnimatedLevelThemeBadge
import com.nyyrox.aniverse.ui.components.AniListSyncModalSheet
import com.nyyrox.aniverse.ui.components.MalSyncModalSheet
import com.nyyrox.aniverse.ui.components.decorations.AnimatedStreakBadge
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import com.nyyrox.aniverse.ui.theme.AniButton
import com.nyyrox.aniverse.ui.theme.AniIconButton
import com.nyyrox.aniverse.ui.theme.AniSwitch
import com.nyyrox.aniverse.ui.theme.GlassBackdropHost
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.ThemeMode
import com.nyyrox.aniverse.ui.theme.ThemeRuntime
import androidx.compose.foundation.isSystemInDarkTheme
import com.nyyrox.aniverse.ui.theme.aniGlass
import com.styropyr0.prismal.PrismalGlass
import com.nyyrox.aniverse.ui.components.BioFontSelectorPills
import com.nyyrox.aniverse.ui.components.FavoriteAnimeShowcaseCard
import com.nyyrox.aniverse.ui.components.AniListAnimePickerSheet
import com.nyyrox.aniverse.ui.components.getBioTextStyle
import com.nyyrox.aniverse.ui.components.parseFavoriteAnimeString
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.foundation.lazy.LazyRow
import com.nyyrox.aniverse.data.manga.MangaBookmarkItem
import com.nyyrox.aniverse.ui.components.AvatarCropDialog
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.util.AppHaptics
import com.nyyrox.aniverse.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    vm: MainViewModel,
    onAnimeClick: ((Int) -> Unit)? = null,
    onPlayEpisode: ((Int, Int) -> Unit)? = null,
    onMangaClick: ((String) -> Unit)? = null,
    onOpenSavedManga: (() -> Unit)? = null,
    onOpenNovelHistory: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSeeAllWatchlist: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    onAvatarPicker: (() -> Unit)? = null,
    onBannerPicker: (() -> Unit)? = null,
    onOpenDecorations: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenRankings: (() -> Unit)? = null,
    onOpenUserProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeSettings = LocalThemeSettings.current
    val systemDark = isSystemInDarkTheme()

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    var showDmcaModal by remember { mutableStateOf(false) }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showAvatarOptionsSheet by remember { mutableStateOf(false) }
    var showBannerOptionsSheet by remember { mutableStateOf(false) }
    var showBannerEffectsSheet by remember { mutableStateOf(false) }
    var showNinjaTagsSheet by remember { mutableStateOf(false) }
    var showRefreshRateSheet by remember { mutableStateOf(false) }
    var showAniListSheet by remember { mutableStateOf(false) }
    var showMalSheet by remember { mutableStateOf(false) }
    var showDiscordSheet by remember { mutableStateOf(false) }
    var showChangelogs by remember { mutableStateOf(false) }
    var showDownloadSettingsSheet by remember { mutableStateOf(false) }
    var selectedAvatarCropUri by remember { mutableStateOf<Uri?>(null) }
    var uploadNoticeMessage by remember { mutableStateOf<String?>(null) }
    val savedMangaList = remember(vm.mangaBookmarks) { vm.mangaBookmarks.distinctBy { it.mangaId } }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedAvatarCropUri = uri
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.uploadBanner(context, uri) { success, err ->
                if (!success && err != null) {
                    uploadNoticeMessage = err
                }
            }
        }
    }

    // Apply display refresh rate and respect Battery Saver mode
    LaunchedEffect(themeSettings.isPowerSavingEnabled, themeSettings.preferredRefreshRate) {
        activity?.let { act ->
            DisplayRefreshRateManager.applyRefreshRate(
                act,
                themeSettings.preferredRefreshRate,
                themeSettings.isPowerSavingEnabled
            )
        }
    }

    val supportedRates = remember(context) { DisplayRefreshRateManager.getSupportedRefreshRates(context) }
    val effectiveHz = DisplayRefreshRateManager.calculateEffectiveRefreshRate(
        context,
        themeSettings.preferredRefreshRate,
        themeSettings.isPowerSavingEnabled
    )
    val isBatterySaverActive = themeSettings.isPowerSavingEnabled || DisplayRefreshRateManager.isSystemPowerSaveMode(context)

    val c = LocalAppColors.current
    val listState = rememberLazyListState()
    val isLoggedIn = vm.authState !is AuthState.LoggedOut
    val scrollPx: () -> Float = {
        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 100000f
    }

    Box(modifier.fillMaxSize()) {
        GlassBackdropHost(background = { ProfileBanner(vm, scrollPx) }) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                // ── Header ────────────────────────────────────────────────
                item {
                    Box(Modifier.fillMaxWidth().height(242.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp)
                                .size(84.dp)
                                .clickable { showAvatarOptionsSheet = true }
                        ) {
                            AvatarDecorationRenderer(
                                decorationId = vm.userProfile.avatarDecorationId,
                                frameId = vm.userProfile.avatarFrameId,
                                size = 84.dp
                            ) {
                                val hasDecoration = !vm.userProfile.avatarDecorationId.isNullOrBlank()
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(c.surfaceElevated)
                                        .border(if (hasDecoration) 1.dp else 3.dp, if (hasDecoration) c.glass.border else c.bg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (vm.userProfile.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = vm.userProfile.avatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = vm.userProfile.displayName.ifBlank { "G" }.take(1).uppercase(),
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = c.textPrimary
                                        )
                                    }
                                }
                            }
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c.accent)
                                    .border(2.dp, c.bg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, "Edit avatar", tint = c.onAccent, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                item {
                    val email = vm.userProfile.email
                    val handle = when {
                        vm.userProfile.username.isNotBlank() -> "@${vm.userProfile.username}"
                        !email.isNullOrBlank() -> email
                        else -> "Guest"
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    TextEffectRenderer(
                                        effectId = vm.userProfile.textEffectId,
                                        text = vm.userProfile.displayName.ifBlank { "Guest" },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RoleBadgeRow(roles = vm.userProfile.roles, isAdmin = vm.userProfile.isAdmin)
                                }
                                Text(handle, fontSize = 13.sp, color = c.textSecondary)
                            }
                            Spacer(Modifier.width(12.dp))
                            if (isLoggedIn) {
                                AniButton(onClick = { showEditProfileModal = true }) {
                                    Text("Edit profile", color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                AniButton(onClick = { onLogin?.invoke() }, primary = true) {
                                    Text("Sign in", color = c.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        val bio = vm.userProfile.bio
                        if (bio.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(text = bio, style = getBioTextStyle(vm.userProfile.bioFont), color = c.textPrimary)
                        }
                    }
                }

                // ── Level, streak & library stats ─────────────────────────
                item {
                    val level = vm.userProfile.level
                    val exp = vm.userProfile.exp
                    val progress = LevelAndStreakSystem.calculateLevelProgress(exp)
                    val nextExp = LevelAndStreakSystem.expForNextLevel(level)
                    val equippedTag = vm.userProfile.customTag?.takeIf {
                        it.isNotBlank() && !it.equals("null", true) && !it.equals("none", true)
                    }
                    val watching = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.WATCHING }
                    val completed = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.COMPLETED }
                    val planned = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.PLAN_TO_WATCH }
                    val tile = c.skeleton.copy(alpha = 0.35f)

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .aniGlass(radius = 24.dp)
                            .padding(16.dp)
                    ) {
                        // Level + rank tag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedLevelThemeBadge(level = level)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.clickable { showNinjaTagsSheet = true }) {
                                if (equippedTag != null) LevelRankTagBadge(tag = equippedTag, level = level)
                                else Text("+ Equip tag", fontSize = 12.sp, color = c.textMuted)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Ranks",
                                fontSize = 12.sp, color = c.textSecondary, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { showNinjaTagsSheet = true }
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // EXP progress
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = c.accent,
                            trackColor = c.skeleton
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EXP $exp / $nextExp", fontSize = 11.sp, color = c.textMuted)
                            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = c.textMuted)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Streaks
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tile)
                                    .padding(12.dp)
                            ) {
                                Text("CURRENT STREAK", fontSize = 10.sp, color = c.textMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                                Spacer(Modifier.height(8.dp))
                                AnimatedStreakBadge(streakCount = vm.userProfile.streakCount, isFullLabel = true)
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tile)
                                    .padding(12.dp)
                            ) {
                                Text("BEST STREAK", fontSize = 10.sp, color = c.textMuted, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${vm.userProfile.longestStreak} days",
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.textPrimary
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Library stats
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tile)
                                .padding(vertical = 14.dp)
                        ) {
                            Stat(watching, "Watching", Modifier.weight(1f))
                            Stat(completed, "Completed", Modifier.weight(1f))
                            Stat(planned, "Planned", Modifier.weight(1f))
                        }
                    }
                }

                // ── Appearance ────────────────────────────────────────────
                item {
                    Section("Appearance") {
                        ThemeModeSelector(
                            current = themeSettings.mode,
                            onChange = { mode ->
                                vm.setThemeMode(mode)
                                ThemeRuntime.isLight = when (mode) {
                                    ThemeMode.LIGHT -> true
                                    ThemeMode.DARK -> false
                                    ThemeMode.SYSTEM -> !systemDark
                                }
                            }
                        )
                        RowDivider()
                        SettingRow(Icons.Default.Palette, "Profile decorations", onClick = { onOpenDecorations?.invoke() })
                        RowDivider()
                        SettingRow(
                            Icons.Default.Speed, "Refresh rate",
                            subtitle = if (isBatterySaverActive) "Locked to 60 Hz by Battery Saver" else null,
                            value = "$effectiveHz Hz",
                            onClick = { showRefreshRateSheet = true }
                        )
                        RowDivider()
                        SettingRow(
                            Icons.Default.BatteryChargingFull, "Battery saver",
                            subtitle = "Caps to 60 Hz, lighter rendering",
                            trailing = { AniSwitch(themeSettings.isPowerSavingEnabled) { vm.setPowerSaving(it) } }
                        )
                        RowDivider()
                        SettingRow(
                            Icons.Default.MotionPhotosOff, "Reduced motion",
                            trailing = { AniSwitch(themeSettings.isReducedMotionEnabled) { vm.setReducedMotion(it) } }
                        )
                        if (PrismalGlass.supportsRefraction || !themeSettings.isGlassmorphismEnabled) {
                            RowDivider()
                            SettingRow(
                                Icons.Default.AutoAwesome, "Liquid glass",
                                subtitle = "Turn off for lower GPU use",
                                trailing = { AniSwitch(themeSettings.isGlassmorphismEnabled) { vm.setGlassmorphism(it) } }
                            )
                        }
                    }
                }

                // ── Library ───────────────────────────────────────────────
                item {
                    Section("Library") {
                        SettingRow(Icons.Default.Bookmark, "Watchlist", value = "${vm.watchlist.size}", onClick = { onSeeAllWatchlist?.invoke() })
                        RowDivider()
                        SettingRow(Icons.Default.History, "Watch history", value = "${vm.history.size}", onClick = { onSeeAllWatchlist?.invoke() })
                        RowDivider()
                        SettingRow(Icons.Default.AutoStories, "Saved manga", value = "${vm.mangaBookmarks.size}",
                            onClick = { onOpenSavedManga?.invoke() ?: onSeeAllWatchlist?.invoke() })
                        RowDivider()
                        SettingRow(Icons.Default.MenuBook, "Manga history", value = "${vm.mangaHistory.size}",
                            onClick = { onOpenSavedManga?.invoke() ?: onSeeAllWatchlist?.invoke() })
                        RowDivider()
                        SettingRow(Icons.Default.AutoStories, "Novel history", value = "${vm.novelHistoryList.size}",
                            onClick = { onOpenNovelHistory?.invoke() ?: onSeeAllWatchlist?.invoke() })
                        RowDivider()
                        SettingRow(Icons.Default.Download, "Downloads & storage", onClick = { showDownloadSettingsSheet = true })
                    }
                }

                if (savedMangaList.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(savedMangaList, key = { it.mangaId }) { bm ->
                                ProfileMangaCard(item = bm, onClick = {
                                    vm.searchManga(bm.title)
                                    onMangaClick?.invoke(bm.mangaId)
                                })
                            }
                        }
                    }
                }

                // ── Connections ───────────────────────────────────────────
                item {
                    val discordAcc by com.nyyrox.aniverse.data.discord.DiscordAccountManager.accountState.collectAsState()
                    val ani = vm.aniListAccount
                    val mal = vm.malAccount
                    Section("Connections") {
                        SettingRow(Icons.Default.Sync, "AniList", value = ani?.let { "@${it.username}" } ?: "Connect",
                            onClick = { showAniListSheet = true })
                        RowDivider()
                        SettingRow(Icons.Default.SyncAlt, "MyAnimeList", value = mal?.let { "@${it.username}" } ?: "Connect",
                            onClick = { showMalSheet = true })
                        RowDivider()
                        SettingRow(Icons.Default.PlayArrow, "Discord presence", value = discordAcc?.let { "@${it.username}" } ?: "Connect",
                            onClick = { showDiscordSheet = true })
                        RowDivider()
                        SettingRow(
                            Icons.Default.CloudSync, "Auto-sync",
                            subtitle = "Update AniList & MAL while you watch",
                            trailing = {
                                AniSwitch(vm.trackerSettings.isAutoSyncEnabled) {
                                    vm.updateTrackerSettings(vm.trackerSettings.copy(isAutoSyncEnabled = it))
                                }
                            }
                        )
                    }
                }

                // ── Privacy ───────────────────────────────────────────────
                if (isLoggedIn) {
                    item {
                        Section("Privacy") {
                            SettingRow(Icons.Default.Lock, "Private watchlist",
                                trailing = { AniSwitch(vm.userProfile.isWatchlistPrivate) { vm.updatePrivacySettings(isWatchlistPrivate = it) } })
                            RowDivider()
                            SettingRow(Icons.Default.Lock, "Private watch history",
                                trailing = { AniSwitch(vm.userProfile.isWatchHistoryPrivate) { vm.updatePrivacySettings(isWatchHistoryPrivate = it) } })
                            RowDivider()
                            SettingRow(Icons.Default.Lock, "Private manga & novels",
                                trailing = {
                                    AniSwitch(vm.userProfile.isMangaPrivate || vm.userProfile.isNovelPrivate) {
                                        vm.updatePrivacySettings(isMangaPrivate = it, isNovelPrivate = it)
                                    }
                                })
                        }
                    }
                }

                // ── About & community ─────────────────────────────────────
                item {
                    Section("About") {
                        SettingRow(Icons.Default.Info, "What's new", value = "v${com.nyyrox.aniverse.BuildConfig.VERSION_NAME}",
                            onClick = { showChangelogs = true })
                        RowDivider()
                        SettingRow(Icons.Default.Security, "Legal & DMCA", onClick = { showDmcaModal = true })
                        RowDivider()
                        SettingRow(Icons.AutoMirrored.Filled.OpenInNew, "Discord",
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/aXTfS6XGg"))) })
                        RowDivider()
                        SettingRow(Icons.AutoMirrored.Filled.OpenInNew, "Telegram",
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/StreamverseOfc"))) })
                        RowDivider()
                        SettingRow(Icons.Default.Code, "GitHub",
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nyyrox/Zenkai"))) })
                    }
                }

                if (isLoggedIn) {
                    item {
                        Section("Account") {
                            SettingRow(Icons.AutoMirrored.Filled.Logout, "Sign out", danger = true, onClick = { vm.signOut() })
                        }
                    }
                }

                item {
                    Text(
                        "ZENKAI · v${com.nyyrox.aniverse.BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp, color = c.textMuted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Pinned glass actions over the banner
            Row(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenNotifications != null) {
                    Box {
                        AniIconButton(Icons.Default.Notifications, "Notifications", onOpenNotifications)
                        if (vm.unreadNotificationsCount > 0) {
                            Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(7.dp).clip(CircleShape).background(c.accent))
                        }
                    }
                }
                AniIconButton(Icons.Default.Edit, "Edit banner", { showBannerOptionsSheet = true })
            }
        }
    }

    // ── Dialog: DMCA Legal Modal ──────────────────────────────────────────
    if (showDmcaModal) {
        ModalBottomSheet(
            onDismissRequest = { showDmcaModal = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "DMCA & Legal Disclaimer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Text(
                    text = "ZENKAI is an open-source software application designed for educational and informational purposes. ZENKAI does NOT host, upload, or store any media files, video streams, or manga images on any server owned or controlled by the developers.\n\nAll content presented in the application is parsed and scraped in real-time from publicly accessible third-party internet sources. All trademarks, copyrights, and intellectual property belong to their respective rightful owners.\n\nIf you are a copyright owner or an agent thereof and believe that any indexed content infringes upon your copyright, please contact the third-party provider directly or contact the developer via GitHub for prompt filtering and assistance within 48 hours.",
                    fontSize = 12.sp,
                    color = Color(0xFFCAD5E2),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { showDmcaModal = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = OttPillShape
                ) {
                    Text(
                        text = "I Understand",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // ── Dialog: Edit Profile Modal ───────────────────────────────────────
    if (showEditProfileModal) {
        var editDisplayName by remember { mutableStateOf(vm.userProfile.displayName) }
        var editUsername by remember { mutableStateOf(vm.userProfile.username) }
        var editBio by remember { mutableStateOf(vm.userProfile.bio) }
        var editBioFont by remember { mutableStateOf(vm.userProfile.bioFont) }
        var editFavoriteAnime by remember { mutableStateOf(vm.userProfile.favoriteAnime) }
        var editRoles by remember { mutableStateOf(vm.userProfile.roles.toSet()) }
        var showAniListPicker by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showEditProfileModal = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = OttPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Edit Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = OttTextMuted
                    )
                )

                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = OttTextMuted
                    )
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = OttTextMuted
                    )
                )

                // Bio Typography Selector
                BioFontSelectorPills(
                    selectedFont = editBioFont,
                    onFontSelected = { editBioFont = it }
                )

                // Favorite Anime Selector from AniList
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "FAVORITE ANIME (FROM ANILIST)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = OttTextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (editFavoriteAnime.isNotBlank()) {
                        val parsed = parseFavoriteAnimeString(editFavoriteAnime)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (parsed.coverImage.isNotBlank()) {
                                AsyncImage(
                                    model = parsed.coverImage,
                                    contentDescription = parsed.title,
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 56.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = parsed.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (parsed.score.isNotBlank()) {
                                    Text("⭐ ${parsed.score}% Score", color = Color(0xFFFBBF24), fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { showAniListPicker = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Change", tint = OttPrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { editFavoriteAnime = "" }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = { showAniListPicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x18FFFFFF),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0x30FFFFFF))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = OttPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search & Pick Favorite Anime", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        vm.updateCloudProfile(
                            displayName = editDisplayName,
                            username = editUsername.ifBlank { null },
                            bio = editBio,
                            bioFont = editBioFont,
                            favoriteAnime = editFavoriteAnime,
                            roles = null
                        )
                        showEditProfileModal = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = OttPillShape
                ) {
                    Text(
                        text = "Save Changes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        if (showAniListPicker) {
            AniListAnimePickerSheet(
                onDismiss = { showAniListPicker = false },
                onAnimeSelected = { anime ->
                    val encoded = "${anime.id}|${anime.title}|${anime.cover}|${anime.score ?: 0}|${anime.displayFormat}"
                    editFavoriteAnime = encoded
                    vm.updateCloudProfile(favoriteAnime = encoded)
                },
                onSearch = { q -> vm.searchAniListAnime(q) }
            )
        }
    }

    // ── Dialog: Avatar Options Modal ───────────────────────────────────────
    if (showAvatarOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Profile Avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 1. Upload from Gallery
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x16FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            avatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                    Column {
                        Text("Upload from Device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Choose any photo from your gallery", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // 2. Customize Avatar Decorations (Zenkai Locker)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFA0A4B2).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFA0A4B2).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            onOpenDecorations?.invoke()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White)
                    Column {
                        Text("Avatar & Text Effects", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Equip animated avatars & kinetic text effects", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // 3. Remove Avatar (if set)
                if (vm.userProfile.avatarUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x16FF3B30))
                            .border(1.dp, Color(0x40FF453A), RoundedCornerShape(14.dp))
                            .clickable {
                                showAvatarOptionsSheet = false
                                vm.updateCloudProfile(avatarUrl = "")
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Text("Remove Avatar", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // ── Dialog: Banner Options Modal ───────────────────────────────────────
    if (showBannerOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBannerOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Profile Banner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 1. Upload from Gallery
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x16FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                        .clickable {
                            showBannerOptionsSheet = false
                            bannerPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                    Column {
                        Text("Upload from Device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Choose a wallpaper from your gallery", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // 2. Customize Banner Themes (Zenkai Locker)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFA0A4B2).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFA0A4B2).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showBannerOptionsSheet = false
                            onOpenDecorations?.invoke()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White)
                    Column {
                        Text("Zenkai Banner Themes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Choose from 16 dark, energy & anime themes", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // 3. Banner Animated Overlays (Sakura, Butterflies, Starlight...)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showBannerOptionsSheet = false
                            showBannerEffectsSheet = true
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA))
                    Column {
                        Text("Animated Overlays (Sakura, Butterflies...)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Add gentle drifting sakura, butterflies, or starlight", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // 4. Remove Banner (if set)
                if (vm.userProfile.bannerUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x16FF3B30))
                            .border(1.dp, Color(0x40FF453A), RoundedCornerShape(14.dp))
                            .clickable {
                                showBannerOptionsSheet = false
                                vm.updateCloudProfile(bannerUrl = "")
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Text("Remove Banner", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // ── Dialog: Banner Effects Sheet (Sakura, Butterflies, Starlight...) ─────
    if (showBannerEffectsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBannerEffectsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Banner Animated Overlays",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Delicate ambient particle animations that gently drift over your banner",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                BANNER_EFFECT_PRESETS.forEach { preset ->
                    val isEquipped = (vm.userProfile.bannerEffectId == preset.id) ||
                            (preset.id == "none" && (vm.userProfile.bannerEffectId.isBlank() || vm.userProfile.bannerEffectId == "none"))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isEquipped) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color(0x14FFFFFF))
                            .border(
                                1.dp,
                                if (isEquipped) Color(0xFF8B5CF6) else Color(0x22FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                vm.equipBannerEffect(preset.id)
                                showBannerEffectsSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x228B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (preset.id) {
                                        "embers" -> Icons.Default.LocalFireDepartment
                                        "starlight" -> Icons.Default.AutoAwesome
                                        "none" -> Icons.Default.Close
                                        else -> Icons.Default.Palette
                                    },
                                    contentDescription = preset.name,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = preset.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = preset.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isEquipped) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Equipped", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── Dialog: Ninja Ranks & Tags Unlock Sheet ─────────────────────────────
    if (showNinjaTagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNinjaTagsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Ninja Ranks & Title Tags",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Level up & build daily streaks to unlock prestigious titles",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    AnimatedLevelThemeBadge(level = vm.userProfile.level)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val currentEquipped = vm.userProfile.customTag?.takeIf {
                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true)
                }
                val userLvl = vm.userProfile.level
                val userStrk = vm.userProfile.streakCount

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isNoTagEquipped = currentEquipped == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNoTagEquipped) Color(0xFFA0A4B2).copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isNoTagEquipped) Color(0xFFA0A4B2) else Color(0x15FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    vm.equipCustomTag(null)
                                    showNinjaTagsSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "No Tag",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Clear equipped badge from your profile",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            if (isNoTagEquipped) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFA0A4B2))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Equipped", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(LevelAndStreakSystem.ALL_TAGS) { tag ->
                        val isUnlocked = userLvl >= tag.minLevel && userStrk >= tag.minStreak
                        val isEquipped = currentEquipped?.equals(tag.title, ignoreCase = true) == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isEquipped -> Color(tag.colorHex).copy(alpha = 0.25f)
                                        isUnlocked -> Color(0x14FFFFFF)
                                        else -> Color(0x0AFFFFFF)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isEquipped -> Color(tag.colorHex)
                                        isUnlocked -> Color(0x30FFFFFF)
                                        else -> Color(0x15FFFFFF)
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isUnlocked) {
                                    if (isEquipped) {
                                        vm.equipCustomTag(null)
                                    } else {
                                        vm.equipCustomTag(tag.title)
                                    }
                                    showNinjaTagsSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = if (isUnlocked) Modifier else Modifier.alpha(0.45f)
                                ) {
                                    LevelRankTagBadge(
                                        tag = tag.title,
                                        level = tag.minLevel
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = tag.category,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tag.description,
                                        color = if (isUnlocked) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            when {
                                isEquipped -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(tag.colorHex))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Equipped", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                isUnlocked -> {
                                    Button(
                                        onClick = {
                                            vm.equipCustomTag(tag.title)
                                            showNinjaTagsSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x28FFFFFF)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Equip", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                else -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (tag.minStreak > 0) "${tag.minStreak}d streak" else "Lv. ${tag.minLevel}",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (showDownloadSettingsSheet) {
        DownloadSettingsSheet(
            onDismiss = { showDownloadSettingsSheet = false }
        )
    }

    // ── Dialog: Refresh Rate Selector Modal ──────────────────────────────
    if (showRefreshRateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRefreshRateSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF0D0F14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Display Refresh Rate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isBatterySaverActive)
                        "Battery saver is currently active. App runs at 60Hz to save battery."
                    else
                        "Select target screen refresh rate for smooth animations and scrolling.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                var currentPhysicalHz by remember {
                    mutableStateOf(DisplayRefreshRateManager.getCurrentDisplayRefreshRate(context))
                }
                LaunchedEffect(showRefreshRateSheet, themeSettings.preferredRefreshRate) {
                    if (showRefreshRateSheet) {
                        currentPhysicalHz = DisplayRefreshRateManager.getCurrentDisplayRefreshRate(context)
                        kotlinx.coroutines.delay(100)
                        currentPhysicalHz = DisplayRefreshRateManager.getCurrentDisplayRefreshRate(context)
                        kotlinx.coroutines.delay(250)
                        currentPhysicalHz = DisplayRefreshRateManager.getCurrentDisplayRefreshRate(context)
                        kotlinx.coroutines.delay(400)
                        currentPhysicalHz = DisplayRefreshRateManager.getCurrentDisplayRefreshRate(context)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Hardware Display Rate",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "${(currentPhysicalHz + 0.5f).toInt()}Hz Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val availableRates = remember(supportedRates) {
                    val base = mutableSetOf(60, 90, 120, 144)
                    base.addAll(supportedRates)
                    base.sorted()
                }
                availableRates.forEach { hz ->
                    val isSelected = themeSettings.preferredRefreshRate == hz
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0x22FFFFFF) else Color(0x0CFFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) Color.White else Color(0x1AFFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                vm.setPreferredRefreshRate(hz)
                                activity?.let { act ->
                                    DisplayRefreshRateManager.applyRefreshRate(act, hz, false)
                                }
                                showRefreshRateSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${hz}Hz",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if ((currentPhysicalHz + 0.5f).toInt() == hz) {
                                    Box(
                                        modifier = Modifier
                                            .clip(OttPillShape)
                                            .background(Color(0x2510B981))
                                            .border(0.8.dp, Color(0x5010B981), OttPillShape)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6EE7B7)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when (hz) {
                                    60 -> "Standard (lowest battery use, locked)"
                                    90 -> "Smooth & balanced (recommended)"
                                    120 -> "Ultra fluid (high refresh rate)"
                                    144 -> "Extreme 144Hz (maximum frame rate)"
                                    else -> "${hz} frames per second"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // ── Dialog: AniList Account Sheet ────────────────────────────────────
    if (showAniListSheet) {
        AniListSyncModalSheet(
            vm = vm,
            account = vm.aniListAccount,
            onDismiss = { showAniListSheet = false }
        )
    }

    // ── Dialog: MyAnimeList (MAL) Account Sheet ──────────────────────────
    if (showMalSheet) {
        MalSyncModalSheet(
            vm = vm,
            account = vm.malAccount,
            onDismiss = { showMalSheet = false }
        )
    }

    // ── Dialog: Discord Rich Presence Sheet ──────────────────────────────
    if (showDiscordSheet) {
        com.nyyrox.aniverse.ui.components.DiscordIntegrationSheet(
            onDismiss = { showDiscordSheet = false }
        )
    }

    // ── Dialog: Avatar Interactive Cropper ───────────────────────────────
    if (selectedAvatarCropUri != null) {
        AvatarCropDialog(
            imageUri = selectedAvatarCropUri!!,
            onConfirm = { croppedBytes, mimeType ->
                selectedAvatarCropUri = null
                vm.uploadAvatarBytes(croppedBytes, mimeType) { success, err ->
                    if (!success && err != null) {
                        uploadNoticeMessage = err
                    }
                }
            },
            onDismiss = { selectedAvatarCropUri = null }
        )
    }

    // ── Dialog: In-App Changelogs Overlay ────────────────────────────────
    if (showChangelogs) {
        ZenkaiChangelogsOverlay(onDismiss = { showChangelogs = false })
    }

    // ── Dialog: Upload Notice / Rate Limit Alert ─────────────────────────
    if (uploadNoticeMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { uploadNoticeMessage = null },
            title = {
                Text("Upload Notice", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            },
            text = {
                Text(uploadNoticeMessage ?: "", color = Color(0xFFCBD5E1), fontSize = 13.5.sp)
            },
            confirmButton = {
                Button(
                    onClick = { uploadNoticeMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("OK", color = Color.Black)
                }
            },
            containerColor = Color(0xFF141720),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

// ── Profile UI pieces ─────────────────────────────────────────────────────────

@Composable
private fun ProfileBanner(vm: MainViewModel, scrollPx: () -> Float) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer { translationY = -scrollPx() }
    ) {
        ProfileBannerRenderer(
            bannerThemeId = vm.userProfile.bannerThemeId,
            customBannerUrl = vm.userProfile.bannerUrl,
            modifier = Modifier.fillMaxSize()
        )
        BannerOverlayRenderer(effectId = vm.userProfile.bannerEffectId, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxWidth().height(90.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(110.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.bg)))
        )
    }
}

@Composable
private fun Stat(count: Int, label: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Text(label, fontSize = 12.sp, color = c.textMuted)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            title,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textMuted, letterSpacing = 0.6.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = LocalAppColors.current.glass.border.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp, end = 20.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { AppHaptics.tick(context); onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) c.error else c.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (danger) c.error else c.textPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = c.textMuted, lineHeight = 16.sp)
            }
        }
        if (value != null) {
            Text(value, fontSize = 13.sp, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(Icons.Default.ChevronRight, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemeModeSelector(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val c = LocalAppColors.current
    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .aniGlass(radius = 999.dp)
            .padding(4.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = mode == current
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) c.accent else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) c.onAccent else c.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ProfileMangaCard(
    item: MangaBookmarkItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable {
                AppHaptics.tick(context)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD000000))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.latestChapter ?: item.type ?: "Manga",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LocalAppColors.current.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

