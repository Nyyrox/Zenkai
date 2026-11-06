package com.nyyrox.aniverse.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.snapshotFlow
import com.nyyrox.aniverse.ui.theme.LocalIsDark
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.nyyrox.aniverse.ui.components.decorations.ApngDecorationImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredSize
import com.nyyrox.aniverse.data.decorations.FrameItem
import com.nyyrox.aniverse.data.decorations.PRESET_FRAMES
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nyyrox.aniverse.ui.components.ShimmerBox
import com.nyyrox.aniverse.data.supabase.AuthState
import com.nyyrox.aniverse.data.UserProfile
import com.nyyrox.aniverse.ui.components.DownloadSettingsSheet
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.ThemeMode
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getResolvedFrameUrl(id: String?): String? {
    return com.nyyrox.aniverse.ui.components.decorations.resolveAvatarOverlayUrl(id)
}

// Rich AniList and TMDB Anime Banners (High-res, NO labels, clean art)
val ANILIST_TMDB_BANNERS = listOf(
    "https://image.tmdb.org/t/p/w1280/2rmK7mnchw9Xr3XdiTFSxTTLXqv.jpg",
    "https://image.tmdb.org/t/p/w1280/nTvM4mhqNlHIvUkI1gVnW6XP7GG.jpg",
    "https://image.tmdb.org/t/p/w1280/3US5UXJSStOVozc6EwhGSMDguol.jpg",
    "https://image.tmdb.org/t/p/w1280/rqbCbjB19amtOtFQbb3K2lgm2zv.jpg",
    "https://image.tmdb.org/t/p/w1280/3ILMlmC30QUnYkY3XEBOyJ82Dqu.jpg",
    "https://image.tmdb.org/t/p/w1280/5cvnxEHT3e39DvT6ARw4GNCFrB0.jpg",
    "https://image.tmdb.org/t/p/w1280/xMNH87maNLt9n2bMDYeI6db5VFm.jpg",
    "https://image.tmdb.org/t/p/w1280/rBOnrVlck7BIlGeWVlzYiZeg4l2.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-8jpFCOcDmneX.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101922-33MtJGsUSxga.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113415-jQBSkxWAAk83.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1535.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21459-yeVkolGKdGUV.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11061-8WkkTZ6duKpq.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21087-sHb9zUZFsHe1.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20605-RCJ7M71zLmrh.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20958-Y7eQdz9VENBD.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/5114-q0V5URebphSG.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20-HHxhPj5JD13a.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/11757-TlEEV9weG4Ag.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20954-f30bHMXa5Qoe.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21519-1ayMXgNlmByb.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/99147-HACsFVrynFf5.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21856-wtSHgeHFmzdG.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/110277-iuGn6F5bK1U1.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/127230-o8IRwCGVr9KW.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101759-MhlCoeqnODso.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20755-D4ipww9U8YkC.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21507-Qx8bGsLXUgLo.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21355-f9SjOfEJMk5P.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20665-j4kSsfhfkM24.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/104578-z7SadpYEuAsy.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/1735.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21234-7lfSSPoMmwr2.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/100166-k7RXwN5vZg0r.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/n9253-JIhmKgBKsWUN.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/97940-1URQdQ4U1a0b.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/20464-PpYjO9cPN1gs.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/n101291-fqIUvQ6apEtD.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/140960-Z7xSvkRxHKfj.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101921-GgvvFhlNhzlF.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/105333-KWKGvBM8Hyga.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101348-pivKKffCAwAY.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21827-ROucgYiiiSpR.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/124080-ARyLAHHgikRq.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/4224-iPUOHdMde27j.jpg",
    "https://s4.anilist.co/file/anilistcdn/media/anime/banner/19815-BqbuD0wyBS3P.jpg"
)

// Rich Anime Character Avatars from AniList (High-res, NO source labels, clean art)

