package com.nyyrox.aniverse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nyyrox.aniverse.data.ALL_PROVIDERS
import com.nyyrox.aniverse.data.Anime
import com.nyyrox.aniverse.data.DUB_PROVIDERS
import com.nyyrox.aniverse.data.MULTI_PROVIDERS
import com.nyyrox.aniverse.data.Provider
import com.nyyrox.aniverse.data.SUB_PROVIDERS
import com.nyyrox.aniverse.data.UserProfile
import com.nyyrox.aniverse.data.supabase.WatchTogetherRoom
import com.nyyrox.aniverse.player.CinePlayerView
import com.nyyrox.aniverse.player.IframePlayerView
import com.nyyrox.aniverse.ui.components.OttEpisodeCardItem
import com.nyyrox.aniverse.ui.theme.AppColors
import com.nyyrox.aniverse.ui.theme.LocalAppColors
import com.nyyrox.aniverse.ui.theme.LocalIsDark
import com.nyyrox.aniverse.ui.theme.OttBg
import com.nyyrox.aniverse.ui.theme.OttCardShape
import com.nyyrox.aniverse.ui.theme.OttPillShape
import com.nyyrox.aniverse.ui.theme.OttPrimary
import com.nyyrox.aniverse.ui.theme.OttSurface
import com.nyyrox.aniverse.ui.theme.OttSurfaceBorder
import com.nyyrox.aniverse.ui.theme.OttTextMuted
import com.nyyrox.aniverse.ui.theme.OttTextPrimary
import com.nyyrox.aniverse.ui.theme.OttTextSecondary
import com.nyyrox.aniverse.ui.theme.ZenkaiFontFamily
import com.nyyrox.aniverse.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Room code extractor matching Web WatchPartyPage: handles pure code or full URLs */
private fun extractRoomCode(raw: String): String {
    val trimmed = raw.trim()
    val match = Regex("""[?&]room=([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE).find(trimmed)
    if (match != null) return match.groupValues[1].lowercase()
    return trimmed.lowercase().replace(Regex("""[^a-z0-9_-]"""), "").take(12)
}

/** WebRTC live floating reaction bubble item */
data class LiveReactionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emoji: String,
    val username: String,
    val avatarUrl: String? = null,
    val startMs: Long = System.currentTimeMillis(),
    val xRatio: Float = (0.15f + (java.util.Random().nextFloat() * 0.7f))
)

@Composable
fun WatchTogetherScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    initialRoomId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRoom = vm.activeWatchRoom
    val myProfile = vm.userProfile

    LaunchedEffect(Unit) {
        if (initialRoomId != null && currentRoom == null) {
            vm.joinWatchTogetherRoom(initialRoomId)
        } else {
            vm.loadActiveWatchRooms()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Keep room in vm or leave on back if user chooses
        }
    }

    if (currentRoom == null) {
        WatchTogetherLobby(
            vm = vm,
            onBack = onBack,
            modifier = modifier
        )
    } else {
        WatchTogetherRoomView(
            vm = vm,
            room = currentRoom,
            onLeaveRoom = { vm.leaveWatchTogetherRoom() },
            modifier = modifier
        )
    }
}

@Composable
private fun WatchTogetherLobby(
    vm: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val isDark = LocalIsDark.current
    var roomCodeInput by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadActiveWatchRooms()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.glass.border, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Watch Together",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontFamily = ZenkaiFontFamily
                )
                Text(
                    text = "Sync & stream anime with friends in real time",
                    fontSize = 11.5.sp,
                    color = colors.textMuted
                )
            }
            IconButton(
                onClick = { vm.loadActiveWatchRooms() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.glass.border, CircleShape)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Synchronized Cinema Network radar badge
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Synchronized Cinema Network Pill with animated pulsing green dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Synchronized Cinema Network",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Watch Together",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        fontFamily = ZenkaiFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Watch anime with friends in real-time sync. Ultra-low-latency playback, live audio voice channel, and instant room reactions.",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Card 1: Join with Room Code
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Join with Room Code", color = colors.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("Enter the code or paste the link from your friend", color = colors.textMuted, fontSize = 11.5.sp)
                            }
                        }

                        HorizontalDivider(color = colors.glass.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(Modifier.height(14.dp))

                        // Room code input with Paste button
                        Text(
                            "ROOM CODE / INVITE LINK",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it },
                            placeholder = {
                                Text(
                                    "ENTER CODE OR PASTE LINK",
                                    color = colors.textMuted.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = colors.textPrimary
                            ),
                            trailingIcon = {
                                Row(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border, RoundedCornerShape(8.dp))
                                        .clickable {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                                val code = extractRoomCode(clip)
                                                if (code.isNotBlank()) {
                                                    roomCodeInput = code
                                                    Toast.makeText(context, "Pasted code: $code", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = colors.textPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surfaceElevated,
                                unfocusedContainerColor = colors.surfaceElevated,
                                focusedBorderColor = colors.textPrimary,
                                unfocusedBorderColor = colors.glass.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        val cleanCode = remember(roomCodeInput) { extractRoomCode(roomCodeInput) }
                        Button(
                            onClick = {
                                if (cleanCode.length >= 3 && !isJoining) {
                                    isJoining = true
                                    vm.joinWatchTogetherRoom(cleanCode) { room ->
                                        isJoining = false
                                        if (room == null) {
                                            Toast.makeText(context, "Watch party not found or expired", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Joined room ${room.roomCode.uppercase()}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = cleanCode.length >= 3 && !isJoining,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.textPrimary,
                                contentColor = colors.bg,
                                disabledContainerColor = colors.surfaceElevated,
                                disabledContentColor = colors.textMuted
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isJoining || vm.isWatchRoomLoading) {
                                CircularProgressIndicator(color = colors.bg, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting to room...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Join Room", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = colors.glass.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(Modifier.height(14.dp))

                        // 3 Feature Pills: Sync Playback, Voice Audio, Room Chat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(Icons.Default.Radio, "Sync Playback", "Sub-50ms sync"),
                                Triple(Icons.Default.Mic, "Voice Audio", "Live mic channel"),
                                Triple(Icons.Default.Chat, "Room Chat", "Live reactions")
                            ).forEach { (icon, title, desc) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.glass.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
                                    Text(desc, fontSize = 9.5.sp, color = colors.textMuted, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Host a Room
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Host a Room", color = colors.textPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("Start a room and invite your friends", color = colors.textMuted, fontSize = 11.5.sp)
                            }
                        }

                        HorizontalDivider(color = colors.glass.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(Modifier.height(14.dp))

                        // Pick up from watch history
                        val recentHistory = remember(vm.history) { vm.history.take(3) }
                        if (recentHistory.isNotEmpty()) {
                            Text(
                                "PICK UP FROM WATCH HISTORY",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentHistory.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.surfaceElevated)
                                            .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.episodeStill ?: item.cover,
                                            contentDescription = item.displayTitle,
                                            modifier = Modifier
                                                .size(width = 68.dp, height = 44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.displayTitle,
                                                color = colors.textPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Ep ${item.lastEpisode} • SUB",
                                                color = colors.textMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val animeObj = Anime(
                                                    id = item.animeId,
                                                    title = item.displayTitle,
                                                    cover = item.cover,
                                                    banner = item.banner
                                                )
                                                vm.createWatchTogetherRoom(
                                                    name = "${item.displayTitle} Party",
                                                    anime = animeObj,
                                                    episode = item.lastEpisode
                                                ) { r ->
                                                    if (r != null) {
                                                        Toast.makeText(context, "Room ${r.roomCode.uppercase()} created!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.textPrimary,
                                                contentColor = colors.bg
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Host Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Browse Anime Catalog button
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.glass.border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Browse Anime Catalog to Host", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = colors.glass.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(Modifier.height(12.dp))

                        // How Hosting Works Checklist
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Pair(Icons.Default.CheckCircle, "Host controls video play, pause & episodes"),
                                Pair(Icons.Default.Bolt, "Instant code & shareable link generation"),
                                Pair(Icons.Default.Shield, "Private peer-to-peer room security")
                            ).forEach { (icon, desc) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(desc, fontSize = 11.5.sp, color = colors.textMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Active Public Rooms
            item {
                Text(
                    text = "Active Public Parties",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ZenkaiFontFamily
                )
            }

            if (vm.isWatchRoomLoading && vm.activeWatchRoomsList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.textPrimary)
                    }
                }
            } else if (vm.activeWatchRoomsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No active public parties right now", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Create a room or join with a code from a friend!", color = colors.textMuted, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(vm.activeWatchRoomsList, key = { it.id }) { room ->
                    WatchPartyCard(
                        room = room,
                        onJoin = {
                            vm.joinWatchTogetherRoom(room.id)
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWatchPartyDialog(
            vm = vm,
            onDismiss = { showCreateDialog = false },
            onCreated = {
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun WatchPartyCard(
    room: WatchTogetherRoom,
    onJoin: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.glass.border, RoundedCornerShape(16.dp))
            .clickable { onJoin() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = room.animeCover,
            contentDescription = room.animeTitle,
            modifier = Modifier
                .size(width = 64.dp, height = 84.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = room.animeTitle,
                color = colors.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ep ${room.episode}",
                    color = colors.textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${room.members.size.coerceAtLeast(1)} watching",
                    color = colors.textMuted,
                    fontSize = 11.5.sp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bg),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("Join", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** WebRTC live reactions overlay container */
@Composable
private fun LiveReactionsOverlay(
    reactions: List<LiveReactionItem>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        reactions.forEach { item ->
            key(item.id) {
                FloatingReactionBubble(item)
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingReactionBubble(item: LiveReactionItem) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(item.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
        )
    }

    val progress = animProgress.value
    val yOffsetDp = -progress * 140f
    val swayDp = kotlin.math.sin(progress * 3.14159f * 2f) * 14f
    val scale = if (progress < 0.2f) 0.5f + (progress / 0.2f) * 0.5f else (1.1f - (progress - 0.2f) * 0.2f).coerceAtLeast(0.8f)
    val alpha = if (progress < 0.15f) progress / 0.15f else (1f - (progress - 0.5f) / 0.5f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(
                start = ((item.xRatio * 240f) + swayDp).dp,
                bottom = 20.dp
            )
            .graphicsLayer {
                translationY = yOffsetDp * density
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (!item.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(item.emoji, fontSize = 20.sp)
        }
    }
}

/** WebRTC Voice Party Bar with audio wave indicators & microphone toggle */
@Composable
private fun WebRtcVoicePartyBar(
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    isDeafened: Boolean,
    onToggleDeafen: () -> Unit,
    room: WatchTogetherRoom,
    currentUser: UserProfile,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "w1"
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "w2"
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "w3"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.glass.border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Top status & controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (!isMicMuted) Color(0xFF10B981) else Color(0xFF6B7280))
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "LIVE AUDIO PARTY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        if (!isMicMuted) "WebRTC Voice Active • Mic Transmitting" else "WebRTC Voice Connected • Mic Muted",
                        fontSize = 9.5.sp,
                        color = if (!isMicMuted) Color(0xFF10B981) else colors.textMuted
                    )
                }
            }

            // Audio controls (Mic & Deafen)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mic Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isMicMuted) Color(0xFF10B981) else colors.surfaceElevated)
                        .border(1.dp, if (!isMicMuted) Color(0xFF059669) else colors.glass.border, RoundedCornerShape(10.dp))
                        .clickable { onToggleMic() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMicMuted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(14.dp)
                        ) {
                            Box(Modifier.width(2.5.dp).height(waveHeight1.dp).background(Color.White, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(2.5.dp).height(waveHeight2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(2.5.dp).height(waveHeight3.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Mic On", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Muted", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }

                // Deafen / Speaker Toggle Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.glass.border, RoundedCornerShape(10.dp))
                        .clickable { onToggleDeafen() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (!isDeafened) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Deafen",
                        tint = if (!isDeafened) colors.textPrimary else Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = colors.glass.border.copy(alpha = 0.4f), thickness = 0.8.dp)
        Spacer(Modifier.height(8.dp))

        // Members in Voice channel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CONNECTED",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Current user
                item {
                    VoiceMemberPill(
                        displayName = "You",
                        avatarUrl = currentUser.avatarUrl,
                        isSpeaking = !isMicMuted,
                        isMuted = isMicMuted,
                        isHost = room.hostId == currentUser.id,
                        colors = colors
                    )
                }

                // Remote members
                items(room.members.filter { it.userId != currentUser.id }) { m ->
                    VoiceMemberPill(
                        displayName = m.displayName.ifBlank { "Otaku" },
                        avatarUrl = m.avatarUrl,
                        isSpeaking = false,
                        isMuted = false,
                        isHost = m.isHost,
                        colors = colors
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceMemberPill(
    displayName: String,
    avatarUrl: String?,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isHost: Boolean,
    colors: AppColors
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(
                1.dp,
                if (isSpeaking) Color(0xFF10B981) else colors.glass.border,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.textPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
            if (isMuted) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = displayName + if (isHost) " 👑" else "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 1
        )
    }
}

/** Live Reactions Bar for instant tap-to-react emojis */
@Composable
private fun LiveReactionEmojiBar(
    onSendReaction: (String) -> Unit,
    colors: AppColors
) {
    val emojis = listOf("🔥", "😱", "❤️", "😂", "👏", "⚡", "🍿", "⚔️")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.glass.border, CircleShape)
                    .clickable { onSendReaction(emoji) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun CreateWatchPartyDialog(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var partyName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedAnime by remember { mutableStateOf(vm.selected ?: vm.trending.firstOrNull()) }
    var episodeNum by remember { mutableIntStateOf(1) }
    var isPrivate by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            try {
                searchResults = vm.searchAniListAnime(searchQuery)
            } catch (_: Exception) {}
            isSearching = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onDismiss() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LocalAppColors.current.surfaceElevated)
                .border(1.dp, LocalAppColors.current.glass.border, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Text("Create Watch Party", color = LocalAppColors.current.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                // Party Name
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Party Title (Optional)", color = OttTextMuted, fontSize = 12.sp) },
                    placeholder = { Text(selectedAnime?.let { "${it.title} Party" } ?: "Anime Watch Party", color = OttTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LocalAppColors.current.surface,
                        unfocusedContainerColor = LocalAppColors.current.surface,
                        focusedBorderColor = LocalAppColors.current.textPrimary,
                        unfocusedBorderColor = LocalAppColors.current.glass.border,
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Search Anime
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Anime to stream...", color = OttTextMuted, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OttTextMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LocalAppColors.current.surface,
                        unfocusedContainerColor = LocalAppColors.current.surface,
                        focusedBorderColor = LocalAppColors.current.textPrimary,
                        unfocusedBorderColor = LocalAppColors.current.glass.border,
                        focusedTextColor = LocalAppColors.current.textPrimary,
                        unfocusedTextColor = LocalAppColors.current.textPrimary
                    )
                )

                if (searchResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults) { a ->
                            Column(
                                modifier = Modifier
                                    .width(70.dp)
                                    .clickable {
                                        selectedAnime = a
                                        searchResults = emptyList()
                                        searchQuery = ""
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = a.cover,
                                    contentDescription = a.title,
                                    modifier = Modifier
                                        .size(width = 65.dp, height = 85.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            if (selectedAnime?.id == a.id) 2.dp else 0.dp,
                                            if (selectedAnime?.id == a.id) LocalAppColors.current.textPrimary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                Text(a.title, color = LocalAppColors.current.textPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Selected Anime Preview
                selectedAnime?.let { anime ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1C2130))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = anime.cover,
                            contentDescription = anime.title,
                            modifier = Modifier
                                .size(width = 40.dp, height = 55.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(anime.displayTitle, color = LocalAppColors.current.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Total Episodes: ${anime.episodes ?: 12}", color = OttTextMuted, fontSize = 10.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = LocalAppColors.current.textPrimary)
                    }
                    Button(
                        onClick = {
                            selectedAnime?.let { a ->
                                vm.createWatchTogetherRoom(
                                    name = partyName.ifBlank { "${a.title} Watch Party" },
                                    anime = a,
                                    episode = episodeNum,
                                    isPrivate = isPrivate
                                ) {
                                    onCreated()
                                }
                            }
                        },
                        enabled = selectedAnime != null && !vm.isCreatingWatchRoom,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.textPrimary, contentColor = LocalAppColors.current.bg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (vm.isCreatingWatchRoom) {
                            CircularProgressIndicator(color = LocalAppColors.current.bg, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Start Party", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun WatchTogetherRoomView(
    vm: MainViewModel,
    room: WatchTogetherRoom,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isHost = (room.hostId == vm.userProfile.id)
    val colors = LocalAppColors.current
    val isDark = LocalIsDark.current

    // Voice & Reactions state
    var isMicMuted by remember { mutableStateOf(false) }
    var isDeafened by remember { mutableStateOf(false) }
    var activeReactions by remember { mutableStateOf<List<LiveReactionItem>>(emptyList()) }

    // Clean up expired reactions (> 2400ms)
    LaunchedEffect(activeReactions.size) {
        if (activeReactions.isNotEmpty()) {
            delay(2400)
            val now = System.currentTimeMillis()
            activeReactions = activeReactions.filter { now - it.startMs < 2400 }
        }
    }

    // Auto-spawn incoming reaction emoji from remote chat messages
    LaunchedEffect(vm.watchRoomChatMessages.size) {
        val lastMsg = vm.watchRoomChatMessages.lastOrNull()
        if (lastMsg != null && lastMsg.userId != vm.userProfile.id) {
            val emojiCandidate = lastMsg.message.trim()
            if (emojiCandidate in listOf("🔥", "😱", "❤️", "😂", "👏", "⚡", "🍿", "⚔️", "🤣", "💀", "✨")) {
                val remoteReaction = LiveReactionItem(
                    emoji = emojiCandidate,
                    username = lastMsg.displayName ?: lastMsg.username ?: "Otaku",
                    avatarUrl = lastMsg.avatarUrl
                )
                activeReactions = (activeReactions + remoteReaction).takeLast(8)
            }
        }
    }

    val onSendReaction: (String) -> Unit = { emoji ->
        val newReaction = LiveReactionItem(
            emoji = emoji,
            username = vm.userProfile.displayName.ifBlank { "You" },
            avatarUrl = vm.userProfile.avatarUrl.takeIf { it.isNotBlank() }
        )
        activeReactions = (activeReactions + newReaction).takeLast(8)
        vm.sendWatchRoomChatMessage(emoji)
    }

    // Player state tracking
    var currentEpisode by remember(room.id) { mutableIntStateOf(room.episode) }
    var currentProvider by remember(room.id) {
        mutableStateOf(ALL_PROVIDERS.find { it.id == room.providerId } ?: ALL_PROVIDERS.first())
    }
    var localPositionMs by remember(room.id) { mutableLongStateOf(room.positionMs) }
    var isHostPlaying by remember(room.id) { mutableStateOf(room.isPlaying) }

    // Sync state
    var isOutOfSync by remember { mutableStateOf(false) }
    var syncOffsetSeconds by remember { mutableLongStateOf(0L) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Playback (Host) or Room Details (Member)

    // TMDB Episode metadata
    var tmdbEpisodes by remember(room.animeId) { mutableStateOf<Map<Int, com.nyyrox.aniverse.data.tmdb.TmdbEpisodeStill>>(emptyMap()) }
    LaunchedEffect(room.animeId) {
        val eps = com.nyyrox.aniverse.data.tmdb.TmdbRepository.fetchEpisodesMapped(
            anilistId = room.animeId,
            title = room.animeTitle,
            romajiTitle = null,
            season = null,
            totalEpisodes = null,
            format = null
        )
        if (eps.isNotEmpty()) {
            tmdbEpisodes = eps
        }
    }

    val maxEpisode = remember(room.animeId, tmdbEpisodes, vm.selected) {
        vm.selected?.episodes ?: vm.selected?.nextEpisode ?: tmdbEpisodes.keys.maxOrNull() ?: 24
    }

    // Direct Stream Resolution
    var directStreamResult by remember(currentProvider.id, currentEpisode) {
        mutableStateOf<com.nyyrox.aniverse.data.extractor.DirectStreamResult?>(null)
    }
    var isDirectStreamLoading by remember(currentProvider.id, currentEpisode) {
        mutableStateOf(false)
    }

    LaunchedEffect(currentProvider.id, currentEpisode) {
        if (currentProvider.isDirectStream) {
            isDirectStreamLoading = true
            try {
                val res = com.nyyrox.aniverse.data.extractor.DirectStreamResolver.resolveDirectStream(
                    anilistId = room.animeId,
                    episode = currentEpisode,
                    providerId = currentProvider.id,
                    isDub = currentProvider.isDub,
                    animeTitle = room.animeTitle
                )
                directStreamResult = res
            } catch (_: Exception) {
                directStreamResult = null
            } finally {
                isDirectStreamLoading = false
            }
        } else {
            directStreamResult = null
            isDirectStreamLoading = false
        }
    }

    // MAL ID resolution
    var malIdState by remember(room.animeId) { mutableStateOf<Int?>(room.idMal ?: vm.selected?.idMal) }
    LaunchedEffect(room.animeId) {
        if (malIdState == null) {
            var id = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(room.animeId)?.malId
            if (id == null || id <= 0) {
                id = com.nyyrox.aniverse.data.AniListRepository().fetchMalId(room.animeId)
            }
            if (id != null && id > 0) {
                malIdState = id
            }
        }
    }

    // TMDB ID resolution
    var tmdbIdState by remember(room.animeId) { mutableStateOf<Int?>(null) }
    var tmdbTypeState by remember(room.animeId) { mutableStateOf<String?>(null) }
    LaunchedEffect(room.animeId) {
        if (tmdbIdState == null) {
            try {
                val resolved = com.nyyrox.aniverse.data.mapping.AniBridgeResolver.resolve(room.animeId)
                if (resolved?.tmdbId != null && resolved.tmdbId > 0) {
                    tmdbIdState = resolved.tmdbId
                    tmdbTypeState = resolved.tmdbType ?: if (vm.selected?.format.equals("MOVIE", ignoreCase = true)) "movie" else "tv"
                } else {
                    val tmdbResult = com.nyyrox.aniverse.data.tmdb.TmdbRepository.resolveTmdbId(
                        anilistId = room.animeId,
                        title = room.animeTitle,
                        year = vm.selected?.seasonYear,
                        format = vm.selected?.format
                    )
                    if (tmdbResult != null && tmdbResult.id > 0) {
                        tmdbIdState = tmdbResult.id
                        tmdbTypeState = tmdbResult.type
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Host heartbeat ping every 15 seconds
    LaunchedEffect(room.id, isHost) {
        if (isHost) {
            while (true) {
                delay(15000)
                vm.pingWatchRoomHostActivity()
            }
        }
    }

    // Periodic 10-second sync check & Inactivity 5-minute auto-exit
    LaunchedEffect(room.id, isHost) {
        while (true) {
            delay(10000)
            vm.refreshWatchRoomState { updatedRoom ->
                val now = System.currentTimeMillis()
                val hostIdleMs = now - updatedRoom.hostLastActive

                // If host is gone for > 5 minutes (300,000 ms), auto-leave
                if (!isHost && hostIdleMs > 300_000L) {
                    Toast.makeText(context, "Host disconnected for over 5 minutes. Room closed.", Toast.LENGTH_LONG).show()
                    onLeaveRoom()
                    return@refreshWatchRoomState
                }

                if (!isHost) {
                    // Calculate predicted host position
                    val elapsedSinceUpdate = (now - updatedRoom.updatedAt).coerceAtLeast(0)
                    val predictedHostPos = updatedRoom.positionMs + if (updatedRoom.isPlaying) elapsedSinceUpdate else 0L
                    val diff = abs(localPositionMs - predictedHostPos)
                    syncOffsetSeconds = diff / 1000

                    // Check if out of sync (> 5 seconds)
                    if (diff > 5000L || currentEpisode != updatedRoom.episode || currentProvider.id != updatedRoom.providerId) {
                        isOutOfSync = true
                    } else {
                        isOutOfSync = false
                    }
                }
            }
        }
    }

    // Auto-sync for late joiner / room updates
    LaunchedEffect(room.episode, room.providerId) {
        if (!isHost) {
            currentEpisode = room.episode
            ALL_PROVIDERS.find { it.id == room.providerId }?.let {
                currentProvider = it
            }
        }
    }

    val isMovie = remember(vm.selected?.format, tmdbTypeState) {
        vm.selected?.format.equals("MOVIE", ignoreCase = true) || tmdbTypeState == "movie"
    }
    val activeStreamUrl = when {
        currentProvider.isDirectStream -> directStreamResult?.streamUrl.orEmpty()
        else -> currentProvider.url(room.animeId, currentEpisode, malId = malIdState, tmdbId = tmdbIdState, isMovie = isMovie)
    }
    val activeDirectHeaders = directStreamResult?.headers ?: emptyMap()
    val activeDirectSubs = directStreamResult?.subtitles ?: emptyList()
    val isDirectStreamActive = currentProvider.isDirectStream && !directStreamResult?.streamUrl.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Room Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    vm.leaveWatchTogetherRoom(closeRoomIfHost = isHost)
                    onLeaveRoom()
                },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${room.animeTitle} • Ep $currentEpisode (${currentProvider.name})",
                    color = OttPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))

            // Room Code Copy Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2435))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                        Toast.makeText(context, "Room Code ${room.roomCode} copied!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Code: ${room.roomCode}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = OttPrimary, modifier = Modifier.size(12.dp))
            }
        }

        // Synchronized Player View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (isDirectStreamLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = OttPrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Connecting Server...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = ZenkaiFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else if (currentProvider.isEmbedOnly && vm.isPlayEmbed) {
                IframePlayerView(
                    embedUrl = activeStreamUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CinePlayerView(
                    title = room.animeTitle,
                    episode = currentEpisode,
                    episodeTitle = tmdbEpisodes[currentEpisode]?.name,
                    streamUrl = activeStreamUrl,
                    isDirectStream = isDirectStreamActive,
                    isDub = currentProvider.isDub,
                    headers = activeDirectHeaders,
                    externalSubtitles = activeDirectSubs,
                    malId = malIdState,
                    anilistId = room.animeId,
                    modifier = Modifier.fillMaxSize(),
                    initialTimeMs = localPositionMs,
                    onSelectEpisode = { newEp ->
                        if (isHost) {
                            currentEpisode = newEp
                            vm.updateWatchPlaybackState(episode = newEp)
                        }
                    },
                    onProgressUpdate = { pos, _ ->
                        localPositionMs = pos
                    },
                    availableServers = directStreamResult?.availableServers ?: emptyList(),
                    isAutoPlayNext = vm.isAutoPlayNext,
                    onToggleAutoPlayNext = { vm.setAutoPlayNext(it) },
                    isAutoSkipIntroOutro = vm.isAutoSkipIntroOutro,
                    onToggleAutoSkipIntroOutro = { vm.setAutoSkipIntroOutro(it) }
                )
            }

            // Live floating reactions overlay
            LiveReactionsOverlay(
                reactions = activeReactions,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Small Sync Status Banner (Under Player)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHost) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("👑 You are Host (Broadcasting)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOutOfSync) Color(0xFFF59E0B) else Color(0xFF10B981))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isOutOfSync) "⚠️ Out of sync (${syncOffsetSeconds}s delay)" else "🟢 Synced with Host",
                        color = if (isOutOfSync) Color(0xFFF59E0B) else Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sync Button for Member
            if (!isHost) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOutOfSync) Color(0xFFE11D48) else Color(0x22FFFFFF))
                        .clickable {
                            vm.refreshWatchRoomState { latest ->
                                currentEpisode = latest.episode
                                ALL_PROVIDERS.find { it.id == latest.providerId }?.let { currentProvider = it }
                                val elapsed = (System.currentTimeMillis() - latest.updatedAt).coerceAtLeast(0)
                                localPositionMs = latest.positionMs + if (latest.isPlaying) elapsed else 0L
                                isOutOfSync = false
                                Toast.makeText(context, "Synced to Host (Ep $currentEpisode)", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isOutOfSync) "Sync Now" else "Sync", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // WebRTC Voice Party Bar
        WebRtcVoicePartyBar(
            isMicMuted = isMicMuted,
            onToggleMic = {
                isMicMuted = !isMicMuted
                val msg = if (!isMicMuted) "Microphone unmuted (Voice Party Connected)" else "Microphone muted"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                vm.sendWatchRoomChatMessage(if (!isMicMuted) "🎙️ Unmuted mic" else "🔇 Muted mic")
            },
            isDeafened = isDeafened,
            onToggleDeafen = {
                isDeafened = !isDeafened
                Toast.makeText(context, if (!isDeafened) "Audio unmuted" else "Audio deafened", Toast.LENGTH_SHORT).show()
            },
            room = room,
            currentUser = vm.userProfile,
            colors = colors,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Live Reactions Emoji Bar
        LiveReactionEmojiBar(
            onSendReaction = onSendReaction,
            colors = colors
        )

        Spacer(Modifier.height(6.dp))

        // Two-Row Tabs (Chat & Playback/Details)
        val tabs = if (isHost) listOf("💬 Room Chat", "🎬 Playback & Servers (Host)") else listOf("💬 Room Chat", "ℹ️ Room Details & Status")

        TabRow(
            selectedTabIndex = activeTab.coerceIn(0, tabs.size - 1),
            containerColor = colors.surfaceElevated,
            contentColor = Color.White,
            indicator = { tabPositions ->
                val pos = tabPositions.getOrNull(activeTab.coerceIn(0, tabPositions.size - 1))
                if (pos != null) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(pos),
                        color = OttPrimary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (activeTab == index) Color.White else OttTextMuted
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (activeTab == 0) {
                WatchTogetherChatTab(
                    vm = vm,
                    roomId = room.id
                )
            } else if (isHost) {
                HostPlaybackControlsTab(
                    room = room,
                    currentEpisode = currentEpisode,
                    currentProvider = currentProvider,
                    maxEpisode = maxEpisode,
                    tmdbEpisodes = tmdbEpisodes,
                    onSelectEpisode = { newEp ->
                        currentEpisode = newEp
                        vm.updateWatchPlaybackState(episode = newEp)
                    },
                    onSelectProvider = { newProvider ->
                        currentProvider = newProvider
                        vm.updateWatchPlaybackState(providerId = newProvider.id)
                    }
                )
            } else {
                RoomDetailsTab(
                    room = room,
                    currentEpisode = currentEpisode,
                    currentProvider = currentProvider
                )
            }
        }
    }
}


@Composable
private fun WatchTogetherChatTab(
    vm: MainViewModel,
    roomId: String
) {
    var messageText by remember { mutableStateOf("") }
    val messages = vm.watchRoomChatMessages
    val listState = rememberLazyListState()

    LaunchedEffect(roomId) {
        vm.loadWatchRoomChatMessages(roomId)
        while (true) {
            delay(3000)
            vm.loadWatchRoomChatMessages(roomId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bg)
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No messages in party yet. Say hi!", color = OttTextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(messages, key = { it.id.ifBlank { it.message + it.userId } }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!msg.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = msg.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF242B3D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (msg.displayName?.firstOrNull() ?: 'O').uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = msg.displayName ?: msg.username ?: "Otaku",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (msg.role == "ADMIN" || msg.role == "OWNER") {
                                    Spacer(Modifier.width(4.dp))
                                    Text("👑", fontSize = 9.sp)
                                }
                            }
                            Text(
                                text = msg.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Emoji Reaction Bar
        val emojis = listOf("🔥", "😱", "🤣", "🍿", "👏", "❤️", "💀", "✨")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF161A26))
                        .clickable {
                            vm.sendWatchRoomChatMessage(emoji)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 14.sp)
                }
            }
        }

        // Message Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Chat with room...", color = OttTextMuted, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF121520),
                    unfocusedContainerColor = Color(0xFF121520),
                    focusedBorderColor = OttPrimary,
                    unfocusedBorderColor = Color(0x28FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            val text = messageText.trim()
                            messageText = ""
                            vm.sendWatchRoomChatMessage(text)
                        }
                    }
                )
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val text = messageText.trim()
                        messageText = ""
                        vm.sendWatchRoomChatMessage(text)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OttPrimary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HostPlaybackControlsTab(
    room: WatchTogetherRoom,
    currentEpisode: Int,
    currentProvider: Provider,
    maxEpisode: Int,
    tmdbEpisodes: Map<Int, com.nyyrox.aniverse.data.tmdb.TmdbEpisodeStill>,
    onSelectEpisode: (Int) -> Unit,
    onSelectProvider: (Provider) -> Unit
) {
    var selectedSourceMode by remember {
        mutableStateOf(if (currentProvider.isDirectStream) "DIRECT" else "EMBED")
    }
    var selectedCategory by remember {
        mutableStateOf(if (currentProvider.isDub) "DUB" else if (currentProvider.isMulti) "MULTI" else "SUB")
    }
    var isServerDropdownExpanded by remember { mutableStateOf(false) }

    var isEpisodeGridView by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf((currentEpisode - 1) / 100) }
    val pageSize = 100
    val totalPages = ((maxEpisode - 1) / pageSize) + 1

    val availableServers = remember(selectedSourceMode, selectedCategory) {
        ALL_PROVIDERS.filter { p ->
            val matchesMode = if (selectedSourceMode == "DIRECT") p.isDirectStream else p.isEmbedOnly
            val matchesCategory = when (selectedCategory) {
                "DUB" -> p.isDub
                "MULTI" -> p.isMulti
                else -> !p.isDub && !p.isMulti
            }
            matchesMode && matchesCategory
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Host Activity Status Table Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0xFF0F1117))
                    .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "HOST BROADCASTING STATUS",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Auto-close: 5m",
                        fontSize = 10.5.sp,
                        color = OttTextMuted
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Table Details Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Host:", color = OttTextMuted, fontSize = 11.sp)
                        Text("${room.hostName} (👑 You)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Members:", color = OttTextMuted, fontSize = 11.sp)
                        Text("${room.members.size} connected", color = OttPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inactivity Rule:", color = OttTextMuted, fontSize = 11.sp)
                        Text("Auto-closes if host is away > 5m", color = Color(0xFFFBBF24), fontSize = 10.5.sp)
                    }
                }
            }
        }

        // 2. Streaming Server Selection Container (Identical to PlayerScreen)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0xFF0F1117))
                    .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STREAMING SERVERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = OttTextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (selectedSourceMode == "DIRECT") "Native Direct HD Playback" else "Web Sniffing Embed Sources",
                            fontSize = 10.sp,
                            color = OttTextMuted
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // DIRECT vs EMBED Mode Toggle Pill
                    Row(
                        modifier = Modifier
                            .clip(OttPillShape)
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), OttPillShape)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf("DIRECT" to "⚡ DIRECT", "EMBED" to "🌐 EMBED").forEach { (modeKey, label) ->
                            val isModeSelected = selectedSourceMode == modeKey
                            Box(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(if (isModeSelected) Color.White else Color.Transparent)
                                    .clickable {
                                        if (selectedSourceMode != modeKey) {
                                            selectedSourceMode = modeKey
                                            if (modeKey == "DIRECT" && selectedCategory == "MULTI") {
                                                selectedCategory = "SUB"
                                            }
                                            val nextList = ALL_PROVIDERS.filter { p ->
                                                val m = if (modeKey == "DIRECT") p.isDirectStream else p.isEmbedOnly
                                                val c = when (selectedCategory) {
                                                    "DUB" -> p.isDub
                                                    "MULTI" -> p.isMulti
                                                    else -> !p.isDub && !p.isMulti
                                                }
                                                m && c
                                            }
                                            val newProv = nextList.firstOrNull() ?: ALL_PROVIDERS.first { if (modeKey == "DIRECT") it.isDirectStream else it.isEmbedOnly }
                                            onSelectProvider(newProv)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isModeSelected) Color.Black else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // SUB / DUB / MULTI Category Selection Bar (Direct mode only shows SUB / DUB)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val audioCategories = if (selectedSourceMode == "DIRECT") {
                        listOf("SUB" to "SUB", "DUB" to "DUB")
                    } else {
                        listOf("SUB" to "SUB", "DUB" to "DUB", "MULTI" to "MULTI-AUDIO")
                    }
                    audioCategories.forEach { (catKey, label) ->
                        val isCatSelected = selectedCategory == catKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCatSelected) Color.White else Color.Transparent)
                                .clickable {
                                    if (selectedCategory != catKey) {
                                        selectedCategory = catKey
                                        val nextList = ALL_PROVIDERS.filter { p ->
                                            val m = if (selectedSourceMode == "DIRECT") p.isDirectStream else p.isEmbedOnly
                                            val c = when (catKey) {
                                                "DUB" -> p.isDub
                                                "MULTI" -> p.isMulti
                                                else -> !p.isDub && !p.isMulti
                                            }
                                            m && c
                                        }
                                        val newProv = nextList.firstOrNull() ?: ALL_PROVIDERS.first { if (selectedSourceMode == "DIRECT") it.isDirectStream else it.isEmbedOnly }
                                        onSelectProvider(newProv)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCatSelected) Color.Black else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Server Dropdown Trigger Selector Button
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161922))
                            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { isServerDropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (currentProvider.isDirectStream) Color(0xFF22C55E) else Color(0xFF3B82F6))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = currentProvider.name,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1FFFFFFF))
                                .border(0.8.dp, Color(0x35FFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentProvider.serverBadge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentProvider.isDirectStream) Color(0xFF4ADE80) else Color(0xFF93C5FD)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Server",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isServerDropdownExpanded,
                        onDismissRequest = { isServerDropdownExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF161922))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        val serverOptions = if (availableServers.isNotEmpty()) availableServers else ALL_PROVIDERS
                        serverOptions.forEach { p ->
                            val isCurrent = p.id == currentProvider.id
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (p.isDirectStream) Color(0xFF22C55E) else Color(0xFF3B82F6))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = p.name,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                            fontSize = 13.sp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "(${p.serverBadge})",
                                            fontSize = 10.sp,
                                            color = OttTextMuted
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectProvider(p)
                                    isServerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Episode Navigator Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalPages > 1) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(totalPages) { page ->
                            val pStart = page * pageSize + 1
                            val pEnd = minOf((page + 1) * pageSize, maxEpisode)
                            val isSelected = selectedPage == page
                            Box(
                                modifier = Modifier
                                    .clip(OttPillShape)
                                    .background(if (isSelected) Color.White else Color(0x18FFFFFF))
                                    .border(1.dp, if (isSelected) Color.White else Color(0x22FFFFFF), OttPillShape)
                                    .clickable { selectedPage = page }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "$pStart–$pEnd",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else OttTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Episodes 1 – $maxEpisode",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Grid vs Card View Toggle
                IconButton(
                    onClick = { isEpisodeGridView = !isEpisodeGridView },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x25FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isEpisodeGridView) Icons.Default.ViewHeadline else Icons.Default.GridView,
                        contentDescription = "Toggle Episode View",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 4. Episode Content (5-col Grid or Rich OttEpisodeCardItem Cards)
        val startEp = selectedPage * pageSize + 1
        val endEp = minOf(startEp + pageSize - 1, maxEpisode)
        val episodeNumbers = (startEp..endEp).toList()

        if (isEpisodeGridView) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = episodeNumbers.chunked(5)
                    chunked.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { epNum ->
                                val isCurrent = epNum == currentEpisode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) Color.White else Color(0xFF181C2A))
                                        .border(
                                            1.dp,
                                            if (isCurrent) Color.White else Color(0x22FFFFFF),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectEpisode(epNum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$epNum",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.Black else OttTextPrimary
                                    )
                                }
                            }
                            repeat(5 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            val dummyAnime = Anime(
                id = room.animeId,
                title = room.animeTitle,
                cover = room.animeCover,
                banner = room.animeCover,
                episodes = maxEpisode
            )
            items(episodeNumbers, key = { it }) { epNum ->
                val epStillInfo = tmdbEpisodes[epNum]
                OttEpisodeCardItem(
                    anime = dummyAnime,
                    episode = epNum,
                    isSelected = epNum == currentEpisode,
                    episodeStill = epStillInfo?.stillUrl?.takeIf { it.isNotBlank() }
                        ?: tmdbEpisodes[1]?.stillUrl?.takeIf { it.isNotBlank() }
                        ?: dummyAnime.banner?.takeIf { it.isNotBlank() }
                        ?: dummyAnime.cover,
                    episodeTitle = epStillInfo?.name,
                    isDownloaded = false,
                    isDownloading = false,
                    onDownload = null,
                    onClick = { onSelectEpisode(epNum) }
                )
            }
        }
    }
}

@Composable
private fun RoomDetailsTab(
    room: WatchTogetherRoom,
    currentEpisode: Int,
    currentProvider: Provider
) {
    val now = System.currentTimeMillis()
    val isHostActive = (now - room.hostLastActive) < 45_000L
    val hostAwayMinutes = ((now - room.hostLastActive) / 60000).coerceAtLeast(1)
    val closeCountdownMinutes = (5 - (now - room.hostLastActive) / 60000).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Host Activity Card (Table)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OttCardShape)
                    .background(Color(0xFF0F1117))
                    .border(1.dp, Color(0x22FFFFFF), OttCardShape)
                    .padding(14.dp)
            ) {
                Text(
                    "PARTY & HOST ACTIVITY DETAILS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color.White
                )

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Host:", color = OttTextMuted, fontSize = 11.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (room.hostAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = room.hostAvatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(room.hostName, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Host Status:", color = OttTextMuted, fontSize = 11.5.sp)
                        Text(
                            text = if (isHostActive) "🟢 Online (Active)" else "🟡 Away (${hostAwayMinutes}m ago)",
                            color = if (isHostActive) Color(0xFF10B981) else Color(0xFFFBBF24),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isHostActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto-Close Timer:", color = OttTextMuted, fontSize = 11.sp)
                            Text("Closes in ${closeCountdownMinutes}m if host is away", color = Color(0xFFF87171), fontSize = 10.5.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Episode:", color = OttTextMuted, fontSize = 11.5.sp)
                        Text("Episode $currentEpisode", color = OttPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Server:", color = OttTextMuted, fontSize = 11.5.sp)
                        Text("${currentProvider.name} (${currentProvider.serverBadge})", color = Color(0xFF60A5FA), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Connected Members List
        item {
            Text("Connected Party Members (${room.members.size})", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }

        items(room.members, key = { it.userId }) { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121520))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF242B3D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((member.displayName.firstOrNull() ?: 'O').uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.displayName, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text(if (member.isHost) "👑 Host" else "Member", color = if (member.isHost) Color(0xFFFBBF24) else OttTextMuted, fontSize = 10.5.sp)
                }
            }
        }
    }
}
