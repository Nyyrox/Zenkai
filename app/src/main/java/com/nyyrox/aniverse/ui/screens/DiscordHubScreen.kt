package com.nyyrox.aniverse.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.discord.DiscordAccountManager
import com.nyyrox.aniverse.data.discord.DiscordConfig
import com.nyyrox.aniverse.data.discord.DiscordGuildInfo
import com.nyyrox.aniverse.data.discord.DiscordRepository
import com.nyyrox.aniverse.ui.components.DiscordIntegrationSheet
import com.nyyrox.aniverse.ui.components.DiscordLoginModal
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import kotlinx.coroutines.launch

private val HubCardBg = Color(0xFF131722)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordHubScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var guildInfo by remember { mutableStateOf<DiscordGuildInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showIntegrationSheet by remember { mutableStateOf(false) }
    var showLoginModal by remember { mutableStateOf(false) }
    // In-app Discord community WebView
    var showCommunityWebView by remember { mutableStateOf(false) }
    var communityWebViewUrl by remember { mutableStateOf("") }

    val accountState by DiscordAccountManager.accountState.collectAsState()

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            val res = DiscordRepository.getGuildInfo()
            guildInfo = res.getOrNull()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        containerColor = OttBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Discord Community",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Official ZENKAI Server Hub",
                            fontSize = 11.sp,
                            color = OttTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OttBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Hero Guild Banner & Live Card ────────────────────────────────
            item {
                DiscordHeroCard(
                    guildInfo = guildInfo,
                    isLoading = isLoading,
                    onOpenInApp = {
                        // Open Discord community directly in-app via WebView
                        val guildId = guildInfo?.guildId ?: DiscordConfig.GUILD_ID
                        val channelId = guildInfo?.channelId ?: DiscordConfig.MAIN_CHANNEL_ID
                        communityWebViewUrl = "https://discord.com/channels/$guildId/$channelId"
                        showCommunityWebView = true
                    },
                    onOpenExternal = {
                        DiscordConfig.launchDiscordInvite(context)
                    }
                )
            }

            // ── Quick Channel Deep Links ─────────────────────────────────────
            item {
                Text(
                    text = "COMMUNITY CHANNELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HubCardBg)
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp)
                ) {
                    DiscordChannelRow(
                        icon = Icons.Default.Forum,
                        name = "#general-chat",
                        description = "Chat with otaku anime fans worldwide",
                        onClick = {
                            val guildId = guildInfo?.guildId ?: DiscordConfig.GUILD_ID
                            val channelId = guildInfo?.channelId ?: DiscordConfig.MAIN_CHANNEL_ID
                            communityWebViewUrl = "https://discord.com/channels/$guildId/$channelId"
                            showCommunityWebView = true
                        }
                    )
                    HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                    DiscordChannelRow(
                        icon = Icons.Default.Notifications,
                        name = "#announcements",
                        description = "Instant release notifications & updates",
                        onClick = {
                            communityWebViewUrl = "https://discord.com/channels/${DiscordConfig.GUILD_ID}/${DiscordConfig.MAIN_CHANNEL_ID}"
                            showCommunityWebView = true
                        }
                    )
                    HorizontalDivider(color = Color(0x12FFFFFF), modifier = Modifier.padding(horizontal = 16.dp))
                    DiscordChannelRow(
                        icon = Icons.Default.PlayArrow,
                        name = "#anime-requests",
                        description = "Request missing anime series and movies",
                        onClick = {
                            communityWebViewUrl = "https://discord.com/channels/${DiscordConfig.GUILD_ID}/${DiscordConfig.MAIN_CHANNEL_ID}"
                            showCommunityWebView = true
                        }
                    )
                }
            }

            // ── Rich Presence & Account Card ─────────────────────────────────
            item {
                Text(
                    text = "DISCORD RICH PRESENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                DiscordRpcStatusCard(
                    account = accountState,
                    onConfigureClick = {
                        if (accountState != null) {
                            showIntegrationSheet = true
                        } else {
                            showLoginModal = true
                        }
                    }
                )
            }

            // ── Perks & Features ─────────────────────────────────────────────
            item {
                Text(
                    text = "WHY JOIN ZENKAI DISCORD?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HubCardBg)
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Live Streaming Activity",
                        description = "Show what anime episode you're streaming directly on your Discord profile."
                    )
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Direct Anime Requests",
                        description = "Request new anime titles and report dead streams directly to the admin team."
                    )
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Exclusive Otaku Roles",
                        description = "Earn badges, level up, and participate in community anime watch parties."
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // ── In-App Discord Community WebView ─────────────────────────────────────
    if (showCommunityWebView) {
        DiscordCommunityWebView(
            url = communityWebViewUrl,
            onDismiss = { showCommunityWebView = false },
            onOpenExternal = { DiscordConfig.launchDiscordInvite(context) }
        )
    }

    if (showIntegrationSheet) {
        DiscordIntegrationSheet(
            onDismiss = { showIntegrationSheet = false }
        )
    }

    // Fix: pass onSuccess so accountState StateFlow triggers immediate UI refresh
    if (showLoginModal) {
        DiscordLoginModal(
            onDismiss = { showLoginModal = false },
            onSuccess = {
                showLoginModal = false
                loadData() // refresh guild member count too
            }
        )
    }
}

