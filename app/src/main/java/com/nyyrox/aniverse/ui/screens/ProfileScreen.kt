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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import com.nyyrox.aniverse.ui.theme.LocalThemeColors
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
    val themeColors = LocalThemeColors.current
    val themeSettings = LocalThemeSettings.current

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OttBg),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Header Profile Banner & Info ────────────────────────────────
        item {
            val isLoggedIn = vm.authState !is AuthState.LoggedOut
            val displayName = vm.userProfile.displayName.ifBlank { "Guest" }
            val email = vm.userProfile.email
            val displayHandle: String = when {
                vm.userProfile.username.isNotBlank() -> "@${vm.userProfile.username}"
                !email.isNullOrBlank() -> email
                else -> "Guest"
            }
            val bio = vm.userProfile.bio
            val favoriteAnime = vm.userProfile.favoriteAnime

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Box containing Banner + naturally overlapping Avatar
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Cinematic Full-Bleed Banner covering status bar (210.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        ProfileBannerRenderer(
                            bannerThemeId = vm.userProfile.bannerThemeId,
                            customBannerUrl = vm.userProfile.bannerUrl,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Banner Overlay Animation (Sakura, Butterflies, Starlight, etc.)
                        BannerOverlayRenderer(
                            effectId = vm.userProfile.bannerEffectId,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top cinematic dark gradient (for status bar visibility)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xD9000000), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom cinematic dark gradient fading into dark background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, OttBg)
                                    )
                                )
                        )

                        // Action buttons in top-right
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 10.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onOpenNotifications != null) {
                                val unread = vm.unreadNotificationsCount
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x99000000))
                                        .border(1.dp, Color(0x35FFFFFF), CircleShape)
                                        .clickable { onOpenNotifications() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (unread > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(OttPrimary)
                                        )
                                    }
                                }
                            }

                            // Edit Banner circular pencil button in top-right
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99000000))
                                    .border(1.dp, Color(0x35FFFFFF), CircleShape)
                                    .clickable { showBannerOptionsSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Banner",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // 2. Avatar overlapping banner bottom
                    Box(
                        modifier = Modifier
                            .padding(start = 20.dp, top = 168.dp)
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF0A0C10))
                                    .border(
                                        width = if (hasDecoration) 1.dp else 3.dp,
                                        color = if (hasDecoration) Color(0x33FFFFFF) else Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (vm.userProfile.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = vm.userProfile.avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF2E3440), Color(0xFF191D24))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName.take(1).uppercase(),
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Small camera badge on bottom-right of avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit Avatar",
                                tint = Color.Black,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. User Name & Edit Profile Row (Single clean Edit Profile button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            TextEffectRenderer(
                                effectId = vm.userProfile.textEffectId,
                                text = displayName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            RoleBadgeRow(
                                roles = vm.userProfile.roles,
                                isAdmin = vm.userProfile.isAdmin
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayHandle,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    if (isLoggedIn) {
                        Button(
                            onClick = { showEditProfileModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x18FFFFFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF)),
                            shape = OttPillShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = { onLogin?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = OttPillShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Sign In", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bio & Favorite Anime
                if (bio.isNotBlank() || favoriteAnime.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        if (bio.isNotBlank()) {
                            Text(
                                text = bio,
                                style = getBioTextStyle(vm.userProfile.bioFont)
                            )
                        }
                        if (favoriteAnime.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FavoriteAnimeShowcaseCard(
                                favoriteAnime = favoriteAnime,
                                onAnimeClick = onAnimeClick
                            )
                        }
                    }
                }

                // 3.5 Level, EXP Progress & Daily Streak Card
                val userExp = vm.userProfile.exp
                val userLevel = vm.userProfile.level
                val nextLvlBase = LevelAndStreakSystem.expForNextLevel(userLevel)
                val progress = LevelAndStreakSystem.calculateLevelProgress(userExp)
                val userStreak = vm.userProfile.streakCount
                val longestStreak = vm.userProfile.longestStreak
                val equippedTag = vm.userProfile.customTag?.takeIf {
                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals("none", ignoreCase = true)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F121A))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Level Badge (Escalating Kinetic Animations)
                                AnimatedLevelThemeBadge(level = userLevel)

                                Spacer(modifier = Modifier.width(8.dp))

                                // Ninja Tag Pill with Level-Scaled Kinetic Overlay (clickable)
                                Box(
                                    modifier = Modifier.clickable { showNinjaTagsSheet = true }
                                ) {
                                    if (equippedTag != null) {
                                        LevelRankTagBadge(
                                            tag = equippedTag,
                                            level = userLevel
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x18FFFFFF))
                                                .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        ) {
                                            Text(
                                                text = "+ Equip Tag",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Daily Streak - Animated Badge
                            AnimatedStreakBadge(
                                streakCount = userStreak,
                                isFullLabel = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // EXP Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXP: $userExp / $nextLvlBase",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFE11D48),
                            trackColor = Color(0x28FFFFFF)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Best streak: $longestStreak d · +$userStreak EXP bonus",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Ninja Ranks ›",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showNinjaTagsSheet = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Monochrome Stats Row
                val watchingCount = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.WATCHING }
                val completedCount = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.COMPLETED }
                val ptwCount = vm.watchlist.count { it.watchlistStatus == WatchlistStatus.PLAN_TO_WATCH }

                val statPill: @Composable (Int, String, Modifier) -> Unit = { count, label, mod ->
                    Column(
                        modifier = mod
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x12FFFFFF))
                            .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    statPill(watchingCount, "Watching", Modifier.weight(1f))
                    statPill(completedCount, "Completed", Modifier.weight(1f))
                    statPill(ptwCount, "Plan to Watch", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Time Spent & Activity Log Card
                TimeSpentSectionCard(
                    record = vm.timeSpentRecord,
                    onResetStats = { vm.resetTimeSpentStats() },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── Customization & Zenkai Locker ─────────────────────────────────
        item {
            ProfileSettingGroup(title = "Customization") {
                ProfileSettingRow(
                    icon = Icons.Default.Palette,
                    title = "Zenkai Decoration Locker",
                    subtitle = "Avatar frames, animated profile effects, banners & nameplates",
                    badgeText = "20 Items",
                    badgeColor = Color(0xFFE11D48),
                    onClick = { onOpenDecorations?.invoke() }
                )
            }
        }

        // ── 2. Library & Content ──────────────────────────────────────────
        item {
            ProfileSettingGroup(title = "Library") {
                ProfileSettingRow(
                    icon = Icons.Default.Bookmark,
                    title = "My Anime Watchlist",
                    subtitle = "Anime saved to watch or plan to watch",
                    badgeText = "${vm.watchlist.size}",
                    onClick = { onSeeAllWatchlist?.invoke() }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.History,
                    title = "Anime Watch History",
                    subtitle = "Episode playback history & progress",
                    badgeText = "${vm.history.size} recent",
                    onClick = { onSeeAllWatchlist?.invoke() }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.AutoStories,
                    title = "Saved Manga & Manhwa",
                    subtitle = "Bookmarked manga collection & reading list",
                    badgeText = "${vm.mangaBookmarks.size} saved",
                    badgeColor = Color(0xFFE11D48),
                    onClick = { onOpenSavedManga?.invoke() ?: onSeeAllWatchlist?.invoke() }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.MenuBook,
                    title = "Manga Reading History",
                    subtitle = "Recent chapters & reading progress checkpoints",
                    badgeText = "${vm.mangaHistory.size} read",
                    onClick = { onOpenSavedManga?.invoke() ?: onSeeAllWatchlist?.invoke() }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.AutoStories,
                    title = "Novel Reading History",
                    subtitle = "Recent light novel chapters & reading progress",
                    badgeText = "${vm.novelHistoryList.size} read",
                    badgeColor = Color(0xFF0284C7),
                    onClick = { onOpenNovelHistory?.invoke() ?: onSeeAllWatchlist?.invoke() }
                )
            }
        }

        // ── Saved Manga Shelf (Profile Showcase) ──────────────────────────
        if (savedMangaList.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = OttPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Saved Manga (${savedMangaList.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View All ›",
                            fontSize = 12.sp,
                            color = OttPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onOpenSavedManga?.invoke() ?: onSeeAllWatchlist?.invoke() }
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedMangaList, key = { it.mangaId }) { bm ->
                            ProfileMangaCard(
                                item = bm,
                                onClick = {
                                    vm.searchManga(bm.title)
                                    onMangaClick?.invoke(bm.mangaId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Anime Tracking & Watchlist Sync ───────────────────────────
        item {
            ProfileSettingGroup(title = "Anime Tracking & Sync") {
                val aniListAcc = vm.aniListAccount
                ProfileSettingRow(
                    icon = Icons.Default.Sync,
                    title = "AniList",
                    subtitle = if (aniListAcc != null) "Connected as @${aniListAcc.username}" else "Sync your AniList watchlist and history",
                    badgeText = if (aniListAcc != null) "Linked" else "Connect",
                    badgeColor = if (aniListAcc != null) Color(0xFF02A9FF) else Color(0x33FFFFFF),
                    onClick = {
                        showAniListSheet = true
                    }
                )
                ProfileDivider()
                val malAcc = vm.malAccount
                ProfileSettingRow(
                    icon = Icons.Default.SyncAlt,
                    title = "MyAnimeList (MAL)",
                    subtitle = if (malAcc != null) "Connected as @${malAcc.username}" else "Sync your MyAnimeList watchlist and history",
                    badgeText = if (malAcc != null) "Linked" else "Connect",
                    badgeColor = if (malAcc != null) Color(0xFF2E51A2) else Color(0x33FFFFFF),
                    onClick = {
                        showMalSheet = true
                    }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.CloudSync,
                    title = "Auto-Sync with Trackers",
                    subtitle = "Automatically update AniList & MAL when watching episodes or changing status",
                    trailing = {
                        Switch(
                            checked = vm.trackerSettings.isAutoSyncEnabled,
                            onCheckedChange = { vm.updateTrackerSettings(vm.trackerSettings.copy(isAutoSyncEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                )
            }
        }

        // ── Discord Rich Presence & Account ──────────────────────────────
        item {
            val discordAcc by com.nyyrox.aniverse.data.discord.DiscordAccountManager.accountState.collectAsState()
            ProfileSettingGroup(title = "Discord Integration") {
                ProfileSettingRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Discord Rich Presence",
                    subtitle = if (discordAcc != null) "Connected as @${discordAcc?.username} · Live Activity" else "Connect Discord to show what anime you're watching",
                    badgeText = if (discordAcc != null) "Connected" else "Connect",
                    badgeColor = if (discordAcc != null) Color(0xFF5865F2) else Color(0x33FFFFFF),
                    onClick = {
                        showDiscordSheet = true
                    }
                )
            }
        }

        // ── 4. Display & Playback Settings ────────────────────────────────
        item {
            ProfileSettingGroup(title = "Display & Performance") {
                ProfileSettingRow(
                    icon = Icons.Default.Speed,
                    title = "Display Refresh Rate",
                    subtitle = if (isBatterySaverActive) "Locked to 60Hz (Battery Saver active)" else "Current: ${effectiveHz}Hz",
                    badgeText = "${effectiveHz} Hz",
                    badgeColor = if (isBatterySaverActive) Color(0xFF10B981) else Color.White,
                    onClick = { showRefreshRateSheet = true }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Saver (60Hz)",
                    subtitle = "Caps refresh rate to 60Hz and reduces shader load",
                    trailing = {
                        Switch(
                            checked = themeSettings.isPowerSavingEnabled,
                            onCheckedChange = { vm.setPowerSaving(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.MotionPhotosOff,
                    title = "Reduced Motion",
                    subtitle = "Replaces spring physics with accessible fades",
                    trailing = {
                        Switch(
                            checked = themeSettings.isReducedMotionEnabled,
                            onCheckedChange = { vm.setReducedMotion(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                )
            }
        }

        // ── 5. Privacy & Visibility Settings ──────────────────────────────
        val isCurrentlyLoggedIn = vm.authState !is AuthState.LoggedOut
        if (isCurrentlyLoggedIn) {
            item {
                ProfileSettingGroup(title = "Privacy & Visibility") {
                    ProfileSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Private Anime Watchlist",
                        subtitle = "Hide saved anime watchlist from other community members",
                        trailing = {
                            Switch(
                                checked = vm.userProfile.isWatchlistPrivate,
                                onCheckedChange = { vm.updatePrivacySettings(isWatchlistPrivate = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Private Anime Watch History",
                        subtitle = "Hide recent episode playback history from public profile",
                        trailing = {
                            Switch(
                                checked = vm.userProfile.isWatchHistoryPrivate,
                                onCheckedChange = { vm.updatePrivacySettings(isWatchHistoryPrivate = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    )
                    ProfileDivider()
                    ProfileSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Private Manga & Novel Reading",
                        subtitle = "Hide manga bookmarks & novel reading history from public profile",
                        trailing = {
                            Switch(
                                checked = vm.userProfile.isMangaPrivate || vm.userProfile.isNovelPrivate,
                                onCheckedChange = { 
                                    vm.updatePrivacySettings(isMangaPrivate = it, isNovelPrivate = it) 
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0x33FFFFFF)
                                )
                            )
                        }
                    )
                }
            }
        }

        // ── OWNER ADMINISTRATION & ROLE MANAGEMENT ──────────────────────────
        if (vm.isCurrentUserOwner()) {
            item {
                var ownerUserQuery by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141722))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OWNER ADMINISTRATION",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Search user by username or display name to promote/demote Admin & assign roles",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ownerUserQuery,
                        onValueChange = {
                            ownerUserQuery = it
                            vm.adminSearchUsers(it)
                        },
                        placeholder = {
                            Text("Search username to manage roles...", color = OttTextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (ownerUserQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    ownerUserQuery = ""
                                    vm.adminSearchUsers("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF10131B),
                            unfocusedContainerColor = Color(0xFF090B10),
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0x28FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    if (vm.isAdminSearching) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else if (vm.adminSearchResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161A26))
                                .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            vm.adminSearchResults.forEach { u ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onOpenUserProfile?.invoke(u.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = u.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x22FFFFFF)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = u.displayName ?: u.username ?: "Otaku",
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val subtitle = listOfNotNull(
                                            u.username?.let { "@$it" },
                                            u.email
                                        ).joinToString(" • ")
                                        if (subtitle.isNotBlank()) {
                                            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x22F59E0B))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Open Profile 👑", color = Color(0xFFF59E0B), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 6. App Updates & About ────────────────────────────────────────
        item {
            ProfileSettingGroup(title = "About & Updates") {
                ProfileSettingRow(
                    icon = Icons.Default.Info,
                    title = "Check for Updates",
                    subtitle = "Version ${com.nyyrox.aniverse.BuildConfig.VERSION_NAME} (Current Build)",
                    onClick = { showChangelogs = true }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.History,
                    title = "Changelogs",
                    subtitle = "See what's new in all versions",
                    onClick = { showChangelogs = true }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.Security,
                    title = "DMCA & Legal Notice",
                    subtitle = "Non-hosting indexer policy & copyright disclaimer",
                    onClick = { showDmcaModal = true }
                )
            }
        }

        // ── 5. Community & Links ──────────────────────────────────────────
        item {
            ProfileSettingGroup(title = "Community & Support") {
                ProfileSettingRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = "Join Discord Community",
                    subtitle = "discord.gg/aXTfS6XGg",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/aXTfS6XGg"))
                        context.startActivity(intent)
                    }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = "Telegram Channel",
                    subtitle = "t.me/StreamverseOfc",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/StreamverseOfc"))
                        context.startActivity(intent)
                    }
                )
                ProfileDivider()
                ProfileSettingRow(
                    icon = Icons.Default.Code,
                    title = "GitHub Repository",
                    subtitle = "Source code, issues & contributions",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nyyrox/Zenkai"))
                        context.startActivity(intent)
                    }
                )
            }
        }

        // ── 7. Account & Session ──────────────────────────────────────────
        if (isCurrentlyLoggedIn) {
            val userEmailOrHandle = when {
                !vm.userProfile.email.isNullOrBlank() -> vm.userProfile.email
                vm.userProfile.username.isNotBlank() -> "@${vm.userProfile.username}"
                else -> "Active session"
            }
            item {
                ProfileSettingGroup(title = "Account") {
                    ProfileSettingRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        subtitle = "Sign out from $userEmailOrHandle",
                        onClick = { vm.signOut() },
                        titleColor = Color(0xFFFF5252),
                        iconTint = Color(0xFFFF5252)
                    )
                }
            }
        }

        // ── 7. App Footer ─────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ZENKAI • v${com.nyyrox.aniverse.BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Crafted for Otakus",
                    fontSize = 10.sp,
                    color = Color(0xFF475569)
                )
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

                val canManageRoles = vm.isCurrentUserOwner()
                if (canManageRoles) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ASSIGN DISPLAYED ROLES",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = OttTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val roleChoices = listOf("OWNER" to "OWNER", "ADMIN" to "ADMIN", "MODERATOR" to "MOD")
                            roleChoices.forEach { (rKey, rLabel) ->
                                val isSelected = editRoles.contains(rKey)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) OttPrimary.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                                        .border(1.dp, if (isSelected) OttPrimary else Color(0x30FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            editRoles = if (isSelected) editRoles - rKey else editRoles + rKey
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = rLabel,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Zenkai Closet & Customization Locker Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE11D48).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showEditProfileModal = false
                            onOpenDecorations?.invoke()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Zenkai Decoration Locker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Customise avatar frames, profile effects & banners",
                                fontSize = 11.sp,
                                color = Color(0xFFCAD5E2)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(18.dp)
                    )
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
                            roles = if (canManageRoles) editRoles.toList() else null
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
                        .background(Color(0xFFE11D48).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showAvatarOptionsSheet = false
                            onOpenDecorations?.invoke()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFF4D6D))
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
                        .background(Color(0xFFE11D48).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable {
                            showBannerOptionsSheet = false
                            onOpenDecorations?.invoke()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFF4D6D))
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
                                .background(if (isNoTagEquipped) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isNoTagEquipped) Color(0xFFE11D48) else Color(0x15FFFFFF),
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
                                        .background(Color(0xFFE11D48))
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

                Spacer(Modifier.height(4.dp))

                val availableRates = if (supportedRates.isNotEmpty()) supportedRates else listOf(60, 90, 120)
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
                                showRefreshRateSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${hz}Hz",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when (hz) {
                                    60 -> "Standard (lowest battery use)"
                                    90 -> "Smooth & balanced (recommended)"
                                    120 -> "Ultra fluid (maximum smoothness)"
                                    144 -> "Extreme high-frame rate"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color(0xFF141720),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

// ── Shared Profile Setting Composables ────────────────────────────────────────

@Composable
private fun ProfileSettingGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x0EFFFFFF))
                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(18.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color.White,
    titleColor: Color = Color.White,
    iconTint: Color = Color.White,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable {
                    AppHaptics.tick(context)
                    onClick()
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x1CFFFFFF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.8.dp, Color(0x2AFFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = Color(0x10FFFFFF),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
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
                .background(Color(0x15FFFFFF))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
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
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