object AniListCatalog {
    fun fetchMoreAvatars(page: Int): List<String> {
        return try {
            val query = "query (\$page: Int) { Page(page: \$page, perPage: 40) { characters(sort: FAVOURITES_DESC) { image { large } } } }"
            val payload = org.json.JSONObject().apply {
                put("query", query)
                put("variables", org.json.JSONObject().put("page", page))
            }.toString()

            val url = java.net.URL("https://graphql.anilist.co")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Zenkai-Android/1.0")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val data = org.json.JSONObject(json).optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("characters")
                val list = mutableListOf<String>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val img = data.optJSONObject(i)?.optJSONObject("image")?.optString("large")
                        if (!img.isNullOrBlank() && !img.contains("default")) {
                            list.add(img)
                        }
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fetchMoreBanners(page: Int): List<String> {
        return try {
            val query = "query (\$page: Int) { Page(page: \$page, perPage: 40) { media(type: ANIME, sort: POPULARITY_DESC) { bannerImage } } }"
            val payload = org.json.JSONObject().apply {
                put("query", query)
                put("variables", org.json.JSONObject().put("page", page))
            }.toString()

            val url = java.net.URL("https://graphql.anilist.co")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Zenkai-Android/1.0")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val data = org.json.JSONObject(json).optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
                val list = mutableListOf<String>()
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val banner = data.optJSONObject(i)?.optString("bannerImage")
                        if (!banner.isNullOrBlank()) {
                            list.add(banner)
                        }
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

val ANILIST_TMDB_AVATARS = listOf(
    "https://s4.anilist.co/file/anilistcdn/character/large/b127691-9zqh1xpIubn7.png", // Gojo Satoru
    "https://s4.anilist.co/file/anilistcdn/character/large/b45627-CR68RyZmddGG.png", // Levi
    "https://s4.anilist.co/file/anilistcdn/character/large/b40-MNypXsxSRb1R.png", // Luffy
    "https://s4.anilist.co/file/anilistcdn/character/large/b27-Z5O02kQUydpT.jpg", // Killua
    "https://s4.anilist.co/file/anilistcdn/character/large/b40882-dsj7IP943WFF.jpg", // Eren
    "https://s4.anilist.co/file/anilistcdn/character/large/b62-S7oAeA9WInjV.png", // Zoro
    "https://s4.anilist.co/file/anilistcdn/character/large/b417-gVLmIJu9phcK.png", // Lelouch
    "https://s4.anilist.co/file/anilistcdn/character/large/b88572-IzTwXEHSobRs.jpg", // Emilia
    "https://s4.anilist.co/file/anilistcdn/character/large/b71-1W4panC53vfs.png", // L
    "https://s4.anilist.co/file/anilistcdn/character/large/b87275-mb13EWZBdbh3.png", // Kaneki
    "https://s4.anilist.co/file/anilistcdn/character/large/b40881-F3gr1PkreDvj.png", // Mikasa
    "https://s4.anilist.co/file/anilistcdn/character/large/b89334-OPj1hCzvrt7X.png", // Rem
    "https://s4.anilist.co/file/anilistcdn/character/large/b422-XTaiTuvRohsV.png", // C.C.
    "https://s4.anilist.co/file/anilistcdn/character/large/b126824-MqsCncTO1qpv.png", // Megumin
    "https://s4.anilist.co/file/anilistcdn/character/large/b176754-PCnpqIOkjhFk.png", // Frieren
    "https://s4.anilist.co/file/anilistcdn/character/large/b34470-Jw2LXZBL5R8i.png", // Kurisu
    "https://s4.anilist.co/file/anilistcdn/character/large/b10138-zOPrka0ddZOR.png", // Holo
    "https://s4.anilist.co/file/anilistcdn/character/large/b137080-UHcynYNjb5ZU.png", // Power
    "https://s4.anilist.co/file/anilistcdn/character/large/b14-9Kb1E5oel1ke.png", // Kakashi
    "https://s4.anilist.co/file/anilistcdn/character/large/b85-mkVBh2yjxjmx.png", // Edward Elric
    "https://s4.anilist.co/file/anilistcdn/character/large/b35252-DY9TW6pusqeh.png", // Rintarou Okabe
    "https://s4.anilist.co/file/anilistcdn/character/large/b127212-FVm2tD0erQ5B.png", // Mai Sakurajima
    "https://s4.anilist.co/file/anilistcdn/character/large/b127222-Jh5hhP7vZ7s1.png", // Sukuna
    "https://s4.anilist.co/file/anilistcdn/character/large/b130102-FO1VHNnEnLlB.png", // Denji
    "https://s4.anilist.co/file/anilistcdn/character/large/b28-ivA7UGnfE40a.png", // Hisoka
    "https://s4.anilist.co/file/anilistcdn/character/large/b80-26EhwSsSqQ50.png", // Light Yagami
    "https://s4.anilist.co/file/anilistcdn/character/large/b89616-dXmdOc7L6SDi.png", // Violet Evergarden
    "https://s4.anilist.co/file/anilistcdn/character/large/b137079-6yLEUYR3bmpr.png", // Makima
    "https://s4.anilist.co/file/anilistcdn/character/large/b90169-4wr1Zehnsac8.png", // Aqua
    "https://s4.anilist.co/file/anilistcdn/character/large/b120649-NPaWaIpWy60E.png", // Marin Kitagawa
    "https://s4.anilist.co/file/anilistcdn/character/large/b11-TA5Nuk7EDUZG.jpg", // Spike Spiegel
    "https://s4.anilist.co/file/anilistcdn/character/large/b17-phjcWCkRuIhu.png", // Naruto
    "https://s4.anilist.co/file/anilistcdn/character/large/b126071-BTNEc1nRIv68.png", // Tanjiro
    "https://s4.anilist.co/file/anilistcdn/character/large/b500-NQrLbnBr1sDv.png", // Guts
    "https://s4.anilist.co/file/anilistcdn/character/large/b126635-L0y3I92JSUkN.png", // Shinobu Kocho
    "https://s4.anilist.co/file/anilistcdn/character/large/b133676-kV2czE3C8Qls.png", // Anya Forger
    "https://s4.anilist.co/file/anilistcdn/character/large/b6356-RImEfUfHpC58.png", // Asuka Langley
    "https://s4.anilist.co/file/anilistcdn/character/large/b89220-KNBwaVFAR8FD.png", // Kazuma Satou
    "https://s4.anilist.co/file/anilistcdn/character/large/b13020-ZdiYlNmpRUNS.png", // Roy Mustang
    "https://s4.anilist.co/file/anilistcdn/character/large/b89361-tq8PQQ4MmF0M.png"  // Megumi Fushiguro
)

private data class ProfileFeatureEntry(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

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
    onOpenUserProfile: ((String) -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditSheet by remember { mutableStateOf(false) }
    var initialEditTab by remember { mutableIntStateOf(0) }
    var showDownloadSheet by remember { mutableStateOf(false) }

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

    val isDark = LocalIsDark.current
    val pageBg = colors.bg
    val cardBg = colors.surface
    val cardElevated = colors.surfaceElevated
    val textPrimary = colors.textPrimary
    val textSecondary = colors.textSecondary
    val textMuted = colors.textMuted
    val borderColor = colors.glass.border

    val bannerUrl = userProfile.bannerUrl.takeIf { it.isNotBlank() }
    val avatarUrl = userProfile.avatarUrl.takeIf { it.isNotBlank() } ?: ANILIST_TMDB_AVATARS[0]
    val frameUrl = getResolvedFrameUrl(userProfile.avatarDecorationId ?: userProfile.avatarFrameId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
        ) {
            // Full-bleed Profile Hero Header
            item(key = "profile_hero_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(pageBg)
                ) {
                    if (bannerUrl != null) {
                        SubcomposeAsyncImage(
                            model = bannerUrl,
                            contentDescription = "Profile Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                ShimmerBox(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(0.dp)
                                )
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF141416))
                        )
                    }

                    // Top status bar scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xCC000000), Color.Transparent)
                                )
                            )
                    )

                    // Cinematic Bottom Gradient (Melts into page background in both light & dark themes)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        pageBg.copy(alpha = 0.40f),
                                        pageBg.copy(alpha = 0.85f),
                                        pageBg
                                    )
                                )
                            )
                    )

                    // Top Bar: Back button on left (top-right edit option removed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x80000000) else Color(0xCCFFFFFF))
                                    .border(1.dp, borderColor, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Continuous Profile Identity Section (Direct page content, no floating card box)
            item(key = "profile_identity_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Avatar Row overlapping bottom edge of banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-36).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Avatar with animated APNG frame
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clickable {
                                    initialEditTab = 1
                                    showEditSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, pageBg, CircleShape),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    ShimmerBox(
                                        modifier = Modifier.size(74.dp),
                                        shape = CircleShape
                                    )
                                }
                            )
                            if (frameUrl != null) {
                                ApngDecorationImage(
                                    url = frameUrl,
                                    contentDescription = "Avatar Frame",
                                    modifier = Modifier
                                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                                        .requiredSize(96.dp)
                                )
                            }
                            // Small edit camera badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                                    .border(1.5.dp, pageBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Right corner pencil button for customizing profile
                        IconButton(
                            onClick = {
                                initialEditTab = 0
                                showEditSheet = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(cardElevated)
                                .border(1.dp, borderColor, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Negative spacer to recover from avatar offset
                    Spacer(Modifier.height((-20).dp))

                    // Display Name
                    Text(
                        text = userProfile.displayName.ifBlank { "Zenkai User" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )

                    // Username
                    if (userProfile.username.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "@${userProfile.username.removePrefix("@")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMuted
                        )
                    }

                    // Direct Bio Section (Tap anywhere or tap pencil to edit bio)
                    Spacer(Modifier.height(10.dp))
                    if (userProfile.bio.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardElevated)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    initialEditTab = 0
                                    showEditSheet = true
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = userProfile.bio,
                                fontSize = 13.5.sp,
                                color = textPrimary,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit bio",
                                tint = textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardElevated)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    initialEditTab = 0
                                    showEditSheet = true
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "+ Add a bio...",
                                fontSize = 12.5.sp,
                                color = textMuted
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }

            // Quick Access & Features Section (Moved from top navbar / sidebar to profile)
            item(key = "profile_features_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EXPLORE & FEATURES",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    val featureList = listOf(
                        ProfileFeatureEntry("Movies & TV", "Browse anime movies, series, OVAs & specials", Icons.Default.Movie, "movies"),
                        ProfileFeatureEntry("Airing & Schedule", "Release calendar & episode countdowns", Icons.Default.Schedule, "airing"),
                        ProfileFeatureEntry("Watch Together", "Stream videos in sync with friends", Icons.Default.Groups, "watchtogether"),
                        ProfileFeatureEntry("Community & Chat", "Join discussions, rankings & user feed", Icons.Default.Forum, "community"),
                        ProfileFeatureEntry("Saved Library", "Your watchlist, bookmarks & history", Icons.Default.Bookmark, "library"),
                        ProfileFeatureEntry("Downloads", "Manage offline anime and downloaded episodes", Icons.Default.Download, "downloads"),
                        ProfileFeatureEntry("Discord Community", "Official chat, updates & feature requests", Icons.AutoMirrored.Filled.Send, "discord"),
                        ProfileFeatureEntry("Telegram Channel", "Announcements and instant release alerts", Icons.AutoMirrored.Filled.Send, "telegram")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardElevated)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    ) {
                        featureList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (item.route) {
                                            "downloads" -> onNavigate?.invoke("downloads")
                                            "library" -> onSeeAllWatchlist?.invoke() ?: onNavigate?.invoke("library")
                                            "community" -> onNavigate?.invoke("discord_hub")
                                            "discord" -> {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kHxHzZtH7"))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                            "telegram" -> {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/StreamverseOfc"))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                            else -> onNavigate?.invoke(item.route)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 12.sp,
                                        color = textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (index < featureList.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    color = borderColor.copy(alpha = 0.5f),
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            // Seamless Page Tabs (Scrollable so tabs never wrap text)
            item(key = "profile_tabs_header") {
                val pageTabs = listOf("Preferences", "Cloud & Sync", "AniList", "About")
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 20.dp,
                    containerColor = Color.Transparent,
                    contentColor = textPrimary,
                    divider = { HorizontalDivider(color = borderColor, thickness = 1.dp) },
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = textPrimary
                            )
                        }
                    }
                ) {
                    pageTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) textPrimary else textMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }

            // Tab 0: Preferences
            if (selectedTab == 0) {
                item(key = "tab_preferences") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Theme Mode
                        SettingRow(title = "Theme", hint = "Dark or light appearance") {
                            val isDarkTheme = vm.themeSettings.mode == ThemeMode.DARK
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0))
                                    .border(1.dp, borderColor, CircleShape)
                                    .padding(3.dp)
                            ) {
                                listOf("Dark" to true, "Light" to false).forEach { (label, isD) ->
                                    val selected = isDarkTheme == isD
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) textPrimary else Color.Transparent)
                                            .clickable {
                                                vm.setThemeMode(if (isD) ThemeMode.DARK else ThemeMode.LIGHT)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) pageBg else textMuted
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Default Audio (Sub / Dub)
                        SettingRow(title = "Default audio", hint = "Sub or dub for multi-audio streams") {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0))
                                    .border(1.dp, borderColor, CircleShape)
                                    .padding(3.dp)
                            ) {
                                listOf("sub", "dub").forEach { opt ->
                                    val selected = currentAudio.equals(opt, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selected) textPrimary else Color.Transparent)
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
                                            color = if (selected) pageBg else textMuted
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Preferred Server
                        SettingRow(title = "Preferred server", hint = "Default playback source") {
                            val serverList = listOf("megaplay" to "MegaPlay", "acdn" to "ACDN", "direct" to "Direct", "animesalt" to "AnimeSalt")
                            val currentServer = vm.preferredServerId?.ifBlank { "megaplay" } ?: "megaplay"
                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0))
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable { expanded = !expanded }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = serverList.firstOrNull { it.first == currentServer }?.second ?: currentServer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textMuted, modifier = Modifier.size(14.dp))
                                }

                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(cardElevated)
                                ) {
                                    serverList.forEach { (id, name) ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(name, color = textPrimary, fontSize = 13.sp) },
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

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Preferred Quality
                        SettingRow(title = "Preferred quality", hint = "Video resolution preference") {
                            val qualityList = listOf("" to "Auto", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p")
                            var expandedQ by remember { mutableStateOf(false) }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0))
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable { expandedQ = !expandedQ }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = qualityList.firstOrNull { it.first == currentQuality }?.second ?: "Auto",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textMuted, modifier = Modifier.size(14.dp))
                                }

                                androidx.compose.material3.DropdownMenu(
                                    expanded = expandedQ,
                                    onDismissRequest = { expandedQ = false },
                                    modifier = Modifier.background(cardElevated)
                                ) {
                                    qualityList.forEach { (id, name) ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(name, color = textPrimary, fontSize = 13.sp) },
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

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Autoplay next episode
                        SettingRow(title = "Autoplay next episode", hint = "Plays next episode when current ends") {
                            Switch(
                                checked = autoPlayNext,
                                onCheckedChange = {
                                    autoPlayNext = it
                                    prefs.edit().putBoolean("auto_play_next", it).apply()
                                    Toast.makeText(context, "Autoplay ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = pageBg,
                                    checkedTrackColor = textPrimary,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0),
                                    uncheckedBorderColor = borderColor
                                )
                            )
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Auto-skip intro & outro
                        SettingRow(title = "Auto-skip intro & outro", hint = "Automatically skips opening & ending") {
                            Switch(
                                checked = autoSkipIntro,
                                onCheckedChange = {
                                    autoSkipIntro = it
                                    prefs.edit().putBoolean("auto_skip_intro_outro", it).apply()
                                    Toast.makeText(context, "Auto-skip ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = pageBg,
                                    checkedTrackColor = textPrimary,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0),
                                    uncheckedBorderColor = borderColor
                                )
                            )
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Skip filler episodes
                        SettingRow(title = "Skip filler episodes", hint = "Jumps over non-canon and filler episodes") {
                            Switch(
                                checked = skipFiller,
                                onCheckedChange = {
                                    skipFiller = it
                                    prefs.edit().putBoolean("pref_skip_filler", it).apply()
                                    Toast.makeText(context, "Skip filler ${if (it) "on" else "off"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = pageBg,
                                    checkedTrackColor = textPrimary,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFE2E8F0),
                                    uncheckedBorderColor = borderColor
                                )
                            )
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Downloads configuration
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDownloadSheet = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Offline Downloads Settings",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Storage location, quality and Wi-Fi rules",
                                    fontSize = 12.sp,
                                    color = textMuted
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Tab 1: Cloud & Sync
            if (selectedTab == 1) {
                item(key = "tab_cloud_sync") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isLoggedIn) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardElevated)
                                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable { onLogin?.invoke() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sign in to Cloud Sync",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Back up watch history, bookmarks & continue watching across any device.",
                                        fontSize = 12.sp,
                                        color = textMuted
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            val loggedInState = authState as AuthState.LoggedIn
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connected Account",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = loggedInState.email ?: userProfile.username.ifBlank { "Cloud Connected" },
                                        fontSize = 13.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Last synced: ${formatLastSync(vm.lastCloudSyncTime)}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                if (vm.isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = textPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            HorizontalDivider(color = borderColor, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Push to Cloud
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(cardElevated)
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable(enabled = !vm.isCloudSyncing) {
                                            vm.pushToCloud { success ->
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Synced to Cloud successfully" else "Sync failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Push",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }

                                // Pull from Cloud
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(cardElevated)
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable(enabled = !vm.isCloudSyncing) {
                                            vm.pullFromCloud { success ->
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Restored from Cloud" else "Cloud sync failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Pull",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                }

                                // Sign Out
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF2A1517) else Color(0xFFFEE2E2))
                                        .border(1.dp, Color(0x44EF4444), CircleShape)
                                        .clickable {
                                            vm.signOut()
                                            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp),
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

            // Tab 2: AniList
            if (selectedTab == 2) {
                item(key = "tab_anilist") {
                    val aniList = vm.aniListAccount
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AniList Tracker",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (aniList != null) "Connected as ${aniList.username}" else "Sync watch progress automatically with AniList",
                                    fontSize = 12.sp,
                                    color = textMuted
                                )
                            }

                            if (aniList != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(cardElevated)
                                        .border(1.dp, borderColor, CircleShape)
                                        .clickable {
                                            vm.syncTrackers { res ->
                                                Toast.makeText(context, "AniList synced (${res.totalPulled} items)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Sync Now",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val url = "https://anilist.co/api/v2/oauth/authorize?client_id=18497&response_type=token"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = textPrimary,
                                        contentColor = pageBg
                                    )
                                ) {
                                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Tab 3: About
            if (selectedTab == 3) {
                item(key = "tab_about") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Disclaimer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Zenkai does not host, upload or store any video, novel text or manga images. Streams, chapters and pages are provided by third-party services. All titles, artwork and trademarks belong to their respective owners.",
                                fontSize = 12.sp,
                                color = textMuted,
                                lineHeight = 17.sp
                            )
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Data & credits",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Anime data from AniList & MyAnimeList (via Jikan); filler information from AnimeFillerList; artwork and episode details from TMDB. Avatar frames from SillyTavern-AvatarDecorations collection.",
                                fontSize = 12.sp,
                                color = textMuted,
                                lineHeight = 17.sp
                            )
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(cardElevated)
                                .border(1.dp, borderColor, CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/kHxHzZtH7"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = textPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Support Discord Server", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }

                        Text(
                            text = "Zenkai v2.1.0 • Android Native & Web Unified",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }
                }
            }
        }

        // Profile Customizer Bottom Sheet (Supports swipe-up to fullscreen, isolated content scrolling)
        if (showEditSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val blockSheetScroll = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = available

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity = available
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                sheetState = sheetState,
                containerColor = if (isDark) Color(0xFF101012) else Color(0xFFFFFFFF),
                dragHandle = null
            ) {
                var editTab by remember { mutableIntStateOf(initialEditTab) }
                var editName by remember { mutableStateOf(userProfile.displayName) }
                var editBio by remember { mutableStateOf(userProfile.bio) }
                var editUsername by remember { mutableStateOf(userProfile.username) }
                var editAvatarUrl by remember { mutableStateOf(userProfile.avatarUrl) }
                var editBannerUrl by remember { mutableStateOf(userProfile.bannerUrl) }
                var editFrameId by remember { mutableStateOf(userProfile.avatarDecorationId ?: userProfile.avatarFrameId ?: "none") }

                var avatarList by remember { mutableStateOf(ANILIST_TMDB_AVATARS) }
                var avatarPage by remember { mutableIntStateOf(1) }
                var isLoadingMoreAvatars by remember { mutableStateOf(false) }
                val avatarGridState = rememberLazyGridState()

                var bannerList by remember { mutableStateOf(ANILIST_TMDB_BANNERS) }
                var bannerPage by remember { mutableIntStateOf(1) }
                var isLoadingMoreBanners by remember { mutableStateOf(false) }
                val bannerGridState = rememberLazyGridState()

                // Endless / Unlimited Avatar Pagination
                LaunchedEffect(avatarGridState) {
                    snapshotFlow { avatarGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastIdx ->
                            if (lastIdx != null && lastIdx >= avatarList.size - 8 && !isLoadingMoreAvatars) {
                                isLoadingMoreAvatars = true
                                val nextPage = avatarPage + 1
                                val more = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    AniListCatalog.fetchMoreAvatars(nextPage)
                                }
                                if (more.isNotEmpty()) {
                                    avatarList = (avatarList + more).distinct()
                                    avatarPage = nextPage
                                }
                                isLoadingMoreAvatars = false
                            }
                        }
                }

                // Endless / Unlimited Banner Pagination
                LaunchedEffect(bannerGridState) {
                    snapshotFlow { bannerGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastIdx ->
                            if (lastIdx != null && lastIdx >= bannerList.size - 6 && !isLoadingMoreBanners) {
                                isLoadingMoreBanners = true
                                val nextPage = bannerPage + 1
                                val more = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    AniListCatalog.fetchMoreBanners(nextPage)
                                }
                                if (more.isNotEmpty()) {
                                    bannerList = (bannerList + more).distinct()
                                    bannerPage = nextPage
                                }
                                isLoadingMoreBanners = false
                            }
                        }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .nestedScroll(blockSheetScroll)
                ) {
                    // Sheet Top Bar: Drag indicator & Save/Cancel buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0x40FFFFFF))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showEditSheet = false },
                                shape = CircleShape
                            ) {
                                Text("Cancel", color = textMuted, fontSize = 12.sp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Save", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Live Real-Time Profile Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardElevated)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Mini Banner Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .background(Color(0xFF141416))
                            ) {
                                if (editBannerUrl.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = editBannerUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            ShimmerBox(
                                                modifier = Modifier.fillMaxSize(),
                                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                            )
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xAA000000))
                                            )
                                        )
                                )
                            }

                            // Mini Identity Info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = editAvatarUrl.ifBlank { ANILIST_TMDB_AVATARS[0] },
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            ShimmerBox(
                                                modifier = Modifier.size(42.dp),
                                                shape = CircleShape
                                            )
                                        }
                                    )
                                    val resolvedFrame = getResolvedFrameUrl(editFrameId)
                                    if (resolvedFrame != null) {
                                        ApngDecorationImage(
                                            url = resolvedFrame,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .wrapContentSize(align = Alignment.Center, unbounded = true)
                                                .requiredSize(56.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = editName.ifBlank { "Your Name" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${editUsername.ifBlank { "username" }}",
                                        fontSize = 12.sp,
                                        color = textMuted
                                    )
                                    if (editBio.isNotBlank()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = editBio,
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Customizer Tab Row
                    val customizerTabs = listOf("Bio & Info", "Avatar", "Frame", "Banner")
                    TabRow(
                        selectedTabIndex = editTab,
                        containerColor = Color.Transparent,
                        contentColor = textPrimary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[editTab]),
                                color = textPrimary
                            )
                        }
                    ) {
                        customizerTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = editTab == index,
                                onClick = { editTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (editTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (editTab == index) textPrimary else textMuted
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    when (editTab) {
                        // 0: Bio & Identity
                        0 -> {
                            val fieldColors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedContainerColor = if (isDark) Color(0xFF141419) else Color(0xFFF3F4F6),
                                unfocusedContainerColor = if (isDark) Color(0xFF101014) else Color(0xFFF9FAFB),
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = borderColor,
                                focusedLabelColor = textPrimary,
                                unfocusedLabelColor = textMuted,
                                cursorColor = textPrimary,
                                focusedPlaceholderColor = textMuted,
                                unfocusedPlaceholderColor = textMuted
                            )
                            val inputStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 14.sp
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Display Name") },
                                    textStyle = inputStyle,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = editUsername,
                                    onValueChange = { editUsername = it },
                                    label = { Text("Username (@handle)") },
                                    textStyle = inputStyle,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                // Dedicated Bio Editor
                                OutlinedTextField(
                                    value = editBio,
                                    onValueChange = { if (it.length <= 160) editBio = it },
                                    label = { Text("Bio") },
                                    placeholder = { Text("Tell the world what you love...", color = Color(0xFF64748B)) },
                                    textStyle = inputStyle.copy(lineHeight = 20.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 3,
                                    maxLines = 4
                                )

                                // Character Counter
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${editBio.length} / 160",
                                        fontSize = 11.sp,
                                        color = if (editBio.length > 150) Color(0xFFEF4444) else Color(0xFF94A3B8)
                                    )
                                }

                                // Quick Anime Quotes Preset Chips
                                Text(
                                    text = "Quick anime bios:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF888892)
                                )

                                val quotePresets = listOf(
                                    "I'm gonna be King of the Pirates! \uD83C\uDFF4\u200D\u2620\uFE0F",
                                    "Plus Ultra! \uD83D\uDCA5",
                                    "Nothing happened. \uD83D\uDDE1\uFE0F",
                                    "Believe it! Dattebayo! \uD83C\uDF65",
                                    "Wandering the anime multiverse \uD83C\uDF0C",
                                    "Domain Expansion. \uD83E\uDD1E"
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(quotePresets) { quote ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1E1E22))
                                                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(8.dp))
                                                .clickable { editBio = quote }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = quote,
                                                fontSize = 11.sp,
                                                color = Color(0xFFCCCCCC)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1: Avatar Presets (Smoothly scrollable, NO URL links, NO source text)
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    state = avatarGridState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp)
                                        .nestedScroll(blockSheetScroll),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(avatarList) { url ->
                                        val isSelected = editAvatarUrl == url
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(cardElevated)
                                                .border(
                                                    if (isSelected) 2.5.dp else 1.dp,
                                                    if (isSelected) colors.accent else borderColor,
                                                    CircleShape
                                                )
                                                .clickable { editAvatarUrl = url },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    ShimmerBox(
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = CircleShape
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    if (isLoadingMoreAvatars) {
                                        items(5) {
                                            ShimmerBox(
                                                modifier = Modifier.size(56.dp),
                                                shape = CircleShape
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2: Avatar Frame (Animated APNGs, NO URL links)
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // "None" option
                                    item {
                                        val isNone = editFrameId == "none" || editFrameId.isBlank()
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isNone) Color(0xFF24242A) else Color.Transparent)
                                                .border(1.dp, if (isNone) Color.White else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable { editFrameId = "none" }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("None", fontSize = 11.sp, color = Color(0xFF888892))
                                            }
                                            Text("None", fontSize = 10.sp, color = Color(0xFF888892))
                                        }
                                    }

                                    items(PRESET_FRAMES) { frame ->
                                        val isSelected = editFrameId == frame.id
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0xFF24242A) else Color.Transparent)
                                                .border(1.dp, if (isSelected) Color.White else Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable { editFrameId = frame.id }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(46.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SubcomposeAsyncImage(
                                                    model = editAvatarUrl.ifBlank { ANILIST_TMDB_AVATARS[0] },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                    loading = {
                                                        ShimmerBox(
                                                            modifier = Modifier.size(36.dp),
                                                            shape = CircleShape
                                                        )
                                                    }
                                                )
                                                ApngDecorationImage(
                                                    url = frame.url,
                                                    contentDescription = frame.name,
                                                    modifier = Modifier
                                                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                                                        .requiredSize(48.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = frame.name,
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White else Color(0xFF888892),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3: Banner Presets (AniList & TMDB, NO URL links, NO source text)
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    state = bannerGridState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp)
                                        .nestedScroll(blockSheetScroll),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(bannerList) { url ->
                                        val isSelected = editBannerUrl == url
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(68.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cardElevated)
                                                .border(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) colors.accent else borderColor,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { editBannerUrl = url }
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    ShimmerBox(
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    if (isLoadingMoreBanners) {
                                        items(2) {
                                            ShimmerBox(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(68.dp),
                                                shape = RoundedCornerShape(8.dp)
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

        // Offline Downloads Sheet
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
    val textPrimary = colors.textPrimary
    val textMuted = colors.textMuted

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
                color = textPrimary
            )
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = textMuted,
                    lineHeight = 16.sp
                )
            }
        }
        content()
    }
}