// ── In-App Discord Community WebView Dialog ───────────────────────────────────
// Opens Discord web interface directly inside the app.
// User must log in via Discord web (separate from OAuth RPC login).
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DiscordCommunityWebView(
    url: String,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(top = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0F14))
            ) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1D2E))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF5865F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "ZENKAI Discord Community",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Open in external Discord app
                    IconButton(onClick = onOpenExternal, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Discord App",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Discord Web inside WebView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            // Use a desktop user agent so Discord web loads properly
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            webViewClient = WebViewClient()
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun DiscordHeroCard(
    guildInfo: DiscordGuildInfo?,
    isLoading: Boolean,
    onOpenInApp: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5865F2).copy(alpha = 0.28f),
                        Color(0xFF131722)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0xFF5865F2).copy(alpha = 0.6f), Color(0x22FFFFFF))
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Guild Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5865F2))
                        .border(2.dp, Color(0xFF5865F2).copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!guildInfo?.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = guildInfo?.iconUrl,
                            contentDescription = guildInfo?.name ?: "Zenkai",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Z",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guildInfo?.name ?: "ZENKAI",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing online dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF23A55A).copy(alpha = pulseAlpha))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isLoading) "Loading community..." else "${guildInfo?.presenceCount ?: 42} Online · ${guildInfo?.memberCount ?: 220} Members",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF23A55A)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Join our official Discord community for fast anime updates, episode discussions, and live Rich Presence on your profile.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            // Primary: open in-app
            Button(
                onClick = onOpenInApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2))
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Browse Community In-App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            // Secondary: open in Discord app / browser
            OutlinedButton(
                onClick = onOpenExternal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5865F2).copy(alpha = 0.6f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color(0xFF5865F2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Open in Discord App",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF5865F2)
                )
            }
        }
    }
}

@Composable
private fun DiscordChannelRow(
    icon: ImageVector,
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF5865F2).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = OttTextMuted
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DiscordRpcStatusCard(
    account: com.nyyrox.aniverse.data.discord.DiscordAccount?,
    onConfigureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HubCardBg)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (account != null) "Connected as @${account.username}" else "Discord Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (account != null) "Rich Presence Active (Watching Zenkai)" else "Connect to broadcast your watching activity",
                    fontSize = 11.5.sp,
                    color = if (account != null) Color(0xFF23A55A) else OttTextMuted
                )
            }

            Button(
                onClick = onConfigureClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (account != null) Color(0x22FFFFFF) else Color(0xFF5865F2)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (account != null) "Settings" else "Connect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DiscordPerkRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF5865F2).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF5865F2), modifier = Modifier.size(14.dp))
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 16.sp
            )
        }
    }
}
