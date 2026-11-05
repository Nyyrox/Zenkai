package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.data.UserProfile
import com.nyyrox.aniverse.ui.components.DownloadSettingsSheet
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.ThemeMode
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FrameItem(val id: String, val name: String, val file: String) {
    val url: String get() = "https://cdn.jsdelivr.net/gh/Nyyrox/SillyTavern-AvatarDecorations-CSS@c63faa39f5c863da5bfaa9a642720b50283de090/dc-decorations/${file}.png"
}

val PRESET_FRAMES = listOf(
    FrameItem("1-angry", "Angry", "1_Angry"),
    FrameItem("1-cat-ears", "Cat Ears", "1_cat-ears"),
    FrameItem("1-dismay", "Dismay", "1_dismay"),
    FrameItem("1-heartbloom", "Heartbloom", "1_heartbloom"),
    FrameItem("1-in-love", "In Love", "1_In-Love"),
    FrameItem("1-in-tears", "In Tears", "1_in-tears"),
    FrameItem("1-ki-energy", "Ki Energy", "1_ki-energy"),
    FrameItem("1-radiating-energy", "Radiating Energy", "1_Radiating-Energy"),
    FrameItem("1-rage", "Rage", "1_rage"),
    FrameItem("1-shocked", "Shocked", "1_Shocked"),
    FrameItem("1-soul-leaving-body", "Soul Leaving Body", "1_Soul-Leaving-Body"),
    FrameItem("1-starry-eyed", "Starry Eyed", "1_Starry-Eyed"),
    FrameItem("1-sweat-drops", "Sweat Drops", "1_Sweat-Drops"),
    FrameItem("2-cybernetic", "Cybernetic", "2_Cybernetic"),
    FrameItem("2-digital-sunrise", "Digital Sunrise", "2_Digital Sunrise"),
    FrameItem("2-glitch", "Glitch", "2_Glitch"),
    FrameItem("2-implant", "Implant", "2_Implant"),
    FrameItem("3-disxcore-headset", "DISXCORE Headset", "3_DISXCORE Headset"),
    FrameItem("3-futuristic-ui", "Futuristic UI", "3_Futuristic UI"),
    FrameItem("3-smoke", "Smoke", "3_Smoke"),
    FrameItem("4-defensive-shield", "Defensive Shield", "4_Defensive Shield"),
    FrameItem("4-flaming-sword", "Flaming Sword", "4_Flaming Sword"),
    FrameItem("4-glowing-runes", "Glowing Runes", "4_GLowing Runes"),
    FrameItem("4-magical-potion", "Magical Potion", "4_Magical Potion"),
    FrameItem("4-skull-medallion", "Skull Medallion", "4_Skull Medallion"),
    FrameItem("4-treasure-and-key", "Treasure & Key", "4_Treasure and Key"),
    FrameItem("4-wizard-s-staff", "Wizard's Staff", "4_Wizard's Staff"),
    FrameItem("5-dragon-s-smile", "Dragon's Smile", "5_Dragon's Smile"),
    FrameItem("5-fan-fluorish", "Fan Fluorish", "5_Fan Fluorish"),
    FrameItem("5-firecrackers", "Firecrackers", "5_Firecrackers"),
    FrameItem("5-koi-pond", "Koi Pond", "5_Koi Pond"),
    FrameItem("5-lucky-envelopes", "Lucky Envelopes", "5_Lucky Envelopes"),
    FrameItem("5-lunar-lanterns", "Lunar Lanterns", "5_Lunar Lanterns"),
    FrameItem("6-air", "Air", "6_Air"),
    FrameItem("6-balance", "Balance", "6_Balance"),
    FrameItem("6-earth", "Earth", "6_Earth"),
    FrameItem("6-fire", "Fire", "6_Fire"),
    FrameItem("6-lightning", "Lightning", "6_Lightning"),
    FrameItem("6-water", "Water", "6_Water"),
    FrameItem("7-chromawave", "Chromawave", "7_Chromawave"),
    FrameItem("7-cozy-cat", "Cozy Cat", "7_Cozy-Cat"),
    FrameItem("7-cozy-headphones", "Cozy Headphones", "7_Cozy-Headphones"),
    FrameItem("7-oasis", "Oasis", "7_Oasis"),
    FrameItem("7-rainy-mood", "Rainy Mood", "7_Rainy-Mood"),
    FrameItem("8-blade-storm", "Blade Storm", "8_blade-storm"),
    FrameItem("8-omens-cowl", "Omens Cowl", "8_omens-cowl"),
    FrameItem("8-reynas-leer", "Reynas Leer", "8_reynas-leer"),
    FrameItem("9-butterflies", "Butterflies", "9_butterflies"),
    FrameItem("9-honeyblossom", "Honeyblossom", "9_honeyblossom"),
    FrameItem("10-astronaut-helmet", "Astronaut Helmet", "10_Astronaut-Helmet"),
    FrameItem("10-black-hole", "Black Hole", "10_Black-Hole"),
    FrameItem("10-constellations", "Constellations", "10_Constellations"),
    FrameItem("10-solar-orbit", "Solar Orbit", "10_Solar-Orbit"),
    FrameItem("10-stardust", "Stardust", "10_Stardust"),
    FrameItem("10-ufo", "UFO", "10_UFO")
)

