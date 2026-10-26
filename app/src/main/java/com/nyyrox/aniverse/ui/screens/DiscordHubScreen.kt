package com.nyyrox.aniverse.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordHubScreen(
    onBack: () -> Unit
) {
    val p = rememberHomePalette()
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

    val cardBg = if (p.dark) Color(0xFF131722) else p.flatFill

    Scaffold(
        containerColor = p.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Discord Community",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.text
                        )
                        Text(
                            text = "Official ZENKAI Server Hub",
                            fontSize = 11.sp,
                            color = p.textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = p.text
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = p.textSub
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = p.bg)
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
                    p = p,
                    onOpenInApp = {
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
                    color = p.textMuted,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, p.glassBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp)
                ) {
                    DiscordChannelRow(
                        icon = Icons.Default.Forum,
                        name = "#general-chat",
                        description = "Chat with otaku anime fans worldwide",
                        p = p,
                        onClick = {
                            val guildId = guildInfo?.guildId ?: DiscordConfig.GUILD_ID
                            val channelId = guildInfo?.channelId ?: DiscordConfig.MAIN_CHANNEL_ID
                            communityWebViewUrl = "https://discord.com/channels/$guildId/$channelId"
                            showCommunityWebView = true
                        }
                    )
                    HorizontalDivider(color = p.glassBorder, modifier = Modifier.padding(horizontal = 16.dp))
                    DiscordChannelRow(
                        icon = Icons.Default.Notifications,
                        name = "#announcements",
                        description = "Instant release notifications & updates",
                        p = p,
                        onClick = {
                            communityWebViewUrl = "https://discord.com/channels/${DiscordConfig.GUILD_ID}/${DiscordConfig.MAIN_CHANNEL_ID}"
                            showCommunityWebView = true
                        }
                    )
                    HorizontalDivider(color = p.glassBorder, modifier = Modifier.padding(horizontal = 16.dp))
                    DiscordChannelRow(
                        icon = Icons.Default.PlayArrow,
                        name = "#anime-requests",
                        description = "Request missing anime series and movies",
                        p = p,
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
                    color = p.textMuted,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                DiscordRpcStatusCard(
                    account = accountState,
                    p = p,
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
                    color = p.textMuted,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, p.glassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Live Streaming Activity",
                        description = "Show what anime episode you're streaming directly on your Discord profile.",
                        p = p
                    )
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Direct Anime Requests",
                        description = "Request new anime titles and report dead streams directly to the admin team.",
                        p = p
                    )
                    DiscordPerkRow(
                        icon = Icons.Default.Check,
                        title = "Exclusive Otaku Roles",
                        description = "Earn badges, level up, and participate in community anime watch parties.",
                        p = p
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
            p = p,
            onDismiss = { showCommunityWebView = false },
            onOpenExternal = { DiscordConfig.launchDiscordInvite(context) }
        )
    }

    if (showIntegrationSheet) {
        DiscordIntegrationSheet(
            onDismiss = { showIntegrationSheet = false }
        )
    }

    if (showLoginModal) {
        DiscordLoginModal(
            onDismiss = { showLoginModal = false },
            onSuccess = {
                showLoginModal = false
                loadData()
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DiscordCommunityWebView(
    url: String,
    p: HomePalette,
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (p.dark) Color(0xFF0D0F14) else p.bg)
            ) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (p.dark) Color(0xFF1A1D2E) else p.flatFill)
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
                        color = p.text,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenExternal, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Discord App",
                            tint = p.textSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = p.text
                        )
                    }
                }

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
    p: HomePalette,
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

    val heroBgBottom = if (p.dark) Color(0xFF131722) else p.flatFill

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5865F2).copy(alpha = 0.28f),
                        heroBgBottom
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color(0xFF5865F2).copy(alpha = 0.6f), p.glassBorder)
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
                        color = p.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = p.textSub,
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
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (p.dark) Color(0xFF5865F2).copy(alpha = 0.6f) else Color(0xFF5865F2)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = if (p.dark) Color(0xFF7289DA) else Color(0xFF5865F2),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Open in Discord App",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (p.dark) Color(0xFF7289DA) else Color(0xFF5865F2)
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
    p: HomePalette,
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
                color = p.text
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = p.textMuted
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = p.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DiscordRpcStatusCard(
    account: com.nyyrox.aniverse.data.discord.DiscordAccount?,
    p: HomePalette,
    onConfigureClick: () -> Unit
) {
    val cardBg = if (p.dark) Color(0xFF131722) else p.flatFill
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, p.glassBorder, RoundedCornerShape(16.dp))
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
                    color = p.text
                )
                Text(
                    text = if (account != null) "Rich Presence Active (Watching Zenkai)" else "Connect to broadcast your watching activity",
                    fontSize = 11.5.sp,
                    color = if (account != null) Color(0xFF23A55A) else p.textMuted
                )
            }

            Button(
                onClick = onConfigureClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (account != null) p.chipBg else Color(0xFF5865F2)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (account != null) "Settings" else "Connect",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (account != null) p.text else Color.White
                )
            }
        }
    }
}

@Composable
private fun DiscordPerkRow(
    icon: ImageVector,
    title: String,
    description: String,
    p: HomePalette
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
                color = p.text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = p.textSub,
                lineHeight = 16.sp
            )
        }
    }
}