fun getResolvedFrameUrl(id: String?): String? {
    if (id.isNullOrBlank() || id == "none") return null
    if (id.startsWith("http://") || id.startsWith("https://")) return id
    return PRESET_FRAMES.firstOrNull { it.id == id }?.url
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenUserProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    val userProfile = vm.userProfile
    val authState = vm.authState
    val isLoggedIn = authState is AuthState.LoggedIn

    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var currentAudio by remember { mutableStateOf(prefs.getString("pref_audio", "sub") ?: "sub") }
    var currentQuality by remember { mutableStateOf(prefs.getString("pref_quality", "") ?: "") }
    var autoPlayNext by remember { mutableStateOf(prefs.getBoolean("auto_play_next", true)) }
    var autoSkipIntro by remember { mutableStateOf(prefs.getBoolean("auto_skip_intro_outro", true)) }
    var skipFiller by remember { mutableStateOf(prefs.getBoolean("pref_skip_filler", false)) }

    var showEditSheet by remember { mutableStateOf(false) }
    var initialEditTab by remember { mutableIntStateOf(0) }
    var showDownloadSheet by remember { mutableStateOf(false) }

    // Load presets from assets safely
    val presetAvatars = remember {
        try {
            val json = context.assets.open("avatars.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getJSONObject(i).getString("url"))
            }
            list
        } catch (_: Exception) {
            listOf(
                "https://anime-lok-assets.pages.dev/avatars/640.jpg",
                "https://anime-lok-assets.pages.dev/avatars/641.jpg",
                "https://anime-lok-assets.pages.dev/avatars/642.jpg",
                "https://anime-lok-assets.pages.dev/avatars/643.jpg",
                "https://anime-lok-assets.pages.dev/avatars/644.jpg",
                "https://anime-lok-assets.pages.dev/avatars/645.jpg"
            )
        }
    }

    val presetBanners = remember {
        try {
            val json = context.assets.open("banners.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getJSONObject(i).getString("url"))
            }
            list
        } catch (_: Exception) {
            listOf(
                "https://artworks.thetvdb.com/banners/v4/series/81797/backgrounds/6564ee8183d39.jpg",
                "https://artworks.thetvdb.com/banners/v4/series/81797/backgrounds/659a96a80f691.jpg",
                "https://artworks.thetvdb.com/banners/fanart/original/79824-16.jpg",
                "https://artworks.thetvdb.com/banners/v4/series/348545/backgrounds/62489b1bdc673.jpg"
            )
        }
    }

    val formatLastSync: (Long?) -> String = { timestamp ->
        if (timestamp == null || timestamp == 0L) {
            "Never synced"
        } else {
            val diff = System.currentTimeMillis() - timestamp
            when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 24 * 3600_000 -> {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    "Today at ${sdf.format(Date(timestamp))}"
                }
                else -> {
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.glass.border, CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = "Profile & Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            // 1. ProfileHero
            item {
                val bannerUrl = userProfile.bannerUrl.takeIf { it.isNotBlank() }
                val avatarUrl = userProfile.avatarUrl.takeIf { it.isNotBlank() } ?: "https://anime-lok-assets.pages.dev/avatars/641.jpg"
                val frameUrl = getResolvedFrameUrl(userProfile.avatarDecorationId ?: userProfile.avatarFrameId)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Banner with overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(colors.surfaceElevated)
                        ) {
                            if (bannerUrl != null) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Profile Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1E222A))
                                )
                            }
                            // Dark gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x60000000))
                            )

                            // Quick Edit Banner Button
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCC000000))
                                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                    .clickable {
                                        initialEditTab = 3
                                        showEditSheet = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Edit Banner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // Info section below banner
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Avatar with frame
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clickable {
                                            initialEditTab = 1
                                            showEditSheet = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (frameUrl != null) {
                                        AsyncImage(
                                            model = frameUrl,
                                            contentDescription = "Avatar Frame",
                                            modifier = Modifier.size(76.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                // Edit Profile button
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable {
                                            initialEditTab = 0
                                            showEditSheet = true
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Edit profile",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = userProfile.displayName.ifBlank { "Zenkai User" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textPrimary
                            )

                            if (userProfile.username.isNotBlank()) {
                                Text(
                                    text = "@${userProfile.username.removePrefix("@")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted
                                )
                            }

                            if (userProfile.bio.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = userProfile.bio,
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. CloudSyncCard / AccountPanel
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    if (!isLoggedIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLogin?.invoke() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sign in to sync",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Optional. Back up history & resume on any device.",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        val loggedInState = authState as AuthState.LoggedIn
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cloud Sync",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Account: ${loggedInState.email ?: userProfile.username.ifBlank { "Connected" }}",
                                        fontSize = 12.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Last synced: ${formatLastSync(vm.lastCloudSyncTime)}",
                                        fontSize = 11.sp,
                                        color = colors.accent
                                    )
                                }

                                if (vm.isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = colors.accent,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = colors.glass.border, thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Push to Cloud
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                    .clickable(enabled = !vm.isCloudSyncing) {
                                        vm.pushToCloud { success ->
                                            Toast.makeText(
                                                context,
                                                if (success) "Synced to Cloud successfully" else "Sync failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Push",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                // Pull from Cloud
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable(enabled = !vm.isCloudSyncing) {
                                            vm.pullFromCloud { success ->
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Restored from Cloud" else "Cloud sync failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Pull",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                // Sign Out
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable {
                                            vm.signOut()
                                            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Sign out",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. AniList Card
            item {
                val aniList = vm.aniListAccount
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AniList Sync",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (aniList != null) "Connected as ${aniList.username}" else "Sync watch progress with AniList",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }

                            if (aniList != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable {
                                            vm.syncTrackers { res ->
                                                Toast.makeText(context, "AniList synced (${res.totalPulled} items)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Sync",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accent
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable {
                                            val url = "https://anilist.co/api/v2/oauth/authorize?client_id=18497&response_type=token"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Connect",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Settings Section (Matching Web & Desktop)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        // Theme Mode
                        SettingRow(title = "Theme", hint = "Dark or light appearance") {
                            val isDark = vm.themeSettings.mode == ThemeMode.DARK
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.glass.border, CircleShape)
                                    .padding(3.dp)
                            ) {
                                listOf("dark" to true, "light" to false).forEach { (label, dark) ->
                                    val selected = isDark == dark
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) colors.textPrimary else Color.Transparent)
                                            .clickable {
                                                vm.setThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label.replaceFirstChar { it.uppercase() },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) colors.bg else colors.textMuted
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Default Audio (Sub / Dub)
                        SettingRow(title = "Default audio", hint = "Sub or dub for multi-audio streams") {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.glass.border, CircleShape)
                                    .padding(3.dp)
                            ) {
                                listOf("sub", "dub").forEach { opt ->
                                    val selected = currentAudio.equals(opt, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) colors.textPrimary else Color.Transparent)
                                            .clickable {
                                                currentAudio = opt
                                                prefs.edit().putString("pref_audio", opt).apply()
                                                Toast.makeText(context, "Default audio: ${opt.uppercase()}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = opt.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) colors.bg else colors.textMuted
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Preferred Server
                        SettingRow(title = "Preferred server", hint = "Default playback source") {
                            val serverList = listOf("megaplay" to "MegaPlay", "acdn" to "ACDN", "direct" to "Direct", "animesalt" to "AnimeSalt")
                            val currentServer = vm.preferredServerId?.ifBlank { "megaplay" } ?: "megaplay"
                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable { expanded = !expanded }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = serverList.firstOrNull { it.first == currentServer }?.second ?: currentServer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                }

                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    serverList.forEach { (id, name) ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(name, color = colors.textPrimary, fontSize = 13.sp) },
                                            onClick = {
                                                vm.setPreferredServer(id)
                                                expanded = false
                                                Toast.makeText(context, "Server: $name", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Preferred Quality
                        SettingRow(title = "Preferred quality", hint = "Video resolution preference") {
                            val qualityList = listOf("" to "Auto", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p")
                            var expandedQ by remember { mutableStateOf(false) }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, CircleShape)
                                        .clickable { expandedQ = !expandedQ }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qualityList.firstOrNull { it.first == currentQuality }?.second ?: "Auto",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                }

                                androidx.compose.material3.DropdownMenu(
                                    expanded = expandedQ,
                                    onDismissRequest = { expandedQ = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    qualityList.forEach { (id, name) ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(name, color = colors.textPrimary, fontSize = 13.sp) },
                                            onClick = {
                                                currentQuality = id
                                                prefs.edit().putString("pref_quality", id).apply()
                                                expandedQ = false
                                                Toast.makeText(context, "Quality: $name", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Autoplay next episode
                        SettingRow(title = "Autoplay next episode", hint = "Plays next episode when current ends") {
                            Switch(
                                checked = autoPlayNext,
                                onCheckedChange = {
                                    autoPlayNext = it; prefs.edit().putBoolean("auto_play_next", it).apply()
                                    Toast.makeText(context, "Autoplay ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.bg,
                                    checkedTrackColor = colors.textPrimary,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.surfaceElevated
                                )
                            )
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Auto-skip intro & outro
                        SettingRow(title = "Auto-skip intro & outro", hint = "Automatically skips opening & ending") {
                            Switch(
                                checked = autoSkipIntro,
                                onCheckedChange = {
                                    autoSkipIntro = it; prefs.edit().putBoolean("auto_skip_intro_outro", it).apply()
                                    Toast.makeText(context, "Auto-skip ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.bg,
                                    checkedTrackColor = colors.textPrimary,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.surfaceElevated
                                )
                            )
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Skip filler episodes
                        SettingRow(title = "Skip filler episodes", hint = "Jumps over non-canon and filler episodes") {
                            Switch(
                                checked = skipFiller,
                                onCheckedChange = {
                                    skipFiller = it; prefs.edit().putBoolean("pref_skip_filler", it).apply()
                                    Toast.makeText(context, "Skip filler ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.bg,
                                    checkedTrackColor = colors.textPrimary,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.surfaceElevated
                                )
                            )
                        }

                        HorizontalDivider(color = colors.glass.border, thickness = 1.dp)

                        // Downloads configuration
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDownloadSheet = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Downloads",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Storage location, quality and Wi-Fi rules",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                            Icon(Icons.Default.Download, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 5. About & Legal (Matching Web & Desktop)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "About & legal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Disclaimer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Zenkai does not host, upload or store any video, novel text or manga images. Streams, chapters and pages are provided by third-party services and are fetched at the moment you watch or read. Zenkai has no control over that content and is not affiliated with those services. All titles, artwork and trademarks belong to their respective owners.",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                lineHeight = 17.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "DMCA / copyright",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "If you own a work and believe something linked from Zenkai infringes your copyright, the file lives on the third-party host, so please contact that host first. You may also contact maintainers via Discord or GitHub for notice and removal.",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                lineHeight = 17.sp
                            )

                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.glass.border, CircleShape)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kHxHzZtH7"))
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.accent, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Join Discord Community",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Data & credits",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Anime data from AniList & MyAnimeList (via Jikan); filler information from AnimeFillerList; artwork and episode details from TMDB. Avatar frames from SillyTavern-AvatarDecorations collection.",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                lineHeight = 17.sp
                            )
                        }

                        Text(
                            text = "Zenkai v2.1.0 • Android Native & Web Unified",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }

        // Profile Editor Bottom Sheet
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                containerColor = colors.bg,
                dragHandle = null
            ) {
                var editTab by remember { mutableIntStateOf(initialEditTab) }
                var editName by remember { mutableStateOf(userProfile.displayName) }
                var editBio by remember { mutableStateOf(userProfile.bio) }
                var editUsername by remember { mutableStateOf(userProfile.username) }
                var editAvatarUrl by remember { mutableStateOf(userProfile.avatarUrl) }
                var editBannerUrl by remember { mutableStateOf(userProfile.bannerUrl) }
                var editFrameId by remember { mutableStateOf(userProfile.avatarDecorationId ?: userProfile.avatarFrameId ?: "none") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showEditSheet = false },
                                shape = CircleShape
                            ) {
                                Text("Cancel", color = colors.textMuted, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val updated = userProfile.copy(
                                        displayName = editName.trim(),
                                        bio = editBio.trim(),
                                        username = editUsername.trim().lowercase().removePrefix("@"),
                                        avatarUrl = editAvatarUrl.trim(),
                                        bannerUrl = editBannerUrl.trim(),
                                        avatarDecorationId = if (editFrameId == "none") null else editFrameId,
                                        avatarFrameId = if (editFrameId == "none") null else editFrameId
                                    )
                                    vm.updateUserProfile(updated)
                                    if (isLoggedIn) {
                                        vm.pushToCloud()
                                    }
                                    showEditSheet = false
                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary)
                            ) {
                                Text("Save", color = colors.bg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Tab Row
                    val tabs = listOf("Identity", "Avatar", "Frame", "Banner")
                    TabRow(
                        selectedTabIndex = editTab,
                        containerColor = Color.Transparent,
                        contentColor = colors.textPrimary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[editTab]),
                                color = colors.textPrimary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = editTab == index,
                                onClick = { editTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = if (editTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (editTab == index) colors.textPrimary else colors.textMuted
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    when (editTab) {
                        // 0: Identity
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.textPrimary,
                                        unfocusedBorderColor = colors.glass.border
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = editUsername,
                                    onValueChange = { editUsername = it },
                                    label = { Text("Username (@handle)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.textPrimary,
                                        unfocusedBorderColor = colors.glass.border
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = editBio,
                                    onValueChange = { editBio = it },
                                    label = { Text("Bio") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.textPrimary,
                                        unfocusedBorderColor = colors.glass.border
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 3
                                )
                            }
                        }

                        // 1: Avatar Presets & URL
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = editAvatarUrl,
                                    onValueChange = { editAvatarUrl = it },
                                    label = { Text("Avatar Image URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.textPrimary,
                                        unfocusedBorderColor = colors.glass.border
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Text(
                                    text = "Or choose from Anime presets:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMuted
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(presetAvatars.take(40)) { url ->
                                        val isSelected = editAvatarUrl == url
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) colors.textPrimary else colors.glass.border,
                                                    CircleShape
                                                )
                                                .clickable { editAvatarUrl = url },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2: Avatar Frame
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Choose an avatar overlay frame:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMuted
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // "None" option
                                    item {
                                        val isNone = editFrameId == "none" || editFrameId.isBlank()
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isNone) colors.surfaceElevated else Color.Transparent)
                                                .border(1.dp, if (isNone) colors.textPrimary else colors.glass.border, RoundedCornerShape(12.dp))
                                                .clickable { editFrameId = "none" }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("None", fontSize = 11.sp, color = colors.textMuted)
                                            }
                                            Text("None", fontSize = 10.sp, color = colors.textMuted)
                                        }
                                    }

                                    items(PRESET_FRAMES) { frame ->
                                        val isSelected = editFrameId == frame.id
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                                                .border(1.dp, if (isSelected) colors.textPrimary else colors.glass.border, RoundedCornerShape(12.dp))
                                                .clickable { editFrameId = frame.id }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = editAvatarUrl.ifBlank { "https://anime-lok-assets.pages.dev/avatars/641.jpg" },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                                AsyncImage(
                                                    model = frame.url,
                                                    contentDescription = frame.name,
                                                    modifier = Modifier.size(46.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = frame.name,
                                                fontSize = 9.sp,
                                                color = if (isSelected) colors.textPrimary else colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3: Banner Presets & URL
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = editBannerUrl,
                                    onValueChange = { editBannerUrl = it },
                                    label = { Text("Banner Image URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.textPrimary,
                                        unfocusedBorderColor = colors.glass.border
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Text(
                                    text = "Or choose from Anime banner presets:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMuted
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(presetBanners.take(30)) { url ->
                                        val isSelected = editBannerUrl == url
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(65.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) colors.textPrimary else colors.glass.border,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { editBannerUrl = url }
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Offline Download Settings Sheet
        if (showDownloadSheet) {
            DownloadSettingsSheet(onDismiss = { showDownloadSheet = false })
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    lineHeight = 16.sp
                )
            }
        }
        content()
    }
}
